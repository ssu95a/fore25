package ru.inversion.icons;
import javafx.scene.control.Label;
import ru.inversion.icons.enums.IconSize;

/**
 @author fomishkin on 12.07.2017. */
public interface IBaseIconDescriptor {
    IconSize getIconSize();
    String getCompatibleID();

    default Label getLabel() {
        return IconFactory.getLabel( this );
    }
}
