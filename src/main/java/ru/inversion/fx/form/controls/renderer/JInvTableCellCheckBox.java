/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.scene.control.CheckBox;
import javafx.scene.control.cell.CheckBoxTableCell;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author antonovdi
 */
public class JInvTableCellCheckBox extends CheckBoxTableCell<Object, Object>{

    private CheckBox checkBox;
    
    @Override
    public void updateItem(Object item, boolean empty) {
        if (empty || item==null) {
            setText(null);
            setGraphic(null);
        } else {
            Boolean valueBoolean = TypeConverter.convert(item, Boolean.class);
            checkBox.selectedProperty().set(valueBoolean);
            setGraphic(checkBox);
        }
        
    }
    
    
}
