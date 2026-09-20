/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.utils.S;

/**
 * Параллельное действие (вне потока JavaFX), во время выполнения на форме крутится колесо ожидания
 * @author antonovdi, foma
 */
public class JInvParallelAction extends JInvAction {
    /**
     Параллельное действие (вне потока JavaFX), во время выполнения на форме крутится колесо ожидания

     @param handler
     @param formStateListener
     */
    public JInvParallelAction(EventHandler<ActionEvent> handler, IFormStateListener formStateListener) {
        this(handler, formStateListener, S.EMPTY_STRING);
    }

    /**
     Параллельное действие (вне потока JavaFX), во время выполнения на форме крутится колесо ожидания

     @param message Сообщение для показа над крутящимся колесом ожидающей формы
     */
    public JInvParallelAction(EventHandler<ActionEvent> handler, IFormStateListener formStateListener, String message) {
        this(handler, formStateListener, new SimpleStringProperty( message ));
    }

    /**
     @param messageProperty Проперть с текстом для показа над крутящимся колесом ожидающей формы
     */
    public JInvParallelAction(EventHandler<ActionEvent> handler, IFormStateListener formStateListener, StringProperty messageProperty) {
        super();
        setParallel(true);
        setHandler(handler);
        setFormStateListener(formStateListener);
        setStateMessageProperty(messageProperty);
    }

    /**
     *
     * @param handler
     * @param formStateListener
     * @param messageProperty  Свойство, которое будет связано со свойством message у класса Task. Будет доступно внутри таска через метод updateMessage
     */
    public JInvParallelAction(TaskHandler handler, IFormStateListener formStateListener, StringProperty messageProperty) {
        super();
        setParallel(true);
        setHandler(handler);
        setFormStateListener(formStateListener);
        setMessageProperty(messageProperty);
    }

}
