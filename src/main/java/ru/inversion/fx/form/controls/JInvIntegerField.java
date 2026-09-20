package ru.inversion.fx.form.controls;

import javafx.beans.property.Property;
import javafx.util.StringConverter;
import ru.inversion.fx.form.TypeStringConverter;
import ru.inversion.fx.form.property.JInvIntegerProperty;

/**
 *
 * @author ssu @
 */
public class JInvIntegerField extends JInvNumberField<Integer> {

    public final static StringConverter<Integer> stringConverter = new TypeStringConverter<>(Integer.class);

    //private Property<Integer> valueProperty;

    /** */
    public JInvIntegerField() {
        super();
    }

    /** */
    public JInvIntegerField( String s) {
        super(s);
    }

    /** */
    public JInvIntegerField( Integer value) {
        super(value);
    }

    /** */
    @Override
    public StringConverter<Integer> getConverter() {
        return stringConverter;
    }

    /** */
    @Override
    public Property<Integer> valueProperty() {
        if( valueProperty == null )
            valueProperty = new JInvIntegerProperty( this, "value" );
        return valueProperty;
    }

    /** */
    @Override
    public Class<Integer> getClassValue() {
        return Integer.class;
    }
}
