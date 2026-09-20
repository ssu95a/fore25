package ru.inversion.fx.app.sound;
import java.awt.Toolkit;
import java.lang.invoke.MethodHandles;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.frame.menu.PropertyItemEnum;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 Воспроизведение системных звуков
 @author fomishkin on 06.03.2019. */
public class SoundPlayer {
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    public static void playSound( Alert.AlertType alertType ){
        if ( alertType == null ){
            alertType = Alert.AlertType.NONE;
            logger.error( "alertType == null!" );
        }
        switch ( alertType ) {
            case NONE:
            case INFORMATION:
                playSound( SoundType.INFO );
                break;
            case CONFIRMATION:
                playSound( SoundType.QUESTION );
                break;
            case WARNING:
                playSound( SoundType.WARNING );
                break;
            case ERROR:
                playSound( SoundType.ERROR );
                break;
        }
    }
    public static void playSound(SoundType soundType){
        String soundLevel = BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER )
                                             .getProperty( PropertyItemEnum.SOUND_LEVEL.getName() );
        if ( S.isNullOrEmpty(soundLevel) ){
            return;
        }
        try {
            switch ( SoundLevel.valueOf( soundLevel ) ) {
                case OFF:
                    return;
                case ERROR:
                    if ( !U.in(soundType, SoundType.ERROR) ) return;
                    break;
                case WARNING:
                    if ( !U.in(soundType, SoundType.ERROR, SoundType.WARNING) ) return;
                    break;
                case QUESTION:
                    if ( !U.in(soundType, SoundType.ERROR, SoundType.WARNING, SoundType.QUESTION) ) return;
                    break;
                case ALL:
                    break;
            }
        } catch ( Throwable e ) {
            logger.error( "Unknown soundLevel {}, not playing sound.\n{}", soundLevel, e.getMessage() );
            return;
        }
        logger.trace( "playing sound {}", soundType );
        String sound;
        switch ( soundType ) {
            case QUESTION:
//                sound = "win.sound.question";
//                break;
            case WARNING:
            case ERROR:
                sound = "win.sound.exclamation";
                break;
            default:
                sound = "win.sound.asterisk";
                break;
        }
        final Runnable runnable = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty(sound);
        if (runnable != null) {
            runnable.run();
        }
    }
}
