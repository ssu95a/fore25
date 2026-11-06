package ru.inversion.fx.app;

import com.sun.javafx.css.StyleManager;
import com.sun.javafx.stage.StageHelper;
import com.sun.management.OperatingSystemMXBean;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.MenuBar;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.lang.LocaleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.dataset.AbstractDataSetBase;
import ru.inversion.dataset.IDataSetListFactory;
import ru.inversion.dataset.fx.LiveArrayListEx;
import ru.inversion.email.EmailService;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.frame.JInvMainFrame;
import ru.inversion.fx.app.frame.MenuWindow;
import ru.inversion.fx.app.frame.menu.IMenuItemData;
import ru.inversion.fx.app.frame.menu.PropertyItemEnum;
import ru.inversion.fx.app.login.LoginManager;
import ru.inversion.fx.app.property.*;
import ru.inversion.fx.app.service.AppServiceFactory;
import ru.inversion.fx.app.service.IAppService;
import ru.inversion.fx.app.service.IForeXXISupport;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.app.service.exteditor.ExternalEditorManager;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.treetableex.TreeViewItemAdapter;
import ru.inversion.fx.log.LogManager;
import ru.inversion.tc.TCStorage;
import ru.inversion.tc.TaskContext;
import ru.inversion.tds.AbstractTreeDataSet;
import ru.inversion.tds.ITreeDataSet;
import ru.inversion.tds.ITreeDataSetItem;
import ru.inversion.tds.ITreeDataSetItemFactory;
import ru.inversion.utils.S;

import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;
import ru.inversion.xxi.MethodTimeCheck;


import java.awt.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.inversion.fx.app.AppKiller.runCheckXxiUpgrade;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.*;

/**
 * @author ssu @
 */
public class BaseApp extends Application {

    public static final OperatingSystemMXBean OPERATING_SYSTEM_MXBEAN =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    //public static final int OS_BITS = getCorrectOSArch();

    public static final int JVM_BITS = Integer.parseInt(System.getProperty("sun.arch.data.model"));

    /** Singleton экземпляр приложения */
    protected static BaseApp g_app;

    /** Логгер приложения */
    protected static Logger appLog;

    /** Файл ресурсов ядра */
    private static final ResourceBundle foreBundle = ResourceBundle.getBundle("fore");

    /** */
    public static final Locale DEFAULT_LOCALE = new Locale("ru");

    /** TaskContext служебный. Для нужд фреймворка */
    private volatile TaskContext commonTaskContext;

    /** */
    protected ViewContext primaryViewContext;// = new ViewContext( null, null);

    /** Свойства */
    protected final IAppProperties[] appPropertiesList = new IAppProperties[PropertiesTypeEnum.values().length];

    /** Фабрика сервисов !надо убрать */
    private final AppServiceFactory serviceFactory = AppServiceFactory.getInstance();

    /**  Флаг для отображения состояния после процедуры авторизации в БД */
    private final BooleanProperty afterLoginProperty = new SimpleBooleanProperty( this, "afterLogin", false );
    protected PriorityQueue< Pair< Consumer<ViewContext>, Integer > > afterLoginCallbacks;

    public void addAfterLoginCallback( Consumer<ViewContext> cnsmr, int priority ) {

        if( cnsmr != null )
        {
            if( afterLoginCallbacks == null )
                afterLoginCallbacks = new PriorityQueue<>(Comparator.comparingInt(Pair::getValue));

            afterLoginCallbacks.add( new Pair<>(cnsmr, priority ) );
        }
    }


    /** Сколько раз в этом приложении запускали формы с интерфейсом */
    private int guiLaunchCount = 0;

