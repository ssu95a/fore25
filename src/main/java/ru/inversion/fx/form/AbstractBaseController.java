package ru.inversion.fx.form;

import com.sun.javafx.scene.control.skin.TableColumnHeader;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableMap;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.*;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.db.expr.SQLExpressionFactory;
import ru.inversion.db.rs.RSUtils;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.AppKiller;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.frame.JInvMainFrame;
import ru.inversion.fx.app.frame.JInvMainFrame.JInvFrameMode;
import ru.inversion.fx.app.frame.menu.JInvMenuManager;
import ru.inversion.fx.app.login.LoginManager;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.action.*;
import ru.inversion.fx.form.action.decorator.ProgressFormStateDecorator;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.filter.impl.FilterWork;
import ru.inversion.fx.form.controls.progress.ProgressCallback;
import ru.inversion.fx.form.controls.progress.ProgressTaskExecutor;
import ru.inversion.fx.form.controls.sortbutton.JInvSortButton;
import ru.inversion.fx.form.controls.table.TableSettingItemEnum;
import ru.inversion.fx.form.controls.table.TripleBoolValueEnum;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableColumnEx;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableEx;
import ru.inversion.fx.form.dbtrace.DBTraceDialog;
import ru.inversion.fx.form.mdi.JInvWindowMdi;
import ru.inversion.fx.form.valid.ButtonValidationListener;
import ru.inversion.fx.form.valid.ValidMan;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.fx.form.valid.validators.LovValidator;
import ru.inversion.fx.service.module.ModuleService;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.*;
import ru.inversion.utils.converter.TypeConverter;
import ru.inversion.xxi.MethodTimeCheck;
import ru.inversion.xxi.impl.XXIMenuLoader;

import javax.persistence.Entity;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_ENABLE_FILTER;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.*;
import static ru.inversion.fx.form.Binder.getInternalValidatorsFromControl;
import static ru.inversion.fx.form.controls.filter.impl.FilterManager.C_MAX_FIELD_BD;

/**
 * Основной класс для работы с формами.
 *
 * @author ssu
 */
public class AbstractBaseController<T> implements Initializable, IFormStateListener {
    /**
     * Объект для доступа к строковым локализованным ресурсам фреймворка
     */
    protected static final ResourceBundle g_baseBundle = ResourceBundle.getBundle("fore");

    /**
     * Коллекция запущенных экземпляров контроллеров с аннотацией Singleton
     */
    final private static Map<Class<?>, AbstractBaseController<?>> g_controllerInstanceMap = Collections.synchronizedMap(new HashMap<>());

    /**
     * Название css класса для кнопки "Отмена"
     */
    public static final String CSS_CANCEL_BUTTON_CLASS = "inversion_cancel_button";

    /**
     * Название css класса для кнопки "OK"
     */
    public static final String CSS_OK_BUTTON_CLASS = "inversion_ok_button";

    /**
     * Максимально допустимая высота формы в пикселях в рамках дефолтного размера шрифта.
     * <p>
     * При большем значении {@link Region#getPrefHeight()}   корневой панели будет показано предупреждение о недопустимости подобного размера
     */
    public static final double MAX_HEIGHT = 700;

    /**
     * Максимально допустимая ширина формы в пикселях в рамках дефолтного размера шрифта.
     * <p>
     * При большем значении {@link Region#getPrefWidth} корневой панели будет показано предупреждение о недопустимости подобного размера
     */
    public static final double MAX_WIDTH = 1200;

    /**
     * Флаг, что необходимо выбрасывать исключение, если не найдены строки в ресурсах.
     * <p>
     * если {@code false} то не найденная по ключу строка подменяется самим ключом.
     */
    public static boolean g_throwOnMissingResources = false;

    /**
     * Константы выполнения ф-ции populateDataSet.
     */
    final public static int
            NO_EXECUTE = 0, // Не выполнять
            EXECUTE = 1, // Выполнить
            EXECUTE_AND_FETCH = 2; // Выполнить и прочитать все записи

    /**
     * Свойства добавляемые в компоненты
     */
    public static final String PROPERTY_CONTROLLER = "ru.inversioin.fx.controller";
    public static final String PROPERTY_NAME = "ru.inversion.form_name";
    public static final String PROPERTY_NAME_FOR_FILTER = "ru.inversion.name_for_filter";
    public static final String PROPERTY_DISABLE_SAVED_DIMENSIONS = "ru.inversion.disable_saved_dimensions";
    public static final String PROPERTY_DISABLE_HANDLE_INIT_EXCEPTION = "ru.inversion.disable_handle_init_exception";
    public static final String PROPERTY_FORCE_OWNER_TASK_CONTEXT = "ru.inversion.force_owner_task_context";
    public static final String PROPERTY_CONTEXT_MENU_INIT = "ru.inversion.context_menu_init";
    public static final String PROPERTY_FORM_VALIDATORS = "ru.inversion.form_validators";

    final public static Object EMPTY_OBJECT = new Object();

    /**
     * Объект, инкапсулирующий в себе соединение к базе данных
     */
    protected TaskContext taskContext;

    public boolean isOwnerTaskContext() {
        return ownerTaskContext;
    }

    /**
     * Признак является ли данный контроллер таким, где {@link TaskContext} был создан
     */
    private boolean ownerTaskContext = false;

    /**
     * Объект, инкапсулирующий в себе контейнер окна. Будь то {@link javafx.stage.Stage} или {@link JInvWindowMdi}
     */
    protected ViewContext viewContext;

    /**
     * Объект, инкапсулирующий в себе контейнер родительского. Будь то {@link javafx.stage.Stage} или {@link JInvWindowMdi}
     */
    protected ViewContext parentviewContext;

    /**
     * Объект для доступа к строковым локализованным ресурсам для текущей формы
     */
    protected ResourceBundle bundle = g_baseBundle;

    /**
     * Pojo-Обьект для заполнения на форме.
     * Если он анотирован {@link Entity}, то будет происходить генерация прокси обьекта {@link IFXEntity}
     * для автоматического связывания значения в контролах.
     */
    protected T dataObject = null;

    /**
     * Обьект, при наличии которого происходит поиск всех контролов на форме и осуществление
     * двухстороннего связывания значения контрола и поля в этом объекте.
     */
    protected IFXEntity<T> fxEntity;

    /**
     * Режим старта форма
     */
    protected FormModeEnum formMode = FormModeEnum.VM_NONE;

    /**
     * Результат завершения формы
     */
    protected Property<FormReturnEnum> returnProperty = new SimpleObjectProperty<>(this, "returnProperty", FormReturnEnum.RET_CANCEL);
    @Deprecated
    final protected Property<FormReturnEnum> returnPropery = new SimpleObjectProperty<>(FormReturnEnum.RET_CANCEL);

    /**
     * Сущность для валидации значения контролов при смене фокуса и нажатии на кнопку OK
     */
    protected ValidMan validMan;

    /**
     * Интерфейс обратного вызова, который вызывается при завершении работы формы
     */
    protected Consumer<ResultForm<T>> returnCallback;

    /**
     * Карта параметров, приходящая при старте формы извне
     */
    protected Map<String, Object> initProperties;

    /**
     * Свойство заголовка окна
     */
    private final StringProperty titleProperty = new SimpleStringProperty();

    /**
     * Название контроллера.
     */
    private String name = getClass().getName();

    /**
     * Признак необходимости удалить размеры при выходе
     */
    private boolean deleteDimensions = false;

    /**
     * Признак неиспользования сохраненных размеров
     */
    protected boolean disableSavedDimensions;

    /**
     * Признак обрабатывать ли исключение при инициализации, либо передавать дальше
     */
    protected boolean disableHandleInitException;

    /**
     * Насильно заставляет контроллер считать переданный таскконтекст своим и соответственно закрывать оный после освобождения ресурсов
     */
    protected boolean forceOwnerTaskContext;

    /**
     * Состояние формы. См. {@link StateEnum}.
     * Применяется при отображении длинных операций
     */
    protected ObjectProperty<StateEnum> activeState = new SimpleObjectProperty<>(StateEnum.ACTIVE);
    protected StringProperty stateText = new SimpleStringProperty( S.EMPTY_STRING );


    protected ObjectProperty<LifeCycleStateEnum> lifeCycleState = new SimpleObjectProperty<>(LifeCycleStateEnum.START);

    /**
     * Логгер для записи служебной информации
     */
    protected static Logger logger = LoggerFactory.getLogger(AbstractBaseController.class.getCanonicalName());

    /**
     * Объект, реализующий отображение формы при различных значениях activeState
     *
     * @see #activeState
     */
    protected ProgressFormStateDecorator stateDecorator;

    /**
     * Контексное меню формы
     */
    final private ContextMenu contextMenu = new ContextMenu();

    /**
     * Набор действий, держащих форму в состоянии WAIT (с колесом)
     */
    private final Set<JInvAction> waitActions = Collections.synchronizedSet(new HashSet<>());

    /**
     * Флаг для включения функционала работы IChoiceControl для выбора какого-либо значения
     */
    private boolean choiceEnabled = true;


    /**
     * Исключение, которое происходит в процессе работы контроллера
     */
    protected Throwable exception;

    /**
     Своя лямбда для инициализации контекстного меню в таблицах
     */
    private Consumer<JInvTable<?>> contextMenuInit;
    void setContextMenuInit( final Consumer<JInvTable<?>> contextMenuInit ) {
        this.contextMenuInit = contextMenuInit;
    }

    /**
     Валидаторы формы из FXFormLauncher'а
     */
    private Validator[] externalValidators;
    final void setExternalFormValidators( final Validator... validators ) {
        if ( validMan == null ){
            this.externalValidators = validators;
        } else {
            logger.error( "setExternalFormValidators: validMan is NOT null, cannot set external form validators" );
        }
    }

    /**
     * Перечисление результатов завершения формы
     */
    public enum FormReturnEnum {
        RET_OK,
        RET_CANCEL,
        RET_MDI;
    }

    /**
     *
     */
    public enum FormModeEnum {

        VM_NONE, VM_INS, VM_EDIT, VM_DEL, VM_CHOICE, VM_CHOICE_MARKED, // Режим
        // множественного
        // выбора
        // значения
        // из
        // формы
        // по
        // пометке
        VM_SHOW;

        public String getTitle() {
            if (g_baseBundle.containsKey(this.name())) {
                return g_baseBundle.getString(this.name());
            }
            return "<No title>";
        }
    }

