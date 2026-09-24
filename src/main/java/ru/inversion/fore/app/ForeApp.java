package ru.inversion.fore.app;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.inversion.fore.app.properties.BootstrapProperties;

/**
 * Базовый JavaFX runtime приложения Fore.
 */
public abstract class ForeApp extends Application {

   private BootstrapProperties bootstrapProperties;
   private Stage primaryStage;


   /**
    * Инициализация runtime до запуска JavaFX UI.
    */
   @Override
   public final void init() throws Exception
   {
      bootstrapProperties = BootstrapProperties.create( getClass(), getParameters().getNamed() );

      initApplication();
   }


   /**
    * Дополнительная инициализация приложения.
    *
    * Вызывается после создания BootstrapProperties.
    */
   protected void initApplication() throws Exception
   {
   }


   /**
    * Запуск JavaFX приложения.
    */
   @Override
   public final void start( Stage primaryStage ) throws Exception
   {
      this.primaryStage = primaryStage;

      startApplication();
   }


   /**
    * Запуск конкретного приложения.
    */
   protected abstract void startApplication() throws Exception;


   /**
    * Bootstrap-свойства приложения.
    */
   public final BootstrapProperties getBootstrapProperties()
   {
      return bootstrapProperties;
   }


   /**
    * Primary Stage приложения.
    */
   public final Stage getPrimaryStage()
   {
      return primaryStage;
   }


   /**
    * Завершение runtime.
    */
   @Override
   public final void stop() throws Exception
   {
      try
      {
         stopApplication();
      }
      finally
      {
         if( bootstrapProperties != null )
         {
            bootstrapProperties.close();
            bootstrapProperties = null;
         }

         primaryStage = null;
      }
   }


   /**
    * Дополнительное завершение приложения.
    */
   protected void stopApplication() throws Exception
   {
   }
}