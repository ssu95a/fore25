package ru.inversion.fx.form.controls.table;


import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet;
import ru.inversion.fx.form.JInvPropertyFactory;
import ru.inversion.utils.converter.TypeConverter;

import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Optional.of;

/** */
public class TableSettingItemValue<T> implements PropertySheet.Item {

    private static final ResourceBundle foreBundle = ResourceBundle.getBundle("fore");

    public static final String BUNDLE_PREFIX = "TABLE_SETTINGS_";

    final private Property<T >          valueProperty;
    final private TableSettingItemEnum  settingDescriptor;

    final private String                category,
                                        name,
                                        descr;

    private boolean editable = true;

    private TableSettingItemValue( TableSettingItemEnum sd ) {
        settingDescriptor = sd;
        valueProperty      = JInvPropertyFactory.createProperty( this, sd.getName(), sd.getType(), sd.getValue() );

        category           = foreBundle.getString( BUNDLE_PREFIX + sd.getCategory() );
        name               = foreBundle.getString( BUNDLE_PREFIX + sd.getName() );
        descr              = foreBundle.getString( BUNDLE_PREFIX + sd.getName() + "_DESCR" );
    }

    @Override
    public Class< ? > getType() {
        return settingDescriptor.getType();
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getName( ) {
        return name;
    }

    @Override
    public String getDescription() { return descr; }

    @Override
    public Object getValue() {
        return valueProperty.getValue();
    }

    @Override
    public void setValue( Object value ) {
        valueProperty.setValue( (T)value );
    }

    /** */
    @Override
    public Optional< ObservableValue< ? extends Object > > getObservableValue() {
        return of(valueProperty);
    }

    /** */
    @Override
    public boolean isEditable() {
        return editable;
    }

    /** */
    public String getSettingName() { return settingDescriptor.name(); }

    /** */
    public static <T> TableSettingItemValue<T> create( TableSettingItemEnum sd, Map<String,Object> valuesMap ) {

        TableSettingItemValue<T> itemValue = new TableSettingItemValue<>( sd );

        Object value = valuesMap.get( sd.name() );

        if( value != null ) {

            if( sd.getType() == TripleBoolValueEnum.class )
                itemValue.setValue( TripleBoolValueEnum.fromInt( TypeConverter.convert( value, Integer.class) ) );
            else
                itemValue.setValue( TypeConverter.convert(value, sd.getType()) );
        }
        return itemValue;
    }
}
