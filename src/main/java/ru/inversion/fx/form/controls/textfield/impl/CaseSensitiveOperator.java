/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.textfield.impl;
import java.util.function.UnaryOperator;
import javafx.scene.control.TextFormatter;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.utils.S;

/**
 *
 * @author antonovdi
 */
public class CaseSensitiveOperator implements UnaryOperator<TextFormatter.Change> {

    private TextFormatter innerFormatter;

    @Override
    public TextFormatter.Change apply(TextFormatter.Change change) {

        if (getInnerFormatter() != null) {
            change = ((UnaryOperator<TextFormatter.Change>) getInnerFormatter().getFilter()).apply(change);
        }

        if (change!=null && change.isAdded() && S.isNotNullOrEmpty(change.getText()) && change.getControl() != null) {

            RegisterEnum caseMode = null;

            if ( change.getControl() instanceof JInvTextField ){
                caseMode = ((JInvTextField) change.getControl()).getCaseSensitiveMode();
            }
            else
            if ( change.getControl() instanceof JInvTextArea ){
                caseMode = ((JInvTextArea) change.getControl()).getCaseSensitiveMode();
            }

            if (caseMode != null) {
                if (caseMode.equals(RegisterEnum.LOWER_CASE)) {
                    change.setText(change.getText().toLowerCase());
                } else if (caseMode.equals(RegisterEnum.UPPER_CASE)) {
                    change.setText(change.getText().toUpperCase());
                }
            }
        }

        return change;
    }

    public TextFormatter getInnerFormatter() {
        return innerFormatter;
    }

    public void setInnerFormatter(TextFormatter innerFormatter) {
        this.innerFormatter = innerFormatter;
    }
}
