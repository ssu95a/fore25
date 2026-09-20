package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 *
 * @author antonovdi
 */
public abstract class AbstractBigDecimalField extends JInvNumberField<BigDecimal> {


    /**
     *
     */
    protected ObjectProperty<BigDecimal> valueProperty;

    /**
     *
     */
    public AbstractBigDecimalField() {
    }

    /**
     *
     */
    public AbstractBigDecimalField(String arg0) {
        super(arg0);
    }

    /**
     *
     */
    public AbstractBigDecimalField(BigDecimal value) {
        super(value);
    }

    /**
     *
     */
    @Override
    public ObjectProperty<BigDecimal> valueProperty() {

        if (valueProperty == null) {

            valueProperty = new SimpleObjectProperty<BigDecimal>(this, "value") {
//                @Override
//                public BigDecimal get() {
//                    return getState( ) == VALUE ? super.get() : null; //To change body of generated methods, choose Tools | Templates.
//                }

                @Override
                public void set(BigDecimal newValue) {

                    if (newValue != null) {
                        newValue = newValue.setScale(getFractionDigits(), RoundingMode.HALF_UP );
                    }

                    super.set(newValue);
                }

            };
        }
        return valueProperty;
    }

    /**
     *
     */
    public abstract int getFractionDigits();

    @Override
    public Class getClassValue() {
        return BigDecimal.class;
    }

}
