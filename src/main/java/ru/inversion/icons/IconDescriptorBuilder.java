package ru.inversion.icons;
import javafx.scene.paint.Color;
import ru.inversion.icons.enums.*;

import java.util.Objects;

/**
 Icon Data Builder - для одиночных иконок
 @author fomishkin on 12.07.2017.
 */
public class IconDescriptorBuilder<T extends Enum<T> & TTFChar> {

    private Object   iconId;
    private String   compatibleId;
    private IconSize iconSize;
    private Color    color;

    private static IconSize defaultIconSize  = IconSize.MEDIUM;
    private static Color    defaultIconColor = Color.BLACK;
    private static Object   defaultIconId    = FontAwesome.fa_question;

    public IconDescriptorBuilder() {
    }

    public IconDescriptorBuilder( T iconId ) {
        this.iconId = iconId;
    }

    public IconDescriptorBuilder( T iconId, String compatibleId ) {
        this.iconId = iconId;
        compatibleId( compatibleId );
    }

    public IconDescriptorBuilder compatibleId( String compatibleId ) {
        this.compatibleId = compatibleId;
        return this;
    }

    public IconDescriptorBuilder iconId( TTFChar id ) {
        iconId = id;
        return this;
    }

    //методы для удобства подхвата IDE
    public IconDescriptorBuilder iconId( FontAwesome id ) {
        return iconId((TTFChar) id);
    }

    public IconDescriptorBuilder iconId( MaterialDesign id ) {
        return iconId((TTFChar) id);
    }

    public IconDescriptorBuilder iconId( Entypo id ) {
        return iconId((TTFChar) id);
    }

    public IconDescriptorBuilder iconId( IonIcon id ) {
        return iconId((TTFChar) id);
    }

    public IconDescriptorBuilder iconSize( IconSize v ) {
        this.iconSize = v;
        return this;
    }

    public IconDescriptorBuilder color( Color v ) {
        this.color = v;
        return this;
    }

    public IconDescriptor build( ) {

        if( iconId == null )
            iconId = defaultIconId;

        if( color == null )
            color = defaultIconColor;

        if( iconSize == null )
            iconSize = defaultIconSize;

        return new IconDescriptor() {
            @Override
            public Object getIconID() {
                return iconId;
            }
            @Override
            public IconSize getIconSize() {
                return iconSize;
            }
            @Override
            public String getCompatibleID() {
                return compatibleId;
            }
            @Override
            public Color getIconColor() {
                return color;
            }
            @Override
            public boolean equals( final Object o ) {
                if ( this == o ) {
                    return true;
                }
                if ( o == null || getClass() != o.getClass() ) {
                    return false;
                }
                final IconDescriptor that = (IconDescriptor) o;
                return Objects.equals( getIconID(), that.getIconID() )
                        && Objects.equals( getIconSize(), that.getIconSize() )
                        && Objects.equals( getCompatibleID(), that.getCompatibleID() )
                        && Objects.equals( getIconColor(), that.getIconColor() );
            }
            @Override
            public int hashCode() {
                return Objects.hash( getIconID(), getIconSize(), getCompatibleID(), getIconColor() );
            }
        };
    }
    public static IconDescriptor of( FontAwesome fa ) {
        return of((TTFChar) fa);
    }

    /** */
    public static IconDescriptor of( TTFChar ttfChar ) {
        return new IconDescriptor() {
            @Override
            public Object getIconID() {
                return ttfChar;
            }
            @Override
            public IconSize getIconSize() {
                return defaultIconSize;
            }
            @Override
            public String getCompatibleID() {
                return null;
            }

            @Override
            public Color getIconColor() {
                return defaultIconColor;
            }

            @Override
            public boolean equals( final Object o ) {
                if ( this == o ) {
                    return true;
                }
                if ( o == null || getClass() != o.getClass() ) {
                    return false;
                }
                final IconDescriptor that = (IconDescriptor) o;
                return Objects.equals( getIconID(), that.getIconID() )
                        && Objects.equals( getIconSize(), that.getIconSize() )
                        && Objects.equals( getCompatibleID(), that.getCompatibleID() )
                        && Objects.equals( getIconColor(), that.getIconColor() );
            }

            @Override
            public int hashCode() {
                return Objects.hash( getIconID(), getIconSize(), getCompatibleID(), getIconColor() );
            }
        };
    }
}
