package ru.inversion.fx.form.controls;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.slf4j.Logger;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.action.*;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.filter.JInvFilterToolBar;
import ru.inversion.fx.form.controls.renderer.CellEditorInfo;
import ru.inversion.fx.form.controls.renderer.Colorizer;
import ru.inversion.fx.form.controls.renderer.IColoredCell;
import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;
import ru.inversion.fx.form.controls.table.JInvTableContextMenuFactory;
import ru.inversion.fx.form.controls.table.TableViewContextMenuHelper;
import ru.inversion.fx.jinvtblexport.Export;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.ConsumerWithException;
import ru.inversion.utils.U;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.*;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.*;
import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.*;

/**
 *
 * @author antonovdi
 */
public class JInvTable<S> extends TableView<S> implements IJInvControl, IChoiceControl {

    //private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
    private static final PseudoClass NO_HEADER = PseudoClass.getPseudoClass( "noheader" );

    public static final String PROPERTY_BIG_TABLE              = "ru.inversion.big_table";
    public static final String PROPERTY_BIG_TABLE_NAME         = "ru.inversion.big_table_name";
    public static final String PROPERTY_CLEAR_WHEN_FILTER_LOST = "ru.inversion.cwfl";
    public static final String PROPERTY_FILTER                 = "ru.inversion.filter";

    private static final List<ActionFactory.ActionTypeEnum> readOnlyActions
            = Arrays.asList( ActionFactory.ActionTypeEnum.REFRESH, ActionFactory.ActionTypeEnum.VIEW );

    /** */
    private int previousIndex;

    /** */
    private JInvToolBar toolBar;

    /** */
    private final BooleanProperty columnManagerProperty = new SimpleBooleanProperty( this, "columnManagerProperty", true );

    /** */
    private final BooleanProperty visibleStatusBarProperty = new SimpleBooleanProperty(this, "visibleStatusBarProperty", false) {
        @Override
        protected void invalidated() {
            //перерисовываем таблицу
            refresh();
        }
    };

    /** */
    private final BooleanProperty checkNoDataFound = new SimpleBooleanProperty(this,"checkNoDataFound",true);

    /**
     * Индекс столбца по которому кликнули
     */
    private final IntegerProperty indexClickedColumnProperty = new SimpleIntegerProperty(this, "indexClickedColumnProperty", 0);

    /** Достаёт вложенные столбцы и учитывает фильтр, если таблица их поддерживает */
    public static ObservableList<? extends TableColumn<?, ?>> getChildColumns( final TableColumn<?, ?> tableColumn ) {
        TableView table = tableColumn.getTableView();
        return table instanceof JInvTable ?
                tableColumn.getColumns().filtered( ( (JInvTable) table ).getColumnFilter() ) :
                tableColumn.getColumns();
    }

    /**
     * Получаем индекс столбца по которому кликнули
     */
    public IntegerProperty indexClickedColumnProperty() {
        return indexClickedColumnProperty;
    }

    /** */
    private StringProperty textLabelProperty = new SimpleStringProperty("");

    private BooleanProperty showHeader = new SimpleBooleanProperty(true);

    private BooleanProperty showMarkColumn = new SimpleBooleanProperty(true);
    private ObjectProperty<TableColumn> markColumn = new SimpleObjectProperty<>();

    /**  */
    private BiConsumer<ActionEvent, IAction> beforeActionHandler;

    // Column Manager
    public BooleanProperty columnManagerProperty() {
        return columnManagerProperty;
    }

    public boolean isEnableColumnManager() {
        return columnManagerProperty.get();
    }

    public JInvToolBar getToolBar() {
        return toolBar;
    }

    public void setToolBar(JInvToolBar toolBar) {
        this.toolBar = toolBar;
    }

    public void setBigTableMode(boolean val) {
        getProperties().put(PROPERTY_BIG_TABLE, val);
    }

    public boolean isBigTableMode() {
        return (Boolean) getProperties().getOrDefault(PROPERTY_BIG_TABLE, false);
    }


    /** Признак, что необходимо после сброса всех фильтров с таблицы,
     *  не перезапрашивать данные, а очищать таблицу
     *  Не используется, решили всегда очищать таблицы,
     *  после сброса всех фильтров, JAVAKERNEL-1760
     *  29.09.2022
     */
    @Deprecated
    public boolean isClearWhenFilterLost() {
        return (Boolean) getProperties().getOrDefault(PROPERTY_CLEAR_WHEN_FILTER_LOST, false);
    }
    @Deprecated
    public void setClearWhenFilterLost(boolean val) {
        getProperties().put(PROPERTY_CLEAR_WHEN_FILTER_LOST, val);
    }

