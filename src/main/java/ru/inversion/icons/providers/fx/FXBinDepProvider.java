package ru.inversion.icons.providers.fx;
import ru.inversion.icons.nogui.NoGuiIcon;
import ru.inversion.icons.nogui.URIFacade;
import java.io.InputStream;
import static java.lang.invoke.MethodHandles.lookup;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.providers.BinDepProvider;

/**
 @author fomishkin on 28.06.2017. */
public class FXBinDepProvider extends BinDepProvider {
    private final static Logger logger = getLogger( lookup().lookupClass() );
    private static final ClassLoader loader = lookup().lookupClass().getClassLoader();

    @Override
    public Object getImage( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        Label label = new Label();
        ImageView imageView = getImageView( icon );
        label.setGraphic( imageView );
        return label;
    }

    private static ImageView getImageView( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        try ( InputStream in = loader.getResourceAsStream( icon.getID().toString() ) ) {
            Image image = SwingFXUtils.toFXImage( ImageIO.read( in ), null ); //new Image(in);
            return new ImageView( image );
        } catch ( Exception e ) {
            throw new URIFacade.IconNotFoundException( "getImage: Icon " + icon + " not found!" + e.toString() );
        }
    }
}
