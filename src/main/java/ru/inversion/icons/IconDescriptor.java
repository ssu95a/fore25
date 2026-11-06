package ru.inversion.icons;
import ru.inversion.icons.descextensions.IColorable;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.TTFChar;

/**
 @author fomishkin on 12.07.2017. */
public interface IconDescriptor<ID> extends IBaseIconDescriptor, IColorable {
    ID getIconID();
    static IconDescriptor of(FontAwesome id){
        return IconDescriptorBuilder.of( id );
    }
    static IconDescriptor of(TTFChar id){
        return IconDescriptorBuilder.of( id );
    }
}