    /**
     *
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (resources != null && resources != g_baseBundle) {
            bundle = resources;
        }
        returnPropery.bindBidirectional(returnProperty);

        lifeCycleState.addListener(new ChangeListener< LifeCycleStateEnum >() {

            private boolean wasSet = false;

            @Override
            public void changed( ObservableValue< ? extends LifeCycleStateEnum > observable, LifeCycleStateEnum oldValue, LifeCycleStateEnum newValue ) {
                switch( newValue ) {
                    case BEFORE_INIT:
                    case INIT:
                    case AFTER_INIT:
                    {
                        if( !wasSet )
                        {
                            if( taskContext != null ) {
                                wasSet = true;
                                assignOraSessionAction();
                            }
                        }
                    }
                    break;
                    case ON_CLOSE:
                    {
                        if( wasSet )
                            getTaskContext().restoreSessionTitle();
                    }
                    break;
                }
            }
        });

    }

    @Deprecated
    public static void show(TaskContext tc, ViewContext parentViewContext, String fxmlPath, ResourceBundle bundle,
                            BiConsumer<FormReturnEnum, JInvFXFormController<Object>> clb, Map<String, Object> initProperties) {

        show(tc, parentViewContext, fxmlPath, bundle, clb, initProperties, false);
    }

    @Deprecated
    public static void show(TaskContext tc, ViewContext parentViewContext, String fxmlPath, ResourceBundle bundle,
                            BiConsumer<FormReturnEnum, JInvFXFormController<Object>> clb, Map<String, Object> initProperties,
                            boolean modal) {

        internalShow(tc, parentViewContext, null, fxmlPath, modal, bundle, null, initProperties, clb);
    }

    @Deprecated
    public static void show(TaskContext tc, ViewContext parentViewContext, Class cl,
                            BiConsumer<FormReturnEnum, JInvFXFormController<Object>> clb, Map<String, Object> initProperties,
                            boolean modal) {

        show(tc, parentViewContext, getSceneFileName(cl), getResourceBundle(cl), clb, initProperties, modal);
    }

    @Deprecated
    public static <T> void show(TaskContext tc, ViewContext parentViewContext, T dataObject, Class cl,
                                FormModeEnum dialogMode, BiConsumer<FormReturnEnum, JInvFXFormController<T>> clb,
                                Map<String, Object> initProperties, boolean modal) {

        internalShow(tc, parentViewContext, dataObject, getSceneFileName(cl), modal, getResourceBundle(cl), dialogMode,
                initProperties, clb);

    }

    @Deprecated
    public static <T> void doModal(TaskContext tc, ViewContext parentViewContext, T dataObject, String fxmlPath,
                                   boolean modal, ResourceBundle bundle, FormModeEnum dialogMode,
                                   BiConsumer<FormReturnEnum, JInvFXFormController<T>> clb, Map<String, Object> initProperties) {
        internalShow(tc, parentViewContext, dataObject, fxmlPath, modal, bundle, dialogMode, initProperties, clb);
    }

    @Deprecated
    public static <T> void doModal(TaskContext tc, ViewContext parentViewContext, T dataObject, String fxmlPath,
                                   ResourceBundle bundle, FormModeEnum dialogMode, BiConsumer<FormReturnEnum, JInvFXFormController<T>> clb,
                                   Map<String, Object> initProperties) {
        internalShow(tc, parentViewContext, dataObject, fxmlPath, true, bundle, dialogMode, initProperties, clb);
    }

    @Deprecated
    static <T> void internalShow(TaskContext tc, ViewContext parentViewContext, T dataObject, String fxmlPath,
                                 boolean modal, ResourceBundle bundle, FormModeEnum dialogMode, Map<String, Object> initProperties,
                                 BiConsumer<FormReturnEnum, JInvFXFormController<T>> clb) {

        Consumer callbackReturn = (Consumer<ResultForm<T>>) (ResultForm<T> t) -> {
            if (t != null) {
                clb.accept(t.getFormReturn(), t.getController());
            }
        };

        internalShow(tc, parentViewContext, dataObject, fxmlPath, modal, bundle, dialogMode, initProperties, null,
                callbackReturn, null);
    }

    static <T> void internalShow(TaskContext tc, final ViewContext parentVc, T dataObject, String fxmlPath,
                                 boolean modal, ResourceBundle bundle, FormModeEnum dialogMode, Map<String, Object> initProperties,
                                 Map<String, Object> innerProperties,
                                 BiConsumer<FormReturnEnum, JInvFXFormController> clb,
                                 Consumer<JInvFXFormController<T>> controllerCallback) {

        Consumer callbackReturn = (Consumer<ResultForm<T>>) (ResultForm<T> t) -> {
            if (t != null) {
                clb.accept(t.getFormReturn(), t.getController());
            }
        };

        internalShow(tc, parentVc, dataObject, fxmlPath, modal, bundle, dialogMode, initProperties, innerProperties, callbackReturn,
                controllerCallback);

    }

    static <T> void internalShow(TaskContext tc,
                                 final ViewContext parentVc,
                                 T dataObject,
                                 String fxmlPath,
                                 boolean modal,
                                 ResourceBundle bundle,
                                 FormModeEnum dialogMode,
                                 Map<String, Object> initProperties,
                                 Map<String, Object> innerProperties,
                                 Consumer<ResultForm<T>> returnCallback,
                                 Consumer<JInvFXFormController<T>> controllerCallback) {

        BaseApp.APP().reportGuiLaunch();

        Platform.runLater(() -> {

            final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(), "internalShow");

            timeCheck.fixTime("INTERNAL_SHOW_1");

            ViewContext parentViewContext = parentVc;

            if (parentViewContext == null) {
                parentViewContext = new ViewContext(null, null);
            }

            JInvFXFormController controller = null;

            try {
                Property<FormReturnEnum> resultProperty = new SimpleObjectProperty<>(FormReturnEnum.RET_CANCEL);

                FXMLLoader loader = new FXMLLoader();
                loader.setResources(new ResourceBundleWrapper(bundle));
                loader.setLocation(JInvFXFormController.class.getClassLoader().getResource(fxmlPath));
                Region pane = loader.load();

                JInvFrameMode frameMode = BaseApp.APP().getViewPrefService().getFrameMode();

                javafx.stage.Stage showStage = null;
//                JInvWindowMdi showWindow = null;
//
//                Optional< JInvWindowMdi > windowMdi;
//                if ( frameMode == JInvFrameMode.MDI ){
//                    windowMdi = tryGetShowWindow( parentViewContext, pane, modal );
//                    if ( windowMdi.isPresent() ){
//                        showWindow = windowMdi.get();
//                    } else {
//                        frameMode = JInvFrameMode.SDI;
//                    }
//                }
//
//                if ( frameMode == JInvFrameMode.SDI ) {
//                    showStage = getShowStage(parentViewContext, pane, modal);
//                }

                showStage = getShowStage( parentViewContext, pane, modal );

                timeCheck.fixTime("INTERNAL_SHOW_2");

                // Проверка что допускается показывать только один экземляр контроллера
                controller = getController(loader, initProperties, innerProperties, fxmlPath);
                {
                    Class clazz = controller.getClass();

                    if (clazz.isAnnotationPresent(SingletonInstance.class)) {

                        synchronized (controller) {

                            AbstractBaseController<?> c = g_controllerInstanceMap.get(clazz);

                            if (c != null) {
                                javafx.stage.Stage stage = c.getViewContext().getStage();
                                if (stage != null)
                                    stage.toFront();
                                return;
                            } else {
                                g_controllerInstanceMap.put(clazz, controller);
                            }
                        } // end if
                    }
                }
                timeCheck.fixTime("INTERNAL_SHOW_3");
                initPane(pane, controller);
                ViewContext vc = new ViewContext(showStage);

                controller.initFormController(tc, vc, dialogMode, dataObject, resultProperty, initProperties, returnCallback, parentViewContext);

                if( controllerCallback != null )
                    controllerCallback.accept(controller);

                /* ***Для JInvDesktop*** */
                // JDESK-20
                final Boolean minimizeFx = Boolean.valueOf(System.getProperty("fx_minimize_on_start", "false"));
                if (minimizeFx != null && minimizeFx) {
                    showStage.setIconified(true);
                    System.setProperty("fx_minimize_on_start", "false"); // Чтобы больше не сворачивало controller'ы
                }
                // Для передачи иконки из одного ViewContext'а в другой
                if (parentViewContext.getIcon() != null) {
                    vc.setIcon(parentViewContext.getIcon());
                    Scene scene = new Scene(new StackPane(parentViewContext.getIcon()));
                    SnapshotParameters sp = new SnapshotParameters();
                    sp.setFill(Color.TRANSPARENT);
                    final WritableImage snapshot = parentViewContext.getIcon().snapshot(sp, null);
                    showStage.getIcons().clear();
                    showStage.getIcons().add(SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(snapshot, null), null));
                }
                /* ***Для JInvDesktop*** */

                logger.debug("internalShow: frameMode={}, showStage={}, modal={}, parentStage={}", frameMode, showStage, modal, parentViewContext.getStage() );

                if( frameMode.equals(JInvMainFrame.JInvFrameMode.SDI) && showStage != null )
                {
                    if( modal && parentViewContext.getStage() != null)
                    {
                        boolean alwaysOnTop = showStage.isAlwaysOnTop();
                        showStage.setAlwaysOnTop(true);

                        final javafx.stage.Stage finalShowStage = showStage;
                        Platform.runLater(() -> {
                            finalShowStage.toFront();
                            finalShowStage.setAlwaysOnTop(alwaysOnTop);
                        });

                        showStage.showAndWait();

                    }
                    else
                    {
                        // JDESK-45. JInvDesktop
                        if( Boolean.parseBoolean( System.getProperty("on_front", "false")) )
                        {
                            showStage.setAlwaysOnTop(true);
                            showStage.show();
                            showStage.toFront();
                            showStage.setAlwaysOnTop(false);
                        }
                        else
                        {
                            showStage.show();
                        }
                    }
                }
