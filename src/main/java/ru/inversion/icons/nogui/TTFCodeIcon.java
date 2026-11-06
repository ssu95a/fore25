package ru.inversion.icons.nogui;

import javafx.scene.paint.Color;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.SizeBehavior;
import ru.inversion.icons.behaviors.TTFColorChange;
import ru.inversion.icons.behaviors.TTFSizeChange;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.enums.TTFChar;
import ru.inversion.icons.providers.IProvider;
import ru.inversion.icons.providers.fx.FXTTFProvider;

/**
 @author Suimoff on 05.04.2024. */
public class TTFCodeIcon< E extends TTFChar> extends NoGuiIconImpl<E> {
    private final E code;
    private final IconSize size;
    private final Color color;

    public TTFCodeIcon( E code ) {
        this(code, IconSize.MEDIUM, Color.BLACK );
    }

    public TTFCodeIcon( E code, IconSize size, Color color ) {
        this.code = code;
        this.size = size;
        this.color = color;
    }

    @Override
    public E getID() {
        return code;
    }

    @Override
    public ColorBehavior getColorBehavior() {
        return new TTFColorChange( color );
    }

    @Override
    public SizeBehavior getSizeBehavior() {
        return new TTFSizeChange( size );
    }

    @Override
    public IProvider getProvider() {
        return new FXTTFProvider();
    }

    @Override
    public String toString() {
        return "TTFIcon{" + code + '}';
    }
}
