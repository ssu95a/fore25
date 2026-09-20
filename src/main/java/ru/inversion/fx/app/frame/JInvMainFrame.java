package ru.inversion.fx.app.frame;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.frame.menu.JInvMenuManager;
import ru.inversion.fx.app.frame.menu.JInvSettingsPane;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.AppServiceFactory;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.mdi.IWindowManager;
import ru.inversion.fx.form.mdi.JInvFrameBar;
import ru.inversion.fx.form.mdi.JInvWindowMdi;
import ru.inversion.fx.service.module.ModuleService;
import ru.inversion.utils.S;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static ru.inversion.fx.app.service.ViewPrefAppService.DEFAULT_WINDOW_SIZE;

/**
 *
 * @author ssu @
 */
public class JInvMainFrame extends Stage {

    /**
     * Режим запуска формы. SDI/MDI
     */

    /**
     * Компонент отвечает за управлением окнами в режиме MDI
     */
    private IWindowManager windowManager;

    /**
     * Рутовая панель главного окна. Состоит из меню и ScrollPane.
     */
    private BorderPane root;

    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");
    protected Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());

    public static final String NAME = "MAIN_FRAME";

    /**
     * Обработчик для пользовательских изменений меню после его создания
     */
    private Consumer<MenuBar> afterCreateMenuHandler;

    private final String menuId;

    private final String title;

    private final Class<? extends MenuWindow> menuClass;

    public JInvMainFrame() {
        this(null, null, null, null);
    }

    public JInvMainFrame(String menuId) {
        this(null, menuId, null, null);
    }

    public JInvMainFrame(Consumer<MenuBar> afterCreateMenuHandler) {
        this(afterCreateMenuHandler, null, null, null);
    }

    public JInvMainFrame(Consumer<MenuBar> afterCreateMenuHandler, String menuId,
                         String title, Class<? extends MenuWindow> clazz) {
        this.afterCreateMenuHandler = afterCreateMenuHandler;
        this.menuId = menuId;
        this.title = title;
        this.menuClass = clazz;

        init();

        showingProperty().addListener(new ChangeListener<Boolean>() {
            //Сработали разок – и хватит
            boolean triggeredOnce = false;

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if ( triggeredOnce ){
                    return;
                }

                BaseApp.APP().reportGuiLaunch();

                try {

                    // В момент когда окно показано, узнаем величину заголовку - важный параметр, который понадобится при масштабировании
                    ViewPrefAppService.heightWindowTitle = getHeight() - root.getScene().getHeight();
                    ViewPrefAppService.widthWindowBorder = getWidth() - root.getScene().getWidth();
                    BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(root, false);
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }

                triggeredOnce = true;
            }
        });

        setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {

                try {
                    List< JInvWindowMdi > list = getWindowManager().getWindows();

                    // Закрываем окна с конца
                    if (list != null && !list.isEmpty()) {
                        for (int i = list.size() - 1; i >= 0; i--) {
                            list.get(i).getCloseButton().fireEvent(new ActionEvent());
                        }
                    }
                    ViewPrefAppService.saveDimensions(NAME, JInvMainFrame.this);
                } catch (Throwable ex) {
                    JInvErrorService.handleException(this, ex);
                }
            }
        });

        BaseApp.APP().getPrimaryViewContext().setStage(this);
    }

    protected void init() {
        try {

            //frameMode = BaseApp.APP().getViewPrefService().getFrameMode();

            root = new BorderPane();
            root.setId(NAME);
            ScrollPane pane = new ScrollPane();
            AnchorPane paneContent = new AnchorPane();
            pane.setContent(paneContent);
            root.setCenter(pane);
            windowManager = new JInvFrameBar();
            root.setBottom((JInvFrameBar) windowManager);

            Scene mainScene = new Scene(root);
            setScene(mainScene);

            initTitle();
            initIcon();
            initSize(root);
            getScene().getStylesheets().add("css/general.css");

            //также инициализирует меню
            refreshViewSettings();
        } catch (Throwable ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    /** */
    protected void initMenu() throws AppException {
        MenuBar menu = createMenu();
        menu.prefWidthProperty().bind(root.widthProperty());
        root.setTop(menu);
    }

    /** */
    public MenuBar getMenuBar( )
    {
        final Node top = root.getTop();
        if( !(top instanceof MenuBar) )
            return null;

        return (MenuBar)top;
    }

    /**
     *
     */
//    protected IMenuLoader getMenuLoader() {
//        return BaseApp.APP().getDBTools().getMenuLoader(BaseApp.APP().getCommonTaskContext(), getMenuID());
//    }

    /**
     * установка заголовка главного окна
     */
    protected void initTitle()
    {
        if( title != null )
        {
            try {

                final String appInfo = BaseApp.APP().getProperties(PropertiesTypeEnum.SMR).getStringProperty("ru.inversion.app.info");
                final ModuleService service = (ModuleService) AppServiceFactory.getInstance().getService(ModuleService.SERVICE_ID);
                this.setTitle( title + " | v. " + service.getModuleContextOrCreate(menuClass).getFullVersion() + " | " + appInfo);

            } catch (AppException e) {
                JInvErrorService.handleException(null, e);
            }
        }
        else
        {
            this.setTitle(BaseApp.APP().getProperties(PropertiesTypeEnum.SMR).getStringProperty("ru.inversion.app.title"));
        }
    }

    /**
     * установка основной иконки приложения
     */
    protected void initIcon() throws AppException {
        final ViewContext primaryViewContext = BaseApp.APP().getPrimaryViewContext();
        if (primaryViewContext != null && primaryViewContext.getIcon() != null) {
            Scene scene = new Scene(new StackPane(primaryViewContext.getIcon()));
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            final WritableImage snapshot = primaryViewContext.getIcon().snapshot(sp, null);
            ((javafx.stage.Stage) getScene().getWindow()).getIcons().clear();
            ((javafx.stage.Stage) getScene().getWindow()).getIcons().add(SwingFXUtils.toFXImage(SwingFXUtils.fromFXImage(snapshot, null), null));
            return;
        }

        Image image = BaseApp.APP().getViewPrefService().getAppIcon();
        ((javafx.stage.Stage) getScene().getWindow()).getIcons().add(image);
    }

    /**
     * Устанавливаем размеры по-умолчанию.
     */
    protected void initSize(Region pane) {

        pane.setPrefSize(DEFAULT_WINDOW_SIZE.getWidth(), DEFAULT_WINDOW_SIZE.getHeight());
        if (BaseApp.APP().getProperties(PropertiesTypeEnum.DB_USER).getBooleanProperty("IS_MAXIMIZED_FRAME", false)) {
            setMaximized(true);
        }

    }

    /**
     * Создаем меню. Загружаем из базы + добавляем другие пункты меню: выход, сервис, окно
     */
    protected MenuBar createMenu() throws AppException {
        MenuBar menu = JInvMenuManager.loadMainMenu(BaseApp.APP().getDBTools().getMenuLoader(BaseApp.APP().getCommonTaskContext(), getMenuID()));
        addExitMenu(menu);
        addServiceMenu(menu);
        //addWindowMenu(menu);
        return menu;
    }

    /** Добавляем пункт меню "Выход", если его ещё нет
        А если есть, добавляем к нему событие закрытия*/
    private void addExitMenu(MenuBar mainMenu) {
        if (mainMenu != null) {
            //Нашли ли мы существующий пункт меню "Выход"?
            final AtomicBoolean isFound = new AtomicBoolean( false );
            String exitText = bundle.getString( "MENU_EXIT" );
            EventHandler<ActionEvent> closeEvent = ( ActionEvent event ) -> fireEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSE_REQUEST ) );
            EventHandler<MouseEvent> mouseEvent = event -> fireEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSE_REQUEST ) );

                //Ищем в корневых меню и подпунктах слово "Выход"
                mainMenu.getMenus()
                        .stream()
                        .flatMap( menu -> Stream.concat( menu.getItems().stream(), Stream.of(menu) ) )
                        .filter( menuItem -> {
                            if ( menuItem == null ) {
                                return false;
                            }
                            String text = menuItem.getText();
                            if ( S.isNullOrEmpty( text ) ) {
                                return false;
                            }
                            return text.equals( "Выход" ) || text.equals( "Вы&xод" ) || text.equals( "Вы&ход" );
                        } )
                        .findFirst()
                        .ifPresent( exitItem -> {
                            if ( exitItem instanceof Menu ){
                                //Для корневых пунктов меню приходится трюкачить, иначе события по нажатию не вызываются
                                prepareExitMenu( (Menu) exitItem, exitText, mouseEvent );
                            } else {
                                exitItem.setOnAction( closeEvent );
                                exitItem.setText( exitText );
                            }
                            isFound.set( true );
                        } );
            //Если не нашли, создаём пункт меню сами
            if ( !isFound.get() ){
                Menu exitMenu = new Menu();
                prepareExitMenu( exitMenu, exitText, mouseEvent );
                mainMenu.getMenus().add( exitMenu );
            }
        }
    }

    /** Делает корневые пункты меню нажимабельными */
    private void prepareExitMenu( final Menu menu, final String exitTitle, final EventHandler<MouseEvent> exitEvent ) {
        final Label actionLabel = new Label( exitTitle );
        actionLabel.setOnMouseClicked( exitEvent );
        menu.setGraphic( actionLabel );
        menu.setText( "" );
    }

    /**
     * Добавляем пункт меню "Сервис"
     */
    private void addServiceMenu(MenuBar mainMenu) {

        if( mainMenu != null )
        {
            int menuCount = mainMenu.getMenus().size();

            Menu serviceMenu = new Menu(bundle.getString("MENU_SERVICE"));

            MenuItem settingsItem = new MenuItem();
            settingsItem.setText(bundle.getString("MENU_SERVICE_SETTINGS"));
            settingsItem.setOnAction((ActionEvent event) -> {
                showSettingsPane();
            });

            serviceMenu.getItems().add(settingsItem);

            if (menuCount == 1) {
                mainMenu.getMenus().add(1, serviceMenu);
            } else {
                mainMenu.getMenus().add(menuCount - 1, serviceMenu);
            }
        }
    }

    /**
     * Возвращает компонет управляющий окнами в режиме MDI
     */
    public IWindowManager getWindowManager() {
        return windowManager;
    }

    /**
     * Добавить регион на новое окно MDI
     *
     * @param content регион с содержимым
     * @param title заголовок окна MDI
     * @return вновь созданное окно JInvWindowMdi
     * @throws AppException
     */
    public JInvWindowMdi attachWindow( Region content, String title) throws Exception {
//        frameMode = BaseApp.APP().getViewPrefService().getFrameMode();
//
//        if (frameMode.equals(JInvFrameMode.MDI)) {
//            JInvWindowMdi w = new JInvWindowMdi(title);
//            w.setVisible(false);
//
//            ScrollPane pane = (ScrollPane) root.getCenter();
//
//            ((AnchorPane) pane.getContent()).getChildren().add(w);
//            w.addRegion(content);
//
//            // Добавляем окно на панель задач
//            windowManager.addWindow(title, w);
//
//            return w;
//        } else {
//            return null;
//        }
        return null;
    }

    public static double computeRelativeExSize(double sizeInPixels) {

        /* Осуществим перевод из абсолютных единиц в относительные. В JavaFX, если не указываем единицу
            измерения, по-умолчанию считается что задаем значение в пикселях px. Будем переводить в
            относительные единицы ex,  1 ex это величина буквы строчной буквы x в текущем шрифте.

               Учитываем что программист задает размеры в дизайнере и видит на экране результат, тем самым указывает
               какое количество пикселей занимает элемент в текущем шрифте, по-умолчанию в javafx принят размер шрифта 12.
               см. NELI:\804\JavaFX\caspian.css_-_javafx.css  строчка 463

               Найдем таблицу соответствия em ex px.
               http://kb.mozillazine.org/Em_units_versus_ex_units

               Известно как перевести px в em.    em = px  / font. В нашем случае это 12.
               Далее видим строчку 1.5em  16px	1.5ex	9px. Итого получаем 1 ex = 0.5625 em.

               Учтем что для определяния значения относительной высота MDI окна, необходимо прибавить высоту заголовка этого окна.
               В файле general.css указали что размер заголовка 2 em, то бишь 2 * 12 = 24.

               Можно было написать проще, обьединив рассчеты в один множитель, но делает это не советую.
               Выглядит это так
               Double prefRelHeight = content.getPrefHeight() * 0.1481 + 4;
               Именно эта непонятная ерунда и привела к написанию столь обширного комментария
         */
        double sizeDefaultFont = 12;
        double exMultiplicator = 0.5625;

        return (sizeInPixels / sizeDefaultFont) / exMultiplicator;
    }

    /**
     * Закрываем окно mdi программно. При этом разблокируем всего его родительские окна, если они были заблокированы
     *
     */
    public void closeWindow( JInvWindowMdi w) {

//        JInvWindowMdi.switchBlockParentWindow(w.getParentWindow(), false);
//        windowManager.removeWindow(w);
//        w.close();
    }

    /** Получить меню привязанное к данному приложению */
    protected String getMenuID()
    {

        if( menuId != null )
            return menuId;

        return BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getStringProperty("MENU_ID");
    }

    /**
     * Возвращает scrollPane главного окна
     */
    public ScrollPane getScrollPane()
    {
        return null;
    }

    /**
     * Метод обновляет все компоненты главного окна основанные на настройках в базе данных:
     * инициализирует меню и заново применяет настройки отображения к главному окну.
     *
     * @throws AppException
     */
    public void refreshViewSettings( ) throws AppException
    {
        // Заново инициализируем меню. В разных режимах могут присутствовать разные меню, например "Окно"
        initMenu( );

        if (afterCreateMenuHandler != null)
            afterCreateMenuHandler.accept((MenuBar) root.getTop());

        BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(root, false);
    }

    /**
     * Запускаем окно настроек системы и, если главное окно присутствует, в модальном режиме
     */
    public static void showSettingsPane() {
        showSettingsPane(null);
    }

    /**
     * Показ окна системных настроек Ctrl-Space.
     *
     * Если Stage передан, то в модальном режиме относительно него,
     * иначе относительно главного окна если оно присутствует
     */
    public static void showSettingsPane( javafx.stage.Stage owner) {

        try {

            JInvSettingsPane content = new JInvSettingsPane();
            String title = bundle.getString("MENU_SERVICE_SETTINGS");

            javafx.stage.Stage settingsStage = new javafx.stage.Stage();
            JInvMainFrame mainFrame = BaseApp.APP().getMainFrame();

            if( owner == null && mainFrame != null )
            {
                settingsStage.initOwner   (mainFrame);
                settingsStage.initModality(Modality.APPLICATION_MODAL);
            }
            else
            {
                settingsStage.initOwner(owner);
                settingsStage.initModality(Modality.APPLICATION_MODAL);
            }

            settingsStage.setTitle(title);
            Scene scene = new Scene(content);
            settingsStage.setScene(scene);
            settingsStage.getScene().getStylesheets().add("css/general.css");
            BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(scene.getRoot());
            settingsStage.show();

        } catch (Throwable ex) {
            JInvErrorService.handleException((ViewContext) null, ex);
        }
    }

    public static enum JInvFrameMode {
        @Deprecated
        MDI,
        SDI;

        public static JInvFrameMode fromInt(Integer val) {
            if (val == null) {
                return null;
            }
            switch (val) {
                case 0:
                    return MDI;
                case 1:
                    return SDI;
                default:
                    return null;
            }
        }
    }

    public Consumer<MenuBar> getAfterCreateMenuHandler() {
        return afterCreateMenuHandler;
    }

    public void setAfterCreateMenuHandler(Consumer<MenuBar> afterCreateMenuHandler) {
        this.afterCreateMenuHandler = afterCreateMenuHandler;
    }

}
