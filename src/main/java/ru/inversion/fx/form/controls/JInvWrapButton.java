/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.application.Platform;
import ru.inversion.fx.form.lov.JInvLOVButton;

/**
 *
 * @author antonovdi
 */
public class JInvWrapButton extends JInvChoiceButton {

    private JInvButton innerButton;

    public JInvButton getInnerButton() {
        return innerButton;
    }

    public void setInnerButton(JInvButton innerButton) {
        //JAVAKERNEL-1550: Игнорим, LOVButton приоритетнее
        if (this.innerButton instanceof JInvLOVButton && innerButton instanceof JInvFEButton) {
            return;
        }

        this.innerButton = innerButton;

        onActionProperty().unbind();
        visibleProperty().unbind();
        graphicProperty().unbind();
        textProperty().unbind();
        disableProperty().unbind();
        visibleProperty().set(false);
        tooltipProperty().unbind();

        if (innerButton != null) {
            Platform.runLater(() -> {
                innerButton.setProperty( Controls.CONTROL_PARENT, this );
                onActionProperty().bind(innerButton.onActionProperty());
                visibleProperty().bind(innerButton.visibleProperty());
                graphicProperty().bind(innerButton.graphicProperty());
                textProperty().bind(innerButton.textProperty());
                disableProperty().bind(innerButton.disableProperty());
                tooltipProperty().bind(innerButton.tooltipProperty());
            });
        }
    }

    public JInvWrapButton() {
    }

    public JInvWrapButton(String text) {
        super(text);
    }

}
