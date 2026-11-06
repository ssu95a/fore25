package ru.inversion.fx.form.controls.progress;

import javafx.stage.Stage;
import ru.inversion.fx.app.Tags;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Класс предназначенный для нескольких задач к которым нужно
 * отобразить прогресс их выполнения.
 *
 * Параллельный набор задач к выполнению
 *
 * Стартовый класс
 *
 */
public class MultiProgressTaskExecutor {

    private final Set<ProgressJob> progressJobList = new HashSet<>();

    private String title;

    private Stage parentStage;

    private boolean stopAllIfFailed;

    private boolean hideInnerProgressOnFinish;
    private boolean closeOnCancel;
    
    private boolean closeIfAllSuccess;

    private ProgressHandler<GeneralProgressCounter> finishHandler;

    public MultiProgressTaskExecutor() {
    }

    public MultiProgressTaskExecutor(String title, Stage parentStage) {
        this(title, parentStage, false);
    }

    public MultiProgressTaskExecutor(String title, Stage parentStage, boolean stopAllIfFailed) {
        this.title = title;
        this.parentStage = parentStage;
        this.stopAllIfFailed = stopAllIfFailed;
    }

    public MultiProgressTaskExecutor addTask(ProgressJob progressCallback) {
        progressJobList.add(progressCallback);
        return this;
    }

    public MultiProgressTaskExecutor addAllTask(Collection<ProgressJob> progressCallbackCollection) {
        progressJobList.addAll(progressCallbackCollection);
        return this;
    }

    public MultiProgressTaskExecutor addAllTask(ProgressJob... progressJobs) {
        progressJobList.addAll(Arrays.asList(progressJobs));
        return this;
    }

    public Stage getParentStage() {
        return parentStage;
    }

    public MultiProgressTaskExecutor setParentStage(Stage parentStage) {
        this.parentStage = parentStage;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public MultiProgressTaskExecutor setTitle(String title) {
        this.title = title;
        return this;
    }

    public boolean isStopAllIfFailed() {
        return stopAllIfFailed;
    }

    public void setStopAllIfFailed(boolean stopAllIfFailed) {
        this.stopAllIfFailed = stopAllIfFailed;
    }

    public boolean isHideInnerProgressOnFinish() {
        return hideInnerProgressOnFinish;
    }

    public MultiProgressTaskExecutor setHideInnerProgressOnFinish(boolean hideInnerProgressOnFinish) {
        this.hideInnerProgressOnFinish = hideInnerProgressOnFinish;
        return this;
    }

    public ProgressHandler<GeneralProgressCounter> getFinishHandler() {
        return finishHandler;
    }

    public MultiProgressTaskExecutor setFinishHandler(ProgressHandler<GeneralProgressCounter> finishHandler) {
        this.finishHandler = finishHandler;
        return this;
    }

    public boolean isCloseOnCancel() {
        return closeOnCancel;
    }

    public void setCloseOnCancel(boolean closeOnCancel) {
        this.closeOnCancel = closeOnCancel;
    }
    
    public boolean isCloseIfAllSuccess() {
        return closeIfAllSuccess;
    }

    public void setCloseIfAllSuccess(boolean closeIfAllSuccess) {
        this.closeIfAllSuccess = closeIfAllSuccess;
    }

    private MultiProgressBinder multiProgressBinder;

    public void execute() {
        if (multiProgressBinder != null && !multiProgressBinder.isDone()) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Task hasn't done yet");
        }
        final MultiProgressConfig multiProgressConfig = MultiProgressConfig.newBuilder()
                .setStageOption(new ProgressStageOption(parentStage, title))
                .setStopAllIfFailed(stopAllIfFailed)
                .setHideInnerProgressOnFinish(hideInnerProgressOnFinish)
                .setFinishGeneralProgressHandler(finishHandler)
                .setCloseOnCancel(closeOnCancel)
                .setCloseIfAllSuccess(closeIfAllSuccess)
                .build();
        multiProgressBinder = new MultiProgressBinder(progressJobList, multiProgressConfig);
        multiProgressBinder.execute();
    }


}
