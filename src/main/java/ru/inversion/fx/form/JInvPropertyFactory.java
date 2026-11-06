package ru.inversion.fx.form;

import javafx.beans.property.*;
import ru.inversion.fx.form.property.JInvIntegerProperty;
import ru.inversion.fx.form.property.JInvLongProperty;
import ru.inversion.utils.converter.TypeConverter;

import java.math.BigDecimal;


/**
 *
 * @author ssu
 */
public class JInvPropertyFactory {

    /** */
    static public Property createProperty( Object bean, String name, Class<?> typeValue, Object initialValue ) {

        if( initialValue != null )
            initialValue = TypeConverter.convert( initialValue, typeValue );

        if( typeValue == Boolean.class )
            return initialValue == null ? new SimpleBooleanProperty( bean, name )
                                        : new SimpleBooleanProperty( bean, name, (Boolean)initialValue );

        if( typeValue == Long.class )
            return initialValue == null ? new JInvLongProperty( bean, name )
                                        : new JInvLongProperty( bean, name, (Long)initialValue );

        if( typeValue == String.class )
            return initialValue == null ? new SimpleStringProperty( bean, name )
                                        : new SimpleStringProperty( bean, name, (String)initialValue );

        if( typeValue == BigDecimal.class )
            return initialValue == null ? new SimpleObjectProperty<BigDecimal>( bean, name )
                                        : new SimpleObjectProperty<>( bean, name, (BigDecimal)initialValue );

        if( typeValue == Integer.class )
            return initialValue == null ? new JInvIntegerProperty( bean, name )
                                        : new JInvIntegerProperty( bean, name, (Integer)initialValue );

        if( typeValue == Double.class )
            return initialValue == null ? new SimpleDoubleProperty( bean, name )
                                        : new SimpleDoubleProperty( bean, name, (Double)initialValue );

        if( typeValue == Float.class )
            return initialValue == null ? new SimpleFloatProperty( bean, name )
                                        : new SimpleFloatProperty( bean, name, (Float)initialValue );

        return initialValue == null ? new SimpleObjectProperty<>( bean, name )
                                    : new SimpleObjectProperty<>( bean, name, initialValue );
    }
}
