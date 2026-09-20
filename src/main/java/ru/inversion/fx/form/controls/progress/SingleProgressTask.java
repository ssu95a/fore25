package ru.inversion.fx.form.controls.progress;

import javafx.application.Platform;
import ru.inversion.utils.S;

/**
 * CallBack для однозадачной задачи
 *
 */
public final class SingleProgressTask<R, P>
        extends AbstractProgressTask<IProgressInfo, R, P>
        implements IProgressInfo
{
    /** */
    SingleProgressTask( TaskCallback<IProgressInfo, R, P> callback, P parameter) {
        super(callback, parameter);
    }

    @Override
    public R invoke() throws Exception {
        return callback.call(this, parameter);
    }

    /** */
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
    public void process(long current, long max) {
        updateProgress(current, max);
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
        } else {
            Platform.runLater(() -> {
                updateProgress(getTotalWork(), getTotalWork());
            });
        }
    }

    @Override
    public boolean isCancelled() {
        return super.isCancelled();
    }

}
