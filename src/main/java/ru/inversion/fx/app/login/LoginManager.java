package ru.inversion.fx.app.login;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.crypto.etoken.ETokenInvo;
import ru.inversion.db.session.AbstractDataSourceCfgBuilder;
import ru.inversion.db.session.DataSourceCfg;
import ru.inversion.db.session.SessionEnvironment;
import ru.inversion.fx.app.AlertException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.ViewContext;
//import ru.inversion.priv.tools.dcont.DCont;
//import ru.inversion.priv.tools.mdom.MDom;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Holder;
import ru.inversion.utils.S;
import ru.inversion.utils.dco.Dco;
import ru.inversion.utils.dco.IDco;
import sun.security.provider.X509Factory;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static ru.inversion.fx.app.AppConstants.*;
import static ru.inversion.fx.app.BaseApp.APP;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.PRP;

/**
 *
 * @author ssu @
 */
public class
LoginManager {

    protected static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    final private static Logger g_logger = LoggerFactory.getLogger(LoginManager.class);

    /** */
    public static boolean showLoginDialog() {
        LoginDialog loginDialog = new LoginDialog();
        return loginDialog.doModal();
    }

    /** */
    private void loadAndSaveImage( )
    {

    }

    /** */
    static void doLogin( String login, String password, String dbUrl ) throws Exception
    {
        String middleDrvServer = APP().getProperties(PRP).getStringProperty( MIDDLE_DRV_SERVER );

        if( middleDrvServer != null )
        {
            middleDrvServer = middleDrvServer.trim();

            if( middleDrvServer.isEmpty() )
                middleDrvServer = null;
        }

        doLogin( login, password, dbUrl, APP().getProperties(PRP).getBooleanProperty( PSEUDO_CONNECTION, true ), middleDrvServer, Collections.emptyMap() );
    }

    /** */
    static void doLogin( String login, String password, String dbUrl, boolean pseudoConnection, String middleDrvServer, Map<String,String> additional ) throws Exception {

//        Function<Connection,Boolean> checkPinHandler = connection -> {
//            LoginPinInputDialog lpid = new LoginPinInputDialog( connection, login );
//            return lpid.showAndWait().get();
//        };
//
//        Function<Connection,String> changePasswordHandler = connection -> {
//            ChangePasswordDialog cpid = new ChangePasswordDialog( connection, login );
//            return cpid.showAndWait().get();
//        };

        final String l = login;
        final Holder<Consumer<ViewContext>> hcnsmr = new Holder<>();

        BiFunction<Connection, Object, Object> resultHandler = ( connection, o ) -> {
            if( o instanceof Integer )
            {
                int result = (Integer)o;
                if( result == 1 ) {
                    ChangePasswordDialog cpid = new ChangePasswordDialog( connection, l );
                    return cpid.showAndWait().orElse(S.EMPTY_STRING);
                }
                else if( result == 2 ) {
                    LoginPinInputDialog lpid = new LoginPinInputDialog( connection, l );
                    return lpid.showAndWait().orElse(Boolean.FALSE);
                }
            }
            else
            {
                if( o != null ) {
                    hcnsmr.set( vc -> Alerts.info( vc, fore.getString( "LOGIN_HEADER" ), o.toString(), null ) );
                }
            }
            return null;
        };

        final IAppProperties       appProperties = APP().getProperties(PRP);
        final AbstractDataSourceCfgBuilder dscb = AbstractDataSourceCfgBuilder.create(s -> {
            switch(s) {
                case "middle_drv_server":
                     return middleDrvServer;
                case "app_id":
                    return BaseApp.APP().getAppID();
                case "auth_server":
                    return appProperties.getStringProperty("auth_server");
                case "vendor_db":
                    return appProperties.getProperty (
                        "ru.inversion.jdbc_driver",
                        ()->appProperties.getProperty (
                            "ru.inversion.vendor_db",
                            ()->dbUrl
                        )
                    );
            }
            return null;
        });

        if( appProperties.getBooleanProperty( PRP_CHECK_XXI_UPGRADE_DISABLE, false ) )
            dscb.property( PRP_CHECK_XXI_UPGRADE_DISABLE, "true" );

        if( additional != null && !additional.isEmpty() )
            additional.forEach( (k,v)->dscb.property(k,v) );

        dscb.property("check_connection", true );

        DataSourceCfg dsc = dscb.login( login )
                                .password     ( password )
                                .dbUrl        ( dbUrl    )
                                .usePseudoConnection( pseudoConnection)
                                .resultHandler( resultHandler   )
                             .build( );

        SessionEnvironment.initialize( dsc );

        if( hcnsmr.isPresent() )
            BaseApp.APP().addAfterLoginCallback( hcnsmr.get(), 0 );

    }

    /*
    private static void showModalLoginMessage( ViewContext vc, final String o ) {
        //final List<Stage>[] stages = new List[]{new ArrayList<>()};
        Timeline timeline = new Timeline();
        timeline.setCycleCount( Timeline.INDEFINITE );

        final KeyFrame kf = new KeyFrame(
            Duration.millis(10),
            e -> {
                //stages[0] = StageHelper.getStages();
                //if ( !stages[0].isEmpty() )
                {
                    timeline.stop();


            Window window = stages[0].get( 0 ).getScene().getWindow();
                    new AlertBuilder( vc, Alert.AlertType.INFORMATION)
                            .title( fore.getString( "LOGIN_HEADER" ) )
                            .headerText( o )
                            .build()
                            .show();

                    g_logger.debug( "modal login message timeline is stopped" );
                }
            }
        );

        timeline.getKeyFrames().add(kf);
        timeline.play();
        g_logger.debug( "modal login message timeline is started" );
    }
    */

    /** */
    private static String[] old_getTokenData( ) {

        try {

            String ret[] = new String[3];

            ret[0] = "XXI logon check data of " + LocalDateTime.now();

            Date        now = new Date();
            ETokenInvo invo = new ETokenInvo();

            ArrayList<byte[]> list = new ArrayList<>();

            if( !invo.init(list) ) {
                //System.out.println("INFO ERROR " + invo.getErrorMessage() );
                throw new SecurityException( invo.getErrorMessage() );
            }//end if

            // System.out.println("The number of certificates found is " + list.size());

            boolean expired = false;

            for( byte[] certificateData : list )
            {
                expired = false;

                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate= (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certificateData) );

                String serialNumber = certificate.getSerialNumber().toString(16);

                //System.out.println("Serial " + serialNumber);

                if( now.after(certificate.getNotBefore()) && now.before(certificate.getNotAfter()) )
                {
                    StringWriter sw = new StringWriter();

                    boolean result = invo.sign( ret[0], serialNumber, sw );

                    if( !result )
                        throw new SecurityException( invo.getErrorMessage() );

                    ret[1] = serialNumber;
                    ret[2] = sw.toString();
                }
                else
                {
                    expired = true;
                }
            }//end for

            if( expired )
                throw new SecurityException( Tags.PRODUCT_LABEL + "Certificate has expired or has not yet expired" );

            return ret;

        } catch (CertificateException e) {
            throw new SecurityException( Tags.PRODUCT_LABEL + "Error on init certdata", e );
        }
    }

    /** */
    private static String getTokenData( ) {

        try {

            //DCont dcRet = new MDom().e("request");
            final IDco dcRet = new Dco("request");

            String signData = "XXI logon check data of " + LocalDateTime.now();

            Date        now = new Date();
            ETokenInvo invo = new ETokenInvo();

            ArrayList<byte[]> list = new ArrayList<>();

            if( !invo.init(list) ) {
                throw new SecurityException( invo.getErrorMessage() );
            }//end if

            if( list == null || list.isEmpty() )
                throw new AlertException( Tags.PRODUCT_LABEL + fore.getString("NO_TOKEN") );

            // System.out.println("The number of certificates found is " + list.size());

            boolean expired = false;

            for( byte[] certificateData : list )
            {
                expired = false;

                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                X509Certificate certificate= (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certificateData) );

                String serialNumber = certificate.getSerialNumber().toString(16);

                //System.out.println("Serial " + serialNumber);

                if( now.after(certificate.getNotBefore()) && now.before(certificate.getNotAfter()) )
                {
                    StringWriter sw = new StringWriter();
                    boolean result = invo.sign( signData, serialNumber, sw );

                    if( !result )
                        throw new SecurityException( invo.getErrorMessage() );

                    dcRet.e("data").value( signData );
                    dcRet.e("signature").value( sw.toString() );

                    {
                        sw = new StringWriter();
                        final Base64.Encoder encoder = Base64.getEncoder( );
                        sw.write( X509Factory.BEGIN_CERT );
                        sw.write( '\n' );
                        sw.write( encoder.encodeToString( certificate.getEncoded() ) );
                        sw.write( X509Factory.END_CERT   );

                        dcRet.e("certificate").value( sw.toString() );
                    }

                    // Сохраняем сертификат в свойства приложения!
                    BaseApp.APP().getProperties(PRP).setProperty( "ru.inversion.crtf", certificate );

                    break;
                }
                else
                {
                    expired = true;
                }
            }//end for

            if( expired )
                throw new SecurityException( Tags.PRODUCT_LABEL + "Certificate has expired" );

            return "<?xml version=\"1.0\"?>\n" + dcRet.asXml();

        } catch ( CertificateException e ) {
            throw new SecurityException( Tags.PRODUCT_LABEL + "Error on init certdata", e );
        }
    }
    /** */
    public static boolean login( Stage stage ) {

        final IAppProperties appProperties = APP().getProperties(PRP);

        String login    = null,
               password = null,
               db       = null,
               middleDrvServer = null;

        boolean pseudoConnection = appProperties.getBooleanProperty( PSEUDO_CONNECTION, true ),
                hasLoginData     = false;

        final Map<String,String> additional = new HashMap<>();

        final Boolean useToken = appProperties.getBooleanProperty( PRP_USE_TOKEN_4_LOGIN, false );

        if( useToken ) {

            final String tokenData = getTokenData( );
            login    = tokenData;
            password = "<none>";

            db       = appProperties.getStringProperty( DB );

            if( S.isNullOrEmpty(db) )
                throw new IllegalStateException( Tags.PRODUCT_LABEL + "DB name must be set!");

//            additional.put( "ru.inversion.token.string_check",  tokenData[0] );
//            additional.put( "ru.inversion.token.string_signed", tokenData[2] );
            additional.put( PRP_USE_TOKEN_4_LOGIN, "true" );

            hasLoginData = true;
        }
        else
        {
            String userID = appProperties.getStringProperty(USER_ID);

            if(!S.isNullOrEmpty(userID) && userID.length() > 3) {

                switch(userID.charAt(0)) {
                    case 'T':
                    case 't':
                        pseudoConnection = true;
                        hasLoginData = true;
                        break;
                    case 'F':
                    case 'f':
                        pseudoConnection = false;
                        hasLoginData = true;
                        break;
                }

                if( hasLoginData ) {

                    hasLoginData = false;

                    userID = userID.substring(1);

                    try {

                        String connectString = new String( DatatypeConverter.parseBase64Binary(userID) );

                        int i1 = connectString.indexOf("/");
                        int i2 = connectString.lastIndexOf("@");

                        if(i1 > 0 && i2 > 0) {

                            login = connectString.substring(0, i1);
                            password = connectString.substring(i1 + 1, i2);
                            db = connectString.substring(i2 + 1);

                            hasLoginData = !login.isEmpty() && !password.isEmpty() && !db.isEmpty();
                        }

                    } catch(Exception ex) {
                        g_logger.error( Tags.PRODUCT_LABEL + "Error on parse userID: ", ex );
                    }
                }//end if

            }//end if (userID != null )

        }//end if useToken

        if( !hasLoginData ) {

            login    = appProperties.getStringProperty( LOGIN    );
            password = appProperties.getStringProperty( PASSWORD );
            db       = appProperties.getStringProperty( DB       );

            if( login == null || password == null || db == null )
                ;
            else
            {
                hasLoginData     = true;
                pseudoConnection = appProperties.getBooleanProperty( PSEUDO_CONNECTION, true );
            }
        }

        middleDrvServer = appProperties.getStringProperty( MIDDLE_DRV_SERVER );

        if( middleDrvServer != null )
        {
            middleDrvServer = middleDrvServer.trim();

            if( middleDrvServer.isEmpty() )
                middleDrvServer = null;
        }

        if( hasLoginData )
        {
            try {

                g_logger.info( Tags.PRODUCT_LABEL +
                              "login by ..." +
                              "\n  user        : " + login +
                              "\n  password    : " + S.space( password.length(), '*') +
                              "\n  db_url      : " + db +
                              "\n  pseudo      : " + pseudoConnection +
                              "\n  middle      : " + ( middleDrvServer != null ? "on, " : "off" ) + ( middleDrvServer == null ? "" : "url : " + middleDrvServer ) +
                              "\n  token       : " + useToken +
                              "\n  auth_server : " + appProperties.getStringProperty("auth_server", "<none>")
                );

                doLogin( login, password, db, pseudoConnection, middleDrvServer, additional );

                g_logger.info("  ... ok" );

                return true;

            } catch (Throwable th) {

                JInvErrorService.handleException(stage, th);

                g_logger.error(" ... fail" );
                g_logger.error("Error on login by command line: ", th);
            }
        }

        if( !useToken )
            return ( new LoginDialog() ).doModal( );

        return false;
    }

    /** */
    public static boolean hasLicense( TaskContext tc, Integer ssccode ) {

        try( CallableStatement st = tc.getConnection().prepareCall( "{? = call QTRN.Get_Bank_Name(?, ?, ?)}" ) )
        {
            if( ssccode == null )
                ssccode = 0;

            st.registerOutParameter( 1, Types.VARCHAR );
            st.setObject( 2, ssccode );
            st.setObject( 3, null );
            st.setObject( 4, null );

            st.execute( );

            return st.getString(1) != null;

        } catch( Exception ex ) {
            g_logger.error("error on get info about license", ex );
        }
        return false;
    }

    /** */
    public static void checkLicense( TaskContext tc, Integer ssccode ) throws LoginException {

        String  result = null;

        Connection con = tc.getConnection( );

        try( CallableStatement st = con.prepareCall( "{? = call QTRN.Get_Bank_Name(?, ?, ?)}" ) )
        {
            if( ssccode == null )
                ssccode = 0;

            st.registerOutParameter( 1, Types.VARCHAR );
            st.setObject( 2, ssccode );
            st.setObject( 3, null );
            st.setObject( 4, null );

            st.execute( );

            result = st.getString(1);

        } catch( Exception ex ) {
            g_logger.error("error on get info about license", ex );
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on get info about license.", ex );
        }

        if( result == null ) {

            String exInfo   = null;

            try( CallableStatement st = con.prepareCall( "{? = call QTRN.Q_LicInfo()}" ) )
            {
                st.registerOutParameter( 1, Types.VARCHAR );
                st.execute( );

                exInfo = st.getString(1);

            } catch( Exception ex ) {
                g_logger.error("error on get 'Q_LicInfo'", ex );
            }

            LoginException.throwNoLicence( ssccode, exInfo );
        }
    }
}
