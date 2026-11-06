/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form;

/**
 * Класс-контейнер для хранения обьектов завершения работы контроллера
 * @author antonovdi
 */
public class ResultForm<T> {
    
    private JInvFXFormController.FormReturnEnum formReturn;
    private JInvFXFormController<T> controller;
    private Throwable exception;

    public AbstractBaseController.FormReturnEnum getFormReturn() {
        return formReturn;
    }
    public void setFormReturn(AbstractBaseController.FormReturnEnum formReturn) {
        this.formReturn = formReturn;
    }

    public JInvFXFormController<T> getController() {
        return controller;
    }
    public void setController(JInvFXFormController<T> controller) {
        this.controller = controller;
    }

    public Throwable getException() {
        return exception;
    }
    public void setException(Throwable exception) {
        this.exception = exception;
    }

}
