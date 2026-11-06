package ru.inversion.icons.nogui;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.SizeBehavior;
import ru.inversion.icons.providers.IProvider;

/**
 @author fomishkin on 26.06.2017. */
public interface NoGuiIcon<T> {
    T getID();
    ColorBehavior getColorBehavior();
    SizeBehavior getSizeBehavior();
    IProvider getProvider();
}



