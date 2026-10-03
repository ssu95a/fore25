package ru.inversion.fore.form.control;

import javafx.scene.control.TextField;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/** Structural tests do not require a running JavaFX toolkit/display. */
class ForeTextFieldContractTest
{
   @Test
   void isAStandardTextFieldAndCanBeConstructedByFxml() throws Exception
   {
      assertTrue(TextField.class.isAssignableFrom(ForeTextField.class));
      assertNotNull(ForeTextField.class.getConstructor());
      assertNotNull(ForeTextField.class.getConstructor(String.class));
   }

   @Test
   void trimOnFocusLostIsAJavaBeanAndObservableProperty() throws Exception
   {
      var descriptor = Arrays.stream(Introspector.getBeanInfo(ForeTextField.class)
                       .getPropertyDescriptors())
              .filter(property -> property.getName().equals("trimOnFocusLost"))
              .findFirst().orElseThrow();

      assertEquals(boolean.class, descriptor.getPropertyType());
      assertNotNull(descriptor.getReadMethod());
      assertNotNull(descriptor.getWriteMethod());
      assertNotNull(ForeTextField.class.getMethod("trimOnFocusLostProperty"));
   }

}
