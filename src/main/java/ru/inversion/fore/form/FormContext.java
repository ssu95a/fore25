package ru.inversion.fore.form;

import javafx.stage.Window;
import ru.inversion.tc.TaskContext;

import java.util.Map;
import java.util.ResourceBundle;

/** */
public record FormContext<T, C extends FormController<?>> (

   TaskContext taskContext,

   Window window,
   Window owner,

   T dataObject,

   Map<String, Object> parameters,

   ResourceBundle bundle,

   C parentController
)
{
   public FormContext {
      parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
   }

   @SuppressWarnings("unchecked")
   public <V> V parameter( String name )
   {
      return (V) parameters.get(name);
   }
}