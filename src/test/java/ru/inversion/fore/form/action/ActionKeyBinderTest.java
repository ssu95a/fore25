package ru.inversion.fore.form.action;

import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.inversion.fore.FxTestSupport;
import ru.inversion.fore.form.control.ForeButton;
import ru.inversion.fore.form.control.ForeMenuItem;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ActionKeyBinderTest
{
   @BeforeAll static void startToolkit() throws Exception { FxTestSupport.start(); }

   @Test
   void buttonMenuAndBothCreateKeysInvokeTheSameActionOnce() throws Exception
   {
      FxTestSupport.run(() -> {
         final Scene scene = scene();
         final AtomicInteger calls = new AtomicInteger();
         final ForeAction create = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         final ForeButton button = new ForeButton(create);
         final ForeMenuItem menu = new ForeMenuItem(create);
         final Runnable nativeAccelerator = menu::fire;
         scene.getAccelerators().put(menu.getAccelerator(), nativeAccelerator);
         try( var binder = new ActionKeyBinder(scene) )
         {
            binder.bind(create);
            binder.bind(create);
            button.fire();
            menu.fire();
            press(scene, KeyCode.F2, false);
            press(scene, KeyCode.F6, false);
            assertEquals(4, calls.get());
            assertSame(nativeAccelerator, scene.getAccelerators().get(menu.getAccelerator()));
         }
      });
   }

   @Test
   void shiftF6UsesDeleteAndDisabledKeysAreConsumed() throws Exception
   {
      FxTestSupport.run(() -> {
         final Scene scene = scene();
         final AtomicInteger creates = new AtomicInteger();
         final AtomicInteger deletes = new AtomicInteger();
         final AtomicInteger nativeCalls = new AtomicInteger();
         final ForeAction create = ForeActions.create(StandardAction.CREATE, event -> creates.incrementAndGet());
         final ForeAction delete = ForeActions.create(StandardAction.DELETE, event -> deletes.incrementAndGet());
         scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F6), nativeCalls::incrementAndGet);
         try( var binder = new ActionKeyBinder(scene) )
         {
            binder.bind(create);
            binder.bind(delete);
            press(scene, KeyCode.F6, false);
            press(scene, KeyCode.F6, true);
            assertEquals(1, creates.get());
            assertEquals(1, deletes.get());
            create.setDisabled(true);
            press(scene, KeyCode.F6, false);
            assertEquals(1, creates.get());
            assertEquals(0, nativeCalls.get());
         }
      });
   }

   @Test
   void keyChangesReplaceAllAlternativesAndCanClearTheBinding() throws Exception
   {
      FxTestSupport.run(() -> {
         final Scene scene = scene();
         final AtomicInteger calls = new AtomicInteger();
         final ForeAction action = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         try( var binder = new ActionKeyBinder(scene) )
         {
            binder.bind(action);
            action.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9)));
            press(scene, KeyCode.F2, false);
            press(scene, KeyCode.F6, false);
            press(scene, KeyCode.F9, false);
            assertEquals(1, calls.get());
            action.setHotkeys(List.of());
            assertNull(action.getAccelerator());
            press(scene, KeyCode.F9, false);
            assertEquals(1, calls.get());
         }
      });
   }

   @Test
   void registrationAndKeyChangeConflictsLeaveExistingBindingsIntact() throws Exception
   {
      FxTestSupport.run(() -> {
         final Scene scene = scene();
         final AtomicInteger calls = new AtomicInteger();
         final ForeAction create = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         final ForeAction update = ForeActions.create(StandardAction.UPDATE, event -> calls.addAndGet(10));
         final ForeAction duplicate = ForeActions.create(StandardAction.CREATE, event -> fail("Rejected action fired"));
         try( var binder = new ActionKeyBinder(scene) )
         {
            binder.bind(create);
            binder.bind(update);
            assertThrows(IllegalArgumentException.class, () -> binder.bind(duplicate));
            assertThrows(IllegalArgumentException.class, () -> create.setHotkeys(update.hotkeys()));
            assertEquals(StandardAction.CREATE.hotkeys(), create.hotkeys());
            assertEquals(new KeyCodeCombination(KeyCode.F2), create.getAccelerator());
            press(scene, KeyCode.F6, false);
            press(scene, KeyCode.F4, false);
            assertEquals(11, calls.get());
         }
      });
   }

   @Test
   void ignoredModifiersAndPlatformShortcutAliasesAreConflicts() throws Exception
   {
      FxTestSupport.run(() -> {
         final ForeAction create = ForeActions.create(StandardAction.CREATE, event -> {});
         final ForeAction other = ForeActions.create(StandardAction.UPDATE, event -> {});
         other.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_ANY)));
         try( var binder = new ActionKeyBinder(scene()) )
         {
            binder.bind(create);
            assertThrows(IllegalArgumentException.class, () -> binder.bind(other));
            create.setHotkeys(List.of(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN)));
            final KeyEvent control = event(KeyCode.S, false, true, false, false);
            final KeyCombination physical = new KeyCodeCombination(KeyCode.S,
                    create.hotkeys().getFirst().match(control) ? KeyCombination.CONTROL_DOWN : KeyCombination.META_DOWN);
            other.setHotkeys(List.of(physical));
            assertThrows(IllegalArgumentException.class, () -> binder.bind(other));
         }
      });
   }

   @Test
   void sharedActionChangesAreValidatedAgainstEveryScene() throws Exception
   {
      FxTestSupport.run(() -> {
         final Scene first = scene();
         final Scene second = scene();
         final AtomicInteger calls = new AtomicInteger();
         final ForeAction create = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         final ForeAction refresh = ForeActions.create(StandardAction.REFRESH, event -> {});
         try( var a = new ActionKeyBinder(first); var b = new ActionKeyBinder(second) )
         {
            a.bind(create);
            b.bind(create);
            b.bind(refresh);
            assertThrows(IllegalArgumentException.class, () -> create.setHotkeys(refresh.hotkeys()));
            press(first, KeyCode.F6, false);
            press(second, KeyCode.F6, false);
            assertEquals(2, calls.get());
            b.unbind(refresh);
            create.setHotkeys(refresh.hotkeys());
            press(first, KeyCode.F8, false);
            press(second, KeyCode.F8, false);
            assertEquals(4, calls.get());
         }
      });
   }

   @Test
   void closeRemovesOnlyOwnedBindingsAndAllowsReopeningAScope() throws Exception
   {
      FxTestSupport.run(() -> {
         final Scene scene = scene();
         final AtomicInteger calls = new AtomicInteger();
         final AtomicInteger unrelatedCalls = new AtomicInteger();
         final ForeAction create = ForeActions.create(StandardAction.CREATE, event -> calls.incrementAndGet());
         final ForeAction update = ForeActions.create(StandardAction.UPDATE, event -> {});
         final Runnable unrelated = unrelatedCalls::incrementAndGet;
         scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F10), unrelated);
         final var binder = new ActionKeyBinder(scene);
         binder.bind(create);
         binder.bind(update);
         assertThrows(IllegalStateException.class, () -> new ActionKeyBinder(scene));
         binder.close();
         binder.close();
         create.setHotkeys(update.hotkeys());
         press(scene, KeyCode.F4, false);
         assertEquals(0, calls.get());
         assertSame(unrelated, scene.getAccelerators().get(new KeyCodeCombination(KeyCode.F10)));
         press(scene, KeyCode.F10, false);
         assertEquals(1, unrelatedCalls.get());
         try( var reopened = new ActionKeyBinder(scene) )
         {
            reopened.bind(create);
            press(scene, KeyCode.F4, false);
            assertEquals(1, calls.get());
         }
      });
   }

   @Test
   void boundAcceleratorFailureDoesNotChangeTheKeys() throws Exception
   {
      FxTestSupport.run(() -> {
         final ForeAction action = ForeActions.create(StandardAction.CREATE, event -> {});
         final var accelerator = new SimpleObjectProperty<KeyCombination>(action.getAccelerator());
         action.acceleratorProperty().bind(accelerator);
         try
         {
            assertThrows(RuntimeException.class,
                    () -> action.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9))));
            assertEquals(StandardAction.CREATE.hotkeys(), action.hotkeys());
            assertEquals(new KeyCodeCombination(KeyCode.F2), action.getAccelerator());
         }
         finally
         {
            action.acceleratorProperty().unbind();
         }
      });
   }

   @Test
   void boundActionUpdatesRequireTheFxThread() throws Exception
   {
      final AtomicReference<ForeAction> action = new AtomicReference<>();
      final AtomicReference<ActionKeyBinder> binder = new AtomicReference<>();
      FxTestSupport.run(() -> {
         action.set(ForeActions.create(StandardAction.CREATE, event -> {}));
         binder.set(new ActionKeyBinder(scene()));
         binder.get().bind(action.get());
      });
      try
      {
         assertThrows(IllegalStateException.class,
                 () -> action.get().setHotkeys(List.of(new KeyCodeCombination(KeyCode.F9))));
         assertThrows(IllegalStateException.class, () -> binder.get().close());
      }
      finally
      {
         FxTestSupport.run(() -> binder.get().close());
      }
   }

   private static Scene scene() { return new Scene(new Pane()); }

   private static KeyEvent event(KeyCode code, boolean shift, boolean control, boolean alt, boolean meta)
   {
      return new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, "", code, shift, control, alt, meta);
   }

   private static void press(Scene scene, KeyCode code, boolean shift)
   {
      Event.fireEvent(scene, event(code, shift, false, false, false));
   }
}
