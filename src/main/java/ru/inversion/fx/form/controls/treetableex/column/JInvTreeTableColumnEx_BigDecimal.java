package ru.inversion.fx.form.controls.treetableex.column;

import javafx.util.StringConverter;
import ru.inversion.fx.form.controls.JInvMoneyField;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.utils.converter.TypeConverter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 *
 * @author Sulimoff
 */
public class JInvTreeTableColumnEx_BigDecimal<P> extends JInvTreeTableColumnEx<P, BigDecimal > {

    // Количество знаков после запятой
    private Integer precision = JInvMoneyField.g_moneyFormat.getMaximumFractionDigits();

    // Разделять на группы
    private Boolean showGroups = Boolean.FALSE;

    // Форматирование
    public DecimalFormat decimalFormat;

    public Boolean getShowGroups() {
        return showGroups;
    }

    public void setShowGroups(Boolean showGroups) {
        this.showGroups = showGroups;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {

        if( this.precision.equals(precision) )
            return;

        this.precision = precision;

        if( precision != null )
        {
            initDecimalFormat();

            if( getTreeTableView() != null)
                getTreeTableView().refresh();
        }
    }

    @Override
    public StringConverter< BigDecimal > getStringConverter() {
        if( this.converter == null )
            initDecimalFormat();
        return super.getStringConverter();
    }

    public DecimalFormat getDecimalFormat() {

        if( decimalFormat == null )
            initDecimalFormat();

        return decimalFormat;
    }

    private void initDecimalFormat() {
        char decimalSeparator = '.';
        DecimalFormatSymbols ds = new DecimalFormatSymbols();
        ds.setMonetaryDecimalSeparator(decimalSeparator);
        ds.setDecimalSeparator(decimalSeparator);

        if (getShowGroups()) {
            ds.setGroupingSeparator(' ');
        }

        decimalFormat = new DecimalFormat();
        decimalFormat.setDecimalFormatSymbols(ds);
        decimalFormat.setDecimalSeparatorAlwaysShown( getPrecision() != 0 );
        decimalFormat.setGroupingSize(3);
        decimalFormat.setMaximumFractionDigits(getPrecision());
        decimalFormat.setParseBigDecimal(true);
        decimalFormat.setMinimumFractionDigits(getPrecision());

        this.converter = new StringConverter< BigDecimal >() {
            @Override
            public String toString( BigDecimal d ) {
                return decimalFormat.format(d);
            }

            @Override
            public BigDecimal fromString( String s ) {
                return TypeConverter.convert( s, BigDecimal.class );
            }
        };
    }

}
