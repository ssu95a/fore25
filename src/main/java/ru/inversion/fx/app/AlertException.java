package ru.inversion.fx.app;

/**
 * Класс для исключений которые не показываются в стандартном диалоге исключений,
 * а выводятся через @{@link javafx.scene.control.Alert}.
 * <p>
 * Может служить оберткой для других исключений.
 * Если не содержит текст, то показывается cause.
 *
 * @author Sulimoff
 * */
public class AlertException extends RuntimeException {

    /** */
    public AlertException( String message ) {
        super(message);
    }

    /** */
    public AlertException( Throwable cause ) {
        super(cause);
    }

    /** */
    static public void ofThrow( Exception ex ) throws AlertException {
        throw new AlertException( ex );
    }
}
