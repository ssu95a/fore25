package ru.inversion.fx.form.lov;

public class PLovMarkableParam<T> extends PLovParam {

    private final Class<T> rowClass;

    public PLovMarkableParam(Class<T> rowClass, JInvLOV lov, String filter) {
        super(lov, filter);
        this.rowClass = rowClass;
    }

    public Class<T> getRowClass() {
        return rowClass;
    }
}
