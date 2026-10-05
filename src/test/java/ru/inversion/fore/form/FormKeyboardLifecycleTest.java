package ru.inversion.fore.form;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.StandardAction;
import ru.inversion.fore.form.control.ForeButton;
import ru.inversion.fore.form.control.ForeMenuItem;
import ru.inversion.fore.form.control.ForeTextField;
import ru.inversion.fore.form.validation.ValidationResult;

import java.util.ListResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FormKeyboardLifecycleTest
{
   private static final AtomicReference<Harness> NEXT = new AtomicReference<>();

   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void rejectedPreparationClosesBackgroundResourcesWithoutStartingGuiCleanup() throws Exception
   {
      final Harness harness = new Harness(false, true);
      launch(harness);
      assertTrue(harness.backgroundReleased.await(10, TimeUnit.SECONDS));
      assertTrue(harness.backgroundOnVirtualThread);
      assertEquals(0, harness.guiCalls.get());
      assertEquals(1, harness.backgroundCalls.get());
      assertEquals(0, harness.callbackCalls.get());
      assertNull(harness.stage);
      FxTestSupport.run(() -> assertThrows(IllegalStateException.class,
              () -> harness.controller.createAction(StandardAction.CREATE, event -> {})));
   }

   @Test
   void realLaunchKeepsVetoedFormActiveAndReleasesGuiBeforeBackgroundResources() throws Exception
   {
      final Harness harness = new Harness(false);
      final var errors = new CopyOnWriteArrayList<Throwable>();
      final Thread.UncaughtExceptionHandler previous = captureErrors(errors, null);
      try
      {
         launch(harness);
         assertTrue(harness.guiReady.await(10, TimeUnit.SECONDS));
         FxTestSupport.run(() -> {
            assertTrue(harness.stage.isShowing());
            final ProbeController controller = harness.controller;
            controller.createButton.fire();
            controller.createMenu.fire();
            press(harness.stage, KeyCode.F2);
            press(harness.stage, KeyCode.F6);
            assertEquals(4, harness.actionCalls.get());

            controller.allowCancel = false;
            controller.requestCancel();
            assertTrue(harness.stage.isShowing());
            press(harness.stage, KeyCode.F6);
            assertEquals(5, harness.actionCalls.get());

            controller.allowCancel = true;
            controller.requestCancel();
            assertFalse(harness.stage.isShowing());
            press(harness.stage, KeyCode.F6);
            assertEquals(5, harness.actionCalls.get());
            harness.stage.setTitle("Released window");
            assertEquals("Keyboard form", controller.getTitle());
         });
         assertTrue(harness.backgroundReleased.await(10, TimeUnit.SECONDS));
         FxTestSupport.run(() -> {});
         assertTrue(harness.guiOnFx);
         assertTrue(harness.backgroundOnVirtualThread);
         assertTrue(harness.guiBeforeBackground);
         assertEquals(1, harness.guiCalls.get());
         assertEquals(1, harness.backgroundCalls.get());
         assertEquals(1, harness.callbackCalls.get());
         assertEquals(1, harness.userHiddenCalls.get());
         assertTrue(errors.isEmpty(), errors.toString());
      }
      finally
      {
         finish(harness, previous);
      }
   }

   @Test
   void failedBindingDuringLaunchRemovesThePartialScopeAndStillClosesResources() throws Exception
   {
      final Harness harness = new Harness(true);
      final var errors = new CopyOnWriteArrayList<Throwable>();
      final CountDownLatch reported = new CountDownLatch(1);
      final Thread.UncaughtExceptionHandler previous = captureErrors(errors, reported);
      try
      {
         launch(harness);
         assertTrue(harness.backgroundReleased.await(10, TimeUnit.SECONDS));
         assertTrue(reported.await(10, TimeUnit.SECONDS));
         FxTestSupport.run(() -> {
            assertFalse(harness.stage.isShowing());
            press(harness.stage, KeyCode.F6);
            assertEquals(0, harness.actionCalls.get());
            harness.controller.create.setHotkeys(StandardAction.UPDATE.hotkeys());
         });
         assertEquals(0, harness.callbackCalls.get());
         assertEquals(1, harness.guiCalls.get());
         assertEquals(1, harness.backgroundCalls.get());
         assertTrue(harness.guiOnFx);
         assertTrue(harness.backgroundOnVirtualThread);
         assertTrue(harness.guiBeforeBackground);
         assertEquals(1, errors.size());
         assertInstanceOf(FormLaunchException.class, errors.getFirst());
         assertInstanceOf(IllegalArgumentException.class, errors.getFirst().getCause());
      }
      finally
      {
         finish(harness, previous);
      }
   }

   private static void launch(Harness harness)
   {
      NEXT.set(harness);
      new FormLauncher<String, ProbeController>(null, null, ProbeController.class)
              .fxml("ru/inversion/fore/form/keyboard-form.fxml")
              .bundle(new ListResourceBundle() {
                 @Override protected Object[][] getContents() { return new Object[0][0]; }
              })
              .callback(result -> harness.callbackCalls.incrementAndGet())
              .runForm();
   }

   private static Thread.UncaughtExceptionHandler captureErrors(
           CopyOnWriteArrayList<Throwable> errors, CountDownLatch reported) throws Exception
   {
      final AtomicReference<Thread.UncaughtExceptionHandler> previous = new AtomicReference<>();
      FxTestSupport.run(() -> {
         previous.set(Thread.currentThread().getUncaughtExceptionHandler());
         Thread.currentThread().setUncaughtExceptionHandler((thread, error) -> {
            errors.add(error);
            if( reported != null ) reported.countDown();
         });
      });
      return previous.get();
   }

   private static void finish(Harness harness, Thread.UncaughtExceptionHandler previous) throws Exception
   {
      FxTestSupport.run(() -> {
         if( harness.stage != null ) harness.stage.hide();
         Thread.currentThread().setUncaughtExceptionHandler(previous);
      });
      NEXT.compareAndSet(harness, null);
   }

   private static void press(Stage stage, KeyCode code)
   {
      Event.fireEvent(stage.getScene(), new KeyEvent(KeyEvent.KEY_PRESSED,
              KeyEvent.CHAR_UNDEFINED, "", code, false, false, false, false));
   }

   public static final class ProbeController extends FormController<String>
   {
      private final Harness harness;
      @FXML private ForeButton createButton;
      @FXML private ForeMenuItem createMenu;
      @FXML private ForeTextField input;
      private ForeAction create;
      private boolean allowCancel = true;

      public ProbeController()
      {
         harness = NEXT.getAndSet(null);
         if( harness == null ) throw new IllegalStateException("Missing launch harness");
         harness.controller = this;
      }

      @Override protected boolean preInit() { return !harness.rejectPreparation; }

      @Override protected void init()
      {
         create = createAction(StandardAction.CREATE, event -> harness.actionCalls.incrementAndGet());
         createButton.setAction(create);
         createMenu.setAction(create);
         validation().forControl(input, control -> ValidationResult.ok());
         if( harness.conflict ) createAction(StandardAction.CREATE, event -> {});
         setTitle("Keyboard form");
      }

      @Override protected void guiInit()
      {
         harness.stage = (Stage) getWindow();
         harness.stage.setOnHidden(event -> harness.userHiddenCalls.incrementAndGet());
         harness.guiReady.countDown();
      }

      @Override protected boolean onCancel() { return allowCancel; }
      private void requestCancel() { cancel(); }

      @Override protected void closeGuiResources()
      {
         harness.guiOnFx = Platform.isFxApplicationThread();
         harness.guiCalls.incrementAndGet();
      }

      @Override protected void closeResources()
      {
         harness.backgroundOnVirtualThread = Thread.currentThread().isVirtual();
         harness.guiBeforeBackground = harness.guiCalls.get() == 1;
         harness.backgroundCalls.incrementAndGet();
         harness.backgroundReleased.countDown();
      }
   }

   private static final class Harness
   {
      final boolean conflict;
      final boolean rejectPreparation;
      final CountDownLatch guiReady = new CountDownLatch(1);
      final CountDownLatch backgroundReleased = new CountDownLatch(1);
      final AtomicInteger actionCalls = new AtomicInteger();
      final AtomicInteger callbackCalls = new AtomicInteger();
      final AtomicInteger userHiddenCalls = new AtomicInteger();
      final AtomicInteger guiCalls = new AtomicInteger();
      final AtomicInteger backgroundCalls = new AtomicInteger();
      volatile ProbeController controller;
      volatile Stage stage;
      volatile boolean guiOnFx;
      volatile boolean backgroundOnVirtualThread;
      volatile boolean guiBeforeBackground;
      Harness(boolean conflict) { this(conflict, false); }
      Harness(boolean conflict, boolean rejectPreparation)
      {
         this.conflict = conflict;
         this.rejectPreparation = rejectPreparation;
      }
   }
}
