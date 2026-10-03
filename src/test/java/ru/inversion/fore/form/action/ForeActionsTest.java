package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForeActionsTest
{
   @Test
   void standardPrototypeHasStableDefaultsButNoSharedMutableAction()
   {
      final var a = ForeActions.prototype(StandardAction.CREATE);
      final var b = ForeActions.prototype(StandardAction.CREATE);

      assertNotSame(a, b);
      assertEquals(a, b);
      assertEquals("Создать…", a.text());
      assertEquals("Создать", a.tooltip());
      assertEquals(2, a.hotkeys().size());
      assertThrows(UnsupportedOperationException.class, () -> a.hotkeys().clear());
   }

   @Test
   void standardShortcutsRetainLegacyCreateAndUpdateDefaults()
   {
      final var create = ForeActions.prototype(StandardAction.CREATE);
      final var update = ForeActions.prototype(StandardAction.UPDATE);

      assertEquals(new KeyCodeCombination(KeyCode.F2), create.hotkeys().get(0));
      assertEquals(new KeyCodeCombination(KeyCode.F6), create.hotkeys().get(1));
      assertEquals(new KeyCodeCombination(KeyCode.F4), update.hotkeys().get(0));
   }
}
