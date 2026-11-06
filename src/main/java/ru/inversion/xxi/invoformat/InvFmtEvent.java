package ru.inversion.xxi.invoformat;

/**
 * 
 * @author ssu
 */
public abstract class InvFmtEvent {

	private String name;
	
	public InvFmtEvent( String name ) {
		this.name = name;
	}
	/** */
	public String getName( ) {
		return name;
	}
	/** */
	public abstract InvFmtEventTypeEnum getEventType();
	/** */
	public boolean isBeginElement() {
		return false;
	}
	/** */
	public boolean isEndElement() {
		return false;
	}
	/** */
	public boolean isAttributeElement() {
		return false;
	}
	/** */
	public BeginEvent asBeginEvent() { 
		return (BeginEvent)this;
	}
	/** */
	public EndEvent asEndEvent() { 
		return (EndEvent)this;
	}
	/** */
	public AttributeEvent asAttributeEvent() { 
		return (AttributeEvent)this;
	}
	/** */
	@Override
	public String toString() {
		return "InvFmtEvent { name='" + getName() + '\'' + ", type =" + getEventType() +"}\n";
	}
}
