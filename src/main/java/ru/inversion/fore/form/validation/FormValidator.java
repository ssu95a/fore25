package ru.inversion.fore.form.validation;

@FunctionalInterface
public interface FormValidator
{
   ValidationResult check() throws Exception;
}