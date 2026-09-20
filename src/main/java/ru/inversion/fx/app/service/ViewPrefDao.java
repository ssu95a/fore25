package ru.inversion.fx.app.service;
import org.slf4j.Logger;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.dataset.ParametersByIndex;
import ru.inversion.db.rs.RSUtils;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
//import ru.inversion.priv.tools.dcont.DCont;
//import ru.inversion.priv.tools.mdom.MDom;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.U;
import ru.inversion.utils.dco.Dco;
import ru.inversion.utils.dco.IDco;

import java.sql.Connection;
import java.util.List;
import java.util.ResourceBundle;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Сервис сохранения настроек параметров UI компонентов в БД.
 * <p>
 * @author sulimoff
 */
public class ViewPrefDao {

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    private final static Logger logger = getLogger( lookup().lookupClass() );

    public static void old_savePreferences( List<PPrefComponent> prefList ) throws AppException {

        if( prefList == null || prefList.isEmpty() )
            return;

        final TaskContext taskContext = BaseApp.APP().getCommonTaskContext();

        try {

            SQLCallBuilder.NEW( taskContext )
                    .name( "saveSettings" )
                    .url ( lookup().lookupClass().getClassLoader().getResource( "ru/inversion/fx/app/service/plsql/def.xml" ) )
                    .build()
                    .set("p_formname", prefList.get(0).getFORM_NAME() )
                    .set("p_settings", prefList )
                    .execute();

            taskContext.commit();

        } catch( Throwable th ) {
            taskContext.rollback();
            throw new AppException( "Error on save UI preferences", th );
        }
    }

    /** */
    public static void savePreferences( List<PPrefComponent> prefList ) throws AppException {

        if( prefList == null || prefList.isEmpty() )
            return;

        final TaskContext taskContext = BaseApp.APP().getCommonTaskContext();

        try {

            //final DCont dc = new MDom().e("prefs");
            IDco dco = new Dco("prefs");

            prefList.forEach(
                    p-> {
                        final IDco dcPref = dco.append("pref");
                        dcPref.a("form_name" ).set( p.getFORM_NAME() );
                        dcPref.a("component" ).set( p.getCOMPONENT() );
                        dcPref.a("element"   ).set( p.getELEMENT()   );
                        dcPref.a("visible"   ).set( p.getVISIBLE()   );
                        dcPref.a("height"    ).set( p.getHEIGHT()    );
                        dcPref.a("width"     ).set( p.getWIDTH()     );
                        dcPref.a("orderby"   ).set( p.getORDBY()     );
                        dcPref.a("serial_uid").set( p.getSERIAL_UID());
                    }
            );

            final String xml = dco.asXml();

            //logger.debug(xml);

            SQLCallBuilder.NEW( taskContext )
                          .name( "saveSettingsX" )
                          .url ( lookup().lookupClass().getClassLoader().getResource( "ru/inversion/fx/app/service/plsql/def.xml" ) )
                    .build()
                          .set ( "p_formname", prefList.get(0).getFORM_NAME() )
                          .set ( "p_settings", xml )
                    .execute();

            taskContext.commit();

        } catch( Throwable th ) {
            taskContext.rollback();
            throw new AppException( "Error on save UI preferences", th );
        }
    }

    /** */
    public static List<PPrefComponent> loadPreferences( String formName ) throws AppException {

        try {

            final Connection c = BaseApp.APP().getCommonTaskContext().getConnection();

            return U.toList (
                RSUtils.createIterable (c,
                        PPrefComponent.class,
                        "form_name=?", //"iusrid = ~to_number(SYS_CONTEXT('B21','IDUsr'))~ and form_name=?",
                        null, (ParametersByIndex) (int parameterIndex) -> formName )
            );

        } catch( Throwable th ) {
            throw new AppException( "Error on load UI preferences for form '" + formName + "'", th );
        }
    }

    /** */
    public static List<PPrefComponent> loadPreferences( String formName, String componentName, List<String> elements ) throws AppException {

        try {

            final Connection c = BaseApp.APP().getCommonTaskContext().getConnection();

            //String strWhere = "iusrid = ~to_number(SYS_CONTEXT('B21','IDUsr'))~ and form_name=? and COMPONENT=?";
            String strWhere = "form_name=? and component=?";

            if( elements != null && !elements.isEmpty()  ) {

                StringBuilder sb = new StringBuilder(strWhere);

                sb.append(" and element in (");

                for( String s : elements )
                     sb.append("'").append( s ).append("',");

                sb.setCharAt( sb.length() - 1, ')' );

                strWhere = sb.toString();
            }

            return U.toList (
                    RSUtils.createIterable (c,
                            PPrefComponent.class,
                            strWhere,
                            null, (ParametersByIndex)
                                           (int parameterIndex) -> U.<Integer,String>decode(
                                                   parameterIndex,
                                                   0, formName,
                                                   1, componentName )
                    )
            );

        } catch( Throwable th ) {
            throw new AppException( "Error on load UI preferences for form '" + formName + "'", th );
        }
    }
}
