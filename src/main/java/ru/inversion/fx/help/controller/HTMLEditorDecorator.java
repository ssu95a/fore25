package ru.inversion.fx.help.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.web.HTMLEditor;

/**
 * Расширяет функционал HTMLEditor.
 * Внутренний класс
 *
 * @author perov
 * @version 1.0.0
 */
class HTMLEditorDecorator extends HTMLEditor implements HtmlEditorToolBar {

    private final BooleanProperty keyPressedProperty = new SimpleBooleanProperty(this, "keyPressedProperty");
    private final BooleanProperty setTextProperty = new SimpleBooleanProperty(this, "setTextProperty");

    @Override
    public BooleanProperty getKeyPressedProperty() {
        return keyPressedProperty;
    }

    @Override
    public BooleanProperty getSetTextProperty() {
        return setTextProperty;
    }

    @Override
    public void addItem(Node node) {
        Node toolbar = this.lookup(".top-toolbar");
        if (toolbar instanceof ToolBar) {
            ToolBar bar = (ToolBar) toolbar;
            bar.getItems().add(node);
        }
    }

    public HTMLEditorDecorator() {
        super();

        this.setOnKeyReleased((KeyEvent event) -> {
            keyPressedProperty.set(false);
            keyPressedProperty.set(true);
        });
    }

    @Override
    public String getText() {
        return getHtmlText();
    }

    @Override
    public void setText(String value) {        
        setHtmlText(value);
        setTextProperty.set(false);
        setTextProperty.set(true);
    }
    
    

}
