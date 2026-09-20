package ru.inversion.fx.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import ru.inversion.db.rs.RSUtils;
import ru.inversion.fx.app.AppConstants;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.property.SyslogProperties;
import ru.inversion.utils.S;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.inversion.fx.app.AppConstants.LOGGER_FOLDER;

/**
 * @author antonovdi
 */
public class LogManager_old {

    public static String DEFAULT_LOGGING_FOLDER_NAME;

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


    private static final String APPENDER_FILE_NAME = "RollingAppender";
    private static final String APPENDER_CONSOLE_NAME = "ConsoleAppender";

    private static LogManager_old instance;

    private static IAppProperties userProperties;
    private static IAppProperties globalProperties;

    private org.slf4j.Logger logger = LoggerFactory.getLogger(LogManager.class);

    private StringBuilder messageAfterConfig = new StringBuilder();

    static {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    public static String getLoggerFolder() {
        String folder;
        if (userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_FOLDER) != null) {
            folder = (String) userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_FOLDER);
        } else {
            folder = new File(DEFAULT_LOGGING_FOLDER_NAME).getAbsolutePath();
        }
        return folder;
    }

    public static String getLoggerTiming() {
        String timing;
        if (userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_TIMING) != null) {
            timing = (String) userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_TIMING);
        } else {
            timing = DEFAULT_ROOTLOGGER_TIMING;
        }
        return timing;
    }

    public static File getCurrentLogFile() {

        Logger logger = (Logger) LoggerFactory.getLogger(DEFAULT_ROOTLOGGER_PATH);
        Appender appender = logger.getAppender(APPENDER_FILE_NAME);
        if (appender instanceof RollingFileAppender) {
            RollingFileAppender fileAppender = (RollingFileAppender) appender;
            String fileName = fileAppender.getFile();
            return new File(fileName);
        }
        return null;
    }

    /**
     * Определение пути для сохранения файлов логгера
     * <p>
     * Параметры передаются из вне, через -D или --
     */
    private void configureDefaultLoggerFolder() {

        try {

            String pathString = System.getProperty(LOGGER_FOLDER, null);

            if (S.isNotNullOrEmpty(pathString)) {
                Path path = null;

                // Если это некорректный Path
                try {
                    path = Paths.get(pathString);
                } catch (InvalidPathException ex) {
                    messageAfterConfig.
                            append(String.format(BaseApp.APP().getCommonResourceBundle().
                                    getString("LOG_USER_FOLDER_IS_NOT_CORRECT"), pathString)).append("\n");
                    throw new RuntimeException();
                }

                // Если это не директория
                if (!Files.isDirectory(path)) {
                    messageAfterConfig.append(String.format(BaseApp.APP().getCommonResourceBundle().
                            getString("LOG_USER_FOLDER_IS_NOT_DIRECTORY"), path.toString())).
                            append("\n");
                    throw new RuntimeException();
                }

                // Если нет прав на запись
                if (!Files.isWritable(path)) {
                    messageAfterConfig.append(String.format(BaseApp.APP().getCommonResourceBundle().
                            getString("LOG_USER_FOLDER_IS_NOT_WRITABLE"), path.toString())).
                            append("\n");
                    throw new RuntimeException();
                }
                DEFAULT_LOGGING_FOLDER_NAME = path.toString();
            } else {
                messageAfterConfig.append(BaseApp.APP().getCommonResourceBundle().getString("LOG_USER_FOLDER_IS_EMPTY")).
                        append("\n");
                throw new RuntimeException();
            }
        } catch (Throwable e) {
            String folderName = "jfx_logs";
            try {
                Path userHomePath = Paths.get(System.getProperty("user.home")).toAbsolutePath();

                if (Files.isWritable(userHomePath)) {
                    DEFAULT_LOGGING_FOLDER_NAME = userHomePath.resolve(folderName).toString();
                } else {
                    throw new RuntimeException();
                }
            } catch (Throwable ex1) {
                try {
                    Path userTempPath = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath();
                    if (Files.isWritable(userTempPath)) {
                        DEFAULT_LOGGING_FOLDER_NAME = userTempPath.resolve(folderName).toString();
                    } else {
                        throw new RuntimeException();
                    }
                } catch (Throwable ex2) {
                    DEFAULT_LOGGING_FOLDER_NAME = folderName;
                }
            }
        }
    }

    private LogManager_old() {
        configureDefaultLoggerFolder();
    }

    /** */
    public static LogManager_old INSTANCE() {
        if (instance == null) {
            instance = new LogManager_old();
        }
        return instance;
    }

    /**
     * Формирование имени файла для логгера
     * <p>
     * Прошито!
     */
    private static String createLoggerFileName(String timing, String folderName) {

        StringBuilder sb = new StringBuilder();
        sb.append(folderName).append("\\")
                .append(System.getProperty("user.name").toUpperCase())
                .append('_')
                .append(BaseApp.APP().getAppID()).append("%d{")
                .append(timing)
                .append("}")
                .append(".log");

        return sb.toString();
    }

    /**
     *
     */
    public void configureDefaultLogger() {
        configureDefaultLogger(DEFAULT_ROOTLOGGER_PATH, APPENDER_FILE_NAME);
    }

    /**
     * Конфигурация LogBack аппендеров файлового и консольного, параметрами по умолчанию
     * до логина
     */
    public Logger configureDefaultLogger(String rootLoggerName, String appenderFileName) {
        //self4g -> cast to LogBack
        ch.qos.logback.classic.Logger rootLogger = (Logger) LoggerFactory.getLogger(rootLoggerName);
        // сброс контекста
        LoggerContext loggerContext = rootLogger.getLoggerContext();
        loggerContext.reset();
        final LevelChangePropagator levelChangePropagator = new LevelChangePropagator();
        levelChangePropagator.setContext(loggerContext);
        levelChangePropagator.setResetJUL(true);
        loggerContext.addListener(levelChangePropagator);
        Runtime.getRuntime().addShutdownHook(new Thread(loggerContext::stop, "StopLoggerThread")); // Хук на отключение логера

        // Аппендер для файлов кторые могут себя чистить
        RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<>();
        rfAppender.setName(appenderFileName);
        rfAppender.setContext(loggerContext);
        //если есть уже файл, чтоб не удалял,а дописывал
        rfAppender.setAppend(true);
        // фильтр на повторяющиеся исключения, чтоб их не было
        // (неизвестно нужен или нет)
        rfAppender.addFilter(new ThrowableFilter());

        // событие по которому старый сотрется файл, новый появиться
        TimeBasedRollingPolicy<ILoggingEvent> timeTriggerPolicy = new TimeBasedRollingPolicy<>();
        timeTriggerPolicy.setMaxHistory(DAFAULT_ROOTLOGGER_HISTORY); //сколько файлов держать
        timeTriggerPolicy.setCleanHistoryOnStart(true);                      //очистить историю при старте
        // шаблон пути для файла
        timeTriggerPolicy.setFileNamePattern(createLoggerFileName(DEFAULT_ROOTLOGGER_TIMING, DEFAULT_LOGGING_FOLDER_NAME));
        timeTriggerPolicy.setParent(rfAppender);
        timeTriggerPolicy.setContext(loggerContext);

        timeTriggerPolicy.start();
        //

        // Маска для события по умолчанию для записи в лог
        PatternLayoutEncoder encoderFile = new PatternLayoutEncoder();
        encoderFile.setContext(loggerContext);
        encoderFile.setPattern(DEFAULT_ROOTLOGGER_FILE_PATTERN);
        encoderFile.setCharset(UTF_8);

        encoderFile.start();
        //

        //утановка события и форматтера на аппендер
        rfAppender.setEncoder(encoderFile);
        rfAppender.setRollingPolicy(timeTriggerPolicy);
        rfAppender.start();
        //

        // установка аппендера на логгер
        rootLogger.addAppender(rfAppender);

        //end of rollingAppender

        ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
        consoleAppender.setName("ConsoleAppender");
        consoleAppender.addFilter(new ThrowableFilter());

        PatternLayoutEncoder encoderConsole = new PatternLayoutEncoder();
        encoderConsole.setContext(loggerContext);
        encoderConsole.setPattern(DEFAULT_ROOTLOGGER_FILE_PATTERN);
        encoderFile.setCharset(UTF_8);

        encoderConsole.start();

        if (BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getBooleanProperty(AppConstants.CONSOLE_LOGGING, Boolean.TRUE)) {
            consoleAppender.setContext(loggerContext);
            consoleAppender.setTarget("System.out");
            consoleAppender.setEncoder(encoderConsole);
            consoleAppender.setName(APPENDER_CONSOLE_NAME);
            consoleAppender.start();
        }

        rootLogger.addAppender(consoleAppender);

        // установка логгеру уровня, сначала пишем все!
        rootLogger.setLevel(DAFAULT_ROOTLOGGER_LEVEL);

        if (S.isNotNullOrEmpty(messageAfterConfig.toString())) {
            rootLogger.info(messageAfterConfig.toString());
        }

        return rootLogger;
    }

    /**
     * Обновление уровня логирования
     * после входа
     */
    private void refreshLoggerLevel(Logger lgr) {
        if (userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_LEVEL) != null) {
            String levelString = (String) userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_LEVEL);

            try {
                Level level = Level.valueOf(levelString);
                lgr.setLevel(level);
            } catch (Throwable ex) {
                logger.error("Error on refreshLoggerLevel", ex);
            }
        }
    }

    private void refreshLoggerHistory(Logger rootLogger) {

        if (userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_HISTORY) != null) {
            String levelString = (String) userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_HISTORY);

            try {
                Integer levelInt = Integer.parseInt(levelString);
                setLoggerHistory(rootLogger, levelInt);
            } catch (NumberFormatException ex) {
                //setLoggerHistory(rootLogger, DAFAULT_ROOTLOGGER_HISTORY);
            }
        }
