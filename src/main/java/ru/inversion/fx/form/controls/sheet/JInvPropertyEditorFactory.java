/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.sheet;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.TextInputControl;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.Editors;
import org.controlsfx.property.editor.PropertyEditor;
import ru.inversion.fx.app.frame.menu.SimpleBeanInfo;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvCalendar;
import ru.inversion.fx.form.controls.JInvCalendarTime;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvComboBox;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.JInvValueField;
import ru.inversion.fx.form.lov.JInvFileChooserLov;
import ru.inversion.utils.S;

import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_DATE_DD_MM_YYYY_HH_MIN_SEC;

/**
 *
 * @author antonovdi
 */
public class JInvPropertyEditorFactory implements Callback<PropertySheet.Item, PropertyEditor<?> > {

    @Override
    public PropertyEditor<?> call(PropertySheet.Item item) {
        Class<?> type = item.getType();

        //TODO: add support for char and collection editors
        if (item.getPropertyEditorClass().isPresent()) {
            Optional<PropertyEditor<?>> ed = Editors.createCustomEditor(item);
            if (ed.isPresent()) {
                return ed.get();
            }
        }

        if (item instanceof SimpleBeanInfo && ((SimpleBeanInfo) item).getItemProvider() != null) {
            return createChoiceEditor(item, ((SimpleBeanInfo) item).getItemProvider().getItems());
        }

        if (/*type != null &&*/type == String.class) {
            return createTextEditor(item);
        }

        if (/*type != null &&*/type == File.class) {
            return createFileEditor(item);
        }

        if (/*type != null &&*/isNumber(type)) {
            return createNumericEditor(item);
        }

        if (/*type != null &&*/(type == boolean.class || type == Boolean.class)) {
            return createCheckEditor(item);
        }

        if (/*type != null &&*/type == LocalDate.class) {
            return createDateEditor(item);
        }

        if (/*type != null &&*/type == LocalDateTime.class) {
            return createDateTimeEditor(item);
        }

        if (/*type != null &&*/type == Color.class || type == Paint.class) {
            return Editors.createColorEditor(item);
        }

        if (type != null && type.isEnum()) {
            return createChoiceEditor(item, Arrays.<Object>asList(type.getEnumConstants()));
        }

//        if (type != null && type == List.class) {
//            return Editors.createChoiceEditor(item, Arrays.<Object>asList(type));
//        }
        if (/*type != null &&*/type == Font.class) {
            return createFontEditor(item);
        }

        return null;
    }

    private static Class<?>[] numericTypes = new Class[]{
        byte.class, Byte.class,
        short.class, Short.class,
        int.class, Integer.class,
        long.class, Long.class,
        float.class, Float.class,
        double.class, Double.class,
        BigInteger.class, BigDecimal.class
    };

    // there should be better ways to do this
    private static boolean isNumber(Class<?> type) {
        if (type == null) {
            return false;
        }
        for (Class<?> cls : numericTypes) {
            if (type == cls) {
                return true;
            }
        }
        return false;
    }

