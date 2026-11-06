package ru.inversion.dataset.fx;

import javafx.util.Callback;

/**
 * Сущность для хранения параметров связывания DataSet и Control.
 * <p>
 * @author antonovdi 
 */
public class BindControlInfo<T> implements Comparable {

    /** */
    private String  dataSetColumn;
    
    /** */
    private Object  component;
    
    /** */
    private Callback<T, ? extends Object> callBack;
    
    /** */
    private int order;

    public BindControlInfo( String dataSetColumn, Object component, Callback<T, ? > callBack, int order ) {
        this.dataSetColumn = dataSetColumn;
        this.component     = component;
        this.callBack      = callBack;
        this.order         = order;
    }
    
    /** */
    public BindControlInfo( ) {
    
    }
    
    /** */
    public Object getComponent( ) {
        return component;
    }

    public void setComponent(Object control) {
        this.component = control;
    }

    public Callback<T, ? extends Object> getClbk() {
        return callBack;
    }

    public void setClbk(Callback<T, ? extends Object> callBack) {
        this.callBack = callBack;
    }

    public String getDataSetColumn() {
        return dataSetColumn;
    }

    public void setDataSetColumn(String dataSetColumn) {
        this.dataSetColumn = dataSetColumn;
    }

    /**
     * Возвращает порядок следования компонента. 
     * <p>
     * Используется например в отображении на диалоге фильтра
     *
     * @return
     *      порядок следования компонента
     */
    public int getOrder() {
        return order;
    }

    /**
     * Порядок следования компонента. Используется например в отображении на диалоге фильтра
     * <p>
     * @param order 
     *      порядок следования компонента
     */
    public void setOrder(int order) {
        this.order = order;
    }
    
    /** */
    @Override
    public int compareTo(Object o) {
        return Integer.compare( order, ((BindControlInfo)o).getOrder() );
    }
}
