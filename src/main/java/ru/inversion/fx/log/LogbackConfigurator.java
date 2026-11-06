package ru.inversion.fx.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;
import ru.inversion.utils.U;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Утилита для динамического управления конфигурацией Logback в рантайме.
 * Позволяет менять уровень логирования, формат, путь к файлам,
 * политику ротации и полностью отключать логирование.
 */
public class LogbackConfigurator {


   /** Фильтр от повторяющихся исключений */
   private final static class ThrowableFilter<E> extends Filter<E> {

      private Throwable previousException;

      @Override
      public FilterReply decide( E e )
      {
         if( !(e instanceof ILoggingEvent) )
            return FilterReply.ACCEPT;

         final ILoggingEvent event = (ILoggingEvent)e;

         final IThrowableProxy throwableProxy = event.getThrowableProxy();

         if( throwableProxy instanceof ThrowableProxy )
         {
            ThrowableProxy throwableProxyImpl = (ThrowableProxy) throwableProxy;
            Throwable ex = throwableProxyImpl.getThrowable();
            if( ex.equals(previousException) )
               return FilterReply.DENY;
            else
            {
               previousException = ex;
               return FilterReply.ACCEPT;
            }
         }
         return FilterReply.ACCEPT;
      }
   }

   private final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

   public LogbackConfigurator() {
      Runtime.getRuntime().addShutdownHook( new Thread(context::stop, "StopLoggerThread")); // Хук на отключение логера
   }

   /**
    * Применяет комплексную конфигурацию логирования в рантайме.
    * Все изменения вступают в силу атомарно.
    *
    * @param newConfig объект с новыми настройками
    */
   synchronized public void applyConfiguration( RuntimeLogConfig newConfig )
   {
      //  Настройка корневого логгера
      Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

      final Level newLevel = newConfig.isLoggingEnabled() ? newConfig.getLogLevel() : Level.OFF;
      rootLogger.setLevel(newLevel);

      // Настройка основного файлового аппендера (FILE)
      reconfigureFileAppender( LogManager.FILE_APPENDER, newConfig );

      //  Настройка аппендера ошибок (ERROR_FILE) — только путь и ротация
      reconfigureFileAppender( LogManager.ERROR_FILE_APPENDER, newConfig);

      configConsole( newConfig );

      // 4. Вывод статуса для отладки
      if( newConfig.isDebugStatus() )
          StatusPrinter.print(context);
   }

   /** */
   private void configConsole( RuntimeLogConfig newConfig )
   {
      if( System.console() == null || !newConfig.isEnableConsole() ) {
         Appender<ILoggingEvent> appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("CONSOLE");
         if( appender == null )
            return;
         context.getLogger(Logger.ROOT_LOGGER_NAME).detachAppender(appender);
      }
   }

   /** */
   private void reconfigureFileAppender(String appenderName, RuntimeLogConfig newConfig) {

      RollingFileAppender<?> appender = (RollingFileAppender<?>) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(appenderName);
      if( appender == null )
          return;

      /*
      // Если путь не изменился, не останавливаем аппендер
      String newFilePath = determineLogFilePath(appenderName, newConfig)
              .normalize().toAbsolutePath().toString();
      String oldFilePath = appender.getFile();
      boolean pathChanged = !Objects.equals(newFilePath, oldFilePath);

      if (pathChanged || !appender.isStarted()) {
         appender.stop();
         appender.setFile(newFilePath);
      }
      */
      appender.stop();

      // Меняем путь к файлу
      Path logFilePath = determineLogFilePath(appenderName, newConfig);
      ensureDirectoryExists(logFilePath);
      appender.setFile(logFilePath.normalize().toAbsolutePath().toString());

      // ротация
      if (appender.getRollingPolicy() instanceof SizeAndTimeBasedRollingPolicy) {
         @SuppressWarnings("unchecked")
         SizeAndTimeBasedRollingPolicy<Object> policy = (SizeAndTimeBasedRollingPolicy<Object>) appender.getRollingPolicy();

         // Не вызываем policy.stop() — это вызывает проверку порядка полей
         // Вместо этого создаём новую политику или используем setter'ы без stop/start

         policy.setMaxHistory(newConfig.getMaxHistory());

         try {
            policy.setMaxFileSize(FileSize.valueOf(newConfig.getMaxFileSize().trim()));
         } catch (Exception e) {
            System.err.println("Invalid maxFileSize: " + newConfig.getMaxFileSize());
         }

         try {
            policy.setTotalSizeCap(FileSize.valueOf(newConfig.getTotalSizeCap().trim()));
         } catch (Exception e) {
            System.err.println("Invalid totalSizeCap: " + newConfig.getTotalSizeCap());
         }

         String fileNamePattern = newConfig.getLogDirectory()
                 .resolve(makeFileNamePatternForAppender(appenderName, newConfig.getAppName()))
                 .normalize().toAbsolutePath().toString();
         policy.setFileNamePattern(fileNamePattern);

         // Не вызываем policy.start()
      }

      // Перенастройка шаблона
      if( appender.getEncoder() instanceof PatternLayoutEncoder )
      {
         PatternLayoutEncoder encoder = (PatternLayoutEncoder) appender.getEncoder();
         encoder.stop();
         encoder.setPattern(newConfig.getLogPattern());
         encoder.start();
      }

      // Фильтр
      if (!appender.getCopyOfAttachedFiltersList().stream().anyMatch(f -> f instanceof ThrowableFilter))
         appender.addFilter(new ThrowableFilter());

      // Запускаем
      appender.start();
   }

