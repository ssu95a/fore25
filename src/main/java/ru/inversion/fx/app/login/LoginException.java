package ru.inversion.fx.app.login;

import ru.inversion.fx.app.AlertException;
import ru.inversion.fx.app.BaseApp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * @author ssu @
 */
public class LoginException extends Exception {

    public enum Reason {
        ERROR,
        NO_TNS_NAMES_FOUND,
        NO_LICENSE,
        NEED_CHANGE_PASSWORD,
    }

    final private Reason reason;

    /** */
    public LoginException( ) {
        this.reason = Reason.ERROR;
    }

    /** */
    public LoginException( Reason r ) {
        super( r.toString() ); this.reason = r;
    }
    
    /** */
    public LoginException( String message ) {
        super(message); reason = Reason.ERROR;
    }

    /** */
    public LoginException(String message, Throwable cause) {
        super(message, cause);
        reason = Reason.ERROR;
    }

    /** */
    public LoginException( Throwable cause ) {
        super(cause);
        reason = Reason.ERROR;
    }

    public Reason getReason() {
        return reason;
    }

    /** */
    public static void throwNoLicence( int sscode, String licInfo ) throws LoginException {

        String cSscName = null;

        if( sscode != 0 )
        {
            try( PreparedStatement ps = BaseApp.APP().getCommonTaskContext().getConnection().prepareStatement("SELECT csscname FROM ssc where ISSCCODE = ?") ) {
                ps.setInt( 1, sscode );
                try( ResultSet rs = ps.executeQuery() ) {
                    if( rs.next() )
                        cSscName = rs.getString(1);
                }
            }
            catch( Exception ignored ) {
            }
        }

        final String   s = ResourceBundle.getBundle("fore").getString("NO_LICENSE_EX");
        final String msg = MessageFormat.format( s, cSscName, sscode, licInfo );

        throw new AlertException( msg );
    }
}
