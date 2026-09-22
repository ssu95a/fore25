package ru.inversion.fore.app.properties;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/** Источник свойств из properties-файла. */
public final class FilePropertySource implements PropertySource {

   private final Path path;
   private final Charset charset;

   private volatile Map<String, Object> properties;

   public FilePropertySource(Path path) {
      this(path, Charset.defaultCharset());
   }

   public FilePropertySource(Path path, Charset charset) {
      this.path = path;
      this.charset = charset;
   }

   @Override
   public String name() {
      return "file";
   }

   @Override
   public Map<String, Object> load(Collection<String> names) {

      if (names == null || names.isEmpty()) {
         return Map.of();
      }

      final Map<String, Object> source = properties();
      final Map<String, Object> result = new LinkedHashMap<>();

      for (String name : names) {
         if (name != null && source.containsKey(name)) {
            result.put(name, source.get(name));
         }
      }

      return result;
   }

   @Override
   public Object get(String key) {
      return key == null ? null : properties().get(key);
   }

   private Map<String, Object> properties() {

      Map<String, Object> result = properties;

      if (result == null) {
         synchronized (this) {
            result = properties;

            if (result == null) {
               properties = result = loadFile();
            }
         }
      }

      return result;
   }

   private Map<String, Object> loadFile()
   {

      if (path == null || !Files.isRegularFile(path)) {
         return Map.of();
      }

      final Properties source = new Properties();

      try (Reader reader = Files.newBufferedReader(path, charset)) {
         source.load(reader);
      }
      catch (IOException ex) {
         throw new IllegalStateException(
                 "Unable to load properties from " + path,
                 ex
         );
      }

      final Map<String, Object> result = new LinkedHashMap<>();

      source.forEach((key, value) ->
              result.put(String.valueOf(key), value)
      );

      return Map.copyOf(result);
   }
}