package ru.inversion.fx.form;

import javafx.fxml.FXML;
import javafx.scene.text.Font;
import ru.inversion.fx.app.service.exteditor.ExtensionType;
import ru.inversion.fx.form.controls.JInvTextArea;

import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_EDIT;
import static ru.inversion.fx.form.JInvTextAreaForm.*;

/**
 * FXML Controller class
 *
 * @author perov
 */
public class JInvTextAreaFormController extends JInvFXFormController<String> {

    @FXML
    private JInvTextArea edText;

    private ExtensionType type;

    @Override
    protected void init() throws Exception {
        super.init();

        edText.setText( getDataObject() );

        if (getInitProperties().containsKey(PROPERTY_EXTENSION_TYPE)) {
            type = getInitParameter(PROPERTY_EXTENSION_TYPE);
        }
        if (getInitProperties().containsKey(PROPERTY_TITLE) && getInitParameter(PROPERTY_TITLE) != null) {
            setTitle( getInitParameter(PROPERTY_TITLE) );
        }
        if ( getInitProperties().containsKey(PROPERTY_TERMINAL_FONT)
                && getInitParameter(PROPERTY_TERMINAL_FONT) != null
                && (Boolean) getInitParameter(PROPERTY_TERMINAL_FONT)
            ){
            edText.setFont(Font.font("Monospaced", 13));
        }
        if (getInitProperties().containsKey(PROPERTY_WRAP_TEXT)) {
            edText.setWrapText( getInitParameter(PROPERTY_WRAP_TEXT) );
        }

        edText.setEditable(getFormMode().equals(VM_EDIT));

    }

}
