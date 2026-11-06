/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.geometry.Pos;
import ru.inversion.fx.form.controls.JInvTableColumnBigDecimal;
import ru.inversion.utils.converter.TypeConverter;

import java.math.BigDecimal;

import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_MONEY_DEFAULT;

/**
 *
 * @author antonovdi
 */
public class JInvTableCellDecimal<P> extends JInvTableCell<P,BigDecimal> {

    public JInvTableCellDecimal(String mask) {
        super(mask);
    }

    @Override
    protected void updateItem(BigDecimal item, boolean empty ) {

        super.updateItem( item, empty);

        if( item == null || empty )
        {
            setText(null);
        }
        else
        {
            String valueString;
            /*
            if(mask!=null && !mask.isEmpty() && !mask.equals(MASK_MONEY_DEFAULT)){
               valueString = TypeConverter.convertToString(item, mask, BigDecimal.class);
            }else if(getTableColumn() instanceof JInvTableColumnBigDecimal){
                valueString = ((JInvTableColumnBigDecimal)getTableColumn()).getDecimalFormat().format(item);
            }else{
                // для обратной совместиоти
                valueString = TypeConverter.convertToString(item, mask, BigDecimal.class);
            }
            */
            if( getTableColumn() instanceof JInvTableColumnBigDecimal) {
                valueString = ((JInvTableColumnBigDecimal< P, ? >)getTableColumn()).getDecimalFormat().format(item);
            }
            else {
                valueString = TypeConverter.convertToString( item, mask, BigDecimal.class );
            }

            setText(valueString);
        }
        applyRenderer(item, empty);
    }
}
