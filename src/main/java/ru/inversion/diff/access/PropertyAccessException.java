package ru.inversion.diff.access;

public class PropertyAccessException extends RuntimeException {

    private static final long serialVersionUID = 7708432473828119744L;

    private final String propertyName;
    private final Class<?> targetType;

    protected PropertyAccessException(String propertyName, Class<?> targetType, Throwable cause) {
        super(cause);
        this.propertyName = propertyName;
        this.targetType = targetType;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Class<?> getTargetType() {
        return targetType;
    }

}
