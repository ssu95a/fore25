package ru.inversion.fx.form.controls;

import javafx.beans.property.Property;
import javafx.util.StringConverter;
import ru.inversion.fx.form.TypeStringConverter;
import ru.inversion.fx.form.property.JInvLongProperty;

/**
 *
 * @author ssu @
 */
public class JInvLongField extends JInvNumberField<Long> {

    public final static StringConverter<Long> stringConverter = new TypeStringConverter<>(Long.class);

    //private Property<Long> valueProperty;
    /**
     *      */
    public JInvLongField() {
        super();
    }

    /**
     *      */
    public JInvLongField(String s) {
        super(s);
    }

    /**
     *      */
    public JInvLongField(Long value) {
        super(value);
    }

    /**
     *      */
    @Override
    public StringConverter<Long> getConverter() {
        return stringConverter;
    }

    /**
     *      */
    @Override
    public Property<Long> valueProperty() {
        if( valueProperty == null )
            valueProperty = new JInvLongProperty(this, "value");
        return valueProperty;
    }

    /**
     *      */
    @Override
    public Class<Long> getClassValue() {
        return Long.class;
    }
}
