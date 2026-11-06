package ru.inversion.fx.form.controls.treetableex;

import javafx.application.Platform;
import javafx.scene.control.Control;
import javafx.util.Callback;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IFilterControl;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tds.*;
import ru.inversion.utils.Pair;
import ru.inversion.utils.S;
import ru.inversion.utils.Tags;


import java.util.*;

import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_SHOW;


/** */
public class TSControlAdapter<P>
        implements ITreeDataSetItemListener<P>, ITreeDataSetNavigationListener<P>, AutoCloseable {

    /**
     * impl
     */
    private static class EntityPropertyCallback<P> implements Callback<P, Object> {
        /** */
        final private IEntityProperty<P,?> ep;
        /** */
        public EntityPropertyCallback(IEntityProperty<P,?> ep) {
            this.ep = ep;
        }
        /** */
        @Override
        public Object call(P pojo) {
            return pojo == null ? null : ep.invokeGetter(pojo);
        }
    }

    /*
    static private class ControlInfo<P> {
        private Object component;
        private Callback<P,?> callback;

        public ControlInfo(Object component, Callback<P, ?> callback) {
            this.component = component;
            this.callback = callback;
        }

        public Object getComponent() {
            return component;
        }

        public Callback<P, ?> getCallback() {
            return callback;
        }
    }
    */

    /** */
    protected ITreeDataSet<P> treeDataSet;

    /** */
    final protected List< Pair<Object,Callback<P,?>> > controlList = new ArrayList<>();

    /** */
    public TSControlAdapter( ITreeDataSet<P> dataSet ) {
        this.treeDataSet = Objects.requireNonNull(dataSet, "'dataSet' is null");
        this.treeDataSet.addItemListener(this);
        this.treeDataSet.addNavigationListener(this);
    }

    /** */
    public ITreeDataSet<P> getTreeDataSet() {
        return treeDataSet;
    }

    /** */
    protected void afterBindControl(Control control, IEntityProperty<P,?> entityProperty, Callback<P, ? extends Object> callBack)
    { }

    /**
     * Связывание UI контрола с полем TreeDataSet.
     */
    public TSControlAdapter<P> bindControl( Object component, String columnName, Callback<P,?> callBack, int order )  {

        try {

            if( component == null )
                return this;

            if( columnName == null && callBack == null )
                throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "columnName and callBack is null." );

            Class<P> rowClass = treeDataSet.getRowClass();

            if( rowClass == null )
                throw new IllegalStateException( Tags.PRODUCT_LABEL + "RowClass in DataSet is not initialized.");

            IEntityProperty<P,?> entityProperty = null;

            if( columnName != null )
            {
                entityProperty = EntityMetadataFactory.<P>getEntityMetaData(rowClass).getProperty(columnName);

                if( entityProperty == null )
                    throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "Property '" + columnName + "' not found in class '" + rowClass.getSimpleName() + "'");
            }

            if( callBack == null )
                callBack = new EntityPropertyCallback<>(entityProperty);

            controlList.removeIf((p) -> p.first == component);

            controlList.add( new Pair<>( component, callBack ) );

            if( entityProperty != null && component instanceof Control ) {

                Control control = (Control) component;

                afterBindControl( control, entityProperty, callBack );

            }

            return this;

        } catch ( Exception e ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on bind component to column '" + columnName + "' to dataSet", e );
        }
    }

    /** */
    public TSControlAdapter<P> bindControl( Object component, String columnName, Callback<P,?> callBack)  {
        return bindControl(component, columnName, callBack, 0);
    }

    /** */
    public TSControlAdapter<P> bindControl( Object control, Callback<P, ? > callBack) {

        if( callBack == null )
            return bindControl(control);

        final String columnName = Controls.getFieldNameFromControl(control);

        if( S.isNotNullOrEmpty(columnName) )
            return bindControl( control, columnName, callBack );

        return this;
    }

    /** */
    public TSControlAdapter<P> bindControl( Object control, String columnName )
    {
        return bindControl( control, columnName, null, 0 );
    }

    /* */
    public TSControlAdapter<P> bindControlList( List controls) {

        if( controls == null || controls.isEmpty())
            return this;

        for (Object control : controls) {

            String dsPropertyName = Controls.getFieldNameFromControl(control);

            if( S.isNotNullOrEmpty(dsPropertyName) )
                bindControl(control, dsPropertyName);
        }

        return this;
    }

    /* */
    public TSControlAdapter<P> bindControl( Object... controls) {

        if( controls == null || controls.length == 0 )
            return this;

        return  bindControlList( Arrays.asList(controls) );

//        for (Object control : controls) {
//
//            String dsPropertyName = Controls.getFieldNameFromControl(control);
//
//            if (S.isNotNullOrEmpty(dsPropertyName)) {
//                bindControl(control, dsPropertyName);
//            }
//        }
//
//        return this;
    }

    /** */
    public TSControlAdapter<P> bindControl( String idF7FilterGroup, Object... controls) {

        if (controls != null && idF7FilterGroup != null)
        {
            Arrays.stream(controls).filter(( Object t) -> t instanceof IFilterControl).
                    forEach((Object t) -> {
                        ((IFilterControl) t).setIdF7FilterGroup(idF7FilterGroup);
                    });
        }

        return bindControl(controls);
    }

    /** */
    @Override
    public void itemChanged( TreeDataSetItemEvent<P> event) {

        if(event.getEventType() == TreeDataSetItemEvent.ItemEventType.CHANGE_VALUE )
            Platform.runLater(() -> {
                P v = event.getNewValue();
                if( v != null )
                    controlList.stream().forEach((p) -> Controls.setValue ( p.first, p.second.call( v ) ) );
                else
                    controlList.forEach((p) -> Controls.setValue( p.first, null) );
            });
    }

    /** */
    @Override
    public void navigated( TreeDataSetNavigationEvent<P> event ) {

        Platform.runLater(() -> {

            final P v = event.getNewItem() == null ? null :  event.getNewItem().getValue();

            if( v != null )
                controlList.stream().forEach((p) -> Controls.setValue ( p.first, p.second.call( v ) ) );
            else
                controlList.forEach((p) -> Controls.setValue( p.first, null) );
        });
    }

    @Override
    public void close( ) {

        if( treeDataSet != null )
        {
            treeDataSet.removeItemListener(this);
            treeDataSet.removeNavigationListener(this);
            treeDataSet = null; //
        }
    }

    /** */
    public void showDSDialogInfo( ViewContext vc ) {
        if (treeDataSet != null) {
            Map<String, Object> param = new HashMap<>();
            param.put("dataSet", treeDataSet);
            FXFormLauncher launcher = new FXFormLauncher(BaseApp.APP().getCommonTaskContext(), vc, "ru/inversion/fx/form/fxml/DSDialogInfo.fxml");
            launcher.modal(true)
                    .dialogMode(VM_SHOW)
                    .initProperties(param)
                    .bundle(ResourceBundle.getBundle("fore"))
                    .show();
        }
    }
}
