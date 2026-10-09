package ru.inversion.fore.dataset;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.event.EventHandler;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import ru.inversion.dataset.AbstractDataSetBase;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.IDataSetListener;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.parser.OrderByParser;
import ru.inversion.fore.ForeException;
import ru.inversion.fore.form.FormTools;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Связывает записи и курсор IDataSet с таблицей JavaFX без копирования списка записей.
 * Таблица читает непосредственно IDataSet; индексы таблицы и набора совпадают.
 * Состав и порядок getItems() доступны только для чтения; изменения выполняются через IDataSet.
 * Свойство items связано с постоянным представлением строк; reset-all инвалидирует эту привязку.
 * Уведомления передаются на поток JavaFX. Согласование чтения и изменения данных остаётся у владельца набора.
 */
public final class ForeDataSetAdapter<T> implements AutoCloseable
{
   private static final Object ADAPTER_KEY = new Object();
   private static final int CURSOR_CHANGED = 1;
   private static final int ROWS_CHANGED = 2;

   private final IDataSet<T> dataSet;
   private final TableView<T> table;
   private final ItemsBinding<T> items;
   private final Callback<TableView<T>, Boolean> sortPolicy = view -> sortDataSet();
   private final AtomicInteger pending = new AtomicInteger();
   private final AtomicBoolean scheduled = new AtomicBoolean();
   private final CurrentRowProperty<T> currentRow = new CurrentRowProperty<>();

   private final IDataSetRowListener<T> rowListener = event -> refresh();
   private final IDataSetNavigationListener<T> navigationListener = event -> requestUpdate(CURSOR_CHANGED);
   private final IDataSetListener dataSetListener = this::dataSetChanged;
   private final ChangeListener<Number> selectionListener = (observable, oldValue, newValue) -> selectDataSetRow();
   private final ChangeListener<TableViewSelectionModel<T>> selectionModelListener = (observable, oldValue, newValue) -> {
      observeSelection(oldValue, false);
      observeSelection(newValue, true);
      requestUpdate(CURSOR_CHANGED);
   };
   private final InvalidationListener itemsListener = observable -> itemsChanged();
   private final InvalidationListener columnsListener = observable -> configureColumns();
   private final EventHandler<SortEvent<TableView<T>>> sortListener = event -> {
      // Настройка привязки и смена источника таблицы не должны запускать запрос к набору.
      if( this.synchronizing || !ownsItems() ) event.consume();
   };

   private boolean synchronizing;
   private volatile boolean closed;

   private ForeDataSetAdapter(IDataSet<T> dataSet, TableView<T> table)
   {
      this.dataSet = dataSet;
      this.table = table;
      items = new ItemsBinding<>(dataSet);
   }

   /** Полностью заменяет содержимое таблицы без сохранения прежних записей. Выполняется на потоке JavaFX. */
   public static <T> ForeDataSetAdapter<T> bind(IDataSet<T> dataSet, TableView<T> table)
   {
      FormTools.requireFxThread();
      Objects.requireNonNull(dataSet, "dataSet");
      Objects.requireNonNull(table, "table");
      if( table.getProperties().containsKey(ADAPTER_KEY) )
         throw new IllegalStateException("Таблица уже связана с набором данных");
      if( table.itemsProperty().isBound() )
         throw new IllegalStateException("Свойство items таблицы уже связано");

      final var adapter = new ForeDataSetAdapter<>(dataSet, table);
      try
      {
         adapter.connect();
         return adapter;
      }
      catch( RuntimeException | Error failure )
      {
         adapter.close();
         throw failure;
      }
   }

   public IDataSet<T> getDataSet() { return dataSet; }
   public TableView<T> getTable() { return table; }
   public boolean isClosed() { return closed; }

   /** Текущая запись набора, опубликованная на потоке JavaFX; очистка выделения не сбрасывает курсор IDataSet. */
   public T getCurrentRow() { return currentRow.get(); }

   /** Свойство также инвалидируется при обновлении данных того же экземпляра текущей записи. */
   public ReadOnlyObjectProperty<T> currentRowProperty() { return currentRow.getReadOnlyProperty(); }

