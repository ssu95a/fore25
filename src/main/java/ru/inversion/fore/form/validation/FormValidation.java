package ru.inversion.fore.form.validation;

import javafx.scene.Node;
import ru.inversion.utils.Checks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FormValidation
{
   private final List<Entry> entries = new ArrayList<>();

   private final List<FormRule> formRules = new ArrayList<>();


   /**
    * Добавить правило проверки значения,
    * связанное с конкретным элементом формы.
    */
   public <T> FormValidation add(
           Node target,
           Supplier<? extends T> valueSupplier,
           Rule<? super T> rule )
   {
      Checks.Require.objects(
              valueSupplier, "valueSupplier",
              rule,          "rule"
      );

      entries.add(
              new Entry(
                      target,
                      () -> attachTarget(
                              target,
                              rule.check(valueSupplier.get())
                      )
              )
      );

      return this;
   }


   /**
    * Добавить правило уровня всей формы.
    */
   public FormValidation add(FormRule rule)
   {
      Checks.Require.object(rule, "rule");

      formRules.add(rule);

      return this;
   }


   /**
    * Проверить всю форму.
    *
    * Проверки выполняются последовательно.
    * Возвращается первый неуспешный результат.
    */
   public ValidationResult validate() throws Exception
   {
      for( Entry entry : entries )
      {
         final ValidationResult result =
                 check(entry.rule());

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


   /**
    * Проверить правила, относящиеся только
    * к указанному элементу формы.
    */
   public ValidationResult validate(Node target) throws Exception
   {
      Checks.Require.object(target, "target");

      for( Entry entry : entries )
      {
         if( entry.target() != target )
            continue;

         final ValidationResult result =
                 check(entry.rule());

         if( !result.valid() )
            return result;
      }

      return ValidationResult.ok();
   }


   public boolean isEmpty()
   {
      return entries.isEmpty() && formRules.isEmpty();
   }


   private ValidationResult check(FormRule rule) throws Exception
   {
      final ValidationResult result = rule.check();

      if( result == null )
         throw new IllegalStateException(
                 "Validation rule returned null"
         );

      return result;
   }


   private ValidationResult attachTarget(
           Node target,
           ValidationResult result )
   {
      if( result == null )
         throw new IllegalStateException(
                 "Validation rule returned null"
         );

      if( result.valid() || target == null )
         return result;

      final List<ValidationFailure> failures =
              result.failures()
                      .stream()
                      .map(failure ->
                              failure.target() == null
                                      ? failure.withTarget(target)
                                      : failure
                      )
                      .toList();

      return ValidationResult.failures(failures);
   }


   private record Entry(
           Node target,
           FormRule rule )
   {
   }
}