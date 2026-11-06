/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author antonovdi
 */
public abstract class JInvValidator<T> implements IStateValidator<T>{

    protected BooleanProperty disabled = new SimpleBooleanProperty(false);
    
    @Override
    public void setDisable(boolean val) {
        disabled.set(val);
    }

    @Override
    public boolean isDisabled() {
        return disabled.get();
    }

    @Override
    public abstract Result validate(T value) ;
    
}
