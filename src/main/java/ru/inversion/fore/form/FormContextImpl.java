package ru.inversion.fore.form;

import javafx.stage.Window;
import ru.inversion.tc.TaskContext;

import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

final class FormContextImpl<T> implements FormContext<T> {

   private final TaskContext taskContext;
   private final Window owner;
   private final T dataObject;
   private final Map<String, Object> parameters;
   private final ResourceBundle bundle;
   private final FormController<?> parentController;

   private Window window;

   FormContextImpl(
           TaskContext taskContext,
           Window owner,
           T dataObject,
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
   }

   @Override
   public TaskContext taskContext()
   {
      return taskContext;
   }

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
         throw new IllegalStateException(
                 "Window is not available before guiInit()"
         );

      return window;
   }

   void setWindow(Window window)
   {
      if( this.window != null )
         throw new IllegalStateException(
                 "Form window already initialized"
         );

      this.window = Objects.requireNonNull(window);
   }
}