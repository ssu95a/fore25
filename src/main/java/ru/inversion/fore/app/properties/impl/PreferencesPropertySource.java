package ru.inversion.fore.app.properties.impl;

import ru.inversion.fore.app.properties.PropertySource;

import java.util.Objects;
import java.util.prefs.Preferences;

/**
 * Источник свойств из {@link Preferences}.
 */
public final class PreferencesPropertySource implements PropertySource {

   private final String name;
   private final Preferences preferences;

   public PreferencesPropertySource(String name, Preferences preferences) {
      this.name        = Objects.requireNonNull(name);
      this.preferences = Objects.requireNonNull(preferences);
   }

   @Override
   public String name() { return name; }

   @Override
   public Object get(String key) {
      return key == null ? null : preferences.get(key, null);
   }
}