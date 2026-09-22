package ru.inversion.fore.app.properties;

import java.util.Collection;

public interface AppProperties {


      PropertySnapshot load( Collection<String> names );

      <T> T get(String name);

      default <T> T get(String name, T defaultValue)
      {
         T value = get(name);
         return value != null ? value : defaultValue;
      }

      String getString(String name);

      default String getString(String name, String defaultValue)
      {
         String value = getString(name);
         return value != null ? value : defaultValue;
      }

      boolean contains(String name);

      void apply(PropertyPatch patch);
}