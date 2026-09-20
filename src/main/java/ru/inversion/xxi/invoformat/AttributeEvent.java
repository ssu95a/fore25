package ru.inversion.xxi.invoformat;

import ru.inversion.utils.scheck.JInvStringWorker;

import java.math.BigDecimal;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 * @author ssu @
 */
public class AttributeEvent extends InvFmtEvent {

	private Object value = null;
	
	public AttributeEvent( String name, String s_value ) {
		super(name);
		determineValue(s_value);
                //value = s_value;
	}
	/** */
	private void determineValue( String s_value ) {

		try {
		
			if( s_value != null ) 
			{
				if( s_value.length() > 100 ) {
					value = s_value;
				}
				else {
					if( JInvStringWorker.INSTANCE().check( "INTEGER_WS", s_value, false ) == null ) {
						String s = s_value;
						if( s_value.indexOf(' ') != -1 ) {
							s = s_value.replaceAll( " ", "" );
						}
						value = Long.parseLong(s);
					}
					else if( JInvStringWorker.INSTANCE().check( "DECIMAL_WS", s_value, false ) == null ) {
						String s = s_value;
						if( s_value.indexOf(' ') != -1 ) {
							s = s_value.replaceAll( " ", "" );
						}
						value = new BigDecimal(s);
					}
					else if( JInvStringWorker.INSTANCE().check( "DATE_DD.MM.YY", s_value, false ) == null ) {
			            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");
			            value = new Date( dateFormat.parse(s_value).getTime() );
					}
					else if( JInvStringWorker.INSTANCE().check( "DATE_DD.MM.YYYY", s_value, false ) == null ) {
			            DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
			            value = new Date( dateFormat.parse(s_value).getTime() );
					}
				}
				
			}
		} catch( Exception ex ) {
			;
		}
		
		if( value == null )
			value = s_value;
	}
	/** */
	@Override
	public InvFmtEventTypeEnum getEventType() {
		return InvFmtEventTypeEnum.ATTRIBUTE;
	}
	/** */
	public Object getValue( ) {
		return value;
	}
	/** */
	@Override
	public boolean isAttributeElement() {
		return true;
	}
	/** */
	@Override
	public String toString() {
		return "AttributeEvent { name='" + getName() + '\'' + ", type =" + getEventType() + ", value = " + getValue() + "}\n";
	}
}
