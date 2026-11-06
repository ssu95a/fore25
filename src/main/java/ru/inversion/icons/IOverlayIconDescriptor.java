package ru.inversion.icons;
import ru.inversion.icons.enums.TTFChar;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import javafx.scene.paint.Color;
import ru.inversion.icons.descextensions.IMultiColorable;

/**
 @author fomishkin on 12.07.2017. */
public interface IOverlayIconDescriptor extends ICombinedIconDescriptor, IMultiColorable {
    @Override
    default URI toUri() {
        try {
            StringBuilder sb = new StringBuilder();
            if ( getIconIDList().size() != getIconColorList().size() ) {
                throw new AssertionError();
            }
            Iterator idIter = getIconIDList().iterator();
            Iterator colorIter = getIconColorList().iterator();
            while ( idIter.hasNext() && colorIter.hasNext() ) {
                Object o = idIter.next();
                Color c = (Color) colorIter.next();
                if ( o instanceof TTFChar ) {
                    sb.append( ":" )
                            .append( ( (TTFChar) o ).getURI() )
                            .append( ":" )
                            .append( c.toString().toLowerCase() );
                }
            }
            return new URI( "mul" + sb.toString() );
        } catch ( URISyntaxException e ) {
            return null;
        }
    }
}
