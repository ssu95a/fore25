/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import ru.inversion.fx.form.action.JInvAction.JInvTask;

import java.util.function.BiConsumer;

/**
 * Обработчик для использования в параллельных задачах. Понадобился в момент необходимости обращения к таску, например для вызова метода updateMessage
 *
 * @author antonovdi
 */
@FunctionalInterface
public interface TaskHandler extends EventHandler<ActionEvent>, BiConsumer<ActionEvent, JInvTask> {

    default void handle(ActionEvent event) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public  void accept(ActionEvent t, JInvTask u);

}
