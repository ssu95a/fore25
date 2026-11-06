package ru.inversion.fx.form.action;

import com.sun.javafx.scene.control.skin.TextAreaSkin;
import javafx.event.ActionEvent;
import javafx.event.EventTarget;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.exteditor.ExternalEditorManager;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.ITextFieldBase;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.filter.impl.FilterManager;
import ru.inversion.fx.form.controls.treetable.AdapterTreeItem;
import ru.inversion.fx.form.controls.treetable.JInvTreeTable;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableEx;
import ru.inversion.fx.form.dbtrace.DBTraceDialog;
import ru.inversion.fx.help.controller.HelpController;
import ru.inversion.fx.help.controller.ManageHelpController;
import ru.inversion.fx.help.entity.IHelped;
import ru.inversion.fx.help.facade.FXHelp;
import ru.inversion.utils.Pair;

import java.util.*;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author antonovdi
 */
public class JInvKeyboardManager {
    private static final Logger logger = getLogger(lookup().lookupClass());

    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");

    public static void initKeyBoard(Scene scene) {

        initFilterActions(scene);
        initTableActions(scene);
        initHelpActions(scene);
        initDBMSActions(scene);
        initExternalEditorActions(scene);

        initDimensionActions(scene);
        initContextMenu(scene);
        initTableCellSelectMode(scene);
        initTabPaneActions(scene);
        initTextEditActions(scene);
    }

