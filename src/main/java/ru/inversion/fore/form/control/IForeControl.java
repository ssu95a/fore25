package ru.inversion.fore.form.control;

import javafx.collections.ObservableMap;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Label;

/**
 * Контракт необязательных метаданных Fore-контролов на базе JavaFX {@link Control}.
 * Связь с меткой использует стандартный {@link Label#labelForProperty()}.
 * Для одного контрола следует задавать не более одной связанной метки.
 *
 * Классы контролов, предоставляющие эти свойства в FXML, должны явно объявлять
 * getter/setter с делегированием default-методам: FXMLLoader не ищет их в интерфейсах.
 */
public interface IForeControl
{
   /** Исторический ключ сохранён для чтения существующих метаданных. */
   String FIELD_NAME_KEY = "ru.inversion.field_name";

   /** Уже реализован каждым JavaFX Node/Control. */
   ObservableMap<Object, Object> getProperties();

   default String getFieldName()
   {
      return (String) getProperties().get(FIELD_NAME_KEY);
   }

   /** Null удаляет метаданные; остальные значения сохраняются без изменений. */
   default void setFieldName(String fieldName)
   {
      if( fieldName == null )
         getProperties().remove(FIELD_NAME_KEY);
      else
         getProperties().put(FIELD_NAME_KEY, fieldName);
   }

   /**
    * Текущая метка из штатной связи JavaFX, в том числе заданной через FXML.
    * Отдельная копия связи в properties не хранится.
    */
   default Label getLabel()
   {
      final Control control = foreControl();
      final Object accessible = control.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
      return accessible instanceof Label label && label.getLabelFor() == control ? label : null;
   }

   /**
    * Связать метку с контролом; null снимает текущую связь.
    * Повторная установка текущей метки ничего не меняет.
    *
    * @throws IllegalStateException если для изменения связи требуется запись
    *         в bound-свойство labelFor; обе связи остаются прежними
    */
   default void setLabel(Label label)
   {
      final Control control = foreControl();
      final Label previous = getLabel();

      if( previous == label )
         return;

      // Проверяем обе метки до изменения связи, чтобы отказ не оставил полусостояние.
      if( previous != null && previous.labelForProperty().isBound() )
         throw new IllegalStateException("Cannot detach a Label with a bound labelFor property");
      if( label != null && label.labelForProperty().isBound() )
         throw new IllegalStateException("Cannot assign a Label with a bound labelFor property");

      if( previous != null )
         previous.setLabelFor(null);
      if( label != null )
         label.setLabelFor(control);
   }

   private Control foreControl()
   {
      if( this instanceof Control control )
         return control;

      throw new IllegalStateException("IForeControl must be implemented by a JavaFX Control");
   }
}
