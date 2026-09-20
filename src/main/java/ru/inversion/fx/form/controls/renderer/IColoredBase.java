package ru.inversion.fx.form.controls.renderer;

import javafx.css.PseudoClass;

import java.util.function.Predicate;

public interface IColoredBase< T > {
    PseudoClass COLORIZED_BACKGROUND = PseudoClass.getPseudoClass("colorized-background");
    PseudoClass COLORIZED_BACKGROUND_SECONDARY = PseudoClass.getPseudoClass("colorized-background-secondary");
    PseudoClass COLORIZED_TEXT = PseudoClass.getPseudoClass("colorized-text");
    PseudoClass COLORIZED_TEXT_SECONDARY = PseudoClass.getPseudoClass("colorized-text-secondary");
    Predicate< Colorizer > IS_CUSTOM_BACKGROUND = c -> c.getBackgroundColor() != null;
    Predicate< Colorizer > IS_CUSTOM_TEXT = c -> c.getTextColor() != null;
    Predicate< Colorizer > IS_CUSTOM_BACKGROUND_SECONDARY = c -> c.getBackgroundSecondaryColor() != null;
    Predicate< Colorizer > IS_CUSTOM_TEXT_SECONDARY = c -> c.getTextSecondaryColor() != null;

    T getPojo();

    void clearColor();
}
