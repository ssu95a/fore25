package ru.inversion.fore.dataset;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.IDataSet;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.control.ForeTableView;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Проверяет совместную работу GUI-контрола и адаптера с настоящим хранилищем JInvCommon. */
class ForeTableDataSetBindingTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void usesTheDataSetStorageAndFollowsItsCursorThroughResetAll() throws Exception
   {
      FxTestSupport.run(() -> {
         final var rows = new ArrayList<>(List.of(new Row(1, "Альфа"), new Row(2, "Бета")));
         final var dataSet = new ArrayDataSet<>(Row.class, rows, false);
         dataSet.setCurrentRowNum(1);
         final var table = new ForeTableView<Row>();
         try( var adapter = ForeDataSetAdapter.bind(dataSet, table) )
         {
            final var items = table.getItems();
            assertSame(rows, dataSet.getRows());
            assertSame(dataSet, adapter.getDataSet());
            assertSame(table, adapter.getTable());
            assertTrue(table.itemsProperty().isBound());
            assertSame(dataSet.getCurrentRow(), table.getSelectionModel().getSelectedItem());

            dataSet.insertRow(new Row(3, "Гамма"), IDataSet.InsertRowModeEnum.FIRST, false);
            assertSame(items, table.getItems());
            assertEquals(dataSet.getCurrentRowNum(), table.getSelectionModel().getSelectedIndex());
            assertSame(dataSet.getCurrentRow(), table.getSelectionModel().getSelectedItem());

            final var updated = new Row(2, "Обновлённая запись");
            dataSet.updateCurrentRow(updated);
            assertSame(updated, items.get(dataSet.getCurrentRowNum()));
            assertSame(updated, table.getSelectionModel().getSelectedItem());
            assertSame(items, table.getItems());

            dataSet.clear();
            assertTrue(items.isEmpty());
            assertNull(table.getSelectionModel().getSelectedItem());
            dataSet.insertRows(List.of(new Row(4, "Дельта"), new Row(5, "Эпсилон")),
                    IDataSet.InsertRowModeEnum.LAST, false);
            table.getSelectionModel().selectLast();
            assertEquals(1, dataSet.getCurrentRowNum());
            assertSame(dataSet.getCurrentRow(), adapter.getCurrentRow());
            assertSame(items, table.getItems());
         }
         assertFalse(table.itemsProperty().isBound());
         assertTrue(table.getItems().isEmpty());
         assertEquals(2, dataSet.getLoadedRowCount());
      });
   }

   private record Row(long id, String name) {}
}
