package ru.inversion.fx.form;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import ru.inversion.fx.app.Tags;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.util.*;


/**
 *
 * @author ssu
 * @param <T>
 */
public class FXEntity<T> implements IFXEntity<T> {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    private class Item {
        public Property             property;
        public IEntityProperty<T,?> propertyDescriptor;
        public Object               initialValue;
        public boolean              wasChanged = false;
    };

    final private ObjectProperty<T>           baseEntityProperty;
    final private Map<String,Item>            mapProperty = new HashMap<>();

    private List<IFXEntityListener<T>>        listeners;

    private Map<String, Pair<Object, Object>> lastChangeMap;

    private boolean autoCommit = true;

    /** @param entityBase */
    public FXEntity( T entityBase )
    {
        baseEntityProperty = new SimpleObjectProperty<>( this, "baseEntity", Objects.requireNonNull( entityBase, "entityBase is null") );
    }

    @Override
    public boolean isAutoCommit() {
        return autoCommit;
    }

    @Override
    public void setAutoCommit( boolean autoCommit ) {
        this.autoCommit = autoCommit;
    }

    /** */
    @Override
    public T getBaseEntity() {
        return baseEntityProperty.get();
    }

    /** */
    @Override
    public <V> void setValue( String name, V value ) {
        final Property<V> property = getProperty(name);
        property.setValue(value);
    }

    /** */
    @Override
    public <V> V getValue( String name ) {
        return (V)getProperty(name).getValue( );
    }
    
    /** */
    @Override
    public <V> V getInitialValue( String name ) {

        if( S.isNullOrEmpty(name) )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'name' is null or empty" );
        
        Item item = mapProperty.get(name);
        if( item != null )
            return (V)item.initialValue;
        
        return null;
    }

    /** */
    public void resetBaseEntity( T newBaseInstance ) {

        Objects.requireNonNull( newBaseInstance, Tags.PRODUCT_LABEL + "'newBaseInstance' is null" );

        baseEntityProperty.set( newBaseInstance );

        for( Map.Entry<String,Item> e : mapProperty.entrySet() ) {

            Item item = e.getValue();

            item.initialValue = item.propertyDescriptor.invokeGetter( newBaseInstance );
            item.property.setValue( item.initialValue );

            item.wasChanged = false;
        }//end for
    }

    /** */
    @Override
    public <V> Property<V> getProperty( String name ) {

        if( S.isNullOrEmpty(name) )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'name' is null or empty" );
    
        Item item = mapProperty.get(name);

        if( item != null )
            return (Property<V>)item.property;
        
        IEntityProperty<T,?> pd = EntityMetadataFactory.getEntityMetaData( (Class<T>)getBaseEntity().getClass() ).getProperty(name);
        if( pd == null )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "Not found property '" + name + "' for class: " + getBaseEntity().getClass().getName() );

        Object initialValue = pd.invokeGetter( getBaseEntity() );

        item = new Item();
        item.initialValue       = initialValue;
        item.propertyDescriptor = pd;
        item.property           = JInvPropertyFactory.createProperty( this, name, pd.getType(), initialValue );
        
        mapProperty.put( name, item );

        if( listeners != null ) {
            final Property<V> p = (Property<V>)item.property;
            listeners.forEach( (IFXEntityListener l)->l.onCreateProperty( p ) );
        }
        return (Property<V>)item.property;
    }
    
    /** */    
    @Override
    public <V> IEntityProperty<V,?> getPropertyDescriptor(String name) {
        
        if( S.isNullOrEmpty(name) )
            throw new IllegalArgumentException("'name' is null or empty");
        
        Item item = mapProperty.get(name);
        if( item != null )
            return (IEntityProperty<V,?>)item.propertyDescriptor;
        else 
        {
            getProperty(name);
            
            item = mapProperty.get(name);
            if( item != null )
                return (IEntityProperty<V,?>) item.propertyDescriptor;
        }
        
        return null;
    }
    
    /** */
    @Override
    public Map<String,Pair<Object,Object>> commit( ) {
        
        lastChangeMap = null;               
        
        Map<String,Pair<Object,Object>> changeMap = new HashMap<>();
        
        Object value = null;
        
        for( Map.Entry<String,Item> e : mapProperty.entrySet() ) {
            
            value = e.getValue().property.getValue();

            if( e.getValue().wasChanged || !U.equals( e.getValue().initialValue, value) ) {

                IEntityProperty<T,?> p = e.getValue().propertyDescriptor;
                        
                if( p != null && !p.isReadOnly() )
                {
                    try
                    {
                        Object o1 = TypeConverter.convert( value, p.getType() );

                        if( o1 != null && o1.getClass() == String.class && S.isNullOrEmpty((String)o1) )
                            o1 = null;

                        if( !U.equals( e.getValue().initialValue, o1 ) )
                        {
                            changeMap.put( e.getKey(), new Pair( e.getValue().initialValue, o1 ) );

                            p.invokeSetter( getBaseEntity(), o1 );
                        
                            e.getValue().wasChanged = true;
                        }
                    }
                    catch( Throwable th ) {
                        throw new RuntimeException( Tags.PRODUCT_LABEL + fore.getString("OSHIBKA_PRI_USTANOVKE_SVOJSTVA"), th );
                    }
                }//end if
            }
        }//end for
        
        if( listeners != null )
            listeners.forEach( (IFXEntityListener l)->l.onCommit(this,changeMap) );
        
        // после коммита текущие 
        // значения становятся исходными
        mapProperty.entrySet().stream().filter((e) -> ( changeMap.containsKey( e.getKey() )  )).forEach((e) -> {
            e.getValue().initialValue = changeMap.get( e.getKey() ).second;
        }); //end for    
        
        lastChangeMap = changeMap;
        
        return changeMap;
    }

    /** */
    @Override
    public void rollback( ) {
        
        T baseEntity = this.getBaseEntity( );

        if( baseEntity == null )
            return;

        lastChangeMap = null;               
        
        for( Map.Entry<String,Item> e : mapProperty.entrySet() ) {

            IEntityProperty p = e.getValue().propertyDescriptor;
            
            if( p != null && !p.isReadOnly() ) 
            {
                try {
                    p.invokeSetter( baseEntity, e.getValue().initialValue );
                }
                catch( Throwable th ) {
                    throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on set FXEntity property '" + e.getKey() + "' to initial value", th );
                }
                
                e.getValue().property.setValue( e.getValue().initialValue );
                
            }//end if
        }//end for

        if( listeners != null )
            listeners.forEach( (IFXEntityListener l)->l.onRollback(this) );
    }
    
    /** */
    @Override
    public void addListener(IFXEntityListener listener) {
        
        if( listener == null )
            return;
        
        if( listeners == null )
            listeners = new ArrayList<>();
        else
            if( listeners.contains(listener) )
                return;
        
        listeners.add(listener);
    }
    
    /** */
    @Override
    public void removeListener(IFXEntityListener listener) {

        if( listener != null ) {

            listeners.remove(listener);

            if( listeners.isEmpty() )
                listeners = null;
        }
    }
    
    /** */
    @Override
    public Map<String, Pair<Object, Object>> getLastChangeMap() {
        return lastChangeMap;
    }


}
