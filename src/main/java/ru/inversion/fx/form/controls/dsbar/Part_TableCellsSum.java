package ru.inversion.fx.form.controls.dsbar;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.ResourceBundle;

/**
 *
 * @author ssu
 */
public class Part_TableCellsSum extends AbstractPartBase {

    final private HBox pane = new HBox(1.0d);

    public Part_TableCellsSum( TableView table, ResourceBundle bundle) {

        super(bundle);
        if( table == null )
            throw new IllegalArgumentException("Table is null");
        
        pane.setAlignment( Pos.CENTER_RIGHT );
        pane.getProperties( ).put( PART, getType() );
        
        TextField textField = new TextField( );
        textField.setEditable  ( false );
        textField.setAlignment ( Pos.CENTER_RIGHT );
        textField.setTooltip   ( new Tooltip(g_bundle.getString("SUM_SELECTED_CELLS")) );
        textField.setBackground( Background.EMPTY );
        textField.setStyle     ( PART_STYLE );
        textField.textProperty ().addListener( (observable, oldValue, newValue) -> {
            int length = newValue.length( );
            textField.setPrefColumnCount( length );
        } );

        Label label = new Label("\u03A3 ");
        table.addEventFilter(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            
            if( event.isControlDown() && event.getButton().equals(MouseButton.PRIMARY)) {
                if (table.getSelectionModel().isCellSelectionEnabled()) {
                    //Сумируем выделенные ячейки
                    Iterator<TablePosition> iterator = table.getSelectionModel().getSelectedCells().iterator();
                    BigDecimal sum = new BigDecimal(0);
                    while( iterator.hasNext() ) {
                        TablePosition next = iterator.next();
                        if( next.getTableColumn().getCellData(0) instanceof Number) {
                            if (next.getTableColumn().getCellObservableValue(table.getItems().get(next.getRow())).getValue() != null) {
                                sum = sum.add(new BigDecimal(next.getTableColumn().getCellObservableValue(table.getItems().get(next.getRow())).getValue().toString()));
                            }
                        }
                    }
                   textField.setText(sum.toString());
                }
            }
        });
        
        final Pane rightSpacer = new Pane();
        HBox.setHgrow( rightSpacer, Priority.ALWAYS );

        pane.getChildren().add( rightSpacer );
        pane.getChildren().addAll( new Separator( Orientation.VERTICAL ), label, textField );
        HBox.setHgrow( pane, Priority.ALWAYS );
    }
    
    /** */
    @Override
    public DSInfoBar.PartEnum getType( ) {
        return DSInfoBar.PartEnum.CellSum;
    }
    
    /** */
    @Override
    public Pane createControlPane() {
        return pane;
    }

    @Override
    public Pane getControlPane() {
        return pane;
    }
    
}
