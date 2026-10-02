package ru.inversion.fore.form;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import ru.inversion.fore.form.validation.ControlValidationSupport;
import ru.inversion.fore.form.validation.FormValidation;
import ru.inversion.fore.form.validation.ValidationFailure;
import ru.inversion.fore.form.validation.ValidationResult;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Checks;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public abstract class FormController<T> implements Initializable {

   private FormContext<T> formContext;

   private Consumer<FormResult<T>> resultHandler;

   private boolean completed;

   private final StringProperty titleProperty = new SimpleStringProperty( this, "title");

   private final FormValidation validation = new FormValidation();

   private ControlValidationSupport validationSupport;

   /** */
   final boolean preInitController( FormContext<T> context, Consumer<FormResult<T>> resultHandler ) throws Exception
   {
      this.formContext   = Checks.Require.object(context, "context");
      this.resultHandler = resultHandler;

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
         stage.titleProperty()
                 .bindBidirectional(titleProperty);

      guiInit();

      validationSupport = new ControlValidationSupport( validation, this::handleInteractiveValidationError );

      validationSupport.install();
   }

   private void handleInteractiveValidationError(Exception ex)
   {
      throw new FormException(
              "Control validation error",
              ex
      );
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

      final ValidationResult result =
              validation.validate();

      if( result.valid() )
         return true;

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
          failure.target().requestFocus();
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
         final boolean allowClose =
                 switch( tempRequest )
                 {
                    case OK -> {

                       if( !validateForm() )
                           yield false;

                       yield onOK();
                    }

                    case CANCEL -> onCancel();
                 };

         if( !allowClose )
         {
            event.consume();
            return;
         }

         result = tempRequest;
      }
      catch( Exception ex )
      {
         event.consume();
         throw new FormException( "Error processing form close request", ex  );
      }
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