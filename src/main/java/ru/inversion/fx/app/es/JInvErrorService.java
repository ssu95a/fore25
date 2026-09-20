package ru.inversion.fx.app.es;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.db.session.xxi.XXIConnectorException;
import ru.inversion.fx.app.AlertException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.AlertBuilder;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.IExceptionInfo;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import javax.persistence.LockTimeoutException;
import javax.persistence.PersistenceException;
import java.io.IOException;
import java.net.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author ssu @
 */
public class JInvErrorService {

	public static final Logger logger = LoggerFactory.getLogger(JInvErrorService.class);

    /**
     * Параметры отправки сообщения об ошибке.
     */
    public interface IErrorSendProperties {
        /** */
        boolean isMakeScreenShort( );
        /** */
        boolean isAttachLog( );
        /** */
        String getSubject( );
        /** */
        String getInfo( );
        /** */
        String getRecipient( );
        /** */
        String getSender( );
    }

    /** интерфейс отправки сообщения об ошибке */
    public interface IErrorMailSender {
        void send( Throwable ex, IErrorSendProperties properties );
    }

    static final private String
    g_lockSQLInfo =
            "SELECT 'Таблица ' || object_name ||' заблокирована. User \"' || se.username || '\", os user \"' || se.osuser || '\", machine \"' || se.machine  || '\"' info\n" +
            " FROM gv$locked_object lo, gv$session se, dba_objects ob\n" +
            "WHERE se.SID = lo.session_id\n" +
            "  AND lo.inst_id = se.inst_id\n" +
            "  AND lo.object_id = ob.object_id\n" +
            "  AND owner = 'XXI'\n" +
            "  AND object_name = ?\n" +
            "  AND ROWNUM = 1";

    /** ChildRecordFoundWrapException */
    static private class CRFException extends PersistenceException implements IExceptionInfo {

        final private String content;

        public CRFException( String message, String content, Throwable cause ) {
            super( message, cause );
            this.content = content;
        }

        @Override
        public String getCategory() { return "dbError";}

        @Override
        public String getContentText() { return content; }
    }

    final static private Pattern crfPattern = Pattern.compile("\\(\\w+.\\w+\\)");

    /*
      if tStr='02292' then
      tRet:='Удаление запрещено!'||chr(10)||'Существует дочерняя запись!';
      if tpos1>0 then
         begin
          SELECT TABLE_NAME into tTABLE_NAME
          FROM SYS.ALL_CONSTRAINTS
          where owner='XXI' and constraint_type='R' and constraint_name=tBuf;

          tRet:='Удаление запрещено!'||chr(10)||'Существует дочерняя запись в таблице <'||tTABLE_NAME||'>';

          select substr(comments,1,512) into tStr
          from sys.ALL_TAB_COMMENTS
          where owner='XXI'and table_name=tTABLE_NAME;

          if tStr is not null then
             tRet:=tRet||' ('||tStr||')!';
          else
             tRet:=tRet||'!';
          end if;
          tpos1:=instr(tBuf,'TO');
          if tpos1 in (4,5) then
             tRet:='Запрещено удаление из таблицы /'||substr(tBuf,1,tpos1-1)||'/!'||chr(10)||
                   'Существует дочерняя запись в таблице <'||tTABLE_NAME||'> /'||substr(tBuf,tpos1+2,4)||'/ ('||tStr||')!';
          end if;
         exception
           when others then
             null;
         end;
      end if;
    */

    /** */
    static class OraUserDefException extends Exception implements IExceptionInfo {
        public OraUserDefException( String message, Throwable cause ) {
            super( message, cause, true, false);
        }
        @Override
        public String getCategory() {
            return "dbError";
        }
    }

    //final static private Pattern msgPattern = Pattern.compile("^(ORA-\\d+:\\s*)(.*)");
    final static private Pattern msgPattern = Pattern.compile("ORA-\\d+:\\s*");

    /** */
    static private Throwable handleOraUserDefException( Throwable th ) {

        try {

            SQLException ex = findThrowable(th, SQLException.class);

            if( ex == null || ex.getErrorCode() < 20000 )
                return th;

            String msg = null;

            try {

                String sa[] = msgPattern.split( ex.getLocalizedMessage() );

                for( String s : sa ) {
                    if( S.isNotNullOrEmpty(s) ) {
                        msg = s;
                        break;
                    }
                }

                if( S.isNullOrEmpty(msg) )
                    msg = ex.getLocalizedMessage();
            }
            catch( Throwable th2 ) {
                msg = ex.getLocalizedMessage();
            }

            return new OraUserDefException( msg, th );
        }
        catch(Throwable th1 ) {
            th1.printStackTrace();
            return th;
        }
    }

