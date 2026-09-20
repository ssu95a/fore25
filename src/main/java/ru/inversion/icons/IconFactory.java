package ru.inversion.icons;
import static java.lang.invoke.MethodHandles.lookup;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import ru.inversion.icons.descextensions.IColorable;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.icons.enums.TTFChar;
import ru.inversion.icons.nogui.NoGuiIcon;
import ru.inversion.icons.nogui.TTFCodeIcon;
import ru.inversion.icons.nogui.TTFIcon;
import ru.inversion.icons.nogui.URIFacade;

/**
 @author fomishkin on 28.06.2017. */
public class IconFactory extends URIFacade {
    private static final Logger logger = getLogger( lookup().lookupClass() );

    private static Label getLabel( URI uri, IconSize size, Color color ) throws URIFacade.IconNotFoundException {
        return getLabelInternal( getImage( uri, size, color ) );
    }
    private static Label getLabel( URL url, IconSize size, Color color ) throws URIFacade.IconNotFoundException {
        try {
            return getLabelInternal( getImage( new URI( "url:" + url ), size, color ) );
        } catch ( URISyntaxException e ) {
            throw new IllegalStateException( e );
        }
    }

    private static Label getLabel( String string, IconSize size, Color color ) throws URIFacade.IconNotFoundException {
        try {
            return getLabel( new URI( string ), size, color );
        } catch ( URISyntaxException e ) {
            throw new URIFacade.IconNotFoundException( "URI exception: " + e.getMessage() );
        }
    }

    private static <E extends Enum<E> & TTFChar> Label getLabel( E e, IconSize size, Color color ) throws URIFacade.IconNotFoundException {
        return getLabelInternal( newTTF( e, size, color ) );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, IconSize size, Color color ) {
        return getLabel( o, size, color, true );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o ) {
        return getLabel( o, IconSize.MEDIUM, Color.BLACK, true );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, IconSize size ) {
        return getLabel( o, size, Color.BLACK, true );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, Color color ) {
        return getLabel( o, IconSize.MEDIUM, color, true );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, boolean enableFailIcon ) {
        return getLabel( o, IconSize.MEDIUM, Color.BLACK, enableFailIcon );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, IconSize size, boolean enableFailIcon ) {
        return getLabel( o, size, Color.BLACK, enableFailIcon );
    }

