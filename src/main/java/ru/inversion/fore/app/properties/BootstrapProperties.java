package ru.inversion.fore.app.properties;

import ru.inversion.fore.app.properties.impl.*;
import ru.inversion.utils.S;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Bootstrap-свойства приложения.
 *
 * <p>Свойства собираются из набора источников в порядке их приоритета.
 * Первый источник, содержащий запрошенное свойство, определяет его значение.</p>
 *
 * <p>Стандартный порядок источников:</p>
 * <ol>
 *     <li>runtime properties</li>
 *     <li>JVM system properties</li>
 *     <li>named application arguments</li>
 *     <li>properties file</li>
 *     <li>environment variables</li>
 *     <li>user preferences</li>
 *     <li>system preferences</li>
 * </ol>
 */
public final class BootstrapProperties implements ForeProperties, AutoCloseable {

   private static final String FILE_PROPERTIES = "file_properties";

   private final RuntimePropertySource runtimeSource;

   private final List<PropertySource>  sources;


   /**
    * Уже загруженное effective-состояние.
    * <p>
    * get()/getString()/contains() никогда сами не обращаются к источникам.
    */
   private final LinkedHashMap<String,Object> properties = new LinkedHashMap<>();

   private BootstrapProperties( RuntimePropertySource runtimeSource, List<PropertySource> sources) {
      this.runtimeSource = Objects.requireNonNull(runtimeSource);
      this.sources       = List.copyOf(sources);
   }


   /**
    * Создаёт стандартный набор bootstrap-свойств Fore.
    *
    * @param applicationClass класс приложения
    * @param arguments named arguments приложения
    */
   public static BootstrapProperties create( Class<?> applicationClass, Map<String, String> arguments)
   {
      Objects.requireNonNull(applicationClass);

      final Map<String, String> args = arguments == null ? Map.of() : Map.copyOf(arguments);

      final String applicationId = applicationId(applicationClass);

      final RuntimePropertySource runtime = new RuntimePropertySource();

      final Path propertyFile = resolvePropertyFile(applicationId, args);

      return new BootstrapProperties (
         runtime,
         List.of (
            runtime,
            new SystemPropertySource(),
            new NamedArgumentsSource(args),
            new FilePropertySource(propertyFile),
            new EnvPropertySource(),
            new PreferencesPropertySource( "preferences-user",
                    Preferences.userRoot().node(applicationId) ),
            new PreferencesPropertySource( "preferences-system",
                    Preferences.systemRoot().node(applicationId) )
         )
      );
   }


   /** */
   private static String applicationId( Class<?> applicationClass )
   {
      final String packageName = applicationClass.getPackageName();
      final String prefix      = "ru.inversion.";

      if( !packageName.startsWith(prefix) )
          throw new IllegalArgumentException( "Application class must be in ru.inversion package: " + applicationClass.getName() );

      final String name = packageName.substring(prefix.length());

      final int dot = name.indexOf('.');

      return dot < 0 ? name : name.substring(0, dot);
   }


   /**
    * Загружает указанные свойства.
    *
    * <p>Каждый следующий источник получает только те имена,
    * которые не были найдены предыдущими источниками.</p>
    *
    * <p>Ранее загруженное значение запрошенного свойства удаляется,
    * если при новой загрузке оно больше не найдено ни в одном источнике.</p>
    */
   @Override
   public PropertySnapshot load( Collection<String> names )
   {
      if( names == null || names.isEmpty() )
          return new PropertySnapshot( PropertyType.BOOTSTRAP, Map.of() );

      final Map<String, Object> loaded = new LinkedHashMap<>();

      for( PropertySource source : sources )
           source.load( names, loaded );

      Set<String> loadedNames = loaded.keySet();
      Set<String> unusedNames = names.stream().filter(s->!loadedNames.contains(s) ).collect(Collectors.toSet());

      /*
       * Обновляем effective-состояние только для явно
       * запрошенных свойств.
       */
      properties.keySet().retainAll(unusedNames);

      properties.putAll(loaded);

      return new PropertySnapshot( PropertyType.BOOTSTRAP, loaded );
   }


   /**
    * Возвращает уже загруженное значение.
    *
    * <p>Метод не выполняет IO и не обращается к PropertySource.
    */
   @Override
   @SuppressWarnings("unchecked")
   public <T> T get(String name) {
      return (T) properties.get(name);
   }


   /**
    * Возвращает уже загруженное значение как String.
    *
    * Метод не выполняет IO и не обращается к PropertySource.
    */
   @Override
   public String getString(String name) {

      final Object value = properties.get(name);
      return value == null ? null : value.toString();
   }


   /**
    * Проверяет наличие свойства в уже загруженном effective-состоянии.
    */
   @Override
   public boolean contains(String name) {
      return properties.containsKey(name);
   }


   /**
    * Применяет runtime-изменения.
    *
    * <p>Runtime source имеет максимальный приоритет.
    */
   @Override
   public void apply(PropertyPatch patch) {

      if( patch == null )
          return;

      runtimeSource.apply(patch);
      properties.putAll(patch.values());
   }


   /**
    * Закрывает все источники свойств.
    */
   @Override
   public void close( ) {

      RuntimeException error = null;

      for( PropertySource source : sources )
      {
         try {
            source.close();
         }
         catch (Exception ex) {
            if( error == null )
                error = new RuntimeException( "Unable to close property source: " + source.name(), ex );
            else
                error.addSuppressed(ex);
         }
      }

      if( error != null )
          throw error;
   }

   /**
    * Определяет properties-файл приложения.
    *
    * Приоритет соответствует старому PRP_AppProperties:
    *
    * <ol>
    *     <li>-D&lt;applicationId&gt;.file_properties</li>
    *     <li>-Dfile_properties</li>
    *     <li>named argument &lt;applicationId&gt;.file_properties</li>
    *     <li>named argument file_properties</li>
    *     <li>${user.home}/&lt;applicationId&gt;.properties</li>
    *     <li>${user.home}/xxiapp.properties</li>
    * </ol>
    */
   private static Path resolvePropertyFile( String applicationId, Map<String, String> arguments )
   {
      final String appFileProperty = applicationId == null || applicationId.isBlank() ? null : applicationId + "." + FILE_PROPERTIES;

      String fileName = null;

      if( appFileProperty != null )
          fileName = System.getProperty(appFileProperty);

      if( S.isNullOrEmpty(fileName) )
          fileName = System.getProperty(FILE_PROPERTIES);

      if( S.isNullOrEmpty(fileName) && appFileProperty != null )
          fileName = arguments.get(appFileProperty);

      if( S.isNullOrEmpty(fileName) )
          fileName = arguments.get(FILE_PROPERTIES);

      if( !S.isNullOrEmpty(fileName) )
      {
         final Path file = Path.of(fileName);

         if( Files.isRegularFile(file) )
            return file;
      }

      final String userHome = System.getProperty("user.home");

      if( S.isNullOrEmpty(userHome) )
          return null;

      final Path home = Path.of(userHome);

      if( applicationId != null && !applicationId.isBlank() )
      {
         final Path appFile = home.resolve( applicationId + ".properties" );

         if( Files.isRegularFile(appFile) )
             return appFile;
      }

      final Path defaultFile = home.resolve("xxiapp.properties");

      return Files.isRegularFile(defaultFile) ? defaultFile : null;
   }

}