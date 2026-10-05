package ru.inversion.fore.form.validation;

import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;

import static org.junit.jupiter.api.Assertions.*;

class ControlValidationSupportTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void valueListenersCanBeUninstalledAndReinstalled() throws Exception
   {
      FxTestSupport.run(() -> {
         final TextField field = new TextField("A");
         final var validation = new FormValidation().forControl(field, control -> ValidationResult.ok());
         final var support = new ControlValidationSupport(validation, new ValidationPresenter(),
                 error -> fail("Unexpected validation error", error));
         support.install();
         support.install();
         ValidationStateSupport.set(field, ValidationState.VALID);
         field.setText("B");
         assertEquals("B", field.getText());
         assertEquals(ValidationState.UNVALIDATED, ValidationStateSupport.get(field));

         support.uninstall();
         support.uninstall();
         ValidationStateSupport.set(field, ValidationState.VALID);
         field.setText("C");
         assertEquals("C", field.getText());
         assertEquals(ValidationState.VALID, ValidationStateSupport.get(field));

         support.install();
         ValidationStateSupport.set(field, ValidationState.VALID);
         field.setText("D");
         assertEquals("D", field.getText());
         assertEquals(ValidationState.UNVALIDATED, ValidationStateSupport.get(field));
         support.uninstall();
      });
   }
}
