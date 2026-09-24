package ru.inversion.fore.app.properties.impl;

import ru.inversion.fore.app.properties.PropertySource;

/** Источник свойств из переменных окружения. */
public final class EnvPropertySource implements PropertySource {

   @Override
   public String name() {
      return "environment";
   }

   @Override
   public Object get(String key) {
      return key == null ? null : System.getenv(key);
   }
}