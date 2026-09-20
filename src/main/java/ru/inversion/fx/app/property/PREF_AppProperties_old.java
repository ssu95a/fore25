package ru.inversion.fx.app.property;

import ru.inversion.utils.Pair;
import org.slf4j.Logger;
import ru.inversion.datacall.IDataCall;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.dataset.ParametersByName;
import ru.inversion.db.expr.SQLExpressionFactory;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.IDBTools;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.frame.menu.IMenuLoader;
import ru.inversion.fx.app.frame.menu.PropertyItemEnum;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.xxi.impl.XXIMenuLoader;

import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.*;

/**
 @author ssu @ */
public class PREF_AppProperties_old extends AbstractAppProperties {

    private static final ResourceBundle fore = ResourceBundle.getBundle( "fore" );

    private static final Map<PropertiesTypeEnum, Properties> staticPrefs = new TreeMap<>();

    final static URL PLSQL_XML = lookup().lookupClass().getClassLoader().getResource( "ru/inversion/fx/app/property/plsql/def.xml" );
    //private static Logger g_logger     = getLogger(lookup().lookupClass());
    private static IDBTools dbTools;
    final private PropertiesTypeEnum propertiesType;

    public PREF_AppProperties_old( BaseApp app ) {
        this( app, DB_USER );
    }

    public PREF_AppProperties_old( BaseApp app, PropertiesTypeEnum propertiesType ) {
        super( app );
        checkDbToolsInit();
        this.propertiesType = propertiesType;
    }

    public static IDBTools getDBTools() {
        checkDbToolsInit();
        return dbTools;
    }

    private static void checkDbToolsInit() {
        if ( dbTools == null && BaseApp.APP().isAfterLogin() ) {
            try {
                dbTools = XXITools.INSTANCE();
                if ( dbTools != null ) {
                    initAfterLoginPrefs();
                }
            } catch ( Throwable th ) {
                throw new RuntimeException( Tags.PRODUCT_LABEL + fore.getString( "ERROR_ON_INIT_DBTOOLS" ), th );
            }
        }
    }

    private static void initAfterLoginPrefs() {
        staticPrefs.putAll( dbTools.initPreferences( BaseApp.APP().getCommonTaskContext(), getInitSettingsMap() ) );
    }

    static Map<PropertiesTypeEnum, List<String>> getInitSettingsMap() {
        Map<PropertiesTypeEnum, List<String>> mapPrefs = new HashMap<>();
        //Настройки ядра
        for ( PropertyItemEnum pie : PropertyItemEnum.values() ) {
            if ( !pie.isEditable() || !U.in( pie.getPropertyType(), DB_USER, DB_GLOBAL, DB_UNIVERSAL ) ) {
                continue;
            }
            List<String> list = mapPrefs.computeIfAbsent( pie.getPropertyType(), k -> new ArrayList<>() );
            list.add( pie.getName() );
        }
        List<String> globalSettings = mapPrefs.get(DB_GLOBAL);
        //Настройки syslog
        Stream.of(
            SyslogProperties.PORT,
            SyslogProperties.FACILITY,
            SyslogProperties.SUFFIX_PATTERN,
            SyslogProperties.SYSLOG_HOST,
            SyslogProperties.USE_SYSLOG,
            SyslogProperties.USE_STDLOG
            )
          .forEach(globalSettings::add);
        return mapPrefs;
    }

    public static Properties getPrpProperties() {
        return staticPrefs.get( PRP );
    }

    public static Properties getSmrProperties(boolean resetCache) {
        if ( resetCache || staticPrefs.computeIfAbsent( SMR, (v)->new Properties() ).isEmpty() ){
            querySmr();
        }
        return staticPrefs.get( SMR );
    }

