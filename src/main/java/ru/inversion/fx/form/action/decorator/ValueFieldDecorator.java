/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action.decorator;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Control;
import ru.inversion.fx.form.controls.IStateControl.State;
import ru.inversion.fx.form.controls.JInvCalendar;

/**
 *
 * @author antonovdi
 */
public class ValueFieldDecorator extends ReadOnlyObjectWrapper<State> {

    public ValueFieldDecorator(Object bean) {
        super(bean, "state", State.NULL);
    }

    @Override
    public void set(State v) {

        if (get() != v) {

            if (getBean() instanceof JInvCalendar) {
                if (v == State.ERROR) {
                    ((JInvCalendar) getBean()).getEditor().getStyleClass().add("jinv-text-error");
                } else {
                    ((JInvCalendar) getBean()).getEditor().getStyleClass().remove("jinv-text-error");
                }
            } else if (v == State.ERROR) {
                ((Control) getBean()).getStyleClass().add("jinv-text-error");
            } else {
                ((Control) getBean()).getStyleClass().remove("jinv-text-error");
            }
            super.set(v);
        }
    }

}
