package ru.inversion.fx.form.valid.validators;

import ru.inversion.fx.form.valid.JInvValidLocalized;
import ru.inversion.fx.form.valid.Validator;



/**
 * Check the required fields
 *
 * @author lebedev-m
 * @param <T>
 */
public class RangeValidator<T> implements Validator<T>
{
    private final Comparable<T> from, to;

    /**
     * @param from
     * @param to 
     */
    public RangeValidator( Comparable<T> from, Comparable<T> to )
    {
        this.from = from;
        this.to   = to;
    }

    @Override
    public Validator.Result validate( T value )
    {
        if ( value != null )
        {
//            final Comparable<T> c = TypeConverter.convert( value, from.getClass() );

//            if( c.compareTo((T) from) < 0 || c.compareTo((T) to) > 0 )
            if (from.compareTo (value) > 0 || to.compareTo (value) < 0)
            {
                return new Validator.Result( null, JInvValidLocalized.getLocalMessage( "RANGERR", from, to, value ) );
            }
        }
        return null;
    }
}
