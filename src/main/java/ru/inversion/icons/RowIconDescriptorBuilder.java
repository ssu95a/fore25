package ru.inversion.icons;

import javafx.scene.paint.Color;
import ru.inversion.icons.enums.*;

import java.util.ArrayList;
import java.util.List;

/**
 Row Data Builder - для иконок в ряд
 @author fomishkin on 12.07.2017. */
public class RowIconDescriptorBuilder {
    private List<Object> iconIdList = new ArrayList<>();
    private IconSize iconSize;
    private String compatibleId;
    private Color color;
    private int padding = 0;

    public RowIconDescriptorBuilder() {
    }

    public RowIconDescriptorBuilder add( Object id ) {
        this.iconIdList.add( id );
        return this;
    }

    public RowIconDescriptorBuilder add( FontAwesome id ) {
        this.iconIdList.add( id );
        return this;
    }

    public RowIconDescriptorBuilder add( MaterialDesign id ) {
        this.iconIdList.add( id );
        return this;
    }

    public RowIconDescriptorBuilder add( IonIcon id ) {
        this.iconIdList.add( id );
        return this;
    }

    public RowIconDescriptorBuilder add( Entypo id ) {
        this.iconIdList.add( id );
        return this;
    }

    public RowIconDescriptorBuilder iconSize( IconSize v ) {
        this.iconSize = v;
        return this;
    }

    public RowIconDescriptorBuilder compatibleId( String compatibleId ) {
        this.compatibleId = compatibleId;
        return this;
    }

    public RowIconDescriptorBuilder color( Color v ) {
        this.color = v;
        return this;
    }
    public RowIconDescriptorBuilder padding( int v ) {
        this.padding = v;
        return this;
    }

    public IRowIconDescriptor build() {
        if ( iconIdList.size() == 0 ) {
            throw new IllegalStateException();
        }
        if ( color == null ) {
            color = Color.BLACK;
        }
        if ( iconSize == null ) {
            iconSize = IconSize.MEDIUM;
        }
        return new IRowIconDescriptor() {
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
            public List<Object> getIconIDList() {
                return iconIdList;
            }

            @Override
            public int getPadding() {
                return padding;
            }
        };
    }
}
