package ru.inversion.fx.form.controls.dsbar;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import ru.inversion.utils.U;

import java.util.ResourceBundle;

/**
 *
 * @author ssu
 */
public class Part_RowNum extends AbstractPartBase {

    final private HBox pane = new HBox(1.0d);

    public Part_RowNum( TableView table, ResourceBundle bundle ) {
        super(bundle);
        pane.setAlignment(Pos.CENTER_LEFT);

        final TextField textField = new TextField( );
        textField.setAlignment ( Pos.CENTER_RIGHT );
        textField.setTooltip   ( new Tooltip(g_bundle.getString("ROW_NUM")) );
        textField.setEditable  ( false );
        textField.setBackground( Background.EMPTY );
        //textField.setPrefColumnCount();
        textField.setStyle     ( PART_STYLE );
        textField.textProperty().addListener( (observable, oldValue, newValue) -> {
            int length = newValue.length( );
            textField.setPrefColumnCount( length );
        } );

        table.getSelectionModel().selectedIndexProperty().addListener( new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                textField.setText( Integer.toString( U.nvl( newValue, 0 ).intValue() + 1 ) );
            }
        } );
        //pane.getChildren().addAll( new Label("#"), textField );
        pane.getChildren().add( textField );
        pane.getProperties().put( PART, getType() );
    }

    /** */
    @Override
    public DSInfoBar.PartEnum getType() {
        return DSInfoBar.PartEnum.RowNum;
    }

    @Override
    public Pane createControlPane( ) {
        return pane;
    }

    @Override
    public Pane getControlPane() {
        return pane;
    }

}
