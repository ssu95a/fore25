package ru.inversion.fx.form.controls.renderer;
/**
 @author fomishkin on 22.01.2020. */
public class JInvTableCellEnum<T, E extends Enum<E>> extends JInvTableCell<T, E>
{
    public JInvTableCellEnum()
    {
        super(null);
    }

    @Override
    protected void updateItem( E item, boolean empty )
    {
        super.updateItem(item, empty);

        applyRenderer(item, empty);
    }
}
