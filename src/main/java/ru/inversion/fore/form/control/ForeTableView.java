package ru.inversion.fore.form.control;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import ru.inversion.fore.form.FormTools;
import ru.inversion.fore.form.action.ForeAction;

/**
 * Таблица с общим действием активации строки.
 * Привязку записей и курсора набора выполняет ForeDataSetAdapter; отображение использует API JavaFX.
 * До подключения набора список записей пуст и доступен только для чтения.
 */
public class ForeTableView<T> extends TableView<T>
{
   private ForeAction activationAction;

   // Ссылки нужны для снятия только обработчиков, установленных самой таблицей.
   private final EventHandler<KeyEvent> activationKeyHandler = this::handleActivationKey;
   private final EventHandler<MouseEvent> activationMouseHandler = this::handleActivationMouse;

   public ForeTableView()
   {
      super(FXCollections.emptyObservableList());
      getStyleClass().add("fore-table-view");
   }

   /** Действие для Enter и двойного щелчка; его состояние определяет доступность активации. */
   @java.beans.Transient
   public ForeAction getActivationAction()
   {
      return activationAction;
   }

   /** Значение null снимает обработчики активации, сохраняя пользовательские обработчики. */
   public void setActivationAction(ForeAction action)
   {
      FormTools.requireFxThread();
      if( activationAction == action )
         return;

      if( activationAction == null && action != null )
      {
         addEventFilter(KeyEvent.KEY_PRESSED, activationKeyHandler);
         addEventHandler(MouseEvent.MOUSE_CLICKED, activationMouseHandler);
      }
      else if( action == null )
      {
         removeEventFilter(KeyEvent.KEY_PRESSED, activationKeyHandler);
         removeEventHandler(MouseEvent.MOUSE_CLICKED, activationMouseHandler);
      }
      activationAction = action;
   }

   private boolean acceptsActivation()
   {
      return activationAction != null && !isDisabled() && getItems() != null
              && getEditingCell() == null && getSelectionModel() != null;
   }

   private void handleActivationKey(KeyEvent event)
   {
      if( event.isConsumed() || event.getCode() != KeyCode.ENTER
              || event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()
              || !acceptsActivation() || activationAction.isDisabled() || getSelectionModel().getSelectedIndex() < 0
              || getSelectionModel().getSelectedItem() == null )
         return;

      if( event.getTarget() != this && activationRow(event.getTarget()) == null )
         return;
      event.consume();
      activationAction.handle(new ActionEvent(this, this));
   }

   private void handleActivationMouse(MouseEvent event)
   {
      if( event.isConsumed() || event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2
              || event.isShiftDown() || event.isControlDown() || event.isAltDown() || event.isMetaDown()
              || !acceptsActivation() )
         return;

      final TableRow<?> row = activationRow(event.getTarget());
      if( row == null || row.isEmpty() || row.getItem() == null
              || row.getIndex() < 0 || row.getIndex() >= getItems().size() )
         return;

      // Действие относится к строке под мышью, даже если до щелчка была выбрана другая.
      getSelectionModel().clearAndSelect(row.getIndex());
      // Смена выбора может отключить действие или снять его через пользовательский слушатель.
      if( !acceptsActivation() || activationAction.isDisabled() )
         return;
      event.consume();
      activationAction.handle(new ActionEvent(this, this));
   }

   private TableRow<?> activationRow(Object target)
   {
      for( Node node = target instanceof Node n ? n : null; node != null && node != this; node = node.getParent() )
      {
         if( node instanceof TableRow<?> row )
            return row.getTableView() == this ? row : null;
         // Встроенные редакторы и кнопки обрабатывают ввод самостоятельно.
         if( node instanceof Control && !(node instanceof TableCell<?, ?>) && !(node instanceof Label) )
            return null;
      }
      return null;
   }
}