//        else {
//            setLoggerHistory(rootLogger, DAFAULT_ROOTLOGGER_HISTORY);
//        }
    }

    private void setLoggerHistory(Logger rootLogger, int history) {

        Appender appender = rootLogger.getAppender(APPENDER_FILE_NAME);
        if (appender instanceof RollingFileAppender) {
            RollingFileAppender fileAppender = (RollingFileAppender) appender;
            RollingPolicy policy = fileAppender.getRollingPolicy();
            if (policy instanceof TimeBasedRollingPolicy) {
                TimeBasedRollingPolicy timeBasedPolicy = (TimeBasedRollingPolicy) policy;
                timeBasedPolicy.setMaxHistory(history);
                timeBasedPolicy.start();
            }
        }
    }

    private void refreshLoggerPattern(Logger rootLogger) {

        if (userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_FILE_PATTERN) != null) {
            String patternString = (String) userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_FILE_PATTERN);

            try {
                setLoggerPattern(rootLogger, patternString);
            } catch (Throwable ex) {
                setLoggerPattern(rootLogger, DEFAULT_ROOTLOGGER_FILE_PATTERN);
            }
        } else {
            setLoggerPattern(rootLogger, DEFAULT_ROOTLOGGER_FILE_PATTERN);
        }
    }

    /**
     * До конфигурирование логгера, после логина в систему
     */
    public void refreshLoggerConfig() {

        try {

            userProperties = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_USER);
            globalProperties = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_GLOBAL);

            Logger rootLogger = (Logger) LoggerFactory.getLogger(DEFAULT_ROOTLOGGER_PATH);

            boolean disableLogForUser = false;
                    //JInvSecurityService.isCanAccessIsAction(BaseApp.APP().getCommonTaskContext(), 3730);
            boolean disableLogGlobal = false;
                    //! globalProperties.getBooleanProperty(SyslogProperties.USE_STDLOG, true);


            if (disableLogForUser || disableLogGlobal) {
                logger.info(":|");
                final File currentLogFile = getCurrentLogFile();
                if (currentLogFile != null) {
                    final Path logFilePath = currentLogFile.toPath();
                    rootLogger.detachAndStopAllAppenders();
                    if (Files.exists(logFilePath) && Files.isRegularFile(logFilePath)) {
                        try {
                            Files.delete(logFilePath);
                        } catch (IOException e) {
                            logger.error("Error on delete file: " + logFilePath.toString(), e);
                        }
                    }
                }
                return;
            }

            refreshLoggerLevel(rootLogger);

            refreshLoggerHistory(rootLogger);

            refreshLoggerFolderAndTiming(rootLogger);

            refreshLoggerPattern(rootLogger);

            initSyslog();

        } catch (Throwable ex) {
            logger.error("", ex);
        }
    }

    private class SyslogConfig {
       final String syslogHost;
       final String suffixPattern;
       final String facility;
       final Integer port;

        public SyslogConfig(String syslogHost, String suffixPattern, String facility, Integer port) {
            this.syslogHost = syslogHost;
            this.suffixPattern = suffixPattern;
            this.facility = facility;
            this.port = port;
        }

        @Override
        public String toString() {
            return "SyslogConfig{" +
                    "syslogHost='" + syslogHost + '\'' +
                    ", suffixPattern='" + suffixPattern + '\'' +
                    ", facility='" + facility + '\'' +
                    ", port=" + port +
                    '}';
        }
    }

    private void initSyslog() {
        Boolean syslogEnabled = globalProperties.getBooleanProperty(SyslogProperties.USE_SYSLOG, false);
        if (!syslogEnabled) {
            return;
        }
        String syslogHost = globalProperties.getStringProperty(SyslogProperties.SYSLOG_HOST,"127.0.0.1");
        String facility = globalProperties.getStringProperty(SyslogProperties.FACILITY, "local0");
        Integer port = globalProperties.getIntegerProperty(SyslogProperties.PORT,514);
        String suffixPattern = globalProperties.getStringProperty(SyslogProperties.SUFFIX_PATTERN);
        SyslogConfig config = new SyslogConfig(syslogHost, suffixPattern, facility, port);
//        logger.trace("Syslog config: {}", config);

        List<PSyslogEntry> packagesToSyslog = new ArrayList<>();

        RSUtils.<PSyslogEntry>createIterable(
                BaseApp.APP().getCommonTaskContext().getConnection(), PSyslogEntry.class, null)
                .forEach(packagesToSyslog::add);

        packagesToSyslog.forEach(p -> {
            LoggerLevelEnum logLevel = p.getLogLevel();

            if (logLevel != LoggerLevelEnum.OFF){
                addSyslogAppender(p.getCSLOG_LRNAME(), logLevel, config);
            } else {
                logger.debug("Skipped adding syslog appender for {} because log level is {}",
                        p.getCSLOG_LRNAME(), logLevel);
            }
        });
    }

    private static final String APPENDER_SYSLOG_NAME = "SyslogAppender";

    private void addSyslogAppender(String loggerPath, LoggerLevelEnum logLevel, SyslogConfig config) {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(DEFAULT_ROOTLOGGER_PATH);
        if (rootLogger == null){
            return;
        }

        boolean startsWithRuInversion = loggerPath.toLowerCase().startsWith(DEFAULT_ROOTLOGGER_PATH);
        if (!startsWithRuInversion) {
            loggerPath = DEFAULT_ROOTLOGGER_PATH + "." + loggerPath;
        }
        String appenderName = APPENDER_SYSLOG_NAME + "_" + loggerPath.toUpperCase().replace(".", "_");

        Logger otherLogger = (Logger) LoggerFactory.getLogger(loggerPath.toLowerCase());

        if (otherLogger.getAppender(appenderName) != null){
            //если такой аппендер уже есть, ничего не делаем
            logger.debug("Skipped adding syslog appender for {} because it already exists", loggerPath);
            return;
        }

        SyslogAppender syslogAppender = new SyslogAppender();
        syslogAppender.setSyslogHost(config.syslogHost); //ip
        syslogAppender.setPort(config.port); //порт UDP, 514 по умолчанию
        syslogAppender.setFacility(config.facility);
        syslogAppender.setName(appenderName);
        if (S.isNotNullOrEmpty(config.suffixPattern)){
            syslogAppender.setSuffixPattern(config.suffixPattern);
        }
//      $$ Commented for using with SGB Syslog Server
//        syslogAppender.setCharset(UTF_8);
        
        syslogAppender.setContext(otherLogger.getLoggerContext());

        otherLogger.addAppender(syslogAppender);

        logger.debug("Added syslog appender {} ({}) to path {}", appenderName, logLevel.name(), loggerPath);

        try {
            Level level = Level.valueOf(logLevel.name());
            otherLogger.setLevel(level);
        } catch (Throwable ex) {
            logger.error("Error on log level parse", ex);
        }

        syslogAppender.start();
    }

    private void setLoggerPattern(Logger logger, String pattern) {
        Appender appender = logger.getAppender(APPENDER_FILE_NAME);
        if (appender instanceof RollingFileAppender) {
            RollingFileAppender fileAppender = (RollingFileAppender) appender;
            initAppender(logger, fileAppender, pattern);
        }

        Appender console = logger.getAppender(APPENDER_CONSOLE_NAME);
        if (console instanceof ConsoleAppender && BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getBooleanProperty(AppConstants.CONSOLE_LOGGING, Boolean.TRUE)) {
            ConsoleAppender consoleAppender = (ConsoleAppender) console;
            initAppender(logger, consoleAppender, pattern);
        }
    }

    private void initAppender(Logger logger, OutputStreamAppender appender, String pattern) {
        if (appender.getEncoder() != null && appender.getEncoder() instanceof PatternLayoutEncoder) {
            final PatternLayoutEncoder encoder = (PatternLayoutEncoder) appender.getEncoder();
            encoder.setContext(logger.getLoggerContext());
            encoder.setPattern(pattern);
            encoder.setCharset(UTF_8);
            try {
                encoder.start();
            } catch (Throwable ex) {
                encoder.setPattern(DEFAULT_ROOTLOGGER_FILE_PATTERN);
                encoder.start();
            }
            appender.setEncoder(encoder);
            appender.start();
        }
    }

    /**
     * Признак для RollingAppender когда нужно делать новый файл
     */
    private void refreshLoggerFolderAndTiming(Logger rootLogger) {

        String timing = DEFAULT_ROOTLOGGER_TIMING;
        String folder = DEFAULT_LOGGING_FOLDER_NAME;

        if (userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_TIMING) != null) {
            String timingString = (String) userProperties.getProperty(PROPERTY_DEFAULT_ROOTLOGGER_TIMING);

            try {
                SimpleDateFormat timingDateFormat = new SimpleDateFormat(timingString);
                timing = timingString;
            } catch (IllegalArgumentException | NullPointerException ignored) {
            }
        }

        setLoggerFolderAndTiming(rootLogger, timing, folder);
    }

    private void setLoggerFolderAndTiming(Logger logger, String timing, String folderName) {

        Appender appender = logger.getAppender(APPENDER_FILE_NAME);
        if (appender instanceof RollingFileAppender) {
            RollingFileAppender fileAppender = (RollingFileAppender) appender;
            RollingPolicy policy = fileAppender.getRollingPolicy();
            if (policy instanceof TimeBasedRollingPolicy) {
                TimeBasedRollingPolicy timeBasedPolicy = (TimeBasedRollingPolicy) policy;
                timeBasedPolicy.setFileNamePattern(createLoggerFileName(timing, folderName));
                timeBasedPolicy.start();
            }
        }
    }

    private final static class ThrowableFilter extends Filter<ILoggingEvent> {

        private Throwable previousException;

        @Override
        public FilterReply decide(ILoggingEvent event) {
            IThrowableProxy throwableProxy = event.getThrowableProxy();
            if (throwableProxy instanceof ThrowableProxy) {
                ThrowableProxy throwableProxyImpl = (ThrowableProxy) throwableProxy;
                Throwable ex = throwableProxyImpl.getThrowable();
                if (ex.equals(previousException)) {
                    return FilterReply.DENY;
                } else {
                    previousException = ex;
                    return FilterReply.ACCEPT;
                }
            }
            return FilterReply.ACCEPT;
        }
    }
}
