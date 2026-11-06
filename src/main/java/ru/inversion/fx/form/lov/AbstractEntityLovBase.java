package ru.inversion.fx.form.lov;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.scene.Node;
import javafx.util.Callback;
import javafx.util.Pair;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IBaseControl;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.fx.form.lov.exceptions.JInvLovException;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.ResourceBundleFactory;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiPredicate;

import static ru.inversion.dataset.IDataSet.DataQueryModeEnum.SINGLE_ROW;

/**
 *
 * @author ssu
 * @param <P> - класс с записью отображаемой в Lov
 * @param <T> - Тип атрибута который будет возвращен при выборе
 */
public abstract class AbstractEntityLovBase<P,T> extends AbstractLovBase<T> {

    /** */
    final private ObjectProperty<P> entityValue = new SimpleObjectProperty<>( this, "entityValue" );

    /** */
    final private StringProperty choiceOrderBy = new SimpleStringProperty( this, "choiceOrderBy" );

    /** */
    final private StringProperty wherePredicate = new SimpleStringProperty( this, "wherePredicate" );

    /** */
    final private StringProperty nativeQueryName = new SimpleStringProperty( this, "nativeQueryName" );

    /** */
    final private StringProperty queryAlias = new SimpleStringProperty( this, "queryAlias" );

    /**
     * Для хранения имени столбца.
     * Для установки фильтра valueColumnName + " = :lov_parameter"
     */
    protected String valueColumnName;

    /** */
    private Pair<String,Boolean> filterData;

    /** Взамен установки wherePredicate */
    private String filterString;

    /** */
    final private Class<? extends P> entityClass;

    /** */
    private TaskContext	taskContext;

    /** */
    private Callback<P,T> valueCallback;

    /** */
    private List< Pair<Object, Callback<P,Object>> > boundControls;
    private List< Pair<WritableValue<?>, Callback<P, ?>> > boundProperties;

    /** */
    protected SQLDataSet<P> valueDataSet;

    private ConfigDataSet configurator;

    private IJInvControl ctrl;

    /**
     * @param entityClass класс entity
     * @param valueColumnName имя поле в БД
     */
    protected AbstractEntityLovBase( Class<? extends P> entityClass, String valueColumnName ) {
        this(entityClass,valueColumnName,null);
    }

    protected AbstractEntityLovBase( Class<? extends P> entityClass) {
        this(entityClass,null,null);
    }

    protected AbstractEntityLovBase( Class<? extends P> entityClass, String valueColumnName, Callback<P,T> valueCallback ) {

        Objects.requireNonNull( entityClass, "'entityClass' is null" );

        this.entityClass     = entityClass;
        this.valueColumnName = valueColumnName;
        this.valueCallback   = valueCallback;

        entityValue.addListener((ObservableValue<? extends P> observable, P oldValue, P newValue) -> {
            Callback<P, T> vc = getValueCallback();
            if( vc != null )
                setValue(vc.call(newValue));
            notifyBoundControls  (newValue);
            notifyBoundProperties(newValue);
        });

        if( this.valueCallback == null )
        {
            final IEntityMetaData<P> em = EntityMetadataFactory.getEntityMetaData( (Class<P>)entityClass );
            final IEntityProperty<P,?> property;
            if( S.isNullOrEmpty( valueColumnName ) )
                property = em.getIDList().get(0);
            else
                property = em.getProperty(valueColumnName);

            this.valueCallback = (P param) -> {
                try {
                    return param == null ? null : (T) property.invokeGetter(param);
                } catch (Throwable th) {
                    throw new JInvLovException(java.text.MessageFormat.format( fore.getString("OSHIBKA_PRI_POLUCHENII_ZNACHENIYA_STOLBCA"), new Object[]{getValueColumnName()}), th);
                }
            };
        }
     }

    /** */
    public String getValueColumnName() {

        if( S.isNullOrEmpty(valueColumnName) )
        {
            IEntityMetaData<P> entityMetaData = EntityMetadataFactory.getEntityMetaData((Class<P>) entityClass);
            if( !entityMetaData.getIDList().isEmpty() )
                //valueColumnName = entityMetaData.getIDList().get(0).getColumnName(getTaskContext().dialect());
                return entityMetaData.getIDList().get(0).getColumnName(getTaskContext().dialect());
        }
        return valueColumnName;
    }

    /** */
    protected void init( ) {
        configurator = new ConfigDataSet();
    }

    /** */
    public void bindControl( Object control, Callback<P,Object> clbk ) {

        if( control == null || clbk == null )
            return;

        if( boundControls == null )
            boundControls = new ArrayList<>();
        else
            boundControls.removeIf( (p)-> p.getKey() == control );

        boundControls.add( new Pair<>(control, clbk) );
    }

