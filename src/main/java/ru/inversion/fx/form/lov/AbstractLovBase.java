package ru.inversion.fx.form.lov;

import javafx.beans.property.*;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.IParameters;
import ru.inversion.dataset.ParametersByIndex;
import ru.inversion.dataset.ParametersByName;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.IBaseControl;
import ru.inversion.utils.S;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiPredicate;

/**
 *
 * @author ssu
 */
public abstract class AbstractLovBase<T> implements ILov<T> {

    /** */
    final static protected Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

    /** */
    final static protected ResourceBundle fore = ResourceBundle.getBundle("fore");

    /** */
    public static final String SKIP_FILTER_STRING = "skip_filter_string";

    /** */
    protected static int SMALL_LOV_MAX_ROWS_COUNT = 25;

    static {
        try {
            SMALL_LOV_MAX_ROWS_COUNT = BaseApp.APP().getViewPrefService().getLovSmallCountRows();
        } catch( AppException ex ) {
            logger.error( Tags.PRODUCT_LABEL + "Ошибка при инициализации 'SMALL_LOV_MAX_ROWS_COUNT'", ex );
        }
    }

    /**
     * Меленький лов
     */
    private final BooleanProperty smallLovProperty = new SimpleBooleanProperty( this, "smallLov", false);

    public BooleanProperty smallLovProperty( ) {
        return smallLovProperty;
    }
    public boolean isSmallLov() {
        return smallLovProperty().get();
    }

    private ObjectProperty< IBaseControl > controlForProperty;

    public ObjectProperty< IBaseControl > controlForProperty() {
        if( controlForProperty == null )
            controlForProperty = new SimpleObjectProperty<>();
        return controlForProperty;
    }
    public IBaseControl getControlFor() {
        return controlForProperty().get();
    }
    public void setControlFor( IBaseControl controlFor ) {
        this.controlForProperty().set(controlFor);
    }

    /** */
    static private class LOVParameters implements IParameters {

        final private IParameters parameters;

        public LOVParameters( IParameters parameters ) {
            this.parameters = parameters;
        }
        @Override
        public boolean hasParameter( String parameterName ) {
            if( "lov_parameter".equals(parameterName) )
                return false;
            return parameters.hasParameter(parameterName);
        }
        @Override
        public boolean hasParameter( int parameterIndex ) {
            return parameters.hasParameter(parameterIndex);
        }
        @Override
        public Object getParameter(String parameterName) {
            return parameters.getParameter(parameterName);
        }
        @Override
        public Object getParameter(int parameterIndex) {
            return parameters.getParameter(parameterIndex);
        }
        @Override
        public void setParameter(int parameterIndex, Object value) {
            parameters.setParameter(parameterIndex, value);
        }
        @Override
        public void setParameter(String parameterName, Object value) {
            parameters.setParameter(parameterName, value);
        }
    }

    // Получение заголовка для LOV
	private final StringProperty title = new SimpleStringProperty( this, "title" ) {
        @Override
        public String get() {
            //Заголовок, заданный кодом
            String s = super.get();
            if( S.isNotNullOrEmpty(s) ) {
                return s;
            }
            //LOV_TITLE, если он без шаблонного текста
            ResourceBundle rb = getResourceBundle();
            if( rb != null && rb.containsKey( "LOV_TITLE" ) ){
                final String lovTitle = rb.getString( "LOV_TITLE" );
                if ( !lovTitle.equals( "LOV_TITLE" ) ){
                    return lovTitle;
                }
            }
            //Заглушка по умолчанию
            return fore.getString("VYBOR_IZ_SPISKA");
        }
    };

    /** */
    protected final ObjectProperty<T> value = new SimpleObjectProperty<>( this, "value" );

    private IParameters             parameters;
    private BiPredicate<T, Boolean> fnCheck;
    private ResourceBundle          bundle;
    private Map<String,Object>      properties;

    /** */
    public ObjectProperty<T> valueProperty() { return value; }

    public final void setValue( T value ) { valueProperty( ).set(value); }
    @Override
    public final T getValue( ) { return valueProperty( ).get( ); }

	/** Заголовок LOV */
    public final StringProperty titleProperty() {
        return title;
    }
	public final String getTitle( ) {
		return title.get();
	}
	public final void setTitle( String title) { this.title.set(title); }
    public final AbstractLovBase< T > title( String v ) { setTitle(v); return this; }

    /** */
    public void setResourceBundle( ResourceBundle bundle ) {
        this.bundle = bundle;
    }

    public void setSkipFilterString ( boolean val ) {
        setProperty (AbstractLovBase.SKIP_FILTER_STRING, val);
    }
    public boolean isSkipFilterString () {
        Object o = getProperty (AbstractLovBase.SKIP_FILTER_STRING);
        return o != null && (Boolean) o;
    }

    public ResourceBundle getResourceBundle( ) {
        return bundle;
    }

    /** */
	public void setParameters( IParameters p ) {
		if( p == null || ( p instanceof LOVParameters ) )
            this.parameters = p;
        else
            this.parameters = new LOVParameters(p);
	}
	public IParameters getParameters( ) {
		return parameters;
	}

    /** */
	public void setParameters( ParametersByIndex pi ) {
        setParameters((IParameters)pi);
	}
	public void setParameters( ParametersByName pn ) { setParameters((IParameters)pn); }
    public AbstractLovBase<T> parameters( ParametersByName pn ) { setParameters(pn); return this; }

    /** */
    @Override
    public boolean checkValue( T value ) {
        return checkValue( value, true );
    }
    /** */
    public boolean checkValue( T value, boolean doSetValue ) {

        // Если значение для проверки не совпадает с текущим значением, то проверяем
        if( getValue()!=null && !getValue().equals(value) )
        {
             return getCheck().test(value, doSetValue);
        }else if(getValue()==null && value!=null) {
             return getCheck().test(value, doSetValue);
        }
        return true;
    }

    /** */
    protected BiPredicate<T, Boolean> createDefaultCheck( ) {
        return (T t, Boolean u) -> { setValue(t); return true; };
    }

    /** */
    public void setCheck( BiPredicate<T, Boolean> fnCheck ) {
        this.fnCheck = fnCheck;
    }

    /** */
    public BiPredicate<T, Boolean> getCheck() {
        if( fnCheck == null ) {
            fnCheck = createDefaultCheck( );
        }
        return fnCheck;
    }

    /** */
    public void setProperty( String property, Object value ) {
        if( properties == null )
            properties = new HashMap<>();
        properties.put(property, value);
    }

    /** */
    public Object getProperty( String property ) {
        return properties == null ? null : properties.get( property );
    }

    protected Integer getMaxCountRow() {
        return SMALL_LOV_MAX_ROWS_COUNT;
    }


    /**
     * Координаты относительно TextField
     */
    private Pair<Integer,Integer> position;

    /**
     * Установка позиции окна для маленького лова
     * @param x
     * @param y
     */
    public void setPosition(int x, int y) {
        this.position = new Pair<>(x,y);
    }

    /**
     * Получение позиции окна для маленткого лова
     * @return
     */
    public Pair<Integer,Integer> getPosition(){
        return position;
    }

}

