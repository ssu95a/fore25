package ru.inversion.fx.form.controls;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.util.StringConverter;
import javafx.util.converter.BigDecimalStringConverter;
import ru.inversion.utils.S;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ssu @
 */
public class JInvBigDecimalField extends AbstractBigDecimalField {

    /** */
    private static final Integer DEFAULT_FRACTION = 2;

    /** */
    private static final boolean DEFAULT_GROUPING_USED = false;

    /** */
    private class JInvBigDecimalConverter extends BigDecimalStringConverter {

        @Override
        public BigDecimal fromString(String value) {

            if( S.isNullOrEmpty(value) )
                return super.fromString(value);
            
            value=value.replaceAll(",", ".");
            return super.fromString( value.replaceAll("\\s", "") );
        }

        @Override
        public String toString( BigDecimal value ) {
            return value == null ? S.EMPTY_STRING : formatter().format( value );
        }
    }

    /** */
    private StringConverter<BigDecimal> g_decConverter;

    /** */
    protected IntegerProperty fractionDigitsProperty;

    /** */
    private DecimalFormat formatter;

    /**
     *
     */
    public JInvBigDecimalField() {
    }

    /**
     *
     */
    public JInvBigDecimalField(String arg0) {
        super( arg0 );
    }

    /**
     *
     */
    public JInvBigDecimalField( BigDecimal value ) {
        super( value );
    }

    /** */
    private DecimalFormat formatter() {
        if( formatter == null )
            formatter = getDecimalFormat( getFractionDigits(), getShowGroups() );
        return formatter;
    }

    /**
     *
     */
    @Override
    public int getFractionDigits() {
        return fractionDigitsProperty == null ? DEFAULT_FRACTION : fractionDigitsProperty().get();
    }

    /** */
    public void setFractionDigits( int nFraction ) {

        if( nFraction != getFractionDigits() )
            fractionDigitsProperty().set(nFraction);
    }

    /**
     * Устанавливает признак отображения групп.
     * По-умолчанию размер группы установлен в размере 3 символов.
     *
     * @param val признак отображения групп.
     */
    public void setShowGroups( boolean val ) {
        if( val != getShowGroups() )
            formatter = getDecimalFormat( getFractionDigits(), val );
    }

    /**
     * Возвращает признак отображения групп.
     * <p>
     * @return признак отображения групп.
     */
    public boolean getShowGroups( ) {
        return formatter == null ? DEFAULT_GROUPING_USED : formatter.isGroupingUsed();
    }

    /**
     *
     */
    public IntegerProperty fractionDigitsProperty( ) {

        if( fractionDigitsProperty == null ) {

            fractionDigitsProperty = new IntegerPropertyBase(DEFAULT_FRACTION) {
                @Override
                public Object getBean() {
                    return JInvBigDecimalField.this;
                }

                @Override
                public String getName() {
                    return "fractionDigits";
                }
            };

            fractionDigitsProperty.addListener( ( o, d, n) -> formatter = getDecimalFormat( n.intValue(), getShowGroups() ));
        }

        return fractionDigitsProperty;
    }

    /**
     *
     */
    @Override
    public StringConverter<BigDecimal> getConverter() {

        if( g_decConverter == null )
            g_decConverter = new JInvBigDecimalConverter();

        return g_decConverter;
    }


    /** */
    final static private List<WeakReference<DecimalFormat>> g_decimalFormatList = new ArrayList<>();

    /** */
    static private DecimalFormat getDecimalFormat( int fraction, boolean groupingUsed ) {

        synchronized(g_decimalFormatList) {

            int nullIndex = -1;
            int i = 0;

            for( WeakReference<DecimalFormat> wdf : g_decimalFormatList) {

                DecimalFormat df = wdf.get();

                if( df == null ) {

                    if( nullIndex == -1 )
                        nullIndex = i;

                    continue;
                }

                if( df.isGroupingUsed() == groupingUsed && df.getMaximumFractionDigits() == fraction )
                    return df;

                i++;
            }//end for

            DecimalFormat df = createFormatter( fraction, groupingUsed );
            if( nullIndex == -1 )
                g_decimalFormatList.add( new WeakReference<>(df) );
            else
                g_decimalFormatList.set( nullIndex, new WeakReference<>(df) );
            return df;
        }//
    }

    /** */
    static private DecimalFormat createFormatter( int fraction, boolean groupingUsed ) {

        DecimalFormatSymbols ds = new DecimalFormatSymbols();

        ds.setMonetaryDecimalSeparator('.');
        ds.setDecimalSeparator('.');
        ds.setGroupingSeparator(' ');

        DecimalFormat df = new DecimalFormat();
        df.setDecimalFormatSymbols(ds);
        df.setDecimalSeparatorAlwaysShown(false);
        df.setGroupingSize( 3 );
        df.setGroupingUsed( groupingUsed );
        df.setMaximumFractionDigits(fraction);
        df.setParseBigDecimal(true);
        df.setMinimumFractionDigits(fraction);

        return df;
    }
}
