package ru.inversion.fx.form.controls.textarea;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import org.fxmisc.flowless.Virtualized;
import org.fxmisc.flowless.VirtualizedScrollPane;

public class JInvVirtualizedScrollPane<V extends Node & Virtualized> extends VirtualizedScrollPane<V> {

    private final BooleanProperty scrollYMaxProperty = new SimpleBooleanProperty(false);

    private Double minVal = null;

    public JInvVirtualizedScrollPane(V content, ScrollPane.ScrollBarPolicy hPolicy, ScrollPane.ScrollBarPolicy vPolicy) {
        super(content, hPolicy, vPolicy);
        initListener();
    }

    public JInvVirtualizedScrollPane(V content) {
        super(content);
        initListener();
    }

    public ScrollBar getVScrollBar() {
        final Node node = lookup(".scroll-bar:vertical");
        if (node != null && node instanceof ScrollBar) {
            return ((ScrollBar) node);
        }
        return null;
    }

    private void initListener() {
        // Слушатель, определяющий, находится ли вертикальный scrollBar в максимальном положении
        estimatedScrollYProperty().addListener((observable, oldValue, newValue) -> {
            final Double totalHeight = totalHeightEstimateProperty().getValue();
            final double v = (totalHeight != null ? totalHeight : 0.0) - getEstimatedScrollY();
            final double scrollPaneHeight = getHeight();

            if ((minVal == null || v < minVal) && (scrollPaneHeight == v)) {
                minVal = v;
                scrollYMaxProperty.set(true);
            } else if (minVal != null && minVal == v) {
                scrollYMaxProperty.set(true);
            } else if (v < scrollPaneHeight && ((scrollPaneHeight - v) <= 8)) {
                // Допускаем небольшую погрешность, если она имела место быть - считаем, что scrollBar находится внизу
                scrollYMaxProperty.set(true);
            } else {
                scrollYMaxProperty.set(false);
            }
        });
    }

    public boolean isScrollYMax() {
        return scrollYMaxProperty.get();
    }

    public BooleanProperty scrollYMaxProperty() {
        return scrollYMaxProperty;
    }
}
