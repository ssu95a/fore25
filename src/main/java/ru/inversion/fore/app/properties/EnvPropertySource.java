package ru.inversion.fore.app.properties;

import ru.inversion.utils.S;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Источник свойств из переменных окружения. */
public final class EnvPropertySource implements PropertySource {

   @Override
   public String name() {
      return "environment";
   }

   @Override
   public Map<String, Object> load(Collection<String> names) {

      if( names == null || names.isEmpty() )
          return Map.of();

      final Map<String, Object> result = new LinkedHashMap<>();

      for( String name : names )
      {
         if(S.isNullOrEmpty(name) )
             continue;

         final String value = System.getenv(name);

         if( value != null )
             result.put(name, value);
      }

      return result;
   }

   @Override
   public Object get(String key) {
      return key == null ? null : System.getenv(key);
   }
}