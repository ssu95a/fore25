package ru.inversion.fx.form.controls.renderer;

import javafx.css.PseudoClass;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 Ячейка, которую можно раскрасить через функцию
 (можно раскрасить исходя из пожика, к которому оная относится)
 @author fomishkin on 21.02.2019. */
public interface IColoredRow<T> extends IColoredBase<T> {
    void addColor( Function<IColoredRow<T>, Colorizer> styleExpr );
}
