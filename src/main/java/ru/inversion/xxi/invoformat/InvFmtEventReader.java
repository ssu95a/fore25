package ru.inversion.xxi.invoformat;


import ru.inversion.utils.ReaderScanner;
import ru.inversion.xxi.invoformat.reader.IItemState;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;
import java.util.function.BiConsumer;

import static ru.inversion.xxi.invoformat.reader.IItemState.EMPTY_EVENT;

/** */
public class InvFmtEventReader implements Iterable<InvFmtEvent> {

    /** */
    private final Iterator< ReaderScanner.IContext > rsIterator;
    /** */
    private IItemState<InvFmtEvent> currentState = null;//IItemState.getState(IItemState.BEFORE_STATE);
    private boolean	                insideBlock	 = false;
    private int                     nCountAttr   = 0,
                                    nCountBlock = 0;

    /** */
    private InvFmtEventReader( Reader reader ) {
        rsIterator = ReaderScanner.newIterable( reader ).iterator();
    }

    /** */
    @Override
    public Iterator< InvFmtEvent > iterator() {

        return new Iterator<InvFmtEvent>() {

            private InvFmtEvent current;

            {
                getNextEvent();
            }

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public InvFmtEvent next( ) {

                if( !hasNext( ) )
                    throw new NoSuchElementException();

                final InvFmtEvent t = current;

                getNextEvent( );

                return t;
            }

            /** */
            private void getNextEvent( ) {

                InvFmtEvent t = null;

                while( rsIterator.hasNext() && t == null ) {
                    t = InvFmtEventReader.this.handle( rsIterator.next() );
                }
                current =  t;
            }
        };
    }

    /** */
    private InvFmtEvent handle( ReaderScanner.IContext context ) {

        try {

            if( currentState == null ) {

                if( context.current() == '#' ) {
                    currentState = IItemState.getState(IItemState.BEGIN_END_STATE);
                }
                else
                {
                    // До первого блока все пропускаем
                    if( nCountBlock == 0 )
                        return null;

                    if( insideBlock )
                    {
                        if(context.current() == '/' && context.next() == '/')
                            currentState = IItemState.getState( IItemState.COMMENT_STATE   );
                        else
                            currentState = IItemState.getState( IItemState.ATTRIBUTE_STATE );
                    }
                    else
                    {
                        currentState = IItemState.getState(IItemState.COMMENT_STATE);
                    }
                }
            }

            InvFmtEvent event = currentState.apply( context );

            if( event != null ) {

                currentState = null;

                if( event != EMPTY_EVENT )
                {
                    if( event.isEndElement() )
                        insideBlock = false;
                    else
                        if( event.isBeginElement() ) {
                            insideBlock = true;
                            nCountBlock++;
                        }
                }//end if
                else
                    event = null;
            }

            if( event != null && event.isAttributeElement() ) {
                nCountAttr++;
            }

            return event;
        }
        catch( Throwable th ) {
            throw new RuntimeException( "Error on parse InvFmt text at line " + context.lineNum() + ", pos " + context.symbNum(), th );
        }
    }

    /** */
    static public InvFmtEventReader createInvFmtReader( Reader reader ) throws InvFmtException {
        return new InvFmtEventReader( reader );
    }

    /** */
    static public InvFmtEventReader createInvFmtReader( File file ) throws InvFmtException {
        try {
            return createInvFmtReader( new FileReader(file) );
        }
        catch( Exception ex ) {
            throw new InvFmtException( "Ошибка при открытии файла", ex );
        }
    }

    /** */
    public static void main(String[] args) {

        String lastName = null;
        
        try {


            final InvFmtEventReader r = createInvFmtReader(
                new File("d:\\XXI\\invo\\ap_800205_275.inv")
            );

            String evName = null;

            Map<String,Object> values = new HashMap<>();

            for( InvFmtEvent e : r ) {

                if( e.isBeginElement() )
                    evName = e.getName();

                else {

                    if( e.isAttributeElement() )
                    {
                        values.put(e.getName(),e.asAttributeEvent().getValue() );
                    }
                }

            }//

            values.forEach(new BiConsumer<String, Object>() {
                @Override
                public void accept(String s, Object o) {
                    System.out.println(s + " = " + o );
                }
            });


        }
        catch( Throwable th ) {
            System.out.println("LAST_NAME: " + lastName );
            th.printStackTrace();
        }
    }

}
