package ru.inversion.fx.form.controls;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/** Добавляет контролам свойство "только для чтения".
 Использовать, если весь IJInvControl реализовывать не хочется, а ReadOnly нужен
 @author fomishkin on 21.12.2017. */
public interface IReadOnlyControl extends IBaseControl {
    String READONLY_PROPERTY = "ru.inversion.readonly";
    default BooleanProperty readOnlyProperty() {

        BooleanProperty bp = this.getProperty( READONLY_PROPERTY );

        if( bp == null ) {
            bp = new SimpleBooleanProperty( this, "readOnly" );
            setProperty( READONLY_PROPERTY, bp );
        }

        return bp;
    }
    default void setReadOnly( boolean readOnly ){
        this.readOnlyProperty().setValue( readOnly );
    }
    default boolean isReadOnly(){
        return this.readOnlyProperty().getValue();
    }
}
