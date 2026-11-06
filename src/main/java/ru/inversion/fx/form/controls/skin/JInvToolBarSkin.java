package ru.inversion.fx.form.controls.skin;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ToolBar;
import javafx.scene.control.skin.ToolBarSkin;

import java.util.List;

/** Штатный skin JavaFX с совместимым доступом к CSS-свойствам панели. */
public class JInvToolBarSkin extends ToolBarSkin {

    public JInvToolBarSkin(ToolBar toolbar) {
        super(toolbar);
    }

    public final void setSpacing(double value) {
        spacingProperty().set(snapSpacing(value));
    }

    public final double getSpacing() {
        return snapSpacing(spacingProperty().get());
    }

    public final DoubleProperty spacingProperty() {
        return (DoubleProperty) styleableProperty("-fx-spacing");
    }

    public final void setBoxAlignment(Pos value) {
        boxAlignmentProperty().set(value);
    }

    public final Pos getBoxAlignment() {
        return boxAlignmentProperty().get();
    }

    @SuppressWarnings("unchecked")
    public final ObjectProperty<Pos> boxAlignmentProperty() {
        return (ObjectProperty<Pos>) styleableProperty("-fx-alignment");
    }

    private double snapSpacing(double value) {
        return getSkinnable().getOrientation() == Orientation.VERTICAL
                ? snapSpaceY(value) : snapSpaceX(value);
    }

    private StyleableProperty<?> styleableProperty(String name) {
        ToolBar toolbar = getSkinnable();
        // Metadata штатного skin получает свойства через toolbar.getSkin().
        if (toolbar == null || toolbar.getSkin() != this)
            throw new IllegalStateException("Install this skin on the toolbar before accessing its properties");
        for (CssMetaData<? extends Styleable, ?> metadata : ToolBarSkin.getClassCssMetaData()) {
            if (name.equals(metadata.getProperty()))
                return property(metadata, toolbar);
        }
        throw new IllegalStateException("Missing ToolBarSkin CSS property: " + name);
    }

    @SuppressWarnings("unchecked")
    private static StyleableProperty<?> property(CssMetaData<? extends Styleable, ?> metadata, ToolBar toolbar) {
        return ((CssMetaData<ToolBar, ?>) metadata).getStyleableProperty(toolbar);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return ToolBarSkin.getClassCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
}
