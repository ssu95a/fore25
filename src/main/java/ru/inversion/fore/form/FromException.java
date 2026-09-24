package ru.inversion.fore.form;

import ru.inversion.fore.ForeException;

public class FromException extends ForeException {

   public FromException(String message) {
      super(message);
   }

   public FromException(String message, Throwable cause) {
      super(message, cause);
   }
}
