package ru.inversion.fore.form.control;

import javafx.collections.ObservableMap;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Label;

/**
 * Common metadata contract for Fore controls backed by a JavaFX {@link Control}.
 *
 * No legacy IJInvControl dependency: validation, Actions, controller lookup,
 * dataset binding and input-specific read-only semantics live elsewhere.
 */
public interface IForeControl
{
   /** Keep the historical field-name key so existing metadata remains readable. */
   String FIELD_NAME_KEY = "ru.inversion.field_name";

   String LABEL_KEY = "ru.inversion.fore.control.label";

   /** Already implemented by every JavaFX Node/Control. */
   ObservableMap<Object, Object> getProperties();

   default String getFieldName()
   {
      return (String) getProperties().get(FIELD_NAME_KEY);
   }

   /** Null removes metadata; non-null names are preserved verbatim. */
   default void setFieldName(String fieldName)
   {
      if( fieldName == null )
         getProperties().remove(FIELD_NAME_KEY);
      else
         getProperties().put(FIELD_NAME_KEY, fieldName);
   }

   /**
    * Return the label bound via setLabel, or a label associated externally
    * via JavaFX Label.setLabelFor (including FXML).
    */
   default Label getLabel()
   {
      final Control control = foreControl();
      final Object stored = getProperties().get(LABEL_KEY);
      if( stored instanceof Label label && label.getLabelFor() == control )
         return label;

      final Object accessible = control.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
      return accessible instanceof Label label ? label : null;
   }

   /**
    * Associate a Label with this control. A null value removes the association.
    * A void JavaBean setter is intentional for FXMLLoader and Scene Builder.
    */
   default void setLabel(Label label)
   {
      final Control control = foreControl();
      final Label previous = getLabel();

      if( previous != null && previous != label && previous.getLabelFor() == control )
         previous.setLabelFor(null);

      if( label == null )
      {
         getProperties().remove(LABEL_KEY);
      }
      else
      {
         label.setLabelFor(control);
         getProperties().put(LABEL_KEY, label);
      }
   }

   private Control foreControl()
   {
      if( this instanceof Control control )
         return control;

      throw new IllegalStateException("IForeControl must be implemented by a JavaFX Control");
   }
}