    private static void initTextEditActions(Scene scene) {
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN)).
                setHandler((ActionEvent event) -> {
                    JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                    try {
                        EventTarget target = param.getEvent().getTarget();
                        if (target instanceof ITextFieldBase) {
                            ((ITextFieldBase) target).getEditDialogAction()
                                                     .ifPresent(a -> a.handle(null));
                        }
                    } catch (Throwable ex) {
                        JInvErrorService.handleException( scene.getWindow(), ex);
                    }
                }).build(),
            JInvEvent.PlaceType.FILTER);
    }

    public static void addAction(Node comp, IAction action) {

        SceneKeyManager.addAction(comp, action, JInvEvent.PlaceType.FILTER);
    }

    public static void addAction(Scene scene, IAction action) {

        SceneKeyManager.addAction(scene, action, JInvEvent.PlaceType.FILTER);
    }

    private static void initFilterActions(Scene scene) {

        //F7: Открыть диалог фильтра
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F7)).
            setHandler((ActionEvent event) -> {

                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {
                    if (param.getEvent().getTarget() instanceof JInvTreeTable) {
                        AdapterTreeItem adapterTreeItem = ((JInvTreeTable) param.getEvent().getTarget()).getAdapterTreeItem();
                        if (adapterTreeItem != null) {
                            adapterTreeItem.showFilterDialog();
                        }
                    } else {
                        DSFXAdapter adapter = Controls.getDsAdapterFromControl( param.getEvent().getTarget() );
                        if( adapter == null ) {
                            adapter = Controls.getDsAdapterFromControl( param.getControl() );
                        }

                        // TODO: Добавить проверку, что адаптер вокруг SQLDataSet и вернуть false, если это не так

                        if( adapter != null && adapter.getDataSet() instanceof ISQLDataSet && adapter.isEnableF7FilterDialog()) {
                            JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());
                            if ( controller != null ) {
                                adapter.showFilterDialog( controller.getViewContext() );
                            } else {
                                adapter.showFilterDialog( (Stage) scene.getWindow() );
                            }
                        }
                    }
                } catch (Throwable ex) {
                    JInvErrorService.handleException( scene.getWindow(), ex);
                }
            }).build(),
            JInvEvent.PlaceType.FILTER);

        //F8: Обновить таблицу, если можно
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F8)).
            setHandler((ActionEvent event) -> {
                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {
                    if (param.getEvent().getTarget() instanceof JInvTreeTable) {
                        AdapterTreeItem adapterTreeItem = ((JInvTreeTable) param.getEvent().getTarget()).getAdapterTreeItem();
                        adapterTreeItem.refreshTree();
                    }
                    else
                    {
                        DSFXAdapter adapter = Controls.getDsAdapterFromControl( param.getEvent().getTarget() );

                        if( adapter == null )
                            adapter = Controls.getDsAdapterFromControl( param.getControl() );

                        if( adapter != null && adapter.getDataSet() instanceof ISQLDataSet && adapter.isEnableF7FilterDialog() )
                        {
                            if( param.getEvent().getTarget() instanceof Node )
                            {
                                final AbstractBaseController controller = Controls.getControllerFromControl( (Node)param.getEvent().getTarget() );

                                if( controller != null )
                                    controller.onRefreshData( adapter );
                                else
                                    adapter.executeQuery();
                            }
                            else
                                adapter.executeQuery();
                        }
                    }
                } catch (Throwable ex) {
                    JInvErrorService.handleException( scene.getWindow(), ex);
                }
            }).build(),
            JInvEvent.PlaceType.FILTER);

        //Диалог выбора фильтра по F8+shift
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F8, KeyCombination.SHIFT_DOWN)).
            setHandler((ActionEvent event) -> {

                showDialog(scene, event);

            }).build(),
            JInvEvent.PlaceType.FILTER);

        //Сохранение текущего фильтра
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F7, KeyCombination.SHIFT_DOWN)).
            setHandler((ActionEvent event) -> {

                saveDialog(scene, event);

            }).build(),
            JInvEvent.PlaceType.FILTER);

    }

    private static TabPane findTabPaneInChildren(Parent parent){
        for ( Node node : getAllNodes( parent ) ) {
            if ( node instanceof TabPane ) {
                return (TabPane) node;
            }
        }
        return new TabPane();
    }

    private static ArrayList<Node> getAllNodes(Parent root) {
        ArrayList<Node> nodes = new ArrayList<>();
        addAllDescendents(root, nodes);
        return nodes;
    }

    private static void addAllDescendents(Parent parent, ArrayList<Node> nodes) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            nodes.add(node);
            if (node instanceof Parent){
                addAllDescendents((Parent)node, nodes);
            }
        }
    }

    private static void initTabPaneActions( Scene scene ) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            try {
                Parent control = scene.getRoot();
                if ( !event.isControlDown() || !event.getCode().equals( KeyCode.TAB ) ) return;

                if(control == null){ return; }

                TabPane tabPane = findTabPaneInChildren( control );
                    tabPane.requestFocus();
                    int size = tabPane.getTabs().size();
                    int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();

                    if (!event.isShiftDown()) {
                        if (selectedIndex < size -1) {
                            tabPane.getSelectionModel().selectNext();
                        } else {
                            tabPane.getSelectionModel().selectFirst();
                        }
                    } else {
                        if (selectedIndex > 0) {
                            tabPane.getSelectionModel().selectPrevious();
                        } else {
                            tabPane.getSelectionModel().selectLast();
                        }
                    }
            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
            }
        });

    }

    /** */
    private static void initTableActions( Scene scene )
    {

        SceneKeyManager.addAction( scene,
            new ActionBuilder( ).
                setKeyCombination (
                    new KeyCodeCombination(KeyCode.INSERT)
                ).
                setKeyCombination (
                    new KeyCodeCombination(KeyCode.SPACE )
                ).
            setHandler( JInvKeyboardManager::markHandler ).build(),
            JInvEvent.PlaceType.HANDLER
        );

        Controls.getControlList(scene.getRoot(),control -> control instanceof TableView)
                .stream()
                .map(TableView.class::cast)
                .forEach(table -> {
                    table.addEventHandler(KeyEvent.KEY_PRESSED,event ->{
//                        logger.info("**** ENTER or SPACE PRESSED ON TABLE {} ****", table);
                        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE ) {
                            try {
                                markHandler(new JInvEvent(scene.getFocusOwner(), JInvEvent.PlaceType.HANDLER, event));
                            } catch (Throwable ex) {
                                JInvErrorService.handleException(null, ex);
                            }
                        }
                    });
                });

        SceneKeyManager.addAction( scene,
            new ActionBuilder( ).
                setKeyCombination (
                    new KeyCodeCombination( KeyCode.A, KeyCombination.CONTROL_DOWN )
                ).
                setHandler((ActionEvent event) -> {

                    JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                    try {

                        DSFXAdapter adapter = Controls.getDsAdapterFromControl( param.getEvent().getTarget() );

                        if( adapter != null && adapter.isEnableMark() )
                        {
                            adapter.markAll();
                        }

                        param.getEvent().consume();

                    } catch (Throwable ex) {
                        JInvErrorService.handleException(null, ex);
                    }
                }).build(),
            JInvEvent.PlaceType.HANDLER
        );

        SceneKeyManager.addAction(scene,
                new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)).
                        setHandler((ActionEvent event) -> {

                            JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                            try {
                                Control control = (Control) param.getEvent().getTarget();
                                if( control != null )
                                {
//                                    if( control instanceof JInvTable) {
//                                        ((JInvTable) control).showDsInfoDialog();
//                                    }
                                    if( control instanceof JInvTreeTableEx) {
                                        ((JInvTreeTableEx) control).showColumnSearchDialog( );
                                    }
                                }
                                param.getEvent().consume();
                            } catch (Throwable ex) {
                                JInvErrorService.handleException( scene.getWindow(), ex );
                            }
                        }).build(),
                JInvEvent.PlaceType.HANDLER);


        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)).
            setHandler((ActionEvent event) -> {

                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {
                    Control control = (Control) param.getEvent().getTarget();
                    if( control != null )
                    {
                        if( control instanceof JInvTable) {
                            ((JInvTable) control).showDsInfoDialog();
                        }
                        else if( control instanceof JInvTreeTableEx) {
                            ((JInvTreeTableEx) control).showDsInfoDialog();
                        }
                    }
                    param.getEvent().consume();
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }).build(),
            JInvEvent.PlaceType.HANDLER);

            SceneKeyManager.addAction(scene,
                new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.PAGE_DOWN, KeyCombination.CONTROL_DOWN)).
                        setHandler((ActionEvent event) -> {

                            JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                            try {
                                Control control = (Control) param.getEvent().getTarget();
                                if( control != null && control instanceof JInvTable) {
                                    ((JInvTable) control).moveToLast();
                                }
                                param.getEvent().consume();
                            } catch (Throwable ex) {
                                JInvErrorService.handleException(null, ex);
                            }
                        }).build(),
                JInvEvent.PlaceType.FILTER);

        SceneKeyManager.addAction(scene,
                new ActionBuilder().setKeyCombination( new KeyCodeCombination(KeyCode.PAGE_UP, KeyCombination.CONTROL_DOWN)).
                        setHandler((ActionEvent event) -> {
                            JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                            try {
                                Control control = (Control) param.getEvent().getTarget();
                                if( control != null && control instanceof JInvTable) {
                                    ((JInvTable) control).moveToFirst();
                                }
                                param.getEvent().consume();
                            } catch (Throwable ex) {
                                JInvErrorService.handleException(null, ex);
                            }
                        }).build(),
        JInvEvent.PlaceType.FILTER);
    }

    /** */
    public static Iterator<IAction> getActionListIterator( Node node ) {

        if( node != null && node.getScene() != null )
        {
            List< Pair<Node, IAction> > list = SceneKeyManager.getActionMap( node.getScene(), JInvEvent.PlaceType.FILTER );

            return list.stream().filter( t -> t.first.equals(node) && t.second != null ).map( t->t.second ).iterator();
        }
        return Collections.emptyIterator( );
    }

    /** */
    public static IAction getAction( Node node, ActionFactory.ActionTypeEnum type ) {

        IAction result = null;

        if( node != null )
        {
            List<Pair<Node, IAction>> list = SceneKeyManager.getActionMap( node.getScene(), JInvEvent.PlaceType.FILTER );

            result = list
                    .stream()
                    .filter( t -> t.first.equals(node) )
                    .map   ( t -> t.second)
                    .filter((IAction a) -> a.getActionType() == type )
                    .findFirst()
                    .orElse(null);
        }
        return result;
    }

