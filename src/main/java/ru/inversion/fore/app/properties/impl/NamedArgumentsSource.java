package ru.inversion.fore.app.properties.impl;

import ru.inversion.fore.app.properties.PropertySource;

import java.util.Map;

/** Источник именованных аргументов приложения. */
public final class NamedArgumentsSource implements PropertySource {

   private final Map<String, String> arguments;

   public NamedArgumentsSource( Map<String, String> arguments) {
      this.arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
   }

   @Override
   public String name() {
      return "arguments";
   }

   @Override
   public Object get(String key) {
      return key == null ? null : arguments.get(key);
   }
}