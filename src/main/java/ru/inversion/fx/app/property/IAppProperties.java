package ru.inversion.fx.app.property;

import java.util.Date;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author ssu @
 */
public interface IAppProperties {
	/** */
	void setProperty( String property, Object value );
	/** */
	void getProperties( Properties properties );
	/** */
	<T> T getProperty( String property );
	<T> T getProperty( String property, T defaultValue );
	<T> T getProperty( String property, Supplier<T> valueSupplier );

	// Deprecated zone
	String getStringProperty( String property );
	String getStringProperty( String property, String defaultValue );
	Boolean getBooleanProperty( String property );
	Boolean getBooleanProperty( String property, Boolean defaultValue );
	Integer getIntegerProperty( String property );
	Integer getIntegerProperty ( String property, Integer defaultValue );
	Date getDateProperty( String property );
	Date getDateProperty ( String property, Date defaultValue );
}