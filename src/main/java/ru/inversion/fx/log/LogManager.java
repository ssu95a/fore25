package ru.inversion.fx.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.net.SyslogAppender;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.inversion.db.rs.RSUtils;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.utils.S;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static ru.inversion.fx.app.property.SyslogProperties.*;

/**
 * @author antonovdi, Sulimoff
 */
public class LogManager {

    public static final String CONSOLE_APPENDER = "CONSOLE";
    public static final String FILE_APPENDER = "FILE";
    public static final String ERROR_FILE_APPENDER = "ERROR_FILE";
    public static final String ASYNC_APPENDER = "ASYNC";


    //public static String DEFAULT_LOGGING_FOLDER_NAME;

    public static final Level DAFAULT_ROOTLOGGER_LEVEL = Level.ALL;

    public static final int DAFAULT_ROOTLOGGER_HISTORY = 2;

    public static final String DEFAULT_ROOTLOGGER_PATH = "ru.inversion";
    public static final String DEFAULT_ROOTLOGGER_TIMING = "yy-MM-dd";
    public static final String DEFAULT_ROOTLOGGER_FILE_PATTERN = "%date{dd-MM-yyyy HH:mm:ss} %-5level %logger{35} %msg%n";

    public static final String PROPERTY_DEFAULT_ROOTLOGGER_LEVEL = "LOGGER_LEVEL";
    public static final String PROPERTY_DEFAULT_ROOTLOGGER_HISTORY = "LOGGER_HISTORY";
    public static final String PROPERTY_DEFAULT_ROOTLOGGER_TIMING = "LOGGER_TIMING";
    public static final String PROPERTY_DEFAULT_ROOTLOGGER_FILE_PATTERN = "LOGGER_FILE_PATTERN";
    public static final String PROPERTY_DEFAULT_ROOTLOGGER_FOLDER = "LOGGER_FOLDER";

    private static LogManager instance;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(LogManager.class);

    private final StringBuilder messageAfterConfig = new StringBuilder();

    final private LogbackConfigurator configurator = new LogbackConfigurator();

    static {

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        //LogbackConfigurator.forceLoadLogbackConfig();
    }

    /** */
    public static File getCurrentLogFile()
    {
        return INSTANCE().configurator.getLogFilePath().toFile();
    }

    /** */
    public static File getCurrentLogFolder()
    {
        return new File(".");
        //return INSTANCE().configurator.getLogDirectoryPath().toFile();
    }


    /** */
    public static LogManager INSTANCE() {
        if( instance == null )
            instance = new LogManager();
        return instance;
    }

    private LogManager() {
        configureDefaultLogger();
    }

    /** */
    public void configureDefaultLogger( ) {
        IAppProperties properties = BaseApp.APP().getProperties(PropertiesTypeEnum.PRP);
        RuntimeLogConfig config = prepareRuntimeConfig(properties, null);
        //configurator.applyConfiguration( config );
    }

    /** */
    private RuntimeLogConfig prepareRuntimeConfig( IAppProperties userProperties, IAppProperties globalProperties )
    {
        RuntimeLogConfig config = new RuntimeLogConfig( );

        boolean disableLogForUser = false; //stub
            //JInvSecurityService.isCanAccessIsAction(BaseApp.APP().getCommonTaskContext(), 3730);
        boolean disableLogGlobal = false;  //stub
            //! globalProperties.getBooleanProperty(SyslogProperties.USE_STDLOG, true);

        config.setLoggingEnabled( !disableLogForUser && !disableLogGlobal );

        {
            final String logFolder = userProperties.getProperty( PROPERTY_DEFAULT_ROOTLOGGER_FOLDER );

            if( !S.isNullOrEmpty(logFolder) )
            {
                Path pathFolder = Paths.get(logFolder);
                if( Files.exists(pathFolder) && Files.isDirectory(pathFolder) && Files.isWritable(pathFolder) )
                    config.setLogDirectory( pathFolder );
                else
                    System.err.println("bad path to loggerPath: " + pathFolder );
            }
        }

        config.setAppName( BaseApp.APP().getAppID() );

        {
            final String rootLevel = userProperties.getStringProperty(PROPERTY_DEFAULT_ROOTLOGGER_LEVEL);
            if( !S.isNullOrEmpty(rootLevel) )
            {
                try {
                    Level level = Level.valueOf(rootLevel);
                    config.setLogLevel( level );
                } catch (Throwable ex) {
                    logger.error("Error on refreshLoggerLevel", ex);
                }
            }
        }

        {
            final String patternString = userProperties.getStringProperty(PROPERTY_DEFAULT_ROOTLOGGER_FILE_PATTERN);
            if(!S.isNullOrEmpty(patternString) )
                config.setLogPattern(patternString);
        }

        {
            final String historyLog = userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_HISTORY);
            if( !S.isNullOrEmpty(historyLog) )
            {
                try {
                    int maxHistory = Integer.parseInt(historyLog);
                    config.setMaxHistory(maxHistory);
                } catch (NumberFormatException ex) {
                    //setLoggerHistory(rootLogger, DAFAULT_ROOTLOGGER_HISTORY);
                }
            }
        }

