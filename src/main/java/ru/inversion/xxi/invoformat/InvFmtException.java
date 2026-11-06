package ru.inversion.xxi.invoformat;

import ru.inversion.utils.IExceptionInfo;

/**
 *
 * @author ssu @
 */
public class InvFmtException extends RuntimeException implements IExceptionInfo {

	/** */
	public InvFmtException( String message) {
		super(message);
	}

	/** */
	public InvFmtException( String message, Throwable cause ) {
		super(message, cause);
	}

	/** */
	@Override
	public String getCategory() {
		return "Invo";
	}
}
