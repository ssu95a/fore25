package ru.inversion.fx.form.controls;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 *
 * @author antonovdi
 */
public class JInvTableColumnMoney<S,T> extends JInvTableColumnBigDecimal<S, BigDecimal >{

    /** */
    public JInvTableColumnMoney() {
        super();
    }

    @Override
    public boolean getShowGroups() {
        return true;
    }
    @Override
    public void setShowGroups( boolean v ) {
    }

    /** */
    @Override
    public DecimalFormat getDecimalFormat() {
        return JInvMoneyField.g_moneyFormat;
    }
}
