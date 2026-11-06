package ru.inversion.fx.form;

import javafx.beans.property.Property;
import ru.inversion.fx.form.mdi.JInvWindowMdi;
import ru.inversion.tc.TaskContext;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 *
 * @author ssu @
 */
public class JInvFXBrowserController extends JInvFXFormController<Object>{

//    private BiConsumer<DialogReturnEnum, JInvFXFormController> clb;
//    private boolean modal;

    @Override
    protected void onCloseWindow() {

        logger.trace("onCloseWindow");
//        if (clb != null) {
//            clb.accept(DialogReturnEnum.RET_OK, this);
//        }
    }

    @Override
    protected void initFormController(TaskContext tc, ViewContext vc, FormModeEnum dialogMode, Object dataObject, Property<FormReturnEnum> returnProperty, Map<String, Object> properties, Consumer<ResultForm<Object>> returnCallback, ViewContext parentViewContext) {
        super.initFormController(tc, vc, dialogMode, dataObject, returnProperty, properties, returnCallback, parentViewContext); //To change body of generated methods, choose Tools | Templates.
    }



//    @Override
//    public void initFormController(TaskContext tc, ViewContext vc, JInvFXFormController.FormModeEnum dialogMode, Object dataObject, Property<FormReturnEnum> returnProperty,
//        Map<String, Object> initProperties, Consumer<ResultForm> clb, ViewContext parentViewContext) {
//
//        super.initFormController(tc, vc, dialogMode, dataObject, returnProperty, initProperties, clb, parentViewContext);
//
////        BiConsumer<DialogReturnEnum, JInvFXFormController> browserClb = null;
////
////        if(clb!=null){
////            browserClb = (DialogReturnEnum t, JInvFXFormController u) -> {
////                clb.accept(DialogReturnEnum.toFormReturn(t), u);
////            };
////        }
//
////        initBrowserController(tc, vc, initProperties, browserClb);
//    }

//    /**
//     *
//     */
//    public void initBrowserController(TaskContext tc, ViewContext vc, Map<String, Object> initProperties,
//        BiConsumer<DialogReturnEnum, JInvFXFormController> clb) {
//
//        try {
//            this.clb = clb;
//        } catch (Throwable th) {
//            JInvErrorService.handleException(null, th);
//        }
//    }

    @Deprecated
    public static void show(TaskContext tc, ViewContext parentViewContext, String fxmlPath,
        boolean modal, ResourceBundle bundle, Map<String, Object> initProperties, Consumer<JInvFXBrowserController> clb) {

        JInvFXFormController.show(tc, parentViewContext, fxmlPath, bundle, (FormReturnEnum t, JInvFXFormController<Object> controller) -> {
            if (clb != null && controller instanceof JInvFXBrowserController) {
                clb.accept((JInvFXBrowserController) controller);
            }
        }, initProperties, modal);
    }

    @Deprecated
    public static void show( TaskContext tc, javafx.stage.Stage parentStage, JInvWindowMdi parentWindow, String fxmlPath,
                             boolean modal, ResourceBundle bundle, Map<String, Object> initProperties, Consumer<JInvFXBrowserController> clb) {

        show(tc, new ViewContext(parentStage), fxmlPath, modal, bundle, initProperties, clb);
    }

    /**
     *
     */
    @Deprecated
    public static void show( TaskContext tc, javafx.stage.Stage parentStage, String fxmlPath, ResourceBundle bundle, Map<String, Object> initProperties) {
        show(tc, parentStage, null, fxmlPath, false, bundle, initProperties, null);
    }

    @Deprecated
    public static void show( TaskContext tc, javafx.stage.Stage parentStage, String fxmlPath, boolean modal, ResourceBundle bundle, Map<String, Object> initProperties) {
        show(tc, parentStage, null, fxmlPath, modal, bundle, initProperties, null);
    }

    /**
     *
     */
    @Deprecated
    public static void show(TaskContext tc, ViewContext parentViewContext, Class cl, boolean modal, Map<String, Object> initProperties, Consumer<JInvFXBrowserController> clb) {
        show(tc, parentViewContext, getSceneFileName(cl), modal, getResourceBundle(cl), initProperties, clb);
    }

}
