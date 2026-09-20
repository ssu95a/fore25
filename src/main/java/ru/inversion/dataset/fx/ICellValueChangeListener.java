package ru.inversion.dataset.fx;

/**
 *
 * @author ssu @
 */
@FunctionalInterface
public interface ICellValueChangeListener<P> {
    void changed( P pojoInstance, String property, Object oldValue, Object newValue );
}
