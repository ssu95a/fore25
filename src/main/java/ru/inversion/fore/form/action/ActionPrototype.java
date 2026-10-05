package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCombination;
import ru.inversion.utils.Checks;

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
      Checks.Require.object( type, "type");
      Checks.Require.text  ( text, "text");
      Checks.Require.text  ( tooltip, "tooltip");
      Checks.Require.object( icon, "icon");
      Checks.Require.object( hotkeys, "hotkeys");
      hotkeys = List.copyOf(hotkeys);
   }
}
