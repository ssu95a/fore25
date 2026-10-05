package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCombination;
import ru.inversion.utils.Checks;

import java.util.List;
import java.util.ResourceBundle;

/** Bundle-backed appearance and shortcut defaults. Never contains a handler or mutable UI state. */
public record ActionPrototype(
   StandardAction type,
   ResourceBundle bundle,
   IconSpec icon,
   List<KeyCombination> hotkeys
)
{
   public ActionPrototype
   {
      Checks.Require.object( type, "type");
      Checks.Require.object( bundle, "bundle");
      Checks.Require.object( icon, "icon");
      Checks.Require.object( hotkeys, "hotkeys");

      final String key = type.name();
      Checks.Require.text(bundle.getString(key), "text");
      if( bundle.containsKey(key + "_TOOLTIP") )
         Checks.Require.text(bundle.getString(key + "_TOOLTIP"), "tooltip");

      hotkeys = List.copyOf(hotkeys);
   }

   /** Caption keys are the exact StandardAction names, such as CREATE or UPDATE. */
   public String text()
   {
      return bundle.getString(type.name());
   }

   /** An optional tooltip key overrides the caption from the same bundle. */
   public String tooltip()
   {
      final String key = type.name() + "_TOOLTIP";
      return bundle.containsKey(key) ? bundle.getString(key) : text();
   }
}
