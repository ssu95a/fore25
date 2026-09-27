package ru.inversion.fore.form.validation;

@FunctionalInterface
public interface FormRule
{
   ValidationResult check() throws Exception;
}