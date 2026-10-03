package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.Objects;

/** Immutable appearance and shortcut defaults. Never contains a handler or mutable UI state. */
public record ActionPrototype(
   StandardAction type,
   String text,
   String tooltip,
   IconSpec icon,
   List<KeyCombination> hotkeys
)
{
   public ActionPrototype
   {
      Objects.requireNonNull(type, "type");
      requireText(text, "text");
      requireText(tooltip, "tooltip");
      Objects.requireNonNull(icon, "icon");
      Objects.requireNonNull(hotkeys, "hotkeys");
      hotkeys = List.copyOf(hotkeys);
   }

   private static void requireText(String value, String name)
   {
      if( value == null || value.isBlank() )
         throw new IllegalArgumentException(name + " must not be blank");
   }
}
