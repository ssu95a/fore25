package ru.inversion.dataset.fx;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.util.Callback;
import ru.inversion.dataset.*;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IFilterControl;
import ru.inversion.fx.form.controls.JInvRadioGroup;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.*;
import java.util.function.Consumer;

import static ru.inversion.dataset.DataSetRowEvent.RowOperationEnum.UPDATE;
import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_DATA_SET_ADAPTER;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_SHOW;
import static ru.inversion.fx.form.controls.JInvTableColumn.*;

/**
 *
 * @author ssu
 * @param <T>
 */
public class DSControlAdapter<T> implements IDataSetRowListener<T>, IDataSetNavigationListener<T>, AutoCloseable {

    /**
     * impl
     */
    private class EntityPropertyCallback<T> implements Callback<T, Object> {

        /** */
        final private IEntityProperty ep;

        /** */
        public EntityPropertyCallback(IEntityProperty ep) {
            this.ep = ep;
        }

        /** */
        @Override
        public Object call(T pojo) {
            return pojo == null ? null : ep.invokeGetter(pojo);
        }
    }

    /** */
    protected IDataSet<T> dataSet;

    /** */
    final protected List< BindControlInfo<T>> controlList = new ArrayList<>();

    /** */
    public DSControlAdapter() {

    }

    /** */
    public DSControlAdapter(IDataSet<T> dataSet) {

        this.dataSet = dataSet;

        if( this.dataSet != null ) {
            this.dataSet.addNavigationListener(this);
            this.dataSet.addRowListener(this);
        }
    }

    /** */
    public List<BindControlInfo<T>> getBindedControls() {
        return controlList;
    }

    /** */
    public void setDataSet(IDataSet<T> ds) {

        if( this.dataSet == ds ) {
            return;
        }

        if (!controlList.isEmpty()
            && !U.containsNull(ds, this.dataSet)
            && !U.containsNull(ds.getRowClass(), this.dataSet.getRowClass())) {

            if( ds.getRowClass() != this.dataSet.getRowClass()) {
                //throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "The record type in both DataSet must be the same.");
                System.out.println( Tags.PRODUCT_LABEL + "wrn: different types of records in dataSets." );
            }
        }

        if( this.dataSet != null ) {
            this.dataSet.removeNavigationListener(this);
            this.dataSet.removeRowListener(this);
        }

        this.dataSet = ds;

        if( this.dataSet != null ) {
            this.dataSet.addNavigationListener(this);
            this.dataSet.addRowListener(this);
        }
    }

    /** */
    public IDataSet<T> getDataSet() { return dataSet; }

    /** */
    protected void afterBindControl(Control control, IEntityProperty<T,?> entityProperty, Callback<T, ? extends Object> callBack)
    { }

    /**
     * Связывание UI контрола с полем DataSet.
     * <p>
     * Заполняет временную Set с именами полей
     */
    public DSControlAdapter<T> bindControl( Object component, String columnName, Callback<T, ? extends Object> callBack, int order )
    {
        if( component == null ) {
            return this;
        }

        if( columnName == null && callBack == null ) {
            throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "columnName and callBack is null");
        }

