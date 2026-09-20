package ru.inversion.fx.form.controls.treetableex;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import ru.inversion.meta.IEntityProperty;


/** */
public class TableObservableValue extends SimpleObjectProperty {

    private final IEntityProperty property;

    private Object pojoInstance = null;

    public TableObservableValue(IEntityProperty property ) {
        this.property = property;
    }

    /** */
    @Override
    public Object get( ) {
        if( pojoInstance == null  )
            return null;
        return property.invokeGetter(pojoInstance);
    }

    /** */
    public TableObservableValue pojoInstance( Object pojoInstance ) {
        this.pojoInstance = pojoInstance;
        return this;
    }

    /** */
    @Override
    public void set( Object value ) {
        if( pojoInstance != null && property != null )
            property.invokeSetter( pojoInstance, value );
    }

    @Override
    public void addListener( InvalidationListener listener ) {
    }

    @Override
    public void addListener( ChangeListener listener ) {
    }

}
