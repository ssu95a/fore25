package ru.inversion.fore.dataset;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableListBase;
import javafx.event.EventHandler;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.util.Callback;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.IDataSetListener;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.fore.form.FormTools;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Связывает записи и курсор IDataSet с таблицей JavaFX без копирования списка записей.
 * Таблица читает непосредственно IDataSet; индексы таблицы и набора совпадают.
 * Уведомления передаются на поток JavaFX. Согласование чтения и изменения данных остаётся у владельца набора.
 */
public final class ForeDataSetAdapter<T> implements AutoCloseable
{
   private static final Object ADAPTER_KEY = new Object();
   private static final int CURSOR_CHANGED = 1;
   private static final int ROWS_CHANGED = 2;

   private final IDataSet<T> dataSet;
   private final TableView<T> table;
   private final Callback<TableView<T>, Boolean> previousSortPolicy;
   private final Callback<TableView<T>, Boolean> blockedSortPolicy = view -> false;
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
   private final EventHandler<SortEvent<TableView<T>>> sortListener = event -> {
      // Восстановление заголовков после уведомления не должно повторно сортировать данные.
      if( this.synchronizing ) event.consume();
   };

   private ItemsView<T> items;
   private boolean synchronizing;
   private volatile boolean closed;

   private ForeDataSetAdapter(IDataSet<T> dataSet, TableView<T> table)
   {
      this.dataSet = dataSet;
      this.table = table;
      previousSortPolicy = table.getSortPolicy();
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
      // Штатная сортировка JavaFX копирует записи. Сортировку IDataSet задаёт владелец через sortPolicy.
      if( (Object) previousSortPolicy == TableView.DEFAULT_SORT_POLICY ) table.setSortPolicy(blockedSortPolicy);
      table.itemsProperty().addListener(itemsListener);
      table.selectionModelProperty().addListener(selectionModelListener);
      observeSelection(table.getSelectionModel(), true);
      dataSet.addRowListener(rowListener);
      dataSet.addNavigationListener(navigationListener);
      dataSet.addDataSetListener(dataSetListener);
      refresh();
   }

   private void itemsChanged()
   {
      if( table.getItems() != items ) close();
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
         boolean changedRows = false;
         int changes;
         while( !closed && (changes = pending.getAndSet(0)) != 0 )
         {
            changedRows |= (changes & ROWS_CHANGED) != 0;
            if( changedRows || items == null || items.rowCount != dataSet.getLoadedRowCount() )
               replaceView();
            // Пользовательский слушатель может изменить курсор или закрыть привязку во время уведомления.
            if( closed ) return;
            if( pending.get() != 0 ) continue;
            selectTableRow();
            if( closed ) return;
            if( pending.get() != 0 ) continue;
            currentRow.publish(dataSet.getCurrentRow(), changedRows);
            if( closed ) return;
            if( changedRows ) table.refresh();
            changedRows = false;
         }
      }
      finally
      {
         synchronizing = false;
      }
   }

   private void replaceView()
   {
      // События IDataSet не содержат всех удалённых строк. Заменяем только оболочку списка,
      // чтобы не хранить прежние данные и не подменять удалённые строки вымышленными значениями.
      final var sortColumns = List.copyOf(table.getSortOrder());
      final var selection = table.getSelectionModel();
      if( selection != null ) selection.clearSelection();
      if( table.getFocusModel() != null ) table.getFocusModel().focus(-1);
      if( closed ) return;
      items = new ItemsView<>(dataSet);
      table.setItems(items);
      if( !closed && !sortColumns.isEmpty() ) table.getSortOrder().setAll(sortColumns);
   }

   private void selectTableRow()
   {
      final var selection = table.getSelectionModel();
      if( selection == null ) return;
      final int index = dataSet.getCurrentRowNum();
      if( index < 0 || index >= items.size() ) selection.clearSelection();
      else if( selection.getSelectedIndex() != index || selection.getSelectedItem() != items.get(index) )
         selection.select(index);
   }

   private void selectDataSetRow()
   {
      if( closed || synchronizing ) return;
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
      table.selectionModelProperty().removeListener(selectionModelListener);
      observeSelection(table.getSelectionModel(), false);
      synchronizing = true;
      try
      {
         if( table.getItems() == items ) table.setItems(FXCollections.emptyObservableList());
         if( table.getSortPolicy() == blockedSortPolicy ) table.setSortPolicy(previousSortPolicy);
      }
      finally
      {
         table.removeEventFilter(SortEvent.sortEvent(), sortListener);
         synchronizing = false;
      }
      if( table.getProperties().get(ADAPTER_KEY) == this ) table.getProperties().remove(ADAPTER_KEY);
      items = null;
      currentRow.set(null);
      pending.set(0);
   }

   /** Оболочка содержит только размер на момент уведомления; get и size всегда читают сам набор. */
   private static final class ItemsView<T> extends ObservableListBase<T> implements RandomAccess
   {
      private final IDataSet<T> dataSet;
      final int rowCount;

      ItemsView(IDataSet<T> dataSet)
      {
         this.dataSet = dataSet;
         rowCount = dataSet.getLoadedRowCount();
      }

      @Override public T get(int index) { return dataSet.getRows().get(index); }
      @Override public int size() { return dataSet.getLoadedRowCount(); }
      @Override public void clear() { throw new UnsupportedOperationException("Изменяйте записи через IDataSet"); }
      @Override public void sort(Comparator<? super T> comparator) {
         throw new UnsupportedOperationException("Сортировку выполняет владелец IDataSet");
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
