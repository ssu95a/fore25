package ru.inversion.fx.form.controls.treetableex.cell;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableCell;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;

public class JInvTreeTableCell_Boolean<P> extends JInvTreeTableCell<P,Boolean> {

    private final Label check = IconDescriptor.of(FontAwesome.fa_check).getLabel();

    public JInvTreeTableCell_Boolean( ) {
        super  (Pos.CENTER);
        setText(S.EMPTY_STRING);
    }

    /** */
    @Override
    protected void updateItem(Boolean item, boolean empty)
    {
        superUpdateItem(item, empty);

        if( empty || item == null )
        {
            clearCell();
            return;
        }

        setText(null);
        setGraphic(item ? check : null);

        applyRenderer(item, false);
    }
}
