package ru.inversion.icons.behaviors;
import javafx.scene.control.Labeled;

/**
 @author fomishkin on 05.07.2017. */
public class NoColorChange implements ColorBehavior {
    @Override
    public <T extends Labeled> T changeColor( T t ) {
        return t;
    }
}
