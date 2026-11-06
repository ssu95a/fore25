package ru.inversion.icons.behaviors;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import ru.inversion.icons.enums.IconSize;

/**
 @author fomishkin on 01.08.2017. */
public class BinSizeChange implements SizeBehavior {
        public final IconSize size;

        public BinSizeChange( IconSize size ) {
            this.size = size;
        }

        @Override
        public <T extends Label> T changeSize( T t ) {
            final ImageView graphic = (ImageView) t.getGraphic();
            final int targetSize = this.size.getSize();
            final int realSize = (int) graphic.getImage().getWidth();
            if(realSize == targetSize) return t;
            final int offset =  ( targetSize - realSize ) /2;
            if(offset > 0) {
                graphic.setViewport( new Rectangle2D( -offset, -offset, targetSize, targetSize ) );
            } else {
                graphic.setViewport( new Rectangle2D( 0.0, 0.0, targetSize, targetSize ) );
            }
//            graphic.setFitWidth( targetSize );
            graphic.setPickOnBounds( true );
            graphic.setPreserveRatio( true );
            return t;
        }

    @Override
    public final IconSize getSize() {
        return size;
    }
}
