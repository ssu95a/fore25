package ru.inversion.icons.nogui;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.SizeBehavior;
import ru.inversion.icons.behaviors.TTFHBoxColorChange;
import ru.inversion.icons.behaviors.TTFSizeChange;
import javafx.scene.paint.Color;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.providers.IProvider;
import ru.inversion.icons.providers.fx.FXRowTTFProvider;

/**
 @author fomishkin on 10.07.2017. */
public class RowTTFIcon extends NoGuiIconImpl {
    private final String ID;
    private final IconSize size;
    private final Color color;

    public RowTTFIcon( String ID, IconSize size, Color color ) {
        this.ID = ID;
        this.size = size;
        this.color = color;
    }

    @Override
    public Object getID() {
        return ID;
    }

    @Override
    public ColorBehavior getColorBehavior() {
        return new TTFHBoxColorChange( color );
    }

    @Override
    public SizeBehavior getSizeBehavior() {
        return new TTFSizeChange( size );
    }

    @Override
    public IProvider getProvider() {
        return new FXRowTTFProvider();
    }
}
