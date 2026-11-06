/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

//import javafx.scene.control.cell.CellUtils;

/**
 *
 * @author mik
 */
@Deprecated
public class JInvTextFieldTableCell<S, T> extends TableCell<S, T> {

    private EventHandler<MouseEvent> _tblMouseHandler = (event) -> tableMouseHandle(event);
    private EventHandler<KeyEvent> _tableFieldKeyHandler = (event) -> tableFieldKeyHandler(event);
    private EventHandler<KeyEvent> _textFieldKeyHandler = (event) -> textFieldKeyHandler(event);

    protected TextInputControl textField = null;
    protected Pos alignment = Pos.CENTER_LEFT;
    protected TablePosition editingCell = null;
    private ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<StringConverter<T>>(this, "converter");

    public JInvTextFieldTableCell() {
        this(null);
    }

    public JInvTextFieldTableCell(StringConverter<T> converter) {
        this.getStyleClass().add("text-field-table-cell");
        setConverter(converter);
    }

    public JInvTextFieldTableCell(StringConverter<T> converter, Pos alignment) {
        this(converter);
        this.alignment = alignment;
    }

    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }

    public void tableMouseHandle(MouseEvent event) {
        editingCell = getTableView().getEditingCell();
//        System.out.println(event);
    }

    public void tableFieldKeyHandler(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            editingCell = null;
        }

    }

    public void textFieldKeyHandler(KeyEvent event) {
        int index = 0;
        TableColumn col = getTableColumn();
        index = getTableRow().indexProperty().get();

        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.F8) {
            commitEdit(converter.get().fromString(textField.getText()));
        }

        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.DOWN) {
            nextEdit(index);
        } else if (event.getCode() == KeyCode.UP) {
            prevEdit(index);
        }
    }

    protected void nextEdit(int index) {
        if (!(index >= getTableView().getItems().size() - 1)) {
            getTableView().selectionModelProperty().get().select(index + 1, getTableColumn());
            getTableView().edit(index + 1, getTableColumn());
        } else {
            Platform.runLater(() -> getTableView().requestFocus());
            getTableView().selectionModelProperty().get().select(index, getTableColumn());
        }
    }

    protected void prevEdit(int index) {
        if (index > 0) {
            getTableView().selectionModelProperty().get().select(index - 1, getTableColumn());
            getTableView().edit(index - 1, getTableColumn());
        } else {
            Platform.runLater(() -> getTableView().requestFocus());
            getTableView().selectionModelProperty().get().select(index, getTableColumn());
        }
    }

    protected TextInputControl createTextField() {

        textField = new TextField();
        //textField = new JInvMoneyField();
        textField.setText(this.getText());
        textField.setPrefWidth(this.getWidth() - this.getGraphicTextGap() * 2);
        textField.setEditable(true);

        textField.addEventHandler(KeyEvent.KEY_RELEASED, _textFieldKeyHandler);

        textField.focusedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if (newValue == false) {
                    if (getTableView().isFocused() == false) {
                        commitEdit(converter.get().fromString(textField.getText()));
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

        
        if (converter != null && getItem() != null) {
            this.setText(converter.get().toString(getItem()));
        } else {
            this.setText(textField.getText());
        }
        //setText(converter.toString(getTableColumn().getCellObservableValue(editingCell.getRow()).getValue()));
        this.setGraphic(textField);

        getTableView().addEventFilter(MouseEvent.MOUSE_PRESSED, _tblMouseHandler);
        getTableView().addEventFilter(KeyEvent.KEY_PRESSED, _tableFieldKeyHandler);

        Platform.runLater(() -> textField.requestFocus());

    }

    @Override
    public void updateItem(T item, boolean empty) {

        super.updateItem(item, empty);
        if (empty) {
            this.setText(null);
            this.setGraphic(null);
        } else {
            if (this.isEditing()) {
                if (textField != null) {
                    textField.setText(converter.get().toString(item));
                }
                this.setText(null);
                this.setGraphic(textField);
            } else {
                this.setAlignment(alignment);
                this.setText(converter.get().toString(item));
                this.setGraphic(null);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEdit() {
        if (editingCell != null) {
            TableColumn.CellEditEvent editEvent = new TableColumn.CellEditEvent(
                getTableView(),
                editingCell,
                TableColumn.editCommitEvent(),
                converter.get().fromString(textField.getText())
            );
            editEvent.fireEvent(getTableColumn(), editEvent);
            setText(textField.getText());
        }
        super.cancelEdit();
        this.setGraphic(null);
        getTableView().removeEventFilter(MouseEvent.MOUSE_PRESSED, _tblMouseHandler);
        getTableView().removeEventFilter(KeyEvent.KEY_PRESSED, _tableFieldKeyHandler);

    }

    @Override
    public void commitEdit(T newValue) {
        super.commitEdit(newValue);
        this.setGraphic(null);
        getTableView().removeEventFilter(MouseEvent.MOUSE_PRESSED, _tblMouseHandler);
        getTableView().removeEventFilter(KeyEvent.KEY_PRESSED, _tableFieldKeyHandler);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
        final StringConverter<T> converter) {
        return list -> new JInvTextFieldTableCell<S, T>(converter);
    }

    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
        final StringConverter<T> converter, final Pos alignment) {
        return list -> new JInvTextFieldTableCell<S, T>(converter, alignment);
    }

}
