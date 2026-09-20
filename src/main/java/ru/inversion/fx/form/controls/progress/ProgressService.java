package ru.inversion.fx.form.controls.progress;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.IMultiProgressControl;
import ru.inversion.fx.form.controls.IProgressControl;

import java.util.concurrent.ExecutionException;

/**
 * Связь FX Task с панелью загрузки?
 * <p>
 * связь текста и прогресс бара и хендлеров
 */
public final class ProgressService<R, P> {

    /** */
    private final IProgressControl progressPane;

    /** Достпная ли кнопка отмены, проверка оттменена ли была задача по кнопке cancel */
    private final BooleanProperty cancelledProperty = new SimpleBooleanProperty();

    /** Показывать диалог при ошибке */
    private final boolean showError;

    /** Задача */
    private AbstractProgressTask<?, R, P> progressTask;

    /** FX handlers */
    private ProgressHandler<R> successHandler;
    private ProgressHandler<R> failedHandler;
    private ProgressHandler<R> cancelHandler;
    /** Вызывается всегда */
    private ProgressHandler<R> allHandler;

    /** Слушатель статуса задачи: в начале и по окончании */
    private ProgressServiceListener startListener;
    private ProgressServiceListener finishListener;

    /** */
    ProgressService(AbstractProgressTask<?, R, P> progressTask, IProgressControl progressPane, boolean showError) {
        this.progressTask = progressTask;
        this.progressPane = progressPane;
        this.showError = showError;
    }

    /** */
    public void start() {
        onStart();
        initTaskProperties();
        ThreadPoolManager.getInstance().executeTask(progressTask);
    }


    public void stop() {
        progressTask.cancel(true);
        cancelledProperty.set(true);
    }

    public R getResult() {
        if (!progressTask.isCancelled()) {
            try {
                return progressTask.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean isDone() {
        return progressTask.isDone();
    }

    public Worker.State getState() {
        return progressTask.getState();
    }

    public boolean isCancelled() {
        return cancelledProperty.get();
    }

    public BooleanProperty cancelledProperty() {
        return cancelledProperty;
    }

    public void setSuccessHandler(ProgressHandler<R> successHandler) {
        this.successHandler = successHandler;
    }

    public void setFailedHandler(ProgressHandler<R> failedHandler) {
        this.failedHandler = failedHandler;
    }

    public void setCancelledHandler(ProgressHandler<R> cancelledHandler) {
        this.cancelHandler = cancelledHandler;
    }

    public void setAllHandler(ProgressHandler<R> allHandler) {
        this.allHandler = allHandler;
    }

    public void setStartListener(ProgressServiceListener startListener) {
        this.startListener = startListener;
    }

    public void setFinishListener(ProgressServiceListener finishListener) {
        this.finishListener = finishListener;
    }

    private void initTaskProperties() {
        progressTask.setOnSucceeded(event -> {
            if (successHandler != null) successHandler.invoke(progressTask.getValue());
            if (allHandler != null) allHandler.invoke(progressTask.getValue());
            onFinish();
        });
        progressTask.setOnFailed(event -> {
            if (failedHandler != null) failedHandler.invoke(progressTask.getValue());
            if (allHandler != null) allHandler.invoke(progressTask.getValue());
            onFinish();
            if (showError) {
                Platform.runLater(() -> {
                    JInvErrorService.handleException(null, progressTask.getException());
                });
            }
        });
        progressTask.setOnCancelled(event -> {
            if (cancelHandler != null) cancelHandler.invoke(progressTask.getValue());
            if (allHandler != null) allHandler.invoke(progressTask.getValue());
            onFinish();
        });
    }

    private void onStart() {
        progressPane.progressProperty().bind(progressTask.progressProperty());
        progressPane.textProperty().bind(progressTask.messageProperty());
        if (progressTask instanceof MultiProgressTask && progressPane instanceof IMultiProgressControl) {
            ((IMultiProgressControl) progressPane).innerProgressProperty()
                    .bind(((MultiProgressTask) progressTask).innerProgressProperty());
            ((IMultiProgressControl) progressPane).innerTextProperty()
                    .bind(((MultiProgressTask) progressTask).innerMessageProperty());
        }
        if (startListener != null) {
            startListener.invoke(progressTask);
        }
    }

    private void onFinish() {
        progressPane.progressProperty().unbind();
        progressPane.textProperty().unbind();
        if (progressTask instanceof MultiProgressTask && progressPane instanceof IMultiProgressControl) {
            ((IMultiProgressControl) progressPane).innerProgressProperty().unbind();
            ((IMultiProgressControl) progressPane).innerTextProperty().unbind();
        }

        if (finishListener != null) {
            finishListener.invoke(progressTask);
        }
    }
}
