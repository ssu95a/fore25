package ru.inversion.fx.app.es;

import javafx.concurrent.WorkerStateEvent;
import javafx.stage.Modality;
import ru.inversion.email.EmailService;
import ru.inversion.email.IEmailBuilder;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.dialog.JInvProgressDialog;
import ru.inversion.fx.log.LogManager;
import ru.inversion.utils.S;

import javax.imageio.ImageIO;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

/**
 *
 * @author Ssu
 */
public class ErrorMessageSender implements JInvErrorService.IErrorMailSender {

    /** */
    private IEmailBuilder prepare( Throwable ex, JInvErrorService.IErrorSendProperties properties, BiConsumer<Integer,String> listener ) {

        Objects.requireNonNull( ex, "Throwable ex is null" );

        listener.accept( 0, "Prepare e-mail data " );

        final IEmailBuilder emailBuilder = EmailService.newBuilder();

        if( properties != null ) {

            emailBuilder.recipient( properties.getRecipient());
            emailBuilder.from     ( properties.getSender()   );
        }

        final String appId   = BaseApp.APP().getAppID();

        final String subject = String.format( "%s%s, %s - %s", Tags.PRODUCT_LABEL, appId, "errorInfo", properties.getSubject() );

        // message.setSubject(subject);
        emailBuilder.subject( subject );

        listener.accept( 5, "Prepare error content" );

        String content =
            String.format (
                    "AppID: %s<br>%s<p><strong>%s<strong><p>%s",
                    appId,
                    BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getStringProperty("ru.inversion.app.title"),
                    ex.getLocalizedMessage(),
                    S.nz( properties.getInfo()
                )
            );

        emailBuilder.content( content, MediaType.TEXT_HTML );

        if( properties != null && properties.isMakeScreenShort() ) {

            listener.accept( 6, "Make and attach ScreenShort" );

            try {
                Thread.sleep(1000);
                Robot robot = new Robot();
                BufferedImage screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
                File f = File.createTempFile( BaseApp.APP().getAppID(), ".jpg");
                ImageIO.write( screenShot, "JPG", f );
                //message.attachFile(f);
                emailBuilder.attach( "ScreenShort.jpg", f );
                f.deleteOnExit();
            } catch( Exception ex1 ) {
                emailBuilder.content( "Error on make screenShot: " + ex1.getLocalizedMessage() );
            }
        }

        if( properties != null && properties.isAttachLog() ) {

            listener.accept( 35, "Attach log file" );

            try {
                File f = LogManager.getCurrentLogFile();
                emailBuilder.attach( f );
                //message.attachFile(f);
            } catch (Exception ex1) {
                emailBuilder.content( "Error on attach log file: " + ex1.getLocalizedMessage() );
                //message.addContent("Attach log file error: " + ex1.getLocalizedMessage());
            }
        }

        try( StringWriter sw = new StringWriter() )
        {
            listener.accept( 50, "Attach stackTrace" );

            ex.printStackTrace ( new PrintWriter(sw) );
            emailBuilder.attach( "StackTrace.txt", sw.toString(), MediaType.TEXT_PLAIN );
        }
        catch( IOException ioex ) {

        }

        return emailBuilder;
    }

    /** */
    @Override
    public void send( Throwable ex, JInvErrorService.IErrorSendProperties properties ) {

        final JInvProgressDialog pd = new JInvProgressDialog( Modality.WINDOW_MODAL ) {
            @Override
            protected void handleSuccess( WorkerStateEvent event ) throws Exception {
                getStage().close();
            }
        };

        pd.setAction( new Callable< Void >() {
            @Override
            public Void call() throws Exception {

                BiConsumer<Integer,String> listener = new BiConsumer< Integer, String >() {
                    @Override
                    public void accept( Integer i, String s )
                    {
                        pd.setText    ( s );
                        pd.setProgress( 50 + i.longValue(), 150L );
                    }
                };

                prepare( ex, properties, listener).send( listener );

                return null;
            }
        });

        pd.showDialog("Отправка письма об ошибке");

    }
}
