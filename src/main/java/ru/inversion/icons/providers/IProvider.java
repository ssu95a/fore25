package ru.inversion.icons.providers;
import ru.inversion.icons.nogui.NoGuiIcon;
import ru.inversion.icons.nogui.URIFacade;

/**
 @author fomishkin on 28.06.2017. */
public interface IProvider<T> {
    T getImage( NoGuiIcon icon ) throws URIFacade.IconNotFoundException;
}
