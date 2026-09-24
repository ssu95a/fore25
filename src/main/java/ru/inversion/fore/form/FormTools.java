package ru.inversion.fore.form;

public class FormTools {

   private static final String CONTROLLER_SUFFIX = "Controller";

   static String defaultFxmlPath( Class<?> controllerClass )
   {
      return controllerClass == null ?
              null
              :
              controllerClass.getPackageName().replace('.', '/')+ "/fxml/"+ formName(controllerClass)+ ".fxml";
   }

   static String defaultBundleName(Class<?> controllerClass)
   {
      return controllerClass == null ?
              null
              :
              controllerClass.getPackageName()+ ".res." + formName(controllerClass);
   }

   private static String formName( Class<?> controllerClass )
   {
      final String name = controllerClass.getSimpleName();

      if( name.endsWith(CONTROLLER_SUFFIX) )
          return name.substring( 0, name.length() - CONTROLLER_SUFFIX.length() );

      return name;
   }
}
