package ru.inversion.xxi.invoformat.reader;

import ru.inversion.utils.ReaderScanner;
import ru.inversion.xxi.invoformat.AttributeEvent;

/**
 *
 * @author ssu @
 */
public class AttributeState implements IItemState<AttributeEvent> {

	private StringBuilder sb;
	private String		  name;
	private int			  internalState	= 0; // 0 - before, 1 - name, 2 - after, 3 - value, 4 - ml_value, -1 - eof
	
	/** */
	private void addChar( char ch ) {

		if( sb == null ) {

			if( ch == '\n' )
				return;

			sb = new StringBuilder();
		}

		sb.append(ch);
	}
	/** */
	private void processBeforeName( ReaderScanner.IContext context ) {

		if( context.current() == '\n' ) {
                    internalState = -1;
		}

		if( !Character.isSpaceChar( context.current()) ) {
                    internalState = 1;
                    processName( context );
		}
	}
        
	/** */
	private void processName( ReaderScanner.IContext context ) {
		
            char ch = context.current();

            if( Character.isSpaceChar(ch) || ch == '=' || ch == '\n' )   
            {
                
                if( sb == null )
                    throw new IllegalStateException("Ошибка в файле Инвоформата в части атрибута: после имени нет значения. строка - " + context.lineNum() + ", позиция - " + context.symbNum() );
                
                name          = sb.toString();
                sb            = null;
                internalState = 2;
                processAfterName( context );
            }// end if
            else 
                addChar( ch );
	}
        
	/** */
	private void processAfterName( ReaderScanner.IContext context ) {
		
            if( Character.isSpaceChar( context.current() ) || context.current() == '=' )
                return;

            if( context.current() == '/' || context.current() == '\n' && context.next() == '/' ) {
                internalState = 4;
		processMultiLineValue( context );
            }
            else {
                
                if( context.current() == '\n' )
                    internalState =-1;
                else {
                    internalState = 3;
                    processValue( context );
                }
            }
	}
        
	/** */
	private void processValue( ReaderScanner.IContext context ) {

		if( context.current() == '\n' )
                    internalState =-1;
		else 
                    addChar( context.current() );
	}
        
	/** */
	private void processMultiLineValue( ReaderScanner.IContext context ) {

		if( context.current() == '\n' && context.next() != '/' )
			internalState =-1;
		else
			addChar( context.current() );
	}

	/** */
	@Override
	public AttributeEvent apply( ReaderScanner.IContext context ) {

		switch( internalState ) {
			case  0: processBeforeName	  ( context ); break;
			case  1: processName	      ( context ); break;
			case  2: processAfterName     ( context ); break;
			case  3: processValue	  	  ( context ); break;
			case  4: processMultiLineValue( context ); break;
		}

		if( internalState == -1 )
			return produceItem( );

		return null;
	}

	/** */
	protected AttributeEvent produceItem( ) {
		if( name == null )
			return EMPTY_EVENT;
		else
			return new AttributeEvent( name, sb == null ? null : sb.toString() );
	}
}
