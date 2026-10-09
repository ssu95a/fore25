package ru.inversion.fore.form.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.util.Objects;

/**
 * Пункт меню со стандартным оформлением и поддержкой общего действия.
 * Свойство standardAction задаёт оформление для FXML и Scene Builder.
 * Метод setAction подключает обработчик и изменяемые свойства общего действия.
 */
public class ForeMenuItem extends MenuItem
{
   private ForeAction action;

   // Указывает, установлены ли привязки свойств через ControlsFX.
   private boolean actionBound;

   public ForeMenuItem()
   { }

   public ForeMenuItem(ForeAction action)
   {
      setAction(action);
   }

   public StandardAction getStandardAction()
   {
      return action == null ? null : action.standardType();
   }

   /**
    * Создаёт действие для стандартного оформления, сохраняя значения из FXML.
    * Обработчик onAction не заменяется; свойства меню остаются доступными для записи.
    */
   public void setStandardAction(StandardAction type)
   {
      // Обработчик FXML принадлежит пункту меню; действию нужен только набор свойств.
      final ForeAction next = type == null ? null : ForeActions.create( type, event -> {} );

      if( actionBound )
      {
         unbindAction();
         clearUI();
      }

      final ForeAction previous = action;
      final boolean replaceText = getText() == null || getText().isEmpty()
              || (previous != null && Objects.equals(getText(), previous.getText()));
      final boolean replaceGraphic = getGraphic() == null
              || (previous != null && getGraphic() == previous.getGraphic());
      final boolean replaceAccelerator = getAccelerator() == null
              || (previous != null && Objects.equals(getAccelerator(), previous.getAccelerator()));

      action = next;

      if( replaceText )
         setText(next == null ? null : next.getText());
      if( replaceGraphic )
         setGraphic(next == null ? null : next.getGraphic());
      if( replaceAccelerator )
         setAccelerator(next == null ? null : next.getAccelerator());
   }

   @java.beans.Transient // В FXML сохраняется standardAction, а не объект действия.
   public ForeAction getAction()
   {
      return action;
   }

   /**
    * Привязывает общее действие, включая обработчик, оформление и основной ускоритель.
    * В этом режиме обработчик задаётся действием вместо FXML onAction.
    * Значение null снимает привязки и очищает оформление.
    */
   public void setAction(ForeAction next)
   {
      if( action == next && (actionBound || next == null) )
         return;

      unbindAction();
      action = next;

      if( next != null )
      {
         ActionUtils.configureMenuItem(next, this);
         actionBound = true;
      }
      else
         clearUI();
   }

   private void unbindAction()
   {
      if( !actionBound )
         return;

      // ControlsFX снимает привязки только при собственном обработчике onAction.
      final EventHandler<ActionEvent> handler = getOnAction();
      if( handler != action )
         setOnAction(action);

      ActionUtils.unconfigureMenuItem(this);
      actionBound = false;

      // Сохраняем обработчик, который пользователь установил после привязки действия.
      if( handler != action )
         setOnAction(handler);
   }

   private void clearUI()
   {
      setText(null);
      setGraphic(null);
      setAccelerator(null);
      setDisable(false);
   }
}