//    @Deprecated
//    public static IAction getAction(node node, ActionFactory.ActionTypeEnum type) {
//        IAction result = null;
//        if (type != null) {
//            result = getAction(node,type);
//        }
//        return result;
//    }

    private static void initHelpActions(Scene scene) {

        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F1)).
            setHandler((ActionEvent event) -> {

                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {

                    if( param.getControl() != null )
                    {
                        JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());

                        if( controller != null && !(controller instanceof HelpController) && !(controller instanceof ManageHelpController))
                        {
                            FXHelp.showHelp(controller.getTaskContext(), controller.getViewContext(), controller.getName(), controller.getClass());
                        }
                    }

                    param.getEvent().consume();

                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }).build(),
            JInvEvent.PlaceType.FILTER);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {

            try {
                if ( event.getCode().equals( KeyCode.ALT ) && isNoCtrlOrShiftDown( event ) ) {
//                  ставим хелповый курсор
                    scene.setCursor(BaseApp.APP().getViewPrefService().getHelpCursor());
                }
            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
            }
        });

        // убираем хелповый курсор, если вместе с альтом нажали ctrl и/или shift
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if(!isNoCtrlOrShiftDown( event )){
                scene.setCursor(Cursor.DEFAULT);
            }
        } );

        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if ( event.getCode().equals(KeyCode.ALT) ) {
                // убираем хелповый курсор, когда альт отпустили
                scene.setCursor(Cursor.DEFAULT);
            } else if ( releasedCtrlOrShift( event ) && event.isAltDown() && isNoCtrlOrShiftDown( event ) ) {
                try {
                    //восстанавливаем хелповый курсор, когда отпустили ctrl и shift, но держат alt
                    scene.setCursor( BaseApp.APP().getViewPrefService().getHelpCursor() );
                }  catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }
        } );

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, (MouseEvent event) -> {
            if (event.isAltDown() && event.getButton().equals(MouseButton.PRIMARY) /*&& isNoCtrlOrShiftDown( event )*/) {
                event.consume();
                Node node = event.getPickResult().getIntersectedNode();
                if (node != null) {
                    Control control;
                    if (checkHelpedNode(node)) {
                        control = (Control) node;
                    } else {
                        control = (Control) Controls.getParentStreamOfControl( node )
                                .filter( JInvKeyboardManager::checkHelpedNode )
                                .findFirst().orElse( null );
                    }

                    if (control != null) {
                        if (event.isControlDown()) {
                            FXHelp.showDialogTooltipHelp(control);
                        } else {
                            FXHelp.showTooltipHelp(control, event.getScreenX(), event.getScreenY());
                        }
                    }
                }
            }
        });

        scene.getWindow().focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
            if(!newValue){
                scene.setCursor(Cursor.DEFAULT);
            }
        } );
    }

    private static boolean releasedCtrlOrShift( final KeyEvent event ) {
        return event.getCode().equals( KeyCode.CONTROL ) ||
                    event.getCode().equals( KeyCode.SHIFT );
    }

    private static boolean isNoCtrlOrShiftDown( final InputEvent event ) {
        //беда JavaFX...
        if ( event instanceof KeyEvent ){
            return !( (KeyEvent) event ).isControlDown() && !( (KeyEvent) event ).isShiftDown();
        } else if ( event instanceof MouseEvent ){
            return !( (MouseEvent) event ).isControlDown() && !( (MouseEvent) event ).isShiftDown();
        }
        throw new UnsupportedOperationException( "Only KeyEvent & MouseEvent supported for NoCtrlOrShiftDown, sorry" );
    }

    private static boolean checkHelpedNode(Node node) {
        return node instanceof IHelped;
    }

    public static void initNaviationOnNode(Node node) {

        node.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {

            switch (event.getCode()) {
                case ENTER: {
                    if (node instanceof TextArea) {
                        break;
                    } else if (node instanceof JInvTable) {

                        JInvTable table = (JInvTable) node;

                        // Если колонка редактируемая, то выходим
                        if (table.getFocusModel().getFocusedCell() != null) {
                            TablePosition pos = table.getFocusModel().getFocusedCell();
                            if (pos != null) {
                                TableColumn column = pos.getTableColumn();
                                if (column != null && column.isEditable()) {
                                    return;
                                }
                            }
                        }

                        if (table.getController() != null && table.getController() instanceof JInvFXDialogController) {

                            if (table.isEnd()) {
                                fireForwardEvent(event, node);
                            } else {
                                fireGoDownEvent(event, node);
                            }
                        }
                    } else if (node instanceof Button) {
                        fireActionEvent(event, node);
                    } else if (node instanceof TabPane || node instanceof TitledPane) {
                        if (node.isFocused()) {
                            fireForwardEvent(event, node);
                        }
                    } else {
                        fireForwardEvent(event, node);
                    }
                }
                break;
                case DOWN: {
                    if (node instanceof TextArea) {
                        break;
                    } else if (node instanceof JInvTable) {
                        JInvTable table = (JInvTable) node;

                        if (table.getController() != null && table.getController() instanceof JInvFXDialogController) {

                            if (table.isEnd()) {
                                fireForwardEvent(event, node);
                            } else {
                                break;
                            }
                        }
                    } else if (node instanceof ComboBox) {
                        fireOpenEvent(event, node);
//                    } else {
                    } else if (node instanceof TabPane || node instanceof TitledPane) {
                        if (node.isFocused()) {
                            fireForwardEvent(event, node);
                        }
                    } else {
                        fireForwardEvent(event, node);
                    }
                }
                break;
                case UP: {
                    if (node instanceof TextArea) {
                        break;
                    } else if (node instanceof JInvTable) {
                        JInvTable table = (JInvTable) node;

                        if (table.getController() != null && table.getController() instanceof JInvFXDialogController) {
                            if (table.isFirstRow()) {
                                fireBackwardEvent(event, node);
                            } else {
                                break;
                            }
                        }

                    } else if (node instanceof ComboBox) {
                        fireOpenEvent(event, node);
//                    } else {
                    } else if (node instanceof TabPane || node instanceof TitledPane) {
                        if (node.isFocused()) {
                            fireBackwardEvent(event, node);
                        }
                    } else {
                        fireBackwardEvent(event, node);
                    }
                }
                break;
                case LEFT: {
                    if (node instanceof ComboBox && !((ComboBox)node).isEditable()) {
                        fireBackwardEvent(event, node);
                    }
                }
                break;
                case RIGHT: {
                    if (node instanceof ComboBox && !((ComboBox)node).isEditable()) {
                        fireForwardEvent(event, node);
                    }
                }
                break;
                case TAB: {
                    if (event.isControlDown()) {
                        fireForwardEvent(event, node);
                        break;
                    }
                    if ( node instanceof TextArea ) {
                        if ( event.isShiftDown() ) {
                            fireBackwardEvent( event, node );
                        } else {
                            fireForwardEvent( event, node );
                        }
                    }
                }
                break;
                default:
                    break;
            }

        });
    }

    public static void fireForwardEvent(KeyEvent event, Node node) {
        if (event != null) {
            event.consume();
        }

        if (node instanceof TextArea) {
            TextArea area = (TextArea) node;
            TextAreaSkin skin = (TextAreaSkin) area.getSkin();
            skin.getBehavior().traverseNext();
        } else {
            KeyEvent navigateEvent = null;
            if (event != null) {
                navigateEvent = new KeyEvent(event.getSource(), event.getTarget(), event.getEventType(), event.getCharacter(),
                    event.getText(), KeyCode.TAB, false, false, false, false);
            } else {
                node.requestFocus();
                navigateEvent = new KeyEvent(node, node, KeyEvent.KEY_PRESSED, " ",
                    "", KeyCode.TAB, false, false, false, false);

            }
            node.fireEvent(navigateEvent);
        }
    }

    private static void fireBackwardEvent(KeyEvent event, Node node) {
        event.consume();
        if (node instanceof TextArea) {
            TextArea area = (TextArea) node;
            TextAreaSkin skin = (TextAreaSkin) area.getSkin();
            skin.getBehavior().traversePrevious();
        } else {
            KeyEvent navigateEvent = new KeyEvent( event.getSource(), event.getTarget(), event.getEventType(), event.getCharacter(),
                    event.getText(), KeyCode.TAB, true, false, false, false );
            node.fireEvent(navigateEvent);
        }
    }

    private static void fireActionEvent(KeyEvent event, Node node) {

        if (node instanceof Button) {
            event.consume();
            ((Button) node).fire();
        }
    }

    private static void fireOpenEvent(KeyEvent event, Node node) {

        if (node instanceof ComboBox) {
            ((ComboBox) node).show();
        }
    }

    private static void fireGoDownEvent(KeyEvent event, Node node) {
        event.consume();

        KeyEvent navigeteEvent = new KeyEvent(event.getSource(), event.getTarget(), event.getEventType(), event.getCharacter(),
            event.getText(), KeyCode.DOWN, true, false, false, false);
        node.fireEvent(navigeteEvent);
    }

    private static void fireGoUpEvent(KeyEvent event, Node node) {
        event.consume();

        KeyEvent navigeteEvent = new KeyEvent(event.getSource(), event.getTarget(), event.getEventType(), event.getCharacter(),
            event.getText(), KeyCode.UP, true, false, false, false);
        node.fireEvent(navigeteEvent);
    }

    private static void initDBMSActions(Scene scene) {

        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.F5)).
            setHandler((ActionEvent event) -> {

                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {

                    if (param.getControl() != null) {

                        JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());
                        if (controller != null) {

//                            new DBTraceDialog(controller.getTaskContext());
                            DBTraceDialog.showDbTraceDialog(controller.getViewContext(), controller.getTaskContext());
                        }
                    }

                    param.getEvent().consume();
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }).build(),
            JInvEvent.PlaceType.FILTER);
    }

    private static void showDialog(Scene scene, ActionEvent event) {

        JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
        DSFXAdapter adapter = Controls.getDsAdapterFromControl( param.getControl() );
        JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());
        if (adapter != null && controller != null) {
            FilterManager filterManager = new FilterManager( adapter.getTaskContext(), controller.getViewContext());
            filterManager.getAndExecuteFilter( adapter );
        }
    }

    private static void saveDialog(Scene scene, ActionEvent event) {

        JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
        DSFXAdapter adapter = Controls.getDsAdapterFromControl( param.getControl() );
        JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());
        if (adapter != null && controller != null) {
            FilterManager filterManager = new FilterManager(adapter.getTaskContext(), controller.getViewContext());
            filterManager.saveCurrentFilter(
                    ((ISQLDataSet) adapter.getDataSet()).getFilter( ISQLDataSet.FilterTypeEnum.TEMPORARY, ISQLDataSet.FilterTypeEnum.FIXED ),
                    controller.getViewContext().getFormNameForFilter(), adapter.getDataSet().getName());
        }
    }

    private static void initDimensionActions(Scene scene) {
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN, KeyCodeCombination.SHIFT_DOWN)).
            setHandler((ActionEvent event) -> {

                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {

                    if (param.getControl() != null) {
                        JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());
                        if (controller != null && !controller.isDisableSavedDimensions()) {
                            controller.setDeleteDimensions(true);
                            Alerts.info(controller.getViewContext(), bundle.getString("ALERT_RESET_SIZE_ITEM"));
                        }
                    }

                    param.getEvent().consume();
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }).build(),
            JInvEvent.PlaceType.FILTER);
    }

    private static void initContextMenu(Scene scene) {
        SceneKeyManager.addAction(scene,
            new ActionBuilder().setKeyCombination(new KeyCodeCombination(KeyCode.SPACE, KeyCombination.CONTROL_DOWN)).
            setHandler((ActionEvent event) -> {

                JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
                try {

                    if (param.getControl() != null) {

                        JInvFXFormController controller = Controls.getControllerFromControl(param.getControl());
                        if ( controller != null ) {
                            ViewContext vc = controller.getViewContext();
                            if ( vc != null ) {
                                Stage stage = vc.getStage();
                                if ( stage != null ) {
                                    controller.getContextMenu()
                                              .show( stage, stage.getX() + stage.getWidth() / 2,
                                                            stage.getY() + stage.getHeight() / 2 );
                                }
                            }
                        }
                    }
                    param.getEvent().consume();
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }

            }).build(),
            JInvEvent.PlaceType.FILTER);
    }

    private static void initTableCellSelectMode(Scene scene) {

        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {

            try {
                if (event.isControlDown() && event.getCode().equals(KeyCode.S)) {

                    if (scene.getFocusOwner() instanceof JInvTable) {
                        JInvTable table = (JInvTable) scene.getFocusOwner();
                        table.getSelectionModel().setCellSelectionEnabled(true);
                        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                    }

                }
            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
            }
        });

        scene.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {

            try {
                if (event.getCode().equals(KeyCode.S)) {

                    if (scene.getFocusOwner() instanceof JInvTable) {
                        JInvTable table = (JInvTable) scene.getFocusOwner();
                        table.getSelectionModel().setCellSelectionEnabled(false);
                        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
                    }

                }
            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
            }

        });
    }

    private static void initExternalEditorActions(Scene scene) {

        addAction(scene, ExternalEditorManager.INSTANCE().getRemoveExtEditorSettingsAction());
    }

    private static void markHandler(ActionEvent event) {

        JInvEvent<KeyEvent> param = (JInvEvent<KeyEvent>) event;
        try {

            DSFXAdapter adapter = Controls.getDsAdapterFromControl(param.getEvent().getTarget());

            if (adapter != null && adapter.isEnableMark()) {
                adapter.revertMarkCurrentRow();
            }

            param.getEvent().consume();

        } catch (Throwable ex) {
            JInvErrorService.handleException(null, ex);
        }
    }
}
