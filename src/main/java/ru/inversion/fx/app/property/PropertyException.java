package ru.inversion.fx.app.property;

import ru.inversion.utils.IExceptionInfo;

/**
 *
 * @author ssu @
 */
public class PropertyException extends RuntimeException implements IExceptionInfo {
	/** */
	public PropertyException( ) {
	}
	/** */
	public PropertyException( String msg ) {
		super( msg );
	}
	/** */
	public PropertyException( String msg, Throwable e ) {
		super( msg, e );
	}
	/** */
	public PropertyException( Throwable e ) {
		super( e );
	}

	/** */
	@Override
	public String getCategory() {
		return "app.prp";
	}
}