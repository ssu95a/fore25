package ru.inversion.fore.form;

import ru.inversion.fore.ForeException;

public class FormException extends ForeException {

   public FormException(String message) {
      super(message);
   }

   public FormException(String message, Throwable cause) {
      super(message, cause);
   }
}
