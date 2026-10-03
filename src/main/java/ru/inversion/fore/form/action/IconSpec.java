package ru.inversion.fore.form.action;

import javafx.scene.Node;
import javafx.scene.text.Text;
import org.controlsfx.tools.Duplicatable;

import java.util.Objects;

/**
 * Immutable vector icon descriptor. It never stores a shared JavaFX Node.
 */
public record IconSpec(IconFont font, String glyph)
{
   public IconSpec
   {
      Objects.requireNonNull(font, "font");
      Objects.requireNonNull(glyph, "glyph");
      if( glyph.isEmpty() )
         throw new IllegalArgumentException("glyph must not be empty");
   }

   /** Compatibility shortcut for existing Fore25 callers, not Oracle Forms. */
   public IconSpec(String glyph)
   {
      this(IconFont.FONT_AWESOME_4, glyph);
   }

   public Node newGraphic()
   {
      return new Glyph(this);
   }

   /** ControlsFX ActionUtils duplicates each graphic before placing it into UI. */
   private static final class Glyph extends Text implements Duplicatable<Glyph>
   {
      private final IconSpec spec;

      private Glyph(IconSpec spec)
      {
         super(spec.glyph());
         this.spec = spec;
         setFont(spec.font().font());
         getStyleClass().add("fore-action-icon");
         setMouseTransparent(true);
      }

      @Override
      public Glyph duplicate()
      {
         return new Glyph(spec);
      }
   }
}
