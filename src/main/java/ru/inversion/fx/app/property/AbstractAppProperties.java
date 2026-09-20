package ru.inversion.fx.app.property;

import ru.inversion.fx.app.BaseApp;
import ru.inversion.utils.ResourceBundleFactory;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Supplier;

/**
 *
 * @author ssu @
 */
public abstract class AbstractAppProperties implements IAppProperties {

	public static ResourceBundle fore = ResourceBundleFactory.INSTANCE().getBundle("fore");

	final private BaseApp app;
	
	public AbstractAppProperties( BaseApp app ) {
		this.app = app;
	}
	/** */
	public BaseApp getApp() {
		return app;
	}
	/** */
	abstract public PropertiesTypeEnum getType();
	/** */
	abstract protected <P> P doLoadProperty( String property );
	/** */
	protected <P> P loadProperty( String property ) {
		P value = app.overridePropertyValue( getType(), property );
		if( value == null )
			value = doLoadProperty( property );
		return value;
	}
	/** */
	@Override
	public <P> P getProperty( String property, P defaultValue ) {
		return U.nvl( loadProperty(property), defaultValue );
	}
	/** */
	@Override
	public <T> T getProperty( String property ) {
		return loadProperty(property);
	}
	/** */
	@Override
	public String getStringProperty( String property ) {
		Object o = loadProperty( property );	
		return o != null ? o.toString() : null;
	}
	/** */
	@Override
	public String getStringProperty( String property, String defaultValue ) {
		return U.nvl( getStringProperty( property ), defaultValue );
	}
	/** */
	@Override
	public Boolean getBooleanProperty( String property ) {

		return TypeConverter.convert( loadProperty( property ), Boolean.class );
		/*
		Boolean value = null;
		
		Object o = loadProperty( property );	
		
	    if( o != null )	
		{
			if( o instanceof Boolean )
				return (Boolean)o;
			if( o instanceof Number )
				return ((Number)o).intValue() != 0;
			String s = o.toString();
			if( s.equalsIgnoreCase("FALSE")		|| s.equals("0")
			|| s.equalsIgnoreCase("OFF") || s.equalsIgnoreCase("N"))
				value = false;
			else if( s.equalsIgnoreCase("TRUE")	|| s.equals("1")
			|| s.equalsIgnoreCase("ON")  || s.equalsIgnoreCase("Y"))
				value = true;
			else
				value = Boolean.valueOf(s);
		}//end if
		return value;
		 */
	}
	/** */
	@Override
	public Boolean getBooleanProperty( String property, Boolean defaultValue ) {
		return U.nvl( getBooleanProperty( property ), defaultValue );
	}
	/** */
	@Override
	public Integer getIntegerProperty( String property ) {
		return TypeConverter.convert( loadProperty( property ), Integer.class );
		/*
		Integer value = null;
		Object o = loadProperty( property );	
		
	    if( o != null )	
		{
			if( o instanceof Number )
				return ((Number)o).intValue();
			if( o instanceof Boolean )
				return ((Boolean)o) ? 1 : 0;
			String s = o.toString();
			try {
				value = Integer.parseInt(s);
			}
			catch( NumberFormatException ex ) {
				throw new PropertyException( java.text.MessageFormat.format(fore.getString("OSHIBKA_KONVERTACII_STROKI_V_CELOE_CHISLO"), new Object[] {s}), ex );
			}
		}//end if
		return value;
		*/
	}
	/** */
	@Override
	public Integer getIntegerProperty ( String property, Integer defaultValue ) {
		return U.nvl( getIntegerProperty( property ), defaultValue );
	}
	/** */
	@Override
	public Date getDateProperty( String property ) {

		return TypeConverter.convert( loadProperty( property ), Date.class );

		/*
		Date value = null;
		Object o = loadProperty( property );	
		
	    if( o != null )	{
			if( o instanceof Date )
				return (Date)o;
			String s = o.toString();
			try {
				value = DateFormat.getInstance().parse(s);
			} catch (ParseException ex) {
				throw new PropertyException( java.text.MessageFormat.format(fore.getString("OSHIBKA_KONVERTACII_STROKI_V_DATU"), new Object[] {s}), ex );
			}
		}//end if
		return value;
		*/
	}
	/** */
	@Override
	public Date getDateProperty ( String property, Date defaultValue ) {
		return U.nvl( getDateProperty( property ), defaultValue );
	}

	/** */
	@Override
	public void setProperty( String property, Object value ) {
		throw new UnsupportedOperationException( getClass().getName() + ".setProperty" );
	}

	/** */
	public <T> T getProperty( String property, Supplier<T> valueSupplier ) {
		return U.nvl( loadProperty(property), valueSupplier == null? null : valueSupplier.get() );
	}
}
