package ru.inversion.icons.behaviors;
import javafx.scene.control.Label;
import ru.inversion.icons.enums.IconSize;

/**
 @author fomishkin on 05.07.2017. */
public interface SizeBehavior {
    <T extends Label> T changeSize( T t );
    IconSize getSize();
}
