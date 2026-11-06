package ru.inversion.fx.log;

import ch.qos.logback.classic.Level;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RuntimeLogConfig {

   private boolean loggingEnabled = true;

   private Level  logLevel     = Level.INFO;
   private Path   logDirectory = Paths.get( System.getProperty("user.home") , "jfx-logs" );
   private String appName      = "fore";
   private String logPattern   = "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n";
   private int    maxHistory   = 14;
   private String maxFileSize  = "10MB";
   private String totalSizeCap = "1GB";

   private boolean debugStatus = false;

   private boolean enableConsole = true;

   public boolean isLoggingEnabled() {
      return loggingEnabled;
   }

   public void setLoggingEnabled(boolean loggingEnabled) {
      this.loggingEnabled = loggingEnabled;
   }

   public Level getLogLevel() {
      return logLevel;
   }

   public void setLogLevel(Level logLevel) {
      this.logLevel = logLevel;
   }

   public Path getLogDirectory() {
      return logDirectory;
   }

   public void setLogDirectory(Path logDirectory) {
      this.logDirectory = logDirectory;
   }

   public String getAppName() {
      return appName;
   }

   public void setAppName(String appName) {
      this.appName = appName;
   }

   public String getLogPattern() {
      return logPattern;
   }

   public void setLogPattern(String logPattern) {
      this.logPattern = logPattern;
   }

   public int getMaxHistory() {
      return maxHistory;
   }

   public void setMaxHistory(int maxHistory) {
      this.maxHistory = maxHistory;
   }

   public String getMaxFileSize() {
      return maxFileSize;
   }

   public void setMaxFileSize(String maxFileSize) {
      this.maxFileSize = maxFileSize;
   }

   public String getTotalSizeCap() {
      return totalSizeCap;
   }

   public void setTotalSizeCap(String totalSizeCap) {
      this.totalSizeCap = totalSizeCap;
   }

   public boolean isDebugStatus() {
      return debugStatus;
   }

   public void setDebugStatus(boolean debugStatus) {
      this.debugStatus = debugStatus;
   }

   public boolean isEnableConsole() {
      return enableConsole;
   }

   public void setEnableConsole(boolean enableConsole) {
      this.enableConsole = enableConsole;
   }
}