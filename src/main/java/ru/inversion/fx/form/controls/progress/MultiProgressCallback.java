package ru.inversion.fx.form.controls.progress;

/**
 * Возможность работы с дополнительным progressBar'ом
 *
 * @param <R> тип результата выполненной задачи
 * @param <P> тип параметра внутри выполняю
 */
@FunctionalInterface
public interface MultiProgressCallback<R, P> extends TaskCallback<IMultiProgressInfo, R, P> {
    R call(IMultiProgressInfo progressInfo, P parameter) throws Exception;
}
