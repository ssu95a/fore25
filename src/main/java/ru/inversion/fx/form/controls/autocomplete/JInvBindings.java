/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.autocomplete;

import impl.org.controlsfx.autocompletion.SuggestionProvider;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import ru.inversion.fx.form.controls.JInvComboBox;

/**
 *
 * @author antonovdi
 */
public class JInvBindings {

    public static <T> AutoCompletionBinding<T> bindAutoCompletion(
        JInvComboBox combobox) {

        return new JInvAutoCompletionComboBoxBinding<>(combobox,
            SuggestionProvider.create(combobox.getItems()));
    }

}
