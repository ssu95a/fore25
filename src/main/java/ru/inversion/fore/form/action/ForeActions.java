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

   /** Use the bundled standard labels for the current locale. */
   public static ActionPrototype prototype(StandardAction type)
   {
      Objects.requireNonNull(type, "type");

      final ResourceBundle bundle = ResourceBundle.getBundle(
              "ru.inversion.fore.form.action.actions", Locale.getDefault(), ForeActions.class.getClassLoader()
      );
      return prototype(type, bundle);
   }

   /** Resolve standard labels from the supplied form or application bundle. */
   public static ActionPrototype prototype(StandardAction type, ResourceBundle bundle)
   {
      Objects.requireNonNull(type, "type");
      return new ActionPrototype(type, bundle, type.icon(), shortcuts(type));
   }

   /** Each invocation returns a new independent action with its own handler/state. */
   public static ForeAction create(StandardAction type, Consumer<ActionEvent> handler)
   {
      return new ForeAction(
              prototype(type),
              Objects.requireNonNull(handler, "handler")
      );
   }

   /** Create an independent action with labels from the supplied bundle. */
   public static ForeAction create(StandardAction type, ResourceBundle bundle, Consumer<ActionEvent> handler)
   {
      return new ForeAction(
              prototype(type, bundle),
              Objects.requireNonNull(handler, "handler")
      );
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
      };
   }
}
