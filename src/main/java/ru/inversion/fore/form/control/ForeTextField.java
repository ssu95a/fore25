package ru.inversion.fore.form.control;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Fore text input. Its value is the inherited textProperty(): ControlsFX
 * ValueExtractor already supports every TextInputControl, including this one.
 *
 * Validation belongs to FormValidation, not to the control itself.
 */
public class ForeTextField extends TextField implements IForeControl
{

   /** Public no-arg constructor for FXMLLoader / Scene Builder. */
   public ForeTextField()
   {
      this("");
   }

   /** */
   public ForeTextField(String text)
   {
      super(text);
      getStyleClass().add("fore-text-field");
   }

   // FXMLLoader ищет JavaBean-методы в классах, а не default-методы интерфейсов.
   @Override
   public String getFieldName()
   {
      return IForeControl.super.getFieldName();
   }

   @Override
   public void setFieldName(String fieldName)
   {
      IForeControl.super.setFieldName(fieldName);
   }

   @Override
   public Label getLabel()
   {
      return IForeControl.super.getLabel();
   }

   @Override
   public void setLabel(Label label)
   {
      IForeControl.super.setLabel(label);
   }

}
