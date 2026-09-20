package ru.inversion.fx.form.controls;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.ClipboardContent;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Общее между JInvTextField и JInvTextArea и их производными
 */
public interface ITextFieldBase extends IJInvControl, IValidatableControl, ILovValueControl,
        IFilterControl, IContextMenuAppendable {

    ResourceBundle fore = ResourceBundle.getBundle("fore");

    /**
     * Экшн вызова диалога редактора по Ctrl+E - JInvFEDialog по умолчанию.
     * При желании изменить поведение или отключить действие по Ctrl+E совсем делайте override.
     * С `return Optional.empty()` пункт контекстного меню рисоваться не будет
     */
    default Optional<EventHandler<ActionEvent>> getEditDialogAction() {
        return Optional.empty();
    }

    /**
     * То, что попадёт в буфер обмена при нажатии Ctrl+C
     */
    default ClipboardContent getClipboardContent() {
        return new ClipboardContent();
    }
}
