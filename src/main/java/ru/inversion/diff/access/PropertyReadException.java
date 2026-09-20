package ru.inversion.diff.access;

public class PropertyReadException extends PropertyAccessException {

    private static final long serialVersionUID = 7949328971883290841L;

    public PropertyReadException(String propertyName, Class<?> targetType, Throwable cause) {
        super(propertyName, targetType, cause);
    }

    @Override
    public String getMessage() {
        return String.format("Failed to read value from property '%s' of type '%s'",
                getPropertyName(), getTargetType().getCanonicalName());
    }

}
