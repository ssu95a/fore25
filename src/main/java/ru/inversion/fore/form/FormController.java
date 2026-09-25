package ru.inversion.fore.form;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.Event;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import ru.inversion.tc.TaskContext;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public abstract class FormController<T> implements Initializable {

   private FormContext<T> formContext;

   private Consumer<FormResult<T>> resultHandler;

   private boolean completed;

   private final StringProperty titleProperty = new SimpleStringProperty( this, "title");

   /** */
   final boolean preInitController( FormContext<T> context, Consumer<FormResult<T>> resultHandler ) throws Exception
   {
      this.formContext   = Objects.requireNonNull(context);
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
      requestedResult = Objects.requireNonNull(result);

      final Window window = formContext.window();

      Event.fireEvent(
              window,
              new WindowEvent(
                      window,
                      WindowEvent.WINDOW_CLOSE_REQUEST
              )
      );
   }

   /** */
   final void handleCloseRequest(WindowEvent event)
   {
      final FormResultType tempRequest = requestedResult;

      /*
       * Следующий обычный WINDOW_CLOSE_REQUEST, например крестик,
       * снова считается CANCEL.
       */
      requestedResult = FormResultType.CANCEL;

      try
      {
         final boolean allowClose =
                 switch( tempRequest )
                 {
                    case OK     -> onOK();
                    case CANCEL -> onCancel();
                 };

         if( !allowClose )
         {
            event.consume();
            return;
         }

         /*
          * Это пока только кандидат на окончательный result.
          *
          * WINDOW_CLOSE_REQUEST ещё может быть consumed
          * другим handler'ом.
          */
         result = tempRequest;
      }
      catch( Exception ex )
      {
         event.consume();
         throw new FormException( "Error processing form close request", ex );
      }
   }

   /** */
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