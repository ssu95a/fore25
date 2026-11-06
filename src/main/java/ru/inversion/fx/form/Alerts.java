package ru.inversion.fx.form;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.sound.SoundPlayer;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.app.service.ViewPrefAppService.localizeDialog;

/**
 *
 * @author ssu
 */
public class Alerts {

    //private final static Logger logger = getLogger(MethodHandles.lookup().lookupClass());

    final static private ResourceBundle g_bundle = ResourceBundle.getBundle("alerts");

    static public ButtonType showAlert(
        Object windowContainer,
        Alert.AlertType alertType,
        String title,
        String headerText,
        String contentText,
        Node graphic,
        ButtonType... buttons
    ) {
        return showAlert( windowContainer, alertType, title, headerText, (Object) contentText, graphic, buttons );
    }

    /**
     @param detailText String / List< String > / String[]
     */
    static public ButtonType showAlert(
            Object windowContainer,
            Alert.AlertType alertType,
            String title,
            String headerText,
            String contentText,
            Object detailText,
            Node graphic,
            ButtonType... buttons
    ) {
        return showAlert(windowContainer, alertType, title, headerText, (Object) contentText, detailText, graphic, buttons);
    }

    /**
     @param contentText String или Node, в случае Node оный подставляется вместо исходной TextArea
     */
    static public ButtonType showAlert(
            Object windowContainer,
            Alert.AlertType alertType,
            String title,
            String headerText,
            Object contentText,
            Node graphic,
            ButtonType... buttons
    ) {
        return showAlert(windowContainer, alertType, title, headerText, contentText, null, graphic, buttons);
    }

    /**
     @param contentText String или Node, в случае Node оный подставляется вместо исходной TextArea
     @param detailText String / List< String > / String[]
     */
    static public ButtonType showAlert(
            Object windowContainer,
            Alert.AlertType alertType,
            String title,
            String headerText,
            Object contentText,
            Object detailText,
            Node graphic,
            ButtonType... buttons
    ) {
        Modality defaultModality = Modality.WINDOW_MODAL;

        Alert alert = getAlert( windowContainer, alertType, title, headerText, contentText, detailText, graphic, defaultModality, buttons );

        return alert.showAndWait().orElse(ButtonType.CANCEL);
    }

