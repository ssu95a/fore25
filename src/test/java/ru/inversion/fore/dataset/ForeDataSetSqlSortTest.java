package ru.inversion.fore.dataset;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.db.entity.ProxyFor;
import ru.inversion.fore.ForeException;
import ru.inversion.fore.FxTestSupport;

import javax.persistence.Column;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет передачу сортировки в SQL-набор и метаданные JInvCommon без TaskContext и соединения с БД. */
class ForeDataSetSqlSortTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void sendsSqlColumnNamesAndReloadsRowsAndCursor() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var name = column("name");
         table.getColumns().add(name);
         sql.rows.setCurrentRowNum(1);
         final var oldRow = sql.rows.getCurrentRow();
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            assertTrue(name.isSortable());
            assertTrue(sql.commands.isEmpty(), "Привязка не должна сама выполнять запрос");
            table.getSortOrder().add(name);
            assertEquals(List.of("ORDER CNNAME ASC", "EXECUTE"), sql.commands);
            assertEquals(0, table.getSelectionModel().getSelectedIndex());
            assertSame(sql.rows.getCurrentRow(), adapter.getCurrentRow());
            assertNotSame(oldRow, adapter.getCurrentRow(), "Записи должны прийти из повторного запроса");
            assertSame(sql.rows.getRow(0), table.getItems().get(0));
            assertEquals(List.of(name), table.getSortOrder());

            sql.commands.clear();
            name.setSortType(TableColumn.SortType.DESCENDING);
            assertEquals(List.of("ORDER CNNAME DESC", "EXECUTE"), sql.commands);
            assertEquals(List.of(name), table.getSortOrder());
         }
      });
   }

   @Test
   void sendsSeveralColumnsAndResetsOrderByWhenHeadersAreCleared() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var name = column("name");
         final var id = column("id");
         id.setSortType(TableColumn.SortType.DESCENDING);
         table.getColumns().setAll(List.of(name, id));
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            table.getSortOrder().setAll(List.of(name, id));
            assertEquals(List.of("ORDER CNNAME ASC, ID DESC", "EXECUTE"), sql.commands);
            sql.commands.clear();
            table.getSortOrder().clear();
            assertNull(sql.orderBy);
            assertEquals(List.of("ORDER null", "EXECUTE"), sql.commands);
         }
      });
   }

   @Test
   void usesMetadataOrderByForEveryExpression() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var priority = column("priority");
         priority.setSortType(TableColumn.SortType.DESCENDING);
         table.getColumns().add(priority);
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            table.getSortOrder().add(priority);
            assertEquals(List.of("ORDER COALESCE(NPRIORITY, 0) DESC, ID DESC", "EXECUTE"), sql.commands);
         }
      });
   }

   @Test
   void mapsAnExplicitColumnIdWhenTheCellFactoryIsALambda() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var name = new TableColumn<SqlRow, String>("Название");
         name.setId("cnname");
         name.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getName()));
         table.getColumns().add(name);
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            table.getSortOrder().add(name);
            assertEquals(List.of("ORDER CNNAME ASC", "EXECUTE"), sql.commands);
         }
      });
   }

   @Test
   void disablesTransientAndUnmappedColumnsEvenWhenAddedToSortOrderProgrammatically() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var transientColumn = column("label");
         final var proxyColumn = column("displayName");
         final var unmapped = column("unknown");
         table.getColumns().setAll(List.of(transientColumn, proxyColumn, unmapped));
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            for( final var column : table.getColumns() )
            {
               assertFalse(column.isSortable());
               table.getSortOrder().add(column);
               assertTrue(table.getSortOrder().isEmpty());
            }
            assertTrue(sql.commands.isEmpty());
         }
      });
   }

   @Test
   void configuresTransientColumnsAddedInsideAGroupAfterBinding() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var group = new TableColumn<SqlRow, Object>("Группа");
         table.getColumns().add(group);
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            final var transientColumn = column("label");
            group.getColumns().add(transientColumn);
            assertFalse(transientColumn.isSortable());
            adapter.close();
            final var detached = column("label");
            group.getColumns().add(detached);
            assertTrue(detached.isSortable(), "После close подписки на столбцы должны быть сняты");
         }
      });
   }

   @Test
   void dataSetNotificationsRefreshAndCloseNeverRepeatTheSqlQuery() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var name = column("name");
         table.getColumns().add(name);
         final var adapter = ForeDataSetAdapter.bind(sql.dataSet, table);
         try
         {
            table.getSortOrder().add(name);
            sql.commands.clear();
            sql.rows.insertRow(new SqlRow(3, "Добавлена"), ru.inversion.dataset.IDataSet.InsertRowModeEnum.LAST, false);
            sql.rows.setCurrentRowNum(2);
            sql.rows.updateCurrentRow(new SqlRow(3, "Обновлена"));
            sql.rows.removeCurrentRow();
            sql.rows.executed();
            adapter.refresh();
            assertEquals(List.of(name), table.getSortOrder());
            adapter.close();
            assertTrue(table.getItems().isEmpty());
            table.sort();
            assertTrue(sql.commands.isEmpty());
         }
         finally { adapter.close(); }
      });
   }

   @Test
   void exposesQueryFailureAndStillSynchronizesSubsequentNavigation() throws Exception
   {
      FxTestSupport.run(() -> {
         final var sql = new SqlSource();
         final var table = new TableView<SqlRow>();
         final var name = column("name");
         table.getColumns().add(name);
         try( var adapter = ForeDataSetAdapter.bind(sql.dataSet, table) )
         {
            sql.failure = new DataSetException("Проверка ошибки запроса");
            final var error = new AtomicReference<Throwable>();
            final var thread = Thread.currentThread();
            final var previousHandler = thread.getUncaughtExceptionHandler();
            try
            {
               thread.setUncaughtExceptionHandler((failedThread, failure) -> error.set(failure));
               table.getSortOrder().add(name);
            }
            finally { thread.setUncaughtExceptionHandler(previousHandler); }
            assertInstanceOf(ForeException.class, error.get());
            assertSame(sql.failure, error.get().getCause());
            assertNull(sql.orderBy);
            assertTrue(table.getSortOrder().isEmpty());
            sql.rows.setCurrentRowNum(1);
            assertEquals(1, table.getSelectionModel().getSelectedIndex());
            assertSame(sql.rows.getCurrentRow(), adapter.getCurrentRow());
            assertEquals(List.of(sql.rows.getCurrentRow()), table.getSelectionModel().getSelectedItems());
            sql.failure = null;
            sql.commands.clear();
            table.getSortOrder().add(name);
            assertEquals(List.of("ORDER CNNAME ASC", "EXECUTE"), sql.commands);
         }
      });
   }

   private static TableColumn<SqlRow, Object> column(String property)
   {
      final var column = new TableColumn<SqlRow, Object>(property);
      column.setCellValueFactory(new PropertyValueFactory<>(property));
      return column;
   }

   /** База моделируется ответами на запрос; события и хранение строк обеспечивает настоящий ArrayDataSet. */
   private static final class SqlSource
   {
      final QueryRows rows = new QueryRows();
      final List<String> commands = new ArrayList<>();
      final ISQLDataSet<SqlRow> dataSet;
      String orderBy;
      DataSetException failure;

      @SuppressWarnings("unchecked")
      SqlSource()
      {
         dataSet = (ISQLDataSet<SqlRow>) Proxy.newProxyInstance(ISQLDataSet.class.getClassLoader(),
                 new Class<?>[]{ISQLDataSet.class}, (proxy, method, args) -> {
                    switch( method.getName() )
                    {
                       case "setOrderBy" -> {
                          orderBy = (String) args[0];
                          commands.add("ORDER " + orderBy);
                          return null;
                       }
                       case "getOrderBy" -> { return orderBy; }
                       case "executeQuery" -> {
                          commands.add("EXECUTE");
                          if( failure != null ) throw failure;
                          rows.getRows().clear();
                          rows.getRows().addAll(List.of(new SqlRow(2, "Из БД"), new SqlRow(1, "Повторный запрос")));
                          rows.setCurrentRowNum(0);
                          rows.executed();
                          return null;
                       }
                       case "hashCode" -> { return System.identityHashCode(proxy); }
                       case "equals" -> { return proxy == args[0]; }
                       case "toString" -> { return "Проверочный SQL-набор"; }
                       default -> {
                          if( method.isDefault() ) return InvocationHandler.invokeDefault(proxy, method, args);
                          try { return method.invoke(rows, args); }
                          catch( InvocationTargetException error ) { throw error.getCause(); }
                       }
                    }
                 });
      }
   }

   private static final class QueryRows extends ArrayDataSet<SqlRow>
   {
      QueryRows()
      {
         super(SqlRow.class, new ArrayList<>(List.of(new SqlRow(1, "Первая"), new SqlRow(2, "Вторая"))), false);
      }
      void executed() { fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false); }
   }

   public static final class SqlRow
   {
      private final long id;
      private final String name;

      public SqlRow(long id, String name) { this.id = id; this.name = name; }
      @Column(name = "ID") public long getId() { return id; }
      @Column(name = "CNNAME") public String getName() { return name; }
      @Column(name = "NPRIORITY") @OrderBy("COALESCE(NPRIORITY, 0), ID")
      public int getPriority() { return 0; }
      @Transient @OrderBy("CNNAME") public String getLabel() { return name + id; }
      @Transient @ProxyFor(columnName = "CNNAME") public String getDisplayName() { return name; }
   }
}