    static final private String
        g_lockSQL02292 =
            "SELECT 'Удаление запрещено! Существует дочерняя запись в таблице\"' || a.TABLE_NAME ||'\"' msg,\n" +
            "'Информация о таблице: ' || chr(10) || ( select comments from sys.ALL_TAB_COMMENTS b where b.owner= a.owner and b.table_name=a.table_name ) cmt\n" +
            " FROM SYS.ALL_CONSTRAINTS a\n" +
            "WHERE a.owner = ? and a.constraint_type='R' and a.constraint_name=?";
    /** */
    static private Throwable handleChildRecordFoundException( Throwable th ) {

        try {

            SQLException ex = findThrowable(th, SQLException.class);

            if( ex == null || ex.getErrorCode() != 2292 )
                return th;

            final String msg = ex.getMessage();

            Matcher m = crfPattern.matcher(msg);

            if( !m.find() )
                return th;

            String s    = m.group().trim();
                   s    = s.substring( 1, s.length() - 1 );
            String sa[] = s.split("\\.");

            final TaskContext tc = BaseApp.APP().getCommonTaskContext();

            try( PreparedStatement ps = tc.getConnection().prepareStatement(g_lockSQL02292) )
            {
                ps.setString( 1, sa[0] );
                ps.setString( 2, sa[1] );

                try( ResultSet rs = ps.executeQuery() )
                {
                    if( rs.next() ) {

                        String ms = rs.getString(1);
                        String dt = rs.getString(2);

                        return new CRFException( ms, dt, th );
                    }
                }
            }

            return th;
        }
        catch( Throwable th1 ) {
            th1.printStackTrace();
            return th;
        }
    }

    /** */
    static private boolean handleLockTimeoutException( Window parentWindow, Throwable th )
    {
        LockTimeoutException ltex = null;

        while( th != null ) {

            if( th instanceof LockTimeoutException ) {

                ltex = (LockTimeoutException)th;

                if( ltex.getObject() == null || !( ltex.getObject() instanceof Object[] ) )
                    ltex = null;
                else
                    break;
            }

            th = th.getCause();
        }

        if( ltex == null )
            return false;

        try {

            final Object[] data    = (Object[])ltex.getObject();
            final String tableName = U.callIfNotNull( (String)data[0], String::toUpperCase );

            if( tableName != null )
            {
                String lockMessage = null;

                final TaskContext tc = BaseApp.APP().getCommonTaskContext();

                try( PreparedStatement ps = tc.getConnection().prepareStatement(g_lockSQLInfo) ) {

                     ps.setString( 1, tableName );

                     try( ResultSet rs = ps.executeQuery() )
                     {
                         if( rs.next() )
                             lockMessage = rs.getString(1);
                     }
                }

                if( lockMessage != null )
                {
                    final String s = lockMessage;

                    Platform.runLater( ()->
                            Alerts.error (
                                    parentWindow,
                                    null,
                                    "Запись заблокирована",
                                    s,
                                    null
                            )
                    );

                    return true;
                }
            }
        }
        catch( Throwable th1 ) {
            printDetailMsg( true, th1 );
        }

        return false;
    }

    /**
    * @author Shchapov
    * Обработка исключений ConnectUrlException. Проверяем доступность сервиса через ping
    * @param th Исключение для обработки
    * @return null если th не обработали(не нашли при обходе цепочки причин исключение ConnectUrlException)
    * */
    static private Throwable handleConnectUrlException( Throwable th ) {

        ConnectUrlException cEx = findThrowable( th, ConnectUrlException.class );

        if( cEx == null )
            return th;

        if( S.isNullOrEmpty( cEx.getUrl() ) )
            return cEx;

        String url = cEx.getUrl();

        try( Socket socket = new Socket() ) {
            URI uri = new URI(url);
            socket.connect(new InetSocketAddress(uri.getHost(), uri.getPort()), 1000);
            socket.close();
            return new ConnectUrlException("Сервис (" + url + ") доступен, но соединение не удалось!");
        } catch (IOException e) {
            return th;
        } catch (URISyntaxException e) {
            return new ConnectUrlException("Некорректный URL: " + url);
        }
    }

    /**
    * @author Shchapov
    * Найти thClass среди cause у th
    * @param th Исключение, у которого будем искать необходимое исключение среди cause
    * @param thClass класс исключения, которого нужно найти среди cause у th
    * @return null если thClass не нашли при обходе цепочки причин исключение у th
    * */
    static public <T extends Throwable> T findThrowable( Throwable th, Class<T> thClass ) {
        T result = null;

        while (th != null) {
            if (thClass.isInstance(th)) {
                result = thClass.cast(th);
                break;
            }
            th = th.getCause();
        }

        return result;
    }

    /** */
    static private boolean handleLoginException( Object vc, Throwable ex )
    {
        if( ex instanceof XXIConnectorException) {

            if( ex.getCause() instanceof SQLException )
                ex = ex.getCause();
            else
            {
                Alerts.error (
                    vc,
                    ResourceBundle.getBundle("fore").getString("LOGIN_HEADER"),
                    ex.getLocalizedMessage(),
                    null
                );
                return true;
            }
        }

        if( ex instanceof SQLException )
        {
            SQLException sqlEx = (SQLException)ex;

            if( sqlEx.getErrorCode() == 1017 || "28P01".equals( sqlEx.getSQLState() )) {
                Alerts.error(
                        vc,
                        null,
                        sqlEx.getLocalizedMessage(),
                        sqlEx.getLocalizedMessage().contains("XXI_LOGON")
                                ?
                                S.EMPTY_STRING
                                :
                                ResourceBundle.getBundle("fore").getString("INCORRECT_LOGIN_CONTEXT_TEXT")
                );
//return
                return true;
            }

            if( S.isNotNullOrEmpty( sqlEx.getMessage() ) &&  sqlEx.getMessage().startsWith("[Xxi-Connector]") ) {

                Alerts.error (
                        vc,
                        ResourceBundle.getBundle("fore").getString("LOGIN_HEADER"),
                        sqlEx.getLocalizedMessage(),
                        null
                );
//return
                return true;

            }
        }
        return false;
    }


