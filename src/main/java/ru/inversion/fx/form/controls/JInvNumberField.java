package ru.inversion.fx.form.controls;
import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.util.StringConverter;
import ru.inversion.fx.form.TypeStringConverter;
import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 *
 * @author ssu @
 */
public class JInvNumberField< T extends Number > extends JInvValueField<T> {

    private boolean fromSetText = false;
    private boolean fromSetValue = false;

    private StringConverter stringConverter = new TypeStringConverter<>( Number.class );

    protected Property<T> valueProperty;

    /**
     *
     */
    public JInvNumberField() {
        this((String) null);
    }

    /**
     *
     */
    public JInvNumberField( String arg0 )
    {
        super(arg0);

        setAlignment(Pos.BASELINE_RIGHT);

        textProperty().addListener((v,o,n) -> {
            if ( S.isNullOrEmpty( o ) && S.isNullOrEmpty( n ) ){
                return;
            }
            try {
                if (fromSetValue) {
                    return;
                }
                fromSetText = true;
                String text1 = getText();
                try {
                    if (S.isNullOrEmpty(text1)) {
                        setValue(null);
                    } else {
                        setValue(getConverter().fromString(text1));
                    }
                    if (getState() == ERROR) {
                        setState(getValue() == null ? State.NULL : State.VALUE);
                    }
                } finally {
                    fromSetText = false;
                }
            } catch (Throwable th) {
                setState(State.ERROR);
            }
        });

        focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (!newValue) {

                if (getState() != ERROR) {

                    fromSetValue = true;

                    try {

                        if (getState() == State.NULL) {
                            setText(null);
                        } else {
                            setText(getConverter().toString(getValue()));
                        }
                    } finally {
                        fromSetValue = false;
                    }
                }
            }
        });

        valueProperty().addListener((Observable observable) -> {
            setState(getValue() == null ? State.NULL : State.VALUE);

            if (!fromSetText) {

                fromSetValue = true;

                try {
                    setText(getConverter().toString(getValue()));
                } finally {
                    fromSetValue = false;
                }
            }
        });

    }

    @Override
    public Property<T> valueProperty() {
        if( valueProperty == null )
            valueProperty = new SimpleObjectProperty<>(this, "value");
        return valueProperty;
    }

    @Override
    public Class getClassValue() {
        return ( valueProperty == null || valueProperty.getValue() == null ) ? Number.class : valueProperty.getValue().getClass();
    }

    /**
     *
     */
    public JInvNumberField(T value) {
        this();
        setValue(value);
    }

    /**
     *
     */
    protected StringConverter<T> getConverter(){
        return stringConverter;
    }

    public void setConverter( final StringConverter stringConverter ) {
        this.stringConverter = stringConverter;
    }
}
