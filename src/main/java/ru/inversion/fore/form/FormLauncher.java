package ru.inversion.fore.form;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ru.inversion.fore.ForeException;
import ru.inversion.tc.TaskContext;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public final class FormLauncher<T, C extends FormController<T>> {

   private final TaskContext taskContext;

   private final Window      owner;

   private final Class<C>    controllerClass;

   private T dataObject;

   private Map<String, Object> parameters = Map.of();

   private Object bundle;

   private String fxmlPath;

   private boolean modal;

   private Consumer< FormResult<T> > controllerCallback;

   private FormController<?> parentController;

   /** */
   public FormLauncher( TaskContext taskContext, Window owner, Class<C> controllerClass ) {
      this.taskContext     = taskContext;
      this.owner           = owner;
      this.controllerClass = Objects.requireNonNull(controllerClass);
   }

   /** */
   public FormLauncher( FormController<?> parentController, Class<C> controllerClass )
   {
      this (
         parentController.getTaskContext(),
         parentController.getWindow(),
         controllerClass
      );

      this.parentController = parentController;
   }

   /** */
   public FormLauncher<T, C> dataObject( T dataObject )
   {
      this.dataObject = dataObject;
      return this;
   }

   /** */
   public FormLauncher<T, C> parameters( Map<String, Object> parameters )
   {
      this.parameters = parameters == null ? Map.of() : new HashMap<>(parameters);
      return this;
   }

   /**
    * Использовать явно переданный ResourceBundle.
    */
   public FormLauncher<T, C> bundle( ResourceBundle bundle )
   {
      this.bundle = bundle; return this;
   }

   public FormLauncher<T, C> bundle( String bundleName )
   {
      this.bundle = bundleName; return this;
   }


   /**
    * Переопределить convention-based путь к FXML.
    */
   public FormLauncher<T, C> fxml( String fxmlPath )
   {
      this.fxmlPath = fxmlPath; return this;
   }

   /** */
   public FormLauncher<T, C> modal(boolean modal )
   {
      this.modal = modal; return this;
   }

   /** */
   public FormLauncher<T, C> callback( Consumer<FormResult<T>> callback )
   {
      this.controllerCallback = callback; return this;
   }


   /**
    * Запускает форму.
    */
   public void runForm( )
   {
      Thread.startVirtualThread(this::prepareForm);
   }

   /** */
   private void prepareForm()
   {
      try
      {
         final ResourceBundle resolvedBundle = resolveBundle();
         final URL fxml = resolveFxml();

         final C controller = controllerClass.getDeclaredConstructor().newInstance();

         final FormContextImpl<T> context =
              new FormContextImpl<>(
                   taskContext,
                   owner,
                   dataObject,
                   parameters,
                   resolvedBundle,
                   parentController
              );

         if( !controller.preInitController( context, controllerCallback ))
             return;

         Platform.runLater(() ->runInternal( controller, context, fxml, resolvedBundle ) );
      }
      catch( Throwable ex ) {
         handleLaunchError(ex);
      }
   }


   /** */
   private void runInternal( C controller, FormContextImpl<T> context, URL fxml, ResourceBundle resolvedBundle )
   {
      try
      {
         /*
          * Controller уже создан и прошёл preInit().
          * <>
          * FXMLLoader должен использовать именно этот экземпляр,
          * а не создавать новый.
          */
         final FXMLLoader loader = new FXMLLoader(fxml, resolvedBundle);
         loader.setControllerFactory(type -> {
            if( type == controllerClass )
                return controller;
            try {
               return type.getDeclaredConstructor().newInstance();
            }
            catch( Exception ex ) {
               throw new FormException("Unable to create FXML controller: "+ type.getName(),ex);
            }
         });


         /*
          * Здесь FXMLLoader:
          *
          * 1. создаёт все FXML-компоненты;
          * 2. выполняет @FXML injection;
          * 3. вызывает FormController.initialize();
          * 4. initialize() вызывает наш init().
          *
          * Всё выполняется на FX Application Thread.
          */
         final Parent root = loader.load();


         /*
          * Дополнительная проверка, что FXML действительно
          * использовал ожидаемый экземпляр controller.
          */
         final C loadedController = loader.getController();

         if( loadedController == null )
         {
            throw new FormLaunchException(
                    controllerClass,
                    "FXML controller is not defined: " + fxml,
                    "FXMLLoader.getController() returned null"
            );
         }

         if( loadedController != controller )
         {
            throw new FormLaunchException(
                    controllerClass,
                    "Unexpected FXML controller instance: "
                            + loadedController.getClass().getName(),
                    "FXMLLoader did not use the controller instance prepared by FormLauncher"
            );
         }


         /*
          * После FXML создаём полноценное окно.
          *
          * После createStage():
          *   Scene есть;
          *   Stage есть;
          *   owner установлен;
          *   modality установлена.
          */
         final Stage stage = createStage(root);


         /*
          * С этого момента window() становится доступен
          * через тот же FormContext, который controller
          * получил ещё перед preInit().
          */
         context.setWindow(stage);


         /*
          * Framework handler не должен занимать setOnHidden(),
          * чтобы пользовательский controller мог установить
          * собственный handler.
          */
         stage.addEventHandler(
                 WindowEvent.WINDOW_HIDDEN,
                 event -> controller.completeController()
         );


         /*
          * Последняя фаза инициализации.
          *
          * Здесь:
          *   FX Application Thread        +
          *   @FXML fields                 +
          *   Scene                        +
          *   Stage / Window               +
          *   owner                        +
          *
          * Но окно ещё не показано.
          */
         controller.guiInitController();


         /*
          * И только после полного lifecycle показываем форму.
          */
         if( modal )
            stage.showAndWait();
         else
            stage.show();
      }
      catch( ForeException ex )
      {
         throw ex;
      }
      catch( Exception ex )
      {
         throw new FormLaunchException(
                 controllerClass,
                 "Error on form launch",
                 ex,
                 "FXML: " + fxml
         );
      }
   }

   /** */
   private Stage createStage( Parent root )
   {
      final Stage stage = new Stage();

      if( owner != null )
          stage.initOwner(owner);

      if( modal )
          stage.initModality( owner == null ? Modality.APPLICATION_MODAL : Modality.WINDOW_MODAL );

      stage.setScene( new Scene(root) );

      return stage;
   }

   /** */
   private URL resolveFxml()
   {
      final String path  = fxmlPath != null ? fxmlPath : FormTools.defaultFxmlPath(controllerClass);
      final URL resource = controllerClass.getClassLoader().getResource(path);

      if( resource == null )
          throw new IllegalArgumentException( "FXML resource not found: " + path );

      return resource;
   }

   /** */
   private ResourceBundle resolveBundle()
   {
      return switch (bundle) {
         case null -> loadBundle(FormTools.defaultBundleName(controllerClass));
         case ResourceBundle b -> b;
         case String name -> loadBundle(name);
         default ->
            throw new IllegalStateException("Unsupported bundle object: " + bundle.getClass().getName());
      };
   }

   /** */
   private ResourceBundle loadBundle( String name )
   {
      return ResourceBundle.getBundle( name, Locale.getDefault(), controllerClass.getClassLoader() );
   }

   private void handleLaunchError(Throwable error)
   {
      Platform.runLater(() -> {
         throw error instanceof RuntimeException runtime
                 ? runtime
                 : new FormException(
                 "Form launch error",
                 error
         );
      });
   }

}

