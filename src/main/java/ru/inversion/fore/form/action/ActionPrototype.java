package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCombination;
import ru.inversion.utils.Checks;

import java.util.List;

/** Standard appearance and shortcut defaults. Never contains a handler or mutable UI state. */
public record ActionPrototype(
   StandardAction type,
   IconSpec icon,
   List<KeyCombination> hotkeys
)
{
   public ActionPrototype
   {
      Checks.Require.object( type, "type");
      Checks.Require.object( icon, "icon");
      Checks.Require.object( hotkeys, "hotkeys");

      Checks.Require.text(type.text(), "text");
      Checks.Require.text(type.tooltip(), "tooltip");

      hotkeys = List.copyOf(hotkeys);
   }

   /** Caption from the standard action's private catalog. */
   public String text()
   {
      return type.text();
   }

   /** Tooltip from the same standard catalog as the caption. */
   public String tooltip()
   {
      return type.tooltip();
   }
}
