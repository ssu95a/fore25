package ru.inversion.fore.app.properties;

import ru.inversion.utils.S;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Источник свойств из {@link Preferences}.
 */
public final class PreferencesPropertySource implements PropertySource {

   private final String name;
   private final Preferences preferences;

   public PreferencesPropertySource(String name, Preferences preferences) {
      this.name = Objects.requireNonNull(name);
      this.preferences = Objects.requireNonNull(preferences);
   }

   @Override
   public String name() { return name; }

   @Override
   public Map<String, Object> load( Collection<String> names )
   {
      if( names == null || names.isEmpty() )
          return Map.of();

      final Map<String, Object> result = new LinkedHashMap<>();

      for( String key : names )
      {
         if(S.isNullOrEmpty(key) )
             continue;

         final String value = preferences.get(key, null);

         if( value != null )
             result.put(key, value);

      }

      return result;
   }

   @Override
   public Object get(String key) {
      return key == null ? null : preferences.get(key, null);
   }
}