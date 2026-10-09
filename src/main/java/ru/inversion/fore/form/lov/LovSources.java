package ru.inversion.fore.form.lov;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Явный реестр разрешённых источников. В XML нет паролей, имён Java-классов и скриптов. */
public final class LovSources
{
   private final Map<String, LovDataSource> providers = new HashMap<>();
   private final Map<String, DataSource> databases = new HashMap<>();

   public LovSources provider(String name, LovDataSource provider)
   {
      LovDefinition.requireName(name, "provider");
      if( providers.putIfAbsent(name, Objects.requireNonNull(provider)) != null )
         throw new IllegalArgumentException("Источник уже зарегистрирован: " + name);
      return this;
   }

   public LovSources database(String name, DataSource database)
   {
      LovDefinition.requireName(name, "database");
      if( databases.putIfAbsent(name, Objects.requireNonNull(database)) != null )
         throw new IllegalArgumentException("База уже зарегистрирована: " + name);
      return this;
   }

   public LovDataSource resolve(LovDefinition definition)
   {
      return switch( definition.source() )
      {
         case LovDefinition.StaticSource source -> request -> {
            var rows = new ArrayList<LovRow>();
            for( LovRow row : source.rows() )
            {
               if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
               if( request.matches(row) )
               {
                  if( rows.size() == request.maxRows() ) return new LovDataSource.Result(rows, false);
                  rows.add(row);
               }
            }
            return new LovDataSource.Result(rows, true);
         };
         case LovDefinition.ProviderSource source -> {
            LovDataSource provider = providers.get(source.ref());
            if( provider == null ) throw new IllegalArgumentException("Неизвестный источник: " + source.ref());
            yield provider;
         }
         case LovDefinition.SqlSource source -> {
            DataSource database = databases.get(source.ref());
            if( database == null ) throw new IllegalArgumentException("Неизвестная база: " + source.ref());
            yield request -> query(database, source, request);
         }
      };
   }

   private static LovDataSource.Result query(DataSource database, LovDefinition.SqlSource source,
                                             LovDataSource.Request request) throws Exception
   {
      // Соединение принадлежит запросу; закрытие возвращает его в пул DataSource.
      try( var connection = database.getConnection();
           var statement = connection.prepareStatement(source.query(), ResultSet.TYPE_FORWARD_ONLY,
                                                       ResultSet.CONCUR_READ_ONLY) )
      {
         statement.setFetchSize(source.fetchSize());
         statement.setMaxRows(request.maxRows() + 1);
         statement.setQueryTimeout(source.timeoutSeconds());
         for( int i = 0; i < source.binds().size(); i++ )
         {
            String name = source.binds().get(i);
            Object value = switch( name )
            {
               case "$pattern" -> request.pattern();
               case "$text" -> request.text();
               case "$limit" -> request.maxRows() + 1;
               default -> {
                  if( !request.parameters().containsKey(name) )
                     throw new IllegalArgumentException("Не передан параметр LOV: " + name);
                  yield request.parameters().get(name);
               }
            };
            statement.setObject(i + 1, value);
         }
         var rows = new ArrayList<LovRow>();
         try( var result = statement.executeQuery() )
         {
            while( result.next() )
            {
               if( Thread.currentThread().isInterrupted() ) throw new InterruptedException();
               if( rows.size() == request.maxRows() ) return new LovDataSource.Result(rows, false);
               var values = new LinkedHashMap<String, Object>();
               for( var column : request.definition().columns() )
                  values.put(column.name(), column.convert(result.getObject(column.name())));
               rows.add(new LovRow(values));
            }
         }
         return new LovDataSource.Result(rows, true);
      }
   }
}