   private Path determineLogFilePath(String appenderName, RuntimeLogConfig config) {
      String baseName = appenderName.equals(LogManager.FILE_APPENDER)
              ? config.getAppName() + ".log"
              : config.getAppName() + ".error.log";
      return config.getLogDirectory().resolve(baseName);
   }

   /** */
   private static String makeFileNamePatternForAppender(String appenderName, String appName) {
      return appenderName.equals(LogManager.FILE_APPENDER) ? appName + ".%d{yyyy-MM-dd}.%i.log.gz" : appName + ".error.%d{yyyy-MM-dd}.%i.log.gz";
   }

   private void ensureDirectoryExists( Path filePath ) {
      Path parent = filePath.getParent();
      if (parent != null && !Files.exists(parent)) {
         try {
            Files.createDirectories(parent);
         } catch (Exception e) {
            System.err.println("Failed to create log directory: " + parent + " - " + e.getMessage());
         }
      }
   }

   /**
    * Возвращает абсолютный путь к директории, в которой хранятся файлы логов.
    * @return путь к папке с логами или null, если аппендер не найден
    */
   public Path getLogDirectoryPath() {
      Path logFilePath = getLogFilePath();
      if (logFilePath != null) {
         Path parent = logFilePath.getParent();
         return parent != null ? parent.toAbsolutePath().normalize() : null;
      }
      return null;
   }

   /**
    * Возвращает путь к текущему файлу лога.
    * Работает как с прямым RollingFileAppender, так и через AsyncAppender.
    *
    * @return Path к файлу лога или null, если файловый аппендер не найден
    */
   public Path getLogFilePath() {

      Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

      //  Пробуем получить ASYNC аппендер (который прикреплён к root)
      Appender<ILoggingEvent> asyncAppender = rootLogger.getAppender(LogManager.ASYNC_APPENDER);

      if (asyncAppender == null)
         // Если ASYNC нет, пробуем напрямую FILE (на случай, если конфигурация изменилась)
         return getFilePathFromAppender(rootLogger.getAppender(LogManager.FILE_APPENDER));

      // 2. Если это AsyncAppender, извлекаем из него обёрнутый аппендер
      if(asyncAppender instanceof ch.qos.logback.classic.AsyncAppender) {
         ch.qos.logback.classic.AsyncAppender async = (ch.qos.logback.classic.AsyncAppender) asyncAppender;

         // У AsyncAppender есть метод getAppender(), который возвращает обёрнутый аппендер
         Appender<ILoggingEvent> wrappedAppender = async.getAppender(LogManager.FILE_APPENDER);

         return getFilePathFromAppender(wrappedAppender);
      }

      return null;
   }

   /**
    * Вспомогательный метод для извлечения пути из файлового аппендера
    */
   private Path getFilePathFromAppender(Appender<ILoggingEvent> appender) {

      if( appender == null)
         return null;

      if(appender instanceof RollingFileAppender) {
         RollingFileAppender<?> fileAppender = (RollingFileAppender<?>) appender;
         String filePath = fileAppender.getFile();
         return filePath != null ? Paths.get(filePath) : null;
      }

      if (appender instanceof ch.qos.logback.core.FileAppender) {
         ch.qos.logback.core.FileAppender<?> fileAppender = (ch.qos.logback.core.FileAppender<?>) appender;
         String filePath = fileAppender.getFile();
         return filePath != null ? Paths.get(filePath) : null;
      }
      return null;
   }

   /**
    * Полностью отключает всё логирование (устанавливает Level.OFF для root).
    */
   public void disableAllLogging() {
      Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.OFF);
   }

   /**
    * Восстанавливает стандартный уровень INFO (или заданный по умолчанию).
    */
   public void enableDefaultLogging() {
      Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
      rootLogger.setLevel(Level.INFO);
   }


   /**
    * Принудительно загружает fore-logback.xml из JInvFore JAR.
    * Вызвать ДО любого обращения к LoggerFactory.
    */
   static void forceLoadLogbackConfig()
   {
      final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

      // Сбрасываем ранее загруженную конфигурацию
      context.reset();

      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);

      // Загружаем fore-logback.xml из classpath
      ClassLoader classLoader = LogManager.class.getClassLoader();
      java.io.InputStream configStream = classLoader.getResourceAsStream("fore-logback.xml");

      if(configStream == null) {
         System.err.println("!!! fore-logback.xml not found in classpath !!!");
         return;
      }

      try {
         configurator.doConfigure(configStream);
         System.out.println("Logback configured with fore-logback.xml from JInvFore");
      } catch (JoranException e) {
         System.err.println("Failed to configure Logback: " + e.getMessage());
         // В случае ошибки выводим статус для диагностики
         ch.qos.logback.core.util.StatusPrinter.print(context);
      }
   }


}