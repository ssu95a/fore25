package ru.inversion.fore.demo.table;

import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.FormLauncher;
import ru.inversion.fore.form.action.StandardAction;
import ru.inversion.fore.form.control.ForeButton;
import ru.inversion.fore.form.control.ForeMenuItem;
import ru.inversion.fore.form.control.ForeTextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TableViewDemoTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void selectionControlsTheSameActionsInButtonsMenusAndKeyboard() throws Exception
   {
      try( var demo = Demo.open() )
      {
         FxTestSupport.run(() -> {
            for( StandardAction type : StandardAction.values() )
               assertSame(button(demo.main, type).getAction(), menu(demo.main, type).getAction());
            assertSelectionActionsDisabled(demo.main, true);
            assertFalse(button(demo.main, StandardAction.CREATE).isDisable());
            assertFalse(button(demo.main, StandardAction.REFRESH).isDisable());
            press(demo.main, KeyCode.F3, false);
            press(demo.main, KeyCode.F4, false);
            press(demo.main, KeyCode.F6, true);
            assertEquals(3, demo.store.snapshot().size());
            assertTrue(demo.shown.isEmpty());

            table(demo.main).getSelectionModel().selectFirst();
            assertSelectionActionsDisabled(demo.main, false);
            table(demo.main).getSelectionModel().clearSelection();
            assertSelectionActionsDisabled(demo.main, true);
         });
      }
   }

   @Test
   void createValidatesTheDraftAndCancelDoesNotAddARecord() throws Exception
   {
      try( var demo = Demo.open() )
      {
         FxTestSupport.run(() -> press(demo.main, KeyCode.F6, false));
         final Stage editor = demo.nextStage();
         FxTestSupport.run(() -> {
            assertEquals(demo.main, editor.getOwner());
            assertEquals(Modality.WINDOW_MODAL, editor.getModality());
            final var name = node(editor, "nameField", ForeTextField.class);
            name.setText("   ");
            node(editor, "saveButton", Button.class).fire();
            assertTrue(editor.isShowing());
            assertEquals(3, demo.store.snapshot().size());

            name.setText("Я".repeat(121));
            node(editor, "saveButton", Button.class).fire();
            assertTrue(editor.isShowing());
            assertEquals(3, demo.store.snapshot().size());

            name.setText("  Новая запись  ");
            node(editor, "saveButton", Button.class).fire();
            assertFalse(editor.isShowing());
            assertEquals(4, demo.store.snapshot().size());
            assertEquals("Новая запись", table(demo.main).getSelectionModel().getSelectedItem().name());
            assertEquals("Записей: 4", node(demo.main, "countLabel", javafx.scene.control.Label.class).getText());
            button(demo.main, StandardAction.CREATE).fire();
         });
         final Stage cancelled = demo.nextStage();
         FxTestSupport.run(() -> {
            node(cancelled, "nameField", ForeTextField.class).setText("Отменённая запись");
            node(cancelled, "cancelButton", Button.class).fire();
            assertFalse(cancelled.isShowing());
            assertEquals(4, demo.store.snapshot().size());
            assertTrue(demo.store.snapshot().stream().noneMatch(row -> row.name().equals("Отменённая запись")));
         });
      }
   }

   @Test
   void editingKeepsSelectionByIdAfterSortingAndCancelLeavesTheRecordUnchanged() throws Exception
   {
      try( var demo = Demo.open() )
      {
         FxTestSupport.run(() -> {
            final var table = table(demo.main);
            final var nameColumn = table.getColumns().get(1);
            nameColumn.setSortType(TableColumn.SortType.ASCENDING);
            table.getSortOrder().add(nameColumn);
            table.getSelectionModel().selectFirst();
            assertEquals(1, table.getSelectionModel().getSelectedItem().id());
            menu(demo.main, StandardAction.UPDATE).fire();
         });
         final Stage editor = demo.nextStage();
         FxTestSupport.run(() -> {
            node(editor, "nameField", ForeTextField.class).setText("Янтарь");
            node(editor, "saveButton", Button.class).fire();
            final var table = table(demo.main);
            assertEquals(1, table.getSelectionModel().getSelectedItem().id());
            assertEquals("Янтарь", table.getSelectionModel().getSelectedItem().name());
            assertEquals(1, table.getItems().getLast().id());
            assertEquals(3, demo.store.snapshot().size());
            press(demo.main, KeyCode.F4, false);
         });
         final Stage cancelled = demo.nextStage();
         FxTestSupport.run(() -> {
            node(cancelled, "nameField", ForeTextField.class).setText("Не сохранять");
            node(cancelled, "cancelButton", Button.class).fire();
            assertEquals("Янтарь", demo.store.snapshot().stream().filter(row -> row.id() == 1).findFirst().orElseThrow().name());
            assertEquals(1, table(demo.main).getSelectionModel().getSelectedItem().id());
         });
      }
   }

   @Test
   void viewCannotSaveAndDeleteUpdatesSelectionAndEmptyTableState() throws Exception
   {
      try( var demo = Demo.open() )
      {
         FxTestSupport.run(() -> {
            table(demo.main).getSelectionModel().select(1);
            press(demo.main, KeyCode.F3, false);
         });
         final Stage viewer = demo.nextStage();
         FxTestSupport.run(() -> {
            final var name = node(viewer, "nameField", ForeTextField.class);
            final var save = node(viewer, "saveButton", Button.class);
            assertFalse(name.isEditable());
            assertFalse(save.isVisible());
            assertFalse(save.isManaged());
            name.setText("Изменение в просмотре");
            save.fire();
            assertTrue(viewer.isShowing());
            node(viewer, "cancelButton", Button.class).fire();
            assertEquals("Бета", demo.store.snapshot().get(1).name());

            menu(demo.main, StandardAction.REFRESH).fire();
            assertEquals(2, table(demo.main).getSelectionModel().getSelectedItem().id());
            press(demo.main, KeyCode.F6, true);
            assertEquals(2, demo.store.snapshot().size());
            assertEquals(3, table(demo.main).getSelectionModel().getSelectedItem().id());
            button(demo.main, StandardAction.DELETE).fire();
            menu(demo.main, StandardAction.DELETE).fire();
            assertTrue(table(demo.main).getItems().isEmpty());
            assertTrue(demo.store.snapshot().isEmpty());
            assertSelectionActionsDisabled(demo.main, true);
            assertFalse(button(demo.main, StandardAction.CREATE).isDisable());
            assertFalse(button(demo.main, StandardAction.REFRESH).isDisable());
         });
      }
   }

   @Test
   void closingTheListRemovesShortcutsAndLiveControlBindings() throws Exception
   {
      try( var demo = Demo.open() )
      {
         FxTestSupport.run(() -> {
            final var createButton = button(demo.main, StandardAction.CREATE);
            final var createMenu = menu(demo.main, StandardAction.CREATE);
            final var action = createButton.getAction();
            demo.main.hide();
            assertTrue(action.isDisabled());
            assertNull(createButton.getAction());
            assertNull(createMenu.getAction());
            assertFalse(createButton.textProperty().isBound());
            assertFalse(createMenu.acceleratorProperty().isBound());
            press(demo.main, KeyCode.F6, false);
            assertTrue(demo.shown.isEmpty());
         });
      }
   }

   private static void assertSelectionActionsDisabled(Stage stage, boolean disabled)
   {
      for( StandardAction type : List.of(StandardAction.UPDATE, StandardAction.VIEW, StandardAction.DELETE) )
      {
         assertEquals(disabled, button(stage, type).isDisable());
         assertEquals(disabled, menu(stage, type).isDisable());
      }
   }

   private static ForeButton button(Stage stage, StandardAction type)
   {
      return node(stage, type.name().toLowerCase(Locale.ROOT) + "Button", ForeButton.class);
   }

   private static ForeMenuItem menu(Stage stage, StandardAction type)
   {
      final var bar = (MenuBar) stage.getScene().getRoot().lookup(".menu-bar");
      return bar.getMenus().getFirst().getItems().stream()
              .filter(item -> item instanceof ForeMenuItem fore && fore.getStandardAction() == type)
              .map(ForeMenuItem.class::cast).findFirst().orElseThrow();
   }

   @SuppressWarnings("unchecked")
   private static TableView<RecordStore.Row> table(Stage stage)
   {
      return (TableView<RecordStore.Row>) node(stage, "table", TableView.class);
   }

   private static <T extends Node> T node(Stage stage, String id, Class<T> type)
   {
      return type.cast(stage.getScene().getRoot().lookup("#" + id));
   }

   private static void press(Stage stage, KeyCode code, boolean shift)
   {
      Event.fireEvent(stage.getScene(), new KeyEvent(KeyEvent.KEY_PRESSED,
              KeyEvent.CHAR_UNDEFINED, "", code, shift, false, false, false));
   }

   private static final class Demo implements AutoCloseable
   {
      final RecordStore store = RecordStore.sample();
      final BlockingQueue<Stage> shown = new LinkedBlockingQueue<>();
      final List<Stage> stages = new ArrayList<>();
      final List<Throwable> errors = new CopyOnWriteArrayList<>();
      Stage main;
      Thread.UncaughtExceptionHandler previousHandler;
      final ListChangeListener<Window> tracker = change -> {
         while( change.next() )
            for( Window window : change.getAddedSubList() )
               if( window instanceof Stage stage )
               {
                  stages.add(stage);
                  shown.add(stage);
               }
      };

      static Demo open() throws Exception
      {
         final var demo = new Demo();
         FxTestSupport.run(() -> {
            demo.previousHandler = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> demo.errors.add(error));
            Window.getWindows().addListener(demo.tracker);
         });
         try
         {
            new FormLauncher<RecordStore, RecordTableController>(null, null, RecordTableController.class)
                    .fxml("ru/inversion/fore/demo/table/record-table.fxml")
                    .bundle("ru.inversion.fore.demo.table.messages")
                    .dataObject(demo.store).runForm();
            demo.main = demo.nextStage();
            FxTestSupport.run(() -> assertTrue(demo.main.isShowing()));
            return demo;
         }
         catch( Exception | Error ex )
         {
            demo.close();
            throw ex;
         }
      }

      Stage nextStage() throws Exception
      {
         final Stage stage = shown.poll(10, TimeUnit.SECONDS);
         assertNotNull(stage, "Окно не открылось; ошибки: " + errors);
         return stage;
      }

      @Override
      public void close() throws Exception
      {
         FxTestSupport.run(() -> {
            for( int index = stages.size() - 1; index >= 0; index-- )
               stages.get(index).hide();
            Window.getWindows().removeListener(tracker);
            Thread.currentThread().setUncaughtExceptionHandler(previousHandler);
         });
         assertTrue(errors.isEmpty(), errors.toString());
      }
   }
}
