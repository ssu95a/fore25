package ru.inversion.fx.form.valid.validators;

import ru.inversion.fx.form.valid.JInvValidLocalized;
import ru.inversion.fx.form.valid.Validator;

/**
 *
 * @author antonovdi
 */
public class LengthValidator implements Validator {

    private final int length;
    /**
     *      */
    public LengthValidator( int length ) {
        this.length = length;
    }

    /**
     *      */
    @Override
    public Validator.Result validate(Object value) {

        if( value != null )
        {
            String val = value.toString();
            int lengthValue = val.length();
            if( lengthValue>length )
                return new Validator.Result(null, JInvValidLocalized.getLocalMessage( "LENGTH", length, lengthValue));
        }
        return null;
    }

}
