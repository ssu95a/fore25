package ru.inversion.icons.nogui;
import ru.inversion.icons.behaviors.BinSizeChange;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.NoColorChange;
import ru.inversion.icons.behaviors.SizeBehavior;
import java.nio.file.Path;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.providers.IProvider;
import ru.inversion.icons.providers.fx.FXBinOSProvider;

/**
 @author fomishkin on 28.06.2017. */
public class BinOSIcon extends NoGuiIconImpl {
    private final Path ID;
    private final IconSize size;

    public BinOSIcon( Path ID, IconSize size ) {
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
        return new BinSizeChange( size );
    }

    @Override
    public IProvider getProvider() {
        return new FXBinOSProvider();
    }

    @Override
    public String toString() {
        return "BinOSIcon{" + ID + '}';
    }
}
