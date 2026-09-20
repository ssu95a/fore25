package ru.inversion.fx.app.frame.menu;

import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import org.apache.commons.lang.LocaleUtils;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.PropertyEditor;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.form.JInvPropertyFactory;
import ru.inversion.fx.form.controls.JInvPasswordField;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static ru.inversion.fx.app.frame.menu.PropertyItemEnum.I18N_LOCALE;
import static ru.inversion.fx.app.frame.menu.PropertyItemEnum.RUN_FX_CALL_PL_MON;
import static ru.inversion.fx.app.service.exteditor.ExternalEditorManager.ASSOC;

/** */
public class PropertyItemValue<T> implements PropertySheet.Item {

    final static ResourceBundle g_bundle =  ResourceBundle.getBundle("fore");
    final static private ResourceBundle languages = ResourceBundle.getBundle("languages");

    final private Property<T>       valueProperty;
    final private PropertyItemEnum  propertyDescriptor;
    final private String            category,
                                    name,
                                    descr;
    final private boolean restartRequired;

    private Supplier<List<T> > supplierValues;

    private boolean editable = true;

    private PropertyItemValue( PropertyItemEnum pd ) {
        propertyDescriptor = pd;
        valueProperty      = (Property<T>) JInvPropertyFactory.createProperty( this, pd.getName(), pd.getType(), pd.getValue() );
        category           = g_bundle.getString( "SETTINGS_" + pd.getCategory() );
        valueProperty.setValue( (T)pd.getValue() );

        String propName = propertyDescriptor.getName();

        if ( !propName.contains( ASSOC ) ) {
            name               = g_bundle.getString( "SETTINGS_" + propertyDescriptor.getName() );
            descr              = g_bundle.getString( "SETTINGS_" + propertyDescriptor.getName() + "_DESCR" );
        } else {
            //Особый случай для PropertyItemEnum.ASSOC
            String subPropName = propName.substring( 0, propName.lastIndexOf( '_' ) );
            String postfix = propName.substring( propName.lastIndexOf( '_' ) + 1);
            this.name = postfix;
            descr = g_bundle.getString( String.format( "SETTINGS_%s_DESCR", subPropName ) ) + " " + postfix;
        }
        restartRequired    = pd.isRestartRequired();
    }

    @Override
    public Class< ? > getType() {
        return propertyDescriptor.getType();
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

    @Override
    public Optional< ObservableValue< ? extends Object > > getObservableValue() {
        return of(valueProperty);
    }

    @Override
    public boolean isEditable() {
        return editable;
    }

    /** */
    public void setEditable( boolean editable ) {
        this.editable = editable;
    }

    /** */
    public PropertyItemEnum getDescriptor() { return propertyDescriptor; }

    /** */
    public PropertiesTypeEnum getPropertyType() { return propertyDescriptor.getPropertyType(); }

    /** */
    public String getPropertyName() { return propertyDescriptor.getName(); }

    /** */
    public Supplier< List<T> > getSupplierValues( ) {
        return supplierValues;
    }

    /** */
    public void setSupplierValues( Supplier< List<T> > supplierValues ) {
        this.supplierValues = supplierValues;
    }

    public boolean isRestartRequired() {
        return restartRequired;
    }

    /** */
    public static class LE<S> extends AbstractPropertyEditor<S, ComboBox<S> > {

        public LE( PropertySheet.Item property ) {

            super( property, new ComboBox<>() );

            List<S> items = ((PropertyItemValue< S >)property).getSupplierValues().get();

            if( items == null )
                items = FXCollections.emptyObservableList();

            if( !items.isEmpty() ) {

                S item = items.get(0);

                if( item.getClass() == Locale.class ) {

                    StringConverter<Locale> stringConverter = new StringConverter< Locale >() {
                        @Override
                        public String toString( Locale l ) { return l.getDisplayName();}
                        @Override
                        public Locale fromString( String string ) { return null; }
                    };
                    getEditor().setConverter((StringConverter< S >)stringConverter);
                }
            }

            getEditor().setItems( FXCollections.observableArrayList( items ) );
        }
        @Override protected ObservableValue<S> getObservableValue() {
            return getEditor().getSelectionModel().selectedItemProperty();
        }
        @Override public void setValue(Object value) {
            getEditor().getSelectionModel().select((S)value);
        }
    }

    public static class ConnectionIndicatorsEditor
            extends AbstractPropertyEditor<ConnectionIndicators, DialogObjectField<ConnectionIndicators> >
    {
        public ConnectionIndicatorsEditor( PropertySheet.Item item ) {
            super( item,
                    new DialogObjectField< ConnectionIndicators >() {
                        @Override
                        protected Class< ConnectionIndicators > getType() {
                            return ConnectionIndicators.class;
                        }
                        @Override
                        protected ConnectionIndicators edit( ConnectionIndicators object ) {
                            ConnectionIndicators retObject = (new IndicatorsListDialog(object)).showAndWait().orElse(null);
                            return retObject == null ? object : retObject;
                        }
            });
        }
        @Override
        protected ObservableValue< ConnectionIndicators > getObservableValue() {
            return getEditor().objectProperty();
        }
        @Override
        public void setValue( ConnectionIndicators value ) {
            getEditor().objectProperty().set(value);
        }
    }

    /** */
    public static class PasswordEditor
            extends AbstractPropertyEditor< String, JInvPasswordField >
    {
        public PasswordEditor( PropertySheet.Item item ) {
            super( item, new JInvPasswordField() );
        }
        @Override
        protected ObservableValue< String > getObservableValue() {
            return getEditor().textProperty();
        }
        @Override
        public void setValue( String value ) {
            getEditor().setText( value );
        }
    }

    /** */
    @Override
    public Optional<Class<? extends PropertyEditor<?> >> getPropertyEditorClass() {

//        Supplier< List<T> > sup = getSupplierValues();
//
//        Class c = LE.class;
//
//        if( sup != null )
//            return Optional.of( c );

        Class c = null;

        switch( this.propertyDescriptor ) {
            case I18N_LOCALE:
                c = LE.class;
            break;
            case IND_ALIAS_LIST:
                c = ConnectionIndicatorsEditor.class;
            break;
            case MAILU_USR_PASSWORD:
                c = PasswordEditor.class;
        }

        if( c != null )
            return Optional.of( c );

        return Optional.empty();
    }

    static public Supplier<List<Locale>>
            g_locales = () -> Collections.list( languages.getKeys() ).stream().map( ( s)-> LocaleUtils.toLocale(s) ).collect( Collectors.toList() );

    /** */
    public static PropertyItemValue<?> create( PropertyItemEnum pd ) {

        if( pd == RUN_FX_CALL_PL_MON  )
            return null;

        if( pd == I18N_LOCALE ) {

            PropertyItemValue<Locale> localeProperty =  new PropertyItemValue<>(pd);

            localeProperty.setSupplierValues(g_locales);

            return localeProperty;
        }

        return new PropertyItemValue<>(pd);
    }
}

