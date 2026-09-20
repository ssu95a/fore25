package ru.inversion.diff;

public class DiffAttributeAccessException extends RuntimeException {

    private static final long serialVersionUID = 6773590822821885862L;

    private final String attributeName;

    public DiffAttributeAccessException(String attributeName) {
        super(String.format("Can't access '%s' attribute", attributeName));
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }
}
