package ru.inversion.fore.form;

import javafx.stage.Window;
import ru.inversion.tc.TaskContext;

import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

final class FormContextImpl<T> implements FormContext<T>, AutoCloseable {

   private TaskContext taskContext;
   private boolean     taskContextOwner = false;

   private final Window owner;
   private Window window;

   private final T dataObject;
   private final Map<String, Object> parameters;

   private final ResourceBundle bundle;

   private final FormController<?> parentController;

   private boolean closed;

   private final FormMode formMode;

   FormContextImpl(
           TaskContext taskContext,
           Window owner,
           T dataObject,
           FormMode mode,
           Map<String, Object> parameters,
           ResourceBundle bundle,
           FormController<?> parentController )
   {
      this.taskContext = taskContext;

      this.owner = owner;
      this.dataObject = dataObject;
      this.parameters =
              parameters == null ? Map.of() : Map.copyOf(parameters);
      this.bundle = bundle;
      this.parentController = parentController;
      this.formMode = mode;
   }


   /** */
   private void checkForClosed()
   {
      if( closed )
          throw new IllegalStateException( "Form context is already closed" );
   }

   /** */
   @Override
   public synchronized TaskContext taskContext()
   {
      checkForClosed();

      if( taskContext == null ) {
          taskContext = new TaskContext();
          taskContextOwner = true;
      }
      return taskContext;
   }

   boolean isTaskContextOwner()
   {
      return taskContextOwner;
   }


   /** */
   synchronized void takeTaskContextOwnership()
   {
      checkForClosed();

      if( taskContext == null )
          throw new IllegalStateException( "TaskContext is not initialized" );

      taskContextOwner = true;
   }


   void closeTaskContext() throws Exception
   {
      if( taskContextOwner && taskContext != null )
          taskContext.close();
   }

   /** */
   @Override
   public Window owner()
   {
      return owner;
   }

   @Override
   public T dataObject()
   {
      return dataObject;
   }

   @Override
   public Map<String, Object> parameters()
   {
      return parameters;
   }

   @Override
   public ResourceBundle bundle()
   {
      return bundle;
   }

   @Override
   public FormController<?> parentController()
   {
      return parentController;
   }

   @Override
   public Window window()
   {
      if( window == null )
         throw new IllegalStateException( "Window is not available before guiInit()" );

      return window;
   }

   @Override
   public FormMode mode() {
      return formMode;
   }

   void setWindow(Window window)
   {
      if( this.window != null )
         throw new IllegalStateException( "Form window already initialized" );

      this.window = Objects.requireNonNull(window);
   }

   @Override
   synchronized public void close() throws Exception
   {
      if( closed )
          return;

      closed = true;

      if( taskContextOwner && taskContext != null )
          taskContext.close();
   }
}