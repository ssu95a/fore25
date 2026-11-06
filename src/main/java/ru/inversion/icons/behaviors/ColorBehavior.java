package ru.inversion.icons.behaviors;
import javafx.scene.control.Labeled;

/**
 @author fomishkin on 05.07.2017. */
public interface ColorBehavior {
    <T extends Labeled> T changeColor( T t );
}
