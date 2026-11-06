package ru.inversion.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация предназначена для обозначения 'стартовых' методов.
 * Используется JInvDesktopAdmin'ом для отображения возможных
 * для запуска методов.
 * <p>
 * Пример: метод  {@code showAccInfo()} из класса {@code PAccInfoMain (FXPdoc)} можно пометить
 * аннотацией {@code StartMethod}, так как этот метод предназначен для запуска контроллера
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StartMethod {
    String description() default "";

    StartMode mode() default StartMode.NONE;
}