    private static void querySmr() {

        try {

            final IDataCall call_loadSmr = SQLCallBuilder.NEW(BaseApp.APP().getCommonTaskContext())
                .url(PREF_AppProperties_old.PLSQL_XML)
                .name("loadSmr")
            .build()
                .execute();

            List o_smr = call_loadSmr.get("O_SMR");

            final Properties properties = staticPrefs.computeIfAbsent(SMR, type -> new Properties());
/*
      o_smr := array_append( o_smr, l_smr.cSmrBic );
      o_smr := array_append( o_smr, l_smr.cSmrCur );
      o_smr := array_append( o_smr, l_smr.cSmrName);

      l_res := null;
      BEGIN
         SELECT ccusKsiva into l_res FROM "CUS" WHERE icusnum = l_smr.ismrcus;

            smrList.add( "CSMRNAME" );
            smrList.add( "CSMRCUR" );
            smrList.add( "CSMRBIC" );
            smrList.add( "ccusKsiva" );

 */
            for( int i = 0; i < o_smr.size(); i++ )
            {
                properties.setProperty (
                    U.decode( i, 0, "CSMRBIC", 1, "CSMRCUR", 2, "CSMRNAME", 3, "ccusKsiva" ),
                    U.nvl( (String)o_smr.get(i), S.EMPTY_STRING )
                );

                if( i >= 3 )
                    break;
            }
        }
        catch( Throwable th ) {
            throw new PropertyException( fore.getString("OSHIBKA_PRI_POLUCHENII_SMR_SVOJSTV"), th );
        }
    }

//    private static void querySmr() {
//        Connection con = BaseApp.APP().getCommonTaskContext().getConnection();
//        try {
//            String strSQL = "select * from smr";
//
//            try( PreparedStatement ps = con.prepareStatement(strSQL) ) {
//
//                try( ResultSet rs = ps.executeQuery() ) {
//
//                    if( rs.next() ) {
//                        ResultSetMetaData rsm = rs.getMetaData();
//                        for( int i = rsm.getColumnCount(); i > 0; i-- )
//                            if( rs.getObject(i) != null ){
//                                staticPrefs.get( SMR ).setProperty( rsm.getColumnName(i).toUpperCase(), rs.getString(i) );
//                            }
//                    }
//                    else
//                        throw new PropertyException( fore.getString("OSHIBKA_PRI_POLUCHENII_SMR_SVOJSTV_TABLICA_SMR_PUSTAYA") );
//
//                } catch( SQLException ex ) {
//                    throw new JInvDbException( ex, strSQL );
//                }
//            }
//        } catch( Throwable th ) {
//            throw new PropertyException( fore.getString("OSHIBKA_PRI_POLUCHENII_SMR_SVOJSTV"), th );
//        }
//    }

    @Override
    public void getProperties( Properties properties ) {
        throw new UnsupportedOperationException( "Not supported yet." ); //To change body of generated methods, choose Tools | Templates.
    }    /**
     *
     */
    @Override
    public PropertiesTypeEnum getType() {
        return propertiesType;
    }

    /**
     *
     */
    @Override
    protected Object doLoadProperty( String property ) {
        PropertiesTypeEnum type = getType();
        if ( U.in( type, /*DB_APP_GLOBAL, DB_APP_UNIVERSAL, DB_APP_USER,*/ DB_UNIVERSAL, DB_GLOBAL, DB_USER ) &&
                !BaseApp.APP().isAfterLogin() ) {
            return null;
        } else {
            return dbTools.getPreference( getApp().getCommonTaskContext(), getType(), property, null );
        }
    }

    @Override
    public void setProperty( String property, Object value ) {
        String val = value == null ? null : value.toString();
        dbTools.setPreference( getApp().getCommonTaskContext(), getType(), property, val );
    }

}

class XXITools implements IDBTools {
    private static final XXITools g_instance = new XXITools();
    private final PrefReader appPrefReader = new PrefReader();

    /** */
    private XXITools() {
    }

    /** */
    static public XXITools INSTANCE() {
        return g_instance;
    }

    public IMenuLoader getMenuLoader( TaskContext tc, String menuID ) {
        return new XXIMenuLoader( tc, menuID );
    }

    /** Получение свойств приложения не из таблицы pref */
    public void getAppProperties( TaskContext tc, Properties properties ) {
    }

    public String getPreference( TaskContext tc, PropertiesTypeEnum type, String preference, String defValue ) {
        String result = null;
        // условие просмотра в кеше
        if ( appPrefReader.contains( type, preference ) ) {
            result = appPrefReader.getProperties( type ).getStringProperty( preference, null );
        } else {
            result = appPrefReader.pref( type, preference )
                    .execute( tc )
                    .getProperties( type )
                    .getStringProperty( preference );
        }
        if ( S.isNullOrEmpty( result ) ) {
            result = defValue;
        }
        return result;
    }

    public void setPreference( TaskContext tc, PropertiesTypeEnum type, String property, String value ) {
        new PrefWriterBuilder().pref( type, property, value ).execute( tc );
    }

    public void setPreference( TaskContext tc, Map<PropertiesTypeEnum, List<Pair<String, String>>> map ) {
        if ( map != null && !map.isEmpty() ) {
            PrefWriterBuilder writer = new PrefWriterBuilder();
            map.keySet().forEach( ( PropertiesTypeEnum t ) -> {
                writer.pref( t, map.get( t ).toArray( new Pair[0] ) );
            } );
            writer.execute( tc );
        }
    }

