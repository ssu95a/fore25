package ru.inversion.icons.utils;
import static java.lang.invoke.MethodHandles.lookup;
import java.util.Arrays;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.enums.TTFChar;

/**
 @author fomishkin on 27.06.2017. */
public class EnumUtils {
    private final static Logger logger = getLogger( lookup().lookupClass() );

    /**
     Находит значение в енуме по строке
     Возвращает null(!) и пишет предупреждение, если не найден
     **/
    public static <T extends Enum<T>> T valueOfIgnoreCase( Class<T> enumeration, String name ) {
        for ( T enumValue : enumeration.getEnumConstants() ) {
            if ( enumValue.name().equalsIgnoreCase( name ) ) {
                return enumValue;
            }
        }
        logger.warn( "Enum {}: no such value {}", enumeration.getSimpleName(), name );
        return null;
    }

    public static <T extends Enum<T> & TTFChar> T getEnumByCode( Class<T> enumeration, String s ) {
        return Arrays.stream( enumeration.getEnumConstants() )
                .filter( e -> e.getCode().equals( s ) )
                .findFirst()
                .orElse( null );
    }

    ;
}