    /**
     @see #getLabel(Object, IconSize, Color, boolean)
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, Color color, boolean enableFailIcon ) {
        return getLabel( o, IconSize.MEDIUM, color, enableFailIcon );
    }

    /**
     Получение лейбла с нужной иконкой
     @param o (Object) String, Uri или Enum
     Enum:
     Enum implements TTFChar
     @param size Размер иконки, работает для векторных изображений, по умолчанию IconSize.MEDIUM
     @param color Цвет иконки, работает только для одиночных векторных изображений, по умолчанию Color.BLACK
     @return JavaFX Label
     @see TTFChar

     String/URI:
     Типы (префиксы):
     fmx - Иконка из файловой системы (использует Path, указанный в констукторе).
     Поддерживаемые расширения: ICO, ???
     Пример: "fmx:ap_export.ico"
     <p>
     dep - Иконка из ресурсов данной библиотеки.
     Пример: "dep:fa_edit.png"
     <p>
     fa/mdi/ent/ion - TTF-иконка.
     Если нужна одна иконка, проще использовать Enum
     Пример 1: "fa:" + FontAwesome.fa_printer
     Пример 2: "fa:fa_printer"
     mul - Мульти-TTF-иконка, наложение нескольких иконок (в порядке снизу-вверх, с указанием цвета).
     Синтаксис:
     Префикс              TTF-иконка     Цвет
     [mul]        :  [ [fa:fa_check] : [red] ]...
     Фиксированная часть    \  Повторяющаяся часть /
     В повторяющейся части все 3 параметра обязательны.
     Пример 1: "mul:fa:fa_printer:black:fa:fa_check:red:mdi:mdi_printer:blue"

     Для экзотических цветов можно использовать web-цвета RGB и RGBA(с прозрачностью)
     Пример 2: "mul:fa:fa_printer:343434bb:fa:fa_check:aabbff"
     Пример 3: "mul:fa:" + FontAwesome.fa_printer + ":black:fa:" + FontAwesome.fa_check + ":aabbff"

     row - Несколько TTF-иконок в ряд слева-направо
     Синтаксис:
     Префикс                   TTF-иконка
     [mul]        :  [     [fa:fa_check]     ]...
     Фиксированная часть    \  Повторяющаяся часть /
     В повторяющейся части все 2 параметра обязательны.
     Пример: "row:ion:" + IonIcon.ion_wifi + ":fa:"+ FontAwesome.fa_angle_double_down
     */
    public static <E extends Enum<E> & TTFChar> Label getLabel( Object o, IconSize size, Color color, boolean enableFailIcon ) {
        try {
            if ( o == null ) {
                return getImageNotFound( enableFailIcon );
                /*throw new IconNotFoundException( "getLabel: Object is null" );*/
            }
            if ( o instanceof IconDescriptor ) {
                return getLabel( ( (IconDescriptor) o ).getIconID(), ( (IBaseIconDescriptor) o ).getIconSize(), ( (IColorable) o )
                        .getIconColor() );
            }
            if ( o instanceof IOverlayIconDescriptor ) {
                return getLabel( ( (ICombinedIconDescriptor) o ).toUri(), ( (IBaseIconDescriptor) o ).getIconSize(), color );
            }
            if ( o instanceof IRowIconDescriptor ) {
                Label label = getLabel( ( (ICombinedIconDescriptor) o ).toUri(), ( (IBaseIconDescriptor) o ).getIconSize(), ( (IColorable) o ).getIconColor() );
                int padding = ( (IRowIconDescriptor) o ).getPadding();
                if ( padding != 0 && label.getGraphic() != null && label.getGraphic() instanceof Parent ){
                    Parent graphic = (Parent) label.getGraphic();
                    ObservableList<Node> icons = graphic.getChildrenUnmodifiable();
                    for ( int i = 0; i < icons.size(); i++ ) {
                        Node node = icons.get( i );
                        if ( node instanceof Label ){
                            final Label icon = (Label) node;
                            if ( i == 0 ){
                                icon.setPadding( new Insets( 0, padding, 0, 0 ) );
                            } else if ( i == icons.size() - 1 ){
                                icon.setPadding( new Insets( 0, 0, 0, padding ) );
                            } else {
                                icon.setPadding( new Insets( 0, padding, 0, padding ) );
                            }
                        }
                    }
                }
                return label;
            }
            if ( o instanceof URI ) {
                return getLabel( (URI) o, size, color );
            }
            if ( o instanceof URL ) {
                return getLabel( (URL) o, size, color );
            }
            if ( o instanceof String ) {
                return getLabel( (String) o, size, color );
            }
            if ( o instanceof Enum && o instanceof TTFChar ) {
                return getLabel( (E) o, size, color );
            }
            if ( o instanceof TTFChar ) {
                final TTFCodeIcon e = new TTFCodeIcon((TTFChar)o, size, color);
                return getLabelInternal( e );
                //return getLabel( (E) o, size, color );
            }
            throw new IconNotFoundException( "Couldn't determine Object type for getLabel: " +
                    o.toString() );
        } catch ( URIFacade.IconNotFoundException e ) {
            logger.trace( "IconNotFoundException ({})", e.getMessage() );
        }
        return getImageNotFound( enableFailIcon );
    }

    /**
     Окрашивание/Изменение размера из параметров происходят здесь
     */
    private static Label getLabelInternal( NoGuiIcon icon ) throws URIFacade.IconNotFoundException {
        Label label = (Label) icon.getProvider().getImage( icon );
        label = icon.getColorBehavior().changeColor( label );
        label = icon.getSizeBehavior().changeSize( label );
        return label;
    }

    private static Label getImageNotFound( boolean enableFailIcon ) {
        if ( enableFailIcon ) {
            try {
                return getLabelInternal( new TTFIcon<>( FontAwesome.fa_question ) );
            } catch ( URIFacade.IconNotFoundException e ) {
                throw new RuntimeException( "Unable to find default '?' icon" );
            }
        } else {
            return null;
        }
    }
}
