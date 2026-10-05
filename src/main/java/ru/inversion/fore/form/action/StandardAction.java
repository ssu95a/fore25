package ru.inversion.fore.form.action;

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
}
