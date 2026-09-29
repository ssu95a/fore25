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

   private final List<FormRule> formRules = new ArrayList<>();


   /**
    * Добавить правило проверки VALUE элемента управления.
    *
    * Какое именно значение является value данного Control,
    * определяет ValueExtractors.
    */
   public FormValidation add( Control control, Rule<?> rule )
   {
      Checks.Require.objects( control, "control", rule, "rule");
      controlRules(control).add( () -> check( control, ValueExtractors.valueOf(control), rule ) );
      return this;
   }

   /**
    * Добавить правило, которое работает непосредственно
    * с Control, а не только с его value.
    *
    * Используется для проверок, которым нужны properties,
    * metadata или другое состояние Control.
    */
   public <C extends Control> FormValidation forControl( C control, ControlRule<? super C> rule )
   {
      Checks.Require.objects( control, "control", rule,  "rule" );
      controlRules(control).add( () -> requireResult( rule.check(control) ).withTarget(control) );

      return this;
   }


   /**
    * Добавить правило уровня всей формы.
    */
   public FormValidation add(FormRule rule)
   {
      formRules.add( Checks.Require.object( rule, "rule" ) );
      return this;
   }


   /**
    * Проверить все правила указанного Control.
    *
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
    * После них выполняются FormRule.
    *
    * Проверка fail-fast.
    */
   public ValidationResult validate()
           throws Exception
   {
      for( ControlRules rules : controls.values() )
      {
         final ValidationResult result =
                 rules.validate();

         if( !result.valid() )
            return result;
      }

      for( FormRule rule : formRules )
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


   /**
    * Есть ли вообще зарегистрированные проверки.
    */
   public boolean isEmpty()
   {
      return controls.isEmpty()
              && formRules.isEmpty();
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
           Rule<?> rule )
           throws Exception
   {
      final ValidationResult result =
              ((Rule<Object>) rule).check(value);

      return requireResult(result)
              .withTarget(control);
   }


   /**
    * Rule не имеет права возвращать null.
    *
    * null означает ошибку реализации Rule,
    * а не успешную validation.
    */
   private static ValidationResult requireResult(
           ValidationResult result )
   {
      if( result == null )
      {
         throw new IllegalStateException(
                 "Validation rule returned null"
         );
      }

      return result;
   }


   /**
    * Уже полностью подготовленная к выполнению проверка.
    */
   @FunctionalInterface
   private interface Check
   {
      ValidationResult check()
              throws Exception;
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