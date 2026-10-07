package ru.inversion.fore.form.control;

import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;
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

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Таблица с восстановлением выбранной строки и общим действием её активации.
 * Источник данных, сортировка и модель выбора используют штатный API JavaFX.
 */
public class ForeTableView<T> extends TableView<T>
{
   private ForeAction activationAction;

   // Ссылки нужны для снятия только обработчиков, установленных самой таблицей.
   private final EventHandler<KeyEvent> activationKeyHandler = this::handleActivationKey;
   private final EventHandler<MouseEvent> activationMouseHandler = this::handleActivationMouse;

   public ForeTableView()
   {
      getStyleClass().add("fore-table-view");
   }

   public ForeTableView(ObservableList<T> items)
   {
      this();
      setItems(items);
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

   /**
    * Заменяет данные и восстанавливает одну выбранную строку по ключу.
    * Источник должен формировать текущий список таблицы, в том числе через преобразования.
    * Ключи записей должны быть уникальными и ненулевыми; null означает отсутствие выбора.
    */
   public <K> void replaceItems(ObservableList<T> source, Collection<? extends T> items,
                                Function<? super T, ? extends K> keyExtractor)
   {
      FormTools.requireFxThread();
      Objects.requireNonNull(keyExtractor, "keyExtractor");
      final T selected = getSelectionModel() == null ? null : getSelectionModel().getSelectedItem();
      final K key = selected == null ? null : keyExtractor.apply(selected);
      replaceItems(source, items, keyExtractor, key);
   }

   /**
    * После замены выбирает запись с заданным ключом, например только что сохранённую.
    * Отсутствующий ключ или null снимает выбор. Данные копируются до изменения источника.
    */
   public <K> void replaceItems(ObservableList<T> source, Collection<? extends T> items,
                                Function<? super T, ? extends K> keyExtractor, K selectedKey)
   {
      FormTools.requireFxThread();
      Objects.requireNonNull(keyExtractor, "keyExtractor");
      replaceSource(source, items);

      final var selection = getSelectionModel();
      if( selection == null )
         return;
      selection.clearSelection();
      if( selectedKey == null )
         return;

      for( int index = 0; index < getItems().size(); index++ )
         if( Objects.equals(selectedKey, keyExtractor.apply(getItems().get(index))) )
         {
            selection.clearAndSelect(index);
            scrollTo(index);
            return;
         }
   }

   /**
    * Обновляет источник после удаления выбранной записи и выбирает строку на её месте.
    * Для последней строки выбирается предыдущая; пустой список остаётся без выбора.
    */
   public void replaceItemsAfterRemoval(ObservableList<T> source, Collection<? extends T> items)
   {
      FormTools.requireFxThread();
      final int index = getSelectionModel() == null ? -1 : getSelectionModel().getSelectedIndex();
      replaceSource(source, items);

      final var selection = getSelectionModel();
      if( selection == null )
         return;
      selection.clearSelection();
      if( index >= 0 && !getItems().isEmpty() )
      {
         final int next = Math.min(index, getItems().size() - 1);
         selection.clearAndSelect(next);
         scrollTo(next);
      }
   }

   private void replaceSource(ObservableList<T> source, Collection<? extends T> items)
   {
      Objects.requireNonNull(source, "source");
      ObservableList<?> view = getItems();
      while( view != source )
         if( view instanceof TransformationList<?, ?> transformed )
            view = transformed.getSource();
         else
            throw new IllegalArgumentException("Источник должен формировать текущий список таблицы");

      final List<T> snapshot = List.copyOf(Objects.requireNonNull(items, "items"));
      source.setAll(snapshot);
      if( getComparator() != null )
         sort();
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
