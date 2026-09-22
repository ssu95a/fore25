package ru.inversion.fore.app.properties;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Источник свойств JVM System Properties. */
public final class SystemPropertySource implements PropertySource {

   @Override
   public String name( ) {
      return "system";
   }

   @Override
   public Map<String, Object> load(Collection<String> names) {

      if( names == null || names.isEmpty() )
          return Map.of();

      final Map<String, Object> result = new LinkedHashMap<>();

      for( String name : names )
      {
         if(name == null )
            continue;

         final String value = System.getProperty(name);

         if( value != null )
             result.put(name, value);
      }

      return result;
   }

   @Override
   public Object get(String key) {
      return key == null ? null : System.getProperty(key);
   }
}