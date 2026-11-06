package ru.inversion.xxi.invoformat;

import ru.inversion.utils.Checks;
import ru.inversion.utils.S;

import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 *
 * @author ssu @
 */
public class InvFmtWriter implements Closeable {

    private static final DateFormat	  g_dateFormat	= new SimpleDateFormat( "dd.MM.yyyy" );
    private static final NumberFormat g_numberFormat= new DecimalFormat( "####################0.00" );

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
	
	private final Writer writer;

	private String beginElement;

	/** */
	private InvFmtWriter( Writer writer ) {
		this.writer = writer;
	}

	/** */
	public void write( InvFmtEvent invFmtEvent ) throws InvFmtException {
		switch( invFmtEvent.getEventType() ) {
			case BEGIN_ELEMENT:
				writeBeginElement( invFmtEvent.getName() );
			break;
			case END_ELEMENT:
				writeEndElement( );
			break;
			case ATTRIBUTE: {
				AttributeEvent ae = invFmtEvent.asAttributeEvent();
				writeAttributeElement( ae.getName(), ae.getValue() );
			}
			break;
		}
	}

	/** */
	public void writeBeginElement( String name ) throws InvFmtException  {

		try {
		
			beginElement = Checks.Require.text(name,"name");
			writer.append("# ").append(beginElement).append(" ").append("BEGIN").append('\n');
		}
		catch( Exception e ) {
			throw new InvFmtException( java.text.MessageFormat.format( fore.getString("OSHIBKA_PRI_ZAPISI_BEGIN_EHLEMENTA"), name ), e );
		}
	}

	/** */
	public void writeEndElement( )  throws InvFmtException  {

		try {

			if( S.isNullOrEmpty(beginElement) )
				 throw new IllegalStateException("Для вызова закрывающего элемента, необходимо сперва вызвать 'writeBeginElement' ('beginElement' is null)");

			writer.append("# ").append(beginElement).append(" ").append("END").append('\n');

		}
		catch( Exception e ) {
			throw new InvFmtException( java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_ZAPISI_END_EHLEMENTA"), new Object[] {beginElement}), e );
		}
	}

	/** */
	private static class MLStringMaker implements Consumer<Writer> {

		final private String s;

      private MLStringMaker( String s ) {
         this.s = s;
      }

      @Override
		public void accept( Writer writer ) {

			try {

				final String[] lines = s.split("\\R");

				writer.append('\n');

				for( String s1 : lines )
					  writer.append("/**/").append(s1).append('\n');

			} catch (IOException e) {
				throw new UncheckedIOException("Error on write multiline string", e);
			}
		}
	}

	/** */
	private Object castValue( Object value ) {

		if( value == null )
			 return S.EMPTY_STRING;

		if( value instanceof String )
		{
			final String s = (String)value;

			 if( s.indexOf('\n') > 0)
				  return new MLStringMaker(s);

		 	 return s;
		}

		if( value instanceof Date )
			 return g_dateFormat.format((Date)value);

		if( value instanceof BigDecimal)
			return g_numberFormat.format(value);

		return value.toString();
	}


	/** */
	public void writeAttributeElement( String name, Object value )  throws InvFmtException  {
		
		try {

			writer.write(name);

			final Object v = castValue(value);

			if( v instanceof String ) {
 				 writer.write( ' ' );
				 writer.write((String) v);
			}
			else
			  	if( v instanceof Consumer )
				{
					Consumer<Writer> c = (Consumer)v;
					c.accept( writer );
				}

			writer.write('\n');
		}
		catch( Exception e ) {
			throw new InvFmtException( java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_ZAPISI_ATRIBUTA"), new Object[] {name, value}), e );
		}
	}

	/** */
	public void writeComment( String comment ) throws InvFmtException {
		
		if( S.isNullOrEmpty(comment) )
			  return;
		
		try {
			writer.write("//");
			writer.write(comment);
			writer.write('\n');
		}
		catch( Exception e ) {
			throw new InvFmtException( fore.getString("OSHIBKA_PRI_ZAPISI_KOMMENTARIYA"), e );
		}
	}

	/** */
	@Override
	public void close() throws IOException {
		if( writer != null )
			 writer.close();
	}

	/** */
	public static InvFmtWriter createInvFmtWriter( File file ) throws InvFmtException {
		try {
			return new InvFmtWriter(new FileWriter(file));
		} catch (Exception ex) {
			throw new InvFmtException( java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_SOZDANII_INVFMTWRITER_PO_FAJLU"), new Object[] {file}), ex );
		}
	}
	/** */
	public static InvFmtWriter createInvFmtWriter( Writer writer ) {
		return new InvFmtWriter(writer);
	}
	/** */
	public static InvFmtWriter createInvFmtWriter( OutputStream os ) throws InvFmtException  {
		try {
			return new InvFmtWriter( new OutputStreamWriter(os));
		} catch (Exception ex) {
			throw new InvFmtException( fore.getString("OSHIBKA_PRI_SOZDANII_INVFMTWRITER_IZ_OUTPUTSTREAM"), ex );
		}
	}
}
