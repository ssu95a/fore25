package ru.inversion.fx.form.controls.filter;

/**
 *
 * @author perov
 */
public interface IFilterAction {
    /**
     * Показать окно фильтра
     */
    void showFilterAction();
    
    /**
     * Применить фильтр
     */
    void executeFilterAction();
    
    /**
     * Сохранить текущий фильтр
     */
    void saveFilterAction();
    
    /**
     * Фильтр для отмеченных записей
     */
    void markFilterAction();

    /**
     * Очистить фильтр
     */
    void clearFilterAction();
    
    /**
     * Настройка фильтров
     */
    void settingsFilterAction();
    
}
