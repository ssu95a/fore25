package ru.inversion.fx.app.login;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import ru.inversion.db.DbNoDataFoundException;
import ru.inversion.db.JInvDbException;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.Alerts;
import ru.inversion.utils.Pair;
import ru.inversion.utils.TriFunction;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.sql.*;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.function.Function;

/** */
public class ChangePasswordDialog extends Dialog<String> {

    /** */
    private static final Function<Connection, TriFunction<Connection, Pair<String,String>, Window, Boolean>> passwordChangerFactory
            = new Function<Connection, TriFunction<Connection, Pair<String,String>, Window, Boolean>>() {

        @Override
        public TriFunction<Connection, Pair<String,String>, Window, Boolean> apply( Connection connection ) {

            try {
                if( connection.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle") )
                    return this::change_Ora;

                return this::change_Pg;

            } catch( SQLException e ) {
                throw new RuntimeException(Tags.PRODUCT_LABEL + "Error on prepare func 4 change password", e );
            }
        }

        private boolean change_Ora(Connection connection, Pair<String,String> newPassword, Window window )
        {
            final String sp_name = "{?= call XXI.XXI_Logon.ChangePass(?,?)}";

            try {

                int result = 0;

                try( CallableStatement cs = connection.prepareCall( sp_name ) ) {

                    cs.registerOutParameter(1, Types.INTEGER );
                    cs.registerOutParameter(2, Types.VARCHAR );
                    cs.setString( 3, newPassword.second ); //Убрано toUpperCase

                    cs.execute();

                    result = cs.getInt(1);

                    if( result == 0 )
                        return true;

                    Alerts.error( window, cs.getString(2) );
                }

            }
            catch( Throwable th ) {
                JInvErrorService.handleException( window, new JInvDbException( th, sp_name ) );
            }
            return false;
        }

        /** */
        private boolean change_Pg( Connection connection, Pair<String,String> newPassword, Window window )
        {
            final String sp_name = "{ call xxi_pseudo.change_XXI_Password(?,?,?,?)}";

            try {

                int result = 0;

                try( CallableStatement cs = connection.prepareCall( sp_name ) ) {

                    cs.setString( 1, newPassword.first );
                    cs.setString( 2, newPassword.second );
                    cs.registerOutParameter(3, Types.NUMERIC );
                    cs.registerOutParameter(4, Types.VARCHAR );

                    cs.execute();

                    result = TypeConverter.convert( cs.getObject(3), Integer.class );

                    if( result == 0 )
                        return true;

                    Alerts.error( window, cs.getString(4) );
                }

            }
            catch( Throwable th ) {
                JInvErrorService.handleException( window, new JInvDbException( th, sp_name ) );
            }
            return false;

        }
    };

    /** */
    private static final Function<Connection, TriFunction<Connection, Pair<String,String>, Window, Boolean>> passwordChangerFactory_Manual
            = new Function<Connection, TriFunction<Connection, Pair<String,String>, Window, Boolean>>() {

        @Override
        public TriFunction<Connection, Pair<String,String>, Window, Boolean> apply( Connection connection ) {

            try {
                if( connection.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle") )
                    return this::change_Ora_Manual;

                return this::change_Pg_Manual;

            } catch( SQLException e ) {
                throw new RuntimeException(Tags.PRODUCT_LABEL + "Error on prepare func 4 change password", e );
            }
        }

        /** */
        private boolean change_Ora_Manual( Connection connection, Pair<String,String> newPassword, Window window )
        {
            String sp_name = "{?=call XXI_USER.Set_Up_Password(?,?)}";

            try {

                int result = 0;

                try( CallableStatement cs = connection.prepareCall( sp_name ) )
                {
                    cs.registerOutParameter( 1, Types.INTEGER );
                    cs.setString( 2, newPassword.first );
                    cs.setString( 3, newPassword.second); //Убрано toUpperCase

                    cs.execute();

                    result = cs.getInt(1);

                    if( result == 0 )
                        return true;

                }

                sp_name = "{?=call XXI_USER.Get_ErrorMessage()}";

                String errMsg = null;

                try( CallableStatement csErr = connection.prepareCall(sp_name) )
                {
                    csErr.registerOutParameter( 1, Types.VARCHAR );
                    csErr.execute();

                    errMsg = csErr.getString(1);
                }

                Alerts.error( window, errMsg );
            }
            catch( Throwable th ) {
                JInvErrorService.handleException( window, new JInvDbException( th, sp_name ) );
            }
            return false;
        }

        private boolean change_Pg_Manual( Connection connection, Pair<String,String> newPassword, Window window )
        {
            final String sql = "SELECT * FROM xxi_user.set_up_password(?, ?)";

            try( PreparedStatement ps = connection.prepareStatement(sql) )
            {
                ps.setString( 1, newPassword.first );
                ps.setString( 2, newPassword.second);

                try( ResultSet rs = ps.executeQuery())
                {
                    if( rs.next() )
                    {
                        int errRet = U.nvl( TypeConverter.convert( rs.getObject(1), Integer.class ), -1 );
                        if( errRet == 0 )
                            return true;

                        Alerts.error( window,  rs.getString(2) );
                    }
                    else
                        throw new DbNoDataFoundException( sql, "Ф-ция 'xxi_user.set_up_password' не вернула результат своей работы", Collections.singletonList(newPassword.first));
                }
            }catch( Throwable th ) {
                JInvErrorService.handleException( window, new JInvDbException( th, sql ) );
            }
            return false;
        }
    };

    final static private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    private final GridPane      grid;

    private final Label         label1,
                                label2,
                                labelWarn = new Label("Пароли должны совпадать");

    private final PasswordField textPassword1,
                                textPassword2;

    private final Connection    connection;
    private final String        userName;

    private final TriFunction<Connection,Pair<String,String>, Window, Boolean> passwordChanger;

    /** */
    public ChangePasswordDialog( Connection connection, String userName ) {
        this(connection, userName, false);
    }

    public ChangePasswordDialog( Connection connection, String userName, boolean manual )
    {
        this.connection      = connection;
        this.userName        = userName;
        this.passwordChanger = manual ?
                               passwordChangerFactory_Manual.apply(connection)
                               :
                               passwordChangerFactory.apply(connection);

        final DialogPane dialogPane = getDialogPane();

        label1 = new Label( bundle.getString("CHANGE_PSWD_NEW_PSWD") );
        this.textPassword1 = new PasswordField ();
        this.textPassword1.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow    ( textPassword1, Priority.ALWAYS );
        GridPane.setFillWidth( textPassword1, true );

        label2 = new Label(bundle.getString("CHANGE_PSWD_CONFIRM") );
        this.textPassword2 = new PasswordField ();
        this.textPassword2.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow    ( textPassword2, Priority.ALWAYS);
        GridPane.setFillWidth( textPassword2, true );

        this.labelWarn.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow    ( labelWarn, Priority.ALWAYS);
        GridPane.setFillWidth( labelWarn, true );

        this.setTitle("Смена пароля");

        this.grid = new GridPane();
        this.grid.setHgap(10);
        this.grid.setVgap(10);
        this.grid.setMaxWidth (Double.MAX_VALUE);
        this.grid.setAlignment(Pos.CENTER_LEFT);

        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        dialogPane.setHeaderText(
            bundle.getString(
                manual ? "CHANGE_PSWD_HEADER_TEXT_MANUAL" : "CHANGE_PSWD_HEADER_TEXT"
            )
        );
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        labelWarn.setStyle( "-fx-background-color: #ffff9999; -fx-background-radius:5; -fx-background-insets: -5;" );

        updateGrid();

        setResultConverter((dialogButton) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? textPassword1.getText() : null;
        });

        final Button buttonOk = (Button)dialogPane.lookupButton(ButtonType.OK);

        buttonOk.addEventFilter (
            ActionEvent.ACTION,
            event ->{
                if( !changePassword() )
                     event.consume();
            }
        );

        buttonOk.disableProperty().bind(
                textPassword1.textProperty().isEmpty()
                        .or(textPassword2.textProperty().isEmpty())
                        .or(textPassword1.textProperty()
                                .isNotEqualTo(textPassword2.textProperty()))
        );

        labelWarn.visibleProperty().bind(
            textPassword1.textProperty()
                .isNotEqualTo(textPassword2.textProperty())
        );

        labelWarn.managedProperty()
                .bind(labelWarn.visibleProperty());

    }

    /** */
    private boolean changePassword( ) {
        return passwordChanger.apply( connection, Pair.makePair( userName,textPassword1.getText() ), getDialogPane().getScene().getWindow() );
    }

    /*
    private boolean changePassword( ) {

        final String sp_name = "{?= call XXI.XXI_Logon.ChangePass(?,?)}";

        try {

            int result = 0;

            String newPassword = textPassword1.getText().toUpperCase();

            try( CallableStatement cs = connection.prepareCall( sp_name ) ) {

                cs.registerOutParameter(1, Types.INTEGER );
                cs.registerOutParameter(2, Types.VARCHAR );
                cs.setString( 3, newPassword  );

                cs.execute();

                result = cs.getInt(1);

                if( result == 0 )
                    return true;

                Alerts.error( this.getDialogPane(), cs.getString(2) );
            }

        }
        catch( Throwable th ) {
            JInvErrorService.handleException(
                    this.getDialogPane().getScene().getWindow(),
                    new JInvDbException( th, sp_name )
            );
        }
        return false;
    }
    */


    /** */
    private void updateGrid() {

        grid.getChildren().clear();

        grid.add(label1, 0, 0);
        grid.add(textPassword1, 1, 0);
        grid.add(label2, 0, 1);
        grid.add(textPassword2, 1, 1);
        grid.add( labelWarn, 0, 2, 2, 1 );

        getDialogPane().setContent(grid);

        Platform.runLater(() -> textPassword1.requestFocus());
    }
}
