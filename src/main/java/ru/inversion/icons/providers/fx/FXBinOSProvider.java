package ru.inversion.icons.providers.fx;
import ru.inversion.icons.nogui.NoGuiIcon;
import ru.inversion.icons.nogui.URIFacade;
import java.awt.image.BufferedImage;
import static java.lang.invoke.MethodHandles.lookup;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.providers.BinOSProvider;

/**
 @author fomishkin on 28.06.2017. */
public class FXBinOSProvider extends BinOSProvider {
    private final static Logger logger = getLogger( lookup().lookupClass() );

    @Override
    public Object getImage( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        Label label = new Label();
        ImageView imageView = getImageView( icon );
        label.setGraphic( imageView );
        return label;
    }

    private static ImageView getImageView( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        try ( ImageInputStream in = ImageIO.createImageInputStream(
                Files.newInputStream( Paths.get( icon.getID().toString() ) ) ) ) {
            ImageReader reader = ImageIO.getImageReaders( in ).next();
            reader.setInput( in );
            int count = reader.getNumImages( true );
            Map<Integer, Image> icoImages = new HashMap<>( count );
            for ( int i = 0; i < count; i++ ) {
                BufferedImage bufferedImage = reader.read(i, null);
                Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
                icoImages.put( (int) fxImage.getWidth(), fxImage);
            }
            reader.dispose();
            return findClosestTo( icoImages, icon.getSizeBehavior().getSize().getSize() );

        } catch ( Exception e ) {
            throw new URIFacade.IconNotFoundException( "getImageView: Icon " + icon + " not found! " + e.toString() );
        }
    }

    private static ImageView findClosestTo( final Map<Integer, Image> icoImages, final int preferredSize) throws URIFacade.IconNotFoundException {
        int closest = -1;
        int distance = Integer.MAX_VALUE;
        for ( final int width : icoImages.keySet() ){
            final int curDistance = Math.abs( preferredSize - width );
            if(curDistance < distance ||
                    sameDistanceButSmaller( closest, distance, width, curDistance ) ){
                closest = width;
                distance = curDistance;
            }
        }
        if (closest == -1) {
            throw new URIFacade.IconNotFoundException( "Unable to find closest icon in " + icoImages );
        }
        return new ImageView( icoImages.get( closest ) );
    }

    private static boolean sameDistanceButSmaller( final int closest, final int distance, final int width, final int curdistance ) {
        return curdistance == distance && width < closest;
    }
//    private static ImageView getImageView( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
//        try ( InputStream in = Files.newInputStream( Paths.get( icon.getID().toString() ) ) ) {
//            Image image = SwingFXUtils.toFXImage( ImageIO.read( in ), null ); //new Image(in);
//            return new ImageView( image );
//        } catch ( Exception e ) {
//            throw new URIFacade.IconNotFoundException( "getImageView: Icon " + icon + " not found! " + e.toString() );
//        }
//    }
}