    /** */
    static public void handleException( Object vc, Throwable ex) {

        Window parent = ViewContext.tryGetWindow(vc);

        if( handleLoginException(vc,ex) )
            return;

//Для случая со старой версией пакета для доставания пропертей
        if( ex instanceof SQLExpressionException )
        {
//            Throwable cause = ex.getCause().getCause().getCause();
            Throwable cause = ex;
            for ( int i = 0; i < 3; ++i ) {
                if ( cause != null ){
                    cause = cause.getCause();
                }
            }
            if ( cause instanceof SQLException && ( (SQLException) cause ).getErrorCode() == 6576 )
            {
                Alerts.error(
                    parent,
                    null,
                    cause.getLocalizedMessage()
                );
//return
                return;
            }
        }

// Не всех exception нужно видеть внутренности
        if( ex instanceof AlertException ) {

            String msg = ex.getLocalizedMessage();

            if( S.isNullOrEmpty(msg) ) {
                if( ex.getCause( ) == null )
                    msg = Tags.PRODUCT_LABEL + "No name alert error";
            }

            if( !S.isNullOrEmpty(msg) ) {
                final String s = msg;
                Platform.runLater( ()->
                    new AlertBuilder()
                        .alertType(Alert.AlertType.ERROR)
                        .windowContainer(vc)
//                        .title(fore.getString("SOOBSHCHENIE_OB_OSHIBKE"))
                        .headerText(s)
                        .modality(Modality.APPLICATION_MODAL)
                        .build()
                        .showAndWait()
                );
            }
            else
            {
                Throwable bex = ex.getCause();

                if (bex != null){
                    if( bex instanceof IExceptionInfo ) {

                        final IExceptionInfo iex = (IExceptionInfo)bex;

                        Platform.runLater( ()->
                            new AlertBuilder()
                                .alertType(Alert.AlertType.ERROR)
                                .windowContainer(vc)
//                                .title(fore.getString("SOOBSHCHENIE_OB_OSHIBKE"))
                                .headerText(iex.getHeaderText())
                                .contentText(iex.getContentText())
                                .detailText(iex.getDetailedMessage())
                                .modality(Modality.APPLICATION_MODAL)
                                .build()
                                .showAndWait()
                        );
                    }
                    else
                        Platform.runLater( ()->
                            new AlertBuilder()
                                .alertType(Alert.AlertType.ERROR)
                                .windowContainer(vc)
//                                .title(fore.getString("SOOBSHCHENIE_OB_OSHIBKE"))
                                .headerText(bex.getLocalizedMessage())
                                .modality(Modality.APPLICATION_MODAL)
                                .build()
                                .showAndWait()
                        );
                }

            }
//return
            return;
        }

        if( handleLockTimeoutException( parent, ex ) )
//return
            return;

        Throwable th = handleOraUserDefException(ex);
        ex = ( th == ex ) ? handleChildRecordFoundException(ex) : th;
        ex = ( th == ex ) ? handleConnectUrlException(ex) : th;

        if( ex instanceof IExceptionInfo) {
            printDetailMsg(true, ex);
        } else {
            logger.error("ErrorDialog: ", ex);
        }

        if( Platform.isFxApplicationThread() )
           new DialogError(parent, ex).showAndWait();
        else {
            final Window parent2 = parent;
            final Throwable th1 = ex;
            Platform.runLater( ()-> new DialogError( parent2, th1 ).showAndWait() );
        }
	}

    /** */
    static public SQLException getInitialSQLException1( Throwable th ) {

        SQLException ex = null;

        while( th != null ) {

            if( th instanceof SQLException )
                ex = (SQLException)th;

            th = th.getCause();
        }

        return ex;
    }

    /**
     * Печать деталей сообщений в лог.
     * <p>
     * Первое сообщение печатаем полностью, для остальных сообщений печатаем детали сообщения.
     * @param first флаг для печати первого сообщения полностью
     * @param ex
     */
    private static void printDetailMsg( boolean first, Throwable ex )
    {
        String text = ex.getMessage();

        if( ex instanceof IExceptionInfo )
        {
            IExceptionInfo iei = (IExceptionInfo)ex;

            if( iei.getDetailedMessage() != null && !iei.getDetailedMessage().isEmpty() )
            {
                text = text + " \n " + iei.getDetailedMessage();
            }

            if( first ) {
                logger.error("ErrorDialog: " + text, ex);
            }
            else {
                logger.error("ErrorDialog: " +  text );
            }
        }

        if( ex.getCause() != null )
            printDetailMsg( false, ex.getCause() );
    }

}