package ru.inversion.fx.app.login;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import ru.inversion.db.JInvDbException;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.Alerts;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.Objects;
import java.util.ResourceBundle;

public class LoginPinInputDialog extends Dialog<Boolean> {

    final static private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    private final static int WAIT_SECONDS = 20;

    private final GridPane   grid;
    private final Label      label;
    private final Label      countDownLabel;
    private final TextField  textField;
    private final Connection connection;
    private final String     userName;

    /**
     * Creates a new TextInputDialog with the default value entered into the
     * dialog {@link TextField}.
     */
    public LoginPinInputDialog( Connection connection, String userName ) {

        this.connection = Objects.requireNonNull( connection, "'connection' is null" );
        this.userName   = Objects.requireNonNull( userName, "'userName' is null" );

        final DialogPane dialogPane = getDialogPane();

        // -- textfield
        this.textField = new TextField();
        this.textField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow    ( textField, Priority.ALWAYS );
        GridPane.setFillWidth( textField, true );

        // -- label
        label = new Label("PIN");
//        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
//        label.textProperty().bind(dialogPane.contentTextProperty());

        final int[] time = { WAIT_SECONDS };

        countDownLabel = new Label();

        this.grid = new GridPane();
        this.grid.setHgap(10);
        this.grid.setVgap(10);
        this.grid.setMaxWidth ( Double.MAX_VALUE);
        this.grid.setAlignment( Pos.CENTER_LEFT );

        dialogPane.contentTextProperty().addListener(o -> updateGrid());
        setTitle( bundle.getString("LOGIN_HEADER") );

        dialogPane.setHeaderText ( bundle.getString("LABEL_ENTER_PIN") );
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label l = IconFactory.getLabel( FontAwesome.fa_id_badge, IconSize.LARGE, Color.LIGHTSKYBLUE );

        l.setStyle( l.getStyle() + ";-fx-font-size:2em;");
        dialogPane.graphicProperty().setValue( l ) ; //IconFactory.getLabel( FontAwesome.fa_id_badge, IconSize.LARGE, Color.DARKGRAY ) );

        updateGrid();

        setResultConverter(( dialogButton ) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? checkPin() : Boolean.FALSE;
        });


        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.millis(1000), //1000 мс * 60 сек = 1 мин
                        ae -> {
                            time[0]--;
                            //countDownLabel.setText( Integer.toString( (int) ( (double)time[0] / (double)WAIT_SECONDS ) ) + " сек." );
                            countDownLabel.setText( Integer.toString( time[0] ) + " сек." );

                            if( time[0] == 0 )
                                ((Button)dialogPane.lookupButton(ButtonType.CANCEL)).fire();
                        }
                )
        );
        // timeline.stop();
        timeline.setCycleCount(time[0]);
        timeline.play(); //Запускаем
    }

    /** */
    private Boolean checkPin( ) {

        final String sp_check = "{?= call XXI_USER.Check_PIN(?,?)}";
        final String sp_ermsg = "{?= call XXI_USER.Get_ErrorMessage()}";

        String lastSp = sp_check;

        try {

            int result = 0;

            try( CallableStatement cs = connection.prepareCall(lastSp) ) {

                cs.registerOutParameter(1, Types.INTEGER);
                cs.setString(2, userName );
                cs.setString(3, textField.getText() );

                cs.execute();

                result = cs.getInt(1);

                if(result == 0)
                    return true;
            }

            lastSp = sp_ermsg;

            String errMsg;

            try(CallableStatement cs = connection.prepareCall(lastSp)) {
                cs.registerOutParameter(1, Types.VARCHAR);
                cs.execute();
                errMsg = cs.getString(1);
            }

            Alerts.error( this.getDialogPane(), errMsg );
        }
        catch( Throwable th ) {
            JInvErrorService.handleException(
                this.getDialogPane().getScene().getWindow(),
                new JInvDbException( th, lastSp )
            );
        }

        return false;
    }

    /**
     * Creates a Label node that works well within a Dialog.
     * @param text The text to display
     */
    static Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        return label;
    }

    /** */
    private void updateGrid() {
        grid.getChildren().clear();

        grid.add(label, 0, 0);
        grid.add(textField, 1, 0);
        grid.add(countDownLabel, 0, 1, 2, 1 );
        getDialogPane().setContent(grid);

        Platform.runLater(() -> textField.requestFocus());
    }

}
