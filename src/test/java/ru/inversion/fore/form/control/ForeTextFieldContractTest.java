package ru.inversion.fore.form.control;

import javafx.scene.control.Label;
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
      assertTrue(IForeControl.class.isAssignableFrom(ForeTextField.class));
      assertNotNull(ForeTextField.class.getConstructor());
      assertNotNull(ForeTextField.class.getConstructor(String.class));
   }

   @Test
   void fieldNameAndLabelAreWritableJavaBeanProperties() throws Exception
   {
      final var descriptors = Introspector.getBeanInfo(ForeTextField.class).getPropertyDescriptors();
      for( String name : new String[]{ "fieldName", "label" } )
      {
         final var descriptor = Arrays.stream(descriptors)
                 .filter(property -> property.getName().equals(name))
                 .findFirst().orElseThrow();
         assertEquals(name.equals("fieldName") ? String.class : Label.class, descriptor.getPropertyType());
         assertNotNull(descriptor.getReadMethod());
         assertNotNull(descriptor.getWriteMethod());
      }
   }

   @Test
   void trimOnFocusLostIsNotExposed() throws Exception
   {
      assertFalse(Arrays.stream(Introspector.getBeanInfo(ForeTextField.class).getPropertyDescriptors())
              .anyMatch(property -> property.getName().equals("trimOnFocusLost")));
      assertThrows(NoSuchMethodException.class, () -> ForeTextField.class.getMethod("trimOnFocusLostProperty"));
   }
}
