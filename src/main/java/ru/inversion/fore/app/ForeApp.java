package ru.inversion.fore.app;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.tc.TaskContext;

import java.util.Locale;
import java.util.ResourceBundle;

public abstract class ForeApp extends Application {

   public static final Locale DEFAULT_LOCALE = new Locale("ru");

   private static final ResourceBundle FORE_BUNDLE =
           ResourceBundle.getBundle("fore");

   protected ViewContext primaryViewContext;

   public abstract String getAppID();

   public abstract TaskContext getCommonTaskContext();

   public Stage getPrimaryStage() {
      return primaryViewContext == null
              ? null
              : primaryViewContext.getStage();
   }

   public ViewContext getPrimaryViewContext() {
      return primaryViewContext;
   }

   public ResourceBundle getCommonResourceBundle() {
      return FORE_BUNDLE;
   }
}