    public void setBigTableName(String bigTableName) {
        getProperties().put(PROPERTY_BIG_TABLE_NAME, bigTableName);
    }

    public String getBigTableName() {
        return (String) getProperties().get(PROPERTY_BIG_TABLE_NAME);
    }

    public JInvTable() {
        super();
        commonInit();
    }

    public JInvTable(ObservableList<S> items) {
        super(items);
        setContextMenu(new ContextMenu());
        commonInit();
    }

    private void commonInit()
    {
        readOnlyProperty().addListener( ( obs, oldV, newV ) -> internalSetReadOnly( obs.getValue() ) );

        this.setSkin( new JInvTableViewSkin(this) );

        this.getSelectionModel().getSelectedCells()
            .addListener((ListChangeListener.Change<? extends TablePosition> c) -> {
                if (c.getList().isEmpty()) {
                    return;
                }
                TableColumn tc = c.getList().get(0).getTableColumn();
                if (tc == null) {
                    return;
                }
                // Если у колонки есть родители, то чтобы верно определить index нужно взять родительскую колонку
                if (tc.getParentColumn() != null) {
                    indexClickedColumnProperty().set(this.getColumns().indexOf(tc.getParentColumn()));
                    return;
                }
                indexClickedColumnProperty().set(this.getColumns().indexOf(tc));
            });

        this.getSelectionModel().selectedIndexProperty()
            .addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                if (oldValue.intValue() != -1) {
                    previousIndex = oldValue.intValue();
                } else {
                    previousIndex = 0;
                }
            });

        initPlaceHolder();

        JInvKeyboardManager.initNaviationOnNode(this);
        new TableViewContextMenuHelper(this);

        showHeaderProperty().addListener((v,oldVal,newVal)->{
            if (oldVal == newVal){
                return;
            }
            pseudoClassStateChanged(NO_HEADER, !newVal);
        });

        initMarkColumnBinding();
    }

    public void initMarkColumnBinding() {

        markColumnProperty().addListener((v,oldValue,newValue)->{
            if (oldValue == newValue){
                return;
            }

            if (oldValue != null){
                oldValue.visibleProperty().unbindBidirectional(showMarkColumnProperty());
                this.getColumns().remove(oldValue);
            }

            if (newValue != null){
                ViewPrefAppService viewPrefService = null;
                try {
                    if (BaseApp.APP() != null){
                        viewPrefService = BaseApp.APP().getViewPrefService();
                    }
                } catch (AppException ignored) { }

                if( viewPrefService == null || viewPrefService.isMarkLeft() ) {
                    this.getColumns().add( 0, newValue );
                } else {
                    this.getColumns().add( newValue );
                }
                newValue.setVisible(isShowMarkColumn());
                newValue.visibleProperty().bindBidirectional(showMarkColumnProperty());
            }
        });
    }

    private void internalSetReadOnly( final Boolean isReadOnly ) {
        getActionList().forEach( action -> {
            if ( action.getActionType() == null || !readOnlyActions.contains(action.getActionType()) ){
                action.setEnabled( !isReadOnly );
            }
        } );
    }

    /** Фильтр столбцов.
     Не удовлетворяющие условию столбцы будут скрыты без возможности отображения и копирования данных */
    private Predicate<TableColumn<S, ?>> columnFilter = c -> true;

    /** Задаёт фильтр для метода getColumnsFiltered().
     Фильтр выдаёт все столбцы без поддержки фильтрации и фильтрует JInvTableColumn согласно переданному предикату
     @param filter будет выдавать только столбцы, соответствующие данному предикату
     */
    public void setColumnFilter( final Predicate<JInvTableColumn> filter ) {
        this.columnFilter = column -> {
            if ( !( column instanceof JInvTableColumn ) ) {
                return true;
            }
            return filter.test( (JInvTableColumn) column );
        };
        hideFilteredColumns();
    }

    public void hideFilteredColumns() {
        getAllColumns().forEach( col -> {
            if ( !getColumnFilter().test( col ) ){
                col.setVisible( false );
            }
        } );
    }

    /**
     Добавить правило раскраски для всей таблицы.
     Цвета, добавленные позже, перекрывают старые в случае коллизии

     @param styleExpr условие раскраски; если цвет менять не надо – возвращать null
     */
    public void addColor( final Function<IColoredCell<S>, Colorizer> styleExpr ) {
        forEachJinvColumn( column -> column.addColor( styleExpr ) );
    }

    /**
     Убрать всю раскраску, заданную через addColor() во всей таблице
     */
    public void clearColor(){
        forEachJinvColumn( JInvTableColumn::clearColor );
    }

    private void forEachJinvColumn( Consumer<JInvTableColumn> operation ){
        getAllColumns().forEach( column -> {
            if (column instanceof JInvTableColumn && column.getColumns().size() == 0){
                operation.accept( (JInvTableColumn) column );
            }
        } );
    }

    /** Текущее значение фильтра для метода getColumnsFiltered() */
    public Predicate<TableColumn<S, ?>> getColumnFilter() {
        return columnFilter;
    }

    /** Выдаёт столбцы верхнего уровня, отфильтрованные с помощью getColumnFilter().
     Внимание! вложенные столбцы нужно фильтровать самому */
    public ObservableList<TableColumn<S, ?>> getColumnsFiltered() {
        return getColumns().filtered( getColumnFilter() );
    }

    /**
     * Stimul
     */
    public void showExportDialog() {

        TaskContext tc = getDataSetAdapter().getTaskContext();
        ViewContext vc = getController().getViewContext();

//        ru.inversion.fx.jinvstimul.Export.exportDialog(this, tc, vc);
        Export.exportDialog( getController(),this, tc, vc  );
    }

    /**
     Своя реализация коммита по изменению ячейки
     Используется в JInvCheckBoxCellEditor
     @see ru.inversion.fx.form.controls.renderer.JInvCheckBoxCellEditor
     @param userCommit своя реализация коммита, кидающая Exception. самому ловить нет необходимости
     */
    public void setColumnUserCommit(String fieldName, ConsumerWithException<CellEditorInfo<S, ?>> userCommit) {
        Optional<TableColumn<S, ?>> result = getColumns().stream().filter((TableColumn col) -> {
            boolean ret = false;
            if (col instanceof JInvTableColumn && fieldName != null) {
                if (fieldName.equalsIgnoreCase(((JInvTableColumn) col).getFieldName())) {
                    ret = true;
                }
            }
            return ret;
        }).findFirst();

        if (result.isPresent()) {
            JInvTableColumn col = (JInvTableColumn) result.get();
            col.setUserCommit(userCommit);
        }
    }

    /**
     *
     */
    public void markRangeOfRows() {

        try {

            DSFXAdapter adapter = Controls.getDsAdapterFromControl(JInvTable.this);

            if (adapter != null && adapter.isEnableMark() && adapter.getDataSet() instanceof XXIDataSet) {

                XXIDataSet dataSet = (XXIDataSet) adapter.getDataSet();
                int previousPosition = previousIndex;
                int currentPosition = getSelectionModel().getSelectedIndex();

                if (previousPosition < currentPosition) {
                    for (int i = previousPosition; i <= currentPosition; i++) {
                        dataSet.markRow(i);
                    }
                } else if (previousPosition > currentPosition) {
                    for (int i = previousPosition; i >= currentPosition; i--) {
                        dataSet.markRow(i);
                    }
                }
            }
        } catch (Throwable th) {
            JInvErrorService.handleException(getScene().getWindow(), th);
        }
    }

    /**
     *
     */
    @Override
    public Control setLabel( Label label ) {

        if( label != null ) {
            label.setLabelFor(this);
            textLabelProperty.bind( label.textProperty() );
        }
        return this;
    }

    @Override
    public Label getLabel() {
        return null;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public void setFieldName(String fieldName) { }

    private List<IAction> actionList = new ArrayList();

    public void initKeyBoardActions() {

        actionList.forEach( (IAction action) -> { initBeforeActionHandler(action); });
    }

    public List<IAction> getActionList() {
        return actionList;
    }

    private IAction initBeforeActionHandler( IAction action)
    {
        IAction result = action;

        BiConsumer<ActionEvent, IAction> handlerBefore = null;

        if( getToolBar() != null && getToolBar().getMultipleUse() ) {
            handlerBefore = getToolBar().getBeforeActionHandler();
        }

        if (handlerBefore == null && this.beforeActionHandler != null) {
            handlerBefore = this.beforeActionHandler;
        }

        final BiConsumer<ActionEvent, IAction> handlerBeforeFinal = handlerBefore;

        if( handlerBeforeFinal != null )
        {
            JInvAction handler = ((JInvAction) action);
            IAction wrapAction = new ActionBuilder(handler).setParallel(false).setNextAction(action).setHandler(null)
                .setHandlerWithNextAction(handlerBeforeFinal).build();
            JInvKeyboardManager.addAction(JInvTable.this, wrapAction);
            result = wrapAction;
        }
        else
        {
            JInvKeyboardManager.addAction( JInvTable.this, action );
        }

        return result;
    }

    @Override
    public void setAction(IAction action) {

        if( action != null ) {
            actionList.add( action);
        }

        // if (beforeActionHandler != null && action instanceof JInvAction) {
        //
        // JInvAction handler = ((JInvAction) action);
        // IAction wrapAction = new
        // ActionBuilder(handler).setParallel(false).setNextAction(action).setHandler(null).setHandlerWithNextAction(beforeActionHandler).build();
        // JInvKeyboardManager.addAction(this, wrapAction);
        // } else {
        // JInvKeyboardManager.addAction(this, action);
        // }
    }

    public void setAction(ActionFactory.ActionTypeEnum type, EventHandler<ActionEvent> handle, boolean parallel) {
        setAction(type, handle, parallel, null, null);
    }

    public void setAction(ActionFactory.ActionTypeEnum type, EventHandler<ActionEvent> handle, boolean parallel,
        Integer id) {
        setAction(type, handle, parallel, id, null);
    }

    public void setAction(ActionFactory.ActionTypeEnum type, EventHandler<ActionEvent> handle, boolean parallel,
        Integer id, SecurityStrategyEnum strategy) {
        IAction action = ActionFactory.getAction(type, handle, parallel, id, strategy);
        setAction(action);
    }

    public void setAction(ActionFactory.ActionTypeEnum type, EventHandler<ActionEvent> handler) {
        setAction(type, handler, false);
    }

    public void setAction(ActionFactory.ActionTypeEnum type, EventHandler<ActionEvent> handler, Integer id) {
        setAction(type, handler, false, id);
    }

    public IAction getAction(ActionFactory.ActionTypeEnum type) {
        IAction action = JInvKeyboardManager.getAction(this, type);
        return action;
    }

    /** */
    public void initToolBar( ) {

        if( toolBar != null )
        {
            toolBar.getItems()
                    .filtered( t -> t instanceof JInvButton && ((JInvButton) t).getOnAction() != null )
                    .forEach((Node t) -> {

                JInvButton button = (JInvButton) t;
                EventHandler handler = button.getOnAction();

                if( handler instanceof JInvAction && ((JInvAction)handler).getHandler()==null ) {

                    if( toolBar.getMultipleUse() )
                    {
                        button.setOnAction( new ToolBarListener(button) );
                    }
                    else
                    {
                        final IAction action = JInvKeyboardManager.getAction( JInvTable.this, button.getType() );

                        if( action != null )
                        {
                            button.setAction  ( action );
                            button.setOnAction( event -> {
                                JInvEvent wrapEvent = new JInvEvent( button, JInvEvent.PlaceType.FILTER, event );
                                wrapEvent.setAction(action);
                                action.handle(wrapEvent);
                            } );
                        }
                    }
                }
                else if (!ToolBarListener.class.isAssignableFrom( handler.getClass()) )
                {
                    if (handler instanceof JInvAction) {
                        button.setOnAction(initBeforeActionHandler((JInvAction) handler));
                    } else {
                        button.setOnAction(
                            initBeforeActionHandler(new ActionBuilder()
                                    .setActionType( button.getType() )
                                    .handler( handler )
                                    .setParallel( false )
                                    .build()));
                    }
                }
            });
        }
    }

    private void fillListColumnsRecursive(ObservableList<TableColumn<S, ?>> listColums, List<TableColumn> resultList,
        Predicate<TableColumn> predicateForParent, Predicate<TableColumn> predicateForLeaves) {

        listColums.stream().forEach((TableColumn t) -> {
            if (!t.getColumns().isEmpty()) {

                if (predicateForParent.test(t)) {
                    resultList.add(t);
                }

                fillListColumnsRecursive(t.getColumns(), resultList, predicateForParent, predicateForLeaves);
            } else if (predicateForLeaves.test(t)) {
                resultList.add(t);
            }
        });
    }

    public List<TableColumn> getAllBottomColumnsWithFieldName() {

        List<TableColumn> list = new ArrayList<>();
        fillListColumnsRecursive(getColumns(), list, (TableColumn t) -> false,
                (TableColumn t) -> Controls.getFieldNameFromTableColumn(t) != null);
        return list;
    }

    public List<TableColumn> getAllColumns() {
        List<TableColumn> list = new ArrayList<>();
        fillListColumnsRecursive(getColumns(), list, t -> true, t -> true );

        return list;
    }

    @Override
    public void setToolTipText(String toolTipText) {
    }

    @Override
    public String getToolTipText() {
        return null;
    }

    public void initMouse() {
        this.setOnMouseClicked((MouseEvent event) -> {
            // Переключатель выбранной ячейки, по аналогии с обычным кликом левой кнопкой мыши
            if (event.getButton().equals(MouseButton.SECONDARY)) {
                if(!this.getSelectionModel().isCellSelectionEnabled()) {
                    EventTarget target = event.getTarget();
                    // Если щелкнули не на колонку, а на ее содержание, тогда достаем выбранную ячейку исходя из родителя
                    if (!(target instanceof TableCell) && target instanceof Node) {
                        target = ((Node) target).getParent();
                    }
                    if (target instanceof TableCell) {
                        final TableCell cell = (TableCell) target;
                        final int index = cell.getTableRow().getIndex();
                        final TableColumn tableColumn = cell.getTableColumn();
                        getFocusModel().focus(index, tableColumn);
                        getSelectionModel().select(index, tableColumn);
                    }
                }
            }

            if(event.isControlDown() && !event.isAltDown() && event.getButton().equals(MouseButton.PRIMARY)) {
                if( !this.getSelectionModel().isCellSelectionEnabled() )
                {
                    DSFXAdapter adapter = Controls.getDsAdapterFromControl(this);
                    adapter.revertMarkCurrentRow();
                }
            }

            if( event.isShiftDown() && !event.isControlDown() && event.getButton().equals(MouseButton.PRIMARY) ) {
                if (
                    !this.getSelectionModel().isCellSelectionEnabled()
                    &&
                    event.getTarget() instanceof TableCell
                )
                {
                    markRangeOfRows();
                }
            }
        });
    }

    /**
     * Признак что необходимо сохранять пометку
     */
    private final BooleanProperty savePrevMarkedRowsProperty =
            new SimpleBooleanProperty(this, "savePrevMarkedRowsProperty", true );

    public BooleanProperty savePrevMarkedRowsProperty() {
        return savePrevMarkedRowsProperty;
    }

    public boolean isSavePrevMarkedRows() { return savePrevMarkedRowsProperty().get(); }

    public void setSavePrevMarkedRows( boolean v ) { savePrevMarkedRowsProperty().set(v); }

    /**
     * Добавляем стандартные кнопки с тулбара в контекстное меню
     */
    public void initContextMenu() {
        initContextMenu( null );
    }

    /**
     * Добавляем стандартные кнопки с тулбара в контекстное меню
     */
    public void initContextMenu( Consumer<JInvTable<?>> contextMenuInitializer ) {


        ContextMenu contextMenu = this.getContextMenu();

        if( contextMenu == null ) {
            contextMenu = new ContextMenu();
            this.setContextMenu( contextMenu );
        }

        if ( contextMenuInitializer != null) {
            contextMenuInitializer.accept(this);
        }

        Menu tableMenu = JInvTableContextMenuFactory.createTableMenu(this);

        boolean first = true;

        Iterator< IAction > iter = JInvKeyboardManager.getActionListIterator(this);

        boolean allowEditOperations = getController() == null ||  U.in( getController().getFormMode(), VM_NONE, VM_INS, VM_EDIT );

        while( iter.hasNext() ) {

            IAction action = iter.next();

            if( action.getActionType() == null )
                continue;

            if( !allowEditOperations || U.notIn( action.getActionType(), CREATE, CREATE_BY, UPDATE, DELETE, CLEAR ) )
                continue;

            JInvMenuItem item = new JInvMenuItem( action.getTitle(), ActionFactory.getLabel( action.getIcon() ), this );

            item.setAction( action );

            if( first ) {

                first = false;

                tableMenu.getItems().add( new SeparatorMenuItem( ) );
            }

            if ( tableMenu.getItems().filtered( i -> i instanceof JInvMenuItem && ( (JInvMenuItem) i ).getLastAction() == action ).isEmpty() ){
                tableMenu.getItems().add( item );
            }

        }

        contextMenu.getItems().add( tableMenu );
    }

    @Override
    public void initChoiceBehavior(AbstractBaseController<?> controller) {
        //Выбор значений в LOV таблице
        setOnMousePressed( (MouseEvent event) -> {
            if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
                chooseAndClose( controller );
            }
        });
        setOnKeyPressed( event -> {
            if (event.getCode() == KeyCode.F9 || event.getCode() == KeyCode.ENTER && event.isControlDown()) {
                chooseAndClose( controller );
            }
        } );
    }

    private void chooseAndClose( final AbstractBaseController<?> controller ) {
        if (controller.isChoiceEnabled()) {
            controller.close(AbstractBaseController.FormReturnEnum.RET_OK);
        }
    }

    private void initPlaceHolder() {

        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);

        Label labelSmallPart = new Label(fore.getString("LABEL_TABLE_PLACEHOLDER_SMALL_PART"));
        labelSmallPart.setTextAlignment(TextAlignment.CENTER);
        labelSmallPart.getStyleClass().add("table-placeholder-small-part");

        Label labelBigPart = new Label(fore.getString("LABEL_TABLE_PLACEHOLDER_BIG_PART"));
        labelBigPart.setTextAlignment(TextAlignment.CENTER);
        labelBigPart.getStyleClass().add("table-placeholder-big-part");
        box.getChildren().addAll(labelSmallPart, labelBigPart);
        setPlaceholder(box);

        // Показываем холдер, если линейные размеры таблицы превышают размеры холдера в два раза
        box.visibleProperty().bind(labelSmallPart.widthProperty().multiply(2).
            lessThan(widthProperty()).and(labelBigPart.heightProperty().add(labelSmallPart.heightProperty()).
                multiply(2).lessThan(heightProperty())));

    }

    private class ToolBarListener implements EventHandler<ActionEvent> {

        private final JInvButton button;

        public ToolBarListener(JInvButton button) {
            this.button = button;
        }

        @Override
        public void handle(ActionEvent event) {
            Scene scene = getScene();
            if (scene == null){
                Alerts.error(BaseApp.APP().getPrimaryViewContext(), String.format(fore.getString("TOOLBAR_BOUND_TO_INVISIBLE_TABLE"), getId()));
                return;
            }
            Node focusNode = scene.getFocusOwner();
            IAction action = JInvKeyboardManager.getAction( focusNode, button.getType() );

            if (action == null && focusNode instanceof IJInvControl){
                DSFXAdapter<Object> dsfx = ((IJInvControl) focusNode).getDataSetAdapter();
                if (dsfx != null && dsfx.getTable() != null){
                    action = JInvKeyboardManager.getAction( dsfx.getTable(), button.getType() );
                }
            }

            if( action != null ) {
                JInvEvent wrapEvent = new JInvEvent(focusNode, JInvEvent.PlaceType.FILTER, event);
                wrapEvent.setAction(action);
                action.handle(wrapEvent);
            }
            else {

                final ActionFactory.ActionTypeEnum type = button.getType();

                if( type != null )
                    Alerts.info( JInvTable.this.getScene().getWindow(), MessageFormat.format( fore.getString("TABLE_OPER_NOT_SUPPORTED_EX"), type.getName().replace("…","") )  );
                else
                    Alerts.info( JInvTable.this.getScene().getWindow(), fore.getString("TABLE_OPER_NOT_SUPPORTED") );
            }
        }
    }

    @Override
    public DSFXAdapter<S> getDataSetAdapter() {

        return Controls.<S>getDsAdapterFromControl(this);
    }

    /** */
    public boolean isEnd( )
    {
        boolean result = false;

        if( getDataSetAdapter() != null && getDataSetAdapter().isEnd() ) {

            if( getSelectionModel().getSelectedIndex() == this.getItems().size() - 1 ) {
                result = true;
            }
        }

        return result;
    }

    public boolean isFirstRow() {
        return getSelectionModel().getSelectedIndex() == 0;
    }

    /**
     *
     */
    public void executeQuery() {
        executeQuery(null);
    }

    /**
     * Метод вызывает метод обновления у dataSet в отдельном потоке, если он привязан к таблице
     *
     * @param clb Колбек, который вызывается после того, как таблица обновилась.
     */
    public void executeQuery(BiConsumer<Boolean, Throwable> clb) {
        executeQuery(clb, null);
    }

    public void executeQuery(boolean readAllRecords) {
        executeQuery(null, readAllRecords);
    }

    public void executeQuery(BiConsumer<Boolean, Throwable> clb, IFormStateListener stateListener) {
        executeQuery(clb, stateListener, true);
    }

    public void executeQuery(BiConsumer<Boolean, Throwable> clb, boolean readAllRecords) {
        executeQuery(clb, readAllRecords, true);
    }

    public void executeQueryBlock() throws DataSetException {
        executeQuery(null, null, false);
    }

    public void executeQueryBlock(boolean readAllRecords) throws DataSetException {
        executeQuery(null, null, false, readAllRecords);
    }

    public void executeQuery(BiConsumer<Boolean, Throwable> clb, boolean readAllRecords, boolean parallel) {
        executeQuery(clb, null, parallel, readAllRecords);
    }

    public void executeQuery(BiConsumer<Boolean, Throwable> clb, IFormStateListener stateListener, boolean parallel) {
        executeQuery(clb, stateListener, parallel, false);
    }

    public void executeQuery(BiConsumer<Boolean, Throwable> clb, IFormStateListener stateListener, boolean parallel,
        boolean readAllRecords) {

        DSFXAdapter adapter = getDataSetAdapter();

        if (adapter != null && adapter.getDataSet() != null) {
            if (stateListener == null && getController() != null) {
                stateListener = getController();
            }

            adapter.executeQuery(clb, stateListener, parallel, readAllRecords);
        }
    }

    public void setSaveMark(boolean val) throws AppException {
        if (BaseApp.APP() != null) {
            BaseApp.APP().getViewPrefService().setSaveMark(val);
        }
    }

    /**
     * Метод возвращающий состояние глобальной, на данный момент переменной для всех таблиц сбрасывания пометок после применения фильтра
     *
     * @return
     * @throws AppException
     */
    public boolean getSaveMark() throws AppException {
        if (BaseApp.APP() != null) {
            return BaseApp.APP().getViewPrefService().getSaveMark();
        } else {
            return false;
        }
    }

    /**
     * Установка видимости СтатусБара
     * @return BooleanProperty
     */
    public BooleanProperty visibleStatusBarProperty() {
        return visibleStatusBarProperty;
    }

    /** */
    public BooleanProperty checkNoDataFound(){
        return checkNoDataFound;
    }

    /**
     * Метод получения статус бара
     *
     * @return JInvToolBar
     */
    public DSInfoBar getStatusToolbar() {
        if (getSkin() instanceof JInvTableViewSkin) {
            return ((JInvTableViewSkin) getSkin()).getStatusBar();
        }

        return null;
    }

    @Override
    public StringProperty labelTextProperty() {
        return textLabelProperty;
    }

    /**
     * Метод получения фильтрбара
     *
     * @return JInvFilterToolBar
     */
    public JInvFilterToolBar getFilterToolbar() {
        if (getSkin() instanceof JInvTableViewSkin) {
            return ((JInvTableViewSkin) getSkin()).getFilterToolBar();
        }
        return null;
    }

    /**
     *
     */
    public boolean isVisibleRow(int index) {

        boolean result = false;

        TableViewSkin<?> skin = (TableViewSkin) getSkin();
        if (skin != null) {
            VirtualFlow<?> flow = (VirtualFlow) skin.getChildren().get(1);
            if (flow != null) {

                IndexedCell f = flow.getFirstVisibleCellWithinViewPort();
                IndexedCell l = flow.getLastVisibleCellWithinViewPort();

                if (f != null && l != null) {
                    int firstVisibleRow = f.getIndex();
                    int lastVisibleRow = l.getIndex();

                    result = index >= firstVisibleRow && index <= lastVisibleRow;
                }
            }
        }

        return result;
    }

    public <P> boolean findRow(BiPredicate<S, P> predicat, P arg2) {

        DSFXAdapter adapter = getDataSetAdapter();
        if (adapter != null) {
            return adapter.findRow(predicat, arg2);
        } else {
            throw new IllegalArgumentException(fore.getString("NOT_ADAPTER_IN_CONTROL"));
        }
    }

    /**
     *
     */
    public void showDsInfoDialog() {

        DSFXAdapter adapter = getDataSetAdapter();
        if (adapter != null) {

            ViewContext vc = null;

            JInvFXFormController<?> controller = getController();
            if (controller != null) {
                vc = controller.getViewContext();
            } else {
                vc = ViewContext.of(getScene().getWindow());
            }

            adapter.showDSDialogInfo(vc);
        }
    }

    public BiConsumer<ActionEvent, IAction> getBeforeActionHandler() {
        return beforeActionHandler;
    }

    public void setBeforeActionHandler(BiConsumer<ActionEvent, IAction> beforeActionHandler) {
        this.beforeActionHandler = beforeActionHandler;
    }

    private VirtualScrollBar getScroll(Orientation orientation) {
        return (VirtualScrollBar) this.lookupAll(".scroll-bar").
            stream().filter((Node t) -> t instanceof VirtualScrollBar
                && ((VirtualScrollBar) t).getOrientation().equals(orientation)).
            findFirst().orElse(null);
    }

    public VirtualScrollBar getScrollVertical() {
        return getScroll(Orientation.VERTICAL);
    }

    public VirtualScrollBar getScrollHorisontal() {
        return getScroll(Orientation.HORIZONTAL);
    }

    public double getScrollVerticalPosition() {
        VirtualScrollBar scrollBar = getScrollVertical();
        if (scrollBar != null) {
            return scrollBar.getValue();
        } else {
            return 0.0;
        }
    }

    public void setScrollVerticalPosition(double position) {
        VirtualScrollBar scrollBar = getScrollVertical();
        if (scrollBar != null) {
            scrollBar.setValue(position);
        }
    }

    public double getScrollHorisontalPosition() {
        VirtualScrollBar scrollBar = getScrollHorisontal();
        if (scrollBar != null) {
            return scrollBar.getValue();
        } else {
            return 0.0;
        }
    }

    public void setScrollHorisontalPosition(double position) {
        VirtualScrollBar scrollBar = getScrollHorisontal();
        if (scrollBar != null) {
            scrollBar.setValue(position);
        }
    }

    /** */
    public void moveToLast() {
        getDataSetAdapter().moveToLast();
    }

    /** */
    public void moveToFirst() {
        getDataSetAdapter().getDataSet().setCurrentRowNum(0);
    }

    /**
     * Показывать ли столбец пометки
     * По умолчанию true
     * Не будет показан, если таблица не поддерживает пометку
     */
    public boolean isShowMarkColumn() {
        return showMarkColumn.get();
    }

    /**
     * Показывать ли столбец пометки
     * По умолчанию true
     * Не будет показан, если таблица не поддерживает пометку
     */
    public BooleanProperty showMarkColumnProperty() {
        return showMarkColumn;
    }

    /**
     * Показывать ли столбец пометки
     * По умолчанию true
     * Не будет показан, если таблица не поддерживает пометку
     */
    public void setShowMarkColumn(boolean showMarkColumn) {
        this.showMarkColumn.set(showMarkColumn);
    }

    /**
     * Столбец с пометкой
     */
    public TableColumn getMarkColumn() {
        return markColumn.get();
    }

    /**
     * Столбец с пометкой
     */
    public ObjectProperty<TableColumn> markColumnProperty() {
        return markColumn;
    }

    /**
     * Столбец с пометкой
     */
    public void setMarkColumn(TableColumn markColumn) {
        this.markColumn.set(markColumn);
    }

    /**
     * Показывать ли заголовок таблицы с названиями столбцов
     */
    public boolean isShowHeader() {
        return showHeader.get();
    }

    /**
     * Показывать ли заголовок таблицы с названиями столбцов
     */
    public BooleanProperty showHeaderProperty() {
        return showHeader;
    }

    /**
     * Показывать ли заголовок таблицы с названиями столбцов
     */
    public void setShowHeader(boolean showHeader) {
        this.showHeader.set(showHeader);
    }
}
