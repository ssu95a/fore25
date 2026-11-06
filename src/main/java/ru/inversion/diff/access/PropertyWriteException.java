package ru.inversion.diff.access;

public class PropertyWriteException extends PropertyAccessException {

    private static final long serialVersionUID = -5989510248552167223L;

    private final transient Object newValue;

    public PropertyWriteException(String propertyName, Class<?> targetType, Object newValue, Throwable cause) {
        super(propertyName, targetType, cause);
        this.newValue = newValue;
    }

    @Override
    public String getMessage() {
        return String.format("Failed to write new value '%s' to property '%s' of type '%s'",
                newValue, getPropertyName(), getTargetType().getCanonicalName());
    }

    public Object getNewValue() {
        return newValue;
    }


}
