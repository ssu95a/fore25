package ru.inversion.fx.form.controls.progress;

public class GeneralProgressCounter {

    private final int successCount;

    private final int failCount;

    private final int cancelCount;

    GeneralProgressCounter(int successCount, int failCount, int cancelCount) {
        this.successCount = successCount;
        this.failCount = failCount;
        this.cancelCount = cancelCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public int getCancelCount() {
        return cancelCount;
    }
}
