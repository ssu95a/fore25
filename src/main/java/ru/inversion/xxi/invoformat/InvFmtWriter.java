package ru.inversion.xxi.invoformat;

import java.io.*;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 *
 * @author ssu @
 */
public class InvFmtWriter implements Closeable {

    private static final DateFormat	  g_dateFormat	= new SimpleDateFormat( "dd.MM.yyyy" );
    private static final NumberFormat g_numberFormat= new DecimalFormat( "####################0.00" );
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
	
	private Writer writer;
	private String lastBeginElement;
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
		
			lastBeginElement = null;
		
			if( name != null && !name.isEmpty() ) {
				lastBeginElement = name.toUpperCase();
				writer.append("# ").append(lastBeginElement).append(" ").append("BEGIN").append('\n');
			}
		}
		catch( Exception e ) {
			throw new InvFmtException( java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_ZAPISI_BEGIN_EHLEMENTA"), new Object[] {name}), e );
		}
	}
	/** */
	public void writeEndElement( )  throws InvFmtException  {
		try {
			if( lastBeginElement != null ) {
				writer.append("# ").append(lastBeginElement).append(" ").append("END").append('\n');
			}
		}
		catch( Exception e ) {
			throw new InvFmtException( java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_ZAPISI_END_EHLEMENTA"), new Object[] {lastBeginElement}), e );
		}
	}
	/** */
	private String castValue( Object value ) {
		if( value == null )
			return "";
		if( value instanceof String )
			return (String)value;
		if( value instanceof Date )
			return g_dateFormat.format((Date)value);
                if( value instanceof Integer )
			return String.valueOf((Integer)value);
                if( value instanceof Long )
			return String.valueOf((Long)value);
		if( value instanceof Number )
			return g_numberFormat.format((Number)value);
		return value.toString();
	}
	/** */
	public void writeAttributeElement( String name, Object value )  throws InvFmtException  {
		
		try {
			writer.write(name);
			writer.write( "=");
			writer.write( castValue(value) );
			writer.write('\n');
		}
		catch( Exception e ) {
			throw new InvFmtException( java.text.MessageFormat.format(fore.getString("OSHIBKA_PRI_ZAPISI_ATRIBUTA"), new Object[] {name, value}), e );
		}
	}
	/** */
	public void writeComment( String comment ) throws InvFmtException {
		
		if( comment == null || comment.isEmpty() )
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
