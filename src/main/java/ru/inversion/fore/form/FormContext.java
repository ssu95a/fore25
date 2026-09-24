package ru.inversion.fore.form;

import javafx.stage.Window;
import ru.inversion.tc.TaskContext;

import java.util.Map;
import java.util.ResourceBundle;

public interface FormContext<T> extends AutoCloseable {

   TaskContext taskContext();

   Window owner();

   T dataObject();

   Map<String, Object> parameters();

   ResourceBundle bundle();

   FormController<?> parentController();

   Window window();

   @SuppressWarnings("unchecked")
   default <V> V parameter(String name)
   {
      return (V)parameters().get(name);
   }
}