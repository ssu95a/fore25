package ru.inversion.fx.form.controls.treetableex.column;

import javafx.geometry.Pos;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.fx.form.controls.JInvMoneyField;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableCell;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.fx.form.controls.treetableex.TableObservableValue;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.Tags;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Столбец TreeTable для отображения денежных данных
 * @author Sulimoff
 */
public class JInvTreeTableColumnEx_Money<P> extends JInvTreeTableColumnEx<P,BigDecimal> {
    public JInvTreeTableColumnEx_Money() {
        super();
        this.converter = JInvMoneyField.stringConverter;
    }

    /** */
    @Override
    public void bind( IEntityProperty<P, BigDecimal> ep, ICellValueChangeListener<P> cellValueChangeListener )
    {
        if( entityProperty != null )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' already bind" );

        final IEntityProperty<P, BigDecimal> property = Objects.requireNonNull( ep, "'entityProperty' is null" );

        if( property.getType() != BigDecimal.class )
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' is not BigDecimal type" );

        final TableObservableValue observableValue = new TableObservableValue(property);

        entityProperty = property;

        setCellFactory     ( column -> new JInvTreeTableCell<>( Pos.CENTER_RIGHT ) );
        setCellValueFactory(param -> observableValue.pojoInstance( param.getValue() == null ? null : param.getValue().getValue() ) );
    }}
