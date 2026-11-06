package ru.inversion.diff.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.asList;

public class PropertyAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyAccessor.class);

    private final String propertyName;
    private final Class<?> type;
    private final Method readMethod;
    private final Method writeMethod;

    public PropertyAccessor(String propertyName, Method readMethod, Method writeMethod) {
        this.propertyName = Objects.requireNonNull(propertyName, "propertyName can't be null");
        this.readMethod = makeAccessible(Objects.requireNonNull(readMethod, "readMethod can't be null"));
        this.writeMethod = makeAccessible(writeMethod);
        this.type = this.readMethod.getReturnType();
    }

    public Object get(Object target) {
        if (target == null) {
            return null;
        }
        try {
            return readMethod.invoke(target);
        } catch (Exception e) {
            throw new PropertyReadException(propertyName, target.getClass(), e);
        }
    }

    /**
     * @return true если успешно установили новое значение
     */
    public boolean set(Object target, Object value) {
        if (target == null) {
            LOG.info("Couldn't set new value '{}' for property '{}' " +
                    "because the target object is null", value, propertyName);
            return false;
        } else if (writeMethod == null) {
            LOG.warn("No setter found for property ({})", propertyName);
            return false;
        } else {
            invokeWriteMethod(target, value);
            return true;
        }
    }

    public Set<Annotation> getFieldAnnotations() {
        return getFieldAnnotations(readMethod.getDeclaringClass());
    }

    public Set<Annotation> getReadMethodAnnotations() {
        return new LinkedHashSet<Annotation>(asList(readMethod.getAnnotations()));
    }

    public <T extends Annotation> T getReadMethodAnnotation(Class<T> annotationClass) {
        final Set<? extends Annotation> annotations = getReadMethodAnnotations();
        for (final Annotation annotation : annotations) {
            if (annotationClass.isAssignableFrom(annotation.annotationType())) {
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    private Method makeAccessible(Method method) {
        if (method != null && !method.isAccessible()) {
            method.setAccessible(true);
        }
        return method;
    }

    private Set<Annotation> getFieldAnnotations(Class<?> clazz) {
        try {
            return new LinkedHashSet<Annotation>(asList(clazz.getDeclaredField(propertyName).getAnnotations()));
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return getFieldAnnotations(clazz.getSuperclass());
            } else {
                LOG.debug("Cannot find propertyName: {}, declaring class: {}", propertyName, clazz);
                return new LinkedHashSet<Annotation>(0);
            }
        }
    }

    private void invokeWriteMethod(final Object target, final Object value) {
        try {
            writeMethod.invoke(target, value);
        } catch (Exception e) {
            throw new PropertyWriteException(propertyName, target.getClass(), value, e);
        }
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getType() {
        return type;
    }
}

