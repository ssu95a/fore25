package ru.inversion.fx.form.controls;

import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 *
 * @author antonovdi
 */
public class JInvMoneyField extends AbstractBigDecimalField {

    public static DecimalFormat g_moneyFormat;

    public final static StringConverter<BigDecimal> stringConverter = new MoneyStringConverter();

    /** */
    static public void initDefaultMoneyFormatter( int fraction, char decimalSeparator, boolean grouping ) {

        DecimalFormatSymbols ds = new DecimalFormatSymbols();
        ds.setMonetaryDecimalSeparator(decimalSeparator);
        ds.setDecimalSeparator(decimalSeparator);
        if (grouping) {
            ds.setGroupingSeparator(' ');
        }

        g_moneyFormat = new DecimalFormat();
        g_moneyFormat.setDecimalFormatSymbols(ds);
        g_moneyFormat.setDecimalSeparatorAlwaysShown(true);
        g_moneyFormat.setGroupingSize(3);
        g_moneyFormat.setMaximumFractionDigits(fraction);
        g_moneyFormat.setParseBigDecimal(true);
        g_moneyFormat.setMinimumFractionDigits(fraction);

    }

    @Override
    protected StringConverter<BigDecimal> getConverter() {
        return stringConverter;
    }

    static {
        initDefaultMoneyFormatter( 2, '.', true );
    }

    /** */
    public JInvMoneyField() {
        this(null);
    }

    /** */
    public JInvMoneyField(String arg0) {
        super(arg0);
    }

    /** */
    @Override
    public int getFractionDigits() {
        return g_moneyFormat.getMaximumFractionDigits();
    }

}
