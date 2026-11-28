package ru.inversion.fore.form.action;

import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Реестр операций одного владельца. Доступ и изменение выполняются в одном потоке.
 * Строки сравниваются без учета регистра. Ключи разных типов независимы.
 * Целые ключи Integer сравниваются по значению, перечисления — по объявленному типу и константе.
 * Остальные ненулевые ключи преобразуются в строки через toString().
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
        return get((Object) key);
    }

    public ForeAction get(Object key) {
        Comparable<?> canonical = normalize(key);
        if (canonical == null) {
            return null;
        }
        for (var scope = this; scope != null; scope = scope.parent) {
            if (scope.local != null) {
                ForeAction action = scope.local.get(canonical);
                if (action != null) {
                    return action;
                }
            }
        }
        return null;
    }

    public ForeAction getLocal(Comparable<?> key) {
        return getLocal((Object) key);
    }

    public ForeAction getLocal(Object key) {
        Comparable<?> canonical = normalize(key);
        return local == null || canonical == null ? null : local.get(canonical);
    }

    public ForeAction put(Comparable<?> key, ForeAction action) {
        return put((Object) key, action);
    }

    public ForeAction put(Object key, ForeAction action) {
        requireLive();
        Comparable<?> canonical = normalize(Objects.requireNonNull(key, "key"));
        if (action == null) {
            return removeLocal(canonical);
        }
        if (local == null) {
            local = new TreeMap<>(ActionMap::compare);
        }
        return local.put(canonical, action);
    }

    public ForeAction remove(Comparable<?> key) {
        return remove((Object) key);
    }

    public ForeAction remove(Object key) {
        return removeLocal(normalize(key));
    }

    private ForeAction removeLocal(Comparable<?> key) {
        if (local == null || key == null) {
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

    private static Comparable<?> normalize(Object key) {
        return switch (key) {
            case null -> null;
            case String value -> value;
            case Enum<?> value -> value;
            case Integer value -> value;
            default -> Objects.requireNonNull(key.toString(), "key.toString()");
        };
    }

    private static Class<?> keyType(Comparable<?> key) {
        return key instanceof Enum<?> value ? value.getDeclaringClass() : key.getClass();
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
            case Integer value -> value.compareTo((Integer) right);
            default -> throw new IllegalArgumentException("Unsupported ActionMap key type: " + type.getName());
        };
    }
}