    static JInvAlert getAlert(
            final Object windowContainer,
            final Alert.AlertType alertType,
            final String title,
            final String headerText,
            final Object contentText,
            final Object detailText,
            final Node graphic,
            final Modality modality,
            final ButtonType... buttons
    ) {
        JInvAlert alert = new JInvAlert( alertType, "", buttons );
        alert.setResizable(true);
        Window window = ViewContext.tryGetWindow(windowContainer);

        //if( BaseApp.APP().isAfterLogin() )
        {
            //Защита от бага FX (HeavyweightDialog.java:329) - JAVAKERNEL-1572
            if (!(window instanceof Stage) || window.getScene() != null)
            {
                U.callIfNotNull(alert::initOwner, window);
            }
        }
        alert.setTitle(U.nvl(title, g_bundle.getString("TITLE_" + alertType.name())));
        alert.setHeaderText(headerText);
        U.callIfNotNull(alert::setGraphic, graphic);
        if (contentText != null)
        {
            if (contentText instanceof Node) {
                setNode4Content((Node) contentText, alert);
            } else {
                setTextArea4Content(contentText.toString(), alert);
            }
        }
        String expandText = null;
        if( detailText != null )
        {
            if (detailText instanceof String) {
                expandText = (String) detailText;
            } else if (detailText instanceof String[]) {
                List<String> sa = Arrays.asList((String[]) detailText);
                expandText = String.join("\n", sa);
            } else if (detailText instanceof List) {
                List<String> sa = (List<String>) detailText;
                expandText = String.join("\n", sa);
            }
            else if( detailText instanceof Node ) {
                alert.getDialogPane().setExpandableContent( (Node)detailText );
                alert.getDialogPane().setExpanded(true);
            }
        }//end if
        if (expandText != null) {
            //Label label = new Label("Подробности:");

            TextArea textArea = new TextArea(expandText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            //expContent.add( label,    0, 0 );
            expContent.add(textArea, 0, 1);
            alert.getDialogPane().setExpandableContent(expContent);
        }//end if
        initBeforeShow( alertType, alert, window, modality );
        return alert;
    }

    public class Builder{

    }


    /**
     Общий код инициализации алертов перед показом
     */
    private static void initBeforeShow( final Alert.AlertType alertType, final Alert alert, final Window window, Modality modality ) {
        alert.initModality( modality );

        //localize
        localizeDialog( alert.getDialogPane() );

        //fix enter
        //Enter прошит как ответ "Да", поэтому гасим его событие и нажимаем нужную кнопку сами
        alert.setOnShown( event -> {
            alert.getDialogPane().getScene().addEventFilter(
            KeyEvent.KEY_PRESSED, (KeyEvent ke) -> {
                if (ke.getCode() == KeyCode.ENTER) {
                    ke.consume();
                    ButtonBase button = (ButtonBase) ke.getTarget();
                    if ( button != null ){
                        button.fire();
                    }
                }
            });
        } );

        //play sound
        if ( BaseApp.APP().isAfterLogin() ) {
            SoundPlayer.playSound( alertType );
        }

        //logger.trace("Alert '{}': type={}, window={}", alert, alertType, window);
        //maximize if minimized
        U.callIfNotNull(w -> {
            if ( w instanceof Stage ) {
                Stage stage = (Stage) w;
//                logger.info("#1 Stage {}: iconified={}, aot={}, showing={}",stage,stage.isIconified(),stage.isAlwaysOnTop(), stage.isShowing());
                stage.setAlwaysOnTop(true);
                stage.setAlwaysOnTop(false);
                stage.setIconified  ( false );
//                logger.info("#2 Stage {}: iconified={}, aot={}, showing={}",stage,stage.isIconified(),stage.isAlwaysOnTop(), stage.isShowing());
            }
        }, window );
    }

    /**
     * Устанавливаем текстарея для текста сообщения
     *
     * @param contentText
     * @param alert
     */
    private static void setTextArea4Content(String contentText, Alert alert) {

        if( contentText != null && !contentText.isEmpty() ) {

            alert.setContentText(contentText);

            VBox vbox = new VBox();
            vbox.setPadding(new Insets(5));

            TextArea textArea = new TextArea();
            vbox.getChildren().add(textArea);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            textArea.setWrapText(true);
            textArea.setEditable(false);

            final Text text = new Text( textArea.textProperty().get() );
            text.textProperty().bind(textArea.textProperty());
            text.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                textArea.setPrefHeight(text.getLayoutBounds().getHeight() + 30);
            });
            //textArea.setPrefHeight( text.getLayoutBounds().getHeight() + 20 );
            alert.getDialogPane().setContent(vbox);
            textArea.textProperty().bind(alert.contentTextProperty());
            //textArea.setMaxHeight( 550 );
        }
    }

    /**
     *
     */
    private static void setNode4Content(Node contentNode, Alert alert) {

        if (contentNode != null) {
            VBox vbox = new VBox();
            vbox.setPadding(new Insets(5));
            vbox.getChildren().add(contentNode);
            VBox.setVgrow(contentNode, Priority.ALWAYS);

            alert.getDialogPane().setContent(vbox);
        }
    }

    /**
     *
     */
    static private int getLineCount( String str ) {
        int len = 2;

        if (!S.isNullOrEmpty(str)) {

            if (str.contains("\n")) {
                len = str.split("\n").length;
            } else {
                len = str.length() / 60;
            }
        }

        return Math.max(len, 2);
    }

    /**
     *
     */
    static public Optional<Boolean> yesNoCancel( Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {

        ButtonType bt = showAlert(
            parentWindow,
            Alert.AlertType.CONFIRMATION,
            title,
            headerText,
            contentText,
            detailText,
            graphic,
            ButtonType.YES, ButtonType.NO, ButtonType.CANCEL
        );

        if (bt == ButtonType.YES) {
            return Optional.of(Boolean.TRUE);
        }
        if (bt == ButtonType.NO) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        return yesNoCancel(parentWindow, title, headerText, contentText, detailText, null);
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String title, String headerText, String contentText) {
        return yesNoCancel(parentWindow, title, headerText, contentText, null);
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String title, String headerText) {
        return yesNoCancel(parentWindow, title, headerText, null);
    }

    static public Optional<Boolean> yesNoCancel(Object parentWindow, String headerText) {
        return yesNoCancel(parentWindow, null, headerText);
    }

    /**
     *
     */
    static public boolean yesNo(Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {
        return showAlert(parentWindow, Alert.AlertType.CONFIRMATION, title, headerText, contentText, detailText, graphic, ButtonType.YES, ButtonType.NO) == ButtonType.YES;
    }

    static public boolean yesNo(Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        return yesNo(parentWindow, title, headerText, contentText, detailText, null);
    }

    static public boolean yesNo(Object parentWindow, String title, String headerText, String contentText) {
        return yesNo(parentWindow, title, headerText, contentText, null);
    }

    static public boolean yesNo(Object parentWindow, String title, String headerText) {
        return yesNo(parentWindow, title, headerText, null);
    }

    static public boolean yesNo(Object parentWindow, String headerText) {
        return yesNo(parentWindow, null, headerText);
    }

    /**
     *
     */
    static public void info(Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {
        showAlert(parentWindow, Alert.AlertType.INFORMATION, title, headerText, contentText, detailText, graphic, ButtonType.OK);
    }

    static public void info(Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        info(parentWindow, title, headerText, contentText, detailText, null);
    }

    static public void info(Object parentWindow, String title, String headerText, String contentText) {
        info(parentWindow, title, headerText, contentText, null);
    }

    static public void info(Object parentWindow, String title, String headerText) {
        info(parentWindow, title, headerText, null);
    }

    static public void info(Object parentWindow, String headerText) {
        info(parentWindow, null, headerText);
    }

    /** */
    static public void error( Object parentWindow, String title, String headerText, String contentText, Object detailText, Node graphic) {
        showAlert(parentWindow, Alert.AlertType.ERROR, title, headerText, contentText, detailText, graphic, ButtonType.OK);
    }
    /** */
    static public void error( Object parentWindow, String title, String headerText, String contentText, Object detailText) {
        error(parentWindow, title, headerText, contentText, detailText, null);
    }
    /** */
    static public void error( Object parentWindow, String title, String headerText, String contentText) {
        error( parentWindow, title, headerText, contentText, null);
    }
    /** */
    static public void error( Object parentWindow, String title, String headerText) {
        error( parentWindow, title, headerText, null);
    }
    /** */
    static public void error( Object parentWindow, String headerText) {
        error(parentWindow, null, headerText);
    }

    /** fluent zone */
    public interface IAlertMaker {
        /** */
        IAlertMaker window( Object w );
        /** */
        IAlertMaker type  ( Alert.AlertType type );
        /** */
        IAlertMaker title ( String title );
        /** */
        IAlertMaker headerText  ( String headerText );
        /** */
        IAlertMaker content     ( Object content );
        /** */
        IAlertMaker contentText ( String contentText );
        /** */
        IAlertMaker detail      ( Object detail );
        /** */
        IAlertMaker checkBoxText( String detailText );
        /** */
        IAlertMaker graphic( Node graphic );
        /** */
        IAlertMaker modality( Modality modality );
        /** */
        IAlertMaker buttons( ButtonType... buttons );
        /** */
        <T> Optional<T> showAndWait();
    }

}
