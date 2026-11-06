/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.textfield.impl;

import javafx.scene.control.TextFormatter;

/**
 *
 * @author antonovdi
 */
public class CaseSensitiveFormatter extends TextFormatter {

    private CaseSensitiveOperator operator;

    public CaseSensitiveFormatter(CaseSensitiveOperator operator) {
        super(operator);
        this.operator = operator;
    }

    public TextFormatter getInnerTextFormatter() {
        return operator.getInnerFormatter();
    }

    public void setInnerTextFormatter(TextFormatter innerTextFormatter) {
        operator.setInnerFormatter(innerTextFormatter);
    }

}
