package ru.inversion.fx.form.controls.textarea;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ResourceBundle;

/** */
public class JInvStyleTextArea extends StyleClassedTextArea {

    private final static ResourceBundle FORE = ResourceBundle.getBundle("fore");

    private final BooleanProperty pausedScrollProperty = new SimpleBooleanProperty(false);

    public JInvStyleTextArea() {
        getStyleClass().add("styleTextArea");
        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setStyle("-fx-font-family:'FontAwesome';");

        // Копировать выделенный текст в буффер обмена
        final MenuItem copyItem = new MenuItem(FORE.getString("CONTEXT_MENU_COPY_ITEM"));
        copyItem.setOnAction(event -> {
            final StringSelection selection = new StringSelection(this.getSelectedText());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        });

        final MenuItem selectAll = new MenuItem(FORE.getString("CONTEXT_MENU_SELECT_ALL_ITEM"));
        selectAll.setOnAction(event -> this.selectAll());

        contextMenu.getItems().addAll(copyItem, selectAll);
        this.setContextMenu(contextMenu);
        this.setOnContextMenuRequested(event -> {
            if (getSelectedText() == null || getSelectedText().isEmpty()) {
                copyItem.setDisable(true);
            } else {
                copyItem.setDisable(false);
            }
        });
    }

    public void setText(String text) {
        if (text == null) return;
        replaceText(text);
        moveTo(0);
        requestFollowCaret();
    }

    /**
     * Добавление текста с автоскроллингом
     * @param text текст
     */
    @Override
    public void appendText(String text) {
        if (text == null) return;

        super.appendText(text);

        if (!isPausedScroll()) {
            moveTo(getCaretPosition());
            requestFollowCaret();
        }
    }

    /**
     * Остановить автоскроллинг
     */
    public void pauseScroll(boolean pause) {
        pausedScrollProperty.set(pause);
    }

    public boolean isPausedScroll() {
        return pausedScrollProperty.get();
    }

    public BooleanProperty pausedScrollPropertyProperty() {
        return pausedScrollProperty;
    }
}
