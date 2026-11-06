package ru.inversion.fx.form.controls;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;
import ru.inversion.fx.form.JInvFEDialog;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 Кнопка для показа содержимого TextInputControl в отдельном окне.
 @author perov
 @version 1.0.1 */
public class JInvFEButton extends JInvButton {
    private static final ResourceBundle fore = ResourceBundle.getBundle( "fore" );
    private StringConverter<String> stringConverter;

    /**
     Задать свой конвертер.
     При открытии диалога будет вызываться toString, а при закрытии – fromString
     @param stringConverter Конвертер
     */
    public void setStringConverter( final StringConverter<String> stringConverter ) {
        this.stringConverter = stringConverter;
    }
    public StringConverter<String> getStringConverter() {
        return stringConverter;
    }

    public JInvFEButton( TextInputControl ed ) {
        this();
        setTextField( ed );
    }

    public JInvFEButton() {
        setGraphic( IconFactory.getLabel( FontAwesome.fa_ellipsis_h ) );
        setOnAction( new FEButtonHandler() );
        setFocusTraversable( false );
        setTooltip( new Tooltip( fore.getString( "OTKRYT_V_DIALOGOVOM_OKNE" ) ) );
    }

    /**
     JAVAKERNEL-542
     должны дизаблиться, а вот ФЕбутоны не должны
     */
    @Override
    public void setTextField( TextInputControl ed ) {
        super.setTextField( ed );
        this.setDisable( false );
    }

    private class FEButtonHandler implements EventHandler {
        @Override
        public void handle( Event event ) {
            TextInputControl p = getTextField();
            if ( p != null ) {
                JInvFEDialog dialog = new JInvFEDialog( p, stringConverter );
                Optional<String> op = dialog.showAndWait();
                op.ifPresent( p::setText );
            }
        }
    }
}
