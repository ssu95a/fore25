package ru.inversion.fore.app.properties;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Источник именованных аргументов приложения. */
public final class NamedArgumentsSource implements PropertySource {

   private final Map<String, String> arguments;

   public NamedArgumentsSource(Map<String, String> arguments) {
      this.arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
   }

   @Override
   public String name() {
      return "arguments";
   }

   @Override
   public Map<String, Object> load( Collection<String> names ) {

      if( names == null || names.isEmpty() )
          return Map.of();

      final Map<String, Object> result = new LinkedHashMap<>();

      for (String name : names)
      {
         if( name == null )
             continue;

         final String value = arguments.get(name);

         if( value != null )
             result.put(name, value);

      }

      return result;
   }

   @Override
   public Object get(String key) {
      return key == null ? null : arguments.get(key);
   }
}