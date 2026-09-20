package ru.inversion.fx.form.controls.treetableex.cell;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableCell;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;

/**
 * Ячейка для пометки записей.
 */
public final class JInvTreeTableCell_Mark<P> extends JInvTreeTableCell<P, Boolean> {

    private static final PseudoClass markPseudoClass = PseudoClass.getPseudoClass("mark");

    private final CheckBox checkBox;
    private final TSFXAdapter<P> adapter;
    private final boolean leafOnly;

    public JInvTreeTableCell_Mark( TSFXAdapter<P> adapter, boolean leafOnly )
    {
        super();

        this.leafOnly = leafOnly;
        this.adapter  = adapter;

        this.checkBox = new CheckBox();
        this.checkBox.setFocusTraversable(false);

        checkBox.setOnAction((ActionEvent event) -> switchMark());

        checkBox.addEventFilter( MouseEvent.MOUSE_RELEASED,
                event -> {

                    if( event.getButton() != MouseButton.PRIMARY )
                        return;

                    if( event.isControlDown() )
                    {
                        checkBox.setSelected(!checkBox.isSelected());
                        switchMark();

                        event.consume();
                        return;
                    }

                    if( event.isShiftDown() )
                    {
                        final TreeTableView<P> table = getTreeTableView();

                        if( table != null && getTreeTableRow() != null )
                        {
                            checkBox.setSelected(true);

                            table.getSelectionModel().select( getTreeTableRow().getIndex() );

                            switchMark();
                        }

                        event.consume();
                    }
                }
        );

        this.getStyleClass().add("check-box-table-cell");

        setGraphic(null);
        setText(null);
    }

    /** */
    private void switchMark() {
        applyMark(checkBox.isSelected());
    }

    /** */
    private boolean isLeaf() {

        if( getTreeTableRow() == null )
            return false;

        final TreeItem<P> item = getTreeTableRow().getTreeItem();
        return item != null && item.isLeaf();
    }

    /** */
    @Override
    public void updateItem(Boolean item, boolean empty)
    {
        superUpdateItem(item, empty);

        if( empty || item == null || (leafOnly && !isLeaf()) )
        {
            setText(null);
            setGraphic(null);
            checkBox.setSelected(false);

            switchMarkColorOnRow(false);
            applyRenderer(item, true);
            return;
        }

        setText(null);
        setGraphic(checkBox);

        final boolean selected = Boolean.TRUE.equals(item);

        checkBox.setSelected(selected);
        switchMarkColorOnRow(selected);

        applyRenderer(item, false);
    }


    /** */
    private void applyMark(boolean mark)
    {
        final TreeTableRow<P> row =
                getTreeTableRow();

        if( row == null || row.isEmpty() )
            return;

        final TreeItem<P> treeItem =
                row.getTreeItem();

        if( treeItem == null )
            return;

        final P value = treeItem.getValue();

        final boolean oldValue = value instanceof IMarkable && ((IMarkable) value).isMark();

        try
        {
            if( mark != oldValue )
            {
                if( mark )
                    adapter.markItem(treeItem);
                else
                    adapter.unMarkItem(treeItem);
            }

            checkBox.setSelected(mark);
            switchMarkColorOnRow(mark);
        }
        catch( RuntimeException ex )
        {
            checkBox.setSelected(oldValue);
            switchMarkColorOnRow(oldValue);

            throw ex;
        }
    }
    /** */
    private void switchMarkColorOnRow(boolean val) {

        if (getTreeTableRow() != null) {
            getTreeTableRow().pseudoClassStateChanged(markPseudoClass, val);
        }
    }
}