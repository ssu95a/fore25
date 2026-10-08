package ru.inversion.fore.dataset;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.IDataSetListener;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.fore.form.FormTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Связывает записи и курсор IDataSet с таблицей JavaFX.
 * Изменения набора выполняются через IDataSet; таблица получает доступное только для чтения представление.
 * События могут приходить из рабочего потока, а представление и выбор обновляются на потоке JavaFX.
 * Сам IDataSet требует последовательного выполнения операций над данными.
 */
public final class ForeDataSetAdapter<T> implements AutoCloseable
{
   private static final Object ADAPTER_KEY = new Object();

   private final IDataSet<T> dataSet;
   private final TableView<T> table;
   private final ObservableList<T> previousItems;
   private final ObservableList<T> rows = FXCollections.observableArrayList();
   private final SortedList<T> items = new SortedList<>(rows);
   private final AtomicReference<State<T>> state = new AtomicReference<>();
   private final CurrentRowProperty<T> currentRow = new CurrentRowProperty<>();

   private final IDataSetRowListener<T> rowListener = event -> refresh();
   private final IDataSetNavigationListener<T> navigationListener = event -> refreshCursor();
   private final IDataSetListener dataSetListener = this::dataSetChanged;
   private final ChangeListener<Number> selectionListener = (observable, oldValue, newValue) -> selectDataSetRow();
   private final ChangeListener<T> selectedItemListener = (observable, oldValue, newValue) -> selectDataSetRow();
   private final ChangeListener<TableViewSelectionModel<T>> selectionModelListener = (observable, oldValue, newValue) -> {
      observeSelection(oldValue, false);
      observeSelection(newValue, true);
      refreshCursor();
   };
   private final ChangeListener<ObservableList<T>> itemsListener = (observable, oldValue, newValue) -> {
      if( newValue != items ) close();
   };

   private State<T> appliedState;
   private List<T> appliedRows;
   private boolean synchronizing;
   private volatile boolean closed;

   private ForeDataSetAdapter(IDataSet<T> dataSet, TableView<T> table)
   {
      this.dataSet = dataSet;
      this.table = table;
      previousItems = table.getItems();
   }

   /** Создаёт одну привязку к таблице; создание и снятие привязки выполняются на потоке JavaFX. */
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

   /**
    * Публикует актуальные записи и курсор; полезно после изменений, для которых набор не формирует события.
    * Снимок создаётся в вызывающем потоке, поэтому вызов следует согласовывать с операциями над IDataSet.
    */
   public void refresh()
   {
      if( closed ) return;
      final List<T> snapshot = Collections.unmodifiableList(new ArrayList<>(dataSet.getRows()));
      state.set(new State<>(snapshot, dataSet.getCurrentRowNum(), dataSet.getCurrentRow()));
      publish();
   }

   private void connect()
   {
      table.getProperties().put(ADAPTER_KEY, this);
      items.comparatorProperty().bind(table.comparatorProperty());
      table.setItems(items);
      table.itemsProperty().addListener(itemsListener);
      table.selectionModelProperty().addListener(selectionModelListener);
      observeSelection(table.getSelectionModel(), true);
      dataSet.addRowListener(rowListener);
      dataSet.addNavigationListener(navigationListener);
      dataSet.addDataSetListener(dataSetListener);
      refresh();
   }

   private void dataSetChanged(DataSetEvent event)
   {
      if( event.isBefore() || closed ) return;
      if( event.getEventType() == DataSetEvent.DataSetEventType.CLOSE )
      {
         if( Platform.isFxApplicationThread() ) close();
         else Platform.runLater(this::close);
      }
      else if( event.getEventType() == DataSetEvent.DataSetEventType.EXECUTE )
         refresh();
   }

   private void refreshCursor()
   {
      if( closed ) return;
      final int index = dataSet.getCurrentRowNum();
      final T row = dataSet.getCurrentRow();
      // Навигация сохраняет снимок записей, в том числе ещё ожидающий публикации.
      state.updateAndGet(previous -> previous == null ? null : new State<>(previous.rows(), index, row));
      publish();
   }

