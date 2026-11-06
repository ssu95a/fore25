/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.skin;

import com.sun.javafx.scene.control.skin.TextFieldSkin;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.controls.JInvTimeField;
import ru.inversion.icons.enums.FontAwesome;

/**
 *
 * @author antonovdi
 */
public class JInvTimeSkin extends TextFieldSkin {

    private final Button buttonUp;
    private final Button buttonDown;
    private VBox box;
    private PseudoClass focusedButton = PseudoClass.getPseudoClass("focused");
    boolean flagChange = true;

    private static Double rightInsect = null;

    public JInvTimeSkin(JInvTimeField fieldTime) {
        super(fieldTime);
        buttonUp = (Button) ActionFactory.createButton(FontAwesome.fa_caret_up, (ActionEvent event) -> {
            fieldTime.increaseTime();
        });

        if (buttonUp.getGraphic() != null && buttonUp.getGraphic() instanceof Label) {
            ((Label) buttonUp.getGraphic()).getStyleClass().add("label_in_jinvtime");
        }
        buttonUp.setCursor(Cursor.DEFAULT);
        buttonUp.setFocusTraversable(false);

        buttonDown = (Button) ActionFactory.createButton(FontAwesome.fa_caret_down, (ActionEvent event) -> {
            fieldTime.decreaseTime();
        });

        buttonDown.setCursor(Cursor.DEFAULT);
        buttonDown.setFocusTraversable(false);
        if (buttonDown.getGraphic() != null && buttonDown.getGraphic() instanceof Label) {
            ((Label) buttonDown.getGraphic()).getStyleClass().add("label_in_jinvtime");
        }
        box = new VBox(buttonUp, buttonDown);
        box.setSpacing(0);
//        box.getStyleClass().add("button_in_textfield");

        fieldTime.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                buttonDown.pseudoClassStateChanged(focusedButton, true);
                buttonUp.pseudoClassStateChanged(focusedButton, true);
            } else {
                buttonDown.pseudoClassStateChanged(focusedButton, false);
                buttonUp.pseudoClassStateChanged(focusedButton, false);
            }
        });

        box.disableProperty().bind(fieldTime.disableProperty());
        box.fillWidthProperty();
        super.getChildren().add(box);
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {

        Insets ins = getSkinnable().getInsets();
        if (rightInsect == null) {
            rightInsect = ins.getRight();
        }

        double H = h + ins.getTop() + ins.getBottom();

        final double baselineOffset = getSkinnable().getBaselineOffset();
        Insets insectWithButton = new Insets(ins.getTop(), 0, ins.getBottom(), ins.getLeft());
        getSkinnable().setPadding(insectWithButton);

        double buttonHeight = (H) / 2;
        buttonUp.setPrefHeight(buttonHeight);
        buttonUp.setMinHeight(buttonHeight);
        buttonUp.setMaxHeight(buttonHeight);
        buttonUp.setPrefWidth(H);
        buttonUp.setMaxWidth(H);
        buttonUp.setMinWidth(H);

        buttonDown.setPrefHeight(buttonHeight);
        buttonDown.setMinHeight(buttonHeight);
        buttonDown.setMaxHeight(buttonHeight);
        buttonDown.setPrefWidth(H);
        buttonDown.setMaxWidth(H);
        buttonDown.setMinWidth(H);

        box.setPrefHeight(H);
        box.setMaxHeight(H);
        box.setPrefWidth(H);
        box.setMaxWidth(H);
        box.setMinWidth(H);

        super.layoutChildren(x, y, w - H - rightInsect, h);
        super.layoutInArea(box, x + w - H, y - ins.getTop(), H, H - ins.getBottom(), baselineOffset, HPos.RIGHT, VPos.TOP);

    }
}
