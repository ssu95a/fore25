package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

   @ParameterizedTest
   @CsvSource({
           "CREATE, Создать…, Создать",
           "UPDATE, Изменить…, Изменить запись",
           "DELETE, Удалить…, Удалить",
           "VIEW, Просмотр…, Просмотр",
           "REFRESH, Обновить, Обновить (F8)"
   })
   void prototypesUseTheStandardCatalog(StandardAction type, String text, String tooltip)
   {
      final var prototype = ForeActions.prototype(type);

      assertEquals(text, type.text());
      assertEquals(tooltip, type.tooltip());
      assertEquals(text, prototype.text());
      assertEquals(tooltip, prototype.tooltip());
   }

   @Test
   void changingOneActionDoesNotChangeTheCatalogOrOtherActions()
   {
      final var first = ForeActions.create(StandardAction.CREATE, event -> {});
      final var second = ForeActions.create(StandardAction.CREATE, event -> {});

      assertEquals("Создать…", first.getText());
      assertEquals("Создать", first.getLongText());
      assertEquals(StandardAction.CREATE, first.standardType());
      assertNotSame(first.getGraphic(), second.getGraphic());

      first.setText("Добавить запись");
      first.setLongText("Создать новую запись");

      assertEquals("Создать…", second.getText());
      assertEquals("Создать", second.getLongText());
      assertEquals("Создать…", StandardAction.CREATE.text());
      assertEquals("Создать", StandardAction.CREATE.tooltip());
   }
}
