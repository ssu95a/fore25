package ru.inversion.icons.nogui;
import ru.inversion.icons.enums.*;
import ru.inversion.icons.utils.EnumUtils;
import static java.lang.invoke.MethodHandles.lookup;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 @author fomishkin on 28.06.2017. */
public class URIFacade {
    public static final String MUL = "mul";
    public static final String FMX = "fmx";
    public static final String BIN = "bin";
    public static final String ROW = "row";
    public static final String JINV_ICON_PATH = "jinv.icon.path";
    public static final String ENV_VAR = "XXI_HOME";
    public static final String ENV_SUBFOLDER = "ICONS";
    public static final String DEFAULT_FMX_EXTENSION = ".ico";
    public static final String URL = "url";
    private final static Logger logger = getLogger( lookup().lookupClass() );
    private static final Path path;
    private static final String dependencyPath = "img/";

    static {
        String property = System.getProperty( JINV_ICON_PATH );
        String env = System.getenv( ENV_VAR );
        if ( property != null ) {
            path = Paths.get( property );
        } else if ( env != null ) {
            path = Paths.get( env, ENV_SUBFOLDER );
        } else {
            logger.warn( "No icon path found!" );
            path = Paths.get( "" );
        }
    }

    protected static NoGuiIcon getImage( URI uri, IconSize size, Color color ) throws IconNotFoundException {
        final String prefix = uri.getScheme();
        if ( prefix == null ) {
            throw new IconNotFoundException( uri + ": No icon prefix!" );
        }
        final String name = uri.getSchemeSpecificPart();
        if ( name != null && !name.isEmpty() ) {
//            try {
            switch ( prefix.toLowerCase() ) {
                    /*inject_start*/
                    case "fa":
                        return newTTF( name, FontAwesome.class, size, color );
                    case "mdi":
                        return newTTF( name, MaterialDesign.class, size, color );
                    case "ion":
                        return newTTF( name, IonIcon.class, size, color );
                    case "ent":
                        return newTTF( name, Entypo.class, size, color );
                    case "far":
                        return newTTF(name, FontAwesomeRegular.class, size, color);
                    case "fas":
                        return newTTF(name, FontAwesomeSolid.class, size, color);

                    /*inject_end*/
                case BIN:
                    return new BinDepIcon( dependencyPath + name );
                case FMX:
                    return new BinOSIcon( Paths.get( path.toString(), getFmxName( name ) ), size );
//                    case URL:
//                        return new BinURLIcon( new java.net.URL( name ) );
                case MUL:
                    return new MultiTTFIcon( uri.getSchemeSpecificPart(), size );
                case ROW:
                    return new RowTTFIcon( uri.getSchemeSpecificPart(), size, color );
                default:
            }
//            } catch ( MalformedURLException e ){
//                throw new IconNotFoundException( "Bad URL icon: " + e.getMessage() );
//            }
        }
        throw new IconNotFoundException( "Unknown prefix: " + uri );
    }

    private static String getFmxName( String name ) {
        return name.contains( "." ) ? name : name + DEFAULT_FMX_EXTENSION;
    }

    /**
     Иконка из URI
     */
    protected static <E extends Enum<E> & TTFChar> NoGuiIcon newTTF( String schemeSpecificPart, Class<E> type, IconSize size, Color color ) throws IconNotFoundException {
        E e = EnumUtils.valueOfIgnoreCase( type, schemeSpecificPart );
        if ( e == null ) {
            throw new IconNotFoundException( "Enum is null" );
        }
        return new TTFIcon<>( e, size, color );
    }

    /**
     Иконка из URI
     */
    protected static <E extends Enum<E> & TTFChar> NoGuiIcon newTTF( String schemeSpecificPart, Class<E> type ) throws IconNotFoundException {
        return newTTF( schemeSpecificPart, type, IconSize.MEDIUM, Color.BLACK );
    }

    /**
     Иконка по енуму
     */
    protected static <E extends Enum<E> & TTFChar> NoGuiIcon newTTF( E type, IconSize size, Color color ) {
        return new TTFIcon<>( type, size, color );
    }

    public static class IconNotFoundException extends Exception {
        public IconNotFoundException() {
            super();
        }

        public IconNotFoundException( String s ) {
            super( s );
        }
    }
}
