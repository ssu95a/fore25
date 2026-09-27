package ru.inversion.fore.form.validation;

import javafx.scene.Node;
import ru.inversion.utils.Checks;

import java.util.List;

public record ValidationResult( List<ValidationFailure> failures )
{
   private static final ValidationResult OK = new ValidationResult(List.of());

   /** */
   public ValidationResult
   {
      Checks.Require.object(failures, "failures");

      failures = List.copyOf(failures);
   }


   public boolean valid()
   {
      return failures.isEmpty();
   }


   public static ValidationResult ok()
   {
      return OK;
   }


   public static ValidationResult failure(String message)
   {
      return new ValidationResult( List.of(ValidationFailure.of(message)) );
   }


   public static ValidationResult failure( Node target, String message )
   {
      return new ValidationResult( List.of(ValidationFailure.of(target, message)) );
   }


   public static ValidationResult failures(
           List<ValidationFailure> failures )
   {
      return new ValidationResult(failures);
   }
}