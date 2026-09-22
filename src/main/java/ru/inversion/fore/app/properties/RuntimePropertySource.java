package ru.inversion.fore.app.properties;

import ru.inversion.utils.S;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Изменяемый runtime-источник свойств. */
public final class RuntimePropertySource implements PropertySource {

   private final Map<String, Object> properties = new ConcurrentHashMap<>();

   @Override
   public String name() {
      return "runtime";
   }

   @Override
   public Map<String, Object> load(Collection<String> names) {

      if( names == null || names.isEmpty() )
          return Map.of();

      final Map<String, Object> result = new LinkedHashMap<>();

      for (String name : names)
      {
         if( S.isNullOrEmpty(name) )
             continue;

         final Object value = properties.get(name);

         if( value != null )
            result.put(name, value);
      }

      return result;
   }

   @Override
   public Object get(String key) { return key == null ? null : properties.get(key); }

   public void apply(PropertyPatch patch) {

      if( patch != null )
          properties.putAll(patch.values());
   }

   public void set(String key, Object value) {

      if( key == null )
          return;

      if( value == null )
         properties.remove(key);
      else
         properties.put(key, value);

   }

   public void remove(String key)
   {
      if( key != null )
         properties.remove(key);
   }

   /** */
   public void clear() {
      properties.clear();
   }
}