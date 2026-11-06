package ru.inversion.fx.form.controls;
/**
 "интерфейсик чтоб потроха Controls.get/setValue на него реагировали
 и соответственно вызывали встроенную в класс переданного объекта реализацию" - psh, JAVAKERNEL-1036
 @author fomishkin on 25.12.2017. */
public interface ICustomValueControl<T> {
    T getCustomValue();
    void setCustomValue(T valueToBeSet);
}
