package ru.inversion.fore.dataset;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.SelectionMode;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.DataSetRowEvent;
import ru.inversion.dataset.IDataReader;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.IDataSetListener;
import ru.inversion.dataset.IDataSetNavigationListener;
import ru.inversion.dataset.IDataSetRowListener;
import ru.inversion.dataset.ReaderDataSet;
import ru.inversion.fore.FxTestSupport;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static ru.inversion.dataset.IDataSet.InsertRowModeEnum.FIRST;
import static ru.inversion.dataset.IDataSet.InsertRowModeEnum.LAST;

/** Проверки настоящих наборов JInvCommon без TaskContext и подключения к БД. */
class ForeDataSetAdapterTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void bindsAnOrdinaryListAndAnAlreadyPositionedCursor() throws Exception
   {
      FxTestSupport.run(() -> {
         final var dataSet = dataSet();
         assertFalse(dataSet.getRows() instanceof ObservableList<?>);
         dataSet.setCurrentRowNum(1);
         final var table = new TableView<Row>(FXCollections.observableArrayList(new Row(9, "Прежние данные")));
         table.getSelectionModel().selectFirst();
         try( var adapter = ForeDataSetAdapter.bind(dataSet, table) )
         {
            assertEquals(dataSet.getRows(), table.getItems());
            assertSame(dataSet, adapter.getDataSet());
            assertSame(table, adapter.getTable());
            assertSame(dataSet.getCurrentRow(), table.getSelectionModel().getSelectedItem());
            assertSame(dataSet.getCurrentRow(), adapter.getCurrentRow());
            assertThrows(UnsupportedOperationException.class, () -> table.getItems().clear());
         }
         assertTrue(table.getItems().isEmpty());
         assertNull(table.getSelectionModel().getSelectedItem());
         assertEquals(3, dataSet.getLoadedRowCount());
      });
   }

   @Test
   void bindingRefreshAndNavigationDoNotTraverseTwoHundredThousandRows() throws Exception
   {
      FxTestSupport.run(() -> {
         final var rows = new NoTraversalRows(200_000);
         final var dataSet = new ArrayDataSet<>(Row.class, rows, false);
         dataSet.setCurrentRowNum(180_000);
         final var table = new TableView<Row>();
         try( var adapter = ForeDataSetAdapter.bind(dataSet, table) )
         {
            assertSame(rows, dataSet.getRows());
            assertEquals(200_000, table.getItems().size());
            final var view = table.getItems();
            dataSet.setCurrentRowNum(199_999);
            assertSame(view, table.getItems());
            assertEquals(199_999, table.getSelectionModel().getSelectedIndex());
            table.getSelectionModel().select(150_000);
            assertEquals(150_000, dataSet.getCurrentRowNum());

            final var replacement = new Row(150_001, "Новая запись");
            rows.set(150_000, replacement);
            assertSame(replacement, view.get(150_000));
            adapter.refresh();
            assertSame(view, table.getItems());
            assertSame(replacement, table.getSelectionModel().getSelectedItem());
            dataSet.updateCurrentRow(new Row(150_001, "Обновлена через набор"));
            dataSet.removeCurrentRow();
            dataSet.insertRow(new Row(200_001, "Добавлена"), LAST, false);
            adapter.refresh();
            assertSame(view, table.getItems());
            assertEquals(200_000, table.getItems().size());
            assertSame(rows, dataSet.getRows());
            assertTrue(rows.reads < 256, "Привязка должна читать только нужные строки, а не весь набор");
         }
      });
   }

   @Test
   void coalescesWorkerNotificationsWithoutRetainingRowsOrAQueueOfEvents() throws Exception
   {
      final var holder = new Fixture[1];
      final var notifications = new AtomicInteger();
      FxTestSupport.run(() -> {
         final var fixture = holder[0] = new Fixture();
         fixture.table.itemsProperty().addListener((InvalidationListener) observable -> {
            assertTrue(Platform.isFxApplicationThread());
            notifications.incrementAndGet();
         });
         runWorker(() -> {
            for( int index = 0; index < 1_000; index++ )
               fixture.dataSet.insertRow(new Row(index + 4, "Добавлена"), LAST, false);
         });
         assertEquals(1_003, fixture.table.getItems().size());
         assertEquals(0, notifications.get());
      });
      try
      {
         FxTestSupport.run(() -> {
            assertEquals(1, notifications.get());
            assertEquals(1_003, holder[0].table.getItems().size());
         });
      }
      finally { FxTestSupport.run(() -> holder[0].close()); }
   }

   @Test
   void readsObservableDataSetStorageAndPublishesWorkerChangesOnlyOnFx() throws Exception
   {
      final var holder = new AtomicReference<ForeDataSetAdapter<Row>>();
      final var view = new AtomicReference<ObservableList<Row>>();
      final var notifications = new AtomicInteger();
      FxTestSupport.run(() -> {
         final var rows = FXCollections.observableArrayList(new Row(1, "Альфа"), new Row(2, "Бета"));
         final var dataSet = new ArrayDataSet<>(Row.class, rows, false);
         dataSet.setCurrentRowNum(0);
         final var table = new TableView<Row>();
         final var adapter = ForeDataSetAdapter.bind(dataSet, table);
         holder.set(adapter);
         view.set(table.getItems());
         assertSame(rows, dataSet.getRows());
         table.itemsProperty().addListener((InvalidationListener) observable -> {
            assertTrue(Platform.isFxApplicationThread());
            notifications.incrementAndGet();
         });
         runWorker(() -> {
            dataSet.insertRow(new Row(3, "Гамма"), LAST, false);
            dataSet.setCurrentRowNum(dataSet.getLoadedRowCount() - 1);
         });
         assertSame(view.get(), table.getItems());
         assertEquals(3, table.getItems().size());
         assertEquals(0, notifications.get());
      });
      try
      {
         FxTestSupport.run(() -> {
            final var adapter = holder.get();
            assertEquals(1, notifications.get());
            assertSame(view.get(), adapter.getTable().getItems());
            assertEquals(2, adapter.getTable().getSelectionModel().getSelectedIndex());
            assertSame(adapter.getDataSet().getCurrentRow(), adapter.getCurrentRow());
         });
      }
      finally { FxTestSupport.run(() -> holder.get().close()); }
   }

   @Test
   void updatesVisibleCellsOfALargeTableWithoutMaterializingItsRows() throws Exception
   {
      FxTestSupport.run(() -> {
         final var rows = new NoTraversalRows(200_000);
         final var dataSet = new ArrayDataSet<>(Row.class, rows, false);
         dataSet.setCurrentRowNum(0);
         final var table = new TableView<Row>();
         final var name = new TableColumn<Row, String>("Название");
         name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
         table.getColumns().add(name);
         table.setFixedCellSize(24);
         final var stage = new Stage();
         try( var adapter = ForeDataSetAdapter.bind(dataSet, table) )
         {
            try
            {
               stage.setScene(new Scene(table, 320, 240));
               stage.show();
               table.applyCss();
               table.layout();
               assertTrue(rows.reads < 1_024, "Отображение таблицы должно читать только видимые строки");
               rows.reads = 0;

               final var replacement = new Row(1, "Обновлена видимая строка");
               dataSet.updateCurrentRow(replacement);
               table.applyCss();
               table.layout();
               final var firstRow = table.lookupAll(".table-row-cell").stream()
                       .filter(node -> node instanceof TableRow<?> row && row.getIndex() == 0)
                       .map(node -> (TableRow<?>) node).findFirst().orElseThrow();
               assertSame(replacement, firstRow.getItem());
               assertSame(replacement, table.getSelectionModel().getSelectedItem());
               assertSame(replacement, adapter.getCurrentRow());
               assertTrue(rows.reads < 1_024, "Обновление ячеек не должно обходить весь набор");
            }
            finally { stage.hide(); }
         }
      });
   }

   @Test
   void replacesAnExistingSortPolicyWithoutRetainingOrRestoringIt() throws Exception
   {
      FxTestSupport.run(() -> {
         final var dataSet = dataSet();
         final var table = new TableView<Row>();
         final var name = new TableColumn<Row, String>("Название");
         name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
         table.getColumns().add(name);
         final var calls = new AtomicInteger();
         table.setSortPolicy(view -> { calls.incrementAndGet(); return true; });
         final var policy = table.getSortPolicy();
         table.getSortOrder().add(name);
         final int previousCalls = calls.get();
         try( var adapter = ForeDataSetAdapter.bind(dataSet, table) )
         {
            assertNotSame(policy, table.getSortPolicy());
            dataSet.insertRow(new Row(4, "Дельта"), LAST, false);
            adapter.refresh();
            assertEquals(previousCalls, calls.get());
            assertEquals(List.of(name), table.getSortOrder());
         }
         assertNotSame(policy, table.getSortPolicy());
         assertEquals(previousCalls, calls.get());
      });
   }

   @Test
   void synchronizesProgrammaticSelectionAndNavigationWithoutRepeatedDataSetEvents() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var calls = new AtomicInteger();
            fixture.dataSet.addNavigationListener(event -> calls.incrementAndGet());
            assertNull(fixture.adapter.getCurrentRow());
            fixture.table.getSelectionModel().select(2);
            assertEquals(2, fixture.dataSet.getCurrentRowNum());
            assertSame(fixture.dataSet.getCurrentRow(), fixture.adapter.getCurrentRow());
            assertEquals(1, calls.get());
            fixture.dataSet.setCurrentRowNum(0);
            assertEquals(0, fixture.table.getSelectionModel().getSelectedIndex());
            assertEquals(2, calls.get());
         }
      });
   }

   @Test
   void defaultSortingDoesNotCreateAnIndependentOrderOrCopyRows() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            fixture.dataSet.setCurrentRowNum(2);
            fixture.sort(TableColumn.SortType.ASCENDING);
            assertEquals(List.of(2L, 3L, 1L), fixture.ids());
            assertEquals(1, fixture.table.getSelectionModel().getSelectedIndex());
            assertEquals(1, fixture.dataSet.getCurrentRowNum());
            assertThrows(UnsupportedOperationException.class,
                    () -> fixture.table.getItems().sort((left, right) -> left.name().compareTo(right.name())));
         }
      });
   }

   @Test
   void dataSetSortingKeepsTheCursorAndTableAtTheSameIndex() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            fixture.dataSet.setCurrentRowNum(0);
            fixture.sort(TableColumn.SortType.ASCENDING);
            assertEquals(List.of(2L, 3L, 1L), fixture.ids());
            assertEquals(2, fixture.table.getSelectionModel().getSelectedIndex());
            assertEquals(2, fixture.dataSet.getCurrentRowNum());
            fixture.table.getSelectionModel().selectFirst();
            assertEquals(0, fixture.dataSet.getCurrentRowNum());
            fixture.sort(TableColumn.SortType.DESCENDING);
            assertEquals(2, fixture.table.getSelectionModel().getSelectedIndex());
            assertEquals(2, fixture.dataSet.getCurrentRowNum());
            assertEquals(fixture.dataSet.getRows(), fixture.table.getItems());
         }
      });
   }

   @Test
   void updatesAndRemovalsFollowTheDataSetCursorEvenWhenSortingMovesTheRow() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            fixture.sort(TableColumn.SortType.ASCENDING);
            fixture.dataSet.setCurrentRowNum(0);
            final var replacement = new Row(2, "Янтарь");
            fixture.dataSet.updateCurrentRow(replacement);
            assertSame(replacement, fixture.table.getSelectionModel().getSelectedItem());
            assertSame(replacement, fixture.adapter.getCurrentRow());
            fixture.table.sort();
            assertEquals(List.of(3L, 1L, 2L), fixture.ids());
            fixture.dataSet.removeCurrentRow();
            assertEquals(List.of(3L, 1L), fixture.ids());
            assertSame(fixture.dataSet.getCurrentRow(), fixture.table.getSelectionModel().getSelectedItem());
            fixture.dataSet.clear();
            assertTrue(fixture.table.getItems().isEmpty());
            assertNull(fixture.table.getSelectionModel().getSelectedItem());
            assertNull(fixture.adapter.getCurrentRow());
         }
      });
   }

   @Test
   void handlesGroupedInsertionAndNonContiguousRemovalUsingActualDataSetRows() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var first = new Row(4, "Дельта");
            fixture.dataSet.insertRows(List.of(first, new Row(5, "Эпсилон")), FIRST, true);
            assertEquals(List.of(4L, 5L, 1L, 2L, 3L), fixture.ids());
            assertSame(first, fixture.adapter.getCurrentRow());
            fixture.dataSet.setCurrentRowNum(4);
            assertEquals(2, fixture.dataSet.removeRows((row, ignored) -> row.id() == 5 || row.id() == 2, null));
            assertEquals(List.of(4L, 1L, 3L), fixture.ids());
            assertSame(fixture.dataSet.getCurrentRow(), fixture.table.getSelectionModel().getSelectedItem());
         }
      });
   }

   @Test
   void distinguishesEqualRowsAndRepeatedReferencesByTheirSourceIndices() throws Exception
   {
      FxTestSupport.run(() -> {
         final var first = new Row(1, "Одинаковые");
         final var equal = new Row(1, "Одинаковые");
         final var dataSet = new TrackedDataSet<>(Row.class, List.of(first, equal, first));
         final var table = new TableView<Row>();
         try( var adapter = ForeDataSetAdapter.bind(dataSet, table) )
         {
            table.getSelectionModel().select(1);
            assertEquals(1, dataSet.getCurrentRowNum());
            assertSame(equal, adapter.getCurrentRow());
            table.getSelectionModel().select(2);
            assertEquals(2, dataSet.getCurrentRowNum());
            assertSame(first, adapter.getCurrentRow());
            dataSet.setCurrentRowNum(0);
            assertEquals(0, table.getSelectionModel().getSelectedIndex());
         }
      });
   }

   @Test
   void navigationKeepsOtherSelectedRowsInMultipleSelectionMode() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var selection = fixture.table.getSelectionModel();
            selection.setSelectionMode(SelectionMode.MULTIPLE);
            selection.select(0);
            selection.select(2);
            assertEquals(List.of(0, 2), selection.getSelectedIndices());
            assertEquals(2, fixture.dataSet.getCurrentRowNum());
            fixture.dataSet.setCurrentRowNum(0);
            assertEquals(0, selection.getSelectedIndex());
            assertEquals(List.of(0, 2), selection.getSelectedIndices());
         }
      });
   }

   @Test
   void clearingSelectionKeepsTheNonEmptyDataSetCursorAsRequiredByCommon() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            fixture.table.getSelectionModel().selectFirst();
            final var current = fixture.dataSet.getCurrentRow();
            fixture.table.getSelectionModel().clearSelection();
            assertEquals(-1, fixture.table.getSelectionModel().getSelectedIndex());
            assertSame(current, fixture.dataSet.getCurrentRow());
            assertSame(current, fixture.adapter.getCurrentRow());
            fixture.dataSet.setCurrentRowNum(1);
            assertEquals(1, fixture.table.getSelectionModel().getSelectedIndex());
         }
      });
   }

   @Test
   void refreshPublishesListChangesThatWereMadeWithoutDataSetEvents() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var count = Bindings.createIntegerBinding(() -> fixture.table.getItems().size(),
                    fixture.table.itemsProperty());
            assertEquals(3, count.get());
            final var view = fixture.table.getItems();
            fixture.dataSet.getRows().add(new Row(4, "Дельта"));
            assertEquals(4, view.size());
            assertSame(fixture.dataSet.getRow(3), view.get(3));
            assertEquals(3, count.get());
            fixture.adapter.refresh();
            assertSame(view, fixture.table.getItems());
            assertEquals(List.of(1L, 2L, 3L, 4L), fixture.ids());
            assertEquals(4, count.get());
            count.dispose();
         }
      });
   }

   @Test
   void refreshOfTheSamePojoInvalidatesBindingsToTheCurrentRow() throws Exception
   {
      FxTestSupport.run(() -> {
         final var row = new MutableRow("До изменения");
         final var dataSet = new TrackedDataSet<>(MutableRow.class, List.of(row));
         dataSet.setCurrentRowNum(0);
         try( var adapter = ForeDataSetAdapter.bind(dataSet, new TableView<>()) )
         {
            final var name = Bindings.createStringBinding(() -> adapter.getCurrentRow().name, adapter.currentRowProperty());
            assertEquals("До изменения", name.get());
            row.name = "После изменения";
            dataSet.refreshCurrent();
            assertEquals("После изменения", name.get());
            assertSame(row, adapter.getCurrentRow());
            name.dispose();
         }
      });
   }

   @Test
   void workerNotificationsPublishOnlyOnFxWhileRowsAreReadDirectlyFromTheDataSet() throws Exception
   {
      final var holder = new Fixture[1];
      final var fxOnly = new AtomicBoolean(true);
      FxTestSupport.run(() -> {
         final var fixture = holder[0] = new Fixture();
         fixture.table.itemsProperty().addListener((InvalidationListener) observable ->
                 fxOnly.compareAndSet(true, Platform.isFxApplicationThread()));
         fixture.adapter.currentRowProperty().addListener((InvalidationListener) observable ->
                 fxOnly.compareAndSet(true, Platform.isFxApplicationThread()));
         runWorker(() -> {
            fixture.dataSet.insertRow(new Row(4, "Дельта"), LAST, false);
            fixture.dataSet.setCurrentRowNum(3);
         });
         assertEquals(4, fixture.table.getItems().size());
         // Выбор происходит до обработки поставленных в очередь событий рабочего потока.
         fixture.table.getSelectionModel().select(1);
         assertEquals(4, fixture.table.getItems().size());
         assertEquals(1, fixture.dataSet.getCurrentRowNum());
      });
      try
      {
         FxTestSupport.run(() -> {
            assertTrue(fxOnly.get());
            assertEquals(4, holder[0].table.getItems().size());
            assertSame(holder[0].dataSet.getCurrentRow(), holder[0].adapter.getCurrentRow());
            assertEquals(1, holder[0].table.getSelectionModel().getSelectedIndex());
         });
      }
      finally { FxTestSupport.run(() -> holder[0].close()); }
   }

   @Test
   void selectionUsesTheCurrentDataSetIndexAfterAWorkerRemovesARow() throws Exception
   {
      final var holder = new Fixture[1];
      FxTestSupport.run(() -> {
         final var fixture = holder[0] = new Fixture();
         runWorker(() -> {
            fixture.dataSet.setCurrentRowNum(1);
            fixture.dataSet.removeCurrentRow();
         });
         fixture.table.getSelectionModel().select(1);
         assertEquals(List.of(1L, 3L), fixture.ids());
         assertEquals(3, fixture.adapter.getCurrentRow().id());
      });
      try { FxTestSupport.run(() -> assertEquals(3, holder[0].table.getSelectionModel().getSelectedItem().id())); }
      finally { FxTestSupport.run(() -> holder[0].close()); }
   }

   @Test
   void readerQueryAndFurtherPagesUseTheSameBinding() throws Exception
   {
      final var data = List.of(new Row(1, "Альфа"), new Row(2, "Бета"), new Row(3, "Гамма"));
      final var readerDataSet = new ReaderDataSet<>(Row.class, ignored -> new PageReader(data));
      readerDataSet.setPageSize(2);
      final var table = new TableView<Row>();
      final var adapter = new ForeDataSetAdapter<?>[1];
      FxTestSupport.run(() -> adapter[0] = ForeDataSetAdapter.bind(readerDataSet, table));
      try
      {
         readerDataSet.executeQuery();
         FxTestSupport.run(() -> {
            assertEquals(data.subList(0, 2), table.getItems());
            assertEquals(0, table.getSelectionModel().getSelectedIndex());
         });
         assertTrue(readerDataSet.swappingData());
         FxTestSupport.run(() -> {
            assertEquals(data, table.getItems());
            table.getSelectionModel().selectLast();
            assertEquals(2, readerDataSet.getCurrentRowNum());
         });
         readerDataSet.executeQuery();
         FxTestSupport.run(() -> {
            assertEquals(data.subList(0, 2), table.getItems());
            assertEquals(0, table.getSelectionModel().getSelectedIndex());
         });
      }
      finally { FxTestSupport.run(() -> adapter[0].close()); }
   }

   @Test
   void closeEmptiesTheTableRemovesAllSubscriptionsAndLeavesTheDataSetWithItsOwner() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            fixture.dataSet.setCurrentRowNum(0);
            assertEquals(1, fixture.dataSet.rowsListeners.size());
            assertEquals(1, fixture.dataSet.navigationListeners.size());
            assertEquals(1, fixture.dataSet.dataSetListeners.size());
            fixture.adapter.close();
            fixture.adapter.close();
            assertTrue(fixture.adapter.isClosed());
            assertTrue(fixture.table.getItems().isEmpty());
            assertEquals(-1, fixture.table.getSelectionModel().getSelectedIndex());
            assertNull(fixture.table.getSelectionModel().getSelectedItem());
            assertFalse(fixture.table.getSortPolicy().call(fixture.table));
            assertNull(fixture.adapter.getCurrentRow());
            assertTrue(fixture.dataSet.rowsListeners.isEmpty());
            assertTrue(fixture.dataSet.navigationListeners.isEmpty());
            assertTrue(fixture.dataSet.dataSetListeners.isEmpty());
            assertEquals(0, fixture.dataSet.closeCalls);
            assertEquals(3, fixture.dataSet.getLoadedRowCount());
            fixture.dataSet.insertRow(new Row(4, "Дельта"), FIRST, true);
            fixture.table.getSelectionModel().selectFirst();
            assertTrue(fixture.table.getItems().isEmpty());
            assertNull(fixture.adapter.getCurrentRow());
         }
      });
   }

   @Test
   void queuedUpdatesCannotOverwriteANewBindingAfterClose() throws Exception
   {
      final var adapters = new ArrayList<ForeDataSetAdapter<Row>>();
      final var table = new TableView<Row>();
      final var replacement = new TrackedDataSet<>(Row.class, List.of(new Row(9, "Новый набор")));
      FxTestSupport.run(() -> {
         final var old = ForeDataSetAdapter.bind(dataSet(), table);
         adapters.add(old);
         runWorker(() -> old.getDataSet().insertRow(new Row(4, "Дельта"), FIRST, true));
         old.close();
         adapters.add(ForeDataSetAdapter.bind(replacement, table));
      });
      try { FxTestSupport.run(() -> assertEquals(replacement.getRows(), table.getItems())); }
      finally { FxTestSupport.run(() -> adapters.forEach(ForeDataSetAdapter::close)); }
   }

   @Test
   void replacingItemsDetachesTheAdapterWithoutOverwritingTheNewList() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var replacement = FXCollections.observableArrayList(new Row(9, "Другие данные"));
            fixture.table.itemsProperty().unbind();
            fixture.table.setItems(replacement);
            assertTrue(fixture.adapter.isClosed());
            assertSame(replacement, fixture.table.getItems());
            assertTrue(fixture.dataSet.rowsListeners.isEmpty());
            assertTrue(fixture.dataSet.navigationListeners.isEmpty());
            assertTrue(fixture.dataSet.dataSetListeners.isEmpty());
         }
      });
   }

   @Test
   void supportsRemovingAndRestoringTheSelectionModel() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var selection = fixture.table.getSelectionModel();
            fixture.table.setSelectionModel(null);
            fixture.dataSet.setCurrentRowNum(1);
            assertSame(fixture.dataSet.getCurrentRow(), fixture.adapter.getCurrentRow());
            fixture.table.setSelectionModel(selection);
            assertEquals(1, selection.getSelectedIndex());
         }
      });
   }

   @Test
   void rollsBackPartialBindingAndRejectsTwoAdaptersOrAnAlreadyBoundItemsProperty() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            assertThrows(IllegalStateException.class, () -> ForeDataSetAdapter.bind(dataSet(), fixture.table));
            assertFalse(fixture.adapter.isClosed());
         }
         final var dataSet = dataSet();
         final var table = new TableView<Row>();
         final var previousItems = table.getItems();
         dataSet.failNavigation = true;
         assertThrows(IllegalStateException.class, () -> ForeDataSetAdapter.bind(dataSet, table));
         assertSame(previousItems, table.getItems());
         assertTrue(dataSet.rowsListeners.isEmpty());
         assertTrue(dataSet.navigationListeners.isEmpty());
         assertTrue(dataSet.dataSetListeners.isEmpty());
         dataSet.failNavigation = false;
         try( var ignored = ForeDataSetAdapter.bind(dataSet, table) ) { assertEquals(3, table.getItems().size()); }
         table.itemsProperty().bind(new SimpleObjectProperty<>(previousItems));
         assertThrows(IllegalStateException.class, () -> ForeDataSetAdapter.bind(dataSet, table));
         table.itemsProperty().unbind();
      });
   }

   @Test
   void aDataSetCloseEventDetachesTheAdapter() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            fixture.dataSet.close();
            assertTrue(fixture.adapter.isClosed());
            assertTrue(fixture.table.getItems().isEmpty());
            assertTrue(fixture.dataSet.dataSetListeners.isEmpty());
         }
      });
   }

   @Test
   void creationAndCloseRequireTheFxThread() throws Exception
   {
      assertThrows(IllegalStateException.class, () -> ForeDataSetAdapter.bind(null, null));
      final var adapter = new ForeDataSetAdapter<?>[1];
      FxTestSupport.run(() -> adapter[0] = ForeDataSetAdapter.bind(dataSet(), new TableView<>()));
      try { assertThrows(IllegalStateException.class, () -> adapter[0].close()); }
      finally { FxTestSupport.run(() -> adapter[0].close()); }
   }

   private static TrackedDataSet<Row> dataSet()
   {
      return new TrackedDataSet<>(Row.class,
              List.of(new Row(1, "Гамма"), new Row(2, "Альфа"), new Row(3, "Бета")));
   }

   private static void runWorker(Runnable operation) throws Exception
   {
      final var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
      final var worker = Thread.ofVirtual().start(() -> {
         try { operation.run(); }
         catch( Throwable error ) { failure.set(error); }
      });
      worker.join(5000);
      assertFalse(worker.isAlive(), "Рабочий поток не завершился");
      if( failure.get() != null ) throw new AssertionError(failure.get());
   }

   private record Row(long id, String name) {}

   /** Запрещает материализацию и полный обход большого списка, оставаясь хранилищем самого IDataSet. */
   private static final class NoTraversalRows extends AbstractList<Row> implements RandomAccess
   {
      private final ArrayList<Row> rows;
      int reads;

      NoTraversalRows(int count)
      {
         rows = new ArrayList<>(count);
         for( int index = 0; index < count; index++ ) rows.add(new Row(index + 1, "Запись"));
      }

      @Override public int size() { return rows.size(); }
      @Override public Row get(int index) {
         if( ++reads > 1_024 ) throw new AssertionError("Обнаружен обход или копирование записей");
         return rows.get(index);
      }
      @Override public Row set(int index, Row row) { return rows.set(index, row); }
      @Override public void add(int index, Row row) { rows.add(index, row); }
      @Override public Row remove(int index) { return rows.remove(index); }
      @Override public Object[] toArray() { throw new AssertionError("Копирование записей запрещено"); }
      @Override public <T> T[] toArray(T[] target) { throw new AssertionError("Копирование записей запрещено"); }
   }

   private static final class MutableRow
   {
      String name;
      MutableRow(String name) { this.name = name; }
   }

   private static final class Fixture implements AutoCloseable
   {
      final TrackedDataSet<Row> dataSet = dataSet();
      final TableView<Row> table = new TableView<>(FXCollections.observableArrayList(new Row(9, "Прежние данные")));
      final TableColumn<Row, String> name = new TableColumn<>("Название");
      final ForeDataSetAdapter<Row> adapter;

      Fixture()
      {
         name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
         table.getColumns().add(name);
         adapter = ForeDataSetAdapter.bind(dataSet, table);
      }

      void sort(TableColumn.SortType direction)
      {
         name.setSortType(direction);
         table.getSortOrder().setAll(List.of(name));
         table.sort();
      }

      List<Long> ids() { return table.getItems().stream().map(Row::id).toList(); }
      @Override public void close() { adapter.close(); }
   }

   private static final class TrackedDataSet<T> extends ArrayDataSet<T>
   {
      final Set<IDataSetRowListener<T>> rowsListeners = Collections.newSetFromMap(new IdentityHashMap<>());
      final Set<IDataSetNavigationListener<T>> navigationListeners = Collections.newSetFromMap(new IdentityHashMap<>());
      final Set<IDataSetListener> dataSetListeners = Collections.newSetFromMap(new IdentityHashMap<>());
      int closeCalls;
      boolean failNavigation;

      TrackedDataSet(Class<T> type, List<T> rows) { super(type, new ArrayList<>(rows), false); }
      @Override public void addRowListener(IDataSetRowListener<T> listener) {
         super.addRowListener(listener); rowsListeners.add(listener);
      }
      @Override public void removeRowListener(IDataSetRowListener<T> listener) {
         super.removeRowListener(listener); rowsListeners.remove(listener);
      }
      @Override public void addNavigationListener(IDataSetNavigationListener<T> listener) {
         if( failNavigation ) throw new IllegalStateException("Проверка отката привязки");
         super.addNavigationListener(listener); navigationListeners.add(listener);
      }
      @Override public void removeNavigationListener(IDataSetNavigationListener<T> listener) {
         super.removeNavigationListener(listener); navigationListeners.remove(listener);
      }
      @Override public void addDataSetListener(IDataSetListener listener) {
         super.addDataSetListener(listener); dataSetListeners.add(listener);
      }
      @Override public void removeDataSetListener(IDataSetListener listener) {
         super.removeDataSetListener(listener); dataSetListeners.remove(listener);
      }
      void refreshCurrent() {
         final T row = getCurrentRow();
         fireRowEvent(DataSetRowEvent.RowOperationEnum.REFRESH, row, row, getCurrentRowNum(), 1);
      }
      @Override public void close() throws Exception {
         closeCalls++;
         fireDataSetEvent(DataSetEvent.DataSetEventType.CLOSE, true);
         super.close();
         fireDataSetEvent(DataSetEvent.DataSetEventType.CLOSE, false);
      }
   }

   private static final class PageReader implements IDataReader<Row>
   {
      final List<Row> rows;
      int offset;
      PageReader(List<Row> rows) { this.rows = rows; }
      @Override public boolean isEOF() { return offset == rows.size(); }
      @Override public List<Row> getNextPage(int count) {
         final int end = count < 0 ? rows.size() : Math.min(rows.size(), offset + count);
         final var page = rows.subList(offset, end);
         offset = end;
         return page;
      }
      @Override public void close() { offset = rows.size(); }
   }
}
