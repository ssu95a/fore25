/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package ru.inversion.fx.form;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import ru.inversion.fx.form.controls.JInvTextArea;

/**
 * A dialog that shows a text input area to the user.
 *
 * @see Dialog, TextInputDialog
 */
public class NoteInputDialog extends Dialog<String> {

    /** */
    private final GridPane grid;
    private final Label label;
    private final JInvTextArea textArea;

    /**
     * Creates a new TextInputDialog without a default value entered into the
     * dialog {@link TextField}.
     */
    public NoteInputDialog() {
        this("", 3);
    }

    public NoteInputDialog( @NamedArg("defaultValue") String defaultValue ) {
        this(defaultValue, 3);
    }

    /**
     * Creates a new TextInputDialog with the default value entered into the
     * dialog {@link TextField}.
     */
    public NoteInputDialog( @NamedArg("defaultValue") String defaultValue, int prefRowCount) {
        final DialogPane dialogPane = getDialogPane();

        // -- textArea
        this.textArea = new JInvTextArea(defaultValue);
        this.textArea.setMaxWidth (Double.MAX_VALUE);
        this.textArea.setMaxHeight(Double.MAX_VALUE);
        this.textArea.setPrefRowCount(prefRowCount);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setFillWidth ( textArea, true );
        GridPane.setFillHeight( textArea, true );

        // -- label
        label = createContentLabel(dialogPane.getContentText());
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.textProperty().bind(dialogPane.contentTextProperty());

        this.grid = new GridPane();
        this.grid.setHgap(5);
        this.grid.setVgap(5);
        this.grid.setMaxWidth ( Double.MAX_VALUE);
        this.grid.setAlignment( Pos.CENTER_LEFT );

        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        setTitle(ControlResources.getString("Dialog.confirm.title"));
        dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
        dialogPane.getStyleClass().add("text-input-dialog");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        updateGrid();

        setResultConverter((dialogButton) -> {
            ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonData.OK_DONE ? textArea.getText() : null;
        });
    }

    /**
     * Returns the {@link TextField} used within this dialog.
     */
    public final TextArea getEditor() {
        return textArea;
    }

    /** */
    private void updateGrid() {
        grid.getChildren().clear();
        grid.add(label, 0, 0);
        grid.add(textArea, 0, 1);
        getDialogPane().setContent(grid);

        Platform.runLater(() -> textArea.requestFocus());
    }

    /** */
    private Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth (Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        return label;
    }

}
