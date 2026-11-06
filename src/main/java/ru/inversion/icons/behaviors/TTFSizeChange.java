package ru.inversion.icons.behaviors;
import javafx.scene.control.Label;
import ru.inversion.icons.enums.IconSize;
import static ru.inversion.icons.providers.TTFProvider.BUTTON_TTF;
import ru.inversion.icons.providers.fx.TTFStyler;

/**
 @author fomishkin on 05.07.2017. */
public class TTFSizeChange implements SizeBehavior {
    public final IconSize size;

    public TTFSizeChange( IconSize size ) {
        this.size = size;
    }

    @Override
    public <T extends Label> T changeSize( T t ) {
        return TTFStyler.setStyle( t, BUTTON_TTF + size.toString().toLowerCase() );
    }

    @Override
    public IconSize getSize() {
        return size;
    }
}
