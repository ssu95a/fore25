package ru.inversion.fx.form.controls.progress;

/**
 * @param <I> Интерфейс который будет использоваться в callback'е
 * @param <R> Тип возвращаемого результата из callback'а
 * @param <P> Тип параметра передаваемого в callback
 */
interface TaskCallback<I, R, P> {
    R call(I progressInfo, P parameter) throws Exception;
}
