package ru.inversion.fx.form.controls.progress;

/**
 * @param <R> тип результата выполненной задачи
 * @param <P> тип параметра внутри выполняю
 */
@FunctionalInterface
public interface ProgressCallback<R, P> extends TaskCallback<IProgressInfo, R, P> {
    R call(IProgressInfo progressInfo, P parameter) throws Exception;
}