    /**
     * ID приложения.
     * В виде короткой строки на латинском, без пробелов и разделителей (например "XL" или "GSM").
     * На основании ID будут создаваться различные объекты системы: логгер, свойства и т.д.
     *
     * @return ID приложения
     */
    public String getAppID( )
    {
        String s = System.getProperty("APP_ID");

        if( s == null )
        {
            String n = this.getClass().getName();
            if( n.startsWith( "ru.inversion." ) )
            {
                n = n.substring( "ru.inversion.".length() );
                int i = n.indexOf('.');
                if( i == -1 ) s = n; else s = n.substring( 0, i );
            }

            System.setProperty("APP_ID", s );
        }

        return s;
    }

    /** */
    public Stage getPrimaryStage() {
        return primaryViewContext.getStage();
    }

    /** */
    public ViewContext getPrimaryViewContext() {
        return primaryViewContext;
    }

    /** bundle по умолчанию */
    public ResourceBundle getCommonResourceBundle() {
        return foreBundle;
    }

    /** Возвращения объекта TaskContext */
    public TaskContext getCommonTaskContext() {

        if( commonTaskContext == null )
        {
            synchronized( this ) {
                if( commonTaskContext == null ) {
                    commonTaskContext = new TaskContext() {
                        @Override
                        public boolean isAutoCommit() {
                            return true;
                        }
                        @Override
                        public void setAutoCommit( boolean autoCommit ) {
                            //do nothing
                        }
                    };
                    commonTaskContext.setProperty( "common", Boolean.TRUE );
                    try {
                        commonTaskContext.getConnection().setAutoCommit(true);
                    } catch(SQLException ignored) {
                    }
                }
            }
        }
        return commonTaskContext;
    }

    /**  Получения объекта свойств приложения */
    public IAppProperties getProperties(PropertiesTypeEnum propertiesType)
    {
        if ( propertiesType == DB_APP_GLOBAL    ) propertiesType = DB_GLOBAL;
        if ( propertiesType == DB_APP_UNIVERSAL ) propertiesType = DB_UNIVERSAL;
        if ( propertiesType == DB_APP_USER      ) propertiesType = DB_USER;
        if ( propertiesType == LOCAL_APP_SYSTEM ) propertiesType = LOCAL_SYSTEM;
        if ( propertiesType == LOCAL_APP_USER   ) propertiesType = LOCAL_USER;

        if( appPropertiesList[propertiesType.ordinal()] == null )
        {
            switch( propertiesType )
            {
                case PRP:
                    appPropertiesList[propertiesType.ordinal()] = new PRP_AppProperties(this);
                break;
                case SMR:
                    appPropertiesList[propertiesType.ordinal()] = new SMR_AppProperties(this);
                break;
                case DB_USER:
                case DB_GLOBAL:
                case DB_UNIVERSAL:
                    appPropertiesList[propertiesType.ordinal()] = new PREF_AppProperties(this, propertiesType);
                break;
                case LOCAL_USER:
                case LOCAL_SYSTEM:
                    appPropertiesList[propertiesType.ordinal()] = new LOCAL_AppProperties(this, propertiesType);
                break;
                case APP_CACHE:
                    appPropertiesList[propertiesType.ordinal()] = new CACHE_AppProperties(this);
                break;
            }
        }//end if
        return appPropertiesList[propertiesType.ordinal()];
    }

    /** возможность перегрузки получения значения свойств */
    public <P> P overridePropertyValue(PropertiesTypeEnum propertiesType, String property) {
        return null;
    }

    /**  Метод конфигурации логера приложения. */
    protected void configureDefaultLogger()
    {
        LogManager.INSTANCE().configureDefaultLogger();
        appLog = LoggerFactory.getLogger( "ru.inversion." + getAppID() );
        // logSystemProperties();
    }

    /** Логирование при старте приложения */
    protected void logSystemProperties() {

        if (!getProperties(PRP).getBooleanProperty("log_system_properties", false)) {
            return;
        }

        appLog.info("Application: {}", getAppID() );
        appLog.info(String.format("----------------- Start at %1$tF %1$tT .... -----------------------------", new Date()));

        appLog.info(" ----- System.Properties: ----- ");
        System.getProperties().forEach(( key, value ) -> appLog.info("{} = {}", key, value));

        appLog.info(" ----- System.Env: ----- ");
        System.getenv().forEach(( key, value ) -> appLog.info("{} = {}", key, value));
    }

