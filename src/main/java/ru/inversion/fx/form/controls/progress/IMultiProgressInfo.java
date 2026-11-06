package ru.inversion.fx.form.controls.progress;

public interface IMultiProgressInfo extends IProgressInfo {

    /**
     * Устаналивает текущий и максимальный прогресс progressBar'а
     * для внутреннего progressBar'а
     *
     * @param current Текущий прогресс
     * @param max     Максимальный прогресс
     */
    default void innerProcess(long current, long max) {
        innerProcess(current, max, "");
    }

    /**
     * {@link #innerProcess(long, long)}
     */
    void innerProcess(long current, long max, String msg);

    /**
     * Обновить сообщение рядом с внутренним progressBar'ом.
     *
     * @param msg Сообщение рядом с progressBar'ом
     */
    void updateInnerMessage(String msg);

}
