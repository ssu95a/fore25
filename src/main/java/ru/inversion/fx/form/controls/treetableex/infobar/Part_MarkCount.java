package ru.inversion.fx.form.controls.treetableex.infobar;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import ru.inversion.fx.form.controls.dsbar.AbstractPartBase;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;
import ru.inversion.tds.ITreeDataSetMarkListener;
import ru.inversion.tds.TreeDataSetMarkEvent;
import ru.inversion.tds.XXITreeDataSet;

import java.util.ResourceBundle;

/** */
public class Part_MarkCount extends AbstractPartBase {

    /** */
    final private HBox pane = new HBox(5.0d);

    /** */
    final private TSFXAdapter adapter;

    /** */
    public Part_MarkCount( TSFXAdapter adapter, ResourceBundle customBundle ) {
        super(customBundle);
        this.adapter = adapter;

        pane.setAlignment(Pos.CENTER_LEFT);
        pane.getProperties( ).put( PART, this );
        pane.getChildren().addAll( new Separator( Orientation.VERTICAL ), getAggregatorLabel() );

        final TextField textField = new TextField( );
        textField.setAlignment ( Pos.CENTER_RIGHT );
        //textField.setTooltip   ( new Tooltip(g_bundle.getString("ROW_NUM")) );
        textField.setEditable  ( false );
        textField.setBackground( Background.EMPTY );
        textField.setStyle     ( PART_STYLE );
        textField.textProperty().addListener( (observable, oldValue, newValue) -> {
            int length = newValue.length( );
            textField.setPrefColumnCount( length );
        } );
        textField.setPrefColumnCount( 3 );

        final XXITreeDataSet xds = (XXITreeDataSet)adapter.getTreeDataSet();
        xds.addMarkListener(new ITreeDataSetMarkListener() {
            @Override
            public void markAction( TreeDataSetMarkEvent event ) {
                if( event.isAfter() )
                    textField.setText( Integer.toString(event.getMarkedCount())  );
            }
        });

        pane.getChildren().add( textField );
    }

    /** */
    @Override
    public DSInfoBar.PartEnum getType() {
        return DSInfoBar.PartEnum.Mark;
    }

    /** */
    @Override
    public Pane createControlPane() {
        return pane;
    }

    /** */
    @Override
    public Pane getControlPane() {
        return pane;
    }
}
