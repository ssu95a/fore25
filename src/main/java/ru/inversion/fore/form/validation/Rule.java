package ru.inversion.fore.form.validation;

@FunctionalInterface
public interface Rule<T>
{
   ValidationResult check(T value) throws Exception;
}