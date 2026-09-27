package ru.inversion.fore.form.validation;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import ru.inversion.utils.Checks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormValidation
{
   private final Map<Control, List<Entry<?>>> controls = new LinkedHashMap<>();
   private final List<FormRule> formRules = new ArrayList<>();


   public <T> FormValidation add( Control control, Rule<T> rule )
   {
      Checks.Require.objects( control, "control", rule, "rule");

      final ObservableValue<T> value = ValueExtractors.valueOf(control);

      controls.computeIfAbsent( control,key -> new ArrayList<>()).add( new Entry<>(control,value,rule));

      /*
       * Любое изменение validation-value означает,
       * что прежний результат проверки устарел.
       */
      value.addListener(_ ->ValidationStateSupport.reset(control));

      ValidationStateSupport.reset(control);

      return this;
   }


   public FormValidation add(FormRule rule)
   {
      Checks.Require.object(rule, "rule");
      formRules.add(rule);

      return this;
   }


   public ValidationResult validate(Control control) throws Exception
   {
      Checks.Require.object(control, "control");

      final List<Entry<?>> entries =
              controls.get(control);

      if( entries == null )
         return ValidationResult.ok();

      for( Entry<?> entry : entries )
      {
         final ValidationResult result =
                 entry.check();

         if( !result.valid() )
         {
            ValidationStateSupport.set(
                    control,
                    ValidationState.INVALID
            );

            return result;
         }
      }

      ValidationStateSupport.set(
              control,
              ValidationState.VALID
      );

      return ValidationResult.ok();
   }


   public ValidationResult validate()
           throws Exception
   {
      /*
       * На OK проверяем заново всё,
       * независимо от текущего ValidationState.
       */
      for( Control control : controls.keySet() )
      {
         final ValidationResult result =
                 validate(control);

         if( !result.valid() )
            return result;
      }

      for( FormRule rule : formRules )
      {
         final ValidationResult result =
                 check(rule);

         if( !result.valid() )
            return result;
      }

      return ValidationResult.ok();
   }


   public ValidationState state(Control control)
   {
      Checks.Require.object(control, "control");

      return ValidationStateSupport.get(control);
   }


   public void reset(Control control)
   {
      Checks.Require.object(control, "control");

      if( controls.containsKey(control) )
         ValidationStateSupport.reset(control);
   }


   Iterable<Control> controls()
   {
      return controls.keySet();
   }


   private ValidationResult check(FormRule rule)
           throws Exception
   {
      final ValidationResult result =
              rule.check();

      if( result == null )
         throw new IllegalStateException(
                 "Validation rule returned null"
         );

      return result;
   }


   private record Entry<T>( Control control, ObservableValue<T> value, Rule<T> rule )
   {
      ValidationResult check() throws Exception
      {
         final ValidationResult result = rule.check(value.getValue());

         if( result == null )
             throw new IllegalStateException( "Validation rule returned null" );

         if( result.valid() )
             return result;

         return result.withTarget(control);
      }
   }
}