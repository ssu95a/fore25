package ru.inversion.fx.form.controls.dsbar;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.dataset.aggr.IAggregator;
import ru.inversion.dataset.aggr.IAggregatorBuilder;
import ru.inversion.dataset.aggr.IAggregatorListener;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.controls.dsbar.DSInfoBar.PartEnum.*;

/**
 *
 * @author ssu
 */
public class Part_Aggregator extends AbstractAggregatorPart {

    /** */
    final private DSInfoBar.PartEnum aggregatorType;

    /** */
    final private HBox pane = new HBox(5.0d);

    /** */
    final private DSFXAdapter adapter;

    /** */
    private IAggregator aggregator;

    /** */
    private IAggregatorListener aggregatorListener;

    /** */
    final private Map< String, Property<? extends Number> > valuesMap = new HashMap<>();

    /** */
    public Part_Aggregator( DSInfoBar.PartEnum aggregatorType, DSFXAdapter adapter, ResourceBundle customBundle ) {

        super(customBundle);
        if( aggregatorType == null || U.notIn(aggregatorType, Mark, Filter, Total ) )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "Bad value for 'aggregatorType' - " + S.nvl( aggregatorType ) );

        this.aggregatorType = aggregatorType;
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.getProperties( ).put( PART, getType() );

        this.adapter = adapter;

        pane.getChildren().addAll( new Separator( Orientation.VERTICAL ), getAggregatorLabel() );
    }

    /** */
    @Override
    public DSInfoBar.PartEnum getType( ) {
        return aggregatorType;
    }

    /** */
    private IAggregatorBuilder createAggregatorBuilder( ) {

        if( getType() == Mark )
        {
            XXIDataSet xds = (XXIDataSet)adapter.getDataSet();
            final IAggregatorBuilder bldr = xds.createMarkedAggregatorBuilder(true);
            if( !S.isNullOrEmpty( getWherePredicate() ) )
                 bldr.addWherePredicate( getWherePredicate() );
            return bldr;
        }

        ISQLDataSet ds = (ISQLDataSet)adapter.getDataSet();
        final IAggregatorBuilder bldr = ds.createAggregatorBuilder(true, getType() == Total);
        if( !S.isNullOrEmpty( getWherePredicate() ) )
            bldr.addWherePredicate( getWherePredicate() );
        return bldr;
    }

    /** */
    public Object getValue( String column ) {

        Property< ? extends Number > value = valuesMap.get(column);

        if( value == null )
            return null;

        return value.getValue();
    }

    /** */
    @Override
    public Pane createControlPane( ) {

        IAggregatorBuilder builder = createAggregatorBuilder( );
        List<JInvNumberField> localFieldList = getControls();
        for( JInvNumberField n : localFieldList ) {

            String       column = (String      )n.getProperties( ).get( "ds.bar.aggr.col"  );
            AggrFuncEnum func   = (AggrFuncEnum)n.getProperties( ).get( "ds.bar.aggr.func" );
            String       alias  = (String      )n.getProperties( ).get( "ds.bar.aggr.a$col");

            builder.add( column, func, alias );
        }

        try {

            aggregator = builder.build( );

            aggregatorListener = source -> Platform.runLater(() -> {

                final Map<String,Object> values = source.getValues();

                localFieldList.forEach( (JInvNumberField f)->f.getProperties( ).get( "ds.bar.aggr.a$col") );
                    for ( JInvNumberField field : localFieldList ) {
                        String s = (String) field.getProperties().get( "ds.bar.aggr.a$col" );
                        if ( S.isNotNullOrEmpty( s ) ) {
                            field.setValue( values.get( s ) );
                        }
                    }
            });
        } catch( DataSetException ex ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on create 'Part_Aggregator'", ex );
        }

        return pane;
    }

    /** */
    public JInvNumberField addAggregator( String column, AggrFuncEnum func, String alias, String tooltip ) {

        JInvNumberField textField = new JInvNumberField();
        textField.setConverter( getDefaultAggrConverter(func) );

        int columnCount     = func == AggrFuncEnum.COUNT ? 8 : -1;

        textField.setEditable( false );
        textField.getProperties( ).put( "ds.bar.aggr.func",  func );
        textField.getProperties( ).put( "ds.bar.aggr.col",   column );
        if( S.isNullOrEmpty(alias) )
            textField.getProperties( ).put( "ds.bar.aggr.a$col", "A$" + column );
        else
            textField.getProperties( ).put( "ds.bar.aggr.a$col", alias );

        if( columnCount != -1 )
            textField.setPrefColumnCount( columnCount );

        if( tooltip != null )
            textField.setTooltip( new Tooltip(tooltip) );

        pane.getChildren().add( textField );

        addField( func, column, textField );

        valuesMap.put( column, (Property<? extends Number>)textField.valueProperty());
        return textField;
    }

    /** */
    @Override
    public void onVisible( ) {

        aggregator.addListener(aggregatorListener);

        try {

            // hack !!
            if( this.adapter != null && adapter.getDataSet().getProperty("ds.execute_count") != null )
                aggregator.execute();
        }
        catch( DataSetException ex ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on refresh aggregator", ex );
        }
    }

    /** */
    @Override
    public void onHide( ) {
        aggregator.removeListener(aggregatorListener);
    }

    /** */
    @Override
    public Pane getControlPane() {
        return pane;
    }

    @Override
    public void recalculate() {
        if( aggregator != null ) {
            try {
                aggregator.execute();
            }
            catch( DataSetException ex ) {
                throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on recalculate aggregator", ex );
            }
        }
    }
}
