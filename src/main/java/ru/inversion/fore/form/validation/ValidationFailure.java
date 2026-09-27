package ru.inversion.fore.form.validation;


import javafx.scene.Node;
import ru.inversion.utils.Checks;

public record ValidationFailure(
        Node target,
        String message )
{
   public ValidationFailure
   {
      Checks.Require.text(message, "message");
   }


   public ValidationFailure withTarget(Node target)
   {
      return new ValidationFailure(target, message);
   }


   public static ValidationFailure of(String message)
   {
      return new ValidationFailure(null, message);
   }


   public static ValidationFailure of(
           Node target,
           String message )
   {
      return new ValidationFailure(target, message);
   }
}