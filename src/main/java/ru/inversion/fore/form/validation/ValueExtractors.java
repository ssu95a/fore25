package ru.inversion.fore.form.validation;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import ru.inversion.utils.Checks;

final class ValueExtractors
{
   @SuppressWarnings("unchecked")
   static <T> ObservableValue<T> valueOf(Control control)
   {
      Checks.Require.object(control, "control");

      return (ObservableValue<T>) switch( control )
      {
         case TextInputControl c -> c.textProperty();
         case ComboBoxBase<?> c  -> c.valueProperty();
         case ChoiceBox<?> c     -> c.valueProperty();
         case CheckBox c         -> c.selectedProperty();
         default -> throw new IllegalArgumentException( "Validation value extractor not found for " + control.getClass().getName() );
      };
   }
}