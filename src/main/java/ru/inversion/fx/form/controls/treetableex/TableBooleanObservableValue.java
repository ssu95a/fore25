package ru.inversion.fx.form.controls.treetableex;


import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.U;


/**
 * @author ssu @
 */
public class TableBooleanObservableValue<P> extends BooleanProperty {

    private final IEntityProperty<P,Boolean> property;
    private P pojoInstance = null;

    public TableBooleanObservableValue( IEntityProperty<P,Boolean> property ) {
        this.property = property;
    }

    @Override
    public Object getBean() {
        return pojoInstance;
    }

    @Override
    public String getName() {
        return property.getPropertyName();
    }

    /** */
    @Override
    public Boolean getValue()
    {
        if( pojoInstance == null )
            return null;
        return property.invokeGetter(pojoInstance);
    }

    /** */
    public TableBooleanObservableValue<P> pojoInstance(P pojoInstance ) {
        this.pojoInstance = pojoInstance;
        return this;
    }

    /** */
    @Override
    public void set( boolean value ) {
        if( pojoInstance != null )
            property.invokeSetter( pojoInstance, value );
    }

    @Override
    public void addListener( InvalidationListener listener ) {
    }
    @Override
    public void removeListener( InvalidationListener listener ) {

    }
    @Override
    public void addListener( ChangeListener< ? super Boolean > listener ) {
    }
    @Override
    public void removeListener( ChangeListener< ? super Boolean > listener ) {
    }
    @Override
    public void bind( ObservableValue< ? extends Boolean > observable ) {
    }
    @Override
    public void unbind() {
    }
    @Override
    public boolean isBound() {
        return false;
    }
    @Override
    public boolean get() {
        return U.nvl( getValue(), false );
    }
}
