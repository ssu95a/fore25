package ru.inversion.fore.form.lov;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Неизменяемое описание LOV. Не содержит контролов, соединений и состояния формы. */
public record LovDefinition(String id, String title, String comment, Window window,
                            Behavior behavior, Search search, List<Column> columns, Source source,
                            Binding binding, Appearance appearance)
{
   /** Совместимость с первой версией прототипа. */
   public LovDefinition(String id, String title, String comment, Window window,
                        Behavior behavior, Search search, List<Column> columns, Source source)
   {
      this(id, title, comment, window, behavior, search, columns, source,
            Binding.defaults(), Appearance.defaults());
   }

   public LovDefinition
   {
      requireName(id, "id");
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(window, "window");
      Objects.requireNonNull(behavior, "behavior");
      Objects.requireNonNull(search, "search");
      Objects.requireNonNull(source, "source");
      Objects.requireNonNull(binding, "binding");
      Objects.requireNonNull(appearance, "appearance");
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

   /** Спецификация колонки record group; длина задаётся в символах, ноль означает «без ограничения». */
   public record Column(String name, String title, ValueType type, double width, String returnTo, int length)
   {
      public Column(String name, String title, ValueType type, double width, String returnTo)
      {
         this(name, title, type, width, returnTo, 0);
      }

      public Column
      {
         requireName(name, "column.name");
         Objects.requireNonNull(title, "title");
         Objects.requireNonNull(type, "type");
         if( !Double.isFinite(width) || width < 0 )
            throw new IllegalArgumentException("Ширина колонки не может быть отрицательной");
         if( length < 0 )
            throw new IllegalArgumentException("Длина колонки не может быть отрицательной");
         returnTo = returnTo == null ? "" : returnTo;
      }

      public boolean visible() { return width > 0; }

      public Object convert(Object value)
      {
         Object converted = type.convert(value);
         if( converted != null && length > 0 )
         {
            int count = ValueType.text(converted).codePointCount(0, ValueType.text(converted).length());
            if( count > length )
               throw new IllegalArgumentException("Значение колонки " + name + " длиннее " + length + " символов");
         }
         return converted;
      }
   }

   /** Настройки, относящиеся к элементу формы, который вызывает LOV. */
   public record Binding(Position position, boolean lovButton)
   {
      public Binding { }
      public static Binding defaults() { return new Binding(null, false); }
   }

   public record Position(double x, double y)
   {
      public Position
      {
         if( !Double.isFinite(x) || !Double.isFinite(y) )
            throw new IllegalArgumentException("Координаты LOV должны быть конечными числами");
      }
   }

   /** Явные параметры оформления; остальные свойства темы остаются CSS приложения. */
   public record Appearance(String visualAttribute, String fontName, double fontSize,
                            String fontWeight, String fontStyle, String foregroundColor,
                            String backgroundColor, String rowLineColor)
   {
      public Appearance
      {
         visualAttribute = empty(visualAttribute);
         fontName = empty(fontName);
         fontWeight = empty(fontWeight);
         fontStyle = empty(fontStyle);
         foregroundColor = color(foregroundColor, "foreground-color");
         backgroundColor = color(backgroundColor, "background-color");
         rowLineColor = color(rowLineColor, "row-line-color");
         if( !Double.isFinite(fontSize) || fontSize < 0 )
            throw new IllegalArgumentException("Размер шрифта не может быть отрицательным");
         if( !fontName.isEmpty() && !fontName.matches("[\\p{L}\\p{N} ._-]+") )
            throw new IllegalArgumentException("Некорректное имя шрифта");
         if( !fontWeight.isEmpty() && !fontWeight.equalsIgnoreCase("normal") && !fontWeight.equalsIgnoreCase("bold") )
            throw new IllegalArgumentException("font-weight должен быть normal или bold");
         if( !fontStyle.isEmpty() && !fontStyle.equalsIgnoreCase("normal") && !fontStyle.equalsIgnoreCase("italic") )
            throw new IllegalArgumentException("font-style должен быть normal или italic");
      }

      public static Appearance defaults()
      {
         return new Appearance("", "", 0, "", "", "", "", "");
      }

      private static String empty(String value) { return value == null ? "" : value.strip(); }

      private static String color(String value, String name)
      {
         value = empty(value);
         if( value.isEmpty() ) return value;
         if( !value.matches("(?i)(#[0-9a-f]{3,8}|rgba?\\s*\\([^)]*\\)|[a-z]+)") )
            throw new IllegalArgumentException("Некорректный цвет " + name + ": " + value);
         return value;
      }
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
