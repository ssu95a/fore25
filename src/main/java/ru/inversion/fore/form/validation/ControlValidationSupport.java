package ru.inversion.fore.form.validation;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;

import ru.inversion.fore.form.control.ValueExtractors;
import ru.inversion.utils.Checks;

import java.util.function.Consumer;


final class ControlValidationSupport
{
   private final FormValidation validation;
   private final Consumer<? super Exception> errorHandler;

   private boolean installed;


   ControlValidationSupport( FormValidation validation, Consumer<? super Exception> errorHandler )
   {
      this.validation = Checks.Require.object( validation, "validation"              );

      this.errorHandler =
              Checks.Require.object(
                      errorHandler,
                      "errorHandler"
              );
   }


   void install()
   {
      if( installed )
          return;

      installed = true;

      for( Control control : validation.controls() )
           install(control);
   }


   private void install(Control control)
   {
      ValidationStateSupport.reset(control);

      installValueListener(control);
      installFocusListener(control);
   }


   private void installValueListener(Control control)
   {
      final ObservableValue<?> value =
              ValueExtractors.observable(control);

      value.addListener(
              observable ->
                      ValidationStateSupport.reset(control)
      );
   }


   private void installFocusListener(Control control)
   {
      control.focusedProperty().addListener(
              (observable, oldValue, focused) ->
              {
                 if( !focused )
                    validateIfNeeded(control);
              }
      );
   }


   private void validateIfNeeded(Control control)
   {
      if( ValidationStateSupport.get(control)
              != ValidationState.UNVALIDATED )
      {
         return;
      }

      try
      {
         final ValidationResult result =
                 validation.validate(control);

         ValidationStateSupport.set(
                 control,
                 result.valid()
                         ? ValidationState.VALID
                         : ValidationState.INVALID
         );
      }
      catch( Exception ex )
      {
         /*
          * Техническая ошибка validation не означает,
          * что данные пользователя INVALID.
          */
         ValidationStateSupport.reset(control);

         errorHandler.accept(ex);
      }
   }
}