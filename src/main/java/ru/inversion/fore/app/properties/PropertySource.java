package ru.inversion.fore.app.properties;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** */
public interface PropertySource extends AutoCloseable {

   String name( );

   Map<String, Object> load( Collection<String> names );

   /**
    * Получает одно свойство непосредственно из источника.
    */
   default Object get(String name) {
      return load(List.of(name)).get(name);
   }

   /** */
   @Override
   default void close() {};
}