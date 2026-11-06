package ru.inversion.fx.form;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 @author fomishkin on 14.06.2019. */
public class JInvAlert extends Alert {
    public JInvAlert( final AlertType alertType ) {
        super( alertType );
    }

    public JInvAlert( final AlertType alertType, final String contentText, final ButtonType... buttons ) {
        super( alertType, contentText, buttons );
    }
}
