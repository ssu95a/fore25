package ru.inversion.fx.form.controls.treetableex.infobar;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import ru.inversion.fx.form.controls.dsbar.AbstractPartBase;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;
import ru.inversion.tds.SQLTreeDataSet;

import java.util.ResourceBundle;

/** */
public class Part_TotalCount extends AbstractPartBase {

    /** */
    final private HBox pane = new HBox(5.0d);
    /** */
    public Part_TotalCount( TSFXAdapter adapter, ResourceBundle customBundle ) {
        super(customBundle);
        pane.setAlignment ( Pos.CENTER_LEFT );
        pane.getProperties( ).put( PART, this );
        pane.getChildren  ( ).add( getAggregatorLabel() );

        final TextField textField = new TextField( );
        textField.setAlignment ( Pos.CENTER_RIGHT );
        textField.setEditable  ( false );
        textField.setBackground( Background.EMPTY );
        textField.setStyle     ( PART_STYLE );
        textField.textProperty().addListener( (observable, oldValue, newValue) -> {
            int length = newValue.length( );
            textField.setPrefColumnCount( length );
        } );
        textField.setPrefColumnCount( 3 );

        final SQLTreeDataSet sds = (SQLTreeDataSet)adapter.getTreeDataSet();
        sds.addRowsListener(event -> {
            if( event.isAfter() )
                textField.setText( Integer.toString( event.getTreeDataSet().getTotalItemCount() ) );
        });
        //pane.getChildren().addAll( new Label("#"), textField );
        textField.setText( Integer.toString( sds.getTotalItemCount() ) );
        pane.getChildren().add( textField );
        //pane.getProperties().put( PART, this );
    }

    /** */
    @Override
    public DSInfoBar.PartEnum getType() {
        return DSInfoBar.PartEnum.Total;
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
