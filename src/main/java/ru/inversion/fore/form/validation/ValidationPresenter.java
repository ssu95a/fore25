package ru.inversion.fore.form.validation;

import javafx.scene.control.Control;

import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;
import org.controlsfx.validation.decoration.ValidationDecoration;

import ru.inversion.utils.Checks;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;


public final class ValidationPresenter
{
   private final ValidationDecoration decorator = new GraphicValidationDecoration();
   private final Set<Control>         active    = Collections.newSetFromMap(new IdentityHashMap<>());


   /**
    * Показать ошибки, имеющие конкретный Control target.
    *
    * Для одного Control используется первое сообщение.
    */
   public void show(ValidationResult result)
   {
      Checks.Require.object(result, "result");

      final Set<Control> processed = Collections.newSetFromMap(new IdentityHashMap<>());

      for( ValidationFailure failure : result.failures() )
      {
         final Control target = failure.target();

         if( target == null || !processed.add(target) )
            continue;

         clear(target);

         decorator.applyValidationDecoration(
                 ValidationMessage.error(
                         target,
                         failure.message()
                 )
         );

         active.add(target);
      }
   }


   /**
    * Убрать визуальную ошибку указанного Control.
    */
   public void clear(Control control)
   {
      Checks.Require.object(control, "control");

      if( active.remove(control) )
         decorator.removeDecorations(control);
   }


   /**
    * Очистить оформление всех Control,
    * обработанных этим presenter.
    */
   public void clearAll()
   {
      for( Control control : List.copyOf(active) )
         clear(control);
   }
}