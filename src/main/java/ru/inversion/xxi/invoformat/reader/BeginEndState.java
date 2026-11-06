package ru.inversion.xxi.invoformat.reader;

import ru.inversion.utils.ReaderScanner;
import ru.inversion.xxi.invoformat.BeginEvent;
import ru.inversion.xxi.invoformat.EndEvent;
import ru.inversion.xxi.invoformat.InvFmtEvent;

/**
 *
 * @author ssu @
 */
public class BeginEndState implements IItemState<InvFmtEvent> {

	private StringBuilder sb;
	private String		  name;
	private boolean		  isName = true;
	//private boolean		  eof	 = false;
	
	/** */
	@Override
	public InvFmtEvent apply( ReaderScanner.IContext context ) {

		char ch = context.current();
		
		if( ch == '#' )
			return null;
		
		if( ch == '\n' ) {
			return produceItem( );
		}
		
		if( Character.isSpaceChar(ch) ) 
		{
			if( !isName )
				 return null;
			else
			{
				if( sb == null )
					return null;
				else
				{
					isName = false;
					name   = sb.toString();
					sb	   = null;
					return null;
				}
			}
		}
		
		if( sb == null )
			sb = new StringBuilder();
		
		sb.append(ch);
		
		return null;
	}
	/** */
	protected InvFmtEvent produceItem( ) {
		if( isName || sb.length() == 0 )
			throw new IllegalStateException("name.length = 0");
		String type = sb.toString();
		if( "BEGIN".equalsIgnoreCase(type) )
			return new BeginEvent(name);
		if( "END".equalsIgnoreCase(type) )
			return new EndEvent(name);
		throw new IllegalStateException( "Неизвестный тип команды" );
	}

}
