package ru.inversion.fore.form;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import ru.inversion.fore.form.validation.*;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Checks;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public abstract class FormController<T> implements Initializable {

   private FormContext<T> formContext;

   private Consumer<FormResult<T>> resultHandler;

   private Consumer<? super Throwable> errorHandler;

   private boolean completed;

   private final StringProperty titleProperty = new SimpleStringProperty( this, "title");

   private final FormValidation validation = new FormValidation();

   private ControlValidationSupport validationSupport;

   private final ValidationPresenter validationPresenter = new ValidationPresenter();

   /** */
   final boolean preInitController(
           FormContext<T> context,
           Consumer<FormResult<T>> resultHandler,
           Consumer<? super Throwable> errorHandler ) throws Exception
   {
      this.formContext   = Checks.Require.object(context, "context");
      this.resultHandler = resultHandler;
      this.errorHandler  = Checks.Require.object(errorHandler, "errorHandler");

      return preInit();
   }


   @Override
   public final void initialize( URL location, ResourceBundle resources )
   {
      try {
         init();
      }
      catch( Exception ex ) {
         throw new FormException( "Controller initialization error", ex );
      }
   }


   final void guiInitController() throws Exception
   {
      final Window window = formContext.window();

      if( window instanceof Stage stage )
         stage.titleProperty() .bindBidirectional(titleProperty);

      guiInit();

      if( requiresValidation() )
      {
         validationSupport = new ControlValidationSupport( validation, validationPresenter,this::handleInteractiveValidationError );
         validationSupport.install();
      }
   }

   /**
    * Interactive validation runs inside JavaFX listeners, outside the close-request
    * handler. Route failures through the same launcher error sink, never rethrow
    * directly from a focus listener.
    */
   final void handleInteractiveValidationError(Exception ex)
   {
      errorHandler.accept(new FormException("Control validation error", ex));
   }

   protected boolean preInit() throws Exception
   {
      return true;
   }


   protected void init() throws Exception
   {
   }


   protected void guiInit() throws Exception
   {
   }


   protected final FormContext<T> formContext()
   {
      return formContext;
   }

   /** */
   public final FormMode getFormMode()
   {
      return formContext().mode();
   }

   public final TaskContext getTaskContext()
   {
      return formContext.taskContext();
   }


   public final Window getWindow()
   {
      return formContext.window();
   }


   public final Window getOwner()
   {
      return formContext.owner();
   }


   public final T getDataObject()
   {
      return formContext.dataObject();
   }


   public final ResourceBundle getBundle()
   {
      return formContext.bundle();
   }


   public final Map<String, Object> getParameters()
   {
      return formContext.parameters();
   }


   public final <V> V getParameter(String name)
   {
      return formContext.parameter(name);
   }


   /** */
   protected final FormValidation validation()
   {
      return validation;
   }

   private boolean validateForm() throws Exception
   {
      if( !requiresValidation() )
         return true;

      final ValidationResult result = validation.validate();

      validationPresenter.clearAll();

      if( result.valid() )
         return true;

      validationPresenter.show(result);
      handleValidationFailure(result);

      return false;
   }


   /** */
   private boolean requiresValidation()
   {
      return switch( getFormMode() )
      {
         case DEFAULT, INSERT, EDIT   -> true;
         default -> false;
      };
   }

   /** */
   protected void handleValidationFailure( ValidationResult result )
   {
      final ValidationFailure failure = result.failures().getFirst();

      if( failure.target() != null )
      {
         failure.target().requestFocus();
         return;
      }

      // A form-level validator may have no Control target. Do not silently
      // reject OK without explaining the failure to the user.
      showValidationMessage(failure.message());
   }

   /**
    * Default presentation for untargeted FormValidator failures.
    * Applications can override this hook to use their own dialog service.
    */
   protected void showValidationMessage(String message)
   {
      final Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
      alert.initOwner(getWindow());
      alert.setHeaderText(null);
      alert.show();
   }

   public final StringProperty titleProperty()
   {
      return titleProperty;
   }


   public final void setTitle(String title)
   {
      titleProperty.set(title);
   }


   public final String getTitle()
   {
      return titleProperty.get();
   }


   private FormResultType requestedResult = FormResultType.CANCEL;

   private FormResultType result = FormResultType.CANCEL;


   /** */
   protected final void close(FormResultType result)
   {
      final FormResultType requested = Checks.Require.object( result, "result");

      if( Platform.isFxApplicationThread() )
         requestClose(requested);
      else
         Platform.runLater(() -> requestClose(requested));
   }

   private void requestClose(FormResultType result)
   {
      requestedResult = result;

      final Window window = formContext.window();

      Event.fireEvent(
              window,
              new WindowEvent(
                      window,
                      WindowEvent.WINDOW_CLOSE_REQUEST
              )
      );
   }


   final void completeController()
   {
      if( completed )
         return;

      completed = true;

      if( resultHandler != null )
      {
         resultHandler.accept(
                 new FormResult<>(
                         result,
                         formContext.dataObject(),
                         formContext.window()
                 )
         );
      }
   }

   /** */
   final void handleCloseRequest(WindowEvent event)
   {
      final FormResultType tempRequest = requestedResult;

      requestedResult = FormResultType.CANCEL;

      try
      {
         if( !processCloseRequest(tempRequest) )
            event.consume();
      }
      catch( Exception ex )
      {
         event.consume();
         throw new FormException( "Error processing form close request", ex  );
      }
   }

   /**
    * Close decision is independent of the WindowEvent so its lifecycle rules
    * can be tested without creating a JavaFX Stage.
    */
   final boolean processCloseRequest(FormResultType request) throws Exception
   {
      final boolean allowClose = switch( request )
      {
         case OK -> {
            if( !validateForm() )
               yield false;

            yield onOK();
         }
         case CANCEL -> onCancel();
      };

      if( allowClose )
         result = request;

      return allowClose;
   }

   /** */
   private boolean released;

   final synchronized void releaseController() throws Exception
   {
      if( released )
          return;

      released = true;

      closeResources();
   }
   /** */
   protected void closeResources( ) throws Exception
   {
   }

   protected boolean onOK() throws Exception
   {
      return true;
   }

   protected boolean onCancel() throws Exception
   {
      return true;
   }

   protected final void ok()
   {
      close(FormResultType.OK);
   }

   protected final void cancel()
   {
      close(FormResultType.CANCEL);
   }

   protected final void close()
   {
      cancel();
   }

}