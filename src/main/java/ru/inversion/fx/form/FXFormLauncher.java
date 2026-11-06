/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import static ru.inversion.fx.form.AbstractBaseController.getResourceBundle;
import static ru.inversion.fx.form.AbstractBaseController.getSceneFileName;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.tc.TaskContext;

/**
 * Класс для старта формы.
 *
 * @author antonovdi
 */
public class FXFormLauncher<T> {

    protected static final ResourceBundle g_foreBundle = ResourceBundle.getBundle("fore");

    //Обязательные параметры
    final protected TaskContext tc;
    final protected ViewContext vc;

    protected T dataObject;
    protected String  fxmlPath;
    protected boolean modal;
    protected boolean forceOwnerTaskContext;
    protected ResourceBundle bundle;
    protected JInvFXFormController.FormModeEnum dialogMode = JInvFXFormController.FormModeEnum.VM_NONE;

    protected Map<String, Object> initProperties;

    /** самый первый */
    protected BiConsumer<JInvFXFormController.FormReturnEnum, JInvFXFormController> clb;

    /** усовершенствованный */
    protected Consumer<ResultForm<T>> closeCallback;

    /** */
    protected Consumer<JInvFXFormController<T>> controllerCallback;

    protected boolean disableSavedDimensions;
    protected boolean disableHandleInitException;
    private Consumer<JInvTable<?>> contextMenuInit;
    private Validator<? extends JInvFXFormController<? super T>>[] validators;

    /** */
    public FXFormLauncher(TaskContext tc, ViewContext vc ) {
        this.tc = tc;
        this.vc = vc;
    }

    /** */
    public FXFormLauncher(TaskContext tc, ViewContext vc, String fxmlPath) {
        this( tc, vc );
        this.fxmlPath = fxmlPath;
    }

    /** */
    public FXFormLauncher(TaskContext tc, ViewContext vc, Class<? extends JInvFXFormController<? super T>> cl) {
        this( tc, vc, cl, getResourceBundle(cl) );
    }

    /**
     *
     */
    public FXFormLauncher(TaskContext tc, ViewContext vc, Class<? extends JInvFXFormController<? super T>> cl, ResourceBundle rb) {
        this( tc, vc, getSceneFileName(cl) );
        this.bundle = rb;
    }

    /** */
    public FXFormLauncher (JInvFXFormController<?> parentController , Class<? extends JInvFXFormController<? super T>> controllerClass ) {
        this( parentController.getTaskContext(),
              parentController.getViewContext(),
              controllerClass
        );
    }


    /** */
    public T getDataObject() {
        return dataObject;
    }

    /** */
    public FXFormLauncher<T> dataObject(T dataObject) {
        this.dataObject = dataObject;
        return this;
    }

    /**
        Насильно заставляет контроллер считать переданный TaskContext своим и
        соответственно закрывать оный после освобождения ресурсов
     */
    public FXFormLauncher<T> forceOwnerTaskContext( ) {
        forceOwnerTaskContext = true;
        return this;
    }

    /** */
    public void setDataObject(T dataObject) {
        this.dataObject = dataObject;
    }

    public Consumer<ResultForm<T>> getCloseCallback() {
        return closeCallback;
    }
    public void setCloseCallback(Consumer<ResultForm<T>> callback) {
        this.closeCallback = callback;
    }

    /**
     * Устанавливает колбек вызываемый при завершении работы формы
     * <p>
     * @param
     *      callback
     *
     * @return
     */
    public FXFormLauncher<T> closeCallback( Consumer<ResultForm<T>> callback ) {
        this.closeCallback = callback;
        return this;
    }

    /** */
    public boolean isModal() {
        return modal;
    }


    /** */
    public FXFormLauncher<T> modal(boolean modal) {
        this.modal = modal;
        return this;
    }

    /** */
    public void setModal(boolean modal) {
        this.modal = modal;
    }

    /**
     * Использовать ли механизм сохранения/загрузки размеров формы
     */
    public boolean isDisableSavedDimensions() {
        return disableSavedDimensions;
    }

    /**
     * Использовать ли механизм сохранения/загрузки размеров формы
     */
    public FXFormLauncher<T> disableSavedDimensions(boolean disableSavedDimensions) {
        this.disableSavedDimensions = disableSavedDimensions;
        return this;
    }

    /**
     * Использовать ли механизм сохранения/загрузки размеров формы
     */
    public void setDisableSavedDimensions(boolean disableSavedDimensions) {
        this.disableSavedDimensions = disableSavedDimensions;
    }

    /** */
    public ResourceBundle getBundle() {
        return bundle;
    }

    /** */
    public FXFormLauncher<T> bundle( ResourceBundle bundle ) {
        this.bundle = bundle;
        return this;
    }

    /**
     *
     * @param bundle
     */
    public void setBundle(ResourceBundle bundle) {
        this.bundle = bundle;
    }

    /**
     *
     * @return
     */
    public JInvFXFormController.FormModeEnum getDialogMode() {
        return dialogMode;
    }

    /**
     *
     * @param dialogMode
     * @return
     */
    public FXFormLauncher<T> dialogMode(JInvFXFormController.FormModeEnum dialogMode) {
        this.dialogMode = dialogMode;
        return this;
    }

    /**
     *
     * @param dialogMode
     */
    public void setDialogMode(JInvFXFormController.FormModeEnum dialogMode) {
        this.dialogMode = dialogMode;
    }

    /**
     *
     * @return
     */
    public Map<String, Object> getInitProperties() {
        return initProperties;
    }

