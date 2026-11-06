/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.skin;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import com.sun.javafx.scene.control.skin.TableViewSkinBase;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.shape.Rectangle;
import ru.inversion.fx.form.controls.filter.JInvFilterToolBar;

import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_MARK;


/**
 *
 * @author perov
 */
public class JInvTableHeaderRow extends TableHeaderRow {

    private Node    corner;
    private ScrollBar vBar;

    public JInvTableHeaderRow( final TableViewSkinBase<?, ?, ?, ?, ?, ?> skin ) {
        super(skin);
    }

    @Override
    protected void layoutChildren() {

        super.layoutChildren();

        final Node corner = corner();
        final ScrollBar vBar = vBar();

        final double cornerWidth = (vBar != null) ? Math.ceil(vBar.prefWidth(-1)) : 0.0;
        final double prefHeight  = getHeight() - snappedTopInset() - snappedBottomInset();

        if( corner != null && cornerWidth > 0 )
            corner.resizeRelocate( getTableWidthWithoutFilterPane() - cornerWidth, snappedTopInset(), cornerWidth, prefHeight );

    }

    @Override
    protected void updateTableWidth() {
        final Node clip = getClip();
        if (clip instanceof Rectangle) {
            ((Rectangle) clip).setWidth( getTableWidthWithoutFilterPane() );
        }
    }
//
//    @SuppressWarnings("unused")
//    private static boolean isMarkColumn(TableColumn<?, ?> col) {
//        if( col == null)
//            return false;
//        final Object v = col.getProperties().get(COLUMN_MARK);
//        return Boolean.TRUE.equals(v);
//    }

    private Node corner() {
        if( corner == null )
            corner = lookup(".show-hide-columns-button");
        return corner;
    }

    private ScrollBar vBar() {
        if (vBar == null) {
            Node n = (getTableSkin().getSkinnable()).lookup(".scroll-bar:vertical");
            if (n instanceof ScrollBar)
                vBar = (ScrollBar) n;
        }
        return vBar;
    }

    private double getTableWidthWithoutFilterPane() {

        final TableView<?> c = (TableView<?>) getTableSkin().getSkinnable();
        final Insets insets  = (c.getInsets() == null) ? Insets.EMPTY : c.getInsets();
        final double padding = Math.ceil(insets.getLeft()) + Math.ceil(insets.getRight());

        double toolBarWidth = 0.0;

        for (Object n : getTableSkin().getChildren()) {
            if (n instanceof JInvFilterToolBar) {
                final JInvFilterToolBar<?> tb = (JInvFilterToolBar<?>) n;
                if (tb.isVisible()) {
                    double pw = tb.prefWidth(-1);
                    if (pw <= 0) pw = tb.getMinWidth();
                    toolBarWidth = Math.ceil(pw);
                }
                break;
            }
        }

        return c.getWidth() - padding - toolBarWidth;
    }
}