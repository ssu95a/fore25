package ru.inversion.fore.form.control;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.TextField;

/**
 * Fore text input. Its value is the inherited textProperty(): ControlsFX
 * ValueExtractor already supports every TextInputControl, including this one.
 *
 * Validation belongs to FormValidation, not to the control itself.
 */
public class ForeTextField extends TextField
{

   /** Public no-arg constructor for FXMLLoader / Scene Builder. */
   public ForeTextField()
   {
      this("");
   }

   public ForeTextField(String text)
   {
      super(text);
      getStyleClass().add("fore-text-field");

   }

}