    /** Пользовательский метод */
    protected void afterLogin() throws Exception {
    }

    /** */
    private void initDBLocale( TaskContext tc, Boolean create )
    {
        if(!create )
            return;

        appLog.debug("call initDBLocale: {}", getLocale().getLanguage() );

        try {
            try( CallableStatement cs = tc.getConnection().prepareCall("{call JF_PKG_UTIL.set_locale_lang(?)}") ) {
                cs.setString( 1, getLocale().getLanguage() );
                cs.execute();
            }
        }
        catch( SQLException ex ) {
            appLog.error("Error on set locale to db.", ex );
        }
    }

    /**  Вызывается после подключения к БД. */
    protected void internalAfterLogin() throws Exception {

        final MethodTimeCheck timeCheck = new MethodTimeCheck( BaseApp.class.getName(), getClass().getName(), "internalAfterLogin" );

        timeCheck.fixTime("INTERNAL_AFTER_LOGIN_1");
        setAfterLogin( true );

        //init PREF
        getProperties( DB_USER );

        LogManager.INSTANCE().refreshLoggerConfig();

        PRP_AppProperties p = (PRP_AppProperties) getProperties( PRP );
        p.initAfterLoginProperties();

        SMR_AppProperties s = (SMR_AppProperties) getProperties( SMR );
        s.init();

        TCStorage.INSTANCE().addListener(new BiConsumer< TaskContext, Boolean >() {
            private String title;
            @Override
            public void accept( TaskContext tc, Boolean create ) {
                if( create ) {
                    if( title == null )
                        title = BaseApp.APP().getProperties(SMR).getStringProperty("ru.inversion.app.title");
                    tc.assignAppTitle( title );
                }
            }
        });

        // Инициализация службы электронной почты
        initServiceMail( );

        // UI config
        viewPrefService().initXMLViewer();

        // Конфигурируем DataSet, в качестве внутренней коллекции устанавливаем коллекции FX
        AbstractDataSetBase.setDataSetListFactory(new IDataSetListFactory() {
            @Override
            public < T > List< T > createDataSetList() {
                return new LiveArrayListEx();
            }
            @Override
            public < T > List< T > createDataSetList( int size ) {
                return new LiveArrayListEx();
            }
            @Override
            public < T > List< T > createDataSetList( List< T > copyFrom ) {
                return FXCollections.observableList( new ArrayList<>(copyFrom) );
            }
        });

        // Настройка TreeDataSet
        AbstractTreeDataSet.setDefaultItemFactory(new ITreeDataSetItemFactory() {

            @Override
            public <P> ITreeDataSetItem<P> createItem( ITreeDataSetItem<P> parent, P value )
            {
                TreeViewItemAdapter<P> item = new TreeViewItemAdapter<>(value);
                parent.addChild(item);
                return item;
            }

            @Override
            public <P> ITreeDataSetItem<P> createRootItem( ITreeDataSet<P> treeDataSet, P value ) {
                return new TreeViewItemAdapter<>( treeDataSet, value);
            }
        });

        //
        if( U.nvl( TypeConverter.convert( getProperties(DB_UNIVERSAL).getProperty("RUN_FX_CALL_PL_MON"), Boolean.class ), Boolean.FALSE ) )
        {
            SQLCallBuilder.setCallRegister( BaseAppHelper.runFxCallMonitor() );
        }

        // Настройка локали
        refreshLocale( );

        //
        afterLogin( );

        if( this.afterLoginCallbacks != null )
        {
            afterLoginCallbacks.forEach( c->c.getKey().accept(primaryViewContext) );
            //clear
            afterLoginCallbacks = null;
        }

        timeCheck.fixTime("INTERNAL_AFTER_LOGIN_2");
        timeCheck.printTime();
    }


