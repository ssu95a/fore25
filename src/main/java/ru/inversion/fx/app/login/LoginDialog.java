package ru.inversion.fx.app.login;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.JInvPasswordField;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.utils.S;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.Optional;
import java.util.ResourceBundle;

import static ru.inversion.fx.app.AppConstants.*;

/**
 * Диалог входа в систему
 * <br>
 * @author Sulimoff
 */
public class LoginDialog extends Dialog<Boolean> {

    final static private ResourceBundle bundle = ResourceBundle.getBundle("fore");

    final private static double defaultFontSize = 14.0;

    private final GridPane      grid;

    private final ChoiceBox<String> cbProfile;

    final private Label         lbProfile;

    final private Button        btProfile,
                                btTns;

    final private Label         lbLogin,
                                lbPassword, lbDb, lbTns;

    final private JInvTextField edLogin,
                                edDb,
                                edTns;

    private final JInvPasswordField   edPassword;

    private boolean showTNSNamesPath = false, middleConnectMode = false;


    public LoginDialog() {

        IAppProperties appProps = BaseApp.APP().getProperties(PropertiesTypeEnum.PRP);

        middleConnectMode = "middle".equalsIgnoreCase( appProps.getStringProperty("connection_mode", "direct") );

        showTNSNamesPath = !middleConnectMode
                            && (
                                S.isNullOrEmpty( System.getProperty("oracle.net.tns_admin"))
                                ||
                                appProps.getBooleanProperty("show_tnsnames_path", false )
                            );

        try {
            ((Stage) getDialogPane().getScene().getWindow()).getIcons().add(ViewPrefAppService.getAppIcon(defaultFontSize));
        } catch (Throwable ex) {
            ;
        }

        this.setResizable ( false );
        this.setTitle     ( bundle.getString("LOGIN_TITLE") );
        this.setHeaderText( bundle.getString("LOGIN_HEADER") );

        final DialogPane dialogPane = getDialogPane();

        dialogPane.setStyle("-fx-font-size: " + defaultFontSize + "px");

        Label l = IconFactory.getLabel( FontAwesome.fa_user, IconSize.LARGE, Color.LIGHTSKYBLUE );
        l.setStyle( l.getStyle() + ";-fx-font-size:2em;");

        dialogPane.graphicProperty().setValue( l ) ; //IconFactory.getLabel( FontAwesome.fa_id_badge, IconSize.LARGE, Color.DARKGRAY ) );

        grid = new GridPane();
        this.grid.setHgap(5);
        this.grid.setVgap(5);
        this.grid.setMaxWidth ( Double.MAX_VALUE );
        this.grid.setAlignment( Pos.CENTER_LEFT  );

        lbProfile = new Label(bundle.getString("LABEL_PROFILE"));
        cbProfile = new ChoiceBox<>(getProfileModel());

        btProfile = new Button();
        btProfile.setGraphic( IconFactory.getLabel( FontAwesome.fa_close, IconSize.SMALL ) );
        btProfile.setOnAction(( ActionEvent event) -> {
            int index = cbProfile.getSelectionModel().getSelectedIndex();
            if( index != -1 )
                cbProfile.getItems().remove(index);
        });

        lbLogin = new Label( bundle.getString("LABEL_LOGIN") );
        edLogin = new JInvTextField( );
        edLogin.setCaseSensitiveMode(RegisterEnum.UPPER_CASE);

        lbPassword = new Label(bundle.getString("LABEL_PASSWORD"));
        edPassword = new JInvPasswordField();

        lbDb = new Label(bundle.getString( !middleConnectMode ? "LABEL_DB" : "LABEL_SERVER") );
        edDb = new JInvTextField();

        if( showTNSNamesPath )
        {
            lbTns = new Label(bundle.getString("LOGIN_TNSPATH"));
            edTns = new JInvTextField(System.getProperty("oracle.net.tns_admin"));

            btTns = new Button();
            btTns.setGraphic( IconFactory.getLabel( FontAwesome.fa_folder_open ) );
            btTns.setOnAction( (e) -> selectTNSFolder() );
        }
        else {
            lbTns = null; edTns = null; btTns = null;
        }

        getDialogPane().getButtonTypes().addAll( ButtonType.OK, ButtonType.CANCEL );

        final Button btOk = (Button) getDialogPane().lookupButton(ButtonType.OK);
        btOk.setId("okButton");
        btOk.addEventFilter(ActionEvent.ACTION, (ActionEvent event) -> {
            if (!onOK()) {
                event.consume();
            }
        });

        cbProfile.valueProperty().addListener(( observable, oldValue, newValue ) -> {

            if( newValue != null && !newValue.isEmpty() ) {

                String sa[] = newValue.split("@");

                if( sa.length > 0 )
                    edLogin.setText(sa[0]);

                if( sa.length > 1 )
                    edDb.setText(sa[1]);

            }//end if
        });

        if(!cbProfile.getItems().isEmpty()) {
            cbProfile.getSelectionModel().select( cbProfile.getItems().size() - 1 );
            Platform.runLater(edPassword::requestFocus);
        } else {
            Platform.runLater(edLogin::requestFocus);
        }

        // если заданы логин и база
        String s = appProps.getStringProperty( LOGIN );
        if( !S.isNullOrEmpty(s) )
            edLogin.setText( s );

        s = appProps.getStringProperty( DB );
        if( !S.isNullOrEmpty(s) )
            edDb.setText( s );

        setResultConverter((ButtonType button) -> (button.getButtonData() == ButtonData.OK_DONE));

        updateGrid( );

        try {

            String lang = sun.awt.im.InputContext.getInstance().getLocale().getISO3Language().toUpperCase();

            Tooltip langTooltip = new Tooltip( lang );
            langTooltip.setAutoHide( true );
            edPassword.setTooltip  ( langTooltip );
            langTooltip.setStyle( lang.equals("RUS") ? "-fx-background-color:darkred; -fx-text-fill:white;" : "-fx-background-color: darkgreen;-fx-text-fill:white;" );

            Platform.runLater (
                this::showLang
            );

            getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler< KeyEvent >() {
                @Override
                public void handle( KeyEvent event ) {
                    hideLang();
                    getDialogPane().removeEventFilter(KeyEvent.KEY_PRESSED,this);
                }
            });
        }
        catch( Throwable th ) {
            th.printStackTrace();
        }
    }

    /** */
    private void showLang( )
    {
        final Bounds layoutBounds = edPassword.getLayoutBounds();

        Point2D p =
                edPassword.localToScreen (
                        layoutBounds.getMaxX(),
                        layoutBounds.getMaxY()
        );

        edPassword.getTooltip().show( edPassword, p.getX(), p.getY() );
    }

    /** */
    private void hideLang( )
    {
        final Tooltip tooltip = edPassword.getTooltip();

        if( tooltip != null )
        {
            tooltip.hide();
            edPassword.setTooltip(null);
        }
    }

    /** */
    private void updateGrid() {

        grid.getChildren().clear();

        grid.add( lbProfile, 0, 0 );
        grid.add( cbProfile, 1, 0 );
        grid.add( btProfile, 2, 0 );
        GridPane.setHalignment( btProfile, HPos.RIGHT);

        Separator separator = new Separator();
        grid.add(separator, 0, 1);
        GridPane.setColumnSpan(separator, 4);

        grid.add( lbLogin, 0, 2 );
        grid.add( edLogin, 1, 2 );
        GridPane.setColumnSpan( edLogin, 2);

        grid.add( lbPassword, 0, 3 );
        grid.add( edPassword, 1, 3 );
        GridPane.setColumnSpan( edPassword, 2);

        grid.add( lbDb, 0, 4 );
        grid.add( edDb, 1, 4 );
        GridPane.setColumnSpan( edDb, 2 );

        if( showTNSNamesPath ) {

            Separator sep = new Separator();
            grid.add( sep, 0, 5 );
            GridPane.setColumnSpan( sep, 4 );

            grid.add( lbTns, 0, 6 );
            grid.add( edTns, 1, 6 );
            grid.add( btTns, 2, 6 );
        }
        getDialogPane().setContent(grid);
    }

    /** */
    private ObservableList<String> getProfileModel( ) {

        try {

            File userDir = new File( System.getProperty("user.home") );
            File file    = new File( userDir, !middleConnectMode ? "xxilogon.txt" : "xxilogon_m.txt");

            if( file.exists() && file.isFile() )
                return FXCollections.observableList( Files.readAllLines(file.toPath()) );

        } catch (Throwable e) {
            e.printStackTrace( );
        }
        return FXCollections.emptyObservableList();
    }

    /** */
    private void saveProfile() {

        try {

            File userDir = new File(System.getProperty("user.home"));

            File file = new File(userDir, !middleConnectMode ? "xxilogon.txt" : "xxilogon_m.txt");

            try( FileWriter fw = new FileWriter(file) )
            {
                final String currentLP = edLogin.getText().trim() + "@" + edDb.getText().trim();

                for( String s : cbProfile.getItems() )
                {
                    if( currentLP.equalsIgnoreCase(s) )
                        continue;

                    fw.write( s );
                    fw.write('\n');
                }

                fw.write( currentLP );
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    private void selectTNSFolder() {

        DirectoryChooser dch = new DirectoryChooser();
        dch.setTitle(bundle.getString("LOGIN_TNSPATH"));

        String s = edTns.getText();

        if( !S.isNullOrEmpty(s) )
        {
            File d = new File(s);

            if( d.exists() )
            {
                if (d.isFile()) {
                    d = d.getParentFile();
                }
                dch.setInitialDirectory(d);
            }
        }

        File dir = dch.showDialog(this.getDialogPane().getScene().getWindow());

        if ( dir != null ) {
            edTns.setText(dir.toString());
        }
    }


    private Boolean onOK() {

        try {

            if( showTNSNamesPath )
            {
                String s = edTns.getText();

                if( !S.isNullOrEmpty(s) )
                    System.setProperty( ORACLE_TNS_ADMIN, edTns.getText() );
            }

            LoginManager.doLogin( edLogin.getText(), edPassword.getText(), edDb.getText() );

            saveProfile();

            return true;

        }
//        catch( SecurityException sex ) {
//
//            Logger logger = LoggerFactory.getLogger(JInvErrorService.class);
//            logger.error( sex.getLocalizedMessage() );
//
//            return false;
//        }
        catch( Exception ex ) {
            JInvErrorService.handleException( grid.getScene().getWindow(), ex );
        }
        return false;
    }

    /** */
    public Boolean doModal() {
        Optional<Boolean> result = showAndWait();
        return result.orElse(Boolean.FALSE);
    }

}
