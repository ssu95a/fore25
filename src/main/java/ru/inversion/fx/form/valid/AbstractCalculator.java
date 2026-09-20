/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.scene.control.Control;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 *
 * @author antonovdi
 */
public abstract class AbstractCalculator<T> implements Calculator<T> {

    private ValidMan validMan;
    private static final ResourceBundle bundle = ResourceBundle.getBundle("valid");

    public void setValidMan(ValidMan validManager) {
        this.validMan = validManager;
    }

    /**
     * Возвращает коллекцию контролов, привязанных к данному калькулятору
     *
     * @return
     */
    public Optional<List<Control>> getControlsOptional() {
        Optional<List<Control>> result = Optional.empty();
        if (validMan != null) {
            result = Optional.ofNullable(validMan.getControlListByCalculator(this));
        }
        return result;
    }

    /**
     * Возвращает коллекцию контролов, привязанных к данному калькулятору
     *
     * @return
     * @throws IllegalArgumentException
     */
    public List<Control> getControls() {
        return getControlsOptional().orElseThrow(() -> new IllegalArgumentException(bundle.getString("NOT_BINDED_CONTROLS_TO_CALCULATOR")));
    }

    /**
     * Возвращает контрол, привязанных к данному калькулятору по индексу
     *
     * @param position
     * @return
     * @throws IllegalArgumentException
     */
    public Control getControl(int position) throws IllegalArgumentException {
        try {
            return getControlsOptional().orElseThrow(() -> new IllegalArgumentException(bundle.getString("NOT_BINDED_CONTROLS_TO_CALCULATOR"))).get(position);
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException(bundle.getString("NOT_BINDED_CONTROL_TO_CALCULATOR_BY_INDEX") + " " + position);
        }
    }

}
