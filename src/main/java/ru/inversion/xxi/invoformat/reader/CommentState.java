package ru.inversion.xxi.invoformat.reader;

import ru.inversion.utils.ReaderScanner;
import ru.inversion.xxi.invoformat.InvFmtEvent;

/**
 *
 * @author ssu @
 */
public class CommentState implements IItemState<InvFmtEvent> {

	@Override
	public InvFmtEvent apply( ReaderScanner.IContext context ) {

		if( context.current() == '\n' )
			return EMPTY_EVENT;

		return null;
	}
}
