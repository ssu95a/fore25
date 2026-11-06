package ru.inversion.fx.app.property;

import org.slf4j.Logger;
import ru.inversion.fx.app.AppConstants;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.utils.S;
import ru.inversion.utils.TriConsumer;
import ru.inversion.utils.U;
import ru.inversion.xxi.MethodTimeCheck;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.prefs.Preferences;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.app.AppConstants.ORACLE_TNS_ADMIN;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.PRP;

/**
 *
 * @author ssu @
 */
public class PRP_AppProperties extends AbstractAppProperties {

    protected static final Logger g_logger = getLogger(lookup().lookupClass());

    private Map<String,Object>  appProperties;
    private Map<String, String> mainArgs;

    public PRP_AppProperties(BaseApp app) {
        super(app);
        app.getParameters();
    }

    /**
     *
     */
    private File getFileProperties() {

        File fileProperties = null;

        String s = System.getProperty( getApp().getAppID() + ".file_properties");

        if( s == null )
            s = System.getProperty("file_properties");

        if( s == null )
            s = mainArgs.get(getApp().getAppID() + ".file_properties");

        if( s == null )
            s = mainArgs.get("file_properties");

        if( s == null ) {
            s = System.getProperty("user.home") + File.separatorChar + getApp().getAppID() + ".properties";
            g_logger.info(fore.getString("FAJL_S_NASTROJKAMI_NE_UKAZAN_ZAGRUZKA_IZ_FAJLA_PO_UMOLCHANIYU"), s);
        }

        File f = new File(s);
        if( f.exists() && f.isFile() ) {
            fileProperties = f;
        }

        if( fileProperties == null ) {
            s = System.getProperty("user.home") + File.separatorChar + "xxiapp.properties";
            g_logger.info( fore.getString("FAJL_S_NASTROJKAMI_NE_UKAZAN_ZAGRUZKA_IZ_FAJLA_PO_UMOLCHANIYU"), s);
        }

        f = new File(s);
        if( f.exists() && f.isFile() ) {
            fileProperties = f;
        }

        if (fileProperties == null) {
            g_logger.info(fore.getString("FAJL_S_NASTROJKAMI_OTSUTSTVUET"));
        } else {
            g_logger.info("file properties: {}", fileProperties.toString());
        }

        return fileProperties;
    }

    @Override
    public void setProperty( final String property, final Object value ) {

        //g_logger.debug("set app property: {} = {} ", property, nvl( value, "null") );

        if( S.isNullOrEmpty(property) || value == null )
            return;

        if( System.getProperties().contains(property) )
            System.setProperty( property, value.toString() );
        else
        {
            if( appProperties == null )
                appProperties = new HashMap<>( );

            appProperties.put( property, value );
        }
    }

    /**
     *
     * @param property
     * @return
     */
    @Override
    protected Object doLoadProperty( String property ) {

        String value = System.getProperty( property );

        if( S.isNullOrEmpty(value) ) {

            if( mainArgs != null )
                value = mainArgs.get( property );

            if( S.isNullOrEmpty(value) )
            {
                if( appProperties != null )
                {
                    Object o = appProperties.get(property);

                    if(!AppConstants.PASSWORD.equals(property) )
                        g_logger.trace("app property: {} = {} ", property, U.nvl( o, "null") );
//return
                    return o;
                }

                // переменные окружения
                value = System.getenv(property);

                if(!AppConstants.PASSWORD.equals(property) )
                    g_logger.trace("env property: {} = {} ", property, U.nvl(value, "null"));
            }
            else
            {
                if(!AppConstants.PASSWORD.equals(property) )
                    g_logger.trace("args property: {} = {} ", property, U.nvl(value, "null"));
            }
        }
        else
        {
            if(!AppConstants.PASSWORD.equals(property) )
                g_logger.trace("system property: {} = {} ", property, U.nvl(value, "null"));
        }
        return value;
    }

