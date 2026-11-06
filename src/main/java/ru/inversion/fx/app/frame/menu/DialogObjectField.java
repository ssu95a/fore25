package ru.inversion.fx.app.frame.menu;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import ru.inversion.utils.S;


/** */
public abstract class DialogObjectField<T> extends HBox {

    private final TextField textField = new TextField();

    private ObjectProperty<T> objectProperty = new SimpleObjectProperty<>();
    private ObjectProperty<StringConverter<T>> converterProperty = new SimpleObjectProperty<>();

    public DialogObjectField() {

        super(1);

        textField.setEditable(false);
        textField.setFocusTraversable(false);

        Button button = new Button("…" );
        //ActionFactory.assignButtonStyle( ActionFactory.ActionTypeEnum.DETAILS, button ) ;
        button.setOnAction( (e)->objectProperty.set( edit( objectProperty.get() ) ) );

        getChildren().add(textField);
        getChildren().add(button   );
        HBox.setHgrow( textField, Priority.ALWAYS);

        objectProperty.addListener((o, oldValue, newValue) -> textProperty().set(objectToString(newValue)));
    }

    public Property<StringConverter<T> > converterProperty( ) {
        return converterProperty;
    }

    public StringProperty textProperty() {
        return textField.textProperty();
    }

    public ObjectProperty<T> objectProperty() {
        return objectProperty;
    }

    protected String objectToString(T object) {
        return object == null ?
                    S.EMPTY_STRING
                    :
                    converterProperty.get() == null ? object.toString() : converterProperty.get().toString( object );
    }

    protected abstract Class<T> getType( );

    protected abstract T edit( T object );

}
