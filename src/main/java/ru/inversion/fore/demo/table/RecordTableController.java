package ru.inversion.fore.demo.table;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.inversion.fore.form.FormController;
import ru.inversion.fore.form.FormLauncher;
import ru.inversion.fore.form.FormMode;
import ru.inversion.fore.form.FormResultType;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.StandardAction;
import ru.inversion.fore.form.control.ForeButton;
import ru.inversion.fore.form.control.ForeMenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Форма списка: выбранная строка определяет доступность действий с записью. */
public final class RecordTableController extends FormController<RecordStore>
{
   @FXML private TableView<RecordStore.Row> table;
   @FXML private TableColumn<RecordStore.Row, Number> idColumn;
   @FXML private TableColumn<RecordStore.Row, String> nameColumn;
   @FXML private Label countLabel;
   @FXML private ForeButton createButton;
   @FXML private ForeButton updateButton;
   @FXML private ForeButton viewButton;
   @FXML private ForeButton deleteButton;
   @FXML private ForeButton refreshButton;
   @FXML private ForeMenuItem createMenu;
   @FXML private ForeMenuItem updateMenu;
   @FXML private ForeMenuItem viewMenu;
   @FXML private ForeMenuItem deleteMenu;
   @FXML private ForeMenuItem refreshMenu;

   private final ObservableList<RecordStore.Row> rows = FXCollections.observableArrayList();
   private final SortedList<RecordStore.Row> sortedRows = new SortedList<>(rows);
   private final List<ForeAction> uiActions = new ArrayList<>();

   @Override
   protected void init()
   {
      Objects.requireNonNull(getDataObject(), "store");
      setTitle(getBundle().getString("list.title"));
      idColumn.setCellValueFactory(cell -> new ReadOnlyLongWrapper(cell.getValue().id()));
      nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
      sortedRows.comparatorProperty().bind(table.comparatorProperty());
      table.setItems(sortedRows);
      table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
      countLabel.textProperty().bind(Bindings.size(rows).asString(getBundle().getString("list.count")));

      connect(StandardAction.CREATE, event -> openEditor(FormMode.INSERT, null), createButton, createMenu);
      final var update = connect(StandardAction.UPDATE, event -> openSelected(FormMode.EDIT), updateButton, updateMenu);
      final var view = connect(StandardAction.VIEW, event -> openSelected(FormMode.VIEW), viewButton, viewMenu);
      final var delete = connect(StandardAction.DELETE, event -> deleteSelected(), deleteButton, deleteMenu);
      connect(StandardAction.REFRESH, event -> refreshRows(selectedId()), refreshButton, refreshMenu);

      final var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
      update.disabledProperty().bind(noSelection);
      view.disabledProperty().bind(noSelection);
      delete.disabledProperty().bind(noSelection);
      refreshRows(null);
   }

   private ForeAction connect(StandardAction type, Consumer<ActionEvent> handler,
                              ForeButton button, ForeMenuItem menu)
   {
      final ForeAction action = createAction(type, handler);
      uiActions.add(action);
      button.setAction(action);
      menu.setAction(action);
      return action;
   }

   private Long selectedId()
   {
      final var selected = table.getSelectionModel().getSelectedItem();
      return selected == null ? null : selected.id();
   }

   private void openSelected(FormMode mode)
   {
      final var selected = table.getSelectionModel().getSelectedItem();
      if( selected != null )
         openEditor(mode, selected);
   }

   private void openEditor(FormMode mode, RecordStore.Row row)
   {
      final RecordStore store = getDataObject();
      final var draft = mode == FormMode.INSERT ? store.newDraft() : store.editDraft(row);

      new FormLauncher<RecordStore.Draft, RecordEditorController>(this, RecordEditorController.class)
              .fxml("ru/inversion/fore/demo/table/record-editor.fxml")
              .bundle(getBundle())
              .dataObject(draft)
              .mode(mode)
              .modal(true)
              .callback(result -> {
                 if( result.result() == FormResultType.OK && mode != FormMode.VIEW )
                 {
                    final var saved = store.save(result.dataObject());
                    refreshRows(saved.id());
                 }
              })
              .runForm();
   }

   private void deleteSelected()
   {
      final var selected = table.getSelectionModel().getSelectedItem();
      if( selected == null )
         return;

      final int index = table.getSelectionModel().getSelectedIndex();
      getDataObject().delete(selected.id());
      refreshRows(null);
      if( !sortedRows.isEmpty() )
         table.getSelectionModel().select(Math.min(index, sortedRows.size() - 1));
   }

   /** Выбор восстанавливается по ID, даже если сохранение изменило положение строки при сортировке. */
   private void refreshRows(Long id)
   {
      rows.setAll(getDataObject().snapshot());
      table.getSelectionModel().clearSelection();
      if( id != null )
         for( var row : sortedRows )
            if( row.id() == id )
            {
               table.getSelectionModel().select(row);
               table.scrollTo(row);
               break;
            }
   }

   @Override
   protected void closeGuiResources()
   {
      for( ForeAction action : uiActions )
      {
         action.disabledProperty().unbind();
         action.setDisabled(true);
      }
      for( ForeButton button : new ForeButton[]{createButton, updateButton, viewButton, deleteButton, refreshButton} )
         if( button != null ) button.setAction(null);
      for( ForeMenuItem menu : new ForeMenuItem[]{createMenu, updateMenu, viewMenu, deleteMenu, refreshMenu} )
         if( menu != null ) menu.setAction(null);

      uiActions.clear();
      sortedRows.comparatorProperty().unbind();
      if( countLabel != null ) countLabel.textProperty().unbind();
      rows.clear();
   }
}
