package ru.inversion.fx.form;

import com.sun.javafx.stage.StageHelper;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.service.IViewPrefSaver;
import ru.inversion.fx.app.service.ViewPrefFormService;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.utils.U;

/**
 * @author antonovdi
 */
public class ViewContext {

    static public ViewContext of( Stage stage) {
        return new ViewContext(stage);
    }

    static public ViewContext of( Window window) {
        return new ViewContext(toStage(window));
    }
    private Stage stage;
    private IViewPrefSaver viewPrefSaver = new ViewPrefFormService();
    private Label icon;

    /** */
    public ViewContext( Stage stage) {
        this(stage, null);
    }

    /** */
    public ViewContext( Stage stage, Label icon )
    {
        if( stage != null )
            this.stage = stage;
        else
        {
            if( BaseApp.APP() == null )
            {
                final ObservableList< Stage > stages = StageHelper.getStages();
                if( stages != null && !stages.isEmpty() )
                    this.stage = stages.get(0);
            }
            else
                this.stage = BaseApp.APP().getPrimaryStage();
        }
        this.icon = icon;
    }

    /** */
    public Stage getStage() {
        return stage;
    }

    /**
     * Возвращается либо текущий stage, при его наличие, либо stage главного окна, если оно имеется.
     */
    public Stage getStageOrPrimaryStage() {

        Stage result = getStage();

        if( result == null )
            result = BaseApp.APP().getMainFrame();

        if( result == null )
            result = BaseApp.APP().getPrimaryStage();

        return result;
    }

    /** */
    public void setStage( Stage stage) {
        this.stage = stage;
    }

    /** */
    public Window getWindow() {
        return getStage();
    }

    /**
     Пытается достать Window из переданного объекта
     @param parent
     @return null, если не нашлось
     */
    public static Window tryGetWindow( Object parent )
    {
        Window window = null;

        if( parent != null )
        {
            if ( parent instanceof Window ) {
                window = (Window) parent;
            } else if ( parent instanceof AbstractBaseController ) {
                window = ((AbstractBaseController) parent).getViewContext().getStageOrPrimaryStage();
            } else if ( parent instanceof ViewContext ) {
                window = ((ViewContext) parent).getStageOrPrimaryStage();
            } else if ( parent instanceof Scene) {
                window = ((Scene) parent).getWindow();
            } else if ( parent instanceof Node) {
                return tryGetWindow(((Node) parent).getScene());
            }
        }
        return window;
    }

    /** */
    public void setWindow( Window window) {
    }

    /** Устанавливает изменяемость размеров окна */
    public void setResizable(boolean value) {
        getStage().setResizable(value);
    }

    /** Возвращает изменяемость размеров окна */
    public boolean isResizable() {
        return getStage().isResizable();
    }

    /**
     * Фокусирует текущее окно. Если оно свернуто, то разворачивает его. Также переводит окно на передний план
     */
    public void requestFocus()
    {
        if (getStage() != null) {
            restoreMinimized();
            getStage().toFront();
            getStage().requestFocus();
        }
    }

    /** Восстанавливает окно из свернутого состояния */
    public void restoreMinimized()
    {
        if( getStage().isIconified() )
            getStage().setIconified(false);
    }

    public void setMinimized() {
        if (getStage() != null)
            getStage().setIconified(true);
    }

    /** */
    public boolean isMinimized() {
        return U.nvl( U.callIfNotNull( getStage(), Stage::isIconified ), Boolean.FALSE );
    }

    /** */
    public void setMaximized()
    {
        getStage().setMaximized(true);
    }

    public boolean isMaximized() {
        return getStage() != null && getStage().isMaximized();
    }

    /** */
    public boolean getMaximizeAllowed() {
        return true;
    }
    /** */
    public boolean getMinimizeAllowed() {
        return true;
    }

    /**
     * Возвращает имя формы.
     * Используется в механизме сохранения размеров и др.
     */
    public String getFormName() {
        return (String) getStage().getProperties().get(AbstractBaseController.PROPERTY_NAME);
    }

    public void setFormName(String name) {
        if( getStage() != null )
            getStage().getProperties().put( AbstractBaseController.PROPERTY_NAME, name );
    }

    //Возвращает имя формы. Используется в механизме сохранения фильтров
    public String getFormNameForFilter()
    {
        return (String) getStage().getProperties().get(AbstractBaseController.PROPERTY_NAME_FOR_FILTER);
    }

    /** */
    public void setFormNameForFilter(String name) {
        getStage().getProperties().put(AbstractBaseController.PROPERTY_NAME_FOR_FILTER, name);
    }

    /** */
    public double getHeight()
    {
        return getStage().isMaximized() ? -1.0d : getStage().getHeight();
    }

    public double getWidth()
    {
        return getStage().isMaximized() ? -1.0d : getStage().getWidth();
    }

    /**
     * Возвращает рутовую панель
     */
    public Parent getContentPane()
    {
        return U.callIfNotNull( getStage().getScene(), Scene::getRoot );
    }

    public IViewPrefSaver getViewPrefSaver() {
        return viewPrefSaver;
    }

    public void setViewPrefSaver(IViewPrefSaver viewPrefSaver) {
        this.viewPrefSaver = viewPrefSaver;
    }

    public <C extends AbstractBaseController<?>> C getController() {

        if (getStage() != null)
        {
            if( getStage().getScene() != null )
                return Controls.getControllerFromControl(getStage().getScene().getRoot());
        } else if (getWindow() != null) {
            return Controls.getControllerFromControl(getStage().getScene().getRoot());
        }
        return null;
    }

//    public static String getMdiTitle() {
//        return BaseApp.APP().getProperties(PropertiesTypeEnum.PRP).getStringProperty("ru.inversion.app.form_mdi_title");
//    }

    public void setIcon( Label icon ) {
        this.icon = icon;
    }

    public Label getIcon() {
        return icon;
    }

    private static Stage toStage( Window window) {
        if (window instanceof ContextMenu) {
            return (Stage) ((ContextMenu) window).getOwnerWindow();
        }
        return (Stage) window;
    }

    public void setMaximizeAllowed(boolean val) {
        //stub
    }

    public void setMinimizeAllowed(boolean val) {
        //stub
    }
}
