package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ForeActionsTest
{
   @Test
   void standardHotkeysCannotBeModified()
   {
      for( StandardAction type : StandardAction.values() )
         assertThrows(UnsupportedOperationException.class, () -> type.hotkeys().clear());
   }

   @Test
   void standardShortcutsRetainLegacyDefaults()
   {
      assertEquals(List.of(new KeyCodeCombination(KeyCode.F2), new KeyCodeCombination(KeyCode.F6)),
              StandardAction.CREATE.hotkeys());
      assertEquals(List.of(new KeyCodeCombination(KeyCode.F4)), StandardAction.UPDATE.hotkeys());
      assertEquals(List.of(new KeyCodeCombination(KeyCode.F6, KeyCombination.SHIFT_DOWN)),
              StandardAction.DELETE.hotkeys());
      assertEquals(List.of(new KeyCodeCombination(KeyCode.F3)), StandardAction.VIEW.hotkeys());
      assertEquals(List.of(new KeyCodeCombination(KeyCode.F8)), StandardAction.REFRESH.hotkeys());
   }

   @ParameterizedTest
   @CsvSource({
           "CREATE, Создать…, Создать",
           "UPDATE, Изменить…, Изменить запись",
           "DELETE, Удалить…, Удалить",
           "VIEW, Просмотр…, Просмотр",
           "REFRESH, Обновить, Обновить (F8)"
   })
   void newActionsUseTheStandardDefaults(StandardAction type, String text, String tooltip)
   {
      final var action = ForeActions.create(type, event -> {});

      assertEquals(text, type.text());
      assertEquals(tooltip, type.tooltip());
      assertEquals(text, action.getText());
      assertEquals(tooltip, action.getLongText());
      assertEquals(type, action.standardType());
      assertEquals(type.icon(), action.icon());
      assertEquals(type.hotkeys(), action.hotkeys());
      assertEquals(type.hotkeys().get(0), action.getAccelerator());
   }

   @Test
   void changingOneActionDoesNotChangeTheCatalogOrOtherActions()
   {
      final var first = ForeActions.create(StandardAction.CREATE, event -> {});
      final var second = ForeActions.create(StandardAction.CREATE, event -> {});

      assertNotSame(first, second);
      assertEquals("Создать…", first.getText());
      assertEquals("Создать", first.getLongText());
      assertEquals(StandardAction.CREATE, first.standardType());
      assertNotSame(first.getGraphic(), second.getGraphic());

      first.setText("Добавить запись");
      first.setLongText("Создать новую запись");
      first.setIcon(StandardAction.UPDATE.icon());
      first.setDisabled(true);
      final var customHotkeys = new ArrayList<KeyCombination>(List.of(new KeyCodeCombination(KeyCode.F9)));
      first.setHotkeys(customHotkeys);
      customHotkeys.clear();

      assertEquals(List.of(new KeyCodeCombination(KeyCode.F9)), first.hotkeys());
      assertEquals(new KeyCodeCombination(KeyCode.F9), first.getAccelerator());
      assertEquals("Создать…", second.getText());
      assertEquals("Создать", second.getLongText());
      assertEquals(StandardAction.CREATE.icon(), second.icon());
      assertEquals(StandardAction.CREATE.hotkeys(), second.hotkeys());
      assertEquals(new KeyCodeCombination(KeyCode.F2), second.getAccelerator());
      assertFalse(second.isDisabled());
      assertEquals("Создать…", StandardAction.CREATE.text());
      assertEquals("Создать", StandardAction.CREATE.tooltip());
   }
}
