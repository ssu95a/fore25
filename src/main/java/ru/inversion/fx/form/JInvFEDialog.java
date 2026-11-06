package ru.inversion.fx.form;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Window;
import javafx.util.StringConverter;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.ITextFieldBase;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 @author fomishkin on 28.09.2017. */
public class JInvFEDialog extends Dialog<String> implements IFormStateListener {
    private static final ResourceBundle fore = ResourceBundle.getBundle( "fore" );
    protected ObjectProperty<StateEnum> state = new SimpleObjectProperty<>( StateEnum.ACTIVE );
    protected StringProperty stateText = new SimpleStringProperty( S.EMPTY_STRING );

    private final StringConverter<String> stringConverter;

    public JInvFEDialog( TextInputControl textField ) {
        this(textField, null);
    }
    public JInvFEDialog( TextInputControl textField, StringConverter<String> stringConverter ) {
        this.stringConverter = stringConverter;
        initOwner( textField.getScene().getWindow() );
        initDialog( textField );
    }

    /**
     Диалог просмотра данных в режиме чтения
     */
    public JInvFEDialog( Window windowContext, String text ) {
        this(windowContext, text, null);
    }
    public JInvFEDialog( Window windowContext, String text, StringConverter<String> stringConverter ) {
        this.stringConverter = stringConverter;
        initOwner( windowContext );
        final JInvTextArea textArea = new JInvTextArea( text );
        textArea.setEditable( false );
        initDialog( textArea );
    }
    /**
     Диалог просмотра/редактирования данных текстового поля
     */
    private void initDialog( final TextInputControl textField ) {
        setTitle( pickTitle(textField) );
        setResizable( true );
        getDialogPane().getButtonTypes().addAll( ButtonType.OK );

        String text = textField.getText();

        textField.selectAll();

        boolean isReadOnly = false;
        if (textField instanceof ITextFieldBase){
            isReadOnly = ((ITextFieldBase) textField).isReadOnly();

            ClipboardContent clipboardContent = ((ITextFieldBase) textField).getClipboardContent();
            if (!clipboardContent.isEmpty()){
                text = clipboardContent.getString();
            }
        }

        String inputText =
            U.nvl(stringConverter == null ? text : stringConverter.toString( textField.getText() ), S.EMPTY_STRING);
        TextArea textArea = new TextArea( inputText );
        textArea.editableProperty().bind( textField.editableProperty() );
        textArea.setWrapText( true );
        textArea.setMaxWidth( Double.MAX_VALUE );
        textArea.setMaxHeight( Double.MAX_VALUE );
        GridPane.setVgrow( textArea, Priority.ALWAYS );
        GridPane.setHgrow( textArea, Priority.ALWAYS );
        GridPane gridPane = new GridPane();
        gridPane.setMaxWidth( Double.MAX_VALUE );
        gridPane.add( textArea, 0, 0 );
        getDialogPane().setPrefWidth( 800 );
        getDialogPane().setContent( gridPane );
        ViewPrefAppService.localizeDialog( getDialogPane() );

        if (textField.isEditable() && !isReadOnly){
            setResultConverter( ( ButtonType param ) ->
                    U.nvl(stringConverter == null ? textArea.getText() : stringConverter.fromString( textArea.getText() ), S.EMPTY_STRING ));

            Platform.runLater(() -> {
                textArea.requestFocus();
                textArea.positionCaret(textField.getCaretPosition());
            });
        } else {
            setResultConverter(( ButtonType param ) -> null);
        }
    }

    private String pickTitle(TextInputControl textField) {
        String title = fore.getString( "INFORMACIYA" ); //1. Дефолтный текст "Информация"

        if (textField instanceof ITextFieldBase) {
            Tooltip tooltip = textField.getTooltip();
            if (tooltip != null && S.isNotNullOrEmpty(tooltip.getText())){
                title = tooltip.getText(); //2. Текст из тултипа самого поля
            }

            Label label = ((ITextFieldBase) textField).getLabel();

            if (label != null) {
                if (S.isNotNullOrEmpty(label.getText())) {
                    title = label.getText(); //3. Текст привязанного лейбла
                }

                Tooltip labelTooltip = label.getTooltip();
                if (labelTooltip != null && S.isNotNullOrEmpty(labelTooltip.getText())) {
                    title = labelTooltip.getText(); //4. Текст тултипа привязанного лейбла
                }

            }
        }
        return title;
    }

    /** */
    @Override
    public ObjectProperty<StateEnum> stateProperty() {
        return state;
    }

    @Override
    public StringProperty stateTextProperty() {
        return stateText;
    }
}
