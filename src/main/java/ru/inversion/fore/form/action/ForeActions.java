package ru.inversion.fore.form.action;

import javafx.event.ActionEvent;

import java.util.function.Consumer;

/** Factory for independent Fore action instances. */
public final class ForeActions
{
   private ForeActions()
   {
   }

   /** Each invocation returns a new independent action with its own handler/state. */
   public static ForeAction create(StandardAction type, Consumer<ActionEvent> handler)
   {
      return new ForeAction(type, handler);
   }
}
