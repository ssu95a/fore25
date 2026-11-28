package ru.inversion.fore.form.action;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Реестр операций одного владельца. Доступ и изменение выполняются в одном потоке.
 * Строки сравниваются без учета регистра. Ключи разных типов независимы.
 * Числовые ключи одного типа равны по compareTo, перечисления — по объявленному типу и константе.
 */
public final class ActionMap {
    private TreeMap<Comparable<?>, ForeAction> local;
    private ActionMap parent;
    private boolean retired;

    public ActionMap getParent() {
        return parent;
    }

    public void setParent(ActionMap parent) {
        requireLive();
        for (var scope = parent; scope != null; scope = scope.parent) {
            if (scope == this) {
                throw new IllegalArgumentException("ActionMap parent cycle");
            }
        }
        this.parent = parent;
    }

    public ForeAction get(Comparable<?> key) {
        if (key == null) {
            return null;
        }
        keyType(key);
        for (var scope = this; scope != null; scope = scope.parent) {
            if (scope.local != null) {
                ForeAction action = scope.local.get(key);
                if (action != null) {
                    return action;
                }
            }
        }
        return null;
    }

    public ForeAction getLocal(Comparable<?> key) {
        if (key == null) {
            return null;
        }
        keyType(key);
        return local == null ? null : local.get(key);
    }

    public ForeAction put(Comparable<?> key, ForeAction action) {
        requireLive();
        if (key == null) {
            throw new NullPointerException("key");
        }
        keyType(key);
        if (action == null) {
            return remove(key);
        }
        if (local == null) {
            local = new TreeMap<>(ActionMap::compare);
        }
        return local.put(key, action);
    }

    public ForeAction remove(Comparable<?> key) {
        if (key == null) {
            return null;
        }
        keyType(key);
        if (local == null) {
            return null;
        }
        ForeAction removed = local.remove(key);
        if (local.isEmpty()) {
            local = null;
        }
        return removed;
    }

    public void clear() {
        local = null;
    }

    public int size() {
        return local == null ? 0 : local.size();
    }

    /** Освобождает регистрации и отсоединяет реестр от родителя при закрытии владельца. */
    public void retire() {
        retired = true;
        local = null;
        parent = null;
    }

    private void requireLive() {
        if (retired) {
            throw new IllegalStateException("ActionMap owner is closed");
        }
    }

    void forEachLocal(BiConsumer<? super Comparable<?>, ? super ForeAction> action) {
        if (local != null) {
            local.forEach(action);
        }
    }

    private static Class<?> keyType(Comparable<?> key) {
        if (key instanceof Enum<?> value) {
            return value.getDeclaringClass();
        }
        Class<?> type = key.getClass();
        if (type == String.class || type == Byte.class || type == Short.class || type == Integer.class
                || type == Long.class || type == Float.class || type == Double.class
                || type == BigInteger.class || type == BigDecimal.class) {
            return type;
        }
        throw new IllegalArgumentException("Unsupported ActionMap key type: " + type.getName());
    }

    private static int compare(Comparable<?> left, Comparable<?> right) {
        Class<?> type = keyType(left);
        Class<?> otherType = keyType(right);
        if (type != otherType) {
            int order = type.getName().compareTo(otherType.getName());
            if (order == 0) {
                throw new IllegalArgumentException("Conflicting ActionMap key classes: " + type.getName());
            }
            return order;
        }
        return switch (left) {
            case String value -> value.compareToIgnoreCase((String) right);
            case Enum<?> value -> Integer.compare(value.ordinal(), ((Enum<?>) right).ordinal());
            case Byte value -> value.compareTo((Byte) right);
            case Short value -> value.compareTo((Short) right);
            case Integer value -> value.compareTo((Integer) right);
            case Long value -> value.compareTo((Long) right);
            case Float value -> value.compareTo((Float) right);
            case Double value -> value.compareTo((Double) right);
            case BigInteger value -> value.compareTo((BigInteger) right);
            case BigDecimal value -> value.compareTo((BigDecimal) right);
            default -> throw new IllegalArgumentException("Unsupported ActionMap key type: " + type.getName());
        };
    }
}
