package ru.inversion.xxi.invoformat;

/**
 *
 * @author ssu @
 */
public class BeginEvent extends InvFmtEvent {
	
	/** */
	public BeginEvent( String name ) {
		super(name);
	}
	/** */
	@Override
	public InvFmtEventTypeEnum getEventType() {
		return InvFmtEventTypeEnum.BEGIN_ELEMENT;
	}
	/** */
	@Override
	public boolean isBeginElement() {
		return true;
        }
}
