package ru.inversion.fore;

import ru.inversion.utils.IExceptionInfo;

/** */
public class ForeException extends RuntimeException implements IExceptionInfo {

   public ForeException(String message) {
      super( Tags.PRODUCT_LABEL + message);
   }

   public ForeException(String message, Throwable cause) {
      super( Tags.PRODUCT_LABEL + message, cause);
   }

   @Override
   public String getCategory() {
      return "fore25";
   }
}
