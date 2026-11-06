package ru.inversion.icons.nogui;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.SizeBehavior;
import ru.inversion.icons.behaviors.TTFColorChange;
import ru.inversion.icons.behaviors.TTFSizeChange;
import javafx.scene.paint.Color;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.enums.TTFChar;
import ru.inversion.icons.providers.IProvider;
import ru.inversion.icons.providers.fx.FXTTFProvider;

/**
 @author fomishkin on 28.06.2017. */
public class TTFIcon<E extends Enum<E> & TTFChar> extends NoGuiIconImpl<E> {
    private final E ID;
    private final IconSize size;
    private final Color color;

    public TTFIcon( E ID ) {
        this( ID, IconSize.MEDIUM, Color.BLACK );
    }

    public TTFIcon( E ID, IconSize size, Color color ) {
        this.ID = ID;
        this.size = size;
        this.color = color;
    }

    @Override
    public E getID() {
        return ID;
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
        return "TTFIcon{" + ID + '}';
    }
}
