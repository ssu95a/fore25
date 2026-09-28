package ru.inversion.fore.form.validation;

import javafx.scene.control.Control;

@FunctionalInterface
public interface ControlRule<C extends Control>
{
   ValidationResult check(C control) throws Exception;
}