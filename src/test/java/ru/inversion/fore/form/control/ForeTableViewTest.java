package ru.inversion.fore.form.control;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.action.ActionKeyBinder;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Проверки выбора, сортировки и настоящих событий таблицы на потоке JavaFX. */
class ForeTableViewTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void enterUsesTheCurrentActionAndRespectsSelectionDisabledStateAndModifiers() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var first = new AtomicInteger();
            final var second = new AtomicInteger();
            final ForeAction a = ForeActions.create(StandardAction.UPDATE, event -> {
               assertSame(fixture.table, event.getSource());
               first.incrementAndGet();
            });
            final ForeAction b = ForeActions.create(StandardAction.VIEW, event -> second.incrementAndGet());
            fixture.table.setActivationAction(a);
            enter(fixture.table, false);
            assertEquals(0, first.get());
            fixture.table.getSelectionModel().selectFirst();
            enter(fixture.table, false);
            assertEquals(1, first.get());
            a.setDisabled(true);
            enter(fixture.table, false);
            a.setDisabled(false);
            enter(fixture.table, true);
            fixture.table.setDisable(true);
            enter(fixture.table, false);
            fixture.table.setDisable(false);
            assertEquals(1, first.get());
            fixture.table.setActivationAction(b);
            fixture.table.setActivationAction(b);
            enter(fixture.table, false);
            assertEquals(1, second.get());
            fixture.table.setActivationAction(a);
            enter(fixture.table, false);
            assertEquals(2, first.get());
         }
      });
   }

   @Test
   void doubleClickSelectsTheClickedRowAndIgnoresOtherClicksHeadersAndBlankSpace() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var calls = new AtomicInteger();
            final var action = ForeActions.create(StandardAction.UPDATE, event -> calls.incrementAndGet());
            fixture.table.setActivationAction(action);
            action.disabledProperty().bind(fixture.table.getSelectionModel().selectedItemProperty().isNull());
            try
            {
               click(fixture.row(1), MouseButton.PRIMARY, 2, false);
               assertEquals(1, calls.get());
               assertEquals(2, fixture.selected().id());
               click(fixture.row(0), MouseButton.PRIMARY, 1, false);
               click(fixture.row(0), MouseButton.SECONDARY, 2, false);
               click(fixture.row(0), MouseButton.PRIMARY, 2, true);
               click(fixture.table, MouseButton.PRIMARY, 2, false);
               click(fixture.table.lookup(".column-header"), MouseButton.PRIMARY, 2, false);
               click(fixture.row(3), MouseButton.PRIMARY, 2, false);
               assertEquals(1, calls.get());
            }
            finally { action.disabledProperty().unbind(); }
         }
      });
   }

   @Test
   void aSelectionListenerCanRemoveTheActionDuringDoubleClick() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var calls = new AtomicInteger();
            fixture.table.setActivationAction(ForeActions.create(StandardAction.UPDATE, event -> calls.incrementAndGet()));
            fixture.table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
               if( value != null ) fixture.table.setActivationAction(null);
            });
            assertDoesNotThrow(() -> click(fixture.row(1), MouseButton.PRIMARY, 2, false));
            assertEquals(0, calls.get());
            assertNull(fixture.table.getActivationAction());
         }
      });
   }

   @Test
   void embeddedButtonsAndTextFieldsHandleTheirOwnEvents() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var calls = new AtomicInteger();
            fixture.table.setActivationAction(ForeActions.create(StandardAction.UPDATE, event -> calls.incrementAndGet()));
            fixture.table.getSelectionModel().selectFirst();
            fixture.nameColumn.setCellFactory(column -> new TableCell<>() {
               @Override protected void updateItem(String item, boolean empty)
               {
                  super.updateItem(item, empty);
                  if( empty ) setGraphic(null);
                  else
                  {
                     final Node control = getIndex() == 0 ? new ForeButton() : new TextField(item);
                     control.setId("embedded-" + getIndex());
                     setGraphic(control);
                  }
               }
            });
            fixture.layout();
            final Node button = fixture.table.lookup("#embedded-0");
            final Node field = fixture.table.lookup("#embedded-1");
            assertNotNull(button);
            assertNotNull(field);
            enter(button, false);
            click(button, MouseButton.PRIMARY, 2, false);
            enter(field, false);
            click(field, MouseButton.PRIMARY, 2, false);
            assertEquals(0, calls.get());
         }
      });
   }

   @Test
   void inlineEditingAndMissingSelectionModelsDoNotActivateRecords() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var calls = new AtomicInteger();
            fixture.table.setActivationAction(ForeActions.create(StandardAction.UPDATE, event -> calls.incrementAndGet()));
            fixture.table.setEditable(true);
            fixture.nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
            fixture.layout();
            fixture.table.getSelectionModel().selectFirst();
            fixture.table.edit(0, fixture.nameColumn);
            assertNotNull(fixture.table.getEditingCell());
            click(fixture.row(0), MouseButton.PRIMARY, 2, false);
            final Node editor = fixture.table.lookup(".text-field");
            assertNotNull(editor);
            enter(editor, false);
            assertEquals(0, calls.get());
            fixture.table.edit(-1, null);
            fixture.table.setSelectionModel(null);
            enter(fixture.table, false);
            assertEquals(0, calls.get());
         }
      });
   }

   @Test
   void aSceneShortcutAndTableActivationInvokeTheSharedActionOnlyOnce() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture(); var binder = new ActionKeyBinder(fixture.stage.getScene()) )
         {
            final var calls = new AtomicInteger();
            final var action = ForeActions.create(StandardAction.UPDATE, event -> calls.incrementAndGet());
            action.setHotkeys(List.of(new KeyCodeCombination(KeyCode.ENTER)));
            fixture.table.setActivationAction(action);
            fixture.table.getSelectionModel().selectFirst();
            binder.bind(action);
            enter(fixture.table, false);
            assertEquals(1, calls.get());
            action.setDisabled(true);
            enter(fixture.table, false);
            assertEquals(1, calls.get());
         }
      });
   }

   @Test
   void removingTheActionKeepsUserHandlersAndRemovesBothActivationPaths() throws Exception
   {
      FxTestSupport.run(() -> {
         try( var fixture = new Fixture() )
         {
            final var calls = new AtomicInteger();
            fixture.table.setOnKeyPressed(event -> {});
            fixture.table.setOnMouseClicked(event -> {});
            final var keyHandler = fixture.table.getOnKeyPressed();
            final var mouseHandler = fixture.table.getOnMouseClicked();
            fixture.table.setActivationAction(ForeActions.create(StandardAction.UPDATE, event -> calls.incrementAndGet()));
            fixture.table.getSelectionModel().selectFirst();
            enter(fixture.table, false);
            fixture.table.setActivationAction(null);
            enter(fixture.table, false);
            click(fixture.row(1), MouseButton.PRIMARY, 2, false);
            assertEquals(1, calls.get());
            assertSame(keyHandler, fixture.table.getOnKeyPressed());
            assertSame(mouseHandler, fixture.table.getOnMouseClicked());
         }
      });
   }

   @Test
   void fxmlCreatesTheControlAndItsStandardColumnsWithoutAnApplicationContext() throws Exception
   {
      FxTestSupport.run(() -> {
         final String fxml = """
                 <?import javafx.scene.control.TableColumn?>
                 <?import ru.inversion.fore.form.control.ForeTableView?>
                 <ForeTableView xmlns:fx="http://javafx.com/fxml/1" prefWidth="640">
                     <columns><TableColumn text="Код"/><TableColumn text="Название"/></columns>
                 </ForeTableView>
                 """;
         final ForeTableView<?> table = new FXMLLoader().load(
                 new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
         assertEquals(2, table.getColumns().size());
         assertEquals("Название", table.getColumns().get(1).getText());
         assertEquals(640, table.getPrefWidth());
         assertTrue(table.getStyleClass().contains("fore-table-view"));
         assertNull(table.getActivationAction());
      });
   }

   @Test
   void activationRequiresTheFxThread() throws Exception
   {
      final var ref = new AtomicReference<Fixture>();
      FxTestSupport.run(() -> ref.set(new Fixture()));
      final Fixture fixture = ref.get();
      try
      {
         assertThrows(IllegalStateException.class, () -> fixture.table.setActivationAction(null));
         FxTestSupport.run(() -> assertEquals(3, fixture.source.size()));
      }
      finally { FxTestSupport.run(fixture::close); }
   }

   private static void enter(Node target, boolean control)
   {
      Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED,
              KeyEvent.CHAR_UNDEFINED, "", KeyCode.ENTER, false, control, false, false));
   }

   private static void click(Node target, MouseButton button, int count, boolean shift)
   {
      Event.fireEvent(target, new MouseEvent(MouseEvent.MOUSE_CLICKED, 5, 5, 5, 5,
              button, count, shift, false, false, false, false, false, false,
              true, false, true, new PickResult(target, 5, 5)));
   }

   private record Row(long id, String name) {}

   private static final class Fixture implements AutoCloseable
   {
      final ObservableList<Row> source = FXCollections.observableArrayList(
              new Row(1, "Альфа"), new Row(2, "Бета"), new Row(3, "Гамма"));
      final SortedList<Row> sorted = new SortedList<>(source);
      final ForeTableView<Row> table = new ForeTableView<>(sorted);
      final TableColumn<Row, String> nameColumn = new TableColumn<>("Название");
      final Stage stage = new Stage();

      Fixture()
      {
         nameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
         nameColumn.setPrefWidth(280);
         table.getColumns().add(nameColumn);
         sorted.comparatorProperty().bind(table.comparatorProperty());
         table.getSortOrder().add(nameColumn);
         stage.setScene(new Scene(table, 320, 240));
         stage.show();
         layout();
      }

      Row selected() { return table.getSelectionModel().getSelectedItem(); }
      List<Long> ids() { return table.getItems().stream().map(Row::id).toList(); }
      void layout() { table.applyCss(); table.layout(); }

      TableRow<?> row(int index)
      {
         return table.lookupAll(".table-row-cell").stream()
                 .filter(node -> node instanceof TableRow<?> row && row.getIndex() == index)
                 .map(node -> (TableRow<?>) node).findFirst().orElseThrow();
      }

      @Override public void close()
      {
         table.setActivationAction(null);
         sorted.comparatorProperty().unbind();
         stage.hide();
      }
   }
}
