package ru.inversion.fx.form.controls.progress;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.IDynamicProgressControl;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvProgressInfoPane;
import ru.inversion.fx.form.controls.JInvProgressPane;

import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Класс предназначенный для взаимодействия с {@link ProgressService}
 */
public class ProgressBinder<R, P> {

    // i18n
    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");
    private IDynamicProgressControl progressPane;
    private JInvProgressPane customProgressPane;
    private ProgressStageOption stageOption;
    private Pane parentPane;
    private TaskCallback<?, R, P> callback;
    private P parameter;
    private boolean allowCancel;
    private ButtonBase stopBtn;
    private boolean indicator;
    private Consumer<R> resultReceiver;
    private Stage currentStage;
    private ProgressService<R, P> progressService;

    ProgressBinder(ProgressStageOption stageOption) {
        this(stageOption, false);
    }

    ProgressBinder(ProgressStageOption stageOption, boolean allowCancel) {
        this.stageOption = Objects.requireNonNull(stageOption, "StageOption can't be null");
        this.allowCancel = allowCancel;
    }

    ProgressBinder(Pane parentPane) {
        this(parentPane, false, null);
    }

    ProgressBinder(Pane parentPane, boolean allowCancel, ButtonBase stopBtn) {
        this.parentPane = Objects.requireNonNull(parentPane, "Parent pane can't be null");
        this.allowCancel = allowCancel;
        this.stopBtn = stopBtn;
    }

    public Pane getParentPane() {
        return parentPane;
    }

    public void setParentPane(Pane parentPane) {
        this.parentPane = parentPane;
    }

    public Stage getParentStage() {
        return stageOption.getParentStage();
    }

    public ButtonBase getStopBtn() {
        return stopBtn;
    }

    public void setStopBtn(Button stopBtn) {
        this.stopBtn = stopBtn;
    }