   /** Уведомляет таблицу после изменения данных без событий. Записи не копируются и не обходятся. */
   public void refresh() { requestUpdate(ROWS_CHANGED | CURSOR_CHANGED); }

   private void connect()
   {
      table.getProperties().put(ADAPTER_KEY, this);
      table.addEventFilter(SortEvent.sortEvent(), sortListener);
      // Политика принадлежит привязке; её установка не должна выполнять запрос к БД.
      synchronizing = true;
      try
      {
         table.setSortPolicy(sortPolicy);
         configureColumns();
      }
      finally { synchronizing = false; }
      table.getVisibleLeafColumns().addListener(columnsListener);
      table.itemsProperty().addListener(itemsListener);
      table.selectionModelProperty().addListener(selectionModelListener);
      observeSelection(table.getSelectionModel(), true);
      dataSet.addRowListener(rowListener);
      dataSet.addNavigationListener(navigationListener);
      dataSet.addDataSetListener(dataSetListener);
      synchronizing = true;
      try
      {
         final var sortColumns = List.copyOf(table.getSortOrder());
         clearTableSelection();
         table.itemsProperty().bind(items);
         if( !sortColumns.isEmpty() ) table.getSortOrder().setAll(sortColumns);
      }
      finally { synchronizing = false; }
      refresh();
   }

   private IEntityProperty<T, ?> getProperty(TableColumn<T, ?> column)
   {
      final var factory = column.getCellValueFactory();
      final String name = factory instanceof PropertyValueFactory<?, ?> propertyFactory
              ? propertyFactory.getProperty() : column.getId();
      if( name == null || name.isBlank() ) return null;
      return EntityMetadataFactory.getEntityMetaData(dataSet.getRowClass()).getProperty(name);
   }

   private void configureColumns()
   {
      if( closed ) return;
      configureColumns(table.getColumns());
   }

   private void configureColumns(List<? extends TableColumn<T, ?>> columns)
   {
      for( final var column : columns )
      {
         if( !column.getColumns().isEmpty() )
         {
            configureColumns(column.getColumns());
            continue;
         }
         final var property = getProperty(column);
         // Даже у вычисляемого свойства с @OrderBy или ProxyFor сортировка запрещена.
         if( property != null && property.isTransient()
                 || dataSet instanceof ISQLDataSet<?> && (property == null
                    || property.getOrderBy() == null || property.getOrderBy().isBlank()) )
            column.setSortable(false);
      }
   }

   private boolean sortDataSet()
   {
      if( closed || synchronizing ) return false;
      configureColumns();
      for( final var column : table.getSortOrder() )
         if( !column.isSortable() ) return false;

      synchronizing = true;
      try
      {
         if( dataSet instanceof ISQLDataSet<?> sqlDataSet )
         {
            final var orderBy = new StringJoiner(", ");
            for( final var column : table.getSortOrder() )
            {
               final var property = getProperty(column);
               if( property == null || property.isTransient() ) return false;
               final String expression = dataSet instanceof SQLDataSet<?> sql && sql.getTaskContext() != null
                       ? property.makeOrderBy(sql.getTaskContext().dialect()) : property.getOrderBy();
               if( expression == null || expression.isBlank() ) return false;
               final String direction = column.getSortType() == TableColumn.SortType.DESCENDING ? " DESC" : " ASC";
               for( String part : OrderByParser.parseAndGetColumnsList(expression) )
                  orderBy.add(part + direction);
            }
            final String previousOrderBy = sqlDataSet.getOrderBy();
            try
            {
               sqlDataSet.setOrderBy(orderBy.length() == 0 ? null : orderBy.toString());
               // Сортирует база; заново загружаются данные и курсор, определённый самим набором.
               sqlDataSet.executeQuery();
            }
            catch( DataSetException failure )
            {
               sqlDataSet.setOrderBy(previousOrderBy);
               throw failure;
            }
         }
         else if( dataSet instanceof AbstractDataSetBase<T> memoryDataSet )
         {
            final var comparator = table.getComparator();
            if( comparator != null )
            {
               final T current = dataSet.getCurrentRow();
               memoryDataSet.sort(comparator);
               if( current != null )
                  for( int index = 0; index < dataSet.getLoadedRowCount(); index++ )
                     if( dataSet.getRow(index) == current )
                     {
                        dataSet.setCurrentRowNum(index);
                        break;
                     }
            }
         }
         else return false;
         refresh();
         return true;
      }
      catch( DataSetException failure )
      {
         // Возвращаем отказ, чтобы JavaFX завершил изменение выделения и отменил изменение заголовка.
         final var thread = Thread.currentThread();
         thread.getUncaughtExceptionHandler().uncaughtException(thread,
                 new ForeException("Не удалось отсортировать набор данных", failure));
         return false;
      }
      finally
      {
         synchronizing = false;
         applyChanges();
      }
   }

