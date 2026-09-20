package ru.inversion.fx.form.controls.treetableex.column;

import javafx.geometry.Pos;
import javafx.util.StringConverter;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.JInvTableColumnDate;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.fx.form.controls.treetableex.TableObservableValue;
import ru.inversion.fx.form.controls.treetableex.cell.JInvTreeTableCell_Date;
import ru.inversion.meta.IEntityProperty;

import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author antonovdi
 */
public class JInvTreeTableColumnEx_Date<P, T> extends JInvTreeTableColumnEx<P, T> {

    private JInvTableColumnDate.DateContentType dateFormat = JInvTableColumnDate.DateContentType.DATE_TIME;

    public JInvTreeTableColumnEx_Date()
    {
        super();
        setAlignment( Pos.CENTER );
    }

    public JInvTableColumnDate.DateContentType getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat( JInvTableColumnDate.DateContentType dateFormat )
    {
        if( entityProperty != null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Date format cannot be changed after column binding" );

        this.dateFormat = Objects.requireNonNull( dateFormat, "'dateFormat' is null" );
    }

    /** */
    @Override
    public void bind( IEntityProperty<P, T> ep, ICellValueChangeListener<P> cellValueChangeListener )
    {
        if( entityProperty != null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' already bind" );

        final IEntityProperty<P, T> property = Objects.requireNonNull( ep, "'entityProperty' is null" );

        final Class<?> propertyType = property.getType();

        if( !Temporal.class.isAssignableFrom(propertyType) && !Date.class.isAssignableFrom(propertyType) )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' is not date/time type" );

        final StringConverter<T> newConverter = dateFormat.getStringConverter(propertyType);

        final TableObservableValue bval = new TableObservableValue(property);

        entityProperty = property;
        converter      = newConverter;

        setCellFactory( param -> new JInvTreeTableCell_Date<>() );
        setCellValueFactory( param -> bval.pojoInstance( param.getValue() == null ? null : param.getValue().getValue() ) );
    }
}
