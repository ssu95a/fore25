package ru.inversion.fx.form.controls;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;

/**
 * Интерфейс для GUI контрола
 */
public interface IProgressControl {

    /** Значение червяка */
    public DoubleProperty progressProperty();

    /** Текст*/
    public StringProperty textProperty();

}
