package ru.inversion.fore.demo.table;

import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.IDataSet;

import java.util.List;
import java.util.Objects;

/** Демонстрационный справочник в памяти; формы изменяют его на потоке JavaFX. */
public final class RecordStore
{
   private final ArrayDataSet<Row> dataSet = new ArrayDataSet<>(Row.class);
   private long nextId = 1;

   public static RecordStore sample()
   {
      final var store = new RecordStore();
      store.dataSet.insertRows(List.of(new Row(1, "Альфа"), new Row(2, "Бета"), new Row(3, "Гамма")),
              IDataSet.InsertRowModeEnum.LAST, false);
      store.nextId = 4;
      return store;
   }

   /** Набор данных остаётся у владельца хранилища; адаптер таблицы подписывается на его события. */
   public IDataSet<Row> getDataSet() { return dataSet; }

   /** Неизменяемый снимок сохранённых записей. */
   public List<Row> snapshot()
   {
      return List.copyOf(dataSet.getRows());
   }

   /** Идентификатор выделяется при открытии новой записи; отмена может оставить пропуск. */
   public Draft newDraft()
   {
      return new Draft(nextId++, "");
   }

   /** Изменения черновика не затрагивают сохранённую запись. */
   public Draft editDraft(Row row)
   {
      Objects.requireNonNull(row, "row");
      return new Draft(row.id(), row.name());
   }

   public Row save(Draft draft)
   {
      Objects.requireNonNull(draft, "draft");
      final String name = draft.name().strip();
      if( name.isEmpty() || name.length() > 120 )
         throw new IllegalArgumentException("Название должно содержать от 1 до 120 символов");

      final var saved = new Row(draft.id(), name);
      for( int index = 0; index < dataSet.getLoadedRowCount(); index++ )
         if( dataSet.getRow(index).id() == saved.id() )
         {
            dataSet.setCurrentRowNum(index);
            dataSet.updateCurrentRow(saved);
            return saved;
         }

      dataSet.insertRow(saved, IDataSet.InsertRowModeEnum.LAST, false);
      // Устанавливаем фактический индекс добавленной строки независимо от прежнего курсора.
      dataSet.setCurrentRowNum(dataSet.getLoadedRowCount() - 1);
      nextId = Math.max(nextId, saved.id() + 1);
      return saved;
   }

   public void delete(long id)
   {
      for( int index = 0; index < dataSet.getLoadedRowCount(); index++ )
         if( dataSet.getRow(index).id() == id )
         {
            dataSet.setCurrentRowNum(index);
            dataSet.removeCurrentRow();
            return;
         }
   }

   /** Строка таблицы неизменяема; сохранение заменяет её новым экземпляром с тем же ID. */
   public record Row(long id, String name)
   {
      public Row
      {
         if( id <= 0 )
            throw new IllegalArgumentException("Идентификатор должен быть положительным");
         Objects.requireNonNull(name, "name");
      }
   }

   /** Данные, передаваемые редактору и возвращаемые через FormResult. */
   public static final class Draft
   {
      private final long id;
      private String name;

      private Draft(long id, String name)
      {
         this.id = id;
         this.name = Objects.requireNonNull(name, "name");
      }

      public long id() { return id; }
      public String name() { return name; }
      public void setName(String name) { this.name = Objects.requireNonNull(name, "name"); }
   }
}
