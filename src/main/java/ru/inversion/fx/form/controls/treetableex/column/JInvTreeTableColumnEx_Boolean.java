package ru.inversion.fx.form.controls.treetableex.column;

import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.meta.IEntityProperty;

import java.util.Objects;

/** */
public class JInvTreeTableColumnEx_Boolean<P> extends JInvTreeTableColumnEx<P, Boolean> {

    private static final int FIXED_WIDTH = 25;

    public JInvTreeTableColumnEx_Boolean() {
        super();
        init();
    }

    public JInvTreeTableColumnEx_Boolean( final String name ) {
        super( name );
        init();
    }

    private void init( ) {
        setMinWidth ( FIXED_WIDTH );
        setPrefWidth( FIXED_WIDTH );
        setMaxWidth ( 40 );
    }

    @Override
    public void bind( IEntityProperty<P, Boolean> ep, ICellValueChangeListener<P> cellValueChangeListener )
    {
        if( entityProperty != null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' already bind" );

        final IEntityProperty<P, Boolean> property = Objects.requireNonNull( ep, "'entityProperty' is null" );

        final Class<?> propertyType = property.getType();

        if( propertyType != boolean.class && propertyType != Boolean.class )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' is not boolean type" );

        super.bind( property, cellValueChangeListener );
    }
}
