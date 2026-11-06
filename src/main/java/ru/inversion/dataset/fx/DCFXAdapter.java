package ru.inversion.dataset.fx;

import javafx.beans.property.Property;
import javafx.scene.control.Control;
import ru.inversion.datacall.DataCallEvent;
import ru.inversion.datacall.IDataCall;
import ru.inversion.datacall.IDataCallListener;
import ru.inversion.datacall.ParameterMetadata;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import javax.persistence.ParameterMode;
import java.util.Objects;

import static javax.persistence.ParameterMode.*;

/**
 *  Адаптер для связывания параметров из DataCall
 *  с органами управления на форме
 */
public class DCFXAdapter implements IDataCallListener, AutoCloseable {

    /** */
    public static DCFXAdapter NEW ( final IDataCall dataCall ) {
        return new DCFXAdapter(dataCall);
    }

    /** */
    public static DCFXAdapter bind( final IDataCall dataCall ) {
        return new DCFXAdapter(dataCall);
    }

    /** */
    private IDataCall dataCall;

    /** */
    private DCFXAdapter( final IDataCall dataCall ) {
        this.dataCall = dataCall;
    }

    /** */
    public DCFXAdapter execute( ) {

        try {
            dataCall.execute();
            return this;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT + "Error on execute DataCall", th );
        }
    }

    /** */
    public <T> DCFXAdapter bindProperty( Object id, Property<T> p ) {

        try {

            Objects.requireNonNull( p, "'p' is null" );

            final ParameterMetadata pm = dataCall.getParameterMetadata( Objects.requireNonNull( id, "'id' is null" ) );

            if( pm == null )
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "No parameter with id '" + id + "' in DataCall" );

            if( pm.getMode() == IN ) {
                dataCall.setValueCallback( id, ()->p.getValue() );
            }
            else if( pm.getMode() == OUT ) {
                dataCall.setValueListener(id, ( idp, value ) -> p.setValue( (T)value ) );
            }
            else
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "No bind for INOUT parameters" );

            return this;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT + "Error on bind control to dataCall parameter '" + id + "'", th );
        }
    }

    /** */
    public DCFXAdapter bind( Object id, Control c) {
        bind( id, c, null ); return this;
    }

    /**
     *  Связывание параметра из DataCall и компонентом на форме
     *
     * @param bindMode - только для INOUT параметров, в остальных случаях игнорируется
     */
    public DCFXAdapter bind( Object id, Control c, ParameterMode bindMode ) {

        try {

            Objects.requireNonNull( id, "'id' is null" );

            if( bindMode != null && !U.in( bindMode, IN, OUT ) )
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'bindMode' must be 'IN' or 'OUT'" );

            final ParameterMetadata pm = dataCall.getParameterMetadata( id );

            if( pm == null )
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "No parameter with id '" + id + "' in DataCall" );

            if( pm.getMode() == IN || ( pm.getMode() == INOUT && bindMode == IN )) {
                dataCall.setValueCallback( id, ()->Controls.getValue(c) );
            }
            else if( pm.getMode() == OUT || ( pm.getMode() == INOUT && bindMode == OUT )) {
                dataCall.setValueListener(id, ( idp, value ) -> Controls.setValue( c, value ) );
            }

            return this;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on bind control to dataCall parameter '" + id + "'", th );
        }
    }

    /** */
    public DCFXAdapter bindControl( Control ... controls ) {

        if( controls == null || controls.length == 0 )
            return this;

        for( Control control : controls) {

            String fieldName = Controls.getFieldNameFromControl( control );

            if( S.isNotNullOrEmpty(fieldName) ) {
                bind( fieldName, control );
            }
        }

        return this;
    }

    /** */
    public DCFXAdapter bindIn( Control ... controls )
    {
        if( controls == null || controls.length == 0 )
            return this;

        for( Control control : controls )
        {
            String fieldName = Controls.getFieldNameFromControl( control );

            if( S.isNotNullOrEmpty(fieldName) ) {
                bind( fieldName, control );
            }
        }

        return this;
    }


    /** */
    public IDataCall getDataCall( ) {
        return dataCall;
    }

    /** */
    @Override
    public void close() throws Exception {
        /** */
        if( dataCall != null ) {
            dataCall.close();
            dataCall = null;
        }
    }
    /** */
    @Override
    public void dataCallChanged( DataCallEvent e ) {
    }

    /** */
    public void linkTo( ) { }
}
