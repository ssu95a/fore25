package ru.inversion.fx.form.controls.progress;

@Deprecated
public class ProgressAdapter {

    private ProgressAdapter() {
    }

    @Deprecated
    @FunctionalInterface
    public interface Callback<R, V> extends ProgressCallback<R, V> {
        R call(IProgressInfo pInfo, V parameter) throws Exception;
    }
}
