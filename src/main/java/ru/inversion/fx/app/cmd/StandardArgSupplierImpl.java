package ru.inversion.fx.app.cmd;

import ru.inversion.fx.app.BaseApp;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;

import java.util.Base64;
import java.util.function.Function;

import static ru.inversion.fx.app.property.PropertiesTypeEnum.PRP;
import static ru.inversion.utils.ConnectionStringFormatEnum.*;

/** */
public class StandardArgSupplierImpl implements Function<Object,Object> {

    private String getXxiUserId( TaskContext tc) {
        String userId = BaseApp.APP().getProperties( PRP ).getStringProperty( "ru.inversion.userid" );
        if( S.isNullOrEmpty( userId ) )
        {
            String cs = tc.getConnectionString( SQL_PLUS );
            userId = 'F' + Base64.getEncoder().encodeToString( cs.getBytes() );
        }
        return userId;
    }


    @Override
    public Object apply( Object arg ) {

        if( arg == null )
            return null;

        Object value = null;

        final TaskContext tc = BaseApp.APP().getCommonTaskContext();

        StandardArgEnum sae = null;
        if( arg instanceof StandardArgEnum )
            sae = (StandardArgEnum)arg;
        else
            if( arg instanceof String )
            {
                // sae = StandardArgEnum.valueOf( arg.toString() );
                for( StandardArgEnum e : StandardArgEnum.values() )
                {
                    if( e.name().equals( arg.toString() ) )
                    {
                        sae = e;
                        break;
                    }//end for
                }//end for
            }

        if( sae != null )
        {
            switch( sae ) {
                case USER_LOGIN:
                    value = tc.getUserName();
                    break;
                case USER_PASSWORD:
                    value = tc.getConnectionString(SQL_PLUS).split("/|@")[1];
                    break;
                case USERID:
                    value = getXxiUserId(tc);
                    break;
                case DB_ALIAS:
                    value = tc.getConnectionString(SQL_DB_ALIS);
                    break;
                case TNS_NAMES:
                    value = System.getProperty("oracle.net.tns_admin");
                    break;
            }
        }
        else
        {
            // stub
        }

        return value;
    }
}
