package ru.inversion.icons;
import java.net.URI;
import java.util.List;

/**
 @author fomishkin on 12.07.2017. */
public interface ICombinedIconDescriptor extends IBaseIconDescriptor {
    List getIconIDList();
    URI toUri();
}
