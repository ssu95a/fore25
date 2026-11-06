package ru.inversion.fx.app.frame.menu;

import ru.inversion.fx.form.controls.JInvMenuBar;

import java.util.List;
import java.util.Optional;

/**
 *
 * @author ssu
 */
public interface IMenuLoader {
	/** */
	List<IMenuItemData> getMenuItemList( );

	default Optional<JInvMenuBar> getMenuBar() {return Optional.empty();}
}
