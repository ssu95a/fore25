package ru.inversion.icons.providers.fx;
import ru.inversion.icons.nogui.NoGuiIcon;
import ru.inversion.icons.nogui.URIFacade;
import static java.lang.invoke.MethodHandles.lookup;
import java.util.LinkedList;
import java.util.List;
import javafx.scene.Group;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.providers.MultiTTFProvider;

/**
 @author fomishkin on 29.06.2017. */
public class FXMultiTTFProvider extends MultiTTFProvider {
    private final static Logger logger = getLogger( lookup().lookupClass() );

    @Override
    public Object getImage( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        Label label = new Label();
        label.setGraphic( group( getLabels( icon ) ) );
        return label;
    }

    private static Label[] getLabels( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        List<Label> labels = new LinkedList<>();
        String[] split = icon.getID().toString().split( ":" );
        if ( split.length % 3 == 0 ) {
            int cnt = 0;
            String tmpUri = "";
            StringBuilder sb = new StringBuilder();
            for ( int i = 0; i < split.length; i++ ) {
                cnt++;
                sb.append( split[i] );
                if ( cnt == 1 ) {
                    sb.append( ":" );
                }
                if ( cnt == 2 ) {
                    tmpUri = sb.toString();
                    sb.setLength( 0 );
                }
                if ( cnt == 3 ) {
                    Label label = IconFactory.getLabel( tmpUri, icon.getSizeBehavior().getSize(), TTFStyler.getColor( sb
                            .toString() ) );
                    labels.add( label );
                    sb.setLength( 0 );
                    cnt = 0;
                }
            }
            return labels.toArray( new Label[labels.size()] );
        }
        throw new URIFacade.IconNotFoundException( icon.getID().toString() + ": URI parameters not dividable by 3" );
    }

    private static Group group( Label... labels ) {
        return new Group( labels );
    }
}
