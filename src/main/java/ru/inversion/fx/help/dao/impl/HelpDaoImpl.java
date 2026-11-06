package ru.inversion.fx.help.dao.impl;

import ru.inversion.db.expr.SQLExpressionFactory;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.help.dao.HelpDao;
import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.utils.U;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static java.lang.invoke.MethodHandles.lookup;

/**
 @author perov */
public class HelpDaoImpl implements HelpDao {

    private static final String PLSQL_XML_PATH = "ru/inversion/fx/help/plsql/def.xml";

    private static Map<String, Object> getNotNullParameters( Map<String, Object> parameters ) {
        Map<String, Object> map = new HashMap<>();
        for ( String p : parameters.keySet() ) {
            if ( parameters.get( p ) == null ) {
                map.put( p, "" );
            } else {
                map.put( p, parameters.get( p ) );
            }
        }
        return map;
    }

    @Override
    public void insertHelpForm( Connection con, PHelp entity ) throws SQLException {
        mergeHelpForm( con, entity, null );
    }

    private void mergeHelpForm( Connection con, PHelp entity, String oldFormValue ) throws SQLException {
        Map<String, Object> delParams = new HashMap<>();
        delParams.put( "oldFORM", oldFormValue );
        delParams.put( "newFORM", entity.getFORM() );
        delParams.put( "CLOCALE", entity.getCLOCALE() );
        execute( con, entity, "help.delete.conditional",
                /*oldFormValue, oldFormValue, entity.getFORM(), oldFormValue*/
                delParams);
        if ( entity.getHTML_TEXT() == null ) {
            entity.setHTML_TEXT( "HTML help for " + entity.getFORM() );
        }
        Map<String, Object> mergeParams = new HashMap<>();
        mergeParams.put( "FORM", entity.getFORM() );
        mergeParams.put( "DESCR", entity.getDESCR() );
        mergeParams.put( "CLOCALE", entity.getCLOCALE() );
        mergeParams.put( "VER", entity.getVER() );
        mergeParams.put( "HTML_TEXT", new StringReader(  entity.getHTML_TEXT() ));
        execute( con, entity, "help.merge", mergeParams );
    }

    private static void execute( Connection con, PHelp entity, String procName, Map<String, Object> parameters ) throws SQLException {
        if ( U.containsNull( con, entity, entity.getFORM() ) ) {
            return;
        }
        Map<String, Object> safeParams = getNotNullParameters(parameters);
        try {
            SQLExpressionFactory.INSTANCE()
                    .execute( lookup().lookupClass()
                            .getClassLoader()
                            .getResource( PLSQL_XML_PATH ), procName, con, safeParams );
            con.commit();
        } catch ( Throwable ex ) {
            con.rollback();
            JInvErrorService.handleException( null, ex );
        }
    }

    @Override
    public void updateHelpForm( Connection con, PHelp entity, String oldFormValue ) throws SQLException {
        mergeHelpForm( con, entity, oldFormValue );
    }

    @Override
    public void deleteHelpForm( Connection con, PHelp entity ) throws SQLException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put( "FORM", entity.getFORM() );
        parameters.put( "CLOCALE", entity.getCLOCALE() );
        execute( con, entity, "help.delete", parameters );
    }
}
