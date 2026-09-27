package ru.inversion.fore.form.validation;

import javafx.scene.control.Control;
import ru.inversion.utils.Checks;

public record ValidationFailure( Control target, String message )
{

   /** */
   public ValidationFailure
   {
      Checks.Require.text(message, "message");
   }


   /** */
   public ValidationFailure withTarget(Control target)
   {
      return new ValidationFailure(target, message);
   }


   /** */
   public static ValidationFailure of(String message)
   {
      return new ValidationFailure(null, message);
   }


   /** */
   public static ValidationFailure of( Control target, String message )
   {
      return new ValidationFailure(target, message);
   }
}