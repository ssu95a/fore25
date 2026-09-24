package ru.inversion.fore.form;

import javafx.fxml.Initializable;
import javafx.stage.Window;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public abstract class FormController<T> implements Initializable {

   private FormContext<T,?> formContext;

   private FormResultType result = FormResultType.CANCEL;
   private Consumer<FormResult<T>> resultHandler;

   private boolean completed;

   @Override
   public void initialize( URL location, ResourceBundle resources )
   { }

   /** */
   final void preInitController( FormContext<T,?> context, Consumer<FormResult<T>> resultHandler ) throws Exception
   {
      this.formContext   = context;
      this.resultHandler = resultHandler;

      preInit();
   }

   /** */
   final void guiInitController() throws Exception
   {
      guiInit();
   }

   protected void preInit() throws Exception
   { }

   protected void guiInit() throws Exception
   { }

   /** */
   protected final FormContext<T,?> fromContext( )
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

   protected final void close()
   {
      close( FormResultType.CANCEL);
   }

   protected final void close( FormResultType result )
   {
      this.result = Objects.requireNonNull(result);

      formContext.window().hide();
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