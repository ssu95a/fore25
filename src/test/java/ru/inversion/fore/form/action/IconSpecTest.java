package ru.inversion.fore.form.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IconSpecTest
{
   @Test
   void allSupportedFontResourcesArePresent()
   {
      for( IconFont font : IconFont.values() )
         assertNotNull(IconFont.class.getResource(font.resourcePath()), font.name());
   }

   @Test
   void fontSelectionIsExplicitAndIndependent()
   {
      final String glyph = "\uf016";
      final var oldDefault = new IconSpec(glyph);
      final var explicit = new IconSpec(IconFont.FONT_AWESOME_4, glyph);
      final var solid = new IconSpec(IconFont.FONT_AWESOME_SOLID, glyph);

      assertEquals(explicit, oldDefault);
      assertEquals(IconFont.FONT_AWESOME_4, oldDefault.font());
      assertNotEquals(explicit, solid);
      assertEquals(IconFont.FONT_AWESOME_SOLID, solid.font());
      assertEquals(glyph, solid.glyph());

      assertThrows(NullPointerException.class, () -> new IconSpec(null, glyph));
      assertThrows(NullPointerException.class,
              () -> new IconSpec(IconFont.FONT_AWESOME_4, null));
      assertThrows(IllegalArgumentException.class,
              () -> new IconSpec(IconFont.FONT_AWESOME_4, ""));
   }

   @Test
   void standardActionsKeepExistingJInvForeVectorMappings()
   {
      assertEquals(new IconSpec(IconFont.FONT_AWESOME_4, "\uf016"),
              ForeActions.prototype(StandardAction.CREATE).icon());
      assertEquals(new IconSpec(IconFont.FONT_AWESOME_4, "\uf044"),
              ForeActions.prototype(StandardAction.UPDATE).icon());
      assertEquals(new IconSpec(IconFont.FONT_AWESOME_4, "\uf00d"),
              ForeActions.prototype(StandardAction.DELETE).icon());
      assertEquals(new IconSpec(IconFont.FONT_AWESOME_4, "\uf05a"),
              ForeActions.prototype(StandardAction.VIEW).icon());
      assertEquals(new IconSpec(IconFont.FONT_AWESOME_4, "\uf021"),
              ForeActions.prototype(StandardAction.REFRESH).icon());
   }
}
