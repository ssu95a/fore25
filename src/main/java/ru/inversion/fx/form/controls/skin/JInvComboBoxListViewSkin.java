package ru.inversion.fx.form.controls.skin;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.ComboBox;

public class JInvComboBoxListViewSkin<T> extends ComboBoxListViewSkin<T> {

    /** */
    public JInvComboBoxListViewSkin( ComboBox comboBox ) {
        super(comboBox);
    }

    /** Переносит значение из editor в value*/
    public void forceCommitValue()
    {
        setTextFromTextFieldIntoComboBoxValue();
    }
}
