package ru.inversion.fx.app.frame.menu;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Pair;

import java.util.ResourceBundle;

/** */
public class IndicatorDialog extends Dialog<Pair<String,Color>> {

    final static private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    final private TextField   match = new TextField();
    final private ColorPicker color = new ColorPicker();
    private Pair<String,Color> pair;

    public IndicatorDialog( Window parent, Pair<String,Color> p ) {

        this.pair = p;

        initOwner( parent );

        setTitle ( bundle.getString("SETTINGS_IND") );

        setResizable( true );

        GridPane grid = new GridPane( );
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setMaxWidth ( Double.MAX_VALUE );
        grid.setAlignment( Pos.CENTER_LEFT  );

        match.setText ( pair.getKey()   );
        color.setValue( pair.getValue() );

        grid.add( new Label("Алиас"), 0, 0);
        grid.add( match, 0, 1 );

        grid.add( new Label("Цвет рамки"), 1, 0);
        grid.add( color, 1, 1);

        getDialogPane().setContent( grid );

        getDialogPane().getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );

        setResultConverter( (b)->b == ButtonType.OK ? new Pair<>( match.getText(), color.getValue() ) : null );
    }
}
