package ru.inversion.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация предназначена для обозначения стартовых классов
 * Используется JInvDesktopAdmin'ом для отображения возможных
 * для запуска классов.
 * <p>
 * Пример: класс {@code App} можно пометить аннотацией {@code StartClass},
 * так как этот класс является 'стартовым' (наследник BaseApp)
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface StartClass {
    String description() default "";
}
