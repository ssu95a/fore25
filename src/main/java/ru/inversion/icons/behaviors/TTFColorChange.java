package ru.inversion.icons.behaviors;
import javafx.scene.control.Labeled;
import javafx.scene.paint.Color;

/**
 @author fomishkin on 05.07.2017. */
public class TTFColorChange implements ColorBehavior {
    public final Color color;

    public TTFColorChange( Color color ) {
        this.color = color;
    }

    @Override
    public <T extends Labeled> T changeColor( T t ) {
        //Не окрашиваем в чёрный цвет, т.к. по умолчанию он и так чёрный, а в тёмном оформлении не будет читаем.
        if ( !color.equals( Color.BLACK ) ) {
            t.setTextFill( color );
        }
        return t;
    }
}
