package ru.inversion.fx.help.controller;
import ru.inversion.tc.TaskContext;

/**
 * Внутренний интерфейс 
 * @author perov
 * @version 1.0.0
 */
interface TabState {
    /**
     * Перерисовка содежимого
     * @param formName
     */
    void draw(String formName);
    
    /**
     * проверка доступа 
     * @param tc
     * @param code
     * @return 
     */
    boolean checkAccess(TaskContext tc, int code);
    
    void preDestroy();
    
}
