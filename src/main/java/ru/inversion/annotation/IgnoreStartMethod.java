package ru.inversion.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер предназначенный для JInvDesktopAdmin, чтобы не попадал в список для запуска.
 * <p>
 * Если в вашем проекте есть методы, которые по сигнатуре совпадают с подобным:
 * <br><code>public static void showNewsAdmin(ViewContext vc, TaskContext tc, Map p)</code><br>
 * Эта аннотация поможет избежать отображения вашего метода в списке JInvDesktopAdmin'а.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreStartMethod {
}
