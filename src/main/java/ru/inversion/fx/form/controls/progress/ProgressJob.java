package ru.inversion.fx.form.controls.progress;

import java.util.Objects;

/**
 * Класс представляет из себя задачу, которая предназначена для {@link MultiProgressTaskExecutor}
 *
 * @param <R> Результат выполненной задачи
 * @param <P> Параметр для использования внутри прогресса
 */
public class ProgressJob<R, P> {

    private final String name;

    private final AbstractProgressTask progressTask;

    private boolean allowCancel;

    private ProgressHandler<R> successHandler;

    private ProgressHandler<R> failedHandler;

    private ProgressHandler<R> cancelHandler;

    private ProgressHandler<R> allHandler;

    private boolean indicator;

    /**
     * Создать новую задачу
     *
     * @param name             название задачи, будет отображаться в процессе выполнения
     * @param progressCallback логика задачи, что будет задача выполнять
     */
    public ProgressJob(String name, ProgressCallback<R, P> progressCallback) {
        this(name, progressCallback, true);
    }

    /**
     * {@link #ProgressJob(String, ProgressCallback)}
     */
    public ProgressJob(String name, MultiProgressCallback<R, P> progressCallback) {
        this(name, progressCallback, true);
    }

    /**
     * Создать новую задачу
     *
     * @param name             название задачи, будет отображаться в процессе выполнения
     * @param progressCallback логика задачи, что будет задача выполнять
     * @param allowCancel      разрешено ли отменять задачу
     */
    public ProgressJob(String name, ProgressCallback<R, P> progressCallback, boolean allowCancel) {
        this(name, progressCallback, null, allowCancel, false);
    }

    /**
     * {@link #ProgressJob(String, ProgressCallback, boolean)}
     */
    public ProgressJob(String name, MultiProgressCallback<R, P> progressCallback, boolean allowCancel) {
        this(name, progressCallback, null, allowCancel, false);
    }


    /**
     * Создать новую задачу
     *
     * @param name             название задачи, будет отображаться в процессе выполнения
     * @param progressCallback логика задачи, что будет задача выполнять
     */
    public ProgressJob(String name, ProgressCallback<R, P> progressCallback, P parameter) {
        this(name, progressCallback, parameter, true, false);
    }

    /**
     * {@link #ProgressJob(String, ProgressCallback, Object)}
     */
    public ProgressJob(String name, MultiProgressCallback<R, P> progressCallback, P parameter) {
        this(name, progressCallback, parameter, true, false);
    }

    /**
     * Создать новую задачу
     *
     * @param name             название задачи, будет отображаться в процессе выполнения
     * @param progressCallback логика задачи, что будет задача выполнять
     * @param allowCancel      разрешено ли отменять задачу
     */
    public ProgressJob(String name, ProgressCallback<R, P> progressCallback, P parameter, boolean allowCancel) {
        this(name, progressCallback, parameter, allowCancel, false);
    }

    /**
     * {@link #ProgressJob(String, ProgressCallback, Object, boolean)}
     */
    public ProgressJob(String name, MultiProgressCallback<R, P> progressCallback, P parameter, boolean allowCancel) {
        this(name, progressCallback, parameter, allowCancel, false);
    }

    /**
     * Создать новую задачу
     *
     * @param name             название задачи, будет отображаться в процессе выполнения
     * @param progressCallback логика задачи, что будет задача выполнять
     * @param allowCancel      разрешено ли отменять задачу
     * @param indicator        если true, то прогресс выполнения задачи будет в виде progressIndicator'а(колесо)
     */
    public ProgressJob(String name, ProgressCallback<R, P> progressCallback, P parameter,
                       boolean allowCancel, boolean indicator) {
        this.name = Objects.requireNonNull(name, "name can't be null");

        if (progressCallback == null) throw new IllegalArgumentException("progressCallback can't be null");

        this.progressTask = new SingleProgressTask<R, P>(progressCallback, parameter);

        this.allowCancel = allowCancel;
        this.indicator = indicator;
    }

    /**
     * {@link #ProgressJob(String, ProgressCallback, Object, boolean, boolean)}
     */
    public ProgressJob(String name, MultiProgressCallback<R, P> progressCallback, P parameter,
                       boolean allowCancel, boolean indicator) {
        this.name = Objects.requireNonNull(name, "name can't be null");

        if (progressCallback == null) throw new IllegalArgumentException("progressCallback can't be null");

        this.progressTask = new MultiProgressTask<R, P>(progressCallback, parameter);

        this.allowCancel = allowCancel;
        this.indicator = indicator;
    }

    public String getName() {
        return name;
    }

    public ProgressHandler<R> getSuccessHandler() {
        return successHandler;
    }

    /**
     * Событие будет вызвано после успешного выполнения задачи
     *
     * @param successHandler обработчик события успешного выполнения, может быть null
     */
    public void setSuccessHandler(ProgressHandler<R> successHandler) {
        this.successHandler = successHandler;
    }

    public ProgressHandler<R> getFailedHandler() {
        return failedHandler;
    }

    /**
     * Событие будет вызвано в случае ошибки в процессе выполнения задачи
     *
     * @param failedHandler обработчик события ошибки выполнения, может быть null
     */
    public void setFailedHandler(ProgressHandler<R> failedHandler) {
        this.failedHandler = failedHandler;
    }

    public ProgressHandler<R> getCancelHandler() {
        return cancelHandler;
    }

    public void setCancelHandler(ProgressHandler<R> cancelHandler) {
        this.cancelHandler = cancelHandler;
    }

    public ProgressHandler<R> getAllHandler() {
        return allHandler;
    }

    /**
     * Событие будет вызвано в любом из случаев(успешное выполнение или ошибка)
     *
     * @param allHandler обработчик события всех событий, может быть null
     */
    public void setAllHandler(ProgressHandler<R> allHandler) {
        this.allHandler = allHandler;
    }

    public AbstractProgressTask getProgressTask() {
        return progressTask;
    }

    public boolean isAllowCancel() {
        return allowCancel;
    }

    public void setAllowCancel(boolean allowCancel) {
        this.allowCancel = allowCancel;
    }

    public boolean isIndicator() {
        return indicator;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProgressJob<?, ?> that = (ProgressJob<?, ?>) o;

        if (allowCancel != that.allowCancel) return false;
        if (indicator != that.indicator) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (progressTask != null ? !progressTask.equals(that.progressTask) : that.progressTask != null) return false;
        if (successHandler != null ? !successHandler.equals(that.successHandler) : that.successHandler != null)
            return false;
        if (failedHandler != null ? !failedHandler.equals(that.failedHandler) : that.failedHandler != null)
            return false;
        if (cancelHandler != null ? !cancelHandler.equals(that.cancelHandler) : that.cancelHandler != null)
            return false;
        return allHandler != null ? allHandler.equals(that.allHandler) : that.allHandler == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (progressTask != null ? progressTask.hashCode() : 0);
        result = 31 * result + (allowCancel ? 1 : 0);
        result = 31 * result + (successHandler != null ? successHandler.hashCode() : 0);
        result = 31 * result + (failedHandler != null ? failedHandler.hashCode() : 0);
        result = 31 * result + (cancelHandler != null ? cancelHandler.hashCode() : 0);
        result = 31 * result + (allHandler != null ? allHandler.hashCode() : 0);
        result = 31 * result + (indicator ? 1 : 0);
        return result;
    }
}
