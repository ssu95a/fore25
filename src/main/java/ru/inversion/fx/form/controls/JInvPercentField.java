package ru.inversion.fx.form.controls;

import java.math.BigDecimal;

/**
 *
 * @author ssu @
 */
public class JInvPercentField extends JInvBigDecimalField {


    static private int defaultFractionDigit = 2;

    /**
     *      */
    static public void initDefaultPercentFormatter(int fraction) {
        defaultFractionDigit = fraction;
    }

    /**
     *      */
    public JInvPercentField() {
    }

    /**
     *      */
    public JInvPercentField(String arg0) {
        super(arg0);
    }

    /**
     *      */
    public JInvPercentField(BigDecimal value) {
        super(value);
    }

    /**
     *      */
    @Override
    public int getFractionDigits() {
        return fractionDigitsProperty == null ? defaultFractionDigit : super.getFractionDigits();
    }
}
