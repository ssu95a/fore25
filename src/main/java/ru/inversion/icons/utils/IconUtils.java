package ru.inversion.icons.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author banin on 01.06.2018.
 */
public class IconUtils {

    private final static String VECTOR_REGEX = "^(mdi|fa|icon|ion|ent|far|fas):";

    private IconUtils() {
    }

    public static boolean isBinaryIcon(String value) {
        return value.startsWith("bin");
    }

    public static boolean isVectorIcon(String value) {
        final Matcher matcher = Pattern.compile(VECTOR_REGEX).matcher(value);
        return matcher.find();
    }

}
