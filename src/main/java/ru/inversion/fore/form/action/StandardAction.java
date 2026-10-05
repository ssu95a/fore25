package ru.inversion.fore.form.action;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Semantics of a standard Fore operation, not the identity of an action instance.
 * Two controls/forms may have different actions with the same standard type.
 */
public enum StandardAction
{
   CREATE(new IconSpec(IconFont.FONT_AWESOME_4, "\uf016")), // fa-file-o
   UPDATE(new IconSpec(IconFont.FONT_AWESOME_4, "\uf044")), // fa-edit
   DELETE(new IconSpec(IconFont.FONT_AWESOME_4, "\uf00d")), // fa-close
   VIEW(new IconSpec(IconFont.FONT_AWESOME_4, "\uf05a")), // fa-info-circle
   REFRESH(new IconSpec(IconFont.FONT_AWESOME_4, "\uf021")); // fa-refresh

   /** One private catalog for all standard actions, loaded once for the initial default locale. */
   private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(
           "ru.inversion.fore.form.action.actions", Locale.getDefault(), StandardAction.class.getClassLoader()
   );

   private final IconSpec icon;

   StandardAction(IconSpec icon)
   {
      this.icon = icon;
   }

   /** Immutable default descriptor; newGraphic() creates a separate node for each control. */
   public IconSpec icon()
   {
      return icon;
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
