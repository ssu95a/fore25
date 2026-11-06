package ru.inversion.diff;

public interface DiffItem<T> {

    Class<T> getValueType();

    String getCategory();

    String getName();

    T getOldValue();

    T getNewValue();

}
