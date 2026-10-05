package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Standard Fore action catalog and factory. No shared mutable Action instances. */
public final class ForeActions
{
   private ForeActions()
   {
   }

   /** Use the standard action's private caption catalog and default appearance. */
   public static ActionPrototype prototype(StandardAction type)
   {
      Objects.requireNonNull(type, "type");

      return new ActionPrototype(type, type.icon(), shortcuts(type));
   }

   /** Each invocation returns a new independent action with its own handler/state. */
   public static ForeAction create(StandardAction type, Consumer<ActionEvent> handler)
   {
      return new ForeAction(
              prototype(type),
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
