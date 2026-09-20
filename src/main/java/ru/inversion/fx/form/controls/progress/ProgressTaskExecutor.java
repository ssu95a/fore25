package ru.inversion.fx.form.controls.progress;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import ru.inversion.fx.app.Tags;

import java.util.function.Consumer;

/**
 * Класс предназначенный для ОДНОЙ задачи к которой требуется
 * отобразить прогресс ее выполнения.
 *
 * Стартовый класс.
 * Используется программистами
 *
 * <p>
 * Отображать прогресс можно двумя способами в новом {@code Stage} или
 * на вашей текущей панели ({@code Pane})
 * </p>
 *
 * @param <R> Результат выполненной задачи
 * @param <P> Параметр для использования внутри прогресса
 */
public class ProgressTaskExecutor<R, P> {

    private boolean allowCancel;
    private boolean indicator;
    private ProgressStageOption stageOption;
    private Pane pane;
    private ButtonBase cancelButton;
    private TaskCallback<?, R, P> callBack;
    private P callBackParameter;
    private Consumer<R> resultReceiver;

    /**  */
    private ProgressBinder<R, P> progressBinder;

    /**
     * Разрешаем отменять выполнение задачи
     * <p>
     * Если этот параметр установлен в {@code true} - отображаем кнопку отмены,
     * но в таком случае необходимо, для корректной работы кнопки отмены, проверять
     * в своем цикле отменилась ли задача.
     * </p>
     */
    public ProgressTaskExecutor<R, P> allowCancel(boolean allowCancel) {
        this.allowCancel = allowCancel;
        return this;
    }

    /**
     * Устанавливает кнопку отмены
     * <p>
     * Нужно только в том случае, если Вы установили {@link #allowCancel(boolean) allowCancel} в {@code true}
     * и выводите прогресс на своей {@code Pane}
     * </p>
     */
    public ProgressTaskExecutor<R, P> cancelButton(Button cancelButton) {
        this.cancelButton = cancelButton;
        return this;
    }

    /**
     * Заменяет progressBar на progressIndicator
     */
    public ProgressTaskExecutor<R, P> indicator(boolean indicator) {
        this.indicator = indicator;
        return this;
    }

    /**
     * Если Вы хотите отобразить прогресс в новом stage
     *
     * @param stage Текущий stage
     */
    public ProgressTaskExecutor<R, P> stage(Stage stage) {
        return this.stage(stage, "Progress", false);
    }

    /**
     * Если Вы хотите отобразить прогресс в новом stage
     *
     * @param stage Текущий stage
     * @param title Заголовок окна прогресса
     */
    public ProgressTaskExecutor<R, P> stage(Stage stage, String title) {
        return this.stage(stage, title, false);
    }

    /**
     * Если Вы хотите отобразить прогресс в новом stage
     *
     * @param stage       Текущий stage
     * @param title       Заголовок окна прогресса
     * @param showSuccess Показывать Alert после успешного завершения прогресса
     */
    public ProgressTaskExecutor<R, P> stage(Stage stage, String title, boolean showSuccess) {
        this.stageOption = new ProgressStageOption(stage, title, showSuccess);
        return this;
    }

    /**
     * Если Вы хотите отобразить прогресс на своей {@code Pane}
     *
     * @param pane {@code Pane} куда будет установлен прогресс
     */
    public ProgressTaskExecutor<R, P> pane(Pane pane) {
        this.pane = pane;
        return this;
    }

    /**
     * Если Вы хотите отобразить прогресс на своей {@code Pane}
     *
     * @param pane         {@code Pane} куда будет установлен прогресс
     * @param cancelButton Кнопка отмены задачи
     */
    public ProgressTaskExecutor<R, P> pane(Pane pane, Button cancelButton) {
        this.pane = pane;
        this.cancelButton = cancelButton;
        this.allowCancel = true;
        return this;
    }

    /**
     * В задаче пользуйтесь интерфейсом IProgressInfo для отображения статуса прогресса
     * <p>
     * <pre>
     * Устаревший метод, используйте {@link #callback(TaskCallback)}.
     * Передавайте в качестве аргумента ProgressCallback/MultiProgressCallback.
     * Пример: taskExecutor.callback((ProgressCallback<Void, Void>) CopyTaskController.this::copyTask)
     * </pre>
     *
     * @param callBack Задача для выполнения
     */
    @Deprecated
    public ProgressTaskExecutor<R, P> callback(ProgressAdapter.Callback<R, P> callBack) {
        this.callBack = callBack;
        return this;
    }

    /**
     * В задаче пользуйтесь интерфейсом IProgressInfo для отображения статуса прогресса
     * MultiProgressCallback работает только если прогресс будете отображаться в новом stage
     *
     * @param callBack Задача для выполнения
     */
    public ProgressTaskExecutor<R, P> callback(TaskCallback<?, R, P> callBack) {
        this.callBack = callBack;
        return this;
    }

    /** */
    public ProgressTaskExecutor<R, P> callbackParameter(P callBackParameter) {
        this.callBackParameter = callBackParameter;
        return this;
    }

    /**
     * Если нужно получить результат после успешного выполнения задачи
     */
    public ProgressTaskExecutor<R, P> resultReceiver(Consumer<R> resultReceiver) {
        this.resultReceiver = resultReceiver;
        return this;
    }

    /**
     * Запускает выполнение задачи с отображением прогресса
     *
     * @return Возвращает результат только в случае выполнения задачи
     * в отдельном {@code Stage}
     */
    public R execute() {

        if( progressBinder != null && !progressBinder.isDone() ) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Task hasn't done yet");
        }
        if (stageOption != null && pane != null) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Stage and Pane can't be combined");
        }
        if (stageOption == null && pane == null) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Stage or Pane must be set");
        }
        if (stageOption != null && stageOption.getParentStage() == null) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Parent stage can't be null");
        }
        if (callBack == null) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "'CallBack' must be set");
        }
        if (stageOption != null) {
            progressBinder = new ProgressBinder<>(stageOption, allowCancel);
            impl_execute();
            return progressBinder.getResult();
        } else {
            if (allowCancel && cancelButton != null) {
                progressBinder = new ProgressBinder<>(pane, true, cancelButton);
            } else {
                progressBinder = new ProgressBinder<>(pane);
            }
        }
        impl_execute();

        return null;
    }

    private void impl_execute() {
        if (progressBinder == null) return;
        if (resultReceiver != null) progressBinder.setResultReceiver(resultReceiver);
        if (indicator) progressBinder.setIndicator(true);

        progressBinder.execute(callBack, callBackParameter);
    }

    /**
     * @return Завершилась ли задача
     */
    public boolean isDone() {
        return progressBinder == null || progressBinder.isDone();
    }
}