    public boolean isIndicator() {
        return indicator;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    public Consumer<R> getResultReceiver() {
        return resultReceiver;
    }

    public void setResultReceiver(Consumer<R> resultReceiver) {
        this.resultReceiver = resultReceiver;
    }

    public IDynamicProgressControl getProgressPane() {
        return progressPane;
    }

    public IDynamicProgressControl getCustomProgressPane() {
        return customProgressPane;
    }

    public void setCustomProgressPane(JInvProgressPane customProgressPane) {
        this.customProgressPane = customProgressPane;
    }

    public void execute(TaskCallback<?, R, P> callback, P parameter) {
        this.callback = callback;
        this.parameter = parameter;

        // Если пользователь присвоил свой progress pane
        if (customProgressPane != null) {
            startProcess();
            return;
        }

        // Определяем будем ли показывать прогресс в новом stage
        if( stageOption != null )
        {
            if (callback instanceof MultiProgressCallback) {
                progressPane = new JInvProgressInfoPane(true);
            } else {
                progressPane = new JInvProgressInfoPane();
            }

            if(indicator)
                progressPane.toIndicator();

            showStageProcess();

        } else if (parentPane != null)
        {
            progressPane = new JInvProgressPane();

            if (indicator)
                progressPane.toIndicator();

            showNodeProcess();
        }
    }

    public R getResult() {
        if (progressService != null) {
            return progressService.getResult();
        }
        return null;
    }

    public boolean isDone() {
        return progressService == null || progressService.isDone();
    }

    private void showStageProcess() {

        if (!(progressPane instanceof JInvProgressInfoPane)) {
            throw new IllegalStateException(String.format("Can't initialize stage. Invalid progress pane type: %s",progressPane));
        }

        // Формируем stage
        currentStage = new Stage();
        currentStage.setTitle(stageOption.getTitle() != null ? stageOption.getTitle() : "Progress");
        currentStage.initStyle(StageStyle.TRANSPARENT);
        // Переносим title stag'а в titlePane, так как stage сделали невидимым
        ((JInvProgressInfoPane) progressPane).setTitleText(stageOption.getTitle());
        currentStage.initOwner(stageOption.getParentStage());
        currentStage.initModality(Modality.WINDOW_MODAL);
        currentStage.setResizable(false);

        final double sceneHeight;
        if (allowCancel) {
            if (((JInvProgressInfoPane) progressPane).isInnerProgress()) {
                sceneHeight = 200;
            } else {
                sceneHeight = 150;
            }
            stopBtn = ((JInvProgressInfoPane) this.progressPane).getCancelBtn();
        } else {
            sceneHeight = 150;
            ((JInvProgressInfoPane) progressPane).hideCancelButton();
        }

        final Scene scene = new Scene(((Parent) progressPane), 350, sceneHeight, Color.WHITE);
        scene.setFill(Color.TRANSPARENT);
        currentStage.setScene(scene);

        currentStage.setOnShown(event -> startProcess()); // После отображения stage запускаем процесс
        if (allowCancel) {
            currentStage.setOnCloseRequest(event -> progressService.stop());
        }

        // Делаем панельку перемещаемой
        final AtomicReference<Double> x = new AtomicReference<>();
        final AtomicReference<Double> y = new AtomicReference<>();
        ((JInvProgressInfoPane) progressPane).getTitlePane().setOnMousePressed(mouseEvent -> {
            x.set(currentStage.getX() - mouseEvent.getScreenX());
            y.set(currentStage.getY() - mouseEvent.getScreenY());
        });

        ((JInvProgressInfoPane) progressPane).getTitlePane().setOnMouseDragged(mouseEvent -> {
            currentStage.setX(mouseEvent.getScreenX() + x.get());
            currentStage.setY(mouseEvent.getScreenY() + y.get());
        });

        currentStage.showAndWait();
    }

    /** Отображение индикатора на панели */
    private void showNodeProcess() {

        final Node progressPane = (Node) this.progressPane;
        final ObservableList<Node> parentPaneChildren = parentPane.getChildren();

        parentPaneChildren.add(progressPane);

        if (allowCancel && stopBtn == null)
        {
            stopBtn = new JInvButton(FORE.getString("CANCEL"));

            if (!parentPaneChildren.contains(stopBtn)) {
                parentPane.getChildren().addAll(stopBtn);
            }
            return;
        }

        startProcess();
    }

    private void startProcess() {
        AbstractProgressTask<?, R, P> abstractProgressTask = null;
        if (callback instanceof ProgressCallback) {
            abstractProgressTask = new SingleProgressTask<R, P>(((ProgressCallback<R, P>) callback), parameter);
        } else if (callback instanceof MultiProgressCallback) {
            abstractProgressTask = new MultiProgressTask<R, P>(((MultiProgressCallback<R, P>) callback), parameter);
        }

        if (abstractProgressTask == null)
            throw new IllegalArgumentException("Can't init task from callback. Wrong callback type: " + callback.getClass().getSimpleName());

        progressService = new ProgressService<>(abstractProgressTask,
                customProgressPane != null ? customProgressPane : progressPane, true);

        progressService.setSuccessHandler(taskResult -> {
            if (resultReceiver != null) resultReceiver.accept(taskResult);
            if (currentStage != null) {
                if (stageOption.isShowSuccess()) {
                    Alerts.info(currentStage, FORE.getString("PROGRESS_INFO_TITLE"), FORE.getString("PROGRESS_INFO_TEXT"));
                }
                currentStage.close();
            }
        });
        progressService.setCancelledHandler(taskResult -> {
            if (currentStage != null) {
                currentStage.close();
            }
        });
        progressService.setAllHandler(taskResult -> {
            // Удаляем pane/stage после завершения task'и
            if (parentPane != null) {
                if (progressPane instanceof Node) {
                    parentPane.getChildren().remove(((Node) progressPane));
                }
            }
            if (currentStage != null) currentStage.close();
        });
        if (allowCancel && stopBtn != null) {
            stopBtn.setOnAction(event -> progressService.stop());
        }
        progressService.start();
    }

}
