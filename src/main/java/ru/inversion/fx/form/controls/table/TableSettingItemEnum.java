package ru.inversion.fx.form.controls.table;

import javafx.beans.value.ObservableValue;
import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.editor.PropertyEditor;

import java.util.Optional;

/** */
public enum TableSettingItemEnum implements PropertySheet.Item {

    /** */
    GUI_SHOW_STATUS_BAR ( TripleBoolValueEnum.class, TripleBoolValueEnum.OFF ),

    /** */
    MARK_SAVE_PREV_MARKED_ROWS( TripleBoolValueEnum.class, TripleBoolValueEnum.OFF ),
    
    /**Флаг проверки на наличие значений*/
    CHECK_NO_DATA_FOUND(TripleBoolValueEnum.class, TripleBoolValueEnum.OFF)
    ;

    final private Class   dataType;
    final private Object  defaultValue;

    TableSettingItemEnum( Class dataType, Object defaultValue ) {
        this.dataType     = dataType;
        this.defaultValue = defaultValue;

    }
    @Override
    public Class< ? > getType() {
        return dataType;
    }

    @Override
    public String getCategory() {

        int index = this.name().indexOf('_');

        if( index == -1 )
            return this.name();
        else
            return this.name().substring( 0, index );
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Object getValue() {
        return defaultValue;
    }

    @Override
    public void setValue( Object value ) { }

    @Override
    public Optional< ObservableValue< ? extends Object > > getObservableValue() {
        return null;
    }

    @Override
    public Optional< Class< ? extends PropertyEditor< ? > > > getPropertyEditorClass() {
        return null;
    }

    @Override
    public boolean isEditable() {
        return false;
    }
}
