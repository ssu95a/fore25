package ru.inversion.fore.form;

public class FormLaunchException extends FormException {

   final private String details;
   final private Class<?> controllerClass;

   public FormLaunchException( String message, String details) {
      super(message);
      this.details = details;
      controllerClass = null;
   }

   public FormLaunchException( String message, Throwable cause, String details) {
      super(message, cause);
      this.details = details;
      controllerClass = null;
   }

   public FormLaunchException( Class<?> controllerClass, String message, Throwable cause, String details) {
      super(message, cause);
      this.details = details;
      this.controllerClass = controllerClass;
   }

   public FormLaunchException( Class<?> controllerClass, String message, String details) {
      super(message );
      this.details = details;
      this.controllerClass = controllerClass;
   }

   public Class<?> controllerClass( ) {
      return controllerClass;
   }

   @Override
   public String getDetailedMessage() {
      return this.details;
   }
}
