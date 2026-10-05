package ru.inversion.fore.demo.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Демонстрационный справочник в памяти; формы изменяют его на потоке JavaFX. */
public final class RecordStore
{
   private final List<Row> rows = new ArrayList<>();
   private long nextId = 1;

   public static RecordStore sample()
   {
      final var store = new RecordStore();
      store.save(new Draft(store.nextId++, "Альфа"));
      store.save(new Draft(store.nextId++, "Бета"));
      store.save(new Draft(store.nextId++, "Гамма"));
      return store;
   }

   /** Неизменяемый снимок списка для заполнения таблицы. */
   public List<Row> snapshot()
   {
      return List.copyOf(rows);
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
      for( int index = 0; index < rows.size(); index++ )
         if( rows.get(index).id() == saved.id() )
         {
            rows.set(index, saved);
            return saved;
         }

      rows.add(saved);
      nextId = Math.max(nextId, saved.id() + 1);
      return saved;
   }

   public void delete(long id)
   {
      rows.removeIf(row -> row.id() == id);
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
