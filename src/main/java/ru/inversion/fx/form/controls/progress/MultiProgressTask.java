package ru.inversion.fx.form.controls.progress;

import javafx.application.Platform;
import javafx.beans.property.*;
import ru.inversion.utils.S;

import java.util.concurrent.atomic.AtomicReference;

public final class MultiProgressTask<R, P> extends AbstractProgressTask<IMultiProgressInfo, R, P>
        implements IMultiProgressInfo {

    private final DoubleProperty innerProgress = new SimpleDoubleProperty(this, "innerProgress", -1);
    private final StringProperty innerMessage = new SimpleStringProperty(this, "innerMessage", "");
    private AtomicReference<ProgressUpdate> innerProgressUpdate = new AtomicReference<>();
    private AtomicReference<String> innerMessageUpdate = new AtomicReference<>();

    MultiProgressTask(TaskCallback<IMultiProgressInfo, R, P> callBack, P parameter) {
        super(callBack, parameter);
    }

    @Override
    public R invoke() throws Exception {
        return callback.call(this, parameter);
    }

    @Override
    public void before(String msg) {
        updateMessage(msg);
        updateProgress(-1, -1);
    }

    @Override
    public void begin(long min, long max, String msg) {
        updateProgress(min, max);
        updateMessage(S.isNullOrEmpty(msg) ? "..." : msg);
    }

    @Override
    public void process(long current, long max, String msg) {
        updateMessage(msg);
        updateProgress(current, max);
    }

    @Override
    public void updateMessage(String msg) {
        super.updateMessage(S.isNullOrEmpty(msg) ? "" : msg);
    }

    @Override
    public void end(String msg) {
        updateMessage(S.isNullOrEmpty(msg) ? "" : msg);
        if (Platform.isFxApplicationThread()) {
            updateProgress(getTotalWork(), getTotalWork());
            updateInnerProgress(getTotalWork(), getTotalWork());
        } else {
            Platform.runLater(() -> {
                updateProgress(getTotalWork(), getTotalWork());
                updateInnerProgress(getTotalWork(), getTotalWork());
            });
        }

    }

    @Override
    public void innerProcess(long current, long max, String msg) {
        updateInnerMessage(msg);
        updateInnerProgress(current, max);
    }

    @Override
    public void updateInnerMessage(String msg) {
        updateInnerProgressMessage(S.isNullOrEmpty(msg) ? "" : msg);
    }

    public final double getInnerProgress() {
        checkThread();
        return innerProgress.get();
    }

    private void setInnerProgress(double value) {
        checkThread();
        innerProgress.set(value);
    }

    public final ReadOnlyDoubleProperty innerProgressProperty() {
        checkThread();
        return innerProgress;
    }

    public final String getInnerMessage() {
        checkThread();
        return innerMessage.get();
    }

    public final ReadOnlyStringProperty innerMessageProperty() {
        checkThread();
        return innerMessage;
    }

    private void updateInnerProgressMessage(String message) {
        if (Platform.isFxApplicationThread()) {
            this.innerMessage.set(message);
        } else {
            if (innerMessageUpdate.getAndSet(message) == null) {
                Platform.runLater(() -> {
                    this.innerMessage.set(innerMessageUpdate.getAndSet(null));
                });
            }
        }
    }

    private void updateInnerProgress(long workDone, long max) {
        updateInnerProgress(((double) workDone), ((double) max));
    }

    private void updateInnerProgress(double workDone, double max) {
        if (Double.isInfinite(workDone) || Double.isNaN(workDone)) {
            workDone = -1;
        }

        if (Double.isInfinite(max) || Double.isNaN(max)) {
            max = -1;
        }

        if (workDone < 0) {
            workDone = -1;
        }

        if (max < 0) {
            max = -1;
        }

        // Clamp the workDone if necessary so as not to exceed max
        if (workDone > max) {
            workDone = max;
        }

        if (Platform.isFxApplicationThread()) {
            _updateInnerProgress(workDone, max);
        } else if (innerProgressUpdate.getAndSet(new ProgressUpdate(workDone, max)) == null) {
            Platform.runLater(() -> {
                final ProgressUpdate update = innerProgressUpdate.getAndSet(null);
                _updateInnerProgress(update.workDone, update.totalWork);
            });
        }
    }

    private void _updateInnerProgress(double workDone, double max) {
        if (workDone == -1) {
            setInnerProgress(-1);
        } else {
            setInnerProgress(workDone / max);
        }
    }

    private void checkThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Task must only be used from the FX Application Thread");
        }
    }

    private final static class ProgressUpdate {
        private final double workDone;
        private final double totalWork;

        private ProgressUpdate(double p, double m) {
            this.workDone = p;
            this.totalWork = m;
        }
    }
}
