/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import ru.inversion.db.entity.ContentTypeEnum;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.JInvTableColumnDate;
import ru.inversion.utils.S;

import java.math.BigDecimal;

/**
 *
 * @author antonovdi
 */
public class JInvCellFactoryProvider {

    public static Callback getCellFactory( Class clazz, TableColumn column ) {

        if( java.util.Date.class.isAssignableFrom(clazz) || column instanceof JInvTableColumnDate )
        {
            String mask = ( (JInvTableColumn) column ).getMask();
            if ( S.isNotNullOrEmpty(mask) ) {
                return getDateCellFactory( mask );
            } else {
                return getDateCellFactory( null );
            }

        } else if ( Number.class.isAssignableFrom(clazz) ) {
            if ( clazz.equals(Float.class) || clazz.equals(Double.class) || clazz.equals(BigDecimal.class) ) {
                return getDecimalCellFactory( "" );
            } else {
                return getIntegerCellFactory();
            }

        } else if ( Boolean.class.isAssignableFrom(clazz) ) {
            return getBooleanCellFactory();

        } else if ( String.class.isAssignableFrom(clazz) ) {
            return getStringCellFactory();

        } else if ( Enum.class.isAssignableFrom(clazz) ) {
            return getEnumCellFactory();

        } else {
            return null;
        }
    }

    public static Callback getCellFactory(ContentTypeEnum type, TableColumn column) {

        String mask = ContentTypeManager.getFormatMask(type);

        switch (type) {
            case DATE:
            case TIME:
            case DATE_TIME:
            case TIME_DATE:
                return getDateCellFactory(mask);
            case INTEGER_VALUE:
                return getIntegerCellFactory();
            case PERCENT:
            case MONEY:
                return getDecimalCellFactory(mask);
            case ICON_CODE:
            {
                String columnMask = ( (JInvTableColumn) column ).getMask();
                return col->new JInvTableCellIcon(columnMask);
            }
        }
        return null;
    }

    private static Callback getDateCellFactory(String mask) {
        return col -> new JInvTableCellDate(mask);
    }

    private static Callback getDecimalCellFactory(String mask) {
        return col -> new JInvTableCellDecimal(mask);
    }

    private static Callback getIntegerCellFactory() {
        return col -> new JInvTableCellLong(null);
    }

    private static Callback getEnumCellFactory() {
        return col -> new JInvTableCellEnum();
    }

    private static Callback getStringCellFactory() {
        return col -> new JInvTableCellString(null);
    }

    private static Callback getBooleanCellFactory() {
        return col -> new JInvTableCellBoolean(null);
    }
}
