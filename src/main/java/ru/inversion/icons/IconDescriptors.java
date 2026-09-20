package ru.inversion.icons;
import ru.inversion.icons.enums.TTFChar;

/** */
public class IconDescriptors {
    /** */
    public static <T extends Enum<T> & TTFChar> IconDescriptorBuilder createIconDescriptorBuilder() {
        return new IconDescriptorBuilder<T>();
    }

    /** */
    public static <T extends Enum<T> & TTFChar> IconDescriptorBuilder createIconDescriptorBuilder( T iconId ) {
        return new IconDescriptorBuilder<>( iconId );
    }

    /** */
    public static OverlayIconDescriptorBuilder createOverlayIconDescriptorBuilder() {
        return new OverlayIconDescriptorBuilder();
    }

    /** */
    public static RowIconDescriptorBuilder createRowIconDescriptorBuilder() {
        return new RowIconDescriptorBuilder();
    }
}
