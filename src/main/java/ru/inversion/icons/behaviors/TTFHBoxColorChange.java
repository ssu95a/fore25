package ru.inversion.icons.behaviors;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 @author fomishkin on 10.07.2017. */
public class TTFHBoxColorChange implements ColorBehavior {
    public final Color color;

    public TTFHBoxColorChange( Color color ) {
        this.color = color;
    }

    @Override
    public <T extends Labeled> T changeColor( T t ) {
        for ( Node label : ( (HBox) t.getGraphic() ).getChildren() ) {
            ( (Labeled) label ).setTextFill( color );
        }
        return t;
    }
}
