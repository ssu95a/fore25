/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.autocomplete;

import impl.org.controlsfx.autocompletion.AutoCompletionTextFieldBinding;
import javafx.util.Callback;
import javafx.util.StringConverter;
import ru.inversion.fx.form.controls.JInvComboBox;
import ru.inversion.utils.S;

import java.util.Collection;

/**
 *
 * @author antonovdi
 */
public class JInvAutoCompletionComboBoxBinding<T> extends AutoCompletionTextFieldBinding<T> {

    private JInvComboBox comboBox;

    public JInvAutoCompletionComboBoxBinding(JInvComboBox combo, Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        super(combo.getEditor(), suggestionProvider);
        this.comboBox = combo;
    }

    @Override
    protected void showPopup() {

        StringConverter<T> converter = comboBox.getConverter();
        String textFromEditor = comboBox.getEditor().getText();
        if (converter != null && comboBox.getValue() != null && S.isNotNullOrEmpty(textFromEditor)) {
            Object valueFromConverter = converter.fromString(textFromEditor);
            if (!comboBox.getValue().equals(valueFromConverter)) {
                super.showPopup();
            }
        }else{
           super.showPopup();
        }
    }

    @Override
    protected void completeUserInput(T completion) {
        super.completeUserInput(completion); //To change body of generated methods, choose Tools | Templates.
        setTextFromTextFieldIntoComboBoxValue();
    }

    private void setTextFromTextFieldIntoComboBoxValue() {
        if (comboBox.getEditor() != null) {
            StringConverter<T> c = comboBox.getConverter();
            if (c != null) {
                Object oldValue = comboBox.getValue();
                Object value = oldValue;
                String text = comboBox.getEditor().getText();

                // conditional check here added due to RT-28245
                if (oldValue == null && (text == null || text.isEmpty())) {
                    value = null;
                } else {
                    try {
                        value = c.fromString(text);
                    } catch (Exception ex) {
                        // Most likely a parsing error, such as DateTimeParseException
                    }
                }

                if ((value != null || oldValue != null) && (value == null || !value.equals(oldValue))) {
                    // no point updating values needlessly if they are the same
                    comboBox.setValue(value);
                }
            }
        }

    }
}
