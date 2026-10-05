package ru.inversion.fore.form.action;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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

   @Test
   void prototypeOwnsTheBundleAndUsesTheStandardActionName()
   {
      final ResourceBundle bundle = bundle(new Object[][] {
              { "CREATE", "Добавить запись" },
              { "CREATE_TOOLTIP", "Создать новую запись" }
      });
      final var prototype = new ActionPrototype(
              StandardAction.CREATE, bundle, StandardAction.CREATE.icon(), List.of()
      );

      assertSame(bundle, prototype.bundle());
      assertEquals("Добавить запись", prototype.text());
      assertEquals("Создать новую запись", prototype.tooltip());
   }

   @Test
   void everyStandardActionUsesTheSuppliedBundle()
   {
      final ResourceBundle bundle = bundle(new Object[][] {
              { "CREATE", "Add" },
              { "UPDATE", "Edit" },
              { "DELETE", "Remove" },
              { "VIEW", "Open" },
              { "REFRESH", "Reload" }
      });

      for( StandardAction type : StandardAction.values() )
      {
         final var prototype = ForeActions.prototype(type, bundle);
         assertEquals(bundle.getString(type.name()), prototype.text(), type.name());
         assertEquals(prototype.text(), prototype.tooltip(), type.name());
      }
   }

   @Test
   void labelsAndTooltipsCanBeInheritedFromAParentBundle()
   {
      final ResourceBundle parent = bundle(new Object[][] {
              { "UPDATE", "Изменить запись" },
              { "UPDATE_TOOLTIP", "Редактировать выбранную запись" }
      });
      final ResourceBundle child = new ListResourceBundle()
      {
         {
            setParent(parent);
         }

         @Override
         protected Object[][] getContents()
         {
            return new Object[][] { { "CREATE", "Добавить" } };
         }
      };
      final var prototype = ForeActions.prototype(StandardAction.UPDATE, child);

      assertSame(child, prototype.bundle());
      assertEquals("Изменить запись", prototype.text());
      assertEquals("Редактировать выбранную запись", prototype.tooltip());
   }

   @Test
   void missingCaptionFailsWhenThePrototypeIsCreated()
   {
      final ResourceBundle bundle = bundle(new Object[][] { { "CREATE_TOOLTIP", "Add" } });

      final var error = assertThrows(MissingResourceException.class,
              () -> ForeActions.prototype(StandardAction.CREATE, bundle));

      assertEquals("CREATE", error.getKey());
   }

   @Test
   void actionsCopyCustomLabelsAndKeepIndependentState()
   {
      final ResourceBundle bundle = bundle(new Object[][] {
              { "CREATE", "Добавить запись" },
              { "CREATE_TOOLTIP", "Создать новую запись" }
      });
      final var first = ForeActions.create(StandardAction.CREATE, bundle, event -> {});
      final var second = ForeActions.create(StandardAction.CREATE, bundle, event -> {});

      assertEquals("Добавить запись", first.getText());
      assertEquals("Создать новую запись", first.getLongText());
      assertEquals(StandardAction.CREATE, first.standardType());
      assertNotSame(first.getGraphic(), second.getGraphic());

      first.setText("Другой текст");
      assertEquals("Добавить запись", second.getText());
      assertEquals("Добавить запись", bundle.getString("CREATE"));
   }

   private static ResourceBundle bundle(Object[][] contents)
   {
      return new ListResourceBundle()
      {
         @Override
         protected Object[][] getContents()
         {
            return contents;
         }
      };
   }
}
