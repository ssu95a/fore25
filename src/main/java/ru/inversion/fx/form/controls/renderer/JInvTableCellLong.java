/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.geometry.Pos;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author antonovdi
 */
public class JInvTableCellLong<S> extends JInvTableCell<S,Number> {

    public JInvTableCellLong(String mask) {
        super(mask);
        setAlignment( Pos.CENTER_RIGHT );
    }

    @Override
    protected void updateItem( Number item, boolean empty) {

        super.super_updateItem(item, empty);

        if (item == null || empty)
        {
            setText(null);
        }
        else
        {
            String valueString = TypeConverter.convert(item, String.class);
            setText(valueString);
        }
        applyRenderer(item, empty);
   }

}
