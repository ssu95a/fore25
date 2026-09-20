/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Control;
import ru.inversion.fx.form.controls.IValidatableByChangeValueControl;

import static ru.inversion.fx.form.valid.ValidMan.setControlValueChanged;

/**
 * Слушатель меняет состояние компонента на измененное.
 * Применяется в валидации. Обратно состояние выставляет FocusListener в случае успешной валидации
 *
 * @author antonovdi
 */
public class ValidInvalidationListener implements ChangeListener<Object> {

    protected Node node;
    protected ValidMan validMan;

    public ValidInvalidationListener(Node control, ValidMan validMan) {
        this.node = control;
        this.validMan = validMan;
    }

    public ValidInvalidationListener(Node control) {
        this.node = control;
    }

//    @Override
//    public void invalidated(Observable observable) {
//
//        if (node != null) {
//            setControlValueChanged(node, true);
//        }
//    }
    @Override
    public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
        if( node != null )
        {
            setControlValueChanged( node, true);

            //для компонентов, которые должны поддерживать запуск валидации, калькуляции сразу при изменении значения
            if (validMan != null && node instanceof IValidatableByChangeValueControl && node instanceof Control) {
                validMan.validateControl((Control)node);
            }
        }
    }

}
