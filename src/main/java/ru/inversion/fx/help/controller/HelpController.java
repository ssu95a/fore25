package ru.inversion.fx.help.controller;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.help.facade.FXHelp;
import ru.inversion.fx.service.module.ModuleContext;
import ru.inversion.fx.service.module.ModuleService;
import ru.inversion.utils.S;

import java.util.ResourceBundle;

/**
 * Класс контроллер для отображения и редактирования справочной информации
 *
 * @author perov
 * @version 1.0.0
 */
public class HelpController extends JInvFXFormController {

    private final ResourceBundle bundle = ResourceBundle.getBundle("ru.inversion.fx.help.fxml.help");
    public static final String HELP_COLOR_TINT = "eee5cf";

    @FXML
    TabPane tabPane;

    private AbstractTab viewTab;
    private AbstractTab hotKeyTab;
    private AbstractTab editTab;
    private AbstractTab infoTab;
    private AbstractTab logTab;

    private TabState currentState;

    private String formName;
    private String constantPartOfTitle;
    private Class classController;

    String getFormName() {
        return formName;
    }

    void setFormName(String formName) {
        this.formName = formName;
    }

    // Перекрыто, чтобы не проверялась версионность хелпового контроллера
    protected void beforeInit() throws Exception {

        initFormStateDecorator();
        dataObject = configureDataObject();
        fxEntity = createFXEntity();
    }

    public Class getClassController() {
        return classController;
    }

    public void setClassController(Class classController) {
        this.classController = classController;
    }

    @Override
    protected void init() throws Exception {
        super.init();

        setFormName((String) getInitProperties().get(FXHelp.HELP_FORM_NAME_PROPERTY));
        setClassController((Class) getInitProperties().get(FXHelp.HELP_FORM_CONTROLLSER_CLASS));
        initConstantPartOfTitle();
        initComponent();
        initListener();
    }

    private void initComponent() {

//        tabPane.setStyle("-fx-base:f7e9c6; -fx-background-color:f7e9c6;");//

        viewTab = new ViewTab(bundle.getString("TITLE_VIEW"), this, getTaskContext(), bundle);
        hotKeyTab = new HotKeyTab(bundle.getString("HOTKEY"), this, bundle);
        editTab = new EditTab(bundle.getString("EDIT"), this, getTaskContext(), bundle);
        infoTab = new InfoTab("Info", this, bundle);
        logTab = new LogTab("Log", this, bundle);

        editTab.setViewContext(getViewContext());

        currentState = viewTab;

        tabPane.getTabs().add(viewTab);
        tabPane.getTabs().add(hotKeyTab);
        if (editTab.checkAccess(getTaskContext(), EditTab.ACCESS_EDIT)) {
            tabPane.getTabs().add(editTab);
            editTab.init();
        }
        tabPane.getTabs().add(infoTab);
        tabPane.getTabs().add(logTab);
        currentState.draw(getFormName());

    }

    private void initListener() {
        tabPane.getSelectionModel().selectedItemProperty()
                .addListener((ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) -> {
                    ((AbstractTab) getCurrentState()).lossSelect();
                    setCurrentState((TabState) newTab);
                    getCurrentState().draw(getFormName());
                });
    }

    TabState getViewTab() {
        return viewTab;
    }

    TabState getHotKeyTab() {
        return hotKeyTab;
    }

    TabState getCurrentState() {
        return currentState;
    }

    void setCurrentState(TabState currentState) {
        this.currentState = currentState;
    }

    TabState getEditTab() {
        return editTab;
    }

    @Override
    protected boolean onCancel() {
        ((EditTab) editTab).doSaveChangeBeforeChangeRow();
        return true;
    }

    @Override
    protected void onCloseWindow() {
        super.onCloseWindow();

        tabPane.getTabs().filtered(p -> p instanceof TabState).forEach(t -> {
            ((TabState) t).preDestroy();
        });
    }

    private void initConstantPartOfTitle() {

        StringBuilder sb = new StringBuilder((String) getInitProperties().get("FORM_NAME"));
        sb.append(" | ");
        try {
            ModuleContext context = ((ModuleService) BaseApp.APP().getAppService(ModuleService.SERVICE_ID)).getModuleContextOrCreate(classController);
            if (context != null && S.isNotNullOrEmpty(context.getManagerVersion())) {
                sb.append(context.getManagerVersion());
            }
        } catch (Throwable ex) {
            logger.error("Error help ", ex);
        }
        constantPartOfTitle = sb.toString();
    }

    public String getConstantPartOfTitle() {
        return constantPartOfTitle;
    }

}
