package ru.inversion.fx.form;

import javafx.util.StringConverter;

import java.util.ResourceBundle;
import java.util.Set;

/**
 *
 * @author psh
 */
public class BundleStringConverter extends StringConverter <String> 
{
    final ResourceBundle bundle;

    /** */
    public BundleStringConverter ( ResourceBundle bundle ) {
        this.bundle = bundle;
    }

    /** */
    public Set<String> keySet ()
    {
        return bundle.keySet ();
    }            

    /** */
    @Override
    public String toString (String key) 
    {
        if( bundle.containsKey (key) )
            return bundle.getString (key);
        return key + "-???";
    }
    
    /** */
    @Override
    public String fromString (String string) {
        return string;
    }
}
