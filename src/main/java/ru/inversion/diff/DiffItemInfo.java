package ru.inversion.diff;

import ru.inversion.diff.access.PropertyAccessor;

import java.util.Collection;
import java.util.LinkedList;

public final class DiffItemInfo {

    private final Class<?> type;

    private final Collection<PropertyAccessor> accessors = new LinkedList<>();

    DiffItemInfo(Class<?> type) {
        if (type == null) throw new IllegalArgumentException("type can't be null");
        this.type = type;
    }

    void addPropertyAccessor(PropertyAccessor propertyAccessor) {
        if (propertyAccessor != null) {
            accessors.add(propertyAccessor);
        }
    }

    public Class<?> getType() {
        return type;
    }

    public Collection<PropertyAccessor> getAccessors() {
        return accessors;
    }
}
