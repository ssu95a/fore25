package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Immutable standard captions, icons and shortcuts for Fore operations.
 * Each runtime ForeAction has its own handler and mutable state.
 */
public enum StandardAction
{
   CREATE(new IconSpec(IconFont.FONT_AWESOME_4, "\uf016"), // fa-file-o
           new KeyCodeCombination(KeyCode.F2), new KeyCodeCombination(KeyCode.F6)),
   UPDATE(new IconSpec(IconFont.FONT_AWESOME_4, "\uf044"), // fa-edit
           new KeyCodeCombination(KeyCode.F4)),
   DELETE(new IconSpec(IconFont.FONT_AWESOME_4, "\uf00d"), // fa-close
           new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN)),
   VIEW(new IconSpec(IconFont.FONT_AWESOME_4, "\uf05a"), // fa-info-circle
           new KeyCodeCombination(KeyCode.F3)),
   REFRESH(new IconSpec(IconFont.FONT_AWESOME_4, "\uf021"), // fa-refresh
           new KeyCodeCombination(KeyCode.F8));

   /** One private catalog for all standard actions, loaded once for the initial default locale. */
   private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(
           "ru.inversion.fore.form.action.actions", Locale.getDefault(), StandardAction.class.getClassLoader()
   );

   private final IconSpec icon;
   private final List<KeyCombination> hotkeys;

   StandardAction(IconSpec icon, KeyCombination... hotkeys)
   {
      this.icon = icon;
      this.hotkeys = List.of(hotkeys);
   }

   /** Immutable default descriptor; newGraphic() creates a separate node for each control. */
   public IconSpec icon()
   {
      return icon;
   }

   /** Immutable defaults; the first shortcut is the primary menu accelerator. */
   public List<KeyCombination> hotkeys()
   {
      return hotkeys;
   }

   /** The caption key is the exact enum name, such as CREATE or UPDATE. */
   public String text()
   {
      return BUNDLE.getString(name());
   }

   /** An optional tooltip key overrides the standard caption. */
   public String tooltip()
   {
      final String key = name() + "_TOOLTIP";
      return BUNDLE.containsKey(key) ? BUNDLE.getString(key) : text();
   }
}
