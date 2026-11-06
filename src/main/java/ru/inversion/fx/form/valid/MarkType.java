package ru.inversion.fx.form.valid;
/**
 @author fomishkin on 20.07.2018.
 */
public enum MarkType {
    NONE,
    /** Поле, обязательное для заполнения */
    REQUIRED,
    /** Компонент, привязанный к каким-либо валидаторам */
    VALIDATABLE
}
