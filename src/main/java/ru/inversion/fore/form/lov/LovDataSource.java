package ru.inversion.fore.form.lov;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Источник выполняется вне потока JavaFX и возвращает одну ограниченную выборку. */
@FunctionalInterface
public interface LovDataSource
{
   Result fetch(Request request) throws Exception;

   record Request(LovDefinition definition, String text, LovDefinition.MatchMode mode,
                  Map<String, Object> parameters)
   {
      public Request
      {
         Objects.requireNonNull(definition, "definition");
         Objects.requireNonNull(mode, "mode");
         text = text == null ? "" : text;
         parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
      }

      public int maxRows() { return definition.search().maxRows(); }

      public String normalized(String value)
      {
         return definition.search().caseSensitive() ? value : value.toUpperCase(Locale.ROOT);
      }

      /** Пользовательские % и _ остаются обычными символами. В SQL нужен ESCAPE '!'. */
      public String pattern()
      {
         String escaped = normalized(text).replace("!", "!!").replace("%", "!%").replace("_", "!_");
         return switch( mode )
         {
            case EXACT -> escaped;
            case PREFIX -> escaped + "%";
            case CONTAINS -> "%" + escaped + "%";
         };
      }

      public boolean matches(LovRow row)
      {
         Object value = row.get(definition.searchColumn().name());
         if( value == null ) return text.isEmpty() && mode != LovDefinition.MatchMode.EXACT;
         String candidate = normalized(LovDefinition.ValueType.text(value));
         String expected = normalized(text);
         return switch( mode )
         {
            case EXACT -> candidate.equals(expected);
            case PREFIX -> candidate.startsWith(expected);
            case CONTAINS -> candidate.contains(expected);
         };
      }
   }

   /**
    * Источник передаёт владение списком: после возврата менять его и строки запрещено.
    * Обёртка не копирует список. complete=false означает наличие ещё не показанных строк.
    */
   record Result(List<LovRow> rows, boolean complete)
   {
      public Result
      {
         rows = Collections.unmodifiableList(Objects.requireNonNull(rows, "rows"));
      }
   }
}
