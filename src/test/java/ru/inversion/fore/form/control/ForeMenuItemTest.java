package ru.inversion.fore.form.control;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ForeMenuItemTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void standardPresentationChangesWithoutBindingProperties() throws Exception
   {
      FxTestSupport.run(() -> {
         final var item = new ForeMenuItem();
         final var other = new ForeMenuItem();
         item.setStandardAction(StandardAction.CREATE);
         other.setStandardAction(StandardAction.CREATE);
         assertEquals(StandardAction.CREATE, item.getStandardAction());
         assertNotSame(item.getAction(), other.getAction());
         assertNotSame(item.getGraphic(), other.getGraphic());
         assertEquals(StandardAction.CREATE.text(), item.getText());
         assertEquals(new KeyCodeCombination(KeyCode.F2), item.getAccelerator());
         assertFalse(item.textProperty().isBound());
         assertFalse(item.graphicProperty().isBound());
         assertFalse(item.acceleratorProperty().isBound());

         item.setStandardAction(StandardAction.UPDATE);
         assertEquals(StandardAction.UPDATE.text(), item.getText());
         assertEquals(new KeyCodeCombination(KeyCode.F4), item.getAccelerator());
         item.setStandardAction(null);
         assertNull(item.getAction());
         assertNull(item.getStandardAction());
         assertNull(item.getText());
         assertNull(item.getGraphic());
         assertNull(item.getAccelerator());
      });
   }

   @Test
   void explicitPresentationAndHandlerSurviveEitherSetterOrder() throws Exception
   {
      FxTestSupport.run(() -> {
         for( boolean standardFirst : new boolean[]{ false, true } )
         {
            final var item = new ForeMenuItem();
            final var graphic = new Label("Своя графика");
            final var accelerator = new KeyCodeCombination(KeyCode.F10);
            final var calls = new AtomicInteger();
            final EventHandler<ActionEvent> handler = event -> calls.incrementAndGet();
            if( standardFirst ) item.setStandardAction(StandardAction.CREATE);
            item.setText("Свой текст");
            item.setGraphic(graphic);
            item.setAccelerator(accelerator);
            item.setOnAction(handler);
            if( !standardFirst ) item.setStandardAction(StandardAction.CREATE);
            item.setStandardAction(StandardAction.UPDATE);
            item.setStandardAction(null);
            assertEquals("Свой текст", item.getText());
            assertSame(graphic, item.getGraphic());
            assertEquals(accelerator, item.getAccelerator());
            assertSame(handler, item.getOnAction());
            item.fire();
            assertEquals(1, calls.get());
         }
      });
   }

   @Test
   void sharedActionUpdatesMenuStateAndCanBeRemoved() throws Exception
   {
      FxTestSupport.run(() -> {
         final var calls = new AtomicInteger();
         final var action = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         final var item = new ForeMenuItem(action);
         assertSame(action, item.getAction());
         action.setText("Добавить запись");
         action.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9)));
         assertEquals("Добавить запись", item.getText());
         assertEquals(new KeyCodeCombination(KeyCode.F9), item.getAccelerator());
         item.fire();
         action.setDisabled(true);
         item.fire();
         assertEquals(1, calls.get());
         assertTrue(item.isDisable());

         item.setAction(null);
         assertNull(item.getAction());
         assertNull(item.getStandardAction());
         assertNull(item.getOnAction());
         assertNull(item.getText());
         assertNull(item.getGraphic());
         assertNull(item.getAccelerator());
         assertFalse(item.isDisable());
         action.setText("Старое действие");
         action.setHotkeys(StandardAction.UPDATE.hotkeys());
         assertNull(item.getText());
         assertNull(item.getAccelerator());
         item.fire();
         assertEquals(1, calls.get());
      });
   }

   @Test
   void changingActionsDisconnectsOldPropertiesAndHandler() throws Exception
   {
      FxTestSupport.run(() -> {
         final var firstCalls = new AtomicInteger();
         final var secondCalls = new AtomicInteger();
         final var first = ForeActions.create(StandardAction.CREATE, event -> firstCalls.incrementAndGet());
         final var second = ForeActions.create(StandardAction.UPDATE, event -> secondCalls.incrementAndGet());
         first.getProperties().put("menu-test", "Первое значение");
         second.getProperties().put("menu-test", "Второе значение");
         final var item = new ForeMenuItem(first);
         item.setAction(second);
         item.setAction(second);
         first.setText("Старое действие");
         first.setDisabled(true);
         first.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9)));
         first.getProperties().put("menu-test", "Старое свойство");
         assertEquals(StandardAction.UPDATE, item.getStandardAction());
         assertEquals(StandardAction.UPDATE.text(), item.getText());
         assertEquals(new KeyCodeCombination(KeyCode.F4), item.getAccelerator());
         assertFalse(item.isDisable());
         assertEquals("Второе значение", item.getProperties().get("menu-test"));
         second.getProperties().put("menu-test", "Новое свойство");
         assertEquals("Новое свойство", item.getProperties().get("menu-test"));
         item.fire();
         assertEquals(0, firstCalls.get());
         assertEquals(1, secondCalls.get());
      });
   }

   @Test
   void returningToStandardPresentationUnbindsTheActionAndKeepsAUserHandler() throws Exception
   {
      FxTestSupport.run(() -> {
         final var action = ForeActions.create(StandardAction.CREATE, event -> fail("Старый обработчик вызван"));
         final var item = new ForeMenuItem(action);
         final var calls = new AtomicInteger();
         final EventHandler<ActionEvent> handler = event -> calls.incrementAndGet();
         item.setOnAction(handler);
         item.setStandardAction(StandardAction.UPDATE);
         action.setText("Старое действие");
         action.setDisabled(true);
         action.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9)));
         assertEquals(StandardAction.UPDATE.text(), item.getText());
         assertEquals(new KeyCodeCombination(KeyCode.F4), item.getAccelerator());
         assertFalse(item.isDisable());
         assertSame(handler, item.getOnAction());
         assertFalse(item.textProperty().isBound());
         assertFalse(item.graphicProperty().isBound());
         assertFalse(item.acceleratorProperty().isBound());
         assertFalse(item.disableProperty().isBound());
         item.fire();
         assertEquals(1, calls.get());
      });
   }

   @Test
   void fxmlLoadsExplicitPresentationAndCallsItsHandlerOnce() throws Exception
   {
      FxTestSupport.run(() -> {
         for( String attributes : new String[]{
                 "standardAction=\"CREATE\" text=\"Свой текст\" accelerator=\"F10\" onAction=\"#onCreate\"",
                 "text=\"Свой текст\" accelerator=\"F10\" onAction=\"#onCreate\" standardAction=\"CREATE\""
         } )
         {
            final String fxml = """
                    <?import javafx.scene.control.Label?>
                    <?import javafx.scene.control.Menu?>
                    <?import javafx.scene.control.MenuBar?>
                    <?import ru.inversion.fore.form.control.ForeMenuItem?>
                    <MenuBar xmlns:fx="http://javafx.com/fxml/1">
                        <Menu text="Действия">
                            <ForeMenuItem %s>
                                <graphic><Label text="Своя графика"/></graphic>
                            </ForeMenuItem>
                        </Menu>
                    </MenuBar>
                    """.formatted(attributes);
            final var controller = new FxmlController();
            final var loader = new FXMLLoader();
            loader.setController(controller);
            final MenuBar bar = loader.load(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
            final var item = (ForeMenuItem) bar.getMenus().getFirst().getItems().getFirst();
            assertEquals(StandardAction.CREATE, item.getStandardAction());
            assertEquals("Свой текст", item.getText());
            assertEquals(new KeyCodeCombination(KeyCode.F10), item.getAccelerator());
            assertEquals("Своя графика", ((Label) item.getGraphic()).getText());
            item.fire();
            assertEquals(1, controller.calls);
         }
      });
   }

   @Test
   void nativeAcceleratorsMoveToTheNewActionAndAreRemovedOnDetach() throws Exception
   {
      FxTestSupport.run(() -> {
         final var firstCalls = new AtomicInteger();
         final var secondCalls = new AtomicInteger();
         final var unrelatedCalls = new AtomicInteger();
         final var first = ForeActions.create(StandardAction.CREATE, event -> firstCalls.incrementAndGet());
         final var second = ForeActions.create(StandardAction.UPDATE, event -> secondCalls.incrementAndGet());
         final var item = new ForeMenuItem(first);
         final var bar = new MenuBar(new Menu("Действия", null, item));
         final var scene = new Scene(bar);
         final var stage = new Stage();
         stage.setScene(scene);
         try
         {
            stage.show();
            bar.applyCss();
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F12), unrelatedCalls::incrementAndGet);
            assertTrue(scene.getAccelerators().containsKey(new KeyCodeCombination(KeyCode.F2)));
            press(scene, KeyCode.F2);
            assertEquals(1, firstCalls.get());

            first.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9)));
            assertFalse(scene.getAccelerators().containsKey(new KeyCodeCombination(KeyCode.F2)));
            press(scene, KeyCode.F2);
            press(scene, KeyCode.F9);
            assertEquals(2, firstCalls.get());

            item.setAction(second);
            press(scene, KeyCode.F9);
            press(scene, KeyCode.F4);
            assertEquals(2, firstCalls.get());
            assertEquals(1, secondCalls.get());

            item.setAction(null);
            assertFalse(scene.getAccelerators().containsKey(new KeyCodeCombination(KeyCode.F4)));
            press(scene, KeyCode.F4);
            press(scene, KeyCode.F12);
            assertEquals(1, secondCalls.get());
            assertEquals(1, unrelatedCalls.get());
         }
         finally
         {
            stage.hide();
         }
      });
   }

   private static void press(Scene scene, KeyCode code)
   {
      Event.fireEvent(scene, new KeyEvent(KeyEvent.KEY_PRESSED,
              KeyEvent.CHAR_UNDEFINED, "", code, false, false, false, false));
   }

   public static final class FxmlController
   {
      private int calls;
      @FXML private void onCreate() { calls++; }
   }
}