        return config;

        /*

        private String maxFileSize    = "10MB";
        private String totalSizeCap   = "1GB";
        */
    }


    /**
     * Конфигурирование логгера после логина в систему
     */
    public void refreshLoggerConfig() {

        try {

            IAppProperties userProperties   = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_USER);
            IAppProperties globalProperties = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_GLOBAL);

            RuntimeLogConfig config = prepareRuntimeConfig(userProperties, globalProperties);
            this.configurator.applyConfiguration(config);

            initSyslog(globalProperties);

        } catch (Throwable ex) {
            logger.error( "refreshLoggerConfig", ex );
        }
    }

    /** */
    static private class SyslogConfig {

        private final String  syslogHost;
        private final String  suffixPattern;
        private final String  facility;
        private final Integer port;

        public SyslogConfig( String syslogHost, String suffixPattern, String facility, Integer port) {
            this.syslogHost    = syslogHost;
            this.suffixPattern = suffixPattern;
            this.facility      = facility;
            this.port          = port;
        }

        public String syslogHost() {
            return syslogHost;
        }

        public String suffixPattern() {
            return suffixPattern;
        }

        public String facility() {
            return facility;
        }

        public Integer port() {
            return port;
        }

        private static SyslogConfig makeOf( IAppProperties globalProperties )
        {
            String syslogHost    = globalProperties.getStringProperty ( SYSLOG_HOST,"127.0.0.1" );
            String facility      = globalProperties.getStringProperty ( FACILITY, "local0" );
            int    port          = globalProperties.getIntegerProperty( PORT,514 );
            String suffixPattern = globalProperties.getStringProperty ( SUFFIX_PATTERN );

            return new SyslogConfig( syslogHost, suffixPattern, facility, port);
        }
    }

    private void initSyslog( IAppProperties globalProperties )
    {
        if( globalProperties.getBooleanProperty( USE_SYSLOG, false ) )
            return;

        final Logger rootLogger = (Logger) LoggerFactory.getLogger(DEFAULT_ROOTLOGGER_PATH);
        if (rootLogger == null)
            return;

        final List<PSyslogEntry> packagesToSyslog = new ArrayList<>();

        RSUtils.<PSyslogEntry>createIterable( BaseApp.APP().getCommonTaskContext().getConnection(), PSyslogEntry.class, null).forEach(packagesToSyslog::add);

        final SyslogConfig syslogConfig = SyslogConfig.makeOf(globalProperties);

        packagesToSyslog.forEach(p -> {

            LoggerLevelEnum logLevel = p.getLogLevel();

            if( logLevel != LoggerLevelEnum.OFF )
                addSyslogAppender( p.getCSLOG_LRNAME(), logLevel, syslogConfig);
            else
                logger.debug("Skipped adding syslog appender for {} because log level is {}", p.getCSLOG_LRNAME(), logLevel );

        });
    }

    private static final String APPENDER_SYSLOG_NAME = "SyslogAppender";

    private void addSyslogAppender( String loggerPath, LoggerLevelEnum logLevel, SyslogConfig config )
    {

        boolean startsWithRuInversion = loggerPath.toLowerCase().startsWith(DEFAULT_ROOTLOGGER_PATH);

        if( !startsWithRuInversion )
            loggerPath = DEFAULT_ROOTLOGGER_PATH + "." + loggerPath;

        String appenderName = APPENDER_SYSLOG_NAME + "_" + loggerPath.toUpperCase().replace(".", "_");

        Logger otherLogger = (Logger) LoggerFactory.getLogger( loggerPath.toLowerCase());

        if( otherLogger.getAppender(appenderName) != null)
        {
            //если такой аппендер уже есть, ничего не делаем
            logger.debug("Skipped adding syslog appender for {} because it already exists", loggerPath);
            return;
        }

        final SyslogAppender syslogAppender = new SyslogAppender();
        syslogAppender.setSyslogHost(config.syslogHost); //ip
        syslogAppender.setPort(config.port); //порт UDP, 514 по умолчанию
        syslogAppender.setFacility(config.facility);
        syslogAppender.setName(appenderName);
        if( S.isNotNullOrEmpty(config.suffixPattern) )
            syslogAppender.setSuffixPattern(config.suffixPattern);

        // $$ Commented for using with SGB Syslog Server
        // syslogAppender.setCharset(UTF_8);
        
        syslogAppender.setContext( otherLogger.getLoggerContext() );
        otherLogger.addAppender  ( syslogAppender );

        logger.debug("Added syslog appender {} ({}) to path {}", appenderName, logLevel.name(), loggerPath);

        try {
            Level level = Level.valueOf(logLevel.name());
            otherLogger.setLevel(level);
        } catch (Throwable ex) {
            logger.error("Error on log level parse", ex);
        }
        syslogAppender.start();
    }

}
