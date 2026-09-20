package ru.inversion.fx.form.controls;

import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.F7FilterTextField;
import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;
import ru.inversion.fx.form.valid.JInvValidTooltip;

/**
 * Интерфейс для контролов со специфичным состоянием.
 * Понадобился так как JInvValueField является TextField
 *
 * @author antonovdi
 */
public interface IStateControl {

    /**
     */
    ResourceBundle bundle = ResourceBundle.getBundle("valid");

    /**
     */
    BooleanProperty focusOnControl = new SimpleBooleanProperty(null, "focusOnControl", true);

    /**
     *
     */
    enum State {
        VALUE,
        NULL,
        ERROR//,
        //NOT_REQUIRED
    }

    /**
     *
     */
    default void showError() {

        Control control = (Control)this;

        focusOnControl.set(false);

        try {
            control.requestFocus();
            JInvValidTooltip.showValidTooltip(control, bundle.getString("CONVERT_ERROR"));
        } finally {
            focusOnControl.set(true);
        }
    }

    /**
     *
     */
    default void initFocusListener() {

        if( this instanceof Control ) {

            ObjectProperty<State> state = Objects.requireNonNull( stateProperty(), Tags.PRODUCT_LABEL + "stateProperty is null");

            Control control = (Control) this;

            control.focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {

                if( !newValue && focusOnControl.get() && control.getScene() != null && control.getScene().getFocusOwner() != null )
                {
                    if( control.getScene().getFocusOwner() instanceof Control )
                    {
                        Control focusControl = (Control) control.getScene().getFocusOwner();

                        if( focusControl.getId() != null && focusControl.getId().equalsIgnoreCase("btCancel") ) {
                            return;
                        }

                        if ( control instanceof F7FilterTextField ){
                            Optional<JInvCheckBox> checkBox = ( (F7FilterTextField) control ).getCheckExpression();
                            if ( checkBox.isPresent() && checkBox.get().equals( focusControl ) ){
                                Platform.runLater( () -> {
                                    checkBox.get().setSelected( !checkBox.get().isSelected() );
                                } );
                                return;
                            }
                        }
                    }

                    if( state.get() == ERROR ) {
                        showError();
                    }
                }
            });

            final Control lc = control instanceof JInvCalendar ? ((JInvCalendar) control).getEditor() : control;

            state.addListener((Observable observable) -> {

                if (((ObservableValue<State>) observable).getValue() == ERROR) {
                    lc.getStyleClass().add("jinv-text-error");
                } else {
                    lc.getStyleClass().remove("jinv-text-error");
                }
            });
        }
    }

    /**
     *
     */
    ObjectProperty<State> stateProperty();
}
