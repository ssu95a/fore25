package ru.inversion.fx.form.controls.progress;

import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Worker;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvLabel;
import ru.inversion.fx.form.controls.JInvMultiProgressInfoPane;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

class MultiProgressBinder {

    private static final int MAX_PROGRESS_STAGE_HEIGHT = 600;

    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");

    private final Collection<ProgressJob> progressJobList;

    private final List<ProgressServiceWrapper> progressServiceList;

    private final IntegerProperty runningJobCount = new SimpleIntegerProperty(0);

    private final IntegerProperty successJobCount = new SimpleIntegerProperty(0);

    private final IntegerProperty cancelJobCount = new SimpleIntegerProperty(0);

    private final IntegerProperty failJobCount = new SimpleIntegerProperty(0);

    private final JInvMultiProgressInfoPane progressPane;

    private final MultiProgressConfig multiProgressConfig;

    private JInvMultiProgressInfoPane.GeneralProgress generalProgress;

    MultiProgressBinder(Collection<ProgressJob> progressJobCollection, MultiProgressConfig multiProgressConfig) {
        this.progressJobList = Collections.unmodifiableCollection(progressJobCollection);
        this.progressServiceList = new ArrayList<>(progressJobList.size());
        this.progressPane = new JInvMultiProgressInfoPane();
        this.multiProgressConfig = multiProgressConfig;
    }

    public void execute() {
        // Строим stage с progressBar'ами
        final Stage stage = buildStage(progressJobList.size());

        // Инициализируем основной progressBar
        generalProgress = progressPane.getGeneralProgress();
        generalProgress.getCancelBtn().setOnAction(event -> {
            if (stopAllProgress()) {
                stage.close();
            }
        });
        generalProgress.getOkBtn().setOnAction(event -> {
            stage.close();
        });

        // Инициализируем все задачи пользователя
        final MultiProgressTask<Void, IntegerProperty> multiProgressTask =
                new MultiProgressTask<Void, IntegerProperty>(new GeneralProgressCallback(), runningJobCount);
        final ProgressService<Void, IntegerProperty> generalService =
                new ProgressService<>(multiProgressTask, generalProgress, true);

        boolean isOneCanBeStopped = false;

        for (ProgressJob progressJob : progressJobList) {
            // Если одну из задач можно отменить - показываем кнопку отмены рядом с общим progressbar'ом
            if (progressJob.isAllowCancel()) {
                isOneCanBeStopped = true;
            }

            // Инициализируем панель
            final JInvMultiProgressInfoPane.SecondaryProgress progressControl
                    = progressPane.addSecondaryProgress(progressJob.isIndicator(), progressJob.isAllowCancel());
            final JInvButton cancelButton = progressControl.getActionButton();
            final JInvLabel title = progressControl.getTitle();

            // В зависимости от привязанного callback'а скрываем дополнительный progressBar
            final boolean isMultiProgress = progressJob.getProgressTask().getCallback() instanceof MultiProgressCallback;
            if (!isMultiProgress) {
                progressControl.hideInnerProgress();
            }

            final ProgressService progressService =
                    new ProgressService(progressJob.getProgressTask(), progressControl, false);
            progressService.setSuccessHandler(progressJob.getSuccessHandler());
            progressService.setCancelledHandler(progressJob.getCancelHandler());
            progressService.setFailedHandler(progressJob.getFailedHandler());
            progressService.setAllHandler(progressJob.getAllHandler());

            title.setText(progressJob.getName());

            if (cancelButton.isVisible()) {
                cancelButton.setCursor(Cursor.HAND);
                cancelButton.setOnAction(event -> {
                    progressService.stop();
                });
                progressService.cancelledProperty().addListener((observable, oldValue, newValue) -> {
                    cancelButton.setOnAction(null); // Убираем нажатие по кнопке отмены
                });
            }

            // Слушатель на завершении выполнения задачи
            progressService.setFinishListener((progressTask) -> {
                progressControl.showButton();
                if (progressJob.isIndicator()) {
                    progressControl.hideProgress(); // Убираем колесо, так как задача завершилась
                }
                if (multiProgressConfig.isHideInnerProgressOnFinish()) {
                    progressControl.hideInnerProgress(true);
                }
                if (progressTask.getState() == Worker.State.SUCCEEDED) {
                    successJobCount.set(successJobCount.get() + 1);
                    cancelButton.setGraphic(IconFactory.getLabel(FontAwesome.fa_check, Color.web("#229551")));
                    cancelButton.setCursor(Cursor.DEFAULT);
                    if(multiProgressConfig.isCloseIfAllSuccess() && successJobCount.get() == progressJobList.size())
                    {
                        if (stopAllProgress()) {
                            stage.close();
                        }
                    }
                } else if (progressTask.getState() == Worker.State.CANCELLED) {
                    cancelJobCount.set(cancelJobCount.get() + 1);
                    cancelButton.getGraphic().getStyleClass().add("redTextFill");
                    cancelButton.setCursor(Cursor.DEFAULT);
                } else if (progressTask.getState() == Worker.State.FAILED) {
                    failJobCount.set(failJobCount.get() + 1);
                    cancelButton.setOnAction(event -> {
                        Platform.runLater(() -> {
                            JInvErrorService.handleException(stage, progressTask.getException());
                        });
                    });
                    final Label label = IconFactory.getLabel(FontAwesome.fa_warning, Color.web("#E74D4A"));
                    label.setTooltip(new Tooltip(FORE.getString("PROGRESS_INFO_SHOW_ERROR_TOOLTIP")));
                    cancelButton.setGraphic(label);
                    if (multiProgressConfig.isStopAllIfFailed()) {
                        stopAllProgress(true);
                    }
                }
                runningJobCount.set(runningJobCount.get() - 1);
            });

            runningJobCount.set(runningJobCount.get() + 1);
            progressServiceList.add(new ProgressServiceWrapper(progressJob, progressService));
        }

        if (!isOneCanBeStopped) {
            generalProgress.hideCancelButton();
        }

        stage.setOnShown(event -> {
            generalService.start();
            for (ProgressServiceWrapper progressServiceWrapper : progressServiceList) {
                progressServiceWrapper.getProgressService().start();
            }
        });
        stage.setOnCloseRequest(event -> {
            final boolean isAnyCanBeCancelled = progressServiceList.stream().anyMatch(progressServiceWrapper -> {
                return progressServiceWrapper.getProgressJob().isAllowCancel();
            });
            final boolean isNotAllDone = progressServiceList.stream().anyMatch(progressServiceWrapper -> {
                return !progressServiceWrapper.getProgressService().isDone();
            });
            if (isNotAllDone && isAnyCanBeCancelled) {
                final boolean yesNo = Alerts.yesNo(stage.getOwner(),
                        FORE.getString("PROGRESS_INFO_ALERT_TITLE"),
                        FORE.getString("PROGRESS_INFO_ALERT_HEAD"));
                if (yesNo) {
                    if (!stopAllProgress())
                        event.consume();
                } else {
                    event.consume();
                }
            } else if (isNotAllDone) {
                event.consume();
            }
        });
        stage.showAndWait();
    }

