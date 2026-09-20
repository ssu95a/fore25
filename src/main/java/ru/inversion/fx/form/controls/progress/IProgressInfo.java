package ru.inversion.fx.form.controls.progress;

/**
 * Интефейс для GUI уведомлений
 *
 */
public interface IProgressInfo {

    /**
     * Устанавливает progressIndicator в indeterminate.
     * Выводит ваше сообщение рядом с progressIndicator'ом
     *
     * @param msg Сообщение
     */
    void before(String msg);

    /**
     * Устанавливает минимальное и максимальное значение progressIndicator'а
     * Выводит ваше сообщение рядом с progressIndicator'ом
     *
     * @param min Минимальное значение progressIndicator'а
     * @param max Максимальное значение progressIndicator'а
     * @param msg Сообщение рядом с progressIndicator'ом
     */
    void begin(long min, long max, String msg);

    /**
     * Устаналивает текущий и максимальный прогресс progressIndicator'а
     * Используйте в цикле, для отображения прогресса
     *
     * @param current Текущий прогресс
     * @param max     Максимальный прогресс
     */
    default void process(long current, long max) {
        process(current, max, "");
    }

    /**
     * Устаналивает текущий и максимальный прогресс progressIndicator'а
     * Выводит ваше сообщение рядом с progressIndicator'ом
     * Используйте в цикле, для отображения прогресса
     *
     * @param current Текущий прогресс
     * @param max     Максимальный прогресс
     * @param msg     Сообщение рядом с progressIndicator'ом
     */
    void process(long current, long max, String msg);

    /**
     * Обновить сообщение рядом с прогресс баром.
     *
     * @param msg Сообщение рядом с progressIndicator'ом
     */
    void updateMessage(String msg);


    /**
     * Устанавливает сообщение рядом с progressIndicator'ом
     * и завершает индикатор прогресса
     *
     * @param msg Сообщение рядом с progressIndicator'ом
     */
    void end(String msg);

    /**
     * Проверяет изменился ли статус задачи на {@code CANCELLED}.
     * Нужно использовать в цикле, там где будете вызывать {@link #process(long, long, String) process()}
     * Возможно использование в тех случая, когда вы устанавливаете кнопку отмены задачи и для корректной остановки
     * вашего цикла - Вам необходимо делать проверку {@link #isCancelled() isCancelled()}
     *
     * @return Возвращает {@code true} если задача была отменена
     */
    boolean isCancelled();
}
