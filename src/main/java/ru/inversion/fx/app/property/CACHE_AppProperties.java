package ru.inversion.fx.app.property;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ru.inversion.fx.app.BaseApp;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.APP_CACHE;

/**
 *
 * @author antonovdi
 */
public class CACHE_AppProperties extends AbstractAppProperties {

	/** */
//	final private Cache<String,Object> cache = CacheBuilder
//			.newBuilder()
//			.expireAfterAccess( 30, TimeUnit.MINUTES )
//			.build();

	final private Cache<String,Object> cache
		= Caffeine.newBuilder().expireAfterAccess(10L,TimeUnit.MINUTES).maximumSize(32).build();

	public CACHE_AppProperties( BaseApp app ) {
		super( app );
	}

	/** */
	@Override
	protected Object doLoadProperty( String property ) {
        return cache.getIfPresent(property);
	}

	/** */
	@Override
	public void getProperties( Properties properties ) {
		throw new UnsupportedOperationException( getClass().getName() + ".getProperties" );
	}

	@Override
	public PropertiesTypeEnum getType() {
		return APP_CACHE;
	}

	/**
	 * @param property
	 * @param value
	 */
	@Override
	public void setProperty( String property, Object value ) {
		cache.put( property, value );
	}

    /** */
    public <T> T getProperty( String property, Supplier<T> valueSupplier ) {

        if( property == null )
            return null;

        T value = getProperty(property);

        if( value == null ) {

            if( valueSupplier != null ) {

                value = valueSupplier.get();

                setProperty( property, value );

            }
        }

        return value;
    }

}
