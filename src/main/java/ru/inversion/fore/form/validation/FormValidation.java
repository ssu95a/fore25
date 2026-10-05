package ru.inversion.fore.form.validation;

import javafx.scene.control.Control;

import ru.inversion.fore.form.control.ValueExtractors;
import ru.inversion.utils.Checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class FormValidation
{
   /*
    * LinkedHashMap нужен для стабильного порядка validation.
    *
    * Control проверяются в порядке их первой регистрации.
    */
   private final Map<Control, ControlRules> controls = new LinkedHashMap<>();

   private final List<FormValidator> formValidators = new ArrayList<>();


   /**
    * Добавить правило проверки VALUE элемента управления.
    * <p>
    * Какое именно значение является value данного Control, определяет ValueExtractors.
    */
   public FormValidation add( Control control, ValueValidator<?> valueValidator)
   {
      Checks.Require.objects( control, "control", valueValidator, "valueValidator");
      controlRules(control).add( () -> check( control, ValueExtractors.valueOf(control), valueValidator) );
      return this;
   }

   /**
    * Добавить правило, которое работает непосредственно
    * с Control, а не только с его value.
    * <p>
    * Используется для проверок, которым нужны properties,
    * metadata или другое состояние Control.
    */
   public <C extends Control> FormValidation forControl( C control, ControlValidator<? super C> controlValidator )
   {
      Checks.Require.objects( control, "control", controlValidator,  "controlValidator" );
      controlRules(control).add( () -> requireResult( controlValidator.check(control) ).withTarget(control) );

      return this;
   }


   /**
    * Добавить правило уровня всей формы.
    */
   public FormValidation add( FormValidator formValidator )
   {
      formValidators.add( Checks.Require.object( formValidator, "formValidator" ) );
      return this;
   }


   /**
    * Проверить все правила указанного Control.
    * <p>
    * Проверка fail-fast.
    */
   public ValidationResult validate( Control control ) throws Exception
   {
      Checks.Require.object( control, "control" );

      final ControlRules rules = controls.get(control);

      if( rules == null )
          return ValidationResult.ok();

      return rules.validate();
   }


   /**
    * Проверить всю форму.
    *
    * Сначала выполняются проверки Control
    * в порядке их регистрации.
    *
    * После них выполняются FormValidator.
    *
    * Проверка fail-fast.
    */
   public ValidationResult validate()
           throws Exception
   {
      for( ControlRules rules : controls.values() )
      {
         final ValidationResult result = rules.validate();

         if( !result.valid() )
              return result;
      }

      for( FormValidator fVldtr : formValidators )
      {
         final ValidationResult result = requireResult( fVldtr.check() );

         if( !result.valid() )
            return result;
      }

      return ValidationResult.ok();
   }


   /**
    * Есть ли вообще зарегистрированные проверки.
    */
   public boolean isEmpty()
   {
      return controls.isEmpty() && formValidators.isEmpty();
   }


   /**
    * Зарегистрированные Control.
    *
    * Package-private API для interactive validation:
    * focus, ValidationState и т.п.
    */
   Set<Control> controls()
   {
      return Collections.unmodifiableSet(
              controls.keySet()
      );
   }


   /**
    * Получить или создать набор проверок Control.
    */
   private ControlRules controlRules(Control control)
   {
      return controls.computeIfAbsent(
              control,
              key -> new ControlRules()
      );
   }


   /**
    * Выполнить обычный value-based Rule.
    *
    * Связь типа value и Rule здесь намеренно динамическая.
    * Это цена за простой универсальный API:
    *
    * validation().add(control, rule)
    */
   @SuppressWarnings("unchecked")
   private static ValidationResult check(
           Control control,
           Object value,
           ValueValidator<?> valueValidator)
           throws Exception
   {
      final ValidationResult result =
              ((ValueValidator<Object>) valueValidator).check(value);

      return requireResult(result)
              .withTarget(control);
   }


   /**
    * Validator не имеет права возвращать {@code null}.
    * <p>
    * {@code null} означает ошибку реализации Validator, а не успешную validation.
    */
   private static ValidationResult requireResult( ValidationResult result )
   {
      if( result == null )
          throw new IllegalStateException( "Validation rule returned null" );

      return result;
   }


   /**
    * Уже полностью подготовленная к выполнению проверка.
    */
   @FunctionalInterface
   private interface Check
   {
      ValidationResult check() throws Exception;
   }


   /**
    * Все проверки одного Control.
    *
    * Не знает:
    * - тип value;
    * - способ извлечения value;
    * - ValidationState;
    * - listeners;
    * - focus;
    * - decoration.
    */
   private static final class ControlRules
   {
      private final List<Check> rules = new ArrayList<>();

      private void add(Check rule)
      {
         rules.add( Checks.Require.object( rule, "rule" ) );
      }


      private ValidationResult validate()
              throws Exception
      {
         for( Check rule : rules )
         {
            final ValidationResult result =
                    requireResult(
                            rule.check()
                    );

            if( !result.valid() )
               return result;
         }

         return ValidationResult.ok();
      }
   }
}