//                else if (frameMode.equals(JInvMainFrame.JInvFrameMode.MDI) && showWindow != null) {
//                    if (modal) {
//                        JInvWindowMdi.switchBlockParentWindow(showWindow.getParentWindow(), true);
//                    }
//
//                    BaseApp.APP().getMainFrame().getWindowManager().positionWindow(showWindow, vc);
//                    showWindow.setVisible(true);
//                }

            }
            catch (Throwable th ) {

                if( controller != null && controller.getClass().isAnnotationPresent( SingletonInstance.class ) ) {
                    g_controllerInstanceMap.remove( controller.getClass() );
                }

                if( Platform.isFxApplicationThread() ) {
                    JInvErrorService.handleException(parentViewContext.getStage(), th );
                }
                else
                {
                    ViewContext finalParentViewContext = parentViewContext;
                    Platform.runLater( () -> {
                        JInvErrorService.handleException(finalParentViewContext.getStage(), th );
                    });
                }
            }
            timeCheck.fixTime("INTERNAL_SHOW_4");

            timeCheck.printTime();
        });
    }

    /**
     *
     */
    public static ResourceBundle getResourceBundle(Class cl) {
        String s = String.format("%s/res/%s", cl.getPackage().getName().replace(".", "/"),
                cl.getSimpleName().replace("Controller", ""));
        return ResourceBundle.getBundle(s);
    }

    /**
     *
     */
    public static String getSceneFileName(Class cl) {
        String s = String.format("%s/fxml/%s.fxml", cl.getPackage().getName().replace(".", "/"),
                cl.getSimpleName().replace("Controller", ""));

        if (cl.getClassLoader().getResource(s) == null) {
            throw new IllegalArgumentException("Can't find resource " + s);
        }
        return s;
    }

    /**
     *
     */
    public TaskContext getTaskContext() {
        if (taskContext == null) {
            taskContext = new TaskContext();
            ownerTaskContext = true;
        }
        return taskContext;
    }

    /**
     *
     */
    public void setTaskContext(TaskContext taskContext) {

        if (this.taskContext != taskContext) {
            this.taskContext = taskContext;
            ownerTaskContext = false;
        }
    }

    /**
     *
     */
    public ViewContext getViewContext() {
        return viewContext;
    }

    public void setViewContext(ViewContext viewContext) {
        setViewContext(viewContext, true);
    }

    /**
     * Задать вьюконтекст для контроллера.
     * Используйте overrideExistingName = false при инициализации субконтроллеров
     * @param viewContext
     * @param overrideExistingName перезаписывать ли имя контроллера, если оно уже есть
     */
    public void setViewContext(ViewContext viewContext, boolean overrideExistingName) {
        this.viewContext = viewContext;

        if (viewContext != null)
        {
            if (viewContext.getStage() != null)
            {
                ObservableMap<Object, Object> stageProperties = viewContext.getStage().getProperties();
                boolean hasExistingName =
                            stageProperties.containsKey(PROPERTY_NAME) && stageProperties.get(PROPERTY_NAME) != null
                         && stageProperties.containsKey(PROPERTY_NAME_FOR_FILTER) && stageProperties.get(PROPERTY_NAME_FOR_FILTER) != null;

                if (overrideExistingName || !hasExistingName) {
                    stageProperties.put(PROPERTY_NAME, getClass().getCanonicalName());
                    stageProperties.put(PROPERTY_NAME_FOR_FILTER, getClass().getCanonicalName());
                }
            }

            if (viewContext.getWindow() != null)
            {
                ObservableMap<Object, Object> windowProperties = viewContext.getWindow().getProperties();
                boolean hasExistingName =
                           windowProperties.containsKey(PROPERTY_NAME) && windowProperties.get(PROPERTY_NAME) != null
                        && windowProperties.containsKey(PROPERTY_NAME_FOR_FILTER) && windowProperties.get(PROPERTY_NAME_FOR_FILTER) != null;

                if (overrideExistingName || !hasExistingName) {
                    windowProperties.put(PROPERTY_NAME, getClass().getCanonicalName());
                    windowProperties.put(PROPERTY_NAME_FOR_FILTER, getClass().getCanonicalName());
                }
            }
        } // end if
    }

    /**
     *
     */
    @Deprecated
    public javafx.stage.Stage getStage() {
        return viewContext.getStage();
    }

    /**
     *
     */
    @Deprecated
    public JInvWindowMdi getWindow() {
        return null;
    }

    /**
     * @return
     */
    public FormModeEnum getFormMode() {
        return formMode;
    }

    /**
     *
     */
    public T getDataObject() {
        return dataObject;
    }

    /**
     *
     */
    public void setDataObject(T o) {
        dataObject = o;
    }

    /**
     *
     */
    public IFXEntity<T> getFXEntity() {
        return fxEntity;
    }

    /**
     *
     */
    public Map<String, Object> getInitProperties() {
        return initProperties;
    }

    /**
     * @param title
     */
    public void setTitle(String title) {
        titleProperty.set(title);
    }

    /**
     * @return
     */
    public String getTitle() {
        return titleProperty.get();
    }

    /**
     * @return
     */
    public StringProperty titleProperty() {
        return this.titleProperty;
    }

    @Override
    public Optional<Set<JInvAction>> getWaitActions() {
        return Optional.of(waitActions);
    }

    public ObjectProperty<StateEnum> stateProperty() {
        return activeState;
    }

    @Override
    public StringProperty stateTextProperty() {
        return stateText;
    }

    /**
     * Возвращается состояние жизнезненно цикла формы.
     *
     * @return
     */
    public LifeCycleStateEnum getLifeCycleState() {
        return lifeCycleState.get();
    }

    public ObjectProperty<LifeCycleStateEnum> lifeCycleStateProperty() {
        return lifeCycleState;
    }

    private final StringProperty nameProperty = new SimpleStringProperty(this, "name", getClass().getName());

    /**
     * @return
     */
    public String getName() {
        return nameProperty.get();
    }

    /**
     * @param name
     */
    public void setName(String name) {
        nameProperty.set(name);
    }

    /**
     * @return
     */
    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    /**
     *
     */
    public ValidMan getValidMan() {

        if (validMan == null) {
            validMan = new ValidMan(this, externalValidators);
        }
        return validMan;
    }

    /**
     * @return
     */
    public boolean isDeleteDimensions() {
        return deleteDimensions;
    }

    /**
     * При установке в true происходит удаление размеров формы при выходе
     *
     * @param deleteDimensions
     */
    public void setDeleteDimensions(boolean deleteDimensions) {
        this.deleteDimensions = deleteDimensions;
    }

    /**
     * @return
     */
    public boolean isDisableSavedDimensions() {
        return disableSavedDimensions;
    }

    /**
     * @param disableSavedDimensions
     */
    public void setDisableSavedDimensions(boolean disableSavedDimensions) {
        this.disableSavedDimensions = disableSavedDimensions;
    }

    /**
     *
     */
    public void handleException(Throwable th) {
        Platform.runLater(() -> {
            JInvErrorService.handleException(getViewContext(), th);
        });
    }

    /**
     * Возвращает значение из bundle-файла строковый ресурс по ключу key
     */
    public String getBundleString(String key) {

        if (bundle.containsKey(key)) {
            return bundle.getString(key);
        }

        if (bundle != g_baseBundle && g_baseBundle.containsKey(key)) {
            return g_baseBundle.getString(key);
        }

        if (g_throwOnMissingResources) {
            return bundle.getString(key); // do Throw
        }
        return key;
    }

    /** */
    public <S> S getInitParameter( Object parameter ) {
        return getInitParameter(parameter, null, null);
    }

    /** */
    public <S> S getInitParameter( Object parameter, S defaultValue ) {
        return getInitParameter(parameter, defaultValue, null);
    }

    /** */
    public <S> S getInitParameter(Object parameter, Class<S> toClass) {
        return getInitParameter(parameter, null, toClass);
    }

    /** */
    public <S> S getInitParameter(Object parameter, S defaultValue, Class<S> toClass) {

        Map<String, Object> map = getInitProperties();

        if( parameter == null || map == null || map.isEmpty() )
            return defaultValue;

        String key = (parameter instanceof Enum) ? ((Enum) parameter).name() : parameter.toString();

        if( toClass != null )
        {
            S value = (S) map.get(key);

            if( value == null )
                return defaultValue;

            if( value.getClass() == toClass )
                return value;

            return TypeConverter.convert(value, toClass);
        }

        return (S) map.getOrDefault( key, defaultValue );
    }

    /**
     *
     */
    public void setInitParameter(Object parameter, Object value) {
        if (parameter == null) {
            return;
        }

        Map<String, Object> map = getInitProperties();

        if (map == null) {
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "initParameter map is null");
        }

        String key = (parameter instanceof Enum) ? ((Enum) parameter).name() : parameter.toString();

        map.put(key, value);
    }

    /**
     * Возвращает значение из bundle-файла строковый ресурс по ключу key.
     * <p>
     * Если параметры не пустые, осуществляем подстановку значений.
     */
    public String getBundleString(String key, Object... params) {

        String str = getBundleString(key);

        if (S.isNotNullOrEmpty(str) && params.length > 0) {
            return String.format(str, params);
        } else {
            return str;
        }
    }

    /** */
    public Node lookupNode( String fxId )
    {
        Node n = null;
        Parent contentPane = getContentPane();

        if( contentPane != null )
            n = contentPane.lookup(fxId);

        if( n == null )
            throw new IllegalArgumentException("lookupNode: node not found, fx:id=" + fxId);

        return n;
    }

    /** */
    public void initLongOperation(Callable<Boolean> callable, BiConsumer<Boolean, Throwable> clb) {


        new JInvParallelAction((ActionEvent event) -> {

            if (clb != null) {
                try {
                    clb.accept(callable.call(), null);
                } catch (Throwable ex) {
                    clb.accept(false, ex);
                }
            } else {
                try {
                    callable.call();
                    int a = 4;


                } catch (Throwable ex) {
                    Platform.runLater(() -> JInvErrorService.handleException(getViewContext(), ex));
                }
            }
        }, this).handle();
    }

    public void initLongOperation(Runnable runnable) {
        new JInvParallelAction(event -> {
            try {
                runnable.run();
            } catch (Exception e) {
                Platform.runLater(() -> JInvErrorService.handleException(getViewContext(), e));
            }
        }, this).handle();
    }

    /** */
    public <T> Iterable<T> populateRSIterator(Class<? extends T> clazz, String strWhere, String strOrderBy, IParameters parameters) {
        return RSUtils.createIterable(getTaskContext().getConnection(), clazz, strWhere, strOrderBy, parameters);
    }

    public <T> Iterable<T> populateRSIterator(Class<? extends T> clazz) {
        return RSUtils.createIterable(getTaskContext().getConnection(), clazz, null);
    }

    /**
     * @param executeMode - если нужно то режим выполнения запроса: NO_EXECUTE - 0 - не выполнять EXECUTE - 1 - выполнить EXECUTE_AND_FETCH - 2 - выполнить и прочитать все записи
     */
    public <T> SQLDataSet<T> populateDataSet(Class<? extends T> clazz, String strSQL, String strWhere, String strOrderBy,
                                             Map<String, Object> namedParameters, List<Object> indexedParameters, int executeMode)
            throws DataSetException {

        /*
         * TODO: if XXI mode -> XXIDataSet
         */
        SQLDataSet<T> ds = new SQLDataSet<>();
        ds.setTaskContext(getTaskContext());

        if (clazz != null) {
            ds.setRowClass(clazz);
        }

        if (!S.isNullOrEmpty(strSQL)) {
            ds.setSQL(strSQL);
        }

        if (!S.isNullOrEmpty(strWhere)) {
            ds.setWherePredicat(strWhere);
        }

        if (!S.isNullOrEmpty(strOrderBy)) {
            ds.setOrderBy(strOrderBy);
        }

        if (namedParameters != null && !namedParameters.isEmpty()) {
            namedParameters.forEach(ds::setParameter);
        }

        if (indexedParameters != null && !indexedParameters.isEmpty()) {
            for (int i = 0; i < indexedParameters.size(); i++) {
                ds.setParameter(i, indexedParameters.get(i));
            } // end for
        }

        if (executeMode > 0) {
            ds.executeQuery(executeMode == 2);
        }

        return ds;
    }

    /**
     *
     */
    public <T> SQLDataSet<T> populateDataSet(Class<? extends T> clazz, String strSQL, String strWhere, String strOrderBy,
                                             List<Object> indexedParameters, int executeMode) throws DataSetException {
        return populateDataSet(clazz, strSQL, strWhere, strOrderBy, null, indexedParameters, executeMode);
    }

    public <T> SQLDataSet<T> populateDataSet(Class<? extends T> clazz, String strSQL, String strWhere, String strOrderBy,
                                             int executeMode) throws DataSetException {
        return populateDataSet(clazz, strSQL, strWhere, strOrderBy, null, null, executeMode);
    }

    public <T> SQLDataSet<T> populateDataSet(Class<? extends T> clazz, String strSQL, String strWhere, String strOrderBy,
                                             Map<String, Object> namedParameters, int executeMode) throws DataSetException {
        return populateDataSet(clazz, strSQL, strWhere, strOrderBy, namedParameters, null, executeMode);
    }

    /**
     *
     */
    public <T> SQLDataSet<T> populateDataSet(Class<? extends T> clazz, int executeMode, Object... indexedParameters)
            throws DataSetException {
        return populateDataSet(clazz, null, null, null, Arrays.asList(indexedParameters), executeMode);
    }

    /**
     *
     */
    public void executeSQLExpression(URL location, String name) throws SQLExpressionException {
        executeSQLExpression(location, name, null, null);
    }

    public void executeSQLExpression(URL location, String name, Map<String, Object> parameters)
            throws SQLExpressionException {
        executeSQLExpression(location, name, parameters, null);
    }

    public void executeSQLExpression(URL location, String name, List<Object> parameters) throws SQLExpressionException {
        executeSQLExpression(location, name, null, parameters);
    }

    /** */
    static final private int V$SESSION_ACTION_LENGTH = 32;

    /** */
    protected void assignOraSessionAction( ) {
        assignOraSessionAction(null);
    }

    /** */
    protected void assignOraSessionAction( Class clazz )
    {
        String className = clazz == null ? this.getClass().getName() : clazz.getName();

        if( className.length() > V$SESSION_ACTION_LENGTH ) {

            className = className.replace("ru.inversion", "xxi");

            if (className.length() > V$SESSION_ACTION_LENGTH )
            {
                if( className.endsWith("Controller") )
                    className = className.replace( "Controller", "C*" );

                if (className.length() > V$SESSION_ACTION_LENGTH )
                    className = className.substring( className.length() - V$SESSION_ACTION_LENGTH );
            }
        }

        getTaskContext().assignSessionTitle(className);
    }

    /** */
    public void executeSQLExpression( URL location, String name, Map<String, Object> namedParameters, List<Object> indexedParameters) throws SQLExpressionException {
/*
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "executeSQLExpression");

        timeCheck.fixTime("EXECUTE_SQL_EXPRESSION_1");

        String className = this.getClass().getName();

        if (className.length() > V$SESSION_ACTION_LENGTH) {

            className = className.replace("ru.inversion", "xxi");

            if (className.length() > V$SESSION_ACTION_LENGTH) {
                //className = this.getClass().getSimpleName();
                className = className.substring( className.length() - V$SESSION_ACTION_LENGTH );
            }
        }

        try (CallableStatement cs = getTaskContext().getConnection()
                .prepareCall("{call dbms_application_info.set_action(?)}")) {
            cs.setString(1, className);
            cs.execute();
        } catch (Throwable ignored) {
        }


        timeCheck.fixTime("EXECUTE_SQL_EXPRESSION_2");

        timeCheck.printTime();
*/
//        assignOraSessionAction( );

        SQLExpressionFactory.INSTANCE().execute(location, name, getTaskContext().getConnection(), namedParameters, indexedParameters );
    }

    /**
     * Инициализируем визуальное представление формы по сохраненным параметрам
     */
    public void initViewSettings() {

        //assignOraSessionAction( );

        try {

            if( viewContext.getStage() != null )
            {
                Parent root = viewContext.getStage().getScene().getRoot();
                BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(root);

            } else if (viewContext.getWindow() != null) {
                ///BaseApp.APP().getViewPrefService().refreshViewSettingsWindow(viewContext.getWindow());
            }

        } catch (Throwable ex) {
            JInvErrorService.handleException(viewContext.getStage(), ex);
        }
    }

    /**
     * Возвращает рутовую панель
     */
    public Parent getContentPane() {

        if (getState().equals(StateEnum.WAIT) && stateDecorator != null) {
            return stateDecorator.getRootPane();
        } else {
            return getViewContext().getContentPane();
        }
    }

    /**
     * Метод закрытия формы с возможностью передать код завершения
     *
     * @param retCode код завершения, по-умолчанию RET_CANCEL
     */
    public void close(FormReturnEnum retCode) {
        if (retCode != null) {
            returnProperty.setValue(retCode);
        }
        close();
    }

    /**
     * Программный метод закрытия формы
     */
    public void close() {

        logger.trace("close");

        if (getViewContext() != null && getViewContext().getStage() != null) {
            Platform.runLater(() -> {
                getViewContext().getStage()
                        .fireEvent(new WindowEvent(getViewContext().getStage(), WindowEvent.WINDOW_CLOSE_REQUEST));
            });

        } else if (getViewContext() != null && getViewContext().getWindow() != null) {
//            Platform.runLater(() -> {
//                getViewContext().getWindow().getCloseButton().fireEvent(new ActionEvent());
//            });
        }
    }

    protected void initFormController(TaskContext tc, ViewContext vc, FormModeEnum dialogMode, T dataObject,
                                      Property<FormReturnEnum> returnProperty,
                                      Map<String, Object> properties,
                                      Consumer<ResultForm<T>> returnCallback,
                                      ViewContext parentViewContext) {

        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "initFormController");

        timeCheck.fixTime("INIT_FORM_CONTROLLER_1");

        logger.info("Launch controller " + getClass());
        lifeCycleStateProperty().addListener((observable, oldValue, newValue) ->
                logger.trace("{} (tc:{}) Life cycle state : {}", getClass(), getTaskContext(), newValue));

        try {
            this.returnCallback = returnCallback;
            this.parentviewContext = parentViewContext;
            this.dataObject = dataObject;
            if (dialogMode != null) {
                this.formMode = dialogMode;
            }
            this.returnProperty = U.nvl(returnProperty, new SimpleObjectProperty<FormReturnEnum>());

        } catch (Throwable th) {
            JInvErrorService.handleException(vc.getStage(), th);
        }

        try {

            setTaskContext(tc);
            setViewContext(vc);

            initProperties = U.nvl(properties, Collections.EMPTY_MAP);

            // Инициализируем работу с клавиатурой
            if (vc.getStage() != null) {
                JInvKeyboardManager.initKeyBoard(vc.getStage().getScene());
            } else if (vc.getWindow() != null) {
                JInvKeyboardManager.initKeyBoard(vc.getWindow().getScene());
            }

            boolean hasStage = viewContext != null && viewContext.getStage() != null;
            boolean hasWindow = viewContext != null && viewContext.getWindow() != null;
            if (hasStage) {
                viewContext.getStage().setOnCloseRequest(this::internalOnClose);
                viewContext.getStage().showingProperty().addListener(new ChangeListener<Boolean>() {
                    //Сработали разок – и хватит
                    boolean triggeredOnce = false;

                    @Override
                    public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue,
                                        Boolean newValue) {
                        if ( triggeredOnce ){
                            return;
                        }

                        try {
                            if (newValue) {
                                internalInit();

                                lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);
                                // Перенес чтоб учитывало изменение состава формы
                                // Пока не до конца еще
//                                initViewSettings();
                            }
                        } catch (Throwable ex) {

                            if (!disableHandleInitException) {
                                Platform.runLater(() -> {
                                    JInvErrorService.handleException(viewContext.getStage(), ex);
                                });
                            } else {
                                exception = ex;
                            }
                            close();
                        } finally {
                            triggeredOnce = true;
                        }
                    }
                });

            } else if (hasWindow) {

                //viewContext.getWindow().getCloseButton().setOnAction(this::internalOnClose);

                internalInit();
                lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);
                // UP
