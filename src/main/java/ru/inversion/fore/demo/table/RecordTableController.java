package ru.inversion.fore.demo.table;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.inversion.fore.dataset.ForeDataSetAdapter;
import ru.inversion.fore.form.FormController;
import ru.inversion.fore.form.FormLauncher;
import ru.inversion.fore.form.FormMode;
import ru.inversion.fore.form.FormResultType;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.StandardAction;
import ru.inversion.fore.form.control.ForeButton;
import ru.inversion.fore.form.control.ForeMenuItem;
import ru.inversion.fore.form.control.ForeTableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Форма списка: выбранная строка определяет доступность действий с записью. */
public final class RecordTableController extends FormController<RecordStore>
{
   @FXML private ForeTableView<RecordStore.Row> table;
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

   private ForeDataSetAdapter<RecordStore.Row> adapter;
   private final List<ForeAction> uiActions = new ArrayList<>();

   @Override
   protected void init()
   {
      Objects.requireNonNull(getDataObject(), "store");
      setTitle(getBundle().getString("list.title"));
      idColumn.setCellValueFactory(cell -> new ReadOnlyLongWrapper(cell.getValue().id()));
      nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
      adapter = ForeDataSetAdapter.bind(getDataObject().getDataSet(), table);
      table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
      countLabel.textProperty().bind(Bindings.size(table.getItems()).asString(getBundle().getString("list.count")));

      connect(StandardAction.CREATE, event -> openEditor(FormMode.INSERT, null), createButton, createMenu);
      final var update = connect(StandardAction.UPDATE, event -> openSelected(FormMode.EDIT), updateButton, updateMenu);
      final var view = connect(StandardAction.VIEW, event -> openSelected(FormMode.VIEW), viewButton, viewMenu);
      final var delete = connect(StandardAction.DELETE, event -> deleteSelected(), deleteButton, deleteMenu);
      connect(StandardAction.REFRESH, event -> adapter.refresh(), refreshButton, refreshMenu);
      table.setActivationAction(update);

      final var noSelection = table.getSelectionModel().selectedItemProperty().isNull();
      update.disabledProperty().bind(noSelection);
      view.disabledProperty().bind(noSelection);
      delete.disabledProperty().bind(noSelection);
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

   private void openSelected(FormMode mode)
   {
      final var selected = adapter.getCurrentRow();
      if( selected != null )
         openEditor(mode, selected);
   }

   private void openEditor(FormMode mode, RecordStore.Row row)
   {
      final RecordStore store = getDataObject();
      final var draft = mode == FormMode.INSERT ? store.newDraft() : store.editDraft(row);

      // Для данных в памяти редактору достаточно владельца окна.
      // Явный null исключает ленивое создание TaskContext родительского контроллера.
      new FormLauncher<RecordStore.Draft, RecordEditorController>(null, getWindow(), RecordEditorController.class)
              .fxml("ru/inversion/fore/demo/table/record-editor.fxml")
              .bundle(getBundle())
              .dataObject(draft)
              .mode(mode)
              .modal(true)
              .callback(result -> {
                 if( result.result() == FormResultType.OK && mode != FormMode.VIEW )
                 {
                    store.save(result.dataObject());
                 }
              })
              .runForm();
   }

   private void deleteSelected()
   {
      final var selected = table.getSelectionModel().getSelectedItem();
      if( selected == null )
         return;

      getDataObject().delete(selected.id());
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
      if( table != null ) table.setActivationAction(null);
      if( countLabel != null ) countLabel.textProperty().unbind();
      if( adapter != null )
      {
         adapter.close();
         adapter = null;
      }
   }
}