    private PropertyEditor<?> createFileEditor( final PropertySheet.Item property ) {
        return new AbstractPropertyEditor<File, JInvTextField>(property, new JInvTextField()) {

            private SimpleObjectProperty<File> fileProperty;
            {
                JInvTextField editor = getEditor();

                enableAutoSelectAll( editor );

                initProperty();

                if ( editor.getLOV() == null ) {
                    JInvFileChooserLov lov = new JInvFileChooserLov();

                    lov.showSaveProperty().set( false );
                    lov.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter( "Executable", "*.exe","*.bat" ),
                            new FileChooser.ExtensionFilter( "All files", "*.*" )
                    );
                    editor.setLOV( lov );
                    Bindings.bindBidirectional( getEditor().textProperty(), fileProperty, FILE_TO_STRING );
                }
            }

            private void initProperty() {
                if ( fileProperty == null ) {
                    fileProperty = new SimpleObjectProperty<>();
                }
            }

            @Override
            protected SimpleObjectProperty<File> getObservableValue() {
                initProperty();
                return fileProperty;
            }

            @Override
            public void setValue(File value) {
                getEditor().setText( FILE_TO_STRING.toString( value ));
            }
        };
    }

    private static PropertyEditor<?> createFontEditor( PropertySheet.Item property ) {

        return new AbstractPropertyEditor<Font, JInvAbstractObjectField<Font>>(property, new JInvAbstractObjectField<Font>() {
            @Override protected Class<Font> getType() {
                return Font.class;
            }

            @Override protected String objectToString(Font font) {
                return font == null? "": String.format("%s, %.1f", font.getName(), font.getSize());
            }

            @Override protected Font edit(Font font) {
                JInvFontSelectorDialog dlg = new JInvFontSelectorDialog(font);
                Optional<Font> optionalFont = dlg.showAndWait();
                return optionalFont.orElse(null);
            }
        }) {

            @Override protected ObservableValue<Font> getObservableValue() {
                return getEditor().getObjectProperty();
            }

            @Override public void setValue(Font value) {
                getEditor().getObjectProperty().set(value);
            }
        };

    }

    public static final StringConverter<File> FILE_TO_STRING = new StringConverter<File>() {
            @Override
            public String toString( final File object ) {
                if ( object == null ) {
                    return S.EMPTY_STRING;
                }
                return object.getAbsolutePath();
            }
            @Override
            public File fromString( final String string ) {
                if ( S.isNullOrEmpty(string) ) {
                    return null;
                }
                return new File( string );
            }
        };


    private static PropertyEditor<?> createTextEditor( PropertySheet.Item property ) {

        return new AbstractPropertyEditor<String, JInvTextField>(property, new JInvTextField()) {

            { enableAutoSelectAll(getEditor()); }

            @Override protected StringProperty getObservableValue() {
                return getEditor().textProperty();
            }

            @Override public void setValue(String value) {
                getEditor().setText(value);
            }
        };
    }

    private static <T> PropertyEditor<?> createChoiceEditor( PropertySheet.Item property, final Collection<T> choices ) {

        return new AbstractPropertyEditor<T, JInvComboBox<T, T>>(property, new JInvComboBox<T, T>()) {

            { getEditor().setItems( FXCollections.observableArrayList(choices)); }

            @Override protected ObservableValue<T> getObservableValue() {
                return getEditor().getSelectionModel().selectedItemProperty();
            }

            @Override public void setValue(T value) {
                getEditor().getSelectionModel().select(value);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static PropertyEditor<?> createNumericEditor( PropertySheet.Item property ) {
        final Class<?> type = property.getType();
        JInvValueField editor = (JInvValueField) Controls.getControlByClass( type, null );
        if ( editor == null ) {
            return Editors.createNumericEditor(property);
        }
        return new AbstractPropertyEditor<Object, JInvValueField>(property, editor) {

            { enableAutoSelectAll(getEditor()); }

            @Override protected ObservableValue<Object> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override public Object getValue() {
                return editor.getValue();
            }

            @Override public void setValue(Object value) {
                editor.setValue( value );
            }

        };
    }

    private static PropertyEditor<?> createCheckEditor( PropertySheet.Item property ) {

        return new AbstractPropertyEditor<Boolean, JInvCheckBox>(property, new JInvCheckBox()) {

            @Override protected BooleanProperty getObservableValue() {
                return getEditor().selectedProperty();
            }

            @Override public void setValue(Boolean value) {
                getEditor().setSelected((Boolean)value);
            }
        };

    }

    private static PropertyEditor<?> createDateEditor( PropertySheet.Item property ) {
        return new AbstractPropertyEditor<LocalDate, JInvCalendar>(property, new JInvCalendar()) {

            @Override protected ObservableValue<LocalDate> getObservableValue() {
                return getEditor().valueProperty();
            }

            @Override public void setValue(LocalDate value) {
                getEditor().setValue((LocalDate) value);
            }
        };
    }

    private static PropertyEditor<?> createDateTimeEditor( PropertySheet.Item property ) {
        JInvCalendarTime calendarTime = new JInvCalendarTime();
        calendarTime.setMask(MASK_DATE_DD_MM_YYYY_HH_MIN_SEC);
        return new AbstractPropertyEditor<LocalDateTime, JInvCalendarTime>(property, calendarTime) {
            @Override protected ObservableValue<LocalDateTime> getObservableValue() {
                return getEditor().dateTimeValueProperty();
            }

            @Override public void setValue(LocalDateTime value) {
                getEditor().setDateTimeValue( value );
            }
        };
    }

    private static void enableAutoSelectAll(final TextInputControl control) {
        control.focusedProperty().addListener((ObservableValue<? extends Boolean> o, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                Platform.runLater( control::selectAll );
            }
        });
    }

}