    public Map<PropertiesTypeEnum, Properties> initPreferences(
            TaskContext tc,
            Map<PropertiesTypeEnum, List<String>> prefNames ) {
        if ( prefNames != null ) {
            appPrefReader.prefMap(prefNames).executeInit( tc );
        }
        return appPrefReader.getStaticProperties();
    }

    public void refreshAppPreferenceCache( TaskContext tc ) {
        appPrefReader.prefMap( PREF_AppProperties_old.getInitSettingsMap() ).execute( tc );
    }

    static class PrefReader {
        protected static Logger logger = getLogger(lookup().lookupClass());
        private Map<PropertiesTypeEnum, Set<String>> upsNames = new TreeMap<>();
        private Map<PropertiesTypeEnum, Properties> upsPrefs = new TreeMap<>();
        private Map<PropertiesTypeEnum, Set<String>> staticNames = new TreeMap<>();
        private Map<PropertiesTypeEnum, Properties> staticPrefs = new TreeMap<>();

        PrefReader() {
            for ( final PropertiesTypeEnum type : PropertiesTypeEnum.values() ) {
                staticPrefs.put( type, new Properties() );
                upsPrefs.put( type, new Properties() );
            }

            Set<String> prpList = new TreeSet<>();
            prpList.add( "ru.inversion.app.user_full_name"            );
            prpList.add( "ru.inversion.app.allow_change_filial"       );
            prpList.add( "ru.inversion.form.table.allow_change_filter");

            Set<String> smrList = new TreeSet<>();
            smrList.add( "CSMRNAME" );
            smrList.add( "CSMRCUR"  );
            smrList.add( "CSMRBIC"  );
            smrList.add( "ccusKsiva");

            staticNames.put( PRP, prpList );
            staticNames.put( SMR, smrList );
        }

        public PrefReader prefMap( Map<PropertiesTypeEnum, List<String>> prefMap ) {
            prefMap.keySet().forEach( ( PropertiesTypeEnum t ) -> {
                pref( t, prefMap.get( t ).toArray( new String[0] ) );
            } );
            return this;
        }
        public PrefReader pref( PropertiesTypeEnum type, String... names ) {
            Set<String> existing = upsNames.get( type );
            final Set<String> nameSet = existing == null ? new TreeSet<>() : existing;
            nameSet.addAll( Arrays.asList( names ) );
            this.upsNames.put( type, nameSet );
            return this;
        }
        public PrefReader executeInit( TaskContext tc ) {
            getPrefFromDB( tc ,true );
            String propertyName = PropertyItemEnum.DB_MAX_RECORDS.getName();
            if (contains( DB_GLOBAL, propertyName ) ){

                String systemMame = "ru.inversion.db." + propertyName.toLowerCase();
                Object value = getProperties( DB_GLOBAL ).getProperty( propertyName );

                BaseApp.APP().getProperties( PropertiesTypeEnum.PRP ).setProperty( systemMame, value
                );
            }

            return this;
        }

        /** Получаем данные из БД */
        public PrefReader execute( TaskContext tc ) {
            getPrefFromDB( tc, false );
            return this;
        }

        private boolean showedPrefError = false;
        private String fillExceptionText = "Failed to get properties from DB. \n" +
                "Program will not function properly. Similar errors will not be displayed for this session. See log for details.";

