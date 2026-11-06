/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.Observable;
import ru.inversion.utils.S;
import ru.inversion.utils.scheck.JInvStringWorker;

import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;

/**
 *
 * @author antonovdi
 */
public class JInvRegExpTextField extends JInvTextField {

    private String regExp;
    private int regExpGroup;

    public int getRegExpGroup() {
        return regExpGroup;
    }

    public void setRegExpGroup(int regExpGroup) {
        this.regExpGroup = regExpGroup;
    }

    public String getRegExp() {
        return regExp;
    }

    public void setRegExp(String regExp) {
        this.regExp = regExp;
    }

    /**
     *
     */
    public JInvRegExpTextField() {
        this((String) null);
    }

    /**
     *
     */
    public JInvRegExpTextField(String arg0) {
        super(arg0);

        textProperty().addListener((Observable observable) -> {
            try {
                String text = getText();
                if (S.isNotNullOrEmpty(text) && S.isNotNullOrEmpty(regExp)) {
                    JInvStringWorker.INSTANCE().checkRegExp(regExp, regExpGroup, text, true);
                }

                if (getState() == ERROR) {
                    setState(S.isNullOrEmpty(text) ? State.NULL : State.VALUE);
                }

            } catch (Throwable th) {
                setState(State.ERROR);
            }
        });

    }
}
