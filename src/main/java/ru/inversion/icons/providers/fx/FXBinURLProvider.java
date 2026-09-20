package ru.inversion.icons.providers.fx;
import ru.inversion.icons.nogui.NoGuiIcon;
import ru.inversion.icons.nogui.URIFacade;
import java.io.InputStream;
import static java.lang.invoke.MethodHandles.lookup;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.providers.BinOSProvider;

/**
 @author fomishkin on 28.06.2017. */
public class FXBinURLProvider extends BinOSProvider {
    private final static Logger logger = getLogger( lookup().lookupClass() );

    @Override
    public Object getImage( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        Label label = new Label();
        ImageView imageView = getImageView( icon );
        label.setGraphic( imageView );
        return label;
    }

    private static ImageView getImageView( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        try {
            final URI iconUri = ( (URL) icon.getID() ).toURI();
            Map<String, String> env = new HashMap<>();
            env.put( "create", "true" );
            env.put("encoding", "UTF-8");
            FileSystem zipfs = FileSystems.newFileSystem(
                    URI.create( "jar:" + iconUri.toString() )
                    , env
                    /*, ClassLoader.getSystemClassLoader()*/ );
            try ( InputStream in = Files.newInputStream( Paths.get( iconUri ) ) ) {
                Image image = SwingFXUtils.toFXImage( ImageIO.read( in ), null ); //new Image(in);
                return new ImageView( image );
            }
        } catch ( Exception e ) {
            throw new URIFacade.IconNotFoundException( "getImageView: Icon " + icon + " not found! " + e.toString() );
        }
    }
}
