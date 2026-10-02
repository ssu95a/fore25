package ru.inversion.fore.form.validation;

import javafx.scene.control.Control;
import ru.inversion.utils.Checks;

final class ValidationStateSupport
{
   private static final Object KEY = new Object();

   private ValidationStateSupport()
   { }


   static ValidationState get( Control control )
   {
      return (ValidationState) control.getProperties().getOrDefault(KEY, ValidationState.UNVALIDATED);
   }


   static void set( Control control, ValidationState state )
   {
      Checks.Require.objects( control, "control", state,   "state" );
      control.getProperties().put(KEY, state);
   }

   /** */
   static void reset(Control control)
   {
      set(control, ValidationState.UNVALIDATED);
   }
}