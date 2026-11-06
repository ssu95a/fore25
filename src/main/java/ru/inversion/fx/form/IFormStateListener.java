/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.utils.S;

import java.util.Optional;
import java.util.Set;

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
            //Если кто-то явно ставит state.ACTIVE, очищаем список действий, требующих колесо
            getWaitActions().ifPresent(Set::clear);
        }

        stateProperty().set(state);
    }

    /** */
    ObjectProperty<StateEnum> stateProperty( );

    /**
     * Добавить действие, держащее форму в состоянии WAIT (с колесом)
     */
    default void addWaitAction(JInvAction action){
        Optional<Set<JInvAction>> waitActions = getWaitActions();
        if (!waitActions.isPresent() || action == null){
            return;
        }
        if (action.getStateMessageProperty() != null){
            stateTextProperty().bind(action.getStateMessageProperty());
        }
        waitActions.get().add(action);
        if (getState() == StateEnum.ACTIVE){
            setState(StateEnum.WAIT);
        }
    }

    /**
     * Убрать действие, держащее форму в состоянии WAIT (с колесом)
     * Если таковых не осталось, форма переходит в состояние ACTIVE (без колеса)
     */
    default void removeWaitAction(JInvAction action){
        Optional<Set<JInvAction>> waitActions = getWaitActions();
        if (!waitActions.isPresent()){
            return;
        }
        stateTextProperty().unbind();
        waitActions.get().remove(action);
        if (waitActions.get().isEmpty() && getState() == StateEnum.WAIT){
            setState(StateEnum.ACTIVE);
            setStateText( S.EMPTY_STRING );
        }
    }

    /**
     * Набор действий, держащих форму в состоянии WAIT (с колесом)
     */
    default Optional<Set<JInvAction>> getWaitActions(){return Optional.empty();}

    default String getStateText() { return stateTextProperty().get(); }

    default void setStateText(String state) { stateTextProperty().set(state);}

    StringProperty stateTextProperty( );
}
