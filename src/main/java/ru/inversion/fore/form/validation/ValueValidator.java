package ru.inversion.fore.form.validation;

@FunctionalInterface
public interface ValueValidator<T>
{
   ValidationResult check( T value ) throws Exception;
}