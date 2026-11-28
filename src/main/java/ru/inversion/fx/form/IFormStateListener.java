/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import ru.inversion.fx.form.action.JInvAction;





/**
 * Слушатель состояния формы
 * <p>
 * @author antonovdi, Sulimoff
 */
public interface IFormStateListener {

    /** */
    default StateEnum getState() { return stateProperty().get(); }

    /** */
    default void setState(StateEnum state) {

        if (state == StateEnum.ACTIVE) {
            FormWait wait = getFormWait();
            if (wait != null) {
                wait.reset();
            }
        }
        stateProperty().set(state);
    }

    /** */
    ObjectProperty<StateEnum> stateProperty( );

    /**
     * Добавить действие, держащее форму в состоянии WAIT (с колесом)
     */
    default void addWaitAction(JInvAction action) {
        FormWait wait = getFormWait();
        if (wait != null) {
            wait.add(action);
        }
    }

    /** Освобождает регистрацию, принадлежащую указанному действию. */
    default void removeWaitAction(JInvAction action) {
        FormWait wait = getFormWait();
        if (wait != null) {
            wait.remove(action);
        }
    }

    /**
     * Для автоматического WAIT форма возвращает один принадлежащий ей FormWait.
     * Пассивные реализации оставляют значение по умолчанию.
     */
    default FormWait getFormWait() {
        return null;
    }
    default String getStateText() { return stateTextProperty().get(); }

    default void setStateText(String state) { stateTextProperty().set(state);}

    StringProperty stateTextProperty( );
}
