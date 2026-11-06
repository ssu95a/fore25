package ru.inversion.icons;
import ru.inversion.icons.enums.*;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

/**
 Multi Data Builder - для наложенных иконок
 @author fomishkin on 12.07.2017. */
public class OverlayIconDescriptorBuilder {
    private List<Object> iconIdList = new ArrayList<>();
    private List<Color> colorList = new ArrayList<>();
    private IconSize iconSize;
    private String compatibleId;

    public OverlayIconDescriptorBuilder() {
    }

    public OverlayIconDescriptorBuilder add( TTFChar id ) {
        this.iconIdList.add( id );
        this.colorList.add( null );
        return this;
    }

    public OverlayIconDescriptorBuilder add( TTFChar id, Color c ) {
        this.iconIdList.add( id );
        this.colorList.add( c );
        return this;
    }

    /* методы для удобства подхвата IDE */

    public OverlayIconDescriptorBuilder add( FontAwesome id ) {
        return add((TTFChar) id);
    }

    public OverlayIconDescriptorBuilder add( FontAwesome id, Color c ) {
        return add((TTFChar)id, c);
    }

    public OverlayIconDescriptorBuilder add( MaterialDesign id ) {
        return add((TTFChar) id);
    }

    public OverlayIconDescriptorBuilder add( MaterialDesign id, Color c ) {
        return add((TTFChar)id, c);
    }

    public OverlayIconDescriptorBuilder add( IonIcon id ) {
        return add((TTFChar) id);
    }

    public OverlayIconDescriptorBuilder add( IonIcon id, Color c ) {
        return add((TTFChar)id, c);
    }

    public OverlayIconDescriptorBuilder add( Entypo id ) {
        return add((TTFChar) id);
    }

    public OverlayIconDescriptorBuilder add( Entypo id, Color c ) {
        return add((TTFChar)id, c);
    }

    public OverlayIconDescriptorBuilder iconSize( IconSize v ) {
        this.iconSize = v;
        return this;
    }

    public OverlayIconDescriptorBuilder compatibleId( String compatibleId ) {
        this.compatibleId = compatibleId;
        return this;
    }

    public IOverlayIconDescriptor build() {
        if ( iconIdList.size() == 0 ) {
            throw new IllegalStateException();
        }
        if ( colorList.size() == 0 ) {
            for ( int i = 0; i < iconIdList.size(); i++ ) {
                colorList.add( Color.BLACK );
            }
        }
        for ( int i = 0; i < colorList.size(); i++ ) {
            if ( colorList.get( i ) == null ) {
                colorList.set( i, Color.BLACK );
            }
        }
        if ( iconSize == null ) {
            iconSize = IconSize.MEDIUM;
        }
        return new IOverlayIconDescriptor() {
            @Override
            public List<Object> getIconIDList() {
                return iconIdList;
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
            public List<Color> getIconColorList() {
                return colorList;
            }
        };
    }
}
