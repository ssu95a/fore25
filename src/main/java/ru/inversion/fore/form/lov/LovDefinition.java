package ru.inversion.fore.form.lov;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Неизменяемое описание LOV. Не содержит контролов, соединений и состояния формы. */
public record LovDefinition(String id, String title, String comment, Window window,
                            Behavior behavior, Search search, List<Column> columns, Source source)
{
   public LovDefinition
   {
      requireName(id, "id");
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(window, "window");
      Objects.requireNonNull(behavior, "behavior");
      Objects.requireNonNull(search, "search");
      Objects.requireNonNull(source, "source");
      comment = comment == null ? "" : comment;
      columns = List.copyOf(columns);
      if( columns.stream().noneMatch(Column::visible) )
         throw new IllegalArgumentException("LOV должен иметь хотя бы одну видимую колонку");
      var names = new HashSet<String>();
      var targets = new HashSet<String>();
      for( Column column : columns )
      {
         if( !names.add(column.name()) )
            throw new IllegalArgumentException("Повтор колонки: " + column.name());
         if( !column.returnTo().isEmpty() && !targets.add(column.returnTo()) )
            throw new IllegalArgumentException("Повтор получателя: " + column.returnTo());
      }
      if( behavior.validateFromList() && columns.stream().filter(Column::visible)
            .findFirst().orElseThrow().returnTo().isEmpty() )
         throw new IllegalArgumentException("Для проверки ввода первая видимая колонка должна иметь return-to");
   }

   public Column searchColumn()
   {
      return columns.stream().filter(Column::visible).findFirst().orElseThrow();
   }

   static void requireName(String value, String name)
   {
      if( value == null || value.isBlank() )
         throw new IllegalArgumentException("Не задано значение: " + name);
   }

   public enum Direction { INHERIT, LEFT_TO_RIGHT, RIGHT_TO_LEFT }
   public enum MatchMode { PREFIX, CONTAINS, EXACT }

   public record Window(double width, double height, Double x, Double y,
                        boolean automaticPosition, boolean automaticColumnWidth,
                        Direction direction, String styleClass)
   {
      public Window
      {
         if( !Double.isFinite(width) || !Double.isFinite(height) || width < 240 || height < 180 )
            throw new IllegalArgumentException("Размер окна LOV должен быть не меньше 240 × 180");
         if( (x == null) != (y == null) || x != null && (!Double.isFinite(x) || !Double.isFinite(y)) )
            throw new IllegalArgumentException("Координаты x и y задаются парой конечных чисел");
         Objects.requireNonNull(direction, "direction");
         styleClass = styleClass == null ? "" : styleClass;
      }
   }

   public record Behavior(boolean autoDisplay, boolean autoRefresh, boolean autoSelect,
                          boolean autoSkip, boolean filterBeforeDisplay,
                          boolean validateFromList, String key)
   {
      public Behavior { requireName(key, "key"); }
   }

   public record Search(MatchMode mode, boolean caseSensitive, int minLength, int maxRows)
   {
      public Search
      {
         Objects.requireNonNull(mode, "mode");
         if( minLength < 0 || maxRows < 1 || maxRows > 10_000 )
            throw new IllegalArgumentException("min-length >= 0; max-rows от 1 до 10000");
      }
   }

   public record Column(String name, String title, ValueType type, double width, String returnTo)
   {
      public Column
      {
         requireName(name, "column.name");
         Objects.requireNonNull(title, "title");
         Objects.requireNonNull(type, "type");
         if( !Double.isFinite(width) || width < 0 )
            throw new IllegalArgumentException("Ширина колонки не может быть отрицательной");
         returnTo = returnTo == null ? "" : returnTo;
      }

      public boolean visible() { return width > 0; }
   }

   /** Набор переносимых типов; дата не зависит от часового пояса интерфейса. */
   public enum ValueType
   {
      STRING, LONG, DECIMAL, DATE, DATETIME, BOOLEAN;

      public Object convert(Object value)
      {
         if( value == null ) return null;
         return switch( this )
         {
            case STRING -> value.toString();
            case LONG -> new BigDecimal(value.toString()).longValueExact();
            case DECIMAL -> value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
            case DATE -> value instanceof LocalDate date ? date :
                  value instanceof java.sql.Date date ? date.toLocalDate() : LocalDate.parse(value.toString());
            case DATETIME -> value instanceof LocalDateTime date ? date :
                  value instanceof java.sql.Timestamp stamp ? stamp.toLocalDateTime() : LocalDateTime.parse(value.toString());
            case BOOLEAN -> {
               if( value instanceof Boolean flag ) yield flag;
               if( "true".equals(value.toString()) || "1".equals(value.toString()) ) yield true;
               if( "false".equals(value.toString()) || "0".equals(value.toString()) ) yield false;
               throw new IllegalArgumentException("Некорректное логическое значение: " + value);
            }
         };
      }

      public static String text(Object value)
      {
         return value == null ? "" : value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
      }
   }

   public sealed interface Source permits StaticSource, ProviderSource, SqlSource {}

   public record StaticSource(List<LovRow> rows) implements Source
   {
      public StaticSource { rows = List.copyOf(rows); }
   }

   public record ProviderSource(String ref) implements Source
   {
      public ProviderSource { requireName(ref, "provider.ref"); }
   }

   /** Параметры соответствуют знакам ? в запросе, в том же порядке. */
   public record SqlSource(String ref, String query, List<String> binds, int fetchSize,
                           int timeoutSeconds) implements Source
   {
      public SqlSource
      {
         requireName(ref, "sql.ref");
         requireName(query, "query");
         binds = List.copyOf(binds);
         binds.forEach(name -> requireName(name, "bind"));
         if( fetchSize < 1 || timeoutSeconds < 1 )
            throw new IllegalArgumentException("fetch-size и timeout-seconds должны быть положительными");
      }
   }
}
