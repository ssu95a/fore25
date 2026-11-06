package ru.inversion.fx.form;

import javafx.beans.property.Property;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.Pair;

import java.util.Map;

/**
 *
 * @author ssu
 */
public interface IFXEntity<T> {

    /** */
    public interface IFXEntityListener<T> {
        default void onCommit  ( IFXEntity<T> fxEntity, Map<String,Pair<Object,Object>> changeMap ) {}
        default void onRollback( IFXEntity<T> fxEntity ) {}
        default void onCreateProperty( Property<?> property ) {}
    }
    
    /**
     * @return  */
    T getBaseEntity();

    /** */
    <V> void setValue( String name, V value );

    /** */
    <V> V getValue( String name );
    
    /** */
    <V> V getInitialValue( String name );
    
    /**
     * @param name */
    <V> Property<V> getProperty( String name );

    /**
     * 
     * @param name
     * @return 
     */
    <V> IEntityProperty<V,?> getPropertyDescriptor(String name);
        
    /** */
    Map<String,Pair<Object,Object>> commit( );

    void resetBaseEntity( T newBaseInstance );

    /** */
    void rollback( );

    /** */
    public void addListener( IFXEntityListener listener );
    /** */
    public void removeListener( IFXEntityListener listener );
    
    /** */
    public Map<String,Pair<Object,Object>> getLastChangeMap();

    /** Признак, что БД транзакцией управляет ядро, при операциях с записью */
    default boolean isAutoCommit() { return true;}
    default void setAutoCommit(boolean autoCommit) { }
}
