package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.skin.BigDecimalFieldSkin;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;

/**
 *
 * @author zarubin
 */
@Deprecated
public class JInvBigDecimalSpinner extends Control implements IJInvControl {

     private StringProperty textLabelProperty = new SimpleStringProperty("");
    
    @Override
    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    @Override
    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
    }

    /**
     *
     */
    public Control setLabel(Label label) {
        if (label != null) {
            label.setLabelFor(this);
            textLabelProperty.bind(label.textProperty());
        }
        return this;
    }

    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }

    /**
     * Default constructor. Returns a {@link BigDecimalField} with no number, minValue and maxValue set, but stepwidth 1 and default {@link NumberFormat}.
     */
    public JInvBigDecimalSpinner() {
        super();
        setStyle(null);
        getStyleClass().add("big-decimal-field");
        number = new SimpleObjectProperty<BigDecimal>(this, "number");
        stepwidth = new SimpleObjectProperty<BigDecimal>(this, "stepwidth", BigDecimal.ONE);
        maxValue = new SimpleObjectProperty<BigDecimal>(this, "maxValue");
        minValue = new SimpleObjectProperty<BigDecimal>(this, "minValue");
        format = new SimpleObjectProperty<NumberFormat>(this, "format", NumberFormat.getNumberInstance());
        promptText = new SimpleStringProperty(this, "promptText", "");
        initFocusSimulation();
    }

    /**
     * Initializes a construct, that mimics the focusedProperty() of e.g. a TextField. The focus is forwarded from the inner TextField of the underlying Skin-Implementation.
     */
    private void initFocusSimulation() {
        setFocusTraversable(false);
        skinProperty().addListener((observable) -> {
            Skin<?> skin = getSkin();
            if (skin instanceof BigDecimalFieldSkin) {
                BigDecimalFieldSkin bigDecimalFieldSkin = (BigDecimalFieldSkin) skin;
                bigDecimalFieldSkin.focusForward.addListener((observable2) -> {
                    super.setFocused(bigDecimalFieldSkin.focusForward.get());
                });
            }
        });
    }

    /**
     * Returns a {@link BigDecimalField} with stepwidth 1 and {@link #number} set to initialValue.
     *
     * @param initialValue The initial BigDecimal value of this control.
     */
    public JInvBigDecimalSpinner(BigDecimal initialValue) {
        this();
        setNumber(initialValue);
    }

    /**
     * @param initialValue The initial BigDecimal value of this control.
     * @param stepwidth The stepwidth for increment/decrement operations.
     * @param format The NumberFormat that is used to format the number in the control.
     */
    public JInvBigDecimalSpinner(BigDecimal initialValue, BigDecimal stepwidth,
        NumberFormat format) {
        this();
        this.number.set(initialValue);
        this.stepwidth.set(stepwidth);
        this.format.set(format);
    }

    /**
     * @return The formatted String representation of {@link #number}
     */
    public String getText() {
        if (number.getValue() != null) {
            return getFormat().format(number.getValue());
        } else {
            return null;
        }
    }

    /**
     * @param formattedNumber representation of {@link #number}
     */
    public void setText(String formattedNumber) {
        try {
            Number parsedNumber = getFormat().parse(formattedNumber);
            setNumber(new BigDecimal(parsedNumber.toString()));
        } catch (ParseException ex) {
            Logger.getLogger(JInvBigDecimalSpinner.class.getName()).log(Level.INFO,
                null, ex);
        }
    }

    /**
     * increments the number by {@link #stepwidth}
     */
    public void increment() {
        if (getNumber() != null && getStepwidth() != null) {
            BigDecimal newValue = getNumber().add(getStepwidth());
            if (checkBounds(newValue) == false) {
                return;
            }
            setNumber(newValue);
        }
    }

    /**
     * decrements the number by {@link #stepwidth}
     */
    public void decrement() {
        if (getNumber() != null && getStepwidth() != null) {
            BigDecimal newValue = getNumber().subtract(getStepwidth());
            if (checkBounds(newValue) == false) {
                return;
            }
            setNumber(newValue);
        }
    }
    /**
     * Contains the number that is controlled in this {@link jfxtras.labs.scene.control.BigDecimalField}.
     */
    final private ObjectProperty<BigDecimal> number;

    /**
     * @return The number of this control as {@link java.math.BigDecimal}.
     */
    public BigDecimal getNumber() {
        return number.getValue();
    }

    /**
     *
     * @param value
     * @throws java.lang.IllegalArgumentException if minValue and/or maxValue are set and value is out of these bounds.
     */
    public void setNumber(BigDecimal value) {
        if (checkBounds(value) == false) {
            String message = MessageFormat.format("number {0} is out of bounds({1}, {2})", value, minValue.get(), maxValue.get());
            throw new IllegalArgumentException(message);
        }
        number.set(value);
    }

    /**
     * Checks if value is between minValue and maxValue (both including) if set at all.
     *
     * @param value
     * @return
     */
    private boolean checkBounds(BigDecimal value) {
        if (value != null && getMaxValue() != null && value.compareTo(getMaxValue()) > 0) {
            return false;
        }
        if (value != null && getMinValue() != null && value.compareTo(getMinValue()) < 0) {
            return false;
        }
        return true;
    }

    /**
     * @return The property containing the BigDecimal number
     */
    public ObjectProperty<BigDecimal> numberProperty() {
        return number;
    }
    /**
     * Stepwidth for inc/dec operation
     */
    final private ObjectProperty<BigDecimal> stepwidth;

    public BigDecimal getStepwidth() {
        return stepwidth.getValue();
    }

    public void setStepwidth(BigDecimal value) {
        stepwidth.set(value);
    }

    public ObjectProperty<BigDecimal> stepwidthProperty() {
        return stepwidth;
    }
    /**
     * Property that contains the {@link java.text.NumberFormat} that is used to format and parse the {@link #number}.
     */
    final private ObjectProperty<NumberFormat> format;

    public NumberFormat getFormat() {
        return format.getValue();
    }

    public final void setFormat(NumberFormat value) {
        format.set(value);
    }

    public ObjectProperty<NumberFormat> formatProperty() {
        return format;
    }
    /**
     * Contains the text that is displayed in the control if no {@link #number} is set.
     */
    final private StringProperty promptText;

    public String getPromptText() {
        return promptText.getValue();
    }

    public final void setPromptText(String value) {
        promptText.setValue(value);
    }

    public StringProperty promptTextProperty() {
        return promptText;
    }
    /**
     * If set the control does not allow to enter a {@link #number} that is greater than maxValue.
     */
    final private ObjectProperty<BigDecimal> maxValue;

    public BigDecimal getMaxValue() {
        return maxValue.getValue();
    }

    public void setMaxValue(BigDecimal value) {
        maxValue.set(value);
    }

    public ObjectProperty<BigDecimal> maxValueProperty() {
        return maxValue;
    }
    /**
     * If set the control does not allow to enter a {@link #number} that is smaller than minValue.
     */
    final private ObjectProperty<BigDecimal> minValue;

    public BigDecimal getMinValue() {
        return minValue.getValue();
    }

    public void setMinValue(BigDecimal value) {
        minValue.set(value);
    }

    public ObjectProperty<BigDecimal> minValueProperty() {
        return minValue;
    }

    @Override
    public String getUserAgentStylesheet() {
        return getClass().getResource(
            "/ru/inversion/fx/control/"
            + getClass().getSimpleName() + ".css").toExternalForm();
    }

    @Override
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {

        return Controls.<T>getDsAdapterFromControl(this);
    }

    @Override
    public void setToolTipText(String toolTipText) {
    }

    @Override
    public String getToolTipText() {
        return null;
    }
    
    @Override
    public StringProperty labelTextProperty() {
        return textLabelProperty;
    }
}
