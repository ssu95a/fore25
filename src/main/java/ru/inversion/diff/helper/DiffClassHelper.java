package ru.inversion.diff.helper;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.*;

import static java.util.Arrays.asList;

public final class DiffClassHelper {

    private static final Set<Class<?>> PRIMITIVE_WRAPPER_TYPES = getPrimitiveWrapperTypes();

    private static final Collection<Class<?>> PRIMITIVE_NUMERIC_TYPES = getPrimitiveNumericTypes();

    private static final Collection<Class<?>> EXTENDABLE_SIMPLE_TYPES = asList(
            BigDecimal.class,
            BigInteger.class,
            CharSequence.class,
            Calendar.class,
            Date.class,
            Enum.class
    );

    private static final List<Class<? extends Serializable>> FINAL_SIMPLE_TYPES = asList(
            Class.class,
            URI.class,
            URL.class,
            Locale.class,
            LocalDate.class,
            UUID.class
    );

    private DiffClassHelper() {
    }

    public static boolean isSimpleType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        } else if (clazz.isPrimitive()) {
            return true;
        } else if (PRIMITIVE_WRAPPER_TYPES.contains(clazz)) {
            return true;
        }
        for (final Class<?> type : FINAL_SIMPLE_TYPES) {
            if (type.equals(clazz)) {
                return true;
            }
        }
        for (final Class<?> type : EXTENDABLE_SIMPLE_TYPES) {
            if (type.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Class<?>> getPrimitiveWrapperTypes() {
        final Set<Class<?>> wrapperTypes = new HashSet<>();
        wrapperTypes.add(Boolean.class);
        wrapperTypes.add(Character.class);
        wrapperTypes.add(Byte.class);
        wrapperTypes.add(Short.class);
        wrapperTypes.add(Integer.class);
        wrapperTypes.add(Long.class);
        wrapperTypes.add(Float.class);
        wrapperTypes.add(Double.class);
        wrapperTypes.add(Void.class);
        return wrapperTypes;
    }

    private static Collection<Class<?>> getPrimitiveNumericTypes() {
        final Collection<Class<?>> numericTypes = new HashSet<>();
        numericTypes.add(char.class);
        numericTypes.add(byte.class);
        numericTypes.add(short.class);
        numericTypes.add(int.class);
        numericTypes.add(long.class);
        numericTypes.add(float.class);
        numericTypes.add(double.class);

        return numericTypes;
    }
}
