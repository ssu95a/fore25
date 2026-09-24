package ru.inversion.fore.app.properties;

import ru.inversion.utils.S;

import java.util.Collection;
import java.util.Map;

/** */
public interface PropertySource extends AutoCloseable {

   String name( );

   default void load( Collection<String> names, Map<String, Object> loadTo )
   {
      if( names == null || names.isEmpty() || loadTo == null )
          return;

      for( String name : names )
      {
         if( S.isNullOrEmpty(name) || loadTo.containsKey(name) )
             continue;

         final Object value = get( name );

         if( value != null )
             loadTo.put( name, value );
      }
   }

   /**
    * Получает одно свойство непосредственно из источника.
    */
   Object get(String name);

   /** */
   @Override
   default void close() {};
}