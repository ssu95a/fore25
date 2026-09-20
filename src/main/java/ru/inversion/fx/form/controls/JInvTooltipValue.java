/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author antonovdi
 */
public class JInvTooltipValue<T> extends Tooltip {

    private Function<T, String> toolTipConverter;
    private StringConverter toolTipStringConverter;

    private BooleanSupplier showPredicate;

    public JInvTooltipValue(Supplier<T> valueSupplier, BooleanSupplier showCondition) {
        this(valueSupplier, showCondition, null);
    }

    public JInvTooltipValue(Supplier<T> valueSupplier, StringConverter converter) {
        this(valueSupplier, null, converter);
    }

    public JInvTooltipValue(Supplier<T> valueSupplier, BooleanSupplier showCondition, StringConverter converter) {
        super();
        toolTipStringConverter = converter;
        this.showPredicate = showCondition;
        setOnShowing((WindowEvent event) -> {

            T value = valueSupplier.get();
            if (value != null) {
                if (toolTipConverter != null) {
                    setText(toolTipConverter.apply(value));
                } else if (toolTipStringConverter != null) {
                    setText(toolTipStringConverter.toString(value));
                } else {
                    setText(value.toString());
                }
            } else {
                setText(null);
            }
        });
    }

    public JInvTooltipValue(Supplier<T> valueSupplier) {
        this(valueSupplier, null, null);
    }

    @Override
    public void show(Window ownerWindow, double anchorX, double anchorY) {

        if (showPredicate != null && !showPredicate.getAsBoolean()) {
            return;
        }
        super.show(ownerWindow, anchorX, anchorY);
    }

    /**
     *
     * @param converter
     */
    public final void setToolTipConverter(Function<T, String> converter) {
        toolTipStringConverter = null;
        showPredicate = null;
        toolTipConverter = converter;
    }

    /**
     *
     * @param converter
     */
    public final void setToolTipConverter(StringConverter converter) {
        toolTipConverter = null;
        showPredicate = null;
        toolTipStringConverter = converter;
    }

    /**
     *
     * @param converter
     */
    public final void setToolTipConverter(StringConverter converter, BooleanSupplier showCondition) {
        setToolTipConverter(converter);
        showPredicate = showCondition;
    }
}
