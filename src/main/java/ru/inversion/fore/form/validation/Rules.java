package ru.inversion.fore.form.validation;

import ru.inversion.utils.Checks;

import java.util.regex.Pattern;

public final class Rules
{
   private Rules()
   {
   }


   public static <T> Rule<T> required( String message )
   {
      Checks.Require.text( message, "message" );

      return value ->
      {
         final boolean empty = value == null || value instanceof CharSequence s && s.isEmpty();
         return empty ? ValidationResult.failure(message) : ValidationResult.ok();
      };
   }


   public static Rule<String> minLength( int min, String message )
   {
      Checks.Numeric.positiveOrZero(min, "min");
      Checks.Require.text(message, "message");

      return value ->
              value != null && value.length() < min
                      ? ValidationResult.failure(message)
                      : ValidationResult.ok();
   }


   public static Rule<String> maxLength(
           int max,
           String message )
   {
      Checks.Numeric.positiveOrZero(max, "max");
      Checks.Require.text(message, "message");

      return value ->
              value != null && value.length() > max
                      ? ValidationResult.failure(message)
                      : ValidationResult.ok();
   }


   public static Rule<String> pattern(
           Pattern pattern,
           String message )
   {
      Checks.Require.objects(
              pattern, "pattern",
              message, "message"
      );

      Checks.Require.text(message, "message");

      return value ->
              value != null && !pattern.matcher(value).matches()
                      ? ValidationResult.failure(message)
                      : ValidationResult.ok();
   }


   public static <T extends Comparable<? super T>>
   Rule<T> range(
           T min,
           T max,
           String message )
   {
      Checks.Require.objects(
              min,     "min",
              max,     "max",
              message, "message"
      );

      Checks.Require.text(message, "message");

      if( min.compareTo(max) > 0 )
         throw new IllegalArgumentException(
                 "min must not be greater than max"
         );

      return value ->
              value != null &&
                      (min.compareTo(value) > 0 ||
                              max.compareTo(value) < 0)
                      ? ValidationResult.failure(message)
                      : ValidationResult.ok();
   }
}