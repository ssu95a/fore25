package ru.inversion.fx.form.controls.progress;

/**
 * Используется дле регистрации обработчиков событий задач
 *
 * @param <T> тип результата задачи
 */
@FunctionalInterface
public interface ProgressHandler<T> {
    void invoke(T result);
}
