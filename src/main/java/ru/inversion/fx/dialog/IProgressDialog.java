package ru.inversion.fx.dialog;

import java.util.concurrent.Callable;

/**
 * Диалог для показа прогресса выполнения операции в отдельном потоке
 * @author perov
 * @param <V>
 */
public interface IProgressDialog<V> {
    
    /**
     * Показать диалог
     * @param title - заголовок окна
     */
    public void showDialog(String title);
    
    /**
     * Установка текста в текстовое поле
     * @param msg - текст
     */
    public void setText(String msg);
    
    /**
     * Установка значение прогресса
     * @param workDone - текущее значение прогресса
     * @param max - максимальное значение прогресса
     */
    public void setProgress(long workDone, long max);
    
    /**
     * Операция для выполнения в фотоновом потоке
     * @param clb 
     */
    public void setAction(Callable<V> clb);
    
    /**
     * Закрытие окна диалога
     */
    public void closeDialog();

}
