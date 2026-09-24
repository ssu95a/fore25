package ru.inversion.fore.form;

import javafx.beans.property.StringProperty;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public abstract class FormController<T> implements Initializable {

   private FormContext<T> formContext;

   private FormResultType result = FormResultType.CANCEL;
   private Consumer<FormResult<T>> resultHandler;

   private boolean completed;

   private StringProperty titleProperty;

   /** */
   final void preInitController( FormContext<T> context, Consumer<FormResult<T>> resultHandler ) throws Exception
   {
      this.formContext   = context;
      this.resultHandler = resultHandler;
      this.titleProperty = ((Stage)getWindow()).titleProperty();

      preInit();
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

   protected void preInit() throws Exception
   { }

   protected void init() throws Exception
   { }

   protected void guiInit() throws Exception
   { }

   /** */
   protected final FormContext<T> fromContext( )
   {
      return formContext;
   }

   /** */
   public final Window getWindow()
   {
      return formContext.window();
   }

   /** */
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


   public final Map<String, Object> getParameters( )
   {
      return formContext.parameters();
   }

   public final <V> V getParameter( String name )
   {
      return formContext.parameter(name);
   }

   public void setTitle( String title )
   {
      ((Stage)getWindow()).setTitle(title);
   }

   /** */
   public String getTitle( )
   {
      return ((Stage)getWindow()).getTitle( );
   }


   protected final void close()
   {
      close( FormResultType.CANCEL);
   }

   /** */
   protected final void close( FormResultType result )
   {
      this.result = Objects.requireNonNull(result);

      final Window window = formContext.window();

      if( window instanceof Stage stage )
         stage.close();
      else
         window.hide();
   }


   final void completeController()
   {
      if( completed )
          return;

      completed = true;

      if( resultHandler != null )
      {
         resultHandler.accept (
            new FormResult<>(
               result,
               formContext.dataObject(),
               getWindow()
            )
         );
      }
   }
}