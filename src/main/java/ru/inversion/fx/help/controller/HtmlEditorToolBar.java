package ru.inversion.fx.help.controller;

import javafx.beans.property.BooleanProperty;
import javafx.scene.Node;

/**
 * Внутренний интерфейс
 * @author perov
 */
interface HtmlEditorToolBar {
    void addItem(Node node);
    String getText();
    void setText(String value);
    BooleanProperty getKeyPressedProperty();
    BooleanProperty getSetTextProperty();
}
