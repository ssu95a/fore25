package ru.inversion.fx.app.frame.menu;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import ru.inversion.fx.form.ActionFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.*;

/** */
public class IndicatorsListDialog extends Dialog<ConnectionIndicators> {

    final static private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    final private TableView<Pair<String,Color> > table = new TableView<>();

    final private ConnectionIndicators indicators;

    public IndicatorsListDialog( ConnectionIndicators ind ) {

        this.indicators = Objects.requireNonNull( ind, "'ind' is null" );

        setTitle( bundle.getString("SETTINGS_IND") );

        setResizable(true);

        setHeight(300);

        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setMaxWidth ( Double.MAX_VALUE);
        grid.setAlignment( Pos.CENTER_LEFT );

        ToolBar toolBar = new ToolBar();
        toolBar.setStyle("-fx-background-color: -fx-background; -fx-padding: 0 0 0 0; ");
        toolBar.getItems().addAll (
            ActionFactory.createButton( CREATE, this::onCreate ),
            ActionFactory.createButton( UPDATE, this::onUpdate ),
            ActionFactory.createButton( DELETE, this::onDelete )
        );

        final TableColumn< Pair<String,Color>,String > matchColumn = new TableColumn<>(bundle.getString("IND_DIALOG_TABLE_MATCH"));
        matchColumn.setCellValueFactory(param -> new SimpleStringProperty( param.getValue().getKey() ));

        final TableColumn< Pair<String,Color>, Color > colorColumn = new TableColumn<>(bundle.getString("IND_DIALOG_TABLE_COLOR"));
        colorColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getValue()));
        colorColumn.setCellFactory(param -> new TableCell<Pair< String, Color >, Color>() {
        @Override
        protected void updateItem(Color item, boolean empty) {
            super.updateItem( item, empty );
                if( !empty && item != null ) {
                    setBackground(  new Background(new BackgroundFill(item, null, new Insets(2,2,2,2))) );
                }
            }
            }
        );
        table.getColumns().addAll( matchColumn, colorColumn );



        grid.add(toolBar, 0, 0);
        grid.add(table, 0, 1);
        GridPane.setFillWidth ( table, true );
        GridPane.setFillHeight( table, true );
        GridPane.setHgrow(table, Priority.ALWAYS);

        table.setItems( indicators.toObservableList() );

        getDialogPane().setContent(grid);

        getDialogPane().getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );

        setResultConverter( (b)->b == ButtonType.OK ? onOK() : null );
    }

    /** */
    private ConnectionIndicators onOK() {
        return ConnectionIndicators.fromList( table.getItems() );
    }

    /** */
    private void onCreate( ActionEvent e ) {
        Pair<String,Color> p = new Pair<>( "", Color.WHITE );
        ( new IndicatorDialog( getDialogPane().getScene().getWindow(), p )).showAndWait().map( (p1)->table.getItems().add(p1) );

    }

    /** */
    private void onUpdate( ActionEvent e ) {

        int index = table.getSelectionModel().getSelectedIndex();

        if( index != -1 ) {
            Pair<String,Color> p = table.getItems().get( index );
            ( new IndicatorDialog( getDialogPane().getScene().getWindow(), p )).showAndWait().map( (p1)->table.getItems().set(index,p1) );

            table.getSelectionModel().select( index );
        }
    }

    /** */
    private void onDelete( ActionEvent e ) {
        int nIndex = table.getSelectionModel().getSelectedIndex();
        if( nIndex >= 0 ) {
            table.getItems().remove(nIndex);
            table.refresh();
        }
    }

}

