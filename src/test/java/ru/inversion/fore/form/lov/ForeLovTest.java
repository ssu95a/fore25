package ru.inversion.fore.form.lov;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ForeLovTest
{
   private final List<ForeLov> lovs = new ArrayList<>();
   private final List<LovBinding> bindings = new ArrayList<>();
   private Stage owner;
   private TextField field;
   private TextField next;
   private SimpleObjectProperty<Long> id;
   private SimpleStringProperty name;

   @BeforeAll static void toolkit() throws Exception { FxTestSupport.start(); }

   @BeforeEach void owner() throws Exception
   {
      FxTestSupport.run(() -> {
         field = new TextField();
         next = new TextField();
         id = new SimpleObjectProperty<>(99L);
         name = new SimpleStringProperty("Старое значение");
         owner = new Stage();
         owner.setScene(new Scene(new VBox(field, next), 500, 200));
         owner.show();
      });
   }

   @AfterEach void cleanup() throws Exception
   {
      FxTestSupport.run(() -> {
         bindings.forEach(LovBinding::close);
         lovs.forEach(ForeLov::close);
         owner.close();
      });
   }

   private ForeLov lov(String behavior, String search, LovDataSource source) throws Exception
   {
      var lov = new ForeLov(LovFixtures.definition(behavior, search), source);
      lovs.add(lov);
      return lov;
   }

   private LovBinding binding(ForeLov lov) throws Exception
   {
      return fx(() -> {
         var binding = new LovBinding(lov, field, Map.of("id", LovBinding.Target.of(id, Long.class),
               "code", LovBinding.Target.text(field.textProperty()), "name", LovBinding.Target.text(name)), Map::of, next::requestFocus);
         bindings.add(binding);
         return binding;
      });
   }

   private static <T> T fx(Callable<T> callable) throws Exception
   {
      var result = new java.util.concurrent.atomic.AtomicReference<T>();
      FxTestSupport.run(() -> result.set(callable.call()));
      return result.get();
   }

   @SuppressWarnings("unchecked")
   private static TableView<LovRow> table(ForeLov lov)
   {
      return (TableView<LovRow>) lov.getDialogPane().lookup("#lov-table");
   }

   private static void button(ForeLov lov, ButtonBar.ButtonData data)
   {
      var pane = lov.getDialogPane();
      ButtonType type = pane.getButtonTypes().stream().filter(b -> b.getButtonData() == data).findFirst().orElseThrow();
      ((Button) pane.lookupButton(type)).fire();
   }

   private static void await(Callable<Boolean> condition) throws Exception
   {
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
      while( System.nanoTime() < deadline )
      {
         if( fx(condition) ) return;
         Thread.sleep(10);
      }
      fail("Ожидаемое состояние JavaFX не наступило");
   }

   @Test void sourceRunsOffFxAndTableIsReadOnlyWithoutHiddenColumn() throws Exception
   {
      var lov = lov("", "", request -> {
         assertFalse(Platform.isFxApplicationThread());
         return new LovDataSource.Result(List.of(LovFixtures.row(1, "A")), true);
      });
      var result = fx(() -> lov.show(field, Map.of(), ""));
      await(() -> table(lov).getItems().size() == 1);
      FxTestSupport.run(() -> {
         assertEquals(List.of("CODE", "NAME"), table(lov).getColumns().stream().map(TableColumn::getId).toList());
         assertTrue(table(lov).getColumns().stream().noneMatch(TableColumn::isSortable));
         assertThrows(UnsupportedOperationException.class, () -> table(lov).getItems().clear());
         button(lov, ButtonBar.ButtonData.OK_DONE);
      });
      assertEquals(1L, result.get(5, TimeUnit.SECONDS).orElseThrow().get("ID"));
   }

   @Test void cancelledSelectionDoesNotChangeAnyTarget() throws Exception
   {
      var lov = lov("", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      var binding = binding(lov);
      FxTestSupport.run(() -> field.setText("A"));
      var result = fx(binding::show);
      await(() -> table(lov).getItems().size() == 1);
      FxTestSupport.run(() -> button(lov, ButtonBar.ButtonData.CANCEL_CLOSE));
      assertFalse(result.get(5, TimeUnit.SECONDS));
      FxTestSupport.run(() -> {
         assertEquals("A", field.getText());
         assertEquals(99L, id.get());
         assertEquals("Старое значение", name.get());
      });
   }

   @Test void uniqueExactMatchReturnsAllColumnsWithoutDialog() throws Exception
   {
      var lov = lov("validate-from-list=\"true\"", "", LovFixtures.rows(LovFixtures.row(1, "A"), LovFixtures.row(2, "AB")));
      var binding = binding(lov);
      FxTestSupport.run(() -> field.setText("a"));
      assertTrue(fx(binding::validate).get(5, TimeUnit.SECONDS));
      FxTestSupport.run(() -> {
         assertFalse(lov.isShowing());
         assertEquals("A", field.getText());
         assertEquals(1L, id.get());
         assertEquals("Имя A", name.get());
         assertEquals(LovBinding.State.VALID, binding.getState());
      });
   }

   @Test void ambiguousExactMatchRequiresChoice() throws Exception
   {
      var lov = lov("validate-from-list=\"true\"", "", LovFixtures.rows(LovFixtures.row(1, "A"), LovFixtures.row(2, "A")));
      var binding = binding(lov);
      FxTestSupport.run(() -> field.setText("A"));
      var result = fx(binding::validate);
      await(() -> lov.isShowing() && table(lov).getItems().size() == 2);
      assertFalse(result.isDone());
      FxTestSupport.run(() -> {
         table(lov).getSelectionModel().select(1);
         button(lov, ButtonBar.ButtonData.OK_DONE);
      });
      assertTrue(result.get(5, TimeUnit.SECONDS));
      assertEquals(2L, fx(id::get));
   }

   @Test void blankValidationClearsHiddenAndVisibleReturns() throws Exception
   {
      var lov = lov("validate-from-list=\"true\"", "", request -> { fail("Запрос для пустого поля не нужен"); return null; });
      var binding = binding(lov);
      assertTrue(fx(binding::validate).get(5, TimeUnit.SECONDS));
      assertNull(fx(id::get));
      assertEquals("", fx(name::get));
   }

   @Test void automaticSelectRequiresCompleteResult() throws Exception
   {
      var complete = lov("auto-select=\"true\"", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      var selected = fx(() -> complete.show(field, Map.of(), ""));
      assertEquals(1L, selected.get(5, TimeUnit.SECONDS).orElseThrow().get("ID"));
      var truncated = lov("auto-select=\"true\"", "max-rows=\"1\"", LovFixtures.rows(LovFixtures.row(1, "A"), LovFixtures.row(2, "AB")));
      var waiting = fx(() -> truncated.show(field, Map.of(), ""));
      await(() -> table(truncated).getItems().size() == 1);
      assertFalse(waiting.isDone());
      assertTrue(fx(() -> ((Label) truncated.getDialogPane().lookup("#lov-status")).getText().contains("Уточните")));
   }

   @Test void filterBeforeDisplayAndMinLengthDeferQuery() throws Exception
   {
      var calls = new AtomicInteger();
      var lov = lov("filter-before-display=\"true\"", "min-length=\"2\"", request -> {
         calls.incrementAndGet();
         return new LovDataSource.Result(List.of(LovFixtures.row(1, "AB")), true);
      });
      fx(() -> lov.show(field, Map.of(), ""));
      FxTestSupport.run(() -> {
         assertEquals(0, calls.get());
         var query = (TextField) lov.getDialogPane().lookup("#lov-query");
         var search = (Button) lov.getDialogPane().lookup("#lov-search");
         query.setText("A"); search.fire();
         assertEquals(0, calls.get());
         query.setText("AB"); search.fire();
      });
      await(() -> table(lov).getItems().size() == 1);
      assertEquals(1, calls.get());
   }

   @Test void cacheKeyIncludesFilterAndParametersAndRefreshInvalidates() throws Exception
   {
      var calls = new AtomicInteger();
      var lov = lov("auto-refresh=\"false\"", "", request -> {
         calls.incrementAndGet();
         return new LovDataSource.Result(List.of(LovFixtures.row(1, request.text())), true);
      });
      var first = fx(() -> lov.query("A", LovDefinition.MatchMode.PREFIX, Map.of("company", 1), false)).get(5, TimeUnit.SECONDS);
      var same = fx(() -> lov.query("A", LovDefinition.MatchMode.PREFIX, Map.of("company", 1), false)).get(5, TimeUnit.SECONDS);
      assertSame(first, same);
      assertEquals(1, calls.get());
      fx(() -> lov.query("A", LovDefinition.MatchMode.PREFIX, Map.of("company", 2), false)).get(5, TimeUnit.SECONDS);
      fx(() -> lov.query("B", LovDefinition.MatchMode.PREFIX, Map.of("company", 2), false)).get(5, TimeUnit.SECONDS);
      fx(() -> lov.query("B", LovDefinition.MatchMode.PREFIX, Map.of("company", 2), true)).get(5, TimeUnit.SECONDS);
      assertEquals(4, calls.get());
   }

   @Test void staleUninterruptibleQueryCannotReplaceNewResults() throws Exception
   {
      var started = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      var finished = new CountDownLatch(1);
      var lov = lov("", "", request -> {
         if( request.text().equals("A") )
         {
            started.countDown();
            while( release.getCount() != 0 )
               try { release.await(); } catch( InterruptedException ignored ) { /* Имитируется драйвер без отмены. */ }
            finished.countDown();
         }
         return new LovDataSource.Result(List.of(LovFixtures.row(1, request.text())), true);
      });
      try
      {
         fx(() -> lov.show(field, Map.of(), "A"));
         assertTrue(started.await(5, TimeUnit.SECONDS));
         FxTestSupport.run(() -> {
            ((TextField) lov.getDialogPane().lookup("#lov-query")).setText("B");
            ((Button) lov.getDialogPane().lookup("#lov-search")).fire();
         });
         await(() -> table(lov).getItems().size() == 1);
         release.countDown();
         assertTrue(finished.await(5, TimeUnit.SECONDS));
         FxTestSupport.run(() -> assertEquals("B", table(lov).getItems().getFirst().get("CODE")));
      }
      finally { release.countDown(); }
   }

   @Test void typingDuringValidationPreventsLateAssignment() throws Exception
   {
      var started = new CountDownLatch(1);
      var release = new CountDownLatch(1);
      var lov = lov("", "", request -> {
         started.countDown(); release.await();
         return new LovDataSource.Result(List.of(LovFixtures.row(1, "A")), true);
      });
      var binding = binding(lov);
      try
      {
         FxTestSupport.run(() -> field.setText("A"));
         var result = fx(binding::validate);
         assertTrue(started.await(5, TimeUnit.SECONDS));
         FxTestSupport.run(() -> field.setText("B"));
         release.countDown();
         assertFalse(result.get(5, TimeUnit.SECONDS));
         assertEquals("B", fx(field::getText));
         assertEquals(99L, fx(id::get));
      }
      finally { release.countDown(); }
   }

   @Test void sourceFailureIsObservableAndNextRequestWorks() throws Exception
   {
      var calls = new AtomicInteger();
      var lov = lov("", "", request -> {
         if( calls.incrementAndGet() == 1 ) throw new IllegalStateException("Сбой источника");
         return new LovDataSource.Result(List.of(LovFixtures.row(1, "A")), true);
      });
      var failed = fx(() -> lov.query("", LovDefinition.MatchMode.PREFIX, Map.of(), false));
      assertThrows(java.util.concurrent.ExecutionException.class, () -> failed.get(5, TimeUnit.SECONDS));
      assertNotNull(fx(lov::getError));
      assertEquals(1, fx(() -> lov.query("", LovDefinition.MatchMode.PREFIX, Map.of(), false)).get(5, TimeUnit.SECONDS).rows().size());
      assertNull(fx(lov::getError));
   }

   @Test void allConversionsAreCheckedBeforeAnyPropertyChanges() throws Exception
   {
      var lov = lov("", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      var binding = fx(() -> {
         var created = new LovBinding(lov, field, Map.of("id", LovBinding.Target.of(id, Long.class),
               "code", LovBinding.Target.text(field.textProperty()),
               "name", new LovBinding.Target<>(name, value -> { throw new IllegalArgumentException("Отказ преобразования"); })));
         bindings.add(created);
         field.setText("A");
         return created;
      });
      assertFalse(fx(binding::validate).get(5, TimeUnit.SECONDS));
      assertEquals(99L, fx(id::get));
      assertEquals("Старое значение", fx(name::get));
      assertInstanceOf(IllegalArgumentException.class, fx(binding::getError));
   }

   @Test void closingBindingRemovesKeyboardHandlerAndCompletesPendingChoice() throws Exception
   {
      var lov = lov("", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      var binding = binding(lov);
      var result = fx(binding::show);
      FxTestSupport.run(binding::close);
      assertFalse(result.get(5, TimeUnit.SECONDS));
      FxTestSupport.run(() -> {
         field.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F9, false, false, false, false));
         assertFalse(lov.isShowing());
      });
   }

   @Test void autoDisplayDoesNotReopenAfterCancel() throws Exception
   {
      var lov = lov("auto-display=\"true\"", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      FxTestSupport.run(next::requestFocus);
      binding(lov);
      FxTestSupport.run(field::requestFocus);
      await(lov::isShowing);
      FxTestSupport.run(() -> button(lov, ButtonBar.ButtonData.CANCEL_CLOSE));
      FxTestSupport.run(() -> {});
      FxTestSupport.run(() -> assertFalse(lov.isShowing()));
   }

   @Test void oversizedProviderResultIsRejected() throws Exception
   {
      var lov = lov("", "max-rows=\"1\"", request -> new LovDataSource.Result(List.of(LovFixtures.row(1, "A"), LovFixtures.row(2, "B")), true));
      var result = fx(() -> lov.query("", LovDefinition.MatchMode.PREFIX, Map.of(), false));
      assertThrows(java.util.concurrent.ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
   }

   @Test void changedParametersCannotApplySelectionFromOldContext() throws Exception
   {
      var company = new AtomicInteger(1);
      var lov = lov("", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      var binding = fx(() -> {
         var created = new LovBinding(lov, field, Map.of("id", LovBinding.Target.of(id, Long.class),
               "code", LovBinding.Target.text(field.textProperty()), "name", LovBinding.Target.text(name)),
               () -> Map.of("company", company.get()), null);
         bindings.add(created);
         return created;
      });
      var result = fx(binding::show);
      await(() -> table(lov).getItems().size() == 1);
      company.set(2);
      FxTestSupport.run(() -> button(lov, ButtonBar.ButtonData.OK_DONE));
      assertFalse(result.get(5, TimeUnit.SECONDS));
      assertEquals(99L, fx(id::get));
      assertInstanceOf(IllegalStateException.class, fx(binding::getError));
   }

   @Test void keyboardInvocationAndAutomaticSkipUsePublicFocusApi() throws Exception
   {
      var lov = lov("auto-skip=\"true\"", "", LovFixtures.rows(LovFixtures.row(1, "A")));
      binding(lov);
      FxTestSupport.run(() -> field.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F9,
            false, false, false, false)));
      await(() -> lov.isShowing() && table(lov).getItems().size() == 1);
      FxTestSupport.run(() -> button(lov, ButtonBar.ButtonData.OK_DONE));
      await(next::isFocused);
      assertEquals(1L, fx(id::get));
   }
}
