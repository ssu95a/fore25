package ru.inversion.fore.form.control;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fore.form.action.StandardAction;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ForeButtonTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void standardActionIsReadFromTheIndependentButtonAction() throws Exception
   {
      FxTestSupport.run(() -> {
         final var first = new ForeButton();
         final var second = new ForeButton();
         first.setStandardAction(StandardAction.CREATE);
         second.setStandardAction(StandardAction.CREATE);
         assertEquals(first.getAction().standardType(), first.getStandardAction());
         assertEquals(first.getAction().getText(), first.getText());
         assertEquals(first.getAction().getLongText(), first.getTooltip().getText());
         assertSame(first.getAction().getGraphic(), first.getGraphic());
         assertNotSame(first.getAction(), second.getAction());
         assertNotSame(first.getGraphic(), second.getGraphic());
      });
   }

   @Test
   void explicitValuesAndHandlerSurviveEitherSetterOrder() throws Exception
   {
      FxTestSupport.run(() -> {
         for( boolean standardFirst : new boolean[]{ false, true } )
         {
            final var button = new ForeButton();
            final var tooltip = new Tooltip("Своя подсказка");
            final var graphic = new Label("Своя графика");
            final var calls = new AtomicInteger();
            final EventHandler<ActionEvent> handler = event -> calls.incrementAndGet();
            if( standardFirst ) button.setStandardAction(StandardAction.CREATE);
            button.setText("Свой текст");
            button.setTooltip(tooltip);
            button.setGraphic(graphic);
            button.setOnAction(handler);
            if( !standardFirst ) button.setStandardAction(StandardAction.CREATE);
            button.setStandardAction(StandardAction.UPDATE);
            assertEquals("Свой текст", button.getText());
            assertSame(tooltip, button.getTooltip());
            assertSame(graphic, button.getGraphic());
            assertSame(handler, button.getOnAction());
            button.fire();
            assertEquals(1, calls.get());
         }
      });
   }

   @Test
   void changingOrClearingTheTypeReplacesOnlyStandardPresentation() throws Exception
   {
      FxTestSupport.run(() -> {
         final var button = new ForeButton();
         button.setStandardAction(StandardAction.CREATE);
         button.setStandardAction(StandardAction.UPDATE);
         assertEquals(StandardAction.UPDATE.text(), button.getText());
         assertEquals(StandardAction.UPDATE.tooltip(), button.getTooltip().getText());
         assertSame(button.getAction().getGraphic(), button.getGraphic());
         final Tooltip changed = button.getTooltip();
         changed.setText("Изменённая подсказка");
         button.setStandardAction(StandardAction.DELETE);
         assertSame(changed, button.getTooltip());
         assertEquals("Изменённая подсказка", changed.getText());
         button.setStandardAction(null);
         assertNull(button.getAction());
         assertNull(button.getStandardAction());
         assertNull(button.getText());
         assertNull(button.getGraphic());
         assertSame(changed, button.getTooltip());
      });
   }

   @Test
   void sharedActionUpdatesTheButtonAndCanBeRemoved() throws Exception
   {
      FxTestSupport.run(() -> {
         final var calls = new AtomicInteger();
         final var action = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         final var button = new ForeButton(action);
         assertSame(action, button.getAction());
         action.setText("Добавить запись");
         action.setLongText("Подсказка действия");
         assertEquals("Добавить запись", button.getText());
         assertEquals("Подсказка действия", button.getTooltip().getText());
         button.fire();
         action.setDisabled(true);
         button.fire();
         assertEquals(1, calls.get());
         assertTrue(button.isDisabled());
         button.setAction(null);
         assertNull(button.getAction());
         assertNull(button.getStandardAction());
         assertNull(button.getOnAction());
         assertNull(button.getText());
         assertNull(button.getTooltip());
         assertNull(button.getGraphic());
         assertFalse(button.isDisabled());
         action.setText("Старое действие");
         assertNull(button.getText());
         button.fire();
         assertEquals(1, calls.get());
      });
   }

   @Test
   void returningToStandardPresentationUnbindsTheActionAndKeepsAUserHandler() throws Exception
   {
      FxTestSupport.run(() -> {
         final var action = ForeActions.create(StandardAction.CREATE, event -> fail("Старый обработчик вызван"));
         final var button = new ForeButton(action);
         final var calls = new AtomicInteger();
         final EventHandler<ActionEvent> handler = event -> calls.incrementAndGet();
         button.setOnAction(handler);
         button.setStandardAction(StandardAction.UPDATE);
         action.setText("Старое действие");
         action.setDisabled(true);
         assertEquals(StandardAction.UPDATE.text(), button.getText());
         assertFalse(button.isDisabled());
         assertSame(handler, button.getOnAction());
         assertFalse(button.textProperty().isBound());
         assertFalse(button.graphicProperty().isBound());
         assertFalse(button.tooltipProperty().isBound());
         assertFalse(button.disableProperty().isBound());
         button.fire();
         assertEquals(1, calls.get());
      });
   }

   @Test
   void fxmlLoadsExplicitPresentationAndCallsItsHandlerOnce() throws Exception
   {
      FxTestSupport.run(() -> {
         for( String attributes : new String[]{
                 "standardAction=\"CREATE\" text=\"Свой текст\" onAction=\"#onCreate\"",
                 "text=\"Свой текст\" onAction=\"#onCreate\" standardAction=\"CREATE\""
         } )
         {
            final String fxml = """
                    <?import javafx.scene.control.Label?>
                    <?import javafx.scene.control.Tooltip?>
                    <?import ru.inversion.fore.form.control.ForeButton?>
                    <ForeButton xmlns:fx="http://javafx.com/fxml/1" %s>
                        <tooltip><Tooltip text="Своя подсказка"/></tooltip>
                        <graphic><Label text="Своя графика"/></graphic>
                    </ForeButton>
                    """.formatted(attributes);
            final var controller = new FxmlController();
            final var loader = new FXMLLoader();
            loader.setController(controller);
            final ForeButton button = loader.load(new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
            assertEquals(StandardAction.CREATE, button.getStandardAction());
            assertEquals("Свой текст", button.getText());
            assertEquals("Своя подсказка", button.getTooltip().getText());
            assertEquals("Своя графика", ((Label) button.getGraphic()).getText());
            button.fire();
            assertEquals(1, controller.calls);
         }
      });
   }

   public static final class FxmlController
   {
      private int calls;
      @FXML private void onCreate() { calls++; }
   }
}