    private Stage buildStage(int progressJobCount) {
        final Stage stage = new Stage();
        stage.setTitle(multiProgressConfig.getStageOption().getTitle() != null ?
                multiProgressConfig.getStageOption().getTitle() : "Progress");
        if (multiProgressConfig.getStageOption().getParentStage() != null) {
            stage.initOwner(multiProgressConfig.getStageOption().getParentStage());
            stage.initModality(Modality.WINDOW_MODAL);
        }
        int height = (progressJobCount * 100) + 100;
        if (height >= MAX_PROGRESS_STAGE_HEIGHT) {
            height = MAX_PROGRESS_STAGE_HEIGHT;
        }
        Scene scene = new Scene(((Parent) progressPane), 700, height, Color.WHITE);
        stage.setScene(scene);

        return stage;
    }

    private boolean stopAllProgress() {
        return stopAllProgress(false);
    }

    /**
     * Останавливает все задачи
     *
     * @return true если удалось завершить все задачи
     */
    private boolean stopAllProgress(boolean ignoreAllowCancel) {
        final List<ProgressJob> noCancelList = new ArrayList<>();
        synchronized (progressServiceList) {
            for (ProgressServiceWrapper progressServiceWrapper : progressServiceList) {
                if (progressServiceWrapper.getProgressService().getState() == Worker.State.FAILED) continue;
                if (ignoreAllowCancel || progressServiceWrapper.getProgressJob().isAllowCancel()) {
                    progressServiceWrapper.getProgressService().stop();
                } else {
                    noCancelList.add(progressServiceWrapper.getProgressJob());
                }
            }
        }
        if (!noCancelList.isEmpty()) {
            final String jobNames = noCancelList.stream().map(ProgressJob::getName).collect(Collectors.joining(","));
            Alerts.info(null, FORE.getString("PROGRESS_INFO_ALERT_TITLE"),
                    FORE.getString("PROGRESS_INFO_ALERT_TEXT_NOT_STOP"), jobNames);
            return false;
        } else {
            return true;
        }
    }

    public boolean isDone() {
        return progressServiceList.isEmpty();
    }

    /**
     * Отображение общего прогресса для всех задач
     */
    private final class GeneralProgressCallback implements TaskCallback<IMultiProgressInfo, Void, IntegerProperty> {

        private final CountDownLatch countDownLatch = new CountDownLatch(1);

        @Override
        public Void call(IMultiProgressInfo progressInfo, IntegerProperty runningJobCount) throws Exception {
            progressInfo.before(FORE.getString("PROGRESS_INFO_INIT"));
            final int size = runningJobCount.get();
            progressInfo.begin(0, size, MessageFormat.format(FORE.getString("PROGRESS_INFO_PROCESS"), 0, size));
            final AtomicInteger count = new AtomicInteger();
            runningJobCount.addListener((observable, oldValue, newValue) -> {
                count.set(size - newValue.intValue());
                progressInfo.process(count.get(), size,
                        MessageFormat.format(FORE.getString("PROGRESS_INFO_PROCESS"), count.get(), size));
                if (count.get() >= size) {
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
            synchronized (progressServiceList) {
                progressServiceList.clear();
            }
            final String msg = MessageFormat.format(FORE.getString("PROGRESS_INFO_COMPLETE"),
                    successJobCount.get(),
                    failJobCount.get(),
                    cancelJobCount.get());
            generalProgress.changeCancelToOkBtn();
            progressInfo.end(msg);
            if (multiProgressConfig.getFinishGeneralProgressHandler() != null) {
                multiProgressConfig.getFinishGeneralProgressHandler().invoke(new GeneralProgressCounter(successJobCount.get(),
                        failJobCount.get(), cancelJobCount.get()));
            }

            // Сбрасываем значения
            successJobCount.set(0);
            failJobCount.set(0);
            cancelJobCount.set(0);
            return null;
        }
    }

    private final class ProgressServiceWrapper {

        private final ProgressJob progressJob;

        private final ProgressService progressService;

        ProgressServiceWrapper(ProgressJob progressJob, ProgressService progressService) {
            this.progressJob = progressJob;
            this.progressService = progressService;
        }

        ProgressJob getProgressJob() {
            return progressJob;
        }

        ProgressService getProgressService() {
            return progressService;
        }
    }
}
