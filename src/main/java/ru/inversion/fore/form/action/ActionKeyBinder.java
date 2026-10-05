package ru.inversion.fore.form.action;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * One keyboard scope per Scene. All operations, including bound-action key changes,
 * run on the FX thread. Matching keys are consumed before native menu accelerators.
 * Unrelated Scene accelerators are preserved. Close removes only this binder's filter.
 */
public final class ActionKeyBinder implements AutoCloseable
{
   private static final Object SCENE_KEY = new Object();

   private final Scene scene;
   private final Map<ForeAction, Consumer<List<KeyCombination>>> bindings = new LinkedHashMap<>();
   private final EventHandler<KeyEvent> keyFilter = this::handleKeyPressed;
   private boolean closed;

   public ActionKeyBinder(Scene scene)
   {
      requireFxThread();
      this.scene = Objects.requireNonNull(scene, "scene");
      if( scene.getProperties().containsKey(SCENE_KEY) )
         throw new IllegalStateException("Scene already has an action key binder");

      scene.getProperties().put(SCENE_KEY, this);
      scene.addEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
   }

   /** Register an action once, even when several controls share it. */
   public void bind(ForeAction action)
   {
      requireOpen();
      Objects.requireNonNull(action, "action");
      if( bindings.containsKey(action) )
         return;

      validateHotkeys(action, action.hotkeys());
      final Consumer<List<KeyCombination>> validator = keys -> validateHotkeys(action, keys);
      bindings.put(action, validator);
      action.addHotkeyValidator(validator);
   }

   public void unbind(ForeAction action)
   {
      requireOpen();
      final var validator = bindings.remove(Objects.requireNonNull(action, "action"));
      if( validator != null )
         action.removeHotkeyValidator(validator);
   }

   private void validateHotkeys(ForeAction action, List<KeyCombination> keys)
   {
      requireOpen();
      for( ForeAction other : bindings.keySet() )
      {
         if( other == action )
            continue;
         for( KeyCombination key : keys )
            for( KeyCombination occupied : other.hotkeys() )
               if( overlap(key, occupied) )
                  throw new IllegalArgumentException("Hotkey " + key.getName() + " for "
                          + action.standardType() + " conflicts with " + other.standardType()
                          + " (" + occupied.getName() + ")");
      }
   }

   /** Compare actual JavaFX matching, including ANY modifiers and platform Shortcut aliases. */
   private static boolean overlap(KeyCombination first, KeyCombination second)
   {
      if( first == KeyCombination.NO_MATCH || second == KeyCombination.NO_MATCH )
         return false;

      final KeyCode[] codes;
      if( first instanceof KeyCodeCombination code )
         codes = new KeyCode[]{ code.getCode() };
      else if( second instanceof KeyCodeCombination code )
         codes = new KeyCode[]{ code.getCode() };
      else
         codes = KeyCode.values();

      for( KeyCode code : codes )
         for( int modifiers = 0; modifiers < 16; modifiers++ )
         {
            final KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, KeyEvent.CHAR_UNDEFINED, "", code,
                    (modifiers & 1) != 0, (modifiers & 2) != 0,
                    (modifiers & 4) != 0, (modifiers & 8) != 0);
            if( first.match(event) && second.match(event) )
               return true;
         }
      return false;
   }

   private void handleKeyPressed(KeyEvent event)
   {
      if( closed || event.isConsumed() )
         return;
      for( ForeAction action : bindings.keySet() )
         for( KeyCombination key : action.hotkeys() )
            if( key.match(event) )
            {
               // Consume even disabled actions: a native accelerator must not provide a fallback.
               event.consume();
               action.handle(new ActionEvent(action, event.getTarget()));
               return;
            }
   }

   @Override
   public void close()
   {
      requireFxThread();
      if( closed )
         return;
      closed = true;
      scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyFilter);
      bindings.forEach(ForeAction::removeHotkeyValidator);
      bindings.clear();
      scene.getProperties().remove(SCENE_KEY, this);
   }

   private void requireOpen()
   {
      requireFxThread();
      if( closed )
         throw new IllegalStateException("Action key binder is closed");
   }

   private static void requireFxThread()
   {
      if( !Platform.isFxApplicationThread() )
         throw new IllegalStateException("Action key binding requires the FX Application Thread");
   }
}
