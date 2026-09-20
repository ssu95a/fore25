package ru.inversion.fx.form.controls.treetableex;

import com.sun.javafx.scene.control.skin.TreeTableViewSkin;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.StackPane;
import ru.inversion.fx.form.controls.treetableex.infobar.TSInfoBar;

public class JInvTreeTableViewSkin extends TreeTableViewSkin<Object> {

    private final TSInfoBar infoBar;

    public JInvTreeTableViewSkin( TreeTableView treeTable ) {

        super(treeTable);

        infoBar = new TSInfoBar();

        if( treeTable instanceof JInvTreeTableEx )
        {
            JInvTreeTableEx jte = (JInvTreeTableEx)treeTable;
            infoBar.init( jte );
            infoBar.visibleProperty().bind( jte.visibleStatusBarProperty() );
        }

        infoBar.setManaged( false );

        super.getChildren().addAll( infoBar );

    }

    // https://stackoverflow.com/questions/27059701/javafx-in-treeview-need-only-scroll-to-index-number-when-treeitem-is-out-of-vie/27786741#27786741
    public boolean isIndexVisible(int index)
    {
        if (flow.getFirstVisibleCell() != null &&
                flow.getLastVisibleCell() != null &&
                flow.getFirstVisibleCell().getIndex() <= index &&
                flow.getLastVisibleCell().getIndex() >= index)
            return true;
        return false;
    }
    /** */
    public TSInfoBar getInfoBar() {
        return infoBar;
    }

    /** */
    double getH(double h) {

        if( infoBar.isVisible() )
            return h - infoBar.getHeight();

        return h;
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {

        final TreeTableView<Object> treeTable = getSkinnable();

        final StackPane placeholder = (StackPane) treeTable.lookup(".placeholder");
//        VirtualFlow vf = (VirtualFlow) treeTable.lookup(".virtual-flow");

        if( treeTable.isVisible() )
        {
            super.layoutInArea  ( placeholder, x, y, w, getH(h), -1, HPos.CENTER, VPos.TOP);
            super.layoutInArea  ( infoBar, x, y, w, h, -1, HPos.CENTER, VPos.BOTTOM);
            super.layoutChildren( x, y, w, getH(h) );
        }
        else
        {
            super.layoutChildren(x, y, w, h);
        }
    }
}
