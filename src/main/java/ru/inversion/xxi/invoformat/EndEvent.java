package ru.inversion.xxi.invoformat;

/**
 *
 * @author ssu @
 */
public class EndEvent extends InvFmtEvent {
	/** */
	public EndEvent(String name) {
		super(name);
	}
	/** */
	@Override
	public InvFmtEventTypeEnum getEventType() {
		return InvFmtEventTypeEnum.END_ELEMENT;
	}
	/** */
	@Override
	public boolean isEndElement() {
		return true;
	}
}