   private boolean ownsItems() { return table.getItems() == items.get(); }

   private void itemsChanged()
   {
      if( !ownsItems() ) close();
   }

   private void dataSetChanged(DataSetEvent event)
   {
      if( event.isBefore() || closed ) return;
      if( event.getEventType() == DataSetEvent.DataSetEventType.CLOSE )
      {
         if( Platform.isFxApplicationThread() ) close();
         else Platform.runLater(this::close);
      }
      else if( event.getEventType() == DataSetEvent.DataSetEventType.EXECUTE ) refresh();
   }

   private void requestUpdate(int changes)
   {
      if( closed ) return;
      pending.getAndUpdate(previous -> previous | changes);
      if( Platform.isFxApplicationThread() ) applyChanges();
      else if( scheduled.compareAndSet(false, true) )
         Platform.runLater(() -> {
            scheduled.set(false);
            applyChanges();
         });
   }

   private void applyChanges()
   {
      if( closed || synchronizing ) return;
      synchronizing = true;
      try
      {
         int changes;
         while( (changes = pending.getAndSet(0)) != 0 )
         {
            final boolean changedRows = (changes & ROWS_CHANGED) != 0;
            if( changedRows )
            {
               clearTableSelection();
               // Инвалидация items обновляет размер в штатных моделях выделения и фокуса.
               // Представление и хранилище записей остаются теми же.
               items.invalidate();
            }
            selectTableRow();
            currentRow.publish(dataSet.getCurrentRow(), changedRows);
            if( changedRows ) table.refresh();
         }
      }
      finally
      {
         synchronizing = false;
      }
   }

   private void clearTableSelection()
   {
      final var selection = table.getSelectionModel();
      if( selection != null ) selection.clearSelection();
      if( table.getFocusModel() != null ) table.getFocusModel().focus(-1);
   }

   private void selectTableRow()
   {
      final var selection = table.getSelectionModel();
      if( selection == null ) return;
      final int index = dataSet.getCurrentRowNum();
      if( index < 0 || index >= dataSet.getLoadedRowCount() ) selection.clearSelection();
      else if( selection.getSelectedIndex() != index || selection.getSelectedItem() != items.get().get(index) )
         selection.select(index);
   }

   private void selectDataSetRow()
   {
      if( closed || synchronizing || !ownsItems() ) return;
      final var selection = table.getSelectionModel();
      if( selection == null ) return;
      final int index = selection.getSelectedIndex();
      if( index < 0 || index >= dataSet.getLoadedRowCount() ) return;
      synchronizing = true;
      try
      {
         dataSet.setCurrentRowNum(index);
         requestUpdate(CURSOR_CHANGED);
      }
      finally
      {
         synchronizing = false;
         applyChanges();
      }
   }

   private void observeSelection(TableViewSelectionModel<T> selection, boolean add)
   {
      if( selection == null ) return;
      if( add ) selection.selectedIndexProperty().addListener(selectionListener);
      else selection.selectedIndexProperty().removeListener(selectionListener);
   }

