package ru.inversion.diff;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.Objects;

public final class DiffNodeElement {

    private final DiffItemResult value;

    private final boolean category;

    private BooleanProperty changeValue = new SimpleBooleanProperty();

    DiffNodeElement(DiffItemResult value) {
        this(value, false);
    }

    DiffNodeElement(DiffItemResult value, boolean category) {
        this.value = Objects.requireNonNull(value, "value can't be null");
        this.category = category;
    }

    public DiffItemResult getValue() {
        return value;
    }

    public boolean isCategory() {
        return category;
    }

    public boolean isChangeValue() {
        return changeValue.get();
    }

    public BooleanProperty changeValueProperty() {
        return changeValue;
    }

    public void setChangeValue(boolean changeValue) {
        this.changeValue.set(changeValue);
    }
}
