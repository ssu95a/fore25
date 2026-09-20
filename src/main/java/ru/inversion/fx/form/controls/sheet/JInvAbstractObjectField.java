package ru.inversion.fx.form.controls.sheet;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Cursor;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import org.controlsfx.control.textfield.CustomTextField;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

/**
 @author fomishkin on 22.04.2019. */
abstract class JInvAbstractObjectField<T> extends HBox {

    private final CustomTextField textField = new CustomTextField();

    private ObjectProperty<T> objectProperty = new SimpleObjectProperty<>();

    JInvAbstractObjectField() {
        super(1);
        textField.setEditable(false);
        textField.setFocusTraversable(false);

        StackPane button = new StackPane( IconFactory.getLabel( FontAwesome.fa_ellipsis_h ) );
        button.setCursor( Cursor.DEFAULT);

        button.setOnMouseReleased(e -> {
            if ( MouseButton.PRIMARY == e.getButton() ) {
                final T result = edit(objectProperty.get());
                if (result != null) {
                    objectProperty.set(result);
                }
            }
        });

        textField.setRight(button);
        getChildren().add(textField);
        HBox.setHgrow(textField, Priority.ALWAYS);

        objectProperty.addListener((o, oldValue, newValue) -> textProperty().set(objectToString(newValue)));
    }

    protected StringProperty textProperty() {
        return textField.textProperty();
    }

    ObjectProperty<T> getObjectProperty() {
        return objectProperty;
    }

    protected String objectToString(T object) {
        return object == null ? "" : object.toString(); //$NON-NLS-1$
    }

    protected abstract Class<T> getType();

    protected abstract T edit(T object);
}