   /** Снимает все подписки и очищает связанное представление таблицы. Сам IDataSet остаётся у владельца. */
   @Override
   public void close()
   {
      FormTools.requireFxThread();
      if( closed ) return;
      closed = true;
      dataSet.removeRowListener(rowListener);
      dataSet.removeNavigationListener(navigationListener);
      dataSet.removeDataSetListener(dataSetListener);
      table.itemsProperty().removeListener(itemsListener);
      table.getVisibleLeafColumns().removeListener(columnsListener);
      table.selectionModelProperty().removeListener(selectionModelListener);
      observeSelection(table.getSelectionModel(), false);
      synchronizing = true;
      try
      {
         if( ownsItems() )
         {
            clearTableSelection();
            table.itemsProperty().unbind();
            table.setItems(FXCollections.emptyObservableList());
         }
         // Не сохраняем прежний обработчик и не оставляем ссылку на закрытый адаптер в таблице.
         if( table.getSortPolicy() == sortPolicy ) table.setSortPolicy(view -> false);
      }
      finally
      {
         table.removeEventFilter(SortEvent.sortEvent(), sortListener);
         synchronizing = false;
      }
      if( table.getProperties().get(ADAPTER_KEY) == this ) table.getProperties().remove(ADAPTER_KEY);
      items.dispose();
      currentRow.set(null);
      pending.set(0);
   }

   /** Источник items уведомляет JavaFX об изменениях, сохраняя один объект представления строк. */
   private static final class ItemsBinding<T> extends ObjectBinding<ObservableList<T>>
   {
      private final ObservableList<T> rows;

      ItemsBinding(IDataSet<T> dataSet) { rows = new ItemsView<>(dataSet); }

      @Override protected ObservableList<T> computeValue() { return rows; }
   }

   /** Представление только для чтения: записи и размер берутся из IDataSet, собственного хранилища нет. */
   private static final class ItemsView<T> extends ObservableListBase<T> implements RandomAccess
   {
      private final IDataSet<T> dataSet;

      ItemsView(IDataSet<T> dataSet)
      {
         this.dataSet = dataSet;
      }

      @Override public T get(int index) { return dataSet.getRows().get(index); }
      @Override public int size() { return dataSet.getLoadedRowCount(); }

      // Отказ сразу: стандартные реализации части этих методов обходят или копируют все строки.
      @Override public boolean add(T row) { throw readOnly(); }
      @Override public void add(int index, T row) { throw readOnly(); }
      @Override public boolean addAll(Collection<? extends T> rows) { throw readOnly(); }
      @Override public boolean addAll(int index, Collection<? extends T> rows) { throw readOnly(); }
      @Override public T set(int index, T row) { throw readOnly(); }
      @Override public boolean setAll(Collection<? extends T> rows) { throw readOnly(); }
      @Override public T remove(int index) { throw readOnly(); }
      @Override public boolean remove(Object row) { throw readOnly(); }
      @Override public void remove(int from, int to) { throw readOnly(); }
      @Override public boolean removeAll(Collection<?> rows) { throw readOnly(); }
      @Override public boolean retainAll(Collection<?> rows) { throw readOnly(); }
      @Override public boolean removeIf(Predicate<? super T> filter) { throw readOnly(); }
      @Override public void replaceAll(UnaryOperator<T> operator) { throw readOnly(); }
      @Override public void sort(Comparator<? super T> comparator) { throw readOnly(); }
      @Override public void clear() { throw readOnly(); }

      @Override public List<T> subList(int from, int to)
      {
         return Collections.unmodifiableList(super.subList(from, to));
      }

      @Override public List<T> reversed()
      {
         return Collections.unmodifiableList(super.reversed());
      }

      private static UnsupportedOperationException readOnly()
      {
         return new UnsupportedOperationException("Изменяйте записи и их порядок через IDataSet");
      }
   }

   private static final class CurrentRowProperty<T> extends ReadOnlyObjectWrapper<T>
   {
      void publish(T row, boolean changedRows)
      {
         if( get() != row ) set(row);
         else if( changedRows ) fireValueChangedEvent();
      }
   }
}
