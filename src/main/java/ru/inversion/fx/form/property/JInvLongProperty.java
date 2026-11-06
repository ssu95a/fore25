package ru.inversion.fx.form.property;

import javafx.beans.property.SimpleObjectProperty;

/**
 * Long св-во с возможностью хранить null значения
 * @author Sulimoff
 * @version 2.0
 * */
public class JInvLongProperty extends SimpleObjectProperty<Long> {
    /** */
    public JInvLongProperty( ) {
        this(null);
    }
    /** */
    public JInvLongProperty( Long initialValue ) {
        super( initialValue );
    }
    /** */
    public JInvLongProperty( Object bean, String name ) {
        super( bean, name );
    }
    /** */
    public JInvLongProperty(Object bean, String name, Long initialValue) {
        super( bean, name, initialValue );
    }
    /** */
    public boolean isNullValue() {return getValue() == null;}
}
