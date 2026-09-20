/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import javafx.stage.Stage;

/**
 *
 * @author antonovdi
 */
public class PopOverListener implements ChangeListener<Boolean> {

    private Control control;

    public PopOverListener(Control c) {
        this.control = c;
    }

    @Override
    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

        if (JInvValidTooltip.isShowed() && oldValue && !newValue) {
            {
                JInvValidTooltip.closeValidTooltip();
                if (control != null && control.getScene() != null && control.getScene().getWindow() != null && control.getScene().getWindow() instanceof Stage) {
                    control.getScene().getWindow().focusedProperty().removeListener(PopOverListener.this);
                }
                if (control != null) {
                    control.focusedProperty().removeListener(PopOverListener.this);
                }
            }
        }
    }
}