        IDataSet ds = getDataSet();
        if( ds == null ) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "DataSet is not initialized.");
        }

        Class rowClass = ds.getRowClass();
        if( rowClass == null ) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "RowClass in DataSet is not initialized.");
        }

        IEntityProperty<T,?> entityProperty = null;

        if (columnName != null) {

            entityProperty = EntityMetadataFactory.<T>getEntityMetaData(rowClass).getProperty(columnName);

            if (entityProperty == null) {
                throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "Property '" + columnName + "' not found in class '" + rowClass.getSimpleName() + "'");
            }
        }

        if (callBack == null) {
            callBack = new EntityPropertyCallback<>(entityProperty);
        }

        controlList.removeIf((p) -> p.getComponent() == component);

        if (component instanceof Control && order == -1) {
            ((Control) component).getProperties().put(COLUMN_SHOW_IN_FILTER, Boolean.FALSE);
        }

        BindControlInfo info = new BindControlInfo<>(columnName, component, callBack, order);

        controlList.add(info);

        if (entityProperty != null && component instanceof Control) {
//            Class clazz = getDataSet( ).getRowClass( );
//            
//            IEntityProperty property = EntityMetadataFactory.<T>getEntityMetaData(clazz).getProperty(columnName);

            Control control = (Control) component;

            if (entityProperty.isTransient()) {
                control.getProperties().put(COLUMN_TRANSIENT, Boolean.TRUE);
            }

            IEntityProperty proxyFor = entityProperty.getProxyFor();
            if (proxyFor != null) {
                control.getProperties().put(COLUMN_PROXY_FOR, proxyFor.getColumnName());
            }

            control.getProperties().put(PROPERTY_DATA_SET_ADAPTER, this);

            afterBindControl(control, entityProperty, callBack);

        } else {
            if (component instanceof JInvRadioGroup) {
                ((JInvRadioGroup) component).getProperties().put(PROPERTY_DATA_SET_ADAPTER, this);
            }
        }

        return this;
    }

    /**
     *
     */
    public DSControlAdapter<T> bindControl(Object component, String columnName, Callback<T, ? extends Object> callBack) {
        return bindControl(component, columnName, callBack, 0);
    }

    /**
     *
     */
    public DSControlAdapter<T> bindControl(Object control, Callback<T, ? extends Object> callBack) {

        if (callBack == null) {
            bindControl(control);
        }

        String columnName = Controls.getFieldNameFromControl(control);
        if (S.isNotNullOrEmpty(columnName)) {
            return bindControl(control, columnName, callBack);
        }

        return this;
        //throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "Control without FIELD_NAME");
    }

    /** */
    public DSControlAdapter<T> bindControl(Object control, String columnName) {
        return bindControl(control, columnName, null, 0);
    }

    /** */
    public DSControlAdapter<T> bindControl( Object... controls ) {

        if( controls == null || controls.length == 0 )
            return this;

        for( Object control : controls ) {

            String controlFieldName = Controls.getFieldNameFromControl(control);

            if( S.isNotNullOrEmpty(controlFieldName) )
                bindControl(control, controlFieldName);
        }

        return this;
    }

    public DSControlAdapter<T> bindControl(String idF7FilterGroup, Object... controls) {

        if (controls != null && idF7FilterGroup != null) {
            Arrays.stream(controls).filter((Object t) -> t instanceof IFilterControl).
                forEach((Object t) -> {
                    ((IFilterControl) t).setIdF7FilterGroup(idF7FilterGroup);
                });
        }
        
        return bindControl(controls);
    }

    /**
     *
     */
    public void bindGroup(Node parentNode, String groupName) {
        throw new UnsupportedOperationException("bindGroup");
    }

    /**
     *
     */
    public void bindGroup(Node parentNode) {
        throw new UnsupportedOperationException("bindGroup.2");
    }

    /**
     *
     */
    @Override
    public void rowOperation(DataSetRowEvent<T> event) {

        if( event.getRowOperation() == UPDATE && !controlList.isEmpty() ) {
            Platform.runLater(() -> {
                final T row = event.getNewRow();
                controlList.forEach (
                    (p) -> Controls.setValue( p.getComponent(), p.getClbk().call(row) )
                );
            });
        }
    }

    protected ViewContext getViewContext()
    {
        if( controlList != null && !controlList.isEmpty() )
        {
            final BindControlInfo< T > bci = controlList.get(0);
            //bci.getComponent()
            if( bci.getComponent() instanceof Node )
            {
                final Node n = (Node)bci.getComponent();

                final AbstractBaseController< ? > ctrl = Controls.getControllerFromControl( n );
                if( ctrl != null )
                    return ctrl.getViewContext();

                return ViewContext.of( n.getScene().getWindow() );
            }
        }
        return null;
    }

    /** */
    @Override
    public void navigated( DataSetNavigationEvent<T> event ) {

        Platform.runLater(() -> {

            try {

                T v = event.getNewRow();

                if( v != null )
                    controlList.stream().forEach((p) -> Controls.setValue ( p.getComponent(), p.getClbk().call( v ) ) );
                else
                    controlList.forEach((p) -> Controls.setValue(p.getComponent(), null));

            } catch (Throwable ex) {
                JInvErrorService.handleException( getViewContext(), ex );
            }
        });

    }

    /** */
    @Override
    public void close( ) {

        if( dataSet != null ) {
            dataSet.removeRowListener(this);
            dataSet.removeNavigationListener(this);
            dataSet = null; //
        }
    }

    /** */
    public void refreshCurrentRowFromDB() {
        if( dataSet != null && dataSet instanceof ISQLDataSet )
            try {
                ((ISQLDataSet)dataSet).refreshCurrentRowFromDB();
            } catch(DataSetException e) {
                JInvErrorService.handleException( getViewContext(), e );
            }
    }

    /** */
    public void refreshCurrentRowFromDB( boolean refreshDependentData ) {
        if( dataSet != null && dataSet instanceof ISQLDataSet )
            try {
                ((ISQLDataSet)dataSet).refreshCurrentRowFromDB(refreshDependentData);
            } catch(DataSetException e) {
                JInvErrorService.handleException( getViewContext(), e );
            }
    }

    /** */
    public <D extends DSControlAdapter> void refreshCurrentRowFromDB( boolean refreshDependentData, Consumer<D> onNoDataFound ) {

        final Consumer<SQLDataSet> nc = new Consumer< SQLDataSet >() {
            @Override
            public void accept( SQLDataSet notUse ) {
                onNoDataFound.accept( (D)DSControlAdapter.this );
            }
        };

        if( dataSet != null && dataSet instanceof SQLDataSet )

            try {
                ((SQLDataSet)dataSet).refreshCurrentRowFromDB(refreshDependentData, nc );
            } catch(DataSetException e) {
                JInvErrorService.handleException( getViewContext(), e );
            }
    }

    /** */
    public void showDSDialogInfo(ViewContext vc) {

        if (dataSet != null) {
            Map<String, Object> param = new HashMap<>();
            param.put("dataSet", dataSet);
            FXFormLauncher launcher = new FXFormLauncher(BaseApp.APP().getCommonTaskContext(), vc, "ru/inversion/fx/form/fxml/DSDialogInfo.fxml");
            launcher.modal(true)
                //                            .dataObject(text)
                .dialogMode(VM_SHOW)
                .initProperties(param)
                .bundle(ResourceBundle.getBundle("fore"))
                .show();
        }
    }
}
