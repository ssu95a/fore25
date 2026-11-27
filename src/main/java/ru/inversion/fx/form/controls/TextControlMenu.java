package ru.inversion.fx.form.controls;

import java.util.ResourceBundle;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ContextMenuEvent;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.enums.FontAwesome;

/** Состояние контекстного меню создаётся только при запросе меню или его расширений. */
final class TextControlMenu {
    private final TextInputControl control;
    private final ResourceBundle labels;
    private final EventHandler<ActionEvent> editAction;
    private ObservableList<MenuItem> extensions;
    private ContextMenu ownedMenu;
    Consumer<ContextMenu> onPopulate;

    TextControlMenu(TextInputControl control, String bundle, EventHandler<ActionEvent> editAction) {
        this.control = control;
        this.labels = ResourceBundle.getBundle(bundle);
        this.editAction = editAction;
    }

    static boolean isRequestEligible(TextInputControl control, ContextMenuEvent event) {
        return !event.isConsumed() && control.getContextMenu() == null
                && control.getOnContextMenuRequested() == null && !control.contextMenuProperty().isBound()
                && control.getScene() != null && control.getScene().getWindow() != null
                && control.getScene().getWindow().isShowing();
    }

    void show(ContextMenuEvent event) {
        if (ownedMenu == null) {
            ownedMenu = new ContextMenu();
            // JavaFX не начинает показ пустого меню. Команды первого показа готовятся один раз.
            populateItems(ownedMenu);
            ownedMenu.setOnShowing(ignored -> {
                ownedMenu.setOnShowing(next -> populate(ownedMenu));
                if (onPopulate != null) onPopulate.accept(ownedMenu);
            });
            control.contextMenuProperty().addListener((observable, previous, current) -> updateInstallation());
            control.onContextMenuRequestedProperty().addListener((observable, previous, current) -> updateInstallation());
        }
        updateInstallation();
        if (control.getContextMenu() == ownedMenu) {
            // На первом запросе штатный обработчик Control уже прошёл без меню.
            ownedMenu.show(control, event.getScreenX(), event.getScreenY());
            event.consume();
        }
    }

    private void updateInstallation() {
        if (control.getContextMenu() == ownedMenu) {
            if (control.getOnContextMenuRequested() != null) {
                ownedMenu.hide();
                if (!control.contextMenuProperty().isBound()) control.setContextMenu(null);
            }
        } else {
            ownedMenu.hide();
            if (control.getContextMenu() == null && control.getOnContextMenuRequested() == null
                    && !control.contextMenuProperty().isBound()) control.setContextMenu(ownedMenu);
        }
    }

    ObservableList<MenuItem> extensions() {
        if (extensions == null) {
            extensions = FXCollections.observableArrayList();
            if (editAction != null) {
                var key = ResourceBundle.getBundle("fore").getString("OTKRYT_V_DIALOGOVOM_OKNE");
                var item = new MenuItem(key, IconDescriptor.of(FontAwesome.fa_ellipsis_h).getLabel());
                item.setOnAction(editAction);
                extensions.addAll(new SeparatorMenuItem(), item);
            }
        }
        return extensions;
    }

    void populate(ContextMenu menu) {
        populateItems(menu);
        if (onPopulate != null) onPopulate.accept(menu);
    }

    private void populateItems(ContextMenu menu) {
        boolean selected = control.getSelection().getLength() > 0;
        menu.getItems().clear();
        if (control.isEditable()) {
            menu.getItems().addAll(
                    item("Undo", control::undo, !control.isUndoable()),
                    item("Redo", control::redo, !control.isRedoable()),
                    item("Cut", control::cut, !selected),
                    item("Copy", control::copy, !selected),
                    item("Paste", control::paste, !Clipboard.getSystemClipboard().hasString()),
                    item("DeleteSelection", control::deleteNextChar, !selected),
                    new SeparatorMenuItem(), item("SelectAll", control::selectAll, false));
        } else {
            menu.getItems().addAll(item("Copy", control::copy, !selected),
                    new SeparatorMenuItem(), item("SelectAll", control::selectAll, false));
        }
        if (extensions != null || editAction != null) {
            extensions().forEach(item -> menu.getItems().add(0, item));
        }
    }

    private MenuItem item(String key, Runnable action, boolean disabled) {
        var item = new MenuItem(labels.getString(key));
        item.setDisable(disabled);
        item.setOnAction(event -> action.run());
        return item;
    }
}