        private void getPrefFromDB( TaskContext tc, boolean loadAll ) {
            try {
                final Map<String, Object> ioMap = new HashMap<>();

                //init maps
                for ( final PropertiesTypeEnum type : PropertiesTypeEnum.values() ) {
                    ioMap.put( type.name(), new ArrayList<>() );
                }
                upsNames.forEach( ( type, nameSet ) -> ioMap.put( type.name(), new ArrayList<String>( nameSet ) ) );
                if ( loadAll ) {
                    staticNames.forEach( ( type, nameSet ) -> ioMap.put( type.name(), new ArrayList<String>( nameSet ) ) );
                }

                //execute
                String mode = loadAll ? "loadAll" : "load";
                SQLExpressionFactory.INSTANCE().execute( PREF_AppProperties_old.PLSQL_XML, mode, tc.getConnection(), ioMap );

                //Заполняем полученные значения
                boolean upsOk = fillPrefs( ioMap, upsNames, upsPrefs );

                if ( !upsOk && !showedPrefError ){
                    showedPrefError = true;
                    throw new AppException( fillExceptionText );
                }

                if ( loadAll ) {
                    boolean staticOk = fillPrefs( ioMap, staticNames, staticPrefs );

                    if ( !staticOk && !showedPrefError ){
                        showedPrefError = true;
                        throw new AppException( fillExceptionText );
                    }
                }
                upsNames.clear();
            } catch ( Throwable th ) {
//                logger.error( ex.getContentText(), ex );
                JInvErrorService.handleException( null, th );
            }
        }
        /** Заполнение мапы prefs пропертями из map, используя названия из names*/
        private static boolean fillPrefs( final Map<String, Object> map, final Map<PropertiesTypeEnum, Set<String>> names, final Map<PropertiesTypeEnum, Properties> prefs )
        {
            try {
                for ( PropertiesTypeEnum t : names.keySet() ) {
//                    if ( true ){
//                        throw new RuntimeException( "Oops! " + t.name() );
//                    }
                    final int[] i = {0};
                    final List<String> result = (List<String>) map.get( "O_" + t.name() );
                    if ( result == null ) {
                        throw new RuntimeException( "Did not receive properties from DB: " + t.name() );
                    }
                    Set<String> nameSet = names.get( t );
//                    if ( result.size() != nameSet.size() ) {
//                        String blame = result.size() > nameSet.size() ? "JInvFore.jar" : "JF_PKG_UTIL package";
//                        throw new IndexOutOfBoundsException( "Result<->NameSet size mismatch: \n" +
//                                result +
//                                '\n' +
//                                nameSet );
//                    }
                    Iterator<String> resultIter = result.iterator();
                    for ( String paramName : nameSet ) {
                        if ( !resultIter.hasNext() ) {
                            throw new IndexOutOfBoundsException( "Did not receive property from DB: " + paramName );
                        }
                        String outValue = resultIter.next();
                        prefs.get( t ).setProperty( paramName, S.isNotNullOrEmpty( outValue ) ? outValue : "" );
                    }
                }
            } catch ( Throwable th ){
                //Пишем ошибки в лог, на экран не выводим
                logger.error( "{}", th );
                return false;
            }
            return true;
        }

        public Map<PropertiesTypeEnum, Properties> getStaticProperties(){
            return staticPrefs;
        }

        public IAppProperties getProperties( PropertiesTypeEnum type ) {
            return new AbstractAppProperties( BaseApp.APP() ) {
                @Override
                public PropertiesTypeEnum getType() {
                    return type;
                }

                @Override
                protected Object doLoadProperty( String property ) {
                    //todo: search static too?
                    return upsPrefs.get( type ).get( property );
                }

                @Override
                public void getProperties( Properties properties ) {
                    throw new UnsupportedOperationException( "Not supported yet." );
                }
            };
        }

        public Boolean contains( PropertiesTypeEnum type, String key ) {
            if ( !upsPrefs.isEmpty() && upsPrefs.get( type ) != null ) {
                return upsPrefs.get( type ).containsKey( key );
            }
            return false;
        }
    }

}


class PrefWriterBuilder {
    private final static String N = "\n";
    private final static String L = "L_";
    private final static String O = "O_";
    private final Map<PropertiesTypeEnum, Map<String, String>> map = new HashMap<>();

    public PrefWriterBuilder pref( PropertiesTypeEnum type, Pair<String, String>... keyValue ) {
        Map<String, String> kvMap = this.map.get( type ) == null
                ? new HashMap<>( keyValue.length )
                : this.map.get( type );
        Arrays.stream( keyValue ).forEach( ( Pair<String, String> p ) -> {
            kvMap.put( p.first, p.second );
        } );
        this.map.put( type, kvMap );
        return this;
    }

    public PrefWriterBuilder pref( PropertiesTypeEnum type, String key, String value ) {
        Map<String, String> kvMap = this.map.get( type ) == null ? new HashMap<>() : this.map.get( type );
        kvMap.put( key, value );
        this.map.put( type, kvMap );
        return this;
    }

    public void execute( TaskContext tc )
    {
        try {

            if ( !map.isEmpty() ) {

                final List<String> keys   = new ArrayList<>();
                final List<String> values = new ArrayList<>();

                for( Map.Entry< PropertiesTypeEnum,Map< String, String >> e : map.entrySet())
                {
                    if( e.getValue().isEmpty() )
                         continue;

                    final Character prefix = U.decode( e.getKey(), DB_USER, 'S', DB_GLOBAL, 'G', DB_UNIVERSAL, 'U' );
                    if( prefix == null )
                        continue;

                    for( Map.Entry<String,String> s : e.getValue().entrySet() )
                    {
                        keys.add  ( prefix + s.getKey() );
                        values.add( s.getValue()        );
                    }
                }

                try {

                    SQLCallBuilder.NEW(tc).url(PREF_AppProperties_old.PLSQL_XML).name("save").callBackParameters(new ParametersByName() {
                        @Override
                        public Object getParameter( String parameterName ) {
                            return U.decode( parameterName, "PREF_KEY", keys, "PREF_VAL", values );
                        }
                    }).build().execute();

                    tc.commit();
                }
                catch( Throwable th ) {
                    tc.rollback();
                    throw th;
                }
            }
        }
        catch( Throwable th ) {
            JInvErrorService.handleException( null, th );
        }

            //Формируем запрос
//            String queryString = getQueryString();
//            saveToDB( queryString, tc );

    }

