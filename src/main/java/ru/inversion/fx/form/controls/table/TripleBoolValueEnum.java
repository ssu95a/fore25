
package ru.inversion.fx.form.controls.table;

import ru.inversion.fx.app.Tags;

import java.util.ResourceBundle;

/** */
public enum TripleBoolValueEnum {

    ON,

    OFF,

    LAST;

    final private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    public int toInt() {

        switch( this ) {
            case ON:  return -1;
            case OFF: return -2;
        }
        return 0;
    }

    @Override
    public String toString() {
        return bundle.getString("TripleBoolValueEnum." + this.name() );
    }

    /** */
    static public TripleBoolValueEnum fromInt( int i ) {

        switch( i ) {
            case -1:
                return ON;
            case -2:
                return OFF;
        }
        return LAST;
    }
}
