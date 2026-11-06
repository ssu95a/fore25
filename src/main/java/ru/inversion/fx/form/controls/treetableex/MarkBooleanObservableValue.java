package ru.inversion.fx.form.controls.treetableex;

import javafx.beans.property.BooleanPropertyBase;
import ru.inversion.dataset.mark.IMarkable;

public class MarkBooleanObservableValue extends BooleanPropertyBase {

    private IMarkable markable = null;

    public MarkBooleanObservableValue( ) {
    }
    /** */
    @Override
    public Object getBean() {
        return markable;
    }

    @Override
    public String getName() {
        return "markable";
    }
    /** */
    @Override
    public Boolean getValue() {

        if( markable == null )
            return null;
        return markable.isMark();
    }
    /** */
    public MarkBooleanObservableValue setMarkable( IMarkable m ) {
        this.markable = m;
        return this;
    }
    /** */
    @Override
    public void set( boolean value ) {
    }
}
