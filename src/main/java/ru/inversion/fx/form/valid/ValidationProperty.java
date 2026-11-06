package ru.inversion.fx.form.valid;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.Control;
import ru.inversion.fx.form.controls.Controls;

/**
 *
 * @author ssu @
 */
public class ValidationProperty implements ObservableBooleanValue {

	private final Validator validator;
	private final Control	control;
	
	public ValidationProperty( Validator validator, Control control ) {
		this.validator = validator;
		this.control   = control;
	}
	@Override
	public boolean get() {
		return validator.validate( Controls.getValue(control)) == null;
	}	
	@Override
	public void addListener(ChangeListener<? super Boolean> listener) {
	}
	@Override
	public void removeListener(ChangeListener<? super Boolean> listener) {
	}
	@Override
	public Boolean getValue() {
		return get();
	}
	@Override
	public void addListener(InvalidationListener listener) {
	}
	@Override
	public void removeListener(InvalidationListener listener) {
	}
}
