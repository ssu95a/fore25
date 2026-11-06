package ru.inversion.fx.form.controls.renderer;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;

/**
 *
 * @author antonovdi
 */
public class JInvTableCellBoolean extends JInvTableCell {

    private final Label check = IconFactory.getLabel( FontAwesome.fa_check, IconSize.SMALL );

    public JInvTableCellBoolean(String mask) {
        super(mask);
        setAlignment(Pos.CENTER);
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        if( item == null || empty || !((Boolean)item) ) {
            setGraphic( null );
        } else {
            setGraphic( check );
        }

        applyRenderer(item, empty);
    }
}
