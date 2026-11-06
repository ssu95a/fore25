package ru.inversion.fx.form.controls.treetableex.column;

import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.fx.form.controls.treetableex.TableObservableValue;
import ru.inversion.fx.form.controls.treetableex.cell.JInvTreeTableCell_Icon;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.Tags;

import java.util.Objects;

/** */
public class JInvTreeTableColumnEx_Icon<P> extends JInvTreeTableColumnEx<P, String> {

    private static final int FIXED_WIDTH = 25;

    public JInvTreeTableColumnEx_Icon() {
        super();
        init();
    }

    public JInvTreeTableColumnEx_Icon( final String name ) {
        super( name );
        init();
    }

    private void init( ) {
        setMinWidth ( FIXED_WIDTH );
        setPrefWidth( FIXED_WIDTH );
        setMaxWidth ( 40 );
    }

    public void bind( IEntityProperty<P,String> ep, ICellValueChangeListener<P> cellValueChangeListener )
    {
        if( this.entityProperty != null )
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' already bind" );

        this.entityProperty = Objects.requireNonNull( ep, "'entityProperty' is null" );

        if( ep.getType() != String.class )
            throw new IllegalStateException(ru.inversion.fx.app.Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' is not String type" );

        Callback< TreeTableColumn<P,String>, TreeTableCell<P,String> > cellFactory = param -> new JInvTreeTableCell_Icon<>();
        final TableObservableValue bval = new TableObservableValue(ep);
        setCellFactory     ( cellFactory );
        setCellValueFactory( param -> bval.pojoInstance( param.getValue() == null ? null : param.getValue().getValue()) );
    }

}
