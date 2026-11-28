package ru.inversion.fore.form.action;

import javafx.scene.Node;
import javafx.scene.text.Text;
import org.controlsfx.tools.Duplicatable;
import ru.inversion.icons.enums.TTFChar;

import java.util.Objects;

/** Неизменяемое описание глифа из существующего набора шрифтов приложения. */
public record IconSpec(String glyph, String fontFamily, int fontWeight) {
    public IconSpec {
        if (Objects.requireNonNull(glyph, "glyph").isEmpty()) {
            throw new IllegalArgumentException("glyph");
        }
        Objects.requireNonNull(fontFamily, "fontFamily");
    }

    static IconSpec of(TTFChar icon) {
        return new IconSpec(icon.getCode(), icon.getFontFamily(), icon.getFontWeight());
    }

    public Node newGraphic() {
        return new Glyph(this);
    }

    private static final class Glyph extends Text implements Duplicatable<Glyph> {
        private final IconSpec icon;

        Glyph(IconSpec icon) {
            super(icon.glyph());
            this.icon = icon;
            setStyle("-fx-font-family:'" + icon.fontFamily() + "'; -fx-font-size:16px;"
                    + (icon.fontWeight() > 0 ? " -fx-font-weight:" + icon.fontWeight() + ";" : ""));
            getStyleClass().add("fore-action-icon");
            setMouseTransparent(true);
        }

        @Override
        public Glyph duplicate() {
            return new Glyph(icon);
        }
    }
}
