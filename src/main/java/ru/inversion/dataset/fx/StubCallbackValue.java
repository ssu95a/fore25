/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.dataset.fx;

import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Callback;

/**
 *
 * @author antonovdi
 */
public class StubCallbackValue<T> extends SimpleObjectProperty {

    private T pojoInstance = null;
    private Callback<T, ? extends Object> clbk;

    public StubCallbackValue(Callback<T, ? extends Object> clbk) {
        this.clbk = clbk;
    }

    /**
     *      */
    @Override
    public Object getValue() {
        if (pojoInstance == null || clbk == null) {
            return null;
        }
        return clbk.call(pojoInstance);
    }

    /**
     *      */
    public StubCallbackValue<T> setPojoInstance(T pojoInstance) {
        this.pojoInstance = pojoInstance;
        return this;
    }

    /**
     *      */
    @Override
    public void setValue(Object value) {
    }

}
