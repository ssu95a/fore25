package ru.inversion.fx.form;

import javafx.util.StringConverter;
import ru.inversion.utils.Pair;
import ru.inversion.utils.converter.TypeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ssu
 */
public class TypeStringConverter<P> extends StringConverter<P> {

    /** */
    final private Class<P> clazz;
    
    public TypeStringConverter( Class<P> clazz ) {
        this.clazz = clazz;
    }

    /** */
    @Override
    public String toString( P object ) {
        return TypeConverter.convert( object, String.class );
    }

    /** */
    @Override
    public P fromString( String string ) {
        return TypeConverter.convert( string, clazz );
    }

    /** */
    public Class<P> getType() { return clazz; }


    /*
    final static private List< Pair<Class<?>, StringConverter<?>> > g_convList = new ArrayList<>();

    static {
        g_convList.add( Pair.makePair() )
    }
    public static <V> StringConverter<V> of( Class<V> vClass )
    {
        if(V )
    }
    public static <V> StringConverter<V> put( Class<V> vClass, StringConverter<V> cnv )
    {
    }
    */
}
