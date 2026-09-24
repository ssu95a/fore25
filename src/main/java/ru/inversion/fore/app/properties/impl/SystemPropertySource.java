package ru.inversion.fore.app.properties.impl;

import ru.inversion.fore.app.properties.PropertySource;

/** Источник свойств JVM System Properties. */
public final class SystemPropertySource implements PropertySource {

   @Override
   public String name( ) {
      return "system";
   }

   @Override
   public Object get(String key) {
      return key == null ? null : System.getProperty(key);
   }
}