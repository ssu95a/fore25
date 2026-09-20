package ru.inversion.icons.testgui;
import static java.lang.invoke.MethodHandles.lookup;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.stream.Stream;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.IconDescriptorBuilder;
import static ru.inversion.icons.IconFactory.getLabel;
import ru.inversion.icons.OverlayIconDescriptorBuilder;
import ru.inversion.icons.RowIconDescriptorBuilder;
import ru.inversion.icons.enums.Entypo;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.FontAwesomeRegular;
import ru.inversion.icons.enums.FontAwesomeSolid;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.enums.IonIcon;

public class TestIconController {
    private final static Logger logger = getLogger( lookup().lookupClass() );
    public Label label1;
    public Label label2;
    public Label label3;
    public Button button1;
    public Button button2;
    public Button button3;
    public Button button4;
    public Button button5;
    public Button button6;
    public Button button7;
    public Button button8;
    public Button button9;
    public Button button10;
    public Button button11;
    public Button button12;

    public void initialize() throws URISyntaxException {
        Stream.of("ICO","JPEG","BMP").forEach( this::listReaders );

        button1.setGraphic( getLabel(IconDescriptorBuilder.of(FontAwesomeRegular.far_file)) );
        button2.setGraphic( getLabel( new IconDescriptorBuilder().iconId( FontAwesome.fa_adjust )
                .color( Color.DARKGOLDENROD )
                .iconSize( IconSize.LARGE )
                .compatibleId( "print_f.ico" )
                .build() ) );
        button3.setGraphic( getLabel( new IconDescriptorBuilder().iconId( Entypo.icon_archive ).build() ) );
//        button4.setGraphic( getLabel( "bin:" + "fa_Printer.png" ) );
        button4.setGraphic( getLabel( IconDescriptorBuilder.of(FontAwesome.fa_print)) );
        button5.setGraphic( getLabel( "fmx:sms3", IconSize.MEDIUM ) );
        button6.setGraphic( getLabel( "fmx:sms3", IconSize.LARGE ) );
        button7.setGraphic( getLabel( "fmx:priNt_f", IconSize.SMALL ));
        button8.setGraphic( getLabel( "fmx:priNt_f", IconSize.MEDIUM ));
        button9.setGraphic( getLabel( "fmx:book_pen", IconSize.SMALL ) );
        button10.setGraphic( getLabel( new RowIconDescriptorBuilder().add( Entypo.icon_basket )
                .add( FontAwesome.fa_adjust )
                .color( Color.CHARTREUSE )
                .build() ) );
        button11.setGraphic( getLabel( new OverlayIconDescriptorBuilder().add( Entypo.icon_basket, Color.CHOCOLATE )
                .add( FontAwesomeSolid.fas_band_aid )
                .iconSize( IconSize.LARGE )
                .build() ) );
        button12.setGraphic( getLabel( "row:" +
                IonIcon.ion_wifi.getURI()/* + ":"+ FontAwesome.fa_angle_double_down.getURI()*/, IconSize.LARGE, Color.CADETBLUE ) );
    }

    private void listReaders(String fileFormat) {
        logger.info( "{} readers:", fileFormat );
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(fileFormat);
        while (readers.hasNext()) {
            logger.info("reader: " + readers.next());
        }
    }
}