    /**
     * @param control
     * @param clbk
     * @return */
    public AbstractEntityLovBase< P, T > bindCtrl( Object control, Callback<P,Object> clbk ) {
        bindControl( control, clbk );
        return this;
    }

    /**
     * @param control */
    public void unBindControl( Object control ) {
        if( boundControls != null && control != null )
            boundControls.removeIf( (p)-> p.getKey() == control );
    }

    /**
     * @param entityValue */
    protected void notifyBoundControls( P entityValue ) {
        if( boundControls != null )
            boundControls.forEach( (p)-> Controls.setValue( p.getKey(), entityValue != null ? p.getValue().call( entityValue ) : null ) );
    }

    /** */
    public void bindProperty ( WritableValue<?> property, Callback<P, ?> clbk ) {

        if( property == null || clbk == null )
            return;

        if( boundProperties == null )
            boundProperties = new ArrayList<> ();
        else
            boundProperties.removeIf ((p)-> p.getKey () == property );

        boundProperties.add ( new Pair<> (property, clbk) );
    }

    /**  */
    public AbstractEntityLovBase< P, T > bindPrpt ( WritableValue<?> property, Callback<P, ?> clbk ) {
        bindProperty( property, clbk );
        return this;
    }

    /** */
    public void unBindProperty ( WritableValue<?> property ) {

        if( boundProperties != null )
        {
            boundProperties.removeIf((p) -> p.getKey() == property);

            if( boundProperties.isEmpty() )
                boundProperties = null;
        }
    }

    /**
     *
     * @param entityValue
     */
    @SuppressWarnings("unchecked")
    protected void notifyBoundProperties ( P entityValue ) {
        if (boundProperties != null)
            boundProperties.forEach ((p)-> {
                ((WritableValue) p.getKey ()).setValue (entityValue != null ? p.getValue ().call (entityValue) : null);});
    }

    /** */
    public ObjectProperty<P> entityValueProperty( ) { return entityValue; }

    /** */
    public final void setEntityValue( P ev ) { entityValueProperty( ).set(ev); }
    public final P getEntityValue( ) { return entityValueProperty( ).get( ); }

    /** */
    public StringProperty choiceOrderByProperty( ) { return choiceOrderBy; }
    public void setChoiceOrderBy( String orderBy ) { choiceOrderByProperty( ).set( orderBy ); }
    public String getChoiceOrderBy( ) { return choiceOrderByProperty( ).get( ); }

    /**
     @see ru.inversion.dataset.ISQLDataSet#setFilter(String, boolean, boolean)
     */
    public void setFilter(String filterString){
        this.filterString = filterString;
    }
    public String getFilter() {
        return filterString;
    }

    /** */
    public StringProperty nativeQueryNameProperty( ) { return nativeQueryName; }
    public void setNativeQueryName( String v ) { nativeQueryName.set( v ); }
    public String getNativeQueryName( ) { return nativeQueryName.get( ); }
    public AbstractEntityLovBase< P, T > nativeQueryName( String v ) { setNativeQueryName(v); return this; }

    public StringProperty queryAliasProperty( ) { return queryAlias; }
    public void setQueryAlias( String v ) { queryAlias.set( v ); }
    public String getQueryAlias( ) { return queryAlias.get( ); }
    public AbstractEntityLovBase< P, T > queryAlias( String v ) { setQueryAlias(v); return this; }

    /** */
    public StringProperty wherePredicatProperty( ) { return wherePredicate; }
    /**
     @see ru.inversion.dataset.ISQLDataSet#setWherePredicat(String)
     */
    public void setWherePredicat( String wherePredicatStr ) { wherePredicatProperty( ).set( wherePredicatStr ); }
    public String getWherePredicat( ) { return wherePredicatProperty( ).get( ); }

    /** */
	public void setTaskContext( TaskContext taskContext ) { this.taskContext = taskContext; }
    public AbstractEntityLovBase<P,T> taskContext( TaskContext taskContext ) { setTaskContext(taskContext); return this; }

    /** */
	public TaskContext getTaskContext()
    {
        if( taskContext == null )
        {
            IBaseControl c1 = ctrl;
            IBaseControl c2 = getControlFor();

            if( c1 == null && c2 == null )
                logger.debug("LOV: getTaskContext() return null. class лова {} не связан с каким либо control. entity pojo: {} ", this.getClass().getName(), U.callIfNotNull(getEntityClass(), Class::getName));
            else
            {
                AbstractBaseController<?> cn = null;
                if (c1 != null)
                    cn = Controls.getControllerFromControl((Node) c1);
                if (cn == null && c2 != null)
                    cn = Controls.getControllerFromControl((Node) c2);

                if( cn == null )
                    logger.debug("LOV: getTaskContext() return null. class лова {}, control {} в control не установлен Controller", this.getClass().getName(), U.nvl(c1, c2).getClass());
                else
                    taskContext = cn.getTaskContext();
            }
        }
        return taskContext;
    }

