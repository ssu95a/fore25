package ru.inversion.fore.app.properties;

import java.util.Map;

public record PropertySnapshot( PropertyType type, Map<String, Object> values)
{

   public PropertySnapshot { values = Map.copyOf(values); }

   public <T> T get(String name) { return (T)values.get(name); }

   public String getString(String name)
   {
      var value = values.get(name);
      return value == null ? null : value.toString();
   }
}