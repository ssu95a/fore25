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
import ru.inversion.fore.form.impl.FormContextImpl;
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

   /** */
   public FormLauncher( TaskContext taskContext, Window owner, Class<C> controllerClass ) {
      this.taskContext     = taskContext;
      this.owner           = owner;
      this.controllerClass = Objects.requireNonNull(controllerClass);
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
      if( Platform.isFxApplicationThread() )
          showInternal();
      else
          Platform.runLater( this::showInternal );
   }

   /** */
   private void showInternal( )
   {
      try
      {
         final ResourceBundle resolvedBundle = resolveBundle();
         final URL            fxml           = resolveFxml();

         final FXMLLoader loader = new FXMLLoader( fxml, resolvedBundle);

         final Parent root  = loader.load();

         final C controller = loader.getController();

         if( controller == null )
             throw new FormLaunchException( controllerClass, "FXML controller is not defined: " + fxml, "'FXMLLoader.getController()' return null" );

         if( !controllerClass.isInstance(controller) )
            throw new IllegalStateException (
               "Unexpected FXML controller. Expected " + controllerClass.getName() + ", actual " + controller.getClass().getName()
            );

         final Stage stage = createStage(root);

/*
      TaskContext taskContext,
      Window owner,
      T dataObject,
      Map<String, Object> parameters,
      ResourceBundle bundle,
      FormController<?> parentController

 */

         final FormContext<T> context =
            new FormContextImpl<>(
               taskContext,
               owner,
               dataObject,
               parameters,
               resolvedBundle,
               null
            );

         controller.preInitController( context, controllerCallback );

         stage.addEventHandler(
                 WindowEvent.WINDOW_SHOWING,
                 e -> {
                    try {
                       controller.guiInitController();
                    } catch (Exception ex) {
                       throw new RuntimeException(ex);
                    }
                 }
         );

         stage.addEventHandler( WindowEvent.WINDOW_HIDDEN, event -> controller.completeController() );

         if( modal )
             stage.showAndWait();
         else
             stage.show();
      }
      catch( ForeException fex) {
         throw fex;
      }
      catch( Exception ex ) {
         throw new FormLaunchException( controllerClass, "Error on launch", ex, null );
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

}

