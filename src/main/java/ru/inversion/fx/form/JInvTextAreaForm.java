
package ru.inversion.fx.form;

import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.service.exteditor.ExtensionType;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;


/**
 *
 * @author perov
 */
public class JInvTextAreaForm {

    static final String PROPERTY_EXTENSION_TYPE = "ru.inversion.jinvtextareaform.extensiontype";
    static final String PROPERTY_TITLE = "ru.inversion.jinvtextareaform.title";
    static final String PROPERTY_TERMINAL_FONT = "ru.inversion.jinvtextareaform.terminalfont";
    static final String PROPERTY_WRAP_TEXT = "ru.inversion.jinvtextareaform.wraptext";

    public static void show(ViewContext vc,String text, ExtensionType type, String title,
                            AbstractBaseController.FormModeEnum mode, boolean terminalFont, boolean wrapText)
    {
        Map<String,Object> param =new HashMap<>();
        param.put(PROPERTY_EXTENSION_TYPE, type);
        param.put(PROPERTY_TITLE, title);
        param.put(PROPERTY_TERMINAL_FONT, terminalFont);
        param.put(PROPERTY_WRAP_TEXT, wrapText);
        FXFormLauncher launcher = new FXFormLauncher(BaseApp.APP().getCommonTaskContext(), vc, "ru/inversion/fx/form/fxml/JInvTextAreaForm.fxml");
                        launcher.modal(true)
                        .dataObject(text)
                        .dialogMode(mode)
                        .initProperties(param)
                        .bundle(ResourceBundle.getBundle("fore"))
                        .show();

    }
    public static void show(ViewContext vc,String text, ExtensionType type, String title,
            AbstractBaseController.FormModeEnum mode, boolean terminalFont)
    {
        show(vc, text, type, title, mode, terminalFont, true);
    }

    public static void show(ViewContext vc, String text, ExtensionType type){
        show(vc, text, type, null, AbstractBaseController.FormModeEnum.VM_SHOW, false);
    }

    public static void show(ViewContext vc, String text,ExtensionType type, AbstractBaseController.FormModeEnum mode){
        show(vc, text, type, null, mode, false);
    }

    public static void show(ViewContext vc, String text, ExtensionType type, AbstractBaseController.FormModeEnum mode, boolean terminalFont){
        show(vc, text, type, null, mode, terminalFont);
    }

}
