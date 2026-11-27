/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.application.Platform;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;

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
        if (getParent() instanceof JInvTextField field
                && field.getSkin() instanceof JInvTextFieldButtonSkin skin && skin.getButton() == this) {
            field.selectInnerButton(innerButton);
            return;
        }
        //JAVAKERNEL-1550: Игнорим, LOVButton приоритетнее
        if (this.innerButton instanceof JInvLOVButton && innerButton instanceof JInvFEButton) {
            return;
        }

        displayInnerButton(innerButton);
    }

    /** Подключает визуальные свойства уже выбранной кнопки. */
    public void displayInnerButton(JInvButton innerButton) {
        var previous = this.innerButton;
        boolean replacing = previous != null && previous != innerButton;
        boolean ownsAction = replacing && onActionProperty().isBound() && getOnAction() == previous.getOnAction();
        this.innerButton = innerButton;

        onActionProperty().unbind();
        if (ownsAction) {
            setOnAction(null);
        }
        if (replacing && previous.hasProperties() && previous.getProperties().get(Controls.CONTROL_PARENT) == this) {
            previous.getProperties().remove(Controls.CONTROL_PARENT);
        }
        visibleProperty().unbind();
        graphicProperty().unbind();
        textProperty().unbind();
        disableProperty().unbind();
        visibleProperty().set(false);
        tooltipProperty().unbind();

        if (innerButton != null) {
            Platform.runLater(() -> {
                if (this.innerButton != innerButton) {
                    return;
                }
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
