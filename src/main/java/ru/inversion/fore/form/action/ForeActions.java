package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.Objects;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/** Standard Fore action catalog and factory. No shared mutable Action instances. */
public final class ForeActions
{
   private ForeActions()
   {
   }

   /** Resolve labels for the current locale, using a Fore-owned snapshot of the legacy labels. */
   public static ActionPrototype prototype(StandardAction type)
   {
      Objects.requireNonNull(type, "type");

      final ResourceBundle bundle = ResourceBundle.getBundle(
              "ru.inversion.fore.form.action.actions", Locale.getDefault(), ForeActions.class.getClassLoader()
      );
      final String key = type.name();
      final String text = bundle.getString(key);
      final String tooltipKey = key + "_TOOLTIP";
      final String tooltip = bundle.containsKey(tooltipKey)
              ? bundle.getString(tooltipKey)
              : text;

      return new ActionPrototype(type, text, tooltip, icon(type), shortcuts(type));
   }

   /** Each invocation returns a new independent action with its own handler/state. */
   public static ForeAction create(StandardAction type, Consumer<ActionEvent> handler)
   {
      return new ForeAction(
              prototype(type),
              Objects.requireNonNull(handler, "handler")
      );
   }

   private static IconSpec icon(StandardAction type)
   {
      return new IconSpec(IconFont.FONT_AWESOME_4, switch(type)
      {
         case CREATE  -> "\uf016"; // FontAwesome fa-file-o
         case UPDATE  -> "\uf044"; // fa-edit
         case DELETE  -> "\uf00d"; // fa-close
         case VIEW    -> "\uf05a"; // fa-info-circle
         case REFRESH -> "\uf021"; // fa-refresh
         case IMPORT -> null;
         case EXPORT -> null;
         case PRINT -> null;
         case STATUS -> null;
      });
   }

   /** Legacy defaults. Scope/conflict resolution belongs to a future keyboard binder. */
   private static List<KeyCombination> shortcuts(StandardAction type)
   {
      return switch(type)
      {
         case CREATE -> List.of(
                 new KeyCodeCombination(KeyCode.F2),
                 new KeyCodeCombination(KeyCode.F6)
         );
         case UPDATE -> List.of(new KeyCodeCombination(KeyCode.F4));
         case DELETE -> List.of(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN));
         case VIEW -> List.of(new KeyCodeCombination(KeyCode.F3));
         case REFRESH -> List.of(new KeyCodeCombination(KeyCode.F8));
         case IMPORT -> null;
         case EXPORT -> null;
         case PRINT -> null;
         case STATUS -> null;
      };
   }
}
