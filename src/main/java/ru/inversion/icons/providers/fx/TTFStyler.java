package ru.inversion.icons.providers.fx;
import static java.lang.invoke.MethodHandles.lookup;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 @author fomishkin on 05.07.2017. */
public class TTFStyler {
    private final static Logger logger = getLogger( lookup().lookupClass() );

    /**
     Применяет стиль к наследникам Node, учитывая псевдоклассы
     @param node Node, или его производные
     @param css Строка из CSS, например button_ttf:fa:pressed
     */
    public static <T extends Node> T setStyle( T node, String css ) {
        int i = css.indexOf( ":" );
        if ( i != -1 ) {
            node.getStyleClass().add( css.substring( 0, i ) );
            String[] pseudoSplit = css.split( ":" );
            for ( int k = 1; k < pseudoSplit.length; k++ ) {
                node.pseudoClassStateChanged( PseudoClass.getPseudoClass( pseudoSplit[k] ), true );
            }
        } else {
            node.getStyleClass().add( css );
        }
        return node;
    }

    public static Color getColor( String color ) {
        if ( !color.equals( "" ) ) {
            try {
                return Color.valueOf( color );
            } catch ( Exception e ) {
                logger.warn( "No such color: {} ({})", color, e.getMessage() );
            }
        }
        return Color.BLACK;
    }
}
