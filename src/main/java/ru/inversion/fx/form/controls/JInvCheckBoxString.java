/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * Контрол преобразующий строковые данные в форму булевого значения.
 * <p>
 * @author antonovdi
 */
public class JInvCheckBoxString extends JInvCheckBox {

    public final StringProperty propertyValueChecked = new SimpleStringProperty();
    public final StringProperty propertyValueUnchecked = new SimpleStringProperty();
    public final ObjectProperty<OtherValuesEnum> propertyOtherValues = new SimpleObjectProperty<>();

    private final StringProperty valueProperty = new CheckBoxValueProperty();

    public JInvCheckBoxString() {

        super();

        setAllowIndeterminate(true);

        indeterminateProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {

            if (newValue) {
                setValue(null);
            }
        });

        selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {

            if (newValue) {
                setValue(getValueChecked());
            } else {
                setValue(getValueUnchecked());
            }
        });
    }

    public JInvCheckBoxString(String text) {

        super(text);
        setAllowIndeterminate(true);

        selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {

            if (newValue) {
                setValue(getValueChecked());
            } else {
                setValue(getValueUnchecked());
            }
        });
    }

    boolean indeteremFlag = true;

    private class CheckBoxValueProperty extends SimpleStringProperty {

        @Override
        public void set(String value) {

            Boolean selected = null;

            if (value != null) {
                if (value.equals(getValueChecked())) {
                    selected = true;
                } else if (value.equals(getValueUnchecked())) {
                    selected = false;
                }
            } else if (getValueChecked() == null) {
                selected = true;
            } else if (getValueUnchecked() == null) {
                selected = false;
            }

            if (selected == null) {
                if (OtherValuesEnum.CHECKED.equals(getOtherValues())) {
                    selected = true;
                } else if (OtherValuesEnum.UNCHECKED.equals(getOtherValues())) {
                    selected = false;
                } else if (isAllowIndeterminate()) {
                    setIndeterminate(true);
                } else {
                    throw new IllegalArgumentException("value - " + value + " not allowed");
                }
            }

            if (selected != null) {

                if (isIndeterminate()) {
                    setIndeterminate(false);
                }

                setSelected(selected);
            }

            super.set(value);
        }

        @Override
        public String get() {

            return super.get();

//            if (isIndeterminate()) {
//                return null;
//            }
//
//            if (isSelected()) {
//                return getValueChecked();
//            }
//
//            return getValueUnchecked();
        }

    }

    /**
     * Установить значение, при котором значение компонента будет true
     *
     * @param value
     */
    public void setValueChecked(String value) {
        propertyValueChecked.set(value != null && value.isEmpty() ? null : value);
    }

    public String getValueChecked() {
        return propertyValueChecked.get();
    }

    /**
     * Установить значение, при котором значение компонента будет false
     *
     * @param value
     */
    public void setValueUnchecked(String value) {
        propertyValueUnchecked.set(value != null && value.isEmpty() ? null : value);
    }

    public String getValueUnchecked() {
        return propertyValueUnchecked.get();
    }

    /**
     * Устанавливает режим работы компонента при наличие другого строго значения в отличие от значений возвращаемых в результате методов {@link #getValueChecked() } и {@link #getValueUnchecked() () }
     * при условии что установлен в true метод {@link #setAllowIndeterminate(boolean)}
     *
     * @param value
     */
    public void setOtherValues(OtherValuesEnum value) {
        propertyOtherValues.set(value);
    }

    /**
     * Возвращает режим работы компонента при наличие другого строго значения в отличие от значений возвращаемых в результате методов {@link #getValueChecked() } и {@link #getValueUnchecked() () }
     *
     * @param value
     */
    public OtherValuesEnum getOtherValues() {
        return propertyOtherValues.get();
    }

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public String getValue() {
        return valueProperty.get();
    }

    public void setValue(String value) {
        valueProperty.set(value);
    }

    public enum OtherValuesEnum {
        CHECKED, UNCHECKED
    }
}