   private void publish()
   {
      if( Platform.isFxApplicationThread() ) applyState();
      else Platform.runLater(this::applyState);
   }

   private void applyState()
   {
      if( closed || synchronizing ) return;
      synchronizing = true;
      try
      {
         boolean changedRows = false;
         State<T> next;
         while( !closed && (next = state.get()) != null && next != appliedState )
         {
            if( next.rows() != appliedRows )
            {
               appliedRows = next.rows();
               changedRows = true;
               rows.setAll(next.rows());
            }
            // Пользовательский слушатель может изменить набор во время обновления представления.
            if( closed ) return;
            if( next != state.get() ) continue;
            selectTableRow(next);
            if( closed ) return;
            if( next != state.get() ) continue;
            currentRow.publish(next.row(), changedRows);
            if( closed ) return;
            appliedState = next;
            if( changedRows ) table.refresh();
            changedRows = false;
         }
      }
      finally
      {
         synchronizing = false;
      }
   }

   private void selectTableRow(State<T> next)
   {
      final var selection = table.getSelectionModel();
      if( selection == null ) return;
      final int sourceIndex = next.index();
      final int index = next.row() != null && sourceIndex >= 0 && sourceIndex < rows.size()
              && rows.get(sourceIndex) == next.row() ? items.getViewIndex(sourceIndex) : -1;
      if( index < 0 ) selection.clearSelection();
      else if( selection.getSelectedIndex() != index || selection.getSelectedItem() != next.row() )
         selection.select(index);
   }

   private void selectDataSetRow()
   {
      if( closed || synchronizing ) return;
      final var selection = table.getSelectionModel();
      if( selection == null ) return;
      final int viewIndex = selection.getSelectedIndex();
      if( viewIndex < 0 || viewIndex >= items.size() ) return;
      final T row = items.get(viewIndex);
      if( row == null ) return;

      final List<T> dataRows = dataSet.getRows();
      int index = items.getSourceIndex(viewIndex);
      // После фонового обновления показанная строка могла сменить позицию или уже исчезнуть.
      if( index >= dataRows.size() || dataRows.get(index) != row )
      {
         index = -1;
         for( int candidate = 0; candidate < dataRows.size(); candidate++ )
            if( dataRows.get(candidate) == row ) { index = candidate; break; }
      }
      synchronizing = true;
      try
      {
         if( index < 0 || !dataSet.setCurrentRowNum(index) ) refresh();
         else refreshCursor();
      }
      finally
      {
         synchronizing = false;
         applyState();
      }
   }

   private void observeSelection(TableViewSelectionModel<T> selection, boolean add)
   {
      if( selection == null ) return;
      if( add )
      {
         selection.selectedIndexProperty().addListener(selectionListener);
         selection.selectedItemProperty().addListener(selectedItemListener);
      }
      else
      {
         selection.selectedIndexProperty().removeListener(selectionListener);
         selection.selectedItemProperty().removeListener(selectedItemListener);
      }
   }

   /** Снимает все подписки и восстанавливает прежний список таблицы. Сам IDataSet остаётся у владельца. */
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
      items.comparatorProperty().unbind();
      if( table.getItems() == items ) table.setItems(previousItems);
      if( table.getProperties().get(ADAPTER_KEY) == this ) table.getProperties().remove(ADAPTER_KEY);
      rows.clear();
      currentRow.set(null);
      state.set(null);
      appliedState = null;
      appliedRows = null;
   }

   private record State<T>(List<T> rows, int index, T row) {}

   private static final class CurrentRowProperty<T> extends ReadOnlyObjectWrapper<T>
   {
      void publish(T row, boolean changedRows)
      {
         if( get() != row ) set(row);
         else if( changedRows ) fireValueChangedEvent();
      }
   }
}
