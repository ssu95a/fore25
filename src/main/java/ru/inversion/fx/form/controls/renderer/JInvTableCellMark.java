/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.controls.JInvTable;

/**
 * Ячейка для пометки записей
 *
 * @author antonovdi,
 *         sulimoff
 */
public class JInvTableCellMark<T> extends TableCell<T, Boolean> { //extends CheckBoxTableCell<T, Boolean> {

    final private static PseudoClass markPseudoClass = PseudoClass.getPseudoClass("mark");

    final private CheckBox       checkBox;
    final private DSFXAdapter<T> adapter;

    public JInvTableCellMark( DSFXAdapter<T> adapter ) {

        super();

        this.adapter = adapter;
        this.checkBox = new CheckBox();
        this.checkBox.setFocusTraversable(false);

        checkBox.setOnAction( (ActionEvent event)->switchMark() );

        checkBox.addEventFilter(MouseEvent.MOUSE_RELEASED, (MouseEvent event) -> {

            if( event.isControlDown() && event.getButton().equals(MouseButton.PRIMARY) )
            {
                checkBox.selectedProperty().set(!checkBox.isSelected());
                switchMark();
            }

            if( event.isShiftDown() && event.getButton().equals(MouseButton.PRIMARY) )
            {
                TableView table = getTableView();
                checkBox.selectedProperty().set(true);
                table.getSelectionModel().select(getTableRow().getIndex());
                if (table instanceof JInvTable) {
                    ((JInvTable) table).markRangeOfRows();
                }
            }
        });

        this.getStyleClass().add("check-box-table-cell");

        setGraphic( null );
        setText   ( null );
    }

    /** */
    private void switchMark() {
        commitEdit( checkBox.isSelected() );
    }

    @Override
    public void updateItem( Boolean item, boolean empty ) {

        if( empty )
        {
            setGraphic(null);
            switchMarkColorOnRow(false);
        }
        else
        {
            setGraphic(checkBox);

            if( item )
            {
                checkBox.setSelected(true);
                switchMarkColorOnRow(true);

            }
            else
            {
                checkBox.setSelected(false);
                switchMarkColorOnRow(false);
            }
        }
    }

    @Override
    public void commitEdit( Boolean newValue ) {
        
        super.commitEdit(newValue);

        T   row   = (T) getTableRow().getItem();
        int index = getTableRow().getIndex();

        if( row != null )
        {
            if( newValue )
            {
                adapter.markRow(index);
                switchMarkColorOnRow(true);
            }
            else
            {
                adapter.unMarkRow(index);
                switchMarkColorOnRow(false);
            }
        }
    }

    /** */
    private void switchMarkColorOnRow(boolean val) {
        getTableRow().pseudoClassStateChanged( markPseudoClass, val );
    }

}
