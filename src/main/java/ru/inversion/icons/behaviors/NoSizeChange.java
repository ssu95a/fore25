package ru.inversion.icons.behaviors;
import javafx.scene.control.Label;
import ru.inversion.icons.enums.IconSize;

/**
 @author fomishkin on 05.07.2017. */
public class NoSizeChange implements SizeBehavior {
    @Override
    public <T extends Label> T changeSize( T t ) {
        return t;
    }

    @Override
    public IconSize getSize() {
        return IconSize.MEDIUM;
    }
}
