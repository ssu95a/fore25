package ru.inversion.fx.form.controls.skin;

import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.util.StringConverter;
import javafx.scene.control.ComboBox;

public class JInvComboBoxListViewSkin<T> extends ComboBoxListViewSkin<T> {

    /** */
    public JInvComboBoxListViewSkin( ComboBox comboBox ) {
        super(comboBox);
    }

    /** Переносит значение из editor в value*/
    public void forceCommitValue()
    {
        ComboBox<T> comboBox = (ComboBox<T>) getSkinnable();
        if (!comboBox.isEditable())
            return;

        StringConverter<T> converter = comboBox.getConverter();
        if (converter == null)
            return;

        T oldValue = comboBox.getValue();
        T value = oldValue;
        String text = comboBox.getEditor().getText();
        if (oldValue == null && (text == null || text.isEmpty())) {
            value = null;
        } else {
            try {
                value = converter.fromString(text);
            } catch (Exception ex) {
                // Сохраняем прежнее значение при ошибке преобразования, как в старом skin.
            }
        }

        if ((value != null || oldValue != null) && (value == null || !value.equals(oldValue)))
            comboBox.setValue(value);

        comboBox.cancelEdit();
    }
}
