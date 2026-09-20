package ru.inversion.icons.nogui;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.NoColorChange;
import ru.inversion.icons.behaviors.SizeBehavior;
import ru.inversion.icons.behaviors.TTFSizeChange;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.providers.IProvider;
import ru.inversion.icons.providers.fx.FXMultiTTFProvider;

/**
 @author fomishkin on 29.06.2017. */
public class MultiTTFIcon extends NoGuiIconImpl {
    private final String ID;
    private final IconSize size;

    //    public MultiTTFIcon(String ID) {
//        this(ID, IconSize.MEDIUM);
//    }
    public MultiTTFIcon( String ID, IconSize size ) {
        this.ID = ID;
        this.size = size;
    }

    @Override
    public Object getID() {
        return ID;
    }

    @Override
    public ColorBehavior getColorBehavior() {
        return new NoColorChange();
    }

    @Override
    public SizeBehavior getSizeBehavior() {
        return new TTFSizeChange( size );
    }

    @Override
    public IProvider getProvider() {
        return new FXMultiTTFProvider();
    }
}
