package ru.inversion.fx.form;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;

/**
 Builder для JInvAlert'ов
 @author fomishkin on 14.06.2019. */
public class AlertBuilder {

    private Object windowContainer;
    private Alert.AlertType alertType;
    private String title;
    private String headerText;
    private Object contentText;
    private Object detailText;
    private Node graphic;
    private ButtonType[] buttons;

    private Modality modality = Modality.WINDOW_MODAL;

    public AlertBuilder() { }

    /**
     @param windowContainer Контроллер формы или ViewContext
     */
    public AlertBuilder( final Object windowContainer ) {
        this.windowContainer = windowContainer;
        this.alertType = Alert.AlertType.NONE;
    }

    /**
     @param windowContainer Контроллер формы или ViewContext
     */
    public AlertBuilder( final Object windowContainer, final Alert.AlertType alertType ) {
        this.windowContainer = windowContainer;
        this.alertType = alertType;
    }

    public AlertBuilder windowContainer( Object windowContainer ) {
        this.windowContainer = windowContainer;
        return this;
    }

    public AlertBuilder alertType( Alert.AlertType alertType ) {
        this.alertType = alertType;
        return this;
    }

    public AlertBuilder title( String title ) {
        this.title = title;
        return this;
    }

    public AlertBuilder headerText( String headerText ) {
        this.headerText = headerText;
        return this;
    }

    public AlertBuilder contentText( String contentText ) {
        this.contentText = contentText;
        return this;
    }

    /**
     Node подставляется вместо исходной TextArea
     */
    public AlertBuilder contentText( Node contentText ) {
        this.contentText = contentText;
        return this;
    }
    /**
     Pane подставляется вместо исходной TextArea
     */
    public AlertBuilder contentText( Pane contentText ) {
        this.contentText = contentText;
        return this;
    }

    public AlertBuilder detailText( String detailText ) {
        this.detailText = detailText;
        return this;
    }

    public AlertBuilder detailText( List<String> detailText ) {
        this.detailText = detailText;
        return this;
    }

    public AlertBuilder detailText( String[] detailText ) {
        this.detailText = detailText;
        return this;
    }

    public AlertBuilder graphic( Node graphic ) {
        this.graphic = graphic;
        return this;
    }

    public AlertBuilder modality( Modality modality ) {
        this.modality = modality;
        return this;
    }

    public AlertBuilder buttons( ButtonType... buttons ) {
        this.buttons = buttons;
        return this;
    }

    public JInvAlert build() {
        if ( alertType == null ){
            alertType = Alert.AlertType.NONE;
        }
        return Alerts.getAlert(
                windowContainer
                ,alertType
                ,title
                ,headerText
                ,contentText
                ,detailText
                ,graphic
                ,modality
                ,buttons
        );
    }
}
