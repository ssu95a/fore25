package ru.inversion.fx.form.controls.renderer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import javafx.scene.paint.Color;

/**
 Класс – стилизатор ячейки в таблице.
 Можно задать цвет фона, текста, а также доп. атрибуты для текста (жирный, курсив, подчёркивание)
 @author fomishkin on 01.03.2019. */
public class Colorizer {
    private Color backgroundColor;
    private Color backgroundSecondaryColor;
    private Color textColor;
    private Color textSecondaryColor;
    private LinkedHashSet<TextStyle> textStyles = new LinkedHashSet<>();

    /**

     @param backgroundColor Цвет фона выделенной строки. Цвет фона НЕ выделенной строки будет таким же, но чуть прозрачнее
     @param textColor Цвет текста НЕ выделенной строки. Цвет текста выделенной строки будет светлее и насыщеннее
     @param textStyles Стиль шрифта (жирный, курсив...)
     */
    public Colorizer( final Color backgroundColor, final Color textColor, TextStyle... textStyles ) {
        this(backgroundColor,
            textColor == null ? null : textColor.deriveColor( 1, 1.5, 2, 1 ),
            backgroundColor == null ?  null : backgroundColor.deriveColor( 1,1,1,0.80 ),
            textColor,
            textStyles);
    }

    /**
     @param backgroundPrimary Цвет фона выделенной строки
     @param textPrimary Цвет текста выделенной строки
     @param backgroundSecondary Цвет фона НЕ выделенной строки
     @param textSecondary Цвет текста НЕ выделенной строки
     @param textStyles Стиль шрифта (жирный, курсив...)
     */
    public Colorizer( final Color backgroundPrimary, final Color textPrimary,
                      final Color backgroundSecondary, final Color textSecondary, TextStyle... textStyles ) {

        this.backgroundColor = backgroundPrimary;
        this.backgroundSecondaryColor = backgroundSecondary;
        this.textColor = textPrimary;
        this.textSecondaryColor = textSecondary;
        this.textStyles.addAll( Arrays.asList( textStyles ) );
    }
    public Colorizer( final Color backgroundColor ) {
        this(backgroundColor, null);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getBackgroundSecondaryColor() {
        return backgroundSecondaryColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getTextSecondaryColor() {
        return textSecondaryColor;
    }

    public Set<TextStyle> getTextStyles() {
        return textStyles;
    }

    public enum TextStyle {
        BOLD("-fx-font-weight: bold;"),
        UNDERLINE("-fx-underline: true;"),
        ITALIC("-fx-font-style: italic; "),
        ;
        private String name;
        TextStyle( String name ) {
            this.name = name;
        }
        public String toString() {
            return name;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if ( getBackgroundColor() != null ){
            sb.append( String.format( " -colorizer-color: %s;",               toRGBACode( getBackgroundColor() )));
        }

        if ( getTextColor() != null ){
            sb.append( String.format( " -colorizer-text-fill: %s;",           toRGBACode( getTextColor() )));
        }

        if ( getBackgroundSecondaryColor() != null ){
            sb.append( String.format( " -colorizer-secondary-color: %s;",     toRGBACode( getBackgroundSecondaryColor() )));
        }

        if ( getTextSecondaryColor() != null ){
            sb.append( String.format( " -colorizer-secondary-text-fill: %s;", toRGBACode( getTextSecondaryColor() )));
        }

        if ( getTextStyles() != null ){
            getTextStyles().forEach( pc -> sb.append( " " ).append( pc.toString() ) );
        }

        return sb.toString();
    }

    private static String toRGBACode( Color color )
    {
        return String.format( "#%02X%02X%02X%02X",
                (int)( color.getRed() * 255 ),
                (int)( color.getGreen() * 255 ),
                (int)( color.getBlue() * 255 ),
                (int)( color.getOpacity() * 255 ) );
    }

}
