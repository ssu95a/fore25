/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.form.controls.Controls;

import static ru.inversion.fx.form.controls.Controls.isColumnSupportFilter;

/**
 *
 * @author antonovdi
 */
public class JInvTableCellFilter<S, T> extends TableCell<S, T> {

    private EventHandler<MouseEvent> _tblMouseHandler = (event) -> tableMouseHandle(event);
    private EventHandler<KeyEvent> _textFieldKeyHandler = (event) -> textFieldKeyHandler(event);

    protected TextInputControl textField = null;
    protected Pos alignment = Pos.CENTER_LEFT;
    protected TablePosition editingCell = null;

    public JInvTableCellFilter() {
        this(null);
    }

    public JInvTableCellFilter(StringConverter<T> converter) {
        this.getStyleClass().add("text-field-table-cell");
    }

    public void tableMouseHandle(MouseEvent event) {
        editingCell = getTableView().getEditingCell();
    }

    Logger logger = LoggerFactory.getLogger(JInvTableCellFilter.class);

    public void textFieldKeyHandler(KeyEvent event) {
        int index = 0;
        TableColumn col = getTableColumn();
        index = getTableView().getColumns().indexOf(col);

        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.LEFT) {
            logger.info("commitEdit from KeyListener");
            commitEdit(null);
        }

        if (event.getCode() == KeyCode.F8) {
            commitEdit(null);

            KeyEvent newEvent = new KeyEvent(KeyEvent.KEY_PRESSED, event.getCharacter(),
                "applyFilter", KeyCode.F8, false, false, false, false);
            getTableView().fireEvent(newEvent);
        }

        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.RIGHT) {
            nextEdit(index);
        } else if (event.getCode() == KeyCode.LEFT) {
            prevEdit(index);
        }
    }

    protected void nextEdit(int index) {

        ObservableList<TableColumn<S, ?>> listColums = getTableView().getColumns();
        if (index + 1 < getTableView().getColumns().size()) {

            TableColumn col = listColums.get(index + 1);
            if (isColumnSupportFilter(col)) {
                startEditColumn(col);
            } else {
                nextEdit(index + 1);
            }
        } else {
            nextEdit(-1);
        }
    }

    private void startEditColumn(TableColumn col) {
        getTableView().scrollToColumn(col);
        Platform.runLater(() -> getTableView().requestFocus());
        getTableView().selectionModelProperty().get().select(0, col);
        getTableView().edit(0, col);
    }

    protected void prevEdit(int index) {

        ObservableList<TableColumn<S, ?>> listColums = getTableView().getColumns();
        if (index > 0) {
            TableColumn col = listColums.get(index - 1);
            if (isColumnSupportFilter(col)) {
                startEditColumn(col);
            } else {
                prevEdit(index - 1);
            }
        } else {
            prevEdit(listColums.size());
        }
    }

    protected TextInputControl createTextField() {

        double height = getTableView().getFixedCellSize();
        textField = new TextField();
        textField.setText(this.getText());
        textField.setPrefWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        textField.setEditable(true);
        textField.setMaxHeight(height);
        textField.setPrefHeight(height);
        textField.getProperties().put(Controls.CONTROL_PARENT, getTableView());
        textField.addEventFilter(KeyEvent.KEY_RELEASED, _textFieldKeyHandler);

        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    if (getTableView().isFocused() == false) {
                        logger.trace("commitEdit from FocusChange");
                        commitEdit(null);
                    }
                }
            }
        });
        return textField;
    }

    @Override
    public void startEdit() {

        if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
            return;
        }
        logger.trace("startEdit");
        super.startEdit();

//        if (isEditing()) {
        if (textField == null) {
            textField = createTextField();
            if (textField instanceof TextField) {
                ((TextField) textField).setAlignment(alignment);
                textField.setText("");
            }
        }
//        }

        this.setText(null);
        this.setGraphic(textField);

        getTableView().addEventFilter(MouseEvent.MOUSE_PRESSED, _tblMouseHandler);
        Platform.runLater(() -> textField.requestFocus());

    }

    @Override
    public void updateItem(T item, boolean empty) {

        super.updateItem(item, empty);

        logger.trace("update item");
        if (empty) {
            return;
        }

        String idColumn = Controls.getFieldNameFromTableColumn(getTableColumn());
        String text = (String) getTableColumn().getProperties().getOrDefault(idColumn, null);

        if (text == null) {
            this.setText(null);
            this.setGraphic(null);
        } else if (this.isEditing()) {
            if (textField != null) {
                textField.setText(text);
            }
            this.setText(null);
            this.setGraphic(textField);
        } else {
            this.setAlignment(alignment);
            this.setText(text);
            this.setGraphic(null);
        }

//        if (!isEditing() && getTableView().getColumns().indexOf(getTableColumn()) == 0 && getTableRow().getIndex()==0) {
//            getTableView().selectionModelProperty().get().select(0, getTableView().getColumns().get(0));
//            getTableView().edit(0, getTableView().getColumns().get(0));
//        }
    }

    @Override
    public void cancelEdit() {

        logger.info("cancelEdit");

        if (editingCell != null) {
            setText(textField.getText());
        }

        super.cancelEdit();
        this.setGraphic(null);
        getTableView().removeEventFilter(MouseEvent.MOUSE_PRESSED, _tblMouseHandler);

    }

    @Override
    public void commitEdit(T newValue) {

        String idColumn = Controls.getFieldNameFromTableColumn(getTableColumn());
        getTableColumn().getProperties().put(idColumn, textField.getText());
        this.setGraphic(null);
        super.commitEdit(newValue);

        getTableView().removeEventFilter(MouseEvent.MOUSE_PRESSED, _tblMouseHandler);
    }
}
