package ru.inversion.icons.providers.fx;
import ru.inversion.icons.enums.TTFChar;
import ru.inversion.icons.nogui.NoGuiIcon;
import javafx.scene.control.Label;
import ru.inversion.icons.providers.TTFProvider;
import ru.inversion.utils.S;

/**
 @author fomishkin on 28.06.2017. */
public class FXTTFProvider extends TTFProvider {
    @Override
    public Object getImage( NoGuiIcon icon ) {
        Label label = new Label();
        TTFChar id = (TTFChar) icon.getID();
        label.setText( id.getCode() );
        String fontWeightString =
                id.getFontWeight() > 0 ? String.format(" -fx-font-weight:%s;", id.getFontWeight()) : S.EMPTY_STRING;
        label.setStyle(String.format("-fx-font-family:'%s';%s", id.getFontFamily(), fontWeightString));
        return label;
    }
}
