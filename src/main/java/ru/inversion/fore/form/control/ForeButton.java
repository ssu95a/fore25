package ru.inversion.fore.form.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.util.Objects;

/**
 * Кнопка со стандартным оформлением и поддержкой общего действия.
 * Свойство standardAction задаёт оформление для FXML и Scene Builder.
 * Метод setAction подключает обработчик и изменяемые свойства общего действия.
 */
public class ForeButton extends Button
{
   private ForeAction action;

   // Ссылка позволяет отличить созданную кнопкой подсказку от пользовательской.
   private Tooltip installedTooltip;

   // Указывает, установлены ли привязки свойств через ControlsFX.
   private boolean actionBound;

   public ForeButton()
   {
      getStyleClass().add("fore-action-button");
      setFocusTraversable(false);
   }

   public ForeButton(ForeAction action)
   {
      this();
      setAction(action);
   }

   public StandardAction getStandardAction()
   {
      return action == null ? null : action.standardType();
   }


   /**
    * Создаёт действие для стандартного оформления, сохраняя значения из FXML.
    * Обработчик onAction не заменяется; свойства кнопки остаются доступными для записи.
    */
   public void setStandardAction( StandardAction type )
   {
      // Обработчик FXML принадлежит кнопке; этому действию нужен только набор свойств.
      final ForeAction next = type == null ? null : ForeActions.create(type, event -> {});

      if( actionBound )
      {
         unbindAction();
         clearUI();
      }

      final ForeAction previous = action;

      final boolean replaceText
         = getText() == null || getText().isEmpty() || (previous != null && Objects.equals(getText(), previous.getText()) );

      final boolean replaceTooltip
         = getTooltip() == null || (getTooltip() == installedTooltip && previous != null && Objects.equals(getTooltip().getText(), previous.getLongText()) );

      final boolean replaceGraphic = getGraphic() == null || (previous != null && getGraphic() == previous.getGraphic() );

      action = next;

      installedTooltip = replaceTooltip && next != null ? new Tooltip(next.getLongText()) : null;

      if( replaceText )
          setText(next == null ? null : next.getText());

      if( replaceTooltip )
          setTooltip(installedTooltip);

      if( replaceGraphic )
          setGraphic(next == null ? null : next.getGraphic());
   }


   /** */
   @java.beans.Transient // В FXML сохраняется standardAction, а не объект действия.
   public ForeAction getAction()
   {
      return action;
   }


   /**
    * Привязывает общее действие, включая обработчик, оформление и состояние disabled.
    * В этом режиме обработчик задаётся действием вместо FXML onAction.
    * Значение null снимает привязки и очищает оформление.
    */
   public void setAction( ForeAction next )
   {
      if( action == next && (actionBound || next == null) )
          return;

      unbindAction();

      action = next;
      installedTooltip = null;

      if( next != null )
      {
         ActionUtils.configureButton(next, this);
         actionBound = true;
      }
      else
         clearUI( );
   }


   /** */
   private void unbindAction()
   {
      if( !actionBound )
          return;

      // ControlsFX снимает привязки только при собственном обработчике onAction.
      final EventHandler<ActionEvent> handler = getOnAction();
      if( handler != action )
          setOnAction(action);

      ActionUtils.unconfigureButton(this);
      actionBound = false;

      // Сохраняем обработчик, который пользователь установил после привязки действия.
      if( handler != action )
          setOnAction(handler);
   }

   /** */
   private void clearUI()
   {
      setText   (null );
      setGraphic(null );
      setTooltip(null );

      setDisable(false);

      installedTooltip = null;
   }
}
