package ru.inversion.fx.form.controls;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;

public interface IMultiProgressControl extends IProgressControl {

    DoubleProperty innerProgressProperty();

    StringProperty innerTextProperty();

}
