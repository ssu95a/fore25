package ru.inversion.fx.form.property;

import javafx.beans.property.SimpleObjectProperty;

/**
 *
 * @author ssu
 */
public class JInvIntegerProperty extends SimpleObjectProperty<Integer> {
    /** */
    public JInvIntegerProperty() {
    }
    /** */
    public JInvIntegerProperty(Integer initialValue) {
        super(initialValue);
    }
    /** */
    public JInvIntegerProperty(Object bean, String name) {
        super(bean, name);
    }
    /** */
    public JInvIntegerProperty(Object bean, String name, Integer initialValue) {
        super(bean, name, initialValue);
    }
    
}
