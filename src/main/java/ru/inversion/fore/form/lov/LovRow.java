package ru.inversion.fore.form.lov;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Одна строка справочника. NULL отличается от отсутствующей колонки. */
public record LovRow(Map<String, Object> values)
{
   public LovRow
   {
      values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
   }

   public Object get(String column)
   {
      if( !values.containsKey(column) )
         throw new IllegalArgumentException("В результате нет колонки: " + column);
      return values.get(column);
   }

   /** Подготавливает все возвращаемые значения до изменения полей формы. */
   public Map<String, Object> returns(LovDefinition definition)
   {
      var result = new LinkedHashMap<String, Object>();
      for( var column : definition.columns() )
         if( !column.returnTo().isEmpty() )
            result.put(column.returnTo(), column.type().convert(get(column.name())));
      return Collections.unmodifiableMap(result);
   }
}
