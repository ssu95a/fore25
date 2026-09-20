package ru.inversion.dataset.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.U;

/**
 *
 * @author ssu @
 */
public class StubBooleanObservableValue<P,T> extends BooleanProperty {

    private final IEntityProperty<P,T> property;
    private P     pojoInstance = null;
    private final ICellValueChangeListener<P> cellValueChangeListener;
	
	public StubBooleanObservableValue( IEntityProperty<P,T> property, ICellValueChangeListener<P> cellValueChangeListener ) {
        //super( null, property.getName() );
        this.cellValueChangeListener = cellValueChangeListener;
        this.property                = property;
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
	public Boolean getValue() {
		if( pojoInstance == null )
			return null;
        return (Boolean)property.invokeGetter(pojoInstance);
	}
    
	/** */
	public StubBooleanObservableValue<P,T> setPojoInstance( P pojoInstance ) {
		this.pojoInstance = pojoInstance;
        return this;
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

    /** */
    @Override
	public void set( boolean value ) {

        if( pojoInstance != null && property != null ) {
            property.invokeSetter( pojoInstance, value );
            if( cellValueChangeListener != null )
                cellValueChangeListener.changed( pojoInstance, getName(), !value, value );
        }
	}
    @Override
    public void bind( ObservableValue< ? extends Boolean > observable ) { }
    @Override
    public void unbind() {  }
    @Override
    public boolean isBound() {
        return false;
    }
    @Override
    public boolean get() {
        return U.nvl( getValue(), Boolean.FALSE );
    }
}
