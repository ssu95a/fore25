package ru.inversion.fx.form.controls.progress;

import javafx.concurrent.Task;

import java.util.Objects;

/**
 *
 * @param <R> Тип возвращаемого значения
 * @param <P> Тип передаваемого параметра
 */
public abstract class AbstractProgressTask<I, R, P> extends Task<R> {

    /** ТО что будет вызываться из Task*/
    protected final TaskCallback<I, R, P> callback;

    /** Парметр, который уйдет в callback*/
    protected final P parameter;

    /** */
    AbstractProgressTask( TaskCallback<I, R, P> callback, P parameter )
    {
        this.callback = Objects.requireNonNull(callback, "callback can't be null");
        this.parameter = parameter;
    }

    @Override
    protected final R call( ) throws Exception {
        return invoke();
    }

    /** Переоределется для Multi & Progress task */
    public abstract R invoke() throws Exception;

    /** */
    public TaskCallback<I, R, P> getCallback() {
        return callback;
    }

    /** */
    public P getParameter() {
        return parameter;
    }
}
