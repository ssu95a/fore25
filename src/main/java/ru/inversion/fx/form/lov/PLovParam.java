package ru.inversion.fx.form.lov;

public class PLovParam {

    private final JInvLOV lov;

    private final String filter;

    private Object result;

    PLovParam(JInvLOV lov, String filter) {
        this.lov = lov;
        this.filter = filter;
    }

    public JInvLOV getLov() {
        return lov;
    }

    public String getFilter() {
        return filter;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