    /**
     *
     * @param initProperties
     * @return
     */
    public FXFormLauncher<T> initProperties(Map<String, Object> initProperties) {
        this.initProperties = initProperties;
        return this;
    }

    /**
     *
     * @param initProperties
     */
    public void setInitProperties(Map<String, Object> initProperties) {
        this.initProperties = initProperties;
    }


    public Consumer<JInvTable<?>> getContextMenuInit() {
        return contextMenuInit;
    }

    /**
     Своя лямбда для инициализации контекстного меню в таблицах
     */
    public FXFormLauncher<T> contextMenuInit(Consumer<JInvTable<?>> contextMenuInit) {
        this.contextMenuInit = contextMenuInit;
        return this;
    }

    /**
     Своя лямбда для инициализации контекстного меню в таблицах
     */
    public void setContextMenuInit(Consumer<JInvTable<?>> contextMenuInit ) {
        this.contextMenuInit = contextMenuInit;
    }


    public Validator<? extends JInvFXFormController<? super T>>[] getFormValidators() {
        return validators;
    }

    /**
     Добавить свои валидаторы формы для запускаемого контроллера
     */
    @SafeVarargs
    public final FXFormLauncher<T> formValidators( Validator<? extends JInvFXFormController<? super T>>... validators ) {
        this.validators = validators;
        return this;
    }

    /**
     Добавить свои валидаторы формы для запускаемого контроллера
     */
    @SafeVarargs
    public final void setFormValidators( Validator<? extends JInvFXFormController<? super T>>... validators ) {
        this.validators = validators;
    }

    /**
     *
     * @return
     */
    @Deprecated
    public BiConsumer<JInvFXFormController.FormReturnEnum, JInvFXFormController> getClb() {
        return clb;
    }

    /**
     *
     * @param clb
     * @return
     */
    public FXFormLauncher<T> clb(BiConsumer<JInvFXFormController.FormReturnEnum, JInvFXFormController> clb) {
        this.clb = clb;
        return this;
    }

    /**
     *
     * @param clb
     */
    public void setClb(BiConsumer<JInvFXFormController.FormReturnEnum, JInvFXFormController> clb) {
        this.clb = clb;
    }

    /**
     *
     * @param clb
     * @return
     */
    public FXFormLauncher<T> callback(BiConsumer<JInvFXFormController.FormReturnEnum, JInvFXFormController<T>> clb) {
        this.clb = (BiConsumer) clb;
        return this;
    }

    /**
     *
     * @return
     */
    public boolean isDisabledHandleInitException() {
        return disableHandleInitException;
    }

    /**
     *
     * @param disableHandleInitException
     */
    public void setDisableHandleInitException(boolean disableHandleInitException) {
        this.disableHandleInitException = disableHandleInitException;
    }

    /**
     * Выключает обработку исключений при инициализации
     *
     * @param disableHandleInitException
     */
    public FXFormLauncher<T> disableHandleInitException(boolean disableHandleInitException) {
        this.disableHandleInitException = disableHandleInitException;
        return this;
    }

    /**
     *
     * @return
     */
    public Consumer<JInvFXFormController<T>> getControllerCallback() {
        return controllerCallback;
    }

    /**
     *
     * @param controllerCallback
     */
    public void setControllerCallback(Consumer<JInvFXFormController<T>> controllerCallback) {
        this.controllerCallback = controllerCallback;
    }

    /**
     *
     * @param controllerCallback
     */
    public FXFormLauncher<T> controllerCallback(Consumer<JInvFXFormController<T>> controllerCallback) {
        this.controllerCallback = controllerCallback;
        return this;
    }

    /**
     * Метод для старта формы
     */
    public void show() {

        Map<String, Object> innerProperties = new HashMap<>();

        if( disableSavedDimensions )
            innerProperties.put(AbstractBaseController.PROPERTY_DISABLE_SAVED_DIMENSIONS, true);

        if( disableHandleInitException )
            innerProperties.put(AbstractBaseController.PROPERTY_DISABLE_HANDLE_INIT_EXCEPTION, true);

        if ( forceOwnerTaskContext )
            innerProperties.put(AbstractBaseController.PROPERTY_FORCE_OWNER_TASK_CONTEXT, true);

        if ( contextMenuInit != null )
            innerProperties.put(AbstractBaseController.PROPERTY_CONTEXT_MENU_INIT, contextMenuInit );

        if ( validators != null )
            innerProperties.put(AbstractBaseController.PROPERTY_FORM_VALIDATORS, validators );

        showInternal( innerProperties );
    }

    /** */
    public void doModal() {

        modal(true);

        show();
    }

    /** */
    protected void showInternal( Map<String, Object> innerProperties ) {

        if( modal && getControllerCallback() != null ) {
            throw new RuntimeException( g_foreBundle.getString("ERROR_LAUNCH_FORM_AND_GET_CONTROLLER_IN_MODAL_REGIME") );
        }

        if( clb != null ) {
            JInvFXFormController.<T>internalShow(tc, vc, dataObject, fxmlPath, modal, bundle, dialogMode, initProperties, innerProperties, clb, controllerCallback);
        } else {
            JInvFXFormController.<T>internalShow(tc, vc, dataObject, fxmlPath, modal, bundle, dialogMode, initProperties, innerProperties, closeCallback, controllerCallback);
        }
    }

    /**
     * Метод для старта формы возвращающий контроллер
     */
    public void showAndReturnController(Consumer<JInvFXFormController<T>> controllerCallback) {
        setControllerCallback(controllerCallback);
        show();
    }

}
