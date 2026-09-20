package ru.inversion.fx.form.controls;
import javafx.collections.ObservableMap;

import java.util.function.Function;

/**
 Добавляет контролам поддержку работы со свойствами
 @author fomishkin on 21.12.2017. */
public interface IBaseControl {

    /**
     * Возвращаются свойства компонента, доступно редактирование, удаление и добавление
     */
    ObservableMap<Object, Object> getProperties();
    /** */
    default void setProperty( String property, Object value ) {
        getProperties().put( property, value );
    }
    /** */
    default boolean hasProperty( String property ) {
        return getProperties().containsKey( property );
    }
    /** */
    default <T> T getProperty( String property ) {
        return (T)getProperties().get(property);
    }
    /** */
    default <T> T getProperty( String property, T defaultValue ) {
        return (T)getProperties().getOrDefault( property, defaultValue );
    }
    /** */
    default <T> T getProperty( String property, Function<Object,T> mapFunc ) {
        return (T)getProperties().computeIfAbsent( property, mapFunc );
    }
}