    /**
     * Обходит все свойства хранящиеся в службе
     * Начиная с System, потом Properties, потом mainArgs
     * Первый параметр в visitor, тип свойства
     * 1 - System
     * 2 - Properties
     * 3 - MainArgs
     * 4 - Env
     */
    public void acceptForEach( TriConsumer<Integer, String, Object> visitor ) {

        if( visitor == null )
            return;

        System.getProperties().forEach( (k,v)->visitor.accept(1, (String)k, v) );

        if( appProperties != null )
            appProperties.forEach( (k,v)->visitor.accept(2, (String)k, v) );

        if( mainArgs != null )
            mainArgs.forEach( (k,v)->visitor.accept(3, k, v) );

        System.getenv().forEach( (k,v)->visitor.accept(4, k, v) );
    }

    /**
     *
     */
    @Override
    public void getProperties(Properties properties) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * file_properties=c:\\... tns_admin=c:\\orant\\
     *
     * private Properties parseMainArgs() { /* Properties p = new Properties();
     *
     * for (String s : Optional.ofNullable(APP_CACHE().getParameters()).map((o)->o.getRaw()).orElse( Collections.emptyList() ) ) { if( s.indexOf('=') != -1 ) { String sa[] = s.split("="); if (sa[1] != null
     * && !sa[1].isEmpty()) { p.setProperty(sa[0], sa[1]); }//end if }//end if } return p; }
     */
    /**
     * Инициализация настроек приложения приходящих из main-аргументов или из файла настроек
     *
     * TNS_ADMIN tns_admin В случае отсутствия, поиск в переменной окружения TNS_ADMIN, далее ключа в реестре TNS_ADMIN
     *
     * Архитектура. architect_mode. Значения: dual - двухзвенная, triple - трехзвенная (с создание соединения к базе данных на промежуточном сервере Hessian
     *
     * Наличие окружения XXI. ru.inversion.noxxi / true /false Влияет например на вид календаря : с праздниками или без
     *
     * Использование псевдосоединения. ru.inversion.db_tools Если значение GO, псевдосоединение не используется
     *
     * Режим выбора TNS_ADMIN. ru.inversion.tns_show/true/false При успешном нахождении значения TNS_ADMIN поле для выбора этого параметра в диалоге логина скрывается, если не указано true.
     */
    public void initPreRunSetting() {

        mainArgs = getApp().getParameters().getNamed();
        initFileProperties();
        initXXIAbsensePropertry();
        initDbToolsSettings();
        initIdSmr();

        findAndSetTnsPath();
    }

    /** Установка текущего филиала */
    private void initIdSmr()
    {
        if( S.isNullOrEmpty( System.getProperty("ru.inversion.idsmr") ) )
        {
            String smrIdProperty = getStringProperty("ru.inversion.idsmr");

            if( !S.isNullOrEmpty(smrIdProperty) )
                System.setProperty("ru.inversion.idsmr", smrIdProperty );
        }
    }

    /**
     *
     */
    private static void findAndSetTnsPath() {

        if( System.getProperty(ORACLE_TNS_ADMIN) == null ) {
            Optional.ofNullable(
                U.firstNotNull(
                    BaseApp.APP().getProperties(PRP).getStringProperty("tns_admin"),
                    System.getenv("TNS_ADMIN"),
                    Preferences.userRoot().get("TNS_ADMIN", null)
                )).ifPresent((String tns) -> {
                    System.setProperty( ORACLE_TNS_ADMIN, tns );
                });
        }

        g_logger.info("oracle.net.tns_admin: {}", System.getProperty(ORACLE_TNS_ADMIN));
    }

    @Override
    public PropertiesTypeEnum getType() {
        return PRP;
    }

    private void initFileProperties() {

        File f = getFileProperties();

        if (f != null) {

            try( Reader reader = new FileReader(f) ) {

                if( appProperties == null )
                    appProperties = new HashMap<>();

                Properties p = new Properties();
                p.load(reader);
                p.forEach( (k,v)->appProperties.put((String)k,v) );

            } catch (Throwable e) {
                g_logger.warn(java.text.MessageFormat.format( fore.getString("OSHIBKA_PRI_ZAGRUZKI_NASTROEK_IZ_FAJLA"), new Object[]{f}), e);
            }
        }
    }

    private void initXXIAbsensePropertry() {

        if (getStringProperty("ru.inversion.noxxi") != null) {
            String isXXIAbsense = getStringProperty("ru.inversion.noxxi");
            if (isXXIAbsense.equalsIgnoreCase("true")) {
                System.setProperty("ru.inversion.noxxi", "true");
            } else {
                System.setProperty("ru.inversion.noxxi", "false");
            }
        }

        g_logger.info("ru.inversion.noxxi: {}", System.getProperty("ru.inversion.noxxi"));
    }

    private void initDbToolsSettings() {

        String dbTools = System.getProperty("ru.inversion.db_tools");
        //g_logger.info("ru.inversion.db_tools: " + dbTools);

        if (dbTools != null && dbTools.equalsIgnoreCase("GO")) {
            //g_logger.info("pseudoConnection: false");
        } else {
            //g_logger.info("pseudoConnection: true");
            System.setProperty("ru.inversion.db_tools", "XXI");
        }
    }

    /**
     * Свойства инициализируемые после подключения
     */
    public void initAfterLoginProperties() throws Exception {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(PRP_AppProperties.class.getName(), "initAfterLoginProperties");

        timeCheck.fixTime("INIT_AFTER_LOGIN_PROPERTIES_1");

        if( appProperties == null )
            appProperties = new HashMap<>( );

        appProperties.putAll( PREF_AppProperties.STORAGE.takeMap(PRP) );

        /*
        //Получение статичных данных из БД
        Properties prpProperties = PREF_AppProperties.getPrpProperties();
        if( prpProperties != null )
        {
            prpProperties.forEach( (k,v)->appProperties.put((String)k,v) );
        }
        else
            g_logger.error( "PRP properties == null!" );
        */
        timeCheck.fixTime("INIT_AFTER_LOGIN_PROPERTIES_2");

        String value = null;

        if( getStringProperty("ru.inversion.app.name") == null)
        {
            ResourceBundle rb = getApp().getCommonResourceBundle();
            if (rb != null && rb.containsKey("APP_NAME")) {
                value = rb.getString("APP_NAME");
                appProperties.put("ru.inversion.app.name", value);
            }
        }

        value = getStringProperty( "ru.inversion.mdi_title" );
        if( value != null ) {
            try {
                String mdiTitle = new String( DatatypeConverter.parseBase64Binary(value) );
                appProperties.put( "ru.inversion.app.form_mdi_title", mdiTitle );
            }
            catch( Throwable ignored ) {}
        }

        //Данные из манифеста
        Enumeration<URL> resources = getClass().getClassLoader().getResources(JarFile.MANIFEST_NAME);
        if( resources.hasMoreElements() ) {
            try {
                Manifest manifest = new Manifest( resources.nextElement().openStream() );
                Attributes attributes = manifest.getMainAttributes();
                //                value = attributes.getValue("Implementation-Title");
                //                if(value!=null){
                //                    System.out.println("MANIFEST "+value);
                //                }
                value = attributes.getValue( "Implementation-Version" );
                if ( value != null ) {
                    appProperties.put( "ru.inversion.app.version", value );
                }

                Manifest manifestCore = new JarFile( "JInvFore.jar" ).getManifest();
                Attributes mainAttribsCore = manifestCore.getMainAttributes();

                value = mainAttribsCore.getValue( "SCM-Revision" );
                if ( value != null ) {
                    g_logger.info( "ru.inversion.app.core_scm_version: " + value );
                    appProperties.put( "ru.inversion.app.core_scm_version", value );
                }
                value = mainAttribsCore.getValue( "Implementation-Version" );
                if ( value != null ) {
                    g_logger.info("ru.inversion.app.core_impl_version: {}", value);
                    appProperties.put( "ru.inversion.app.core_impl_version", value );
                }
            } catch ( IOException ignored ) {
            }
        }

        timeCheck.fixTime("INIT_AFTER_LOGIN_PROPERTIES_3");
        timeCheck.printTime();
    }
}