//                initViewSettings();
            }
            // Перенес из beforeInit чтобы не прыгали размеры.
            initViewSettings();

        } catch (Throwable ex) {

            exception = ex;
            if (!(disableHandleInitException)) {
                JInvErrorService.handleException(null, ex);
            } else {
                exception = ex;
            }
            close();
        }
        timeCheck.fixTime("INIT_FORM_CONTROLLER_2");

        timeCheck.printTime();
    }

    /**
     *
     */
    private void internalInit() throws Exception {

        final MethodTimeCheck timeCheck = new MethodTimeCheck( AbstractBaseController.class.getName(), getClass().getName(), "internalInit" );

        timeCheck.fixTime("INTERNAL_INIT_1");

        // try {

        lifeCycleStateProperty().set( LifeCycleStateEnum.BEFORE_INIT );

        beforeInit( );

        timeCheck.fixTime("INTERNAL_INIT_2");

        lifeCycleStateProperty().set(LifeCycleStateEnum.INIT);

        //JAVAKERNEL-1143 ядерные валидаторы должны быть в приоритете
        //bindControl();

        init( );

        timeCheck.fixTime("INTERNAL_INIT_3");

        bindControls( );

        configControls( );

        configStandartButtons( );

        if( S.isNullOrEmpty(getTitle()) ) {
            setTitle( getFormMode().getTitle() );
        }

        // afterInit
        if (getContentPane().getScene() == null) {
            getContentPane().sceneProperty().addListener(new ChangeListener<Scene>() {
                //Сработали разок – и хватит
                //boolean triggeredOnce = false;

                @Override
                public void changed( ObservableValue<? extends Scene> observable, Scene oldValue, Scene newValue) {

//                    if ( triggeredOnce ){
//                        return;
//                    }

                    if( newValue != null )
                    {
                        try {
                            internalAfterInit( );
                            //triggeredOnce = true;
                        } catch (Throwable ex) {
                            JInvErrorService.handleException(getViewContext(), ex);
                        } finally {
                            getContentPane().sceneProperty().removeListener(this);
                        }
                    }
                }
            });
        } else {
            internalAfterInit();
        }

        timeCheck.fixTime("INTERNAL_INIT_4");

        timeCheck.printTime();
    }

    /**
     *
     */
    protected void beforeInit() throws Exception {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "beforeInit");
        timeCheck.fixTime("BEFORE_INIT_1");
        checkLicense();
        ((ModuleService) BaseApp.APP().getAppService(ModuleService.SERVICE_ID)).invokeModuleService(getClass());
        initFormStateDecorator();

        dataObject = configureDataObject();

        fxEntity = createFXEntity();
        timeCheck.fixTime("BEFORE_INIT_2");
        timeCheck.printTime();
    }

    /**
     *
     */
    protected void init() throws Exception {

    }

    /** Возвращает фильтр для компонентов, для включения/исключения из байндинга */
    protected Predicate<Control> getBindingFilter() {
        return (c)->true;
    }

    /**
     * Привязка компонентов с моделью данных
     */
    protected void bindControls( ) throws Exception {

        final MethodTimeCheck timeCheck = new MethodTimeCheck( this.getClass().getName(), getClass().getName(), "bindControl" );

        timeCheck.fixTime("BIND_CONTROLS_1");

        List<Control> listControl = Controls.getControlList( getContentPane(), (Control t) -> t instanceof IJInvControl);

        // Связываем control и label
        initLabels(listControl);

        IFXEntity fxEnt = getFXEntity();

        if( fxEnt != null )
        {
            List<Control> controlList = Controls.getControlList( getContentPane(), getBindingFilter() );

            Binder.bind( fxEnt, controlList, this );

            boolean isReadOnlyForm = getFormMode() != VM_INS && getFormMode() != VM_EDIT && getFormMode() != VM_NONE;

            if ( isReadOnlyForm )
            {
                controlList.forEach( Controls::disableControl );

                // Сворачиваем toolbar(ы) на форме просмотра
                controlList.forEach((t) -> {
                    if(t instanceof JInvToolBar){
                        t.autosize();
                        t.setMaxHeight(0);
                    }
                });
                //Не использовать валидацию на формах просмотра
                controlList.forEach( control -> getValidMan().getValidators( control ).clear() );
            }
        }

        timeCheck.fixTime("BIND_CONTROLS_2");

        timeCheck.printTime();
    }

    /**
     * Настройка компонентов, после привязки
     */
    protected void configControls() throws Exception {

        initControls();

        if (U.in(getFormMode(), FormModeEnum.VM_INS, VM_EDIT, FormModeEnum.VM_NONE)) {
            getValidMan().markControls();

            // Инициализируем валидатор, который проверяет ошибки конверсии по ok
            getValidMan().initStateValidator(getContentPane());
        }
    }

    /**
     *
     */
    protected void configStandartButtons() {

        Parent parent = getContentPane();

        Button btOK = null, btCancel = null;

        if (parent instanceof DialogPane) {

            DialogPane dp = (DialogPane) parent;

            btOK = (Button) dp.lookupButton(ButtonType.OK);
            btCancel = (Button) dp.lookupButton(ButtonType.CANCEL);
        } else {
            btOK = getStandartButton(ButtonType.OK);
            btCancel = getStandartButton(ButtonType.CANCEL);
        }

        if( btOK != null )
        {
            btOK.addEventFilter(MouseEvent.MOUSE_PRESSED, new EventHandler< MouseEvent >() {
                @Override
                public void handle( MouseEvent event ) {
                    if( event.isPrimaryButtonDown() && event.getClickCount() > 1 ) {
                        event.consume();
                    }
                }
            });

            btOK.setOnAction((ActionEvent event) -> {
                internalOK(event);
            });


            btOK.getStyleClass().add(CSS_OK_BUTTON_CLASS);
            btOK.setId("BUTTON_OK");

            if( S.isNullOrEmpty(btOK.getText()) )
            {
                btOK.setText( g_baseBundle.getString("OK") );
            }

            btOK.setVisible(true);
        }

        if (btCancel != null) {
            btCancel.getStyleClass().add(CSS_CANCEL_BUTTON_CLASS);
            btCancel.setOnAction((ActionEvent a) -> {
                internalCancel(a);
            });
            if (S.isNullOrEmpty(btCancel.getText())) {
                btCancel.setText(g_baseBundle.getString("CANCEL"));
            }
            btCancel.setVisible(true);
        }
    }

    /**
     * Список дополнительных методов инициализации контроллера
     */
    private List<RunnableWithException> onAfterInitRunnables;

    /** */
    protected void addOnAfterInitRunnable(RunnableWithException r) {
        if (r != null) {
            if (onAfterInitRunnables == null)
                onAfterInitRunnables = new LinkedList<>();
            else if (onAfterInitRunnables.contains(r))
                return;

            onAfterInitRunnables.add(r);
        }
    }

    /** */
    private Color showConIndicator( Color color ) {

        int borderWidth = 2;

        try {
            borderWidth = BaseApp.APP().getViewPrefService().getIndicatorBorderWidth();
        }
        catch( AppException ex ) {

        }

        BorderWidths bw =  ( borderWidth == 1 )
                            ? BorderWidths.DEFAULT
                            : new BorderWidths(borderWidth, borderWidth, borderWidth, borderWidth, false, false, false, false);


        ( (Region)getContentPane() ).setBorder (
                new Border( new BorderStroke
                        ( color,
                                BorderStrokeStyle.SOLID,
                                new CornerRadii(5,false),
                                bw,
                                new Insets( 5, 5, 5, 5 ) ) ) );
        return color;
    }

    /** */
    protected void internalAfterInit() throws Exception {

        final MethodTimeCheck timeCheck = new MethodTimeCheck( AbstractBaseController.class.getName(), getClass().getName(), "internalAfterInit" );

        timeCheck.fixTime("INTERNAL_AFTER_INIT_1");

        try {
            BaseApp.APP().getViewPrefService().getIndicatorColor( getTaskContext() ).map( this::showConIndicator );
        }
        catch( Throwable th ) {
            th.printStackTrace();
        }

        getContentPane( ).getScene().addEventFilter(MouseEvent.ANY, new ButtonValidationListener(getValidMan()));

        initContextMenu( );

        lifeCycleStateProperty( ).set( LifeCycleStateEnum.AFTER_INIT );

        afterInit( );

        if( onAfterInitRunnables != null ) {

            for( RunnableWithException r : onAfterInitRunnables )
                r.run();

            onAfterInitRunnables = null; //gc
        }

        initTitle();

        initTextCaretKeeper();

        timeCheck.fixTime("INTERNAL_AFTER_INIT_2");

        timeCheck.printTime();
    }

    /**
     JAVAKERNEL-1321
     Запоминает сфокусированный текстовый элемент и позицию каретки(курсора) при выделении другого окна
     И восстанавливает его при получении фокуса на окно
     */
    private void initTextCaretKeeper() {
        //javafx Stage/Window и jfxtras Window несовместимы :(
        javafx.stage.Stage stage = getViewContext().getStage();
//        JInvMdiWindow window = getViewContext().getWindow();
//        if ( stage == null && window == null ) {
//            return;
//        }

        final Node[] oldFocusOwner = new Node[1];
        final int[] caretPos = {0};

        Scene stageScene;
        if ( stage != null ){
            stageScene = stage.getScene();
        } else {
            //stageScene = window.getScene();
            stageScene = null;
        }

        if ( stageScene != null ){
            //keep track of caret
            stageScene.focusOwnerProperty().addListener( (v,o,n) -> {
                if ( n instanceof TextInputControl ){
                    ( (TextInputControl) n ).caretPositionProperty().addListener( ( v2, o2, n2 ) -> {
                        if ( n2 != null && n2.intValue() != 0 ) {
                            caretPos[0] = n2.intValue();
                        }
                    } );

                }
            } );
        }
        ChangeListener<Boolean> restoreCaretListener = ( v, o, n ) -> {
            if ( !n ) {
                //losing focus, save old focus owner
                if ( stageScene != null ) {
                    oldFocusOwner[0] = stageScene.focusOwnerProperty().get();
                }
            } else {
                //receiving focus, trying to restore if it's textfield
                if ( oldFocusOwner[0] != null && oldFocusOwner[0] instanceof TextInputControl ) {
                    oldFocusOwner[0].requestFocus();
                    ( (TextInputControl) oldFocusOwner[0] ).positionCaret( caretPos[0] );
                } else {
                    caretPos[0] = 0;
                    oldFocusOwner[0] = null;
                }
            }
        };

        if ( stage != null ) { //SDI
            stage.focusedProperty().addListener( restoreCaretListener );
        } else { //MDI
            BaseApp.APP().getMainFrame().focusedProperty().addListener( restoreCaretListener );
        }

    }

    // При закрытии контроллера, если этот флаг стоит, то методы onOk onCancel
    // не вызываются
    private boolean flagForCloseNow = false;

    public void closeNow(FormReturnEnum result) {
        returnProperty.setValue(result);
        closeNow();
    }

    public void closeNow() {
        flagForCloseNow = true;
        close();
        // flagForCloseNow = false;
    }

    /**
     * Вызывается всегда, когда закрывается окно.
     * <p>
     * Содержит как специализированную логику контроллера, так и пользовательскую.
     */
    protected void  internalOnClose(Event event) {

        try {

            logger.trace("internalOnClose. controllerState={}, lifeCycleState={}", getState(), getLifeCycleState());

            if (!flagForCloseNow) {
                // Выполняем логику специфичную для диалогов
                // Если выходим по кресту, либо программно, либо по отмене
                if (returnProperty.getValue() == null || returnProperty.getValue() == FormReturnEnum.RET_CANCEL) {

                    if ((getState() == StateEnum.WAIT) || !onCancel()) {
                        event.consume();
                        returnProperty.setValue(null);
                        lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);

                        if (getState() == StateEnum.WAIT
                                && Alerts.yesNo(getViewContext(),g_baseBundle.getString("CONFIRM_KILL_SESSION")) ){
                            logger.debug("Killing session... TC={} Session={}", getTaskContext(), getTaskContext().getSessionID());
                            try {
                                int killResult = AppKiller.killDBSession(getTaskContext().getSessionID());
                                if (killResult == 31){
                                    Alerts.info(this, g_baseBundle.getString("KILL_SESSION_QUEUED"));
                                }
                            } catch (Exception e){
                                logger.error("Caught exception when killing session:",e);
                            }
                            logger.debug("Finished killing session");
                            Platform.runLater(this::closeNow);
                            return;
                        } else {
                            return;
                        }
                    }

                    // Если выходим по OK
                } else if (returnProperty.getValue().equals(FormReturnEnum.RET_OK)) {

                    // Если в режиме редактирования или вставки запускаем
                    // валидацию
                    //if ((getFormMode().equals(FormModeEnum.VM_EDIT) || getFormMode().equals(FormModeEnum.VM_INS) || getFormMode().equals(FormModeEnum.VM_NONE))
                    //    && validMan != null) {
                    if (U.in(getFormMode(), VM_EDIT, VM_INS, VM_NONE) && validMan != null) {
                        if (!validMan.validate()) {
                            event.consume();

                            returnProperty.setValue(null);

                            return;
                        }
                    }

                    // Запускаем onOK
                    lifeCycleStateProperty().set(LifeCycleStateEnum.ON_OK);

                    if (!onOK()) {

                        event.consume();

                        returnProperty.setValue(null);

                        lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);

                        return;
                    }
                }
            }

            if (returnProperty.getValue() == null) {
                returnProperty.setValue(FormReturnEnum.RET_CANCEL);
            }

            if (getViewContext().getStage() != null) {
                getViewContext().getStage().setOnHidden((WindowEvent event1) -> {
                        try {
                            internalActionsOnClose();
                        } catch (Throwable ex) {
                            JInvErrorService.handleException(getViewContext().getStageOrPrimaryStage(), ex);
                        }
                } );
            } else {
                internalActionsOnClose();
                // Когда закрываем MDI надо очистить панель окон и
                // разблокировать родительские окна
                if (getViewContext() != null && getViewContext().getWindow() != null) {
                    //BaseApp.APP().getMainFrame().closeWindow(getViewContext().getWindow());
                }
            }

        } catch (Throwable ex) {
            lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);
            JInvErrorService.handleException(getViewContext().getStageOrPrimaryStage(), ex);
            onCloseResources();
        } finally {
            if (this.getClass().isAnnotationPresent(SingletonInstance.class)) {
                g_controllerInstanceMap.remove(this.getClass());
            }

        }
    }

    protected void internalActionsOnClose() throws Exception {
        // далее вызываем базовую реализацию завершения работы формы. Там
        // вызывается onCLoseWindow и чистятся ресурсы.
        try {
            logger.trace("internalActionsOnClose");

            lifeCycleStateProperty().set(LifeCycleStateEnum.ON_CLOSE);
            onCloseWindow();

            final String formName = getViewContext().getFormName() != null ? getViewContext().getFormName() : name;

            if( isDeleteDimensions() && !disableSavedDimensions ) {
                ViewPrefAppService.deleteDimensions(formName);
            }
            else
            {
                ViewPrefAppService.saveFormParameters (
                    viewContext,
                    formName,
                    !isDeleteDimensions() && !disableSavedDimensions
                );
            }

            if( returnCallback != null)
            {
                //logger.trace("callback");

                ResultForm result = new ResultForm();
                result.setController((JInvFXFormController<T>) this);
                result.setFormReturn(returnProperty.getValue());
                result.setException(exception);
                returnCallback.accept(result);
            }
        } finally {
            onCloseResources();
        }
    }

    /**
     * Пользовательский метод для реализации логики по закрытию окна.
     */
    protected void onCloseWindow() {
        logger.trace("onCloseWindow");
    }

   /**
     */
    private void onCloseResources() {
        if ( parentviewContext == null || parentviewContext.getStage() == null ){
            new ProgressTaskExecutor<Void, Void>()
                .allowCancel( false )
                .stage( viewContext.getStageOrPrimaryStage() )
                .callback( (ProgressCallback<Void, Void>) (progress, parameter) -> {
                    progress.updateMessage(getBundleString("BE_PATIENT_CLOSING"));
                    closeResourcesAndTaskContext();
                    return null;
                } )
                .execute();
        } else {
            new JInvParallelAction( e -> closeResourcesAndTaskContext(), parentviewContext.getController() ).handle();
        }
    }

    private void closeResourcesAndTaskContext() {
        try {
            logger.trace("closeResources");
            closeResources();
        } catch ( Exception e ){
            JInvErrorService.handleException(getViewContext(), e);
        } finally {
            closeTaskContext();
        }
    }

    /**
     Собственная реализация закрытия ресурсов до закрытия TaskContext.
     super.closeResources() вызывать нет нужды
     @throws Exception
     */
    protected void closeResources() throws Exception {}

    private void closeTaskContext(){
        logger.trace("closeTaskContext");
        if (ownerTaskContext || forceOwnerTaskContext) {
            try {
                getTaskContext().close();
            } catch (Exception ex) {
                JInvErrorService.handleException(getViewContext().getStage(), ex);
            }
        }
    }

    protected void afterInit() throws AppException {

    }

    protected void initControls() throws Exception {

        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "initControls");

        timeCheck.fixTime("INIT_CONTROLS_1");

        List<Control> listControl = Controls.getControlList( getContentPane(), (Control t) -> t instanceof IJInvControl);

        // Инициализация меню, если найден MenuBar
        initMenuBar(listControl);

        // // Связываем control и label
        // initLabels(listControl);
        // Cвязывание кнопки и текстовые компоненты
        initButtons(listControl);

        // Связываем JInvSortButton
        initSortButtons(listControl);

        // Инициализация тултипов для контролов
        initTooltips(listControl);

        // Инициализация таблиц и деревьев
        List<JInvTable<?>> tableList = null;
        {
            List<JInvTreeTableEx<?>> treeTableList = null;

            for( Control c : listControl)
            {
                 if( c instanceof JInvTable ) {
                     if( tableList == null )
                         tableList = new ArrayList<>(4);
                     tableList.add((JInvTable<?>)c);
                 }
                 else if( c instanceof JInvTreeTableEx ) {
                     if( treeTableList == null )
                         treeTableList = new ArrayList<>(3);
                     treeTableList.add((JInvTreeTableEx<?>)c);
                 }
            }

            if( tableList!= null )
                initTables(tableList);

            if( treeTableList != null )
                initTreeTables( treeTableList );
        }

        initKeyboardOnSpecificControls();

        //
        initRadioButtonsGroups();

        // Инициализация компонентов, которые не имеют fieldName
        initNotBindedControls(listControl);

        initPanes();
        initLovTextFields(listControl);

        //Находим не привязанные к таблице тулбары
        if( tableList != null )
        {
            final List<JInvTable<?>> listTables = tableList;

            final List<JInvToolBar> listUnboundToolbar = Controls.getControlList (
                  getContentPane(),
                  (Control bar) -> bar instanceof JInvToolBar && !(bar instanceof DSInfoBar)
            )
                .stream ( )
                .map    ( bar -> (JInvToolBar) bar )
                .filter ( bar -> listTables.stream().noneMatch(table -> table.getToolBar() != null && table.getToolBar().equals(bar)) )
                .collect( Collectors.toList() );

            initUnboundToolbars(listUnboundToolbar);
        }

        timeCheck.fixTime("INIT_CONTROLS_2");
        timeCheck.printTime();
    }

    private void initMenuBar(List<Control> listControl) {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(), getClass().getName(), "initMenuBar");
        timeCheck.fixTime("INIT_MENU_BAR_1");

        listControl.stream()
            .filter((Control control) -> control instanceof JInvMenuBar)
            .findFirst()
            .ifPresent(bar -> {
                JInvMenuBar menuBar = (JInvMenuBar) bar;
                if (!menuBar.isInitialized()) {
                    // 31/08/2022 fix checkItem
                    // Sulimoff
                    JInvMenuManager.checkMenuBar( menuBar, new XXIMenuLoader(taskContext, menuBar.getMnaMenu()));
                } else logger.info("menuBar {} already initialized!", bar);
        });

        timeCheck.fixTime("INIT_MENU_BAR_2");

        timeCheck.printTime();
    }

    /**
     * Добавляем кнопки из свободных (не привязанных к таблицам) тулбаров
     *
     * @param list
     * @throws Exception
     */
    private void initUnboundToolbars(List<JInvToolBar> list) throws Exception {
        list.forEach(toolBar -> {
            toolBar.getItems().forEach((Node t) -> {

                if (t instanceof JInvButton) {
                    JInvButton button = (JInvButton) t;

                    EventHandler handler = button.getOnAction();

                    if (handler instanceof JInvAction && ((JInvAction) handler).getHandler() == null) {
                        IAction action = JInvKeyboardManager.getAction(toolBar, button.getType());
                        if (action != null) {
                            button.setAction(action);
                        }
                    } else {
                        if (handler instanceof JInvAction) {
                            JInvKeyboardManager.addAction(button, (JInvAction) handler);
                        } else {
                            JInvKeyboardManager.addAction(button, new ActionBuilder()
                                    .setActionType(button.getType())
                                    .handler(handler)
                                    .setParallel(false)
                                    .build());
                        }
                    }
                }
            });
        });
    }

    /**
     *
     */
    protected IFXEntity createFXEntity() throws Exception {

        if (getDataObject() != null && getDataObject().getClass().getAnnotation(Entity.class) != null) {
            return new FXEntity<>(getDataObject());
        }
        return null;
    }

    /**
     *
     */
    protected T configureDataObject() throws Exception {
        return dataObject;
    }

    /**
     *
     */
    protected Button getStandartButton(ButtonType buttonType) {

        Parent parent = getContentPane();

        if (buttonType == ButtonType.OK) {
            return (Button) parent.lookup("#btOK");
        }
        if (buttonType == ButtonType.CANCEL) {
            return (Button) parent.lookup("#btCancel");
        }

        return null;
    }

    /**
     *
     */
    protected void internalCancel( ActionEvent event ) {
        logger.trace("internalCancel");
        returnProperty.setValue(FormReturnEnum.RET_CANCEL);
        close();
    }

    protected void internalCancel( ) {
        internalCancel(null);
    }


    protected void internalOK(  ) {
        internalOK(null);
    }

    /** */
    protected void internalOK( ActionEvent event ) {
        logger.trace("internalOK");
        returnProperty.setValue(FormReturnEnum.RET_OK);
        close();

    }

    /**
     * Польвательский метод для реализации логики по кнопке отмены
     */
    protected boolean onCancel() {
        logger.trace("onCancel");
        lifeCycleStateProperty().set(LifeCycleStateEnum.ON_CANCEL);
        return true;
    }

    /**
     *
     */
    protected void onCreateContextMenu(ContextMenu contextMenu) {
    }

    /** */
    protected void onOKCatch( Throwable th ) {
        JInvErrorService.handleException(getViewContext().getStage(), th);
    }

    /** */
    protected boolean onOK( ) {

        try {

            if( U.in( getFormMode(), VM_INS, VM_EDIT, VM_DEL) && getFXEntity() != null )
            {
                try( EntityDBHandler<T> es = new EntityDBHandler<>( getFXEntity(), getViewContext().getStage(), getTaskContext(), getFormMode() ) ) {
                    return es.handle();
                }
            }
            else
            {
                return true;
            }
        } catch ( Throwable th )
        {
            lifeCycleStateProperty().set(LifeCycleStateEnum.RUNTIME);
            onOKCatch(th);
        }
        return false;
    }

    /**
     * Код продукта. Используется для проверки лицензии. Переопределяется наследниками базового контроллера.
     *
     * @return
     */
    protected Integer getSSCODE() {
        return null;
    }


    /** */
    public static Stage getShowStage( ViewContext parentViewContext, Region pane, boolean modal )
    {

        Stage showStage = new Stage();

        if( parentViewContext != null && parentViewContext.getStage() != null )
            showStage.initOwner( parentViewContext.getStage() );

        if( modal && parentViewContext != null && parentViewContext.getStage() != null )
            showStage.initModality( Modality.WINDOW_MODAL );

        final Scene scene = new Scene(pane);

        showStage.setScene( scene );

        return showStage;
    }


    public static Optional< JInvWindowMdi > tryGetShowWindow( ViewContext parentViewContext, Region pane, boolean modal) throws Exception {

        if (BaseApp.APP().getMainFrame() == null)
        {
            logger.warn( "Trying to run in MDI mode without main window. Enforcing SDI mode!" );
//            throw new AppException(
//                    g_baseBundle.getString("RESHIM_OTOBRAZHENIYA_INTERFEJSA_MDI_PREDPOLAGAET_NALICHIE_GLAVNOGO_OKNA")
//            );
            return Optional.empty();
        }

        if ( modal && (parentViewContext == null || parentViewContext.getWindow() == null) ) {
            logger.warn( "Missing window in parent view context!" );
            return Optional.empty();
        }

        JInvWindowMdi showWindow = BaseApp.APP().getMainFrame().attachWindow(pane, "");

        if ( showWindow == null ){
            return Optional.empty();
        }

        if (parentViewContext != null) {
            //showWindow.setParentWindow(parentViewContext.getWindow());
        }

        return Optional.of( showWindow );
    }

    /** */
    private static JInvFXFormController getController(FXMLLoader loader, Map<String, Object> initProperties, Map<String, Object> innerProperties, String fxmlPath) {
        JInvFXFormController controller = loader.getController();

        if (controller == null) {
            throw new RuntimeException(java.text.MessageFormat.format(
                    g_baseBundle.getString("NEVOZMOZHNO_ZAGRUZIT_KONTROLLER_DLYA_FXML_PROVERTE_KORREKTNOST_PUTI_I_TEGA_FX_CONTROLLER_V"), new Object[]{fxmlPath, fxmlPath})
            );
        }

        if ( innerProperties != null ){
            // Признак отмены загрузки / сохранения размеров
            if ( (Boolean) innerProperties.getOrDefault(PROPERTY_DISABLE_SAVED_DIMENSIONS, false)) {
                controller.disableSavedDimensions = true;
            }

            // Признак обрабатывания исключения инита внутри контроллера
            if ( (Boolean) innerProperties.getOrDefault(PROPERTY_DISABLE_HANDLE_INIT_EXCEPTION, false)) {
                controller.disableHandleInitException = true;
            }

            // Насильно заставляет контроллер считать переданный таскконтекст своим и соответственно закрывать оный после освобождения ресурсов
            if ( (Boolean) innerProperties.getOrDefault(PROPERTY_FORCE_OWNER_TASK_CONTEXT, false)) {
                controller.forceOwnerTaskContext = true;
            }

            // Своя лямбда для инициализации контекстного меню в таблицах
            controller.setContextMenuInit( (Consumer)innerProperties.getOrDefault( PROPERTY_CONTEXT_MENU_INIT, null ) );

            // Валидаторы формы, прокинутые из FXFormLaunchera
            controller.setExternalFormValidators( (Validator[])innerProperties.getOrDefault( PROPERTY_FORM_VALIDATORS, null ) );
        }

        return controller;
    }

    private static void initPane(Region pane, JInvFXFormController controller) throws AppException {

        // Ругаемся, если размеры формы превышают максимально допустимые
        if (pane.getPrefHeight() > MAX_HEIGHT || pane.getPrefWidth() > MAX_WIDTH) {
            throw new AppException(
                    g_baseBundle.getString("OSHIBKA_RAZRABOTCHIKA_RAZMER_FORMY_BOLSHE_DOPUSTIMOGO_1200_700")
            );
        }

        ReadOnlyObjectProperty<JInvFXFormController> property = new SimpleObjectProperty<>(controller);
        pane.getProperties().put(PROPERTY_CONTROLLER, property);
    }

    /** */
    private void checkLicense() throws Exception {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "checkLicense");
        timeCheck.fixTime("CHECK_LICENSE_1");
        if (getSSCODE() != null)
            LoginManager.checkLicense(getTaskContext(), getSSCODE());
        timeCheck.fixTime("CHECK_LICENSE_2");
        timeCheck.printTime();
    }

    /**
     * Инициализируем декоратор для отображения долгих операций
     */
    protected void initFormStateDecorator() throws AppException {
        stateDecorator = new ProgressFormStateDecorator(this, getViewContext() );
    }

    /** */
    private void initTitle() {

         if( viewContext.getStage() != null)
         {
            if (viewContext.getStage().titleProperty().get() != null
                    && !viewContext.getStage().titleProperty().get().isEmpty()) {
                titleProperty.bindBidirectional(viewContext.getStage().titleProperty());
            } else {
                viewContext.getStage().titleProperty().bindBidirectional(titleProperty);
            }
        }
    }

    /**
     * Ищем все радио кнопки на панели.
     * После проходим по кнопкам и ищем в коллекции прикладных компонентов
     * на панеле группу с id совпадающим с id группы на кнопке.
     */
    private void initRadioButtonsGroups() {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "initRadioButtonsGroups");
        timeCheck.fixTime("INIT_RADIO_BUTTONS_GROUPS_1");
        List<Control> listRadioButton = Controls.getControlList(getContentPane(), null, JInvRadioButton.class);

        List<Object> listNonControlComponents = Controls.getNonControlComponentFromContainer(getContentPane());

        listRadioButton.stream().map((Control t) -> {
            return (JInvRadioButton) t;
        }).forEach((JInvRadioButton t) -> {

            String idGroup = t.getFieldName();
            if (idGroup != null && !idGroup.isEmpty()) {

                JInvRadioGroup group = listNonControlComponents.stream()
                        .filter((Object t1) -> t1 instanceof JInvRadioGroup).map((Object t1) -> (JInvRadioGroup) t1)
                        .filter((JInvRadioGroup t1) -> t1.getFieldName() != null && t1.getFieldName().equals(idGroup))
                        .findFirst().orElse(null);

                if (group != null) {
                    t.setToggleGroup(group);
                }
            }
        });

        // Так в момент заполнения Entity кнопки еще не были привязаны, вызываем
        // принудительно считывание значение из valueProperty
        Controls.getNonControlComponentFromContainer(getContentPane()).stream()
                .filter((Object t) -> t instanceof JInvRadioGroup).forEach((Object t) -> {
            ((JInvRadioGroup) t).refreshSelection();
        });
        timeCheck.fixTime("INIT_RADIO_BUTTONS_GROUPS_2");

        timeCheck.printTime();
    }

    /**
     * Инициализация контекстного меню
     */
    private void initContextMenu() {
        try {

            final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                    getClass().getName(), "initContextMenu");

            timeCheck.fixTime("INIT_CONTEXT_MENU_1");

            contextMenu.setAutoHide(true);
            MenuItem cmResetSizeItem = new MenuItem(getBundleString("CM_RESET_SIZE_ITEM"));
            cmResetSizeItem.setOnAction((ActionEvent e) -> {
                this.setDeleteDimensions(true);
            });
            MenuItem cmDBMSItem = new MenuItem(getBundleString("CM_DBMS_OUTPUT_WINDOW"));
            cmDBMSItem.setOnAction((ActionEvent e) -> {
                // new DBTraceDialog(this.getTaskContext());
                DBTraceDialog.showDbTraceDialog(getViewContext(), getTaskContext());
            });
            MenuItem cmSettingItem = new MenuItem(getBundleString("CM_SETTING"));
            cmSettingItem.setOnAction((ActionEvent e) -> {
                JInvMainFrame.showSettingsPane(this.getViewContext().getStageOrPrimaryStage());
            });

            contextMenu.getItems().addAll(cmResetSizeItem, cmDBMSItem, cmSettingItem);

            onCreateContextMenu(contextMenu);

            if (BaseApp.APP().getViewPrefService().getFrameMode() == JInvMainFrame.JInvFrameMode.SDI) {
                if (getViewContext().getStage() != null) {

                    getViewContext().getStage().addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
                        if (e.getButton() == MouseButton.SECONDARY) {
                            contextMenu.show((Window) e.getSource(), e.getScreenX(), e.getScreenY());
                        }
                    });

                }

            }
            if (BaseApp.APP().getViewPrefService().getFrameMode() == JInvMainFrame.JInvFrameMode.MDI) {
                if (getViewContext().getWindow() != null) {
                    getViewContext().getWindow().addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent e) -> {
                        if (e.getButton() == MouseButton.SECONDARY) {
                            contextMenu.show(getViewContext().getWindow(), e.getScreenX(), e.getScreenY());
                        }
                        if (contextMenu.isShowing() && e.getButton() == MouseButton.PRIMARY) {
                            contextMenu.hide();
                        }
                    });
                }
            }

            timeCheck.fixTime("INIT_CONTEXT_MENU_2");

            timeCheck.printTime();
        } catch (AppException ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    private void initKeyboardOnSpecificControls() {

        Controls.getControlList(getContentPane(), (Control t) -> t instanceof TabPane || t instanceof TitledPane)
                .forEach((Control t) -> {

                    if (t instanceof TabPane) {
                        t.getStyleClass().add(TabPane.STYLE_CLASS_FLOATING);
                    }

                    JInvKeyboardManager.initNaviationOnNode(t);
                });
    }

    private void initSortButtons(List<Control> listControl) {

        final MethodTimeCheck timeCheck = new MethodTimeCheck( AbstractBaseController.class.getName(), getClass().getName(), "initSortButtons" );

        timeCheck.fixTime("INIT_SORT_BUTTONS_1");

        final Map<String, JInvTable> tableMap = new HashMap<>();
        final List<JInvSortButton> sortButtonList = new ArrayList<>();

        //List<Control> listSortButtonFromPane =
                listControl
                    .stream()
                    .filter(
                        (Control t) -> {

                            if( t instanceof JInvTable &&  S.isNotNullOrEmpty( t.getId() ) ) {
                                tableMap.put( t.getId(), (JInvTable)t );
                            }

                            return t instanceof JInvSortButton && ((JInvSortButton) t).getIdTable() != null;
                        }
                    )
                    .map( (c)->(JInvSortButton)c)
                    .collect( Collectors.toCollection(()->sortButtonList) );

        //List<Control> listSortButtonFromTables =
                tableMap.values().stream().flatMap(
                (JInvTable t) -> (Stream<TableColumn>) t.getColumns().stream() )
                .flatMap((TableColumn column) ->
                {
                    if (column.getGraphic() instanceof JInvSortButton) {
                        JInvSortButton button = (JInvSortButton) column.getGraphic();
                        button.setIdTable(column.getTableView().getId());
                        button.setFocusTraversable(false);
                        return Stream.of(button);
                    }
                    else if (column.getGraphic() instanceof Parent)
                    {
                        List<Control> listSortButton = Controls.getControlList((Parent) column.getGraphic(),(Control m) -> m instanceof JInvSortButton);
                        listSortButton.forEach((Control button) -> {
                            ((JInvSortButton) button).setIdTable(column.getTableView().getId());
                            button.setFocusTraversable(false);
                        });
                        return listSortButton.stream();
                    } else {
                        return Stream.empty();
                    }
                }
//                ).collect(Collectors.toList() );
                ).map( (c)->(JInvSortButton)c)
                 .collect( Collectors.toCollection(()->sortButtonList) );
/*
        List<Control> listSortButtonFromTables = (List<Control>) listControl.stream().filter((Control t) -> {
            return t instanceof JInvTable;
        }).flatMap((Control t) -> (Stream<TableColumn>) ((JInvTable) t).getColumns().stream())
                .flatMap((TableColumn column) -> {
                    if (column.getGraphic() instanceof JInvSortButton) {
                        JInvSortButton button = (JInvSortButton) column.getGraphic();
                        button.setIdTable(column.getTableView().getId());
                        button.setFocusTraversable(false);
                        return Stream.of(button);
                    } else if (column.getGraphic() instanceof Parent) {
                        List<Control> listSortButton = Controls.getControlList((Parent) column.getGraphic(),
                                (Control m) -> m instanceof JInvSortButton);
                        listSortButton.forEach((Control button) -> {
                            ((JInvSortButton) button).setIdTable(column.getTableView().getId());
                            button.setFocusTraversable(false);
                        });
                        return listSortButton.stream();
                    } else {
                        return Stream.empty();
                    }
                }).collect(Collectors.toList());
*/
/*
        List<Control> sortButtonList = new ArrayList<>(listSortButtonFromPane);
        sortButtonList.addAll(listSortButtonFromTables);
*/
        sortButtonList.forEach((Control control) -> {

            JInvSortButton sortButton = (JInvSortButton) control;
            final String idTable = sortButton.getIdTable();
            /*
            JInvTable table = (JInvTable) listControl.stream().filter((Control t1) -> {
                return t1 instanceof JInvTable && ((JInvTable) t1).getId().equals(idTable);
            }).findFirst().orElse(null);
            */

            JInvTable table = tableMap.get(idTable);

            if( table != null )
            {
                DSFXAdapter adapter = table.getDataSetAdapter();

                if (adapter != null && adapter.getOrderByManager() != null )
                {
                    IDataSet ds = adapter.getDataSet();
                    if( ds != null && ds instanceof ISQLDataSet )
                        sortButton.setSortManager(adapter.getOrderByManager());
                }
            }
        });

        timeCheck.fixTime("INIT_SORT_BUTTONS_2");

        timeCheck.printTime();
    }

    /** */
    private void initTooltips(List<Control> listControl) {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "initTooltips");

        timeCheck.fixTime("INIT_TOOLTIPS_1");

        listControl.stream().map((Control t) -> (IJInvControl) t)
                .filter((IJInvControl control) -> control.getFieldName() != null).forEach((IJInvControl control) -> {

            Controls.getTooltipFromBundleByFieldName(control.getFieldName(), bundle).ifPresent(
                    (toolTipText) ->
                    {
                        if( control.getToolTipText() != null )
                            control.setToolTipText(toolTipText);

//                        control.getToolTipText().orElseGet(() -> {
//                            control.setToolTipText(toolTipText);
//                            return null;
//                        });
                    });
        });

        timeCheck.fixTime("INIT_TOOLTIPS_2");

        timeCheck.printTime();
    }

    /** */
    protected void initLabels( List<Control> listControl) {

        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(), getClass().getName(), "initLabels");

        timeCheck.fixTime("INIT_LABELS_1");

        List<Control> listLabel = Controls.getControlList(getContentPane(), (Control t) -> t instanceof JInvLabel);

        listControl.stream().filter((Control t) -> {
            return Controls.getFieldNameFromControl(t) != null && ((IJInvControl) t).getLabel() == null;
        }).forEach((Control control) -> {
            final String fieldName = Controls.getFieldNameFromControl(control);
            JInvLabel label = (JInvLabel) listLabel.stream().filter((Control t1) -> {
                String linkFieldName = ((JInvLabel) t1).getLinkFieldName();
                return ((JInvLabel) t1).getLabelFor() == null && linkFieldName != null
                        && linkFieldName.equals(fieldName);
            }).findFirst().orElse(null);
            if (label != null) {
                ((IJInvControl) control).setLabel(label);
            }
        });

        timeCheck.fixTime("INIT_LABELS_2");

        timeCheck.printTime();
    }

    /** */
    private void initButtons( List<Control> listControl) {

        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(), getClass().getName(), "initButtons");

        timeCheck.fixTime("INIT_BUTTONS_1");

        listControl.stream().filter((Control control) -> control instanceof JInvButton).forEach((Control control) -> {
            JInvButton bt = (JInvButton) control;

            if (bt.getGraphic() == null && bt.getIconName() != null && !bt.getIconName().isEmpty()) {
                ActionFactory.assignButtonStyleFromString(bt.getIconName(), bt.getName(), bt);
            }

            if (bt.getIdTextField() != null) {

                TextInputControl field = (TextInputControl) listControl.stream()
                        .filter((Control t) -> t instanceof TextInputControl && t.getId() != null
                                && t.getId().equalsIgnoreCase(bt.getIdTextField()))
                        .findFirst().orElse(null);

                if (field != null) {
                    bt.setTextField(field);
                }
            }
        });

        timeCheck.fixTime("INIT_BUTTONS_2");

        timeCheck.printTime();
    }

    /** */
    private void initPanes() {

        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(), getClass().getName(), "initPanes" );

        timeCheck.fixTime("INIT_PANES_1");

        List<Node> listNode = Controls.getNodeList( getContentPane(), null);

        listNode.stream().filter((Node t) -> t instanceof TitledPane).forEach((Node t) -> {
            t.setFocusTraversable(false);
        });

        listNode.stream().filter((Node t) -> t instanceof DSInfoBar).forEach((Node t) -> {
            ((DSInfoBar) t).build();
        });
        //TabPanes
        ViewPrefAppService.refreshTabPanePosition(viewContext);

        timeCheck.fixTime("INIT_PANES_2");

        timeCheck.printTime();
    }

    private MethodTimeCheck methodTimeCheck( String methodName, String firstFixTimeName ) {
        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(), getClass().getName(), methodName);
        timeCheck.fixTime( firstFixTimeName );
        return timeCheck;
    }

    /**
     * Инициализация различных параметров таблиц на форме
     * <p>
     * Вызывается после метода пользовательского {@code init()}
     */
    private void initTables( List<JInvTable<?>> listTables ) {

        final MethodTimeCheck timeCheck = methodTimeCheck( "initTables", "INIT_TABLES_1" );

        // Флаг, что диалог выбора фильтра уже был запущен
        final Holder<Boolean> filterDialogAlreadyRun = new Holder<>(false);

        listTables.forEach( (JInvTable table) -> {

            // Инициализация тулбаров таблиц.
            table.initKeyBoardActions();
            table.initToolBar();

            // Инициализация тултипов для колонок
            Platform.runLater (()->initTableColumnTooltips (table));
            
            // Контекстное меню
            table.initContextMenu( contextMenuInit );

            // Обработчики мыши
            table.initMouse();

            // Метод пост инициализация адаптера
            DSFXAdapter adapter = table.getDataSetAdapter();

            if( adapter != null )
            {
                adapter.afterInit( );

                refreshEnableFilterFromDb( adapter );

                if( adapter.isEnableFilter()  )
                {
                    try {

                        String formName    = S.trimLongString( getViewContext().getFormNameForFilter(), C_MAX_FIELD_BD );
                        IDataSet dataSet   = adapter.getDataSet();
                        String dataSetName = S.trimLongString( dataSet.getName(), C_MAX_FIELD_BD );

                        // Фильтр по умолчанию
                        if( !filterDialogAlreadyRun.get() )
                        {
                            try {

                                boolean isRunOnForm = dataSet instanceof XXIDataSet && ((XXIDataSet)dataSet).isEnableAutoFilter();
                                if(isRunOnForm)
                                    isRunOnForm = FilterWork.isRunOnForm ( null, formName, dataSetName );

                                if( isRunOnForm ) {

                                    final Runnable executeFilter = ( ) -> {
                                        table.requestFocus();
                                        table.getFilterToolbar().executeFilterAction();
                                    };

                                    Platform.runLater( executeFilter );

                                    filterDialogAlreadyRun.set( true );
                                }

                            } catch( Throwable th ) {
                                filterDialogAlreadyRun.set( true );
                                th.printStackTrace();
                            }
                        }// end get
                    } catch( Throwable th ) {
                        th.printStackTrace();
                    }
                }
            }

        });

        // Инициализация размеров колонок
        if( getViewContext().getStage() != null ) {
            ViewPrefAppService.refreshTableColumnPosition( viewContext.getStage().getScene().getRoot() );
        }

        timeCheck.fixTime("INIT_TABLES_2");
        timeCheck.printTime();
    }
    
    /**
     * В таблице динамически подгружаемой в SplitPane колонки появляются не сразу
     * <p>
     * Вызывается из метода {@code initTables()}
     */
    private void initTableColumnTooltips ( JInvTable<?> table )
    {
        table.lookupAll (".column-header")
             .forEach (ch -> 
            {
                final TableColumnHeader header = (TableColumnHeader) ch;
                TableColumn<?, ?> tc = (TableColumn) header.getTableColumn ();

                if (tc != null && tc instanceof JInvTableColumn) 
                {
                    JInvTableColumn<?, ?> column = (JInvTableColumn) tc;

                    String tooltipText = null;
                    if (! S.isNullOrEmpty (column.getToolTipText ()))
                    {
                        tooltipText = column.getToolTipText();
                    }
                    else
                    {
                        String toolTip = Controls.getTooltipFromBundleByFieldName (column.getFieldName(), bundle).orElse (null);
                        if (toolTip != null) 
                            tooltipText = toolTip;
                    }

                    if (tooltipText != null)
                    {
                        Tooltip tooltip = new Tooltip (tooltipText);
                        ((Control) header.lookup (".label")).setTooltip (tooltip);
                    }
                }
            });
    }        

    /**
     * Инициализация различных параметров таблиц на форме
     * <p>
     * Вызывается после метода пользовательского {@code init()}
     */
    private void initTreeTables( List< JInvTreeTableEx<?> > listTreeTables ) {

        final MethodTimeCheck timeCheck = methodTimeCheck( "initTreeTables", "INIT_TREETABLES_1" );

        listTreeTables.forEach( (JInvTreeTableEx treeTableView) -> {

            // Инициализация тулбаров таблиц.
            //table.initKeyBoardActions();
            //table.initToolBar();

            // Инициализация тултипов для колонок
            treeTableView.lookupAll(".column-header").forEach( n -> {
                final TableColumnHeader header = (TableColumnHeader) n;
                final TreeTableColumn t = (TreeTableColumn) header.getTableColumn();

                if( t != null && t instanceof JInvTreeTableColumnEx) {

                    JInvTreeTableColumnEx column = (JInvTreeTableColumnEx) t;

                    String tooltipText = null;
                    if( !S.isNullOrEmpty( column.getToolTipText() ) )
                        tooltipText = column.getToolTipText();
                    else
                        tooltipText = Controls.getTooltipFromBundleByFieldName( column.getFieldName(), bundle).orElse(null);

                    if( tooltipText != null )
                    {
                        Tooltip tooltip = new Tooltip(tooltipText);
                        ((Control) header.lookup(".label")).setTooltip(tooltip);
                    }
                }
            });

            // Контекстное меню
            //table.initContextMenu( contextMenuInit );

            // Обработчики мыши
            //table.initMouse();
        });

        // Инициализация размеров колонок
        if( getViewContext().getStage() != null ) {
            ViewPrefAppService.refreshTableColumnPosition( viewContext.getStage().getScene().getRoot() );
        }

        timeCheck.fixTime("INIT_TREETABLES_2");
        timeCheck.printTime();
    }


    /**
     * Возвращает признак активности компонентов с выбором.
     */
    public boolean isChoiceEnabled() {
        return choiceEnabled;
    }

    /**
     * Устанавливает признак активности компонентов с выбором.
     */
    public void setChoiceEnabled(boolean enabled) {
        this.choiceEnabled = enabled;
    }

    /**
     * Устанавливает в контроллер компонент, который имеет поведение для выбора значения
     */
    public void setChoiceControl(IChoiceControl cs) {
        cs.initChoiceBehavior(this);
    }

    private void initNotBindedControls(List<Control> listControl) {

        final MethodTimeCheck timeCheck = new MethodTimeCheck(AbstractBaseController.class.getName(),
                getClass().getName(), "initNotBindedControls");

        timeCheck.fixTime("INIT_NOT_BINDED_CONTROLS_1");

        listControl.stream().filter((Control t) -> S.isNullOrEmpty(((IJInvControl) t).getFieldName()))
                .forEach((Control control) -> {

                    // Если на поле есть класс лова, то генерим лов.
                    if (control instanceof ILovValueControl) {
                        try {
                            Binder.generateLov((ILovValueControl) control, getTaskContext());
                        } catch (ClassNotFoundException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    // Если контрол поддерживает валидацию
                    if (control instanceof IValidatableControl) {
                        IValidatableControl validatableControl = (IValidatableControl) control;

                        // Берем признак обязательности заполнения из контрола и
                        // регистритрируем как обязательный компонент
                        if (validatableControl.getRequiredState() != null && validatableControl.getRequiredState()
                                .equals(IValidatableControl.RequiredStateEnum.REQUIRED)) {
                            getValidMan().addRequiredControl(control);
                        }

                        // Если установлен флажок проверки заполнения по лову и
                        // лов не пустой, то регистрируем валидатор лова
                        if (control instanceof ILovValueControl) {
                            ILovValueControl lovControl = (ILovValueControl) control;
                            if (lovControl.isValidateFromLOV() && lovControl.getLOV() != null) {
                                getValidMan().bindValidators2Control(control, new LovValidator(lovControl));
                            }
                        }

                        //Если внутри компонента, есть валидаторы, которые необходимо поставить
                        getInternalValidatorsFromControl(control).forEach(new Consumer<Validator>() {
                            @Override
                            public void accept(Validator t) {
                                getValidMan().bindValidators2Control(control, t);
                            }
                        });
                    }
                });

        timeCheck.fixTime("INIT_NOT_BINDED_CONTROLS_2");

        timeCheck.printTime();
    }

    /**
     * https://community.oracle.com/message/11240449.
     * this class effectively does nothing, but it will be loaded by the application class loader instead of the system class loader.
     */
    private static class ResourceBundleWrapper extends ResourceBundle {

        private final ResourceBundle bundle;

        ResourceBundleWrapper(ResourceBundle bundle) {
            this.bundle = bundle;
        }

        @Override
        protected Object handleGetObject(String key) {
            return bundle.getObject(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return bundle.getKeys();
        }

        @Override
        public boolean containsKey(String key) {
            return bundle.containsKey(key);
        }

        @Override
        public Locale getLocale() {
            return bundle.getLocale();
        }

        @Override
        public Set<String> keySet() {
            return bundle.keySet();
        }
    }

    /**
     * Возвращает контроллер формы, из которой текущая форма была вызвана.
     * При корректной передаче viewContext при старте формы.
     */
    public <C extends AbstractBaseController<?>> C getParentController() {
        if (parentviewContext != null) {
            return parentviewContext.getController();
        } else {
            return null;
        }
    }

    /**     */
    public Throwable getException() {
        return exception;
    }

    /** */
    public void setException(Throwable exception) {
        this.exception = exception;
    }

    /** */
    private void initLovTextFields(List<Control> listControl) {
        // Без этой штуки кнопка отображается чуть левее в текстифилде, если она
        // генерируется лов генерируется в рантайме
        Platform.runLater(() -> {
            listControl.stream()
                    .filter((Control t) -> t instanceof JInvTextField && ((JInvTextField) t).getLOV() != null)
                    .forEach(Parent::requestLayout);
        });
    }

    /** */
    public String getJarFileName() {

        String result = null;
        File file = ModuleService.getJarFile(getClass());
        if (file != null) {
            result = file.getName();
        }
        return result;
    }

    /** */
    private void applyAdapterSettings(PPrefComponent p, DSFXAdapter adapter) {

        String element = p.getELEMENT();

        if (S.isNullOrEmpty(element))
            return;

        if (element.equals(PROPERTY_ENABLE_FILTER))
            adapter.setEnableFilter(p.getVISIBLE() == 1L);
        else {
            JInvTable jtbl = ((JInvTable) adapter.getTable());

            if (element.equals(TableSettingItemEnum.GUI_SHOW_STATUS_BAR.name())) {
                jtbl.visibleStatusBarProperty().set(U.in(p.getVISIBLE(), 1L, -1L));
                jtbl.getProperties().put(
                        TableSettingItemEnum.GUI_SHOW_STATUS_BAR.name(),
                        TripleBoolValueEnum.fromInt(p.getVISIBLE().intValue())
                );
            } else if (element.equals(TableSettingItemEnum.MARK_SAVE_PREV_MARKED_ROWS.name())) {
                jtbl.getFilterToolbar().savePrevMarkedRowsProperty().set(U.in(p.getVISIBLE(), 1L, -1L));
                jtbl.getProperties().put(
                        TableSettingItemEnum.MARK_SAVE_PREV_MARKED_ROWS.name(),
                        TripleBoolValueEnum.fromInt(p.getVISIBLE().intValue())
                );
            } else if(element.equals(TableSettingItemEnum.CHECK_NO_DATA_FOUND.name())){
                jtbl.checkNoDataFound().set(U.in(p.getVISIBLE(), 1L, -1L));
                jtbl.getProperties().put(
                        TableSettingItemEnum.MARK_SAVE_PREV_MARKED_ROWS.name(),
                        TripleBoolValueEnum.fromInt(p.getVISIBLE().intValue())
                );
                
                adapter.getDataSet().addDataSetListener((e) -> {
                    if(e.getEventType() == DataSetEvent.DataSetEventType.EXECUTE && e.isAfter())
                        if(adapter.getDataSet().getRows().isEmpty() && jtbl.checkNoDataFound().get())
                            Platform.runLater(() -> {
                                Alerts.info(this, g_baseBundle.getString("NO_DATA_FOUND"));
                            });
                });
            }
        }
    }

    /** */
    private void refreshEnableFilterFromDb(DSFXAdapter adapter) {

        if (adapter != null && getViewContext() != null) {
            Collection<PPrefComponent> initialPrefs = getViewContext().getViewPrefSaver().getInitialPrefs();

            final String tableId = adapter.getTable().getId();

            initialPrefs.stream().filter(t ->
                    t.getCOMPONENT() != null
                            && t.getCOMPONENT().equals(tableId)
            ).forEach(t -> applyAdapterSettings(t, adapter));
        }
    }

    /**
     * Вызывается после вызова диалога фильтра
     * для обновления данных в таблицах
     */
    public void onRefreshData( DSFXAdapter<?> adapter )
    {
        if( adapter != null )
            adapter.executeQuery();
    }
}