    /**
     * @param filter
     * @param fixed */
    public void setChoiceFilter( String filter, boolean fixed ) {
        filterData = new Pair<> ( filter, fixed );
    }
    public Pair<String, Boolean> getChoiceFilter( ) {
        return filterData;
    }

    /** for textField setLOV @param valueCallback */
    public void setValueCallback( Callback<P,T> valueCallback ) {
        this.valueCallback = valueCallback;
    }



    private String sceneFileName;

    /**
     * Предоставляется возможность нарисовать часть сцены и отобразить её в основном контроллере лова,
     * предполагается что это JInvTable.
     * Можно не устанавливать, движок сам попытается проверить наличие файла соответствующего энтити
     *
     * @param sceneFileName имя файла сцены
     */
    public void setSceneFileName ( String sceneFileName ) {
        this.sceneFileName = sceneFileName;
    }
    public String getSceneFileName () {
        return sceneFileName;
    }


    /** */
    private String getColumnAsParameter( )
    {
        return getTaskContext().dialect().columnName( getValueColumnName() );
    }

    @SuppressWarnings("unchecked")
    public Callback<P,T> getValueCallback()
    {
        return this.valueCallback;
    }

    /**
     * @return  */
    public Class<? extends P> getEntityClass() {
        return entityClass;
    }

    /**
     * @return  */
    @Override
    public ResourceBundle getResourceBundle( ) {

        ResourceBundle rb = super.getResourceBundle();

        if( rb == null ) {
            rb = ResourceBundleFactory.INSTANCE().getBundle( getEntityClass() );
            setResourceBundle(rb);
        }

        return rb;
    }

    @Override
    protected BiPredicate<T, Boolean> createDefaultCheck() {

        if( configurator != null )
        {
            return configurator;
        }
        else
        {
            return new ConfigDataSet();
        }
    }


    public SQLDataSet<P> createDataSet(){

        SQLDataSet<P> tempDataSet = new SQLDataSet<>();
        tempDataSet.setRowClass(getEntityClass());
        tempDataSet.setTaskContext(getTaskContext());
        tempDataSet.setCallbackParameters(getParameters());
        tempDataSet.setWherePredicat(wherePredicate.getValue());
        tempDataSet.setFilter(filterString, true, true);
        tempDataSet.setNativeQueryName( getNativeQueryName() );
        if (getProperty("ru.inversion.dataset.query_alias") != null){
            tempDataSet.setQueryAlias((String) getProperty("ru.inversion.dataset.query_alias"));
        }
        return tempDataSet;

    }

    @Override
    public void onSetLov(IJInvControl ctrl) {
        this.ctrl = ctrl;
    }

    public IJInvControl getControl(){
        return ctrl;
    }


    private class ConfigDataSet implements BiPredicate<T, Boolean>, ChangeListener<String>, AutoCloseable {

        public ConfigDataSet() {
            init();
        }

        private void init()
        {
            valueDataSet = createDataSet();
            valueDataSet.setDataQueryModeEnum( SINGLE_ROW );

            String strSQL = valueDataSet.getSQL( );

            if( strSQL == null || !strSQL.contains(":lov_parameter") )
            {
                String columnAsParameter = getColumnAsParameter();

                if( !S.isNullOrEmpty(columnAsParameter) )
                    valueDataSet.setFilter( columnAsParameter + " = :lov_parameter", true, true );
            }

            wherePredicate.addListener(this);
        }

        @Override
        public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
            valueDataSet.setWherePredicat(newValue);
        }

        @Override
        public boolean test( T lovParameter, Boolean doSetValue )
        {
            try {

                if( lovParameter == null )
                {
                    if( doSetValue )
                        setEntityValue( null );

                    return true;
                }

                valueDataSet.setParameter( "lov_parameter", lovParameter );
                valueDataSet.executeQuery( );

                if( valueDataSet.isEmpty() )
                    return Boolean.FALSE;

                if( doSetValue )
                    setEntityValue( valueDataSet.getRow(0) );

                return Boolean.TRUE;
            }
            catch( Throwable th ) {
                throw new JInvLovException(th);
            }
        }
        @Override
        public void close() throws Exception {
            valueDataSet.close();
        }
    }

}
