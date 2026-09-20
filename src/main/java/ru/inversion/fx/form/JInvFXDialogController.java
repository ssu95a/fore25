package ru.inversion.fx.form;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.mdi.JInvWindowMdi;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.U;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author ssu @
 */
public class JInvFXDialogController<T> extends JInvFXFormController<T> {

    private BiConsumer<DialogReturnEnum, JInvFXFormController> clb;

    /**
     *
     */
    @Deprecated
    public static enum DialogReturnEnum {
        RET_OK,
        RET_CANCEL,
        RET_MDI;

        static DialogReturnEnum fromFormReturn(FormReturnEnum formReturn) {
            switch (formReturn) {
                case RET_CANCEL:
                    return RET_CANCEL;
                case RET_OK:
                    return RET_OK;
                case RET_MDI:
                    return RET_MDI;
            }
            return null;
        }

        static FormReturnEnum toFormReturn(DialogReturnEnum dialogReturn) {
            switch (dialogReturn) {
                case RET_CANCEL:
                    return FormReturnEnum.RET_CANCEL;
                case RET_OK:
                    return FormReturnEnum.RET_OK;
                case RET_MDI:
                    return FormReturnEnum.RET_MDI;
            }
            return null;
        }
    }

    /**
     *
     */
    @Deprecated
    public enum DialogModeEnum {

        VM_NONE,
        VM_INS,
        VM_EDIT,
        VM_DEL,
        VM_CHOICE,
        VM_SHOW;

        public String getTitle() {
            if (g_baseBundle.containsKey(this.name())) {
                return g_baseBundle.getString(this.name());
            }
            return "<No title>";
        }
/*
        public int getDBOperation() {
            switch (this) {
                case VM_INS:
                    return AbstractWorkBase.CREATE;
                case VM_EDIT:
                    return AbstractWorkBase.UPDATE;
                case VM_DEL:
                    return AbstractWorkBase.DELETE;
            }
            return 0;
        }
*/
        public static DialogModeEnum fromFormMode(FormModeEnum mode) {

            switch (mode) {
                case VM_CHOICE:
                    return VM_CHOICE;
                case VM_DEL:
                    return VM_DEL;
                case VM_EDIT:
                    return VM_EDIT;
                case VM_INS:
                    return VM_INS;
                case VM_NONE:
                    return VM_NONE;
                case VM_SHOW:
                    return VM_SHOW;
            }
            return null;
        }

                public static FormModeEnum toFormMode(DialogModeEnum mode) {

            switch (mode) {
                case VM_CHOICE:
                    return FormModeEnum.VM_CHOICE;
                case VM_DEL:
                    return FormModeEnum.VM_DEL;
                case VM_EDIT:
                    return FormModeEnum.VM_EDIT;
                case VM_INS:
                    return FormModeEnum.VM_INS;
                case VM_NONE:
                    return FormModeEnum.VM_NONE;
                case VM_SHOW:
                    return FormModeEnum.VM_SHOW;
            }
            return null;
        }
    }
    /**
     *
     */

    @Deprecated
    protected DialogModeEnum dialogMode = DialogModeEnum.VM_NONE;

    @Deprecated
    private Property<DialogReturnEnum> returnPropery;

    /**
     * @return
     */
    public DialogModeEnum getDialogMode() {
        return dialogMode;
    }

    @Override
    public void initFormController(TaskContext tc, ViewContext vc, FormModeEnum formMode, T dataObject, Property<FormReturnEnum> returnProperty, Map<String, Object> initProperties, Consumer<ResultForm<T>> returnCallback, ViewContext parentViewContext) {

        super.initFormController(tc, vc, formMode, dataObject, returnProperty, initProperties, returnCallback, parentViewContext);

        DialogModeEnum dialogMode = null;
        if (formMode != null) {
            dialogMode = DialogModeEnum.fromFormMode(formMode);
        }

        Property<DialogReturnEnum> dialogReturnProperty = null;
        if (returnProperty != null) {
            dialogReturnProperty = new SimpleObjectProperty<>();
            dialogReturnProperty.setValue(DialogReturnEnum.fromFormReturn(returnProperty.getValue()));
        }

        BiConsumer<DialogReturnEnum, JInvFXFormController> dialogClb = null;
        if ( returnCallback != null) {

            dialogClb = (DialogReturnEnum t, JInvFXFormController u) -> {

                ResultForm result = new ResultForm();
                result.setController(u);
                result.setFormReturn(DialogReturnEnum.toFormReturn(t));
                result.setException(exception);


                returnCallback.accept(result);
            };
        }

        initDialogController(tc, vc, dialogMode, dataObject, dialogReturnProperty, initProperties, dialogClb);

    }

