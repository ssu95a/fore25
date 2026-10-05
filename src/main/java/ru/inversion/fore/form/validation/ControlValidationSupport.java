package ru.inversion.fore.form.validation;

import javafx.scene.control.Control;

import ru.inversion.fore.form.control.ValueExtractors;
import ru.inversion.utils.Checks;

import java.util.function.Consumer;


public final class ControlValidationSupport
{
   private final FormValidation validation;
   private final Consumer<? super Exception> errorHandler;

   private boolean installed;

   private final ValidationPresenter presenter;

   public ControlValidationSupport(
           FormValidation validation,
           ValidationPresenter presenter,
           Consumer<? super Exception> errorHandler )
   {
      this.validation =
              Checks.Require.object(validation, "validation");

      this.presenter =
              Checks.Require.object(presenter, "presenter");

      this.errorHandler =
              Checks.Require.object(errorHandler, "errorHandler");
   }

   public void install()
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
      ValueExtractors.findObservable(control)
         .ifPresent(
         value -> value.addListener(observable ->
                 {
                    ValidationStateSupport.reset(control);
                    presenter.clear(control);
                 }
            )
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
         return;

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

         if( result.valid() )
            presenter.clear(control);
         else
            presenter.show(result);
      }
      catch( Exception ex )
      {
         ValidationStateSupport.reset(control);
         presenter.clear(control);

         errorHandler.accept(ex);
      }
   }
}