package ru.inversion.fx.form.valid;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Локализация ошибок валидации
 * Singleton
 * @author lebedev-m
 */
public class JInvValidLocalized
{
    private static ResourceBundle bundle = ResourceBundle.getBundle( "valid" );
    
    /* */
    public static void setBundle( String baseName )
    {
        bundle = ResourceBundle.getBundle( baseName );
    }
    
    /* */
    public static String getLocalMessage( String pattern, Object... args ) 
    {
        if ( args.length == 0 )
            return bundle.getString(pattern);
        String msg;
        try {
            msg = MessageFormat.format( bundle.getString( pattern ), args );
        } catch ( Throwable e ) {
            msg = e.toString();
        }
        return msg;
    }
}
