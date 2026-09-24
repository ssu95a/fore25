package ru.inversion.fore.app.properties.impl;

import ru.inversion.fore.app.properties.PropertyPatch;
import ru.inversion.fore.app.properties.PropertySource;

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