    public void initDialogController(TaskContext tc, ViewContext vc, DialogModeEnum dialogMode, T dataObject, Property<DialogReturnEnum> returnProperty,
        Map<String, Object> initProperties, BiConsumer<DialogReturnEnum, JInvFXFormController> clb) {

        try {
            this.clb = clb;
            this.dataObject = dataObject;
            this.dialogMode = dialogMode;
            this.returnPropery = U.nvl(returnProperty, new SimpleObjectProperty<DialogReturnEnum>());

        } catch (Throwable th) {
            JInvErrorService.handleException(vc.getStage(), th);
        }
    }

    @Deprecated
    public static <T> DialogReturnEnum doModal(
        TaskContext tc,
        ViewContext parentViewContext,
        T dataObject,
        String fxmlPath,
        ResourceBundle bundle,
        JInvFXDialogController.DialogModeEnum dialogMode,
        Map<String, Object> initProperties,
        BiConsumer<DialogReturnEnum, JInvFXDialogController> clb
    ) {

        return doModal(tc, parentViewContext, dataObject, fxmlPath, true, bundle, dialogMode, initProperties, clb);

    }

    @Deprecated
    public static <T> DialogReturnEnum doModal(
        TaskContext tc,
        ViewContext parentViewContext,
        T dataObject,
        String fxmlPath,
        boolean modal,
        ResourceBundle bundle,
        JInvFXDialogController.DialogModeEnum dialogMode,
        Map<String, Object> initProperties,
        BiConsumer<DialogReturnEnum, JInvFXDialogController> clb
    ) {

        JInvFXFormController.<T>doModal(tc, parentViewContext, dataObject, fxmlPath, modal, bundle, DialogModeEnum.toFormMode(dialogMode), (FormReturnEnum t, JInvFXFormController<T> u) -> {

            if (clb != null && u instanceof JInvFXDialogController) {
                clb.accept(DialogReturnEnum.fromFormReturn(t), (JInvFXDialogController) u);
            }
        }, initProperties);

        return DialogReturnEnum.RET_CANCEL;

    }

    @Deprecated
    public static <T> DialogReturnEnum doModal( TaskContext tc, javafx.stage.Stage parentStage, JInvWindowMdi parentWindow, T dataObject, String fxmlPath,
                                                ResourceBundle bundle, DialogModeEnum dialogMode, Map<String, Object> initProperties, BiConsumer<DialogReturnEnum, JInvFXDialogController> clb) {

        return doModal(tc, new ViewContext(parentStage), dataObject, fxmlPath, bundle, dialogMode, initProperties, clb);
    }

    /**
     *
     */
    @Deprecated
    public static <T> DialogReturnEnum doModal( TaskContext tc, javafx.stage.Stage parentStage, T dataObject, String fxmlPath, ResourceBundle bundle, DialogModeEnum dialogMode, Map<String, Object> initProperties) {
        return doModal(tc, parentStage, null, dataObject, fxmlPath,
            bundle, dialogMode, initProperties, null);
    }

    /**
     *
     */
    @Deprecated
    public static <T> void doModal(TaskContext tc, ViewContext parentViewContext, T dataObject, Class cl, DialogModeEnum dialogMode, Map<String, Object> initProperties, BiConsumer<DialogReturnEnum, JInvFXDialogController> clb) {
        doModal(tc, parentViewContext, dataObject, getSceneFileName(cl), getResourceBundle(cl), dialogMode, initProperties, clb);
    }

}
