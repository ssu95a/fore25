package ru.inversion.xxi.invoformat.reader;

import ru.inversion.utils.ReaderScanner;
import ru.inversion.xxi.invoformat.AttributeEvent;
import ru.inversion.xxi.invoformat.InvFmtEvent;

import java.util.function.Function;

/**
 * @author ssu @
 */
public interface IItemState<T extends InvFmtEvent> extends Function< ReaderScanner.IContext,T > {

	public final static int BEFORE_STATE	= 0;
	public final static int COMMENT_STATE	= 1;
	public final static int BEGIN_END_STATE = 2;
	public final static int ATTRIBUTE_STATE = 3;
	
	public final static AttributeEvent EMPTY_EVENT = new AttributeEvent("",null);
	/*
	protected abstract void	clear( );
	protected abstract boolean doProcess( ReaderScanner.IContext context ) throws InvFmtException;
	protected abstract T produceItem( ) throws InvFmtException;

	public T process( ReaderScanner.IContext context ) throws InvFmtException {
		if( context.current() == 0 || !doProcess( context ) ) {
			 T item = produceItem( );
			 clear( );
			 return item;
		}//end if
		return null;
	} 
	*/

	/** */
	public static boolean isSpace( char ch ) {

		if( ch <= 127)
			return ch ==  32 || ch == 9 || ch == 12 || ch == 11 || ch == '\n' || ch == '\r';
		else
			return ch == 160 || Character.getType( ch ) == Character.SPACE_SEPARATOR;
	}

	/**
	static IItemState states[] = {
		new BeforeState(),
		new CommentState(),
		new BeginEndState(),
		new AttributeState()
	};
	*/

	/** */
	public static IItemState getState( int type ) {
		//return states[type];
		switch( type ) {
			case 0:
				return new BeforeState();
			case 1:
				return new CommentState();
			case 2:
				return new BeginEndState();
			case 3:
				return new AttributeState();
		}
		return null;
	} 	

}
