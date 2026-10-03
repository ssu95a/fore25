package ru.inversion.fore.form.action;

import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.controlsfx.tools.Duplicatable;
import ru.inversion.utils.Checks;

/**
 * An icon descriptor, not a JavaFX Node prototype.
 * V0.1 uses the existing bundled FontAwesome 4 font.
 */
public record IconSpec(String glyph)
{
   public IconSpec
   {
      Checks.Require.text(glyph, "glyph");
   }

   Node newGraphic()
   {
      return new Glyph(this);
   }

   /** ActionUtils clones the graphic via Duplicatable for each Button/MenuItem. */
   private static final class Glyph extends Text implements Duplicatable<Glyph>
   {
      private final IconSpec spec;

      private Glyph(IconSpec spec)
      {
         super(spec.glyph());
         this.spec = spec;
         setFont(IconFont.FONT);
         getStyleClass().add("fore-action-icon");
         setMouseTransparent(true);
      }

      @Override
      public Glyph duplicate()
      {
         return new Glyph(spec);
      }
   }

   private static final class IconFont
   {
      private static final Font FONT = load();

      private static Font load()
      {
         final var url = IconSpec.class.getResource("/style/fontawesome-webfont.ttf");
         if( url == null )
            throw new IllegalStateException("Bundled FontAwesome font not found");

         final Font font = Font.loadFont(url.toExternalForm(), 16);
         if( font == null )
            throw new IllegalStateException("Unable to load bundled FontAwesome font");

         return font;
      }
   }
}
