package ru.inversion.icons.nogui;
import ru.inversion.icons.behaviors.ColorBehavior;
import ru.inversion.icons.behaviors.NoColorChange;
import ru.inversion.icons.behaviors.NoSizeChange;
import ru.inversion.icons.behaviors.SizeBehavior;
import ru.inversion.icons.providers.IProvider;
import ru.inversion.icons.providers.fx.FXBinDepProvider;

/**
 @author fomishkin on 28.06.2017. */
public class BinDepIcon extends NoGuiIconImpl {
    private final String ID;

    public BinDepIcon( String ID ) {
        this.ID = ID;
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
        return new NoSizeChange();
    }

    @Override
    public IProvider getProvider() {
        return new FXBinDepProvider();
    }

    @Override
    public String toString() {
        return "BinDepIcon{" + ID + '}';
    }
}
