package ru.inversion.icons.enums;

/**
 * @author fomishkin on 26.06.2017.
 */
public interface TTFChar {
    String getCode();

    String getFontFamily();

    String getPrefix();

    String getName();

    default String getURI() {
        return getPrefix() + ":" + toString();
    }

    default int getFontWeight(){
        return 0;
    }

}
