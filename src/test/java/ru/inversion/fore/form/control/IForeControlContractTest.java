package ru.inversion.fore.form.control;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class IForeControlContractTest
{
   /** Проверяет метаданные без создания контрола и запуска JavaFX. */
   @Test
   void fieldNameUsesHistoricalMetadataKeyAndCanBeRemoved()
   {
      final ObservableMap<Object, Object> metadata = FXCollections.observableHashMap();
      final IForeControl control = () -> metadata;

      assertNull(control.getFieldName());
      control.setFieldName("EMP_NAME");
      assertEquals("EMP_NAME", control.getFieldName());
      assertEquals("EMP_NAME", metadata.get("ru.inversion.field_name"));

      control.setFieldName(null);
      assertNull(control.getFieldName());
      assertFalse(metadata.containsKey(IForeControl.FIELD_NAME_KEY));
   }

   @Test
   void foreButtonDoesNotExposeFieldMetadata() throws Exception
   {
      assertFalse(IForeControl.class.isAssignableFrom(ForeButton.class));
      assertFalse(Arrays.stream(Introspector.getBeanInfo(ForeButton.class).getPropertyDescriptors())
              .anyMatch(property -> property.getName().equals("fieldName")
                      || property.getName().equals("label")));
   }

   /** Методы интерфейса должны быть доступны в FXML как свойства JavaBean. */
   @Test
   void fieldNameAndLabelAreJavaBeanProperties() throws Exception
   {
      final var properties = Introspector.getBeanInfo(ForeTextField.class).getPropertyDescriptors();
      final PropertyDescriptor fieldName = find(properties, "fieldName");
      assertEquals(String.class, fieldName.getPropertyType());
      assertNotNull(fieldName.getReadMethod());
      assertNotNull(fieldName.getWriteMethod());

      final PropertyDescriptor label = find(properties, "label");
      assertEquals(Label.class, label.getPropertyType());
      assertNotNull(label.getReadMethod());
      assertNotNull(label.getWriteMethod());
   }

   private static PropertyDescriptor find(PropertyDescriptor[] descriptors, String name)
   {
      return Arrays.stream(descriptors)
              .filter(property -> property.getName().equals(name))
              .findFirst().orElseThrow();
   }
}
