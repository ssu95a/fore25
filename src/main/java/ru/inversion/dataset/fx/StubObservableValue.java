package ru.inversion.dataset.fx;

import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.U;

/**
 *
 * @author ssu @
 */
public class StubObservableValue<P,T> extends SimpleObjectProperty<T> {

    private final IEntityProperty<P,T>  property;

    private P	    pojoInstance = null;
    private String  propertyName     = null;
    private ICellValueChangeListener<P> cellValueChangeListener;
	
	public StubObservableValue( IEntityProperty<P,T> property, String propertyName, ICellValueChangeListener<P> cellValueChangeListener ) {
        this.propertyName = propertyName;
        this.cellValueChangeListener = cellValueChangeListener;
        this.property     = property;
	}
    
	/** */
	@Override
	public T getValue( ) {
		if( pojoInstance == null || property == null )
			return null;
        return property.invokeGetter(pojoInstance);
	}
	/** */
	public StubObservableValue<P,T> setPojoInstance( P pojoInstance ) {
		this.pojoInstance = pojoInstance;
		return this;
	}

	@Override
	public void addListener( InvalidationListener listener ) {
	}

	@Override
	public void addListener( ChangeListener< ? super T > listener ) {
	}

	/** */
    @Override
	public void setValue( Object value ) {
		
        if( pojoInstance != null && property != null ) 
        {
			T oldVal = property.invokeGetter( pojoInstance );
			if( U.equals( oldVal, value ) )
				return;
            property.invokeSetter( pojoInstance, value );
            if( cellValueChangeListener != null )
                cellValueChangeListener.changed( pojoInstance, propertyName, oldVal, value );
        }   
	}

}