    /*
    private String getQueryString() {
        StringBuilder sb = new StringBuilder();
        sb.append( "DECLARE" + N );
        map.keySet().stream().forEach( t -> {
            sb.append( getDeclared( t ) );
        } );
        sb.append( "BEGIN" + N );
        map.keySet().stream().forEach( t -> {
            sb.append( getLoop( t ) );
        } );
        sb.append( "END;" );
        return sb.toString();
    }

    private String getDeclared( PropertiesTypeEnum t ) {
        final String name = t.name().toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append( L ).append( name );
        sb.append( "  T_TAB_VARCHAR2_2000 := T_TAB_VARCHAR2_2000 " );
        sb.append( getListValues( t ) );
        sb.append( ";" + N );
        sb.append( O ).append( name );
        sb.append( " T_TAB_VARCHAR2_2000 := T_TAB_VARCHAR2_2000 " );
        sb.append( getListValues( t ) );
        sb.append( ";" + N );
        return sb.toString();
    }

    private String getListValues( PropertiesTypeEnum t ) {
        return "(" +
                String.join( ",", map.get( t ).keySet().stream().map( s -> "?" ).collect( Collectors.toList() ) ) +
                ")";
    }

    private String getLoop( PropertiesTypeEnum t ) {
        final String name = t.name().toUpperCase();
        StringBuilder sb = new StringBuilder();
        sb.append( " FOR i IN 1.." ).append( L ).append( name ).append( ".Count" + N );
        sb.append( " LOOP " + N );
        sb.append( getFunction( t ) );
        sb.append( "END LOOP;" + N );
        return sb.toString();
    }

    private String getFunction( PropertiesTypeEnum t ) {
//        final String app = "'" + BaseApp.APP().getAppID() + ".' || ";
        String result = "";
        switch ( t ) {
            case DB_USER:
                result = "jf_pkg_util.set_prefupper( " +
                        L +
                        t.name().toUpperCase() +
                        "(i) ,  " +
                        O +
                        t.name().toUpperCase() +
                        "(i));" +
                        N;
                break;
//            case DB_APP_USER:
//                result = "jf_pkg_util.set_prefupper( " +
//                        app +
//                        L +
//                        t.name().toUpperCase() +
//                        "(i) ,  " +
//                        O +
//                        t.name().toUpperCase() +
//                        "(i));" +
//                        N;
//                break;
            case DB_GLOBAL:
                result = "jf_pkg_util.set_Global_PrefUpper( " +
                        L +
                        t.name().toUpperCase() +
                        "(i) ,  " +
                        O +
                        t.name().toUpperCase() +
                        "(i));" +
                        N;
                break;
//            case DB_APP_GLOBAL:
//                result = "jf_pkg_util.set_Global_PrefUpper( " +
//                        app +
//                        L +
//                        t.name().toUpperCase() +
//                        "(i) ,  " +
//                        O +
//                        t.name().toUpperCase() +
//                        "(i));" +
//                        N;
//                break;
            case DB_UNIVERSAL:
                result = "jf_pkg_util.set_Universal_Preference( " +
                        L +
                        t.name().toUpperCase() +
                        "(i) ,  " +
                        O +
                        t.name().toUpperCase() +
                        "(i));" +
                        N;
                break;
//            case DB_APP_UNIVERSAL:
//                result = "jf_pkg_util.set_Universal_Preference( " +
//                        app +
//                        L +
//                        t.name().toUpperCase() +
//                        "(i) ,  " +
//                        O +
//                        t.name().toUpperCase() +
//                        "(i));" +
//                        N;
//                break;
        }
        return result;
    }

    private void saveToDB( String queryString, TaskContext tc ) {
        try ( CallableStatement cs = tc.getConnection().prepareCall( queryString ) ) {
            int i = 1;
            for ( PropertiesTypeEnum t : map.keySet() ) {
                for ( String param : map.get( t ).keySet() ) {
                    cs.setString( i++, param );
                }
                for ( String param : map.get( t ).values() ) {
                    cs.setString( i++, param );
                }
            }
            cs.execute();
        } catch ( SQLException ex ) {
            JInvErrorService.handleException( null, ex );
        }
    }
    */
}
