package ru.inversion.icons;
import ru.inversion.icons.enums.TTFChar;
import java.net.URI;
import java.net.URISyntaxException;
import ru.inversion.icons.descextensions.IColorable;

/**
 @author fomishkin on 12.07.2017. */
public interface IRowIconDescriptor extends ICombinedIconDescriptor, IColorable {
    @Override
    default URI toUri() {
        try {
            StringBuilder sb = new StringBuilder();
            for ( Object o : getIconIDList() ) {
                if ( o instanceof TTFChar ) {
                    sb.append( ":" ).append( ( (TTFChar) o ).getURI() );
                }
            }
            return new URI( "row" + sb.toString() );
        } catch ( URISyntaxException e ) {
            return null;
        }
    }
    default int getPadding() {
        return 0;
    }
}
