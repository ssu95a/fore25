package ru.inversion.fx.form.valid.validators;

import ru.inversion.fx.form.valid.JInvValidLocalized;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.utils.S;
import ru.inversion.utils.scheck.JInvStringWorker;
import ru.inversion.utils.scheck.JInvStringWorkerException;
import ru.inversion.utils.scheck.JInvStringWorkerValidationException;

/**
 */
public class PatternValidator<T> implements Validator<T>
{
    private static final  String ERR_MES = JInvValidLocalized.getLocalMessage("REGEX_PATTERN_NOT_MATCHED") + ": ";

    private final String descr_key;
    private final String pattern;
    private final String description;
    private final boolean isId;
    private final int group;

    public PatternValidator( String pattern, int group, boolean isId, String description )
    {
        this( pattern, group, isId, description, S.EMPTY_STRING);
    }

    /** */
    public PatternValidator( String pattern, int group, boolean isId, String description, String descr_key )
    {
        this.pattern = pattern;
        this.group = group;
        this.isId = isId;
        this.description = description;
        this.descr_key = descr_key;
    }

    /** */
    @Override
    public Validator.Result validate( Object value )
    {
        if ( value != null )
        {
            String val = value.toString();
            try
            {
                if ( isId )
                {
                    JInvStringWorker.INSTANCE().check( pattern, val, true );
                }
                else
                {
                    JInvStringWorker.INSTANCE().checkRegExp( pattern, group, val, true );
                }
            }
            catch ( JInvStringWorkerValidationException ex )
            {
                return new Validator.Result( null, S.isNullOrEmpty( description ) ? ex.getLocalizedMessage() : (ERR_MES + description) );
            }
            catch ( JInvStringWorkerException ex )
            {
                return new Validator.Result( null, ex.getLocalizedMessage());
            }
        }
        return null;
    }

    public String getDescr_key() {
        return descr_key;
    }

    public String getDescription() {
        return description;
    }
}