    /** */
    private void initServiceMail( ) {

        Function<String,Object> parameters = new Function< String, Object >() {
            @Override
            public Object apply( String s ) {

                if( S.isNullOrEmpty(s) )
                    return null;

                switch(s) {
                    case EmailService.SMTP_HOST:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_GLOBAL ).getStringProperty( "DEFAULT_SMTP_SERVER" );
                    case EmailService.SMTP_PORT:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_GLOBAL ).getStringProperty( "DEFAULT_SMTP_PORT"   );
                    case EmailService.SMTP_DEBUG:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_USER   ).getStringProperty( "DEFAULT_SMTP_DEBUG"  );
                    case EmailService.DEFAULT_FROM_ADDR:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_GLOBAL ).getStringProperty( "DEFAULT_SENDER_MAIL" );
                    case EmailService.DEFAULT_FROM_NAME:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_GLOBAL ).getStringProperty( "DEFAULT_SENDER_NAME" );
                    case EmailService.SMTP_LOGIN:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_GLOBAL ).getStringProperty( "MAIL_USR_LOGIN" );
                    case EmailService.SMTP_PASSWORD:
                        return BaseApp.APP().getProperties( PropertiesTypeEnum.DB_GLOBAL ).getStringProperty( "MAIL_USR_PASSWORD" );
                }
                return null;
            }
        };
        EmailService.setDefaultSettingsSupplier( parameters );
    }

    /** */
    @Override
    public void init() {

        // Для oracle.TIMESTAMP преобразований в sql.TimeStamp
        System.setProperty( "oracle.jdbc.J2EE13Compliant", "true" );

        // Чтоб брался только fore-logback.xml из самого JInvFore.jar
        //System.setProperty("logback.configurationFile", "classpath:fore-logback.xml");

        // Чтоб не рвало сетевые соединения
        // Перенесено в параметры jvm
        // System.setProperty( "oracle.net.keepAlive", "true" );

        final MethodTimeCheck timeCheck = new MethodTimeCheck( BaseApp.class.getName(), getClass().getName(), "init" );

        timeCheck.fixTime("INIT_1");

        g_app = this;

        PRP_AppProperties p = (PRP_AppProperties) getProperties(PRP);
        p.initPreRunSetting();

        configureDefaultLogger();

        AppKiller.run();

        timeCheck.fixTime("INIT_2");

        timeCheck.printTime();
    }

    /** */
    @Override
    public void start( Stage primaryStage ) throws Exception {
        try
        {
            // GUI settings
            Application.setUserAgentStylesheet(Application.STYLESHEET_MODENA);
            StyleManager.getInstance().addUserAgentStylesheet("css/general.css");
            StyleManager.getInstance().addUserAgentStylesheet("style/style.css");

            logSystemProperties();

            primaryViewContext = ViewContext.of(primaryStage);

            // JVM info
            appLog.info(" OS: {} bit", getCorrectOSArch() );
            appLog.info("JVM: {} bit", JVM_BITS );

            if( LoginManager.login(primaryStage) )
            {
                // запуск проверяльщика обновителя XXI БД
                runCheckXxiUpgrade();

                //Чтение параметров из базы после логина
                internalAfterLogin( );

                //Показ главного окна
                internalShowMainWindow();
            }
        } catch (Throwable ex) {
            JInvErrorService.handleException( primaryStage, ex );
        }
    }

    /** */
    protected void internalShowMainWindow() throws AppException {

        String runClass  = getProperties(PRP).getStringProperty("ru.inversion.start_class" ),
               runMethod = getProperties(PRP).getStringProperty("ru.inversion.start_method");

        closeSplashScreen( );

        // parameters //
        if( runMethod != null )
        {
            runMethod (
                runClass,
                runMethod,
                BaseAppHelper.prepareParameters (
                    this
                )
            );
        }
        else {
            showMainWindow( );
        }

        //private API! replace with Window.getWindows() in JavaFX 9+ https://stackoverflow.com/a/58283054
        StageHelper.getStages().forEach(stage -> {
            appLog.debug("found stage: {}, showing = {}", stage, stage.isShowing());
            if (stage != primaryViewContext.getStage() && stage.isShowing()) {
                reportGuiLaunch();
            }
        });

        Platform.runLater(() -> {
            appLog.debug("Checking guiLaunchCount: {}", guiLaunchCount);
            if (guiLaunchCount == 0){
                appLog.warn("Shutting down! No GUI launches detected");
                Platform.exit();
            }
        });
    }

    private JInvMainFrame frame = null;

    public JInvMainFrame getMainFrame() {
        return frame;
    }

    /** */
    protected void showMainWindow() {

        try {

            ViewPrefAppService.DEFAULT_FX_FRAME_MODE = JInvMainFrame.JInvFrameMode.SDI;

            frame = new JInvMainFrame(this::afterCreateMenu);
            frame.show();
        }
        catch( Throwable ex ) {
            JInvErrorService.handleException( primaryViewContext.getStage(), ex );
        }
    }

    /** */
    protected void afterCreateMenu( MenuBar menubar ) {
    }

    /** */
    @Override
    public void stop() {

        ExternalEditorManager.INSTANCE().savePreferences();

        if( commonTaskContext != null )
        {
            commonTaskContext.close();
            commonTaskContext = null;
        }

        final ThreadPoolManager threadPoolManager = ThreadPoolManager.getInstance();
        appLog.debug(threadPoolManager.getStats());
        threadPoolManager.shutdown();
        appLog.info("All ThreadPools are now stopped");

        TCStorage.INSTANCE().close();
        appLog.info("All connections are closed");
    }

    /** */
    public IDBTools getDBTools() {
        return PREF_AppProperties.getDBTools();
    }


    public static <T extends BaseApp> T APP() {
        return (T) g_app;
    }

    protected void beforeMethodRun(Class runClass, Method runMethod, Map<String, Object> parameters) throws AppException {
    }

    /** */
    public void runClass( String className, String methodName, Map<String, Object> parameters ) throws AppException {
        runMethod( className, methodName, parameters );
    }

    protected void runMethod( String className, String methodName, Map<String, Object> parameters ) throws AppException {

        if( !Platform.isFxApplicationThread() )
        {
            Platform.runLater( new Runnable() {
                  @Override
                  public void run() {
                      try {
                        internalRunMethod( className, methodName, parameters );
                      }
                      catch( Throwable th ) {
                          throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on runMethod. Details up!", th );
                      }
                  }
               }
            );
        }
        else
            internalRunMethod( className, methodName, parameters );
    }

    /** */
    private void internalRunMethod( String className, String methodName, Map<String, Object> parameters ) throws AppException {

        try {

            Class runClass = null;

            if( S.isNullOrEmpty(className) || this.getClass().getCanonicalName().equals(className) )
            {
                runClass = this.getClass();
            }
            else
            {
                try {
                    runClass = Class.forName(className);
                } catch (Throwable ex) {
                    throw new AppException( java.text.MessageFormat.format( foreBundle.getString("OSHIBKA_ZAGRUZKI_INFORMACII_O_KLASSE"), new Object[]{className}), ex);
                }
            }//end else

            if( S.isNullOrEmpty(methodName) )
            {
                beforeMethodRun(runClass, null, parameters);
                runStartClass  (className);
                return;
            }

            //Objects.requireNonNull( methodName, foreBundle.getString("NEOBHODIMO_ZADAT_IMYA_METODA_METHODNAME_NULL") );

            Method runMethod;

            try {
                runMethod = runClass.getDeclaredMethod( methodName, ViewContext.class, TaskContext.class, Map.class );
            } catch (Throwable ex) {
                try {
                    runMethod = runClass.getDeclaredMethod(methodName, ViewContext.class, Map.class);
                } catch (Throwable ex2) {
                    try {
                        runMethod = runClass.getDeclaredMethod(methodName, ViewContext.class);
                    } catch (Throwable ex3) {
                        try {
                            runMethod = runClass.getDeclaredMethod(methodName);
                        } catch (Throwable ex4) {
                            try {
                                runMethod = runClass.getDeclaredMethod(methodName, TaskContext.class, Map.class);
                            } catch (Throwable ex5) {
                                throw new RuntimeException(java.text.MessageFormat.format( foreBundle.getString("OTSUTSTVUET_METOD_V_KLASSE"), new Object[]{methodName, runClass}), ex5);
                            }
                        }
                    }
                }
            }//end try

            if( !Modifier.isStatic(runMethod.getModifiers()) )
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "Method '" + methodName + "' must be static" );

            beforeMethodRun( runClass, runMethod, parameters);

            Class args[] = runMethod.getParameterTypes();

            if (args == null || args.length == 0) {

                try {
                    runMethod.invoke(null);
                } catch (InvocationTargetException ex) {
                    final Throwable cause = ex.getCause();
                    appLog.error("Invocation of {} failed because of: {}", methodName, cause.getMessage() );
                    throw new AppException( Tags.PRODUCT_LABEL + "Error in the called method", ex);
                }
            }
            else
            {
                int index = 0;

                Object params[] = new Object[args.length];

                for( Class paramClass : args )
                {
                    if( paramClass.equals(TaskContext.class) ) {
                        params[index] = null; // !!! null, не создавать в APP TaskContext
                    } else if (paramClass.equals(ViewContext.class)) {
                        params[index] = primaryViewContext;
                    } else if (paramClass.equals(Stage.class)) {
                        params[index] = primaryViewContext.getStage();
                    } else if (Map.class.isAssignableFrom(paramClass)) {
                        params[index] = U.nvl( parameters, new HashMap() );
                    } else {
                        params[index] = null;
                    }
                    index++;
                }//end for

                try {
                    runMethod.invoke(null, params);
                } catch (InvocationTargetException ex) {
                    final Throwable cause = ex.getCause();
                    appLog.error("Invocation of {} failed because of: {}", methodName, cause.getMessage());
                    throw new AppException(Tags.PRODUCT_LABEL + "Error in the called method", ex);
                }
            }//end else
        } catch (Throwable ex) {
            throw new AppException(String.format( foreBundle.getString("OSHIBKA_PRI_VYPOLNENII_METODA_IZ_KLASSA"), methodName, className ), ex );
        }
    }

    /**
     * Метод для запуска пунктов меню если класс для запуска есть текущее приложение, то запускается из него
     */
    public void runMenuItem(IMenuItemData menuItemData) throws AppException {
        runMethod(menuItemData.getJavaClass(), menuItemData.getJavaMethod(), null);
    }


    /** */
    public void refreshLocale() {

        Locale locale = DEFAULT_LOCALE;

        try {

            final String s_loc = getProperties(PRP).getProperty( AppConstants.LOCALE );

            final String p_loc = getProperties( PropertyItemEnum.I18N_LOCALE.getPropertyType() ).getStringProperty(PropertyItemEnum.I18N_LOCALE.getName(), DEFAULT_LOCALE.toString() );

            if( S.compareToIgnoreCase( s_loc, p_loc ) == 0 )
            {
                locale = LocaleUtils.toLocale ( p_loc );
            }
            else
            {
                if( !S.isNullOrEmpty(s_loc) )
                {
                    final Locale loc_a = LocaleUtils.toLocale(s_loc);

                    if( LocaleUtils.isAvailableLocale(loc_a) )
                    {
                        locale = loc_a;

                        getProperties(PropertyItemEnum.I18N_LOCALE.getPropertyType()).setProperty( PropertyItemEnum.I18N_LOCALE.toString(), s_loc );

                        //initDBLocale( getCommonTaskContext(), true );
                    }
                    else
                        appLog.warn(Tags.PRODUCT_LABEL + "No locale with code '" + s_loc + "'");
                }
                else
                    locale = LocaleUtils.toLocale ( p_loc );
            }

        } catch (Throwable th) {
            appLog.error(Tags.PRODUCT_LABEL + "Error on get '" + PropertyItemEnum.I18N_LOCALE.getName() + "' property", th);
        }

        if(!Locale.getDefault().equals(locale) ) {
            Locale.setDefault( locale );
            initDBLocale( getCommonTaskContext(), true );
        }
//
//        if( isAfterLogin() )
//        {
//            SQLCallBuilder.NEW( getCommonTaskContext() )
//        }
//        else
//            addAfterLoginCallback(null);
    }

    /** */
    public Locale getLocale() {

        return Locale.getDefault();


        /*
        Locale result = DEFAULT_LOCALE;

        try {

            result = LocaleUtils.toLocale (
                    getProperties(PropertyItemEnum.I18N_LOCALE.getPropertyType())
                            .getStringProperty(PropertyItemEnum.I18N_LOCALE.getName(), DEFAULT_LOCALE.toString())
            );

        } catch (Throwable th) {
            appLog.error(Tags.PRODUCT_LABEL + "Error on get '" + PropertyItemEnum.I18N_LOCALE.getName() + "' property", th);
        }

        return result;
        */
    }

    /** */
    public IForeXXISupport foreXXISupport( ) {
        try {
            return (IForeXXISupport) serviceFactory.getService(IForeXXISupport.SERVICE_ID );
        }
        catch( Throwable th ) {
            throw new RuntimeException( th );
        }
    }

    public IAppService getAppService(String serviceID) throws AppException {
        return serviceFactory.getService(serviceID);
    }

    public ViewPrefAppService getViewPrefService( ) throws AppException {
        return (ViewPrefAppService) serviceFactory.getService(ViewPrefAppService.SERVICE_ID );
    }

    public ViewPrefAppService viewPrefService( ) {
        try {
            return (ViewPrefAppService) serviceFactory.getService(ViewPrefAppService.SERVICE_ID );
        }
        catch( Throwable th ) {
            throw new RuntimeException( th );
        }
    }

    @Deprecated
    protected Integer getSSCCODE() {
        return null;
    }

    private void runStartClass(String className) throws Exception
    {
        final Class<?> clazz      = Class.forName(className);
        final Class<?> superclass = clazz.getSuperclass();
        final Object instance     = clazz.newInstance();

        if( instance instanceof MenuWindow )
            superclass.getMethod("show").invoke( clazz.newInstance() );
    }

    /** */

    private void closeSplashScreen() {

        SplashScreen splash = SplashScreen.getSplashScreen();

        if( splash != null )
            splash.close();
    }

    private static int getCorrectOSArch() {

        if (OPERATING_SYSTEM_MXBEAN.getName().startsWith("Windows"))
            return System.getenv("ProgramFiles(x86)") == null ? 32 : 64;

        return System.getProperty("os.arch").contains("64") ? 64 : 32;
    }

    /**
     * @return
     */
    public boolean isAfterLogin() {
        return afterLoginProperty.get();
    }

    /**
     * @param afterLogin
     */
    private void setAfterLogin( boolean afterLogin ) {
        afterLoginProperty.set(afterLogin);
    }

    /** */
    public BooleanProperty afterLoginProperty( ) {
        return afterLoginProperty;
    }

    public void reportGuiLaunch(){
        guiLaunchCount++;
        appLog.debug("Reported GUI launch, guiLaunchCount = {}", guiLaunchCount);
    }
}
