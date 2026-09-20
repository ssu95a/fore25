package ru.inversion.fx.form.controls.dsbar;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.util.Pair;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.dataset.aggr.IAggregator;
import ru.inversion.dataset.aggr.IAggregatorBuilder;
import ru.inversion.dataset.aggr.IAggregatorListener;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.utils.S;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 *
 * @author ssu
 */
public class Part_AggregatorMark extends AbstractAggregatorPart {

    final static private String NONAME = "--";

    private class AL implements IAggregatorListener {

        final List<JInvNumberField> internalFieldList;

        /** */
        public AL( final List<JInvNumberField> totalFieldList, String tableName ) {
            if( S.isNullOrEmpty(tableName) )
                internalFieldList = totalFieldList.stream().filter((JInvNumberField n) -> n.getProperties( ).get( "ds.bar.aggr.table" ) == null ).collect(Collectors.toList());
            else
                internalFieldList = totalFieldList.stream().filter((JInvNumberField n) -> tableName.equals( n.getProperties( ).get( "ds.bar.aggr.table" ) )).collect( Collectors.toList() );
        }

        /** */
        @Override
        public void change( IAggregator source ) {

            final Map<String,Object> values = source.getValues( );

            Platform.runLater(() -> {
                for ( JInvNumberField jInvNumberField : internalFieldList ) {
                    String s = (String) jInvNumberField.getProperties().get( "ds.bar.aggr.a$col" );
                    if ( S.isNotNullOrEmpty( s ) ) {
                        jInvNumberField.setValue( values.get( s ) );
                    }
                }//end for
            });
        }
    };

    /** */
    final private HBox pane = new HBox(5.0d);

    /** */
    final private DSFXAdapter adapter;

    /** */
    final private Map<String,Pair<IAggregator,IAggregatorListener>> aggrMap = new HashMap<>();

    /** */
    final private Map< String, Property<? extends Number> > valuesMap = new HashMap<>();

    /** */
    public Part_AggregatorMark( DSFXAdapter adapter, ResourceBundle customBundle ) {

        super(customBundle);

        this.adapter = adapter;

        pane.setAlignment ( Pos.CENTER_LEFT );
        pane.getProperties( ).put( PART, getType() );

        pane.getChildren().addAll( new Separator( Orientation.VERTICAL ), getAggregatorLabel() );
    }

    /** */
    @Override
    public DSInfoBar.PartEnum getType( ) {
        return DSInfoBar.PartEnum.Mark;
    }

    /** */
    private IAggregatorBuilder getAggregatorBuilder( String aggrTable, Map<String,IAggregatorBuilder> bldrMap ) {

        final String at = S.isNullOrEmpty(aggrTable) ? NONAME : aggrTable;

        XXIDataSet xds = (XXIDataSet)adapter.getDataSet( );

        IAggregatorBuilder bldr = bldrMap.get(at);
        if( bldr == null ) {
            bldr = xds.createMarkedAggregatorBuilder( true, aggrTable );
            String wherePredicate = getWherePredicate();
            if( !S.isNullOrEmpty(wherePredicate) )
                 bldr.addWherePredicate(wherePredicate);
            bldrMap.put( at, bldr );
        }
        return bldr;
    }

    /** */
    @Override
    public Pane createControlPane( ) {

        Map<String,IAggregatorBuilder> bldrMap = new HashMap<>();
        List<JInvNumberField> localFieldList = getControls();
        for( JInvNumberField n : localFieldList ) {

            String       column = (String      )n.getProperties( ).get( "ds.bar.aggr.col" );
            AggrFuncEnum func   = (AggrFuncEnum)n.getProperties( ).get( "ds.bar.aggr.func");
            String       alias  = (String      )n.getProperties( ).get( "ds.bar.aggr.a$col");

            String       table  = (String      )n.getProperties( ).get( "ds.bar.aggr.table" );

            IAggregatorBuilder builder = getAggregatorBuilder(table, bldrMap);

            builder.add( column, func, alias );

        }

        bldrMap.forEach( ( t, u ) -> {
            try {
                aggrMap.put( t, new Pair( u.build(), new AL( localFieldList, NONAME.equals(t) ? null : t ) ) );
            } catch (DataSetException ex) {
                throw new RuntimeException(ex);
            }
        } );
        return pane;
    }

    /** */
    public Object getValue( String column ) {

        Property< ? extends Number > value = valuesMap.get(column);

        if( value == null )
            return null;

        return value.getValue();
    }

    /** */
    public JInvNumberField addAggregator( String column, AggrFuncEnum func, String alias, String tooltip, String aggrTable ) {

        JInvNumberField textField = new JInvNumberField();
        //По умолчанию ставим конвертер либо от Long, либо от Money
        textField.setConverter( getDefaultAggrConverter( func ) );

        int columnCount     = func == AggrFuncEnum.COUNT ? 8 : -1;

        textField.setEditable( false );
        textField.getProperties( ).put( "ds.bar.aggr.func",  func   );
        textField.getProperties( ).put( "ds.bar.aggr.col",   column );
        if( S.isNullOrEmpty( alias ) )
            textField.getProperties( ).put( "ds.bar.aggr.a$col", "A$" + column );
        else
            textField.getProperties( ).put( "ds.bar.aggr.a$col", alias );

        if( S.isNotNullOrEmpty(aggrTable) )
            textField.getProperties( ).put( "ds.bar.aggr.table", aggrTable );

        if( columnCount != -1 )
            textField.setPrefColumnCount( columnCount );

        if( tooltip != null )
            textField.setTooltip( new Tooltip(tooltip) );

        valuesMap.put( column, (Property<? extends Number>)textField.valueProperty());

        pane.getChildren( ).add( textField );
        addField( func, column, textField );
        return textField;
    }

    /** */
    @Override
    public void onVisible( ) {

        aggrMap.forEach((String t, Pair<IAggregator, IAggregatorListener> u) -> {
            u.getKey().addListener( u.getValue() );
        });


        aggrMap.forEach((String t, Pair<IAggregator, IAggregatorListener> u) -> {
            try {
                u.getKey().execute();
            } catch (DataSetException ex) {
                throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on refresh aggregator", ex );
            }
        });
    }

    /** */
    @Override
    public void onHide( ) {
        aggrMap.forEach((String t, Pair<IAggregator, IAggregatorListener> u) -> {
            u.getKey().removeListener( u.getValue() );
        });
    }

    /** */
    @Override
    public Pane getControlPane() {
        return pane;
    }

    @Override
    public void recalculate() {
        aggrMap.forEach((String t, Pair<IAggregator, IAggregatorListener> u) -> {
            try {
                u.getKey().execute();
            } catch (DataSetException ex) {
                throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on recalculate aggregator", ex );
            }
        });
    }
}
