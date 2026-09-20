package ru.inversion.dataset.fx;

import com.sun.javafx.beans.event.AbstractNotifyListener;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.*;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.dataset.aggr.IAggregator;
import ru.inversion.dataset.impl.XXIDsDao;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.dataset.mark.MarkModeEnum;
import ru.inversion.db.entity.ContentTypeEnum;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.filter.JInvFilterToolBar;
import ru.inversion.fx.form.controls.filter.impl.FilterManager;
import ru.inversion.fx.form.controls.progress.ProgressCallback;
import ru.inversion.fx.form.controls.progress.ProgressTaskExecutor;
import ru.inversion.fx.form.controls.renderer.JInvCellFactoryProvider;
import ru.inversion.fx.form.controls.renderer.JInvCheckBoxCellEditor;
import ru.inversion.fx.form.controls.renderer.JInvTableCellMark;
import ru.inversion.fx.form.controls.sortbutton.OrderByManager;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Holder;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import java.util.stream.Collectors;

import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import static ru.inversion.dataset.DataSetRowEvent.RowOperationEnum.INSERT;
import static ru.inversion.dataset.DataSetRowEvent.RowOperationEnum.UPDATE;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_EDIT;
import static ru.inversion.fx.form.AbstractBaseController.FormReturnEnum.RET_OK;
import static ru.inversion.fx.form.controls.JInvTableColumn.*;

/**
 * Адаптер для связывания DataSet с JInvTable
 * @author sulimoff
 * @param <T> тип записи
 */
public class DSFXAdapter<T> extends DSControlAdapter<T> implements IDataSetListener, IDataSetMarkListener<T> {

    //static zone

    /** */
    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");

    /** */
    private static Logger logger = LoggerFactory.getLogger(DSFXAdapter.class);

    /** */
    public static final String PROPERTY_DATA_SET_ADAPTER = "ru.inversion.dsfx";

    /** */
    public static final String PROPERTY_ENABLE_FILTER = "ru.inversion.enableFilter";

    /** Ширина колонки с пометкой */
    public static final int MARK_COLUMN_WIDTH_DEFAULT = 40;

    /** */
    private final Object DATA_SET_LOCK = new Object();

    /**
     * Декоратор для фильтра F7/F8.
     * <p>
     * Вызываемых для таблиц.
     * Позволяет менять содержимое параметров фильтра, формируемое по умолчанию из класса строки DataSet, для более тонкой настройки.
     *
     * Имеет методы вызываемые перед показом диалога фильтра и вызываемые сразу после закрытия диалога.
     * Методы позволяют менять коллекцию и содержимое установленных элементов фильтра.
     *
     * @see F7FilterItem
     *
     * @author sulimoff
     */
    public interface IFilterModifier {

        /**
         * Метод вызываемый перед показом диалога фильтра.
         * <p>
         * @param filterList список фильтра, которые будут показаны на форме фильтра.
         * @return список после применения пользовательского обработчика
         */
        @Deprecated
        default List<F7FilterItem> modifyBefore(List<F7FilterItem> filterList) {
            return filterList;
        }

        /**
         * Метод вызываемый перед показом диалога фильтра.
         * <p>
         * @param filterMap карта, в которой ключи - группы фильтров, а значения - наборы включенных в оные элементов
         */
        default void modifyBefore ( Map<F7FilterGroup, Set<F7FilterItem>> filterMap ) {
            modifyBefore (filterMap.values ()
                                   .stream ()
                                   .flatMap (coll -> coll.stream ())
                                   .collect (Collectors.toList ()));
        }

        /**
         * Метод вызываемый после показа диалога фильтра.
         * <p>
         * @param filterList список фильтра, которые будут применены к установленному {@code ISQLDataSet}
         * @return список после применения пользовательского обработчика
         */
//        default List<F7FilterItem> modifyAfter(List<F7FilterItem> filterList) {
        default List<? extends IFilterItem> modifyAfter ( List<F7FilterItem> filterList ) {
            return filterList;
        }
    }

    /** Признак что будет использоваться пометка для таблицы. */
    private final BooleanProperty enableMarkProperty = new SimpleBooleanProperty( this, "enableMark", false );

    /** Свойство конца данных, при подкачке. */
    private final BooleanProperty eof = new SimpleBooleanProperty(false);

    /** Декоратор для фильтра. */
    private IFilterModifier filterModifier;

    /** Менеджер сортировки. */
    private OrderByManager<T> orderByManager;

    /** Список групп для F7 фильтра */
    private List<F7FilterGroup> listFilterGroup = new ArrayList<>();

    /** */
    private TableView<T> tableView;

    /** */
    private boolean enableF7FilterDialog = true;

    /** */
    private boolean ignoreCheckScrolling = false;

    /** Игнорируется ли проверка на видимость записи в таблице перед позиционированием */
    public boolean isIgnoreCheckScrolling() {
        return ignoreCheckScrolling;
    }

    /** Игнорировать ли проверку на видимость записи в таблице перед позиционированием */
    public void setIgnoreCheckScrolling(boolean ignoreCheckScrolling) {
        this.ignoreCheckScrolling = ignoreCheckScrolling;
    }

    /** */
    final private BooleanProperty savePrevMarkedRowsProperty = new SimpleBooleanProperty(this, "savePrevMarkedRows", true);

    /** Скрыть/показать тулбар с кнопками фильтра */
    private final BooleanProperty enableToolbarFilterProperty = new SimpleBooleanProperty( this, "enableToolbarFilter", false ){
        @Override
        protected void invalidated() {
            //перерисовываем таблицу
            if (getTable() != null) {
                getTable().refresh();
            }
        }
    };

    /** */
    final private BooleanProperty bulkAppend = new SimpleBooleanProperty( this, "bulkAppend", false );

    /** */
    public DSFXAdapter() {
        super();
    }

    public BooleanProperty enableToolbarFilterProperty() { return enableToolbarFilterProperty; }

    /** */
    public void setEnableFilter( boolean val ) 
    {
        this.enableToolbarFilterProperty.set( val );
        
// расширение автофильтров на формы с самогенерируемыми именами        
        if (getDataSet () != null && getDataSet ().getProperty ("form_name") == null)
        {
            JInvFXFormController<?> controllerFromControl = Controls.getControllerFromControl (tableView);
            if (controllerFromControl != null)
                getDataSet ().setProperty ("form_name", controllerFromControl.getViewContext ().getFormNameForFilter ());
        }
    }

    /**
     * Установка фильтра + задаем имя формы + имя блока.
     */
    public void setEnableFilter( boolean enable, String formName, String blockName ) {

        enableToolbarFilterProperty.set (enable);
//        setEnableFilter(enable);

        JInvFXFormController controllerFromControl = Controls.getControllerFromControl(tableView);

        if( controllerFromControl != null )
            controllerFromControl.getViewContext().setFormNameForFilter(formName);

        if( getDataSet() != null ) {
            getDataSet().setName(blockName);
            getDataSet().setProperty("form_name", formName);
        }
    }

    /** */
    public boolean isEnableFilter() {
        return this.enableToolbarFilterProperty.get();
    }

    /** */
    public void refresh() {
        getTable().refresh();
    }

    /** */
    public TableView<T> getTable() {
        return tableView;
    }

    /** */

    public boolean isEnableF7FilterDialog() {
        return enableF7FilterDialog;
    }
    /** */
    public void setEnableF7FilterDialog(boolean enableF7FilterDialog) {
        this.enableF7FilterDialog = enableF7FilterDialog;
    }

    /** */
    public TaskContext getTaskContext() {

        if( isSQL() )
            return ( (SQLDataSet<T>) getDataSet() ).getTaskContext();

        JInvFXFormController<?> controller = Controls.getControllerFromControl(tableView);
        if( controller != null )
            return controller.getTaskContext();

        return null;
    }

    /** */
    @Override
    protected void afterBindControl( Control control, IEntityProperty<T,?> entityProperty, Callback<T, ? extends Object> callBack)
    {
        addColumnToTableByControl( control, entityProperty, callBack );
    }

    /**
     *
     */
    public DSFXAdapter bindTable(IDataSet<T> dataSet, TableView<T> tableView, boolean enableMark) throws Exception {
        return bindTable(dataSet, tableView, null, enableMark);
    }

    /** */
    public DSFXAdapter bindTable(IDataSet<T> dataSet, TableView<T> tableView, ICellValueChangeListener cellValueChangeListener) throws Exception {
        return bindTable(dataSet, tableView, cellValueChangeListener, false);
    }

    /** Набор имен полей столбцов TableView участвующие в bindings */
    final private Set<String> columnFieldsSet = new HashSet<>();

    /** */
    public DSFXAdapter bindTable( IDataSet<T> dataSet, TableView<T> tableView, ICellValueChangeListener cellValueChangeListener, boolean enableMark ) throws Exception {

        if( dataSet == null || tableView == null )
            throw new IllegalArgumentException("dataSet or table is null");

        if( this.dataSet != null )
            throw new IllegalStateException("dataSet already binded!");

        this.dataSet = dataSet;
        this.dataSet.addRowListener(this);
        this.dataSet.addNavigationListener(this);
        this.dataSet.addDataSetListener(this);

        this.tableView = tableView;
//
        this.dataSet.setProperty( "ds.handleAutoFilter", autoFilterMaker( ViewContext.of( ViewContext.tryGetWindow(tableView)),getTaskContext()) );

        this.tableView.addEventHandler( KeyEvent.KEY_RELEASED, ( KeyEvent event ) -> refreshCurrentRow() );
        this.tableView.addEventFilter ( MouseEvent.MOUSE_RELEASED,( MouseEvent event ) -> refreshCurrentRow() );
        this.tableView.addEventFilter ( MouseEvent.MOUSE_PRESSED, ( MouseEvent event ) -> refreshCurrentRow() );
        this.tableView.addEventFilter ( MouseEvent.DRAG_DETECTED, ( MouseEvent event ) -> refreshCurrentRow() );

        this.tableView.getColumns().addListener(new ListChangeListener<TableColumn<T, ?>>() {

            private boolean insideChanging = false;

            @Override
            public void onChanged( Change<? extends TableColumn<T, ?>> change )
            {
                // Если мы уже внутри обновления, игнорируем, чтобы избежать зацикливания
                if(insideChanging)
                    return;

                insideChanging = true;

                try {

                    while (change.next())
                    {
                        // 1. Удаление элементов
                        if (change.wasRemoved())
                        {
                            for(TableColumn<T, ?> column : change.getRemoved())
                            {
                                String filedName = (String) column.getProperties().get(COLUMN_FIELD_NAME);
                                if( S.isNullOrEmpty(filedName) )
                                    columnFieldsSet.remove(filedName);
                            }
                        }

                        // 2. Добавление элементов
                        if( change.wasAdded())
                        {
                            for( TableColumn<T, ?> column : change.getAddedSubList() )
                            {
                                String filedName = (String) column.getProperties().get(COLUMN_FIELD_NAME);
                                if( S.isNullOrEmpty(filedName) )
                                    columnFieldsSet.add(filedName);
                            }
                        }
                    }
                } finally {
                    insideChanging = false;
                }
            }
        });

        final VirtualScrollBar vsb = (VirtualScrollBar) tableView.lookup(".scroll-bar");

        tableView.addEventFilter( KeyEvent.KEY_PRESSED, event ->
        {
            //Подгружаем записи по PageDown, если вдруг оказались на последней записи
            if ( event.getCode() == KeyCode.PAGE_DOWN && vsb.getValue() >= vsb.getMax() ) {
                try {
//            logger.info( " -- SWAPPING on PGDN --" );
                    boolean isEof = !dataSet.swappingData();
                    eof.set( isEof );
                    if ( !isEof ) {
                        Platform.runLater( () -> tableView.getSelectionModel().selectLast() );
                    }
                } catch ( DataSetException ex ) {
                    JInvErrorService.handleException( tableView.getScene().getWindow(), ex );
                }
            }

            //Игнорим HOME/END
            if( event.getCode() == KeyCode.HOME || event.getCode() == KeyCode.END ) {
                event.consume();
            }
        });
        //
        tableView.focusedProperty().addListener(new AbstractNotifyListener() {
            //Сработали разок – и хватит
            boolean triggeredOnce = false;

            @Override
            public void invalidated(Observable observable) {
                if ( triggeredOnce ){
                    return;
                }

                // by psh
                vsb.valueProperty().addListener (

                    new ChangeListener< Number >() {

                        {
                            final ChangeListener<Number> _this = this;

                            bulkAppend.addListener(new ChangeListener< Boolean >() {
                                @Override
                                public void changed( ObservableValue< ? extends Boolean > observable, Boolean oldValue, Boolean newValue ) {
                                    if( newValue )
                                        vsb.valueProperty().removeListener(_this);
                                    else
                                        vsb.valueProperty().addListener(_this);
                                }
                            });
                        }

                        @Override
                        public void changed( ObservableValue< ? extends Number > observable, Number oldValue, Number newValue ) {

                            double value = Double.isNaN(vsb.getValue()) ? vsb.getMax() : vsb.getValue();

                            if( vsb.getMax() - value < 1.1 * vsb.getVisibleAmount() ) {
                                synchronized (DATA_SET_LOCK) {
                                    try {
                                        eof.set(!dataSet.swappingData());
                                    } catch( DataSetException ex ) {
                                        JInvErrorService.handleException(tableView.getScene().getWindow(), ex);
                                    }
                                }
                            }
                        }
                    });

                triggeredOnce = true;

                tableView.focusedProperty().removeListener(this);
            }
        });

        tableView.requestFocus();

        // Связывание данных
        List<T> rows = dataSet.getRows();

        if( !(rows instanceof ObservableList) ) {
              // для SceneBuilder'a
              // rows = new RowList();
            rows = FXCollections.observableList(new ArrayList<>());
        }

        final ObservableList<T> ol = (ObservableList)rows;

        this.tableView.setItems( ol );
        this.tableView.getProperties().put( PROPERTY_DATA_SET_ADAPTER, this );

        //после установки адаптера таблицы для statusbar
        setEnableMark( enableMark );

        if( dataSet instanceof ISQLDataSet )
            orderByManager = new OrderByManager<>(this);

        //
        bindColumns( cellValueChangeListener );

        //Менеджер колонок + статус бар
        if( tableView instanceof JInvTable )
        {
            JInvTable jtbl = (JInvTable) tableView;

            JInvFilterToolBar filterToolbar = jtbl.getFilterToolbar();

            if( filterToolbar != null )
            {
                filterToolbar.enablePrevMarkedRows(enableMark);

                if( enableMark )
                    filterToolbar.savePrevMarkedRowsProperty().bindBidirectional(this.savePrevMarkedRowsProperty);
            }
        }
        return this;
    }

    /** */
    public void setDataSet( IDataSet<T> ds ) {

        super.setDataSet(ds);

        if( dataSet != null )
        {
            // Связывание данных
            final List<T> rows = dataSet.getRows();

            if( !(rows instanceof ObservableList) )
                throw new IllegalStateException( Tags.PRODUCT_LABEL + "Internal collection in DataSet is not ObservableList!" );

            final ObservableList<T> ol = (ObservableList<T>)rows;
            this.tableView.setItems( ol );
        }
        else
        {
            this.tableView.setItems( FXCollections.emptyObservableList() );
        }
    }

    /** */
    protected void initMarkColumn(TableView<T> table) throws AppException {

        final IDataSet<T> ds = getDataSet();

        if( ds != null && ds instanceof XXIDataSet && isEnableMark() ) {

            XXIDataSet<T> xxiDs = (XXIDataSet)ds;

            if( !xxiDs.isSupportMark() ) {
                logger.warn( "Marker not working, the DataSet with rowClass '" + ds.getRowClass() + "' does not support it!" );
                return;
            }

            xxiDs.addMarkDataSetListener(this);
            
            final TableColumn<T, Boolean> markColumn = new TableColumn<>();
            markColumn.setSortable(false);
            markColumn.getProperties().put(COLUMN_MARK, Boolean.TRUE);
            markColumn.setCellValueFactory((TableColumn.CellDataFeatures<T, Boolean> param) -> {
                T pojo = param.getValue();
                if( pojo instanceof IMarkable ) {
                    IMarkable markable = (IMarkable) pojo;
                    return new SimpleBooleanProperty( markable.isMark() );
                }
                return null;
            });

            markColumn.setCellFactory( col -> new JInvTableCellMark<>(this) );

            markColumn.setMaxWidth (MARK_COLUMN_WIDTH_DEFAULT);
            markColumn.setPrefWidth(MARK_COLUMN_WIDTH_DEFAULT);
            markColumn.setMinWidth (MARK_COLUMN_WIDTH_DEFAULT);

            ButtonBase btMarkAll = ActionFactory.createButton(FontAwesome.fa_check_square_o, e -> markAll());
            btMarkAll.setStyle(btMarkAll.getStyle().concat("-fx-padding:3;"));

            ButtonBase btUnMarkAll = ActionFactory.createButton(FontAwesome.fa_square_o, e -> unMarkAll());
            btUnMarkAll.setStyle(btUnMarkAll.getStyle().concat("-fx-padding:3;"));

            HBox box = new HBox(btMarkAll, btUnMarkAll);
            box.setStyle("-fx-alignment:center;");
            markColumn.setGraphic(box);

            if( BaseApp.APP().getViewPrefService().isMarkLeft() )
                markColumn.getProperties().put("order_by", -1 );
            else
                markColumn.getProperties().put("order_by", 10000 );

            if (table instanceof JInvTable)
            {
                ((JInvTable) table).setMarkColumn(markColumn);
            }
            else
            {
                if( BaseApp.APP().getViewPrefService().isMarkLeft() ) {
                    table.getColumns().add(0, markColumn);
                } else {
                    table.getColumns().add( markColumn );
                }
            }

            box.disableProperty().bind( emptyProperty() );

            BaseApp.APP().getViewPrefService().refreshTableColumn(markColumn);
        }//end if
    }

    private JInvParallelAction markAllAction = null, unMarkAllAction = null;

    /**
     *
     */
    private void markAll(boolean doMark) {

        try {

            if( isEnableMark() ) {

                final XXIDataSet xxiDs = (XXIDataSet) getDataSet();

                JInvTable jt = (JInvTable)getTable();
                if( jt.isBigTableMode() && xxiDs.isEmpty() )
                    return;
//return

                if( doMark ) {

                    if( markAllAction == null ) {

                        TableView<T> table = getTable();

                        if (table instanceof JInvTable) {

                            IFormStateListener fsl = ((JInvTable) table).getController();

                            markAllAction = new JInvParallelAction( new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {

                                    try {

                                        xxiDs.markAllRows( savePrevMarkedRowsProperty.get() );

                                        Platform.runLater(() -> {
                                            refresh();
                                        });

                                    } catch (Throwable ex) {
                                        Platform.runLater(() -> {
                                            JInvErrorService.handleException(getTable().getScene().getWindow(), ex);
                                        });
                                    }
                                }
                            }, fsl);
                        }
                    }//end if null

                    markAllAction.handle();
                } else {

                    if (unMarkAllAction == null) {

                        TableView<T> table = getTable();

                        if (table instanceof JInvTable) {

                            IFormStateListener fsl = ((JInvTable) table).getController();

                            unMarkAllAction = new JInvParallelAction(new EventHandler<ActionEvent>() {
                                @Override
                                public void handle(ActionEvent event) {

                                    try {

                                        xxiDs.unMarkAllRows(savePrevMarkedRowsProperty.get());

                                        Platform.runLater(() -> {
                                            refresh();
                                        });

                                    } catch (Throwable ex) {
                                        Platform.runLater(() -> {
                                            JInvErrorService.handleException(getTable().getScene().getWindow(), ex);
                                        });
                                    }
                                }
                            }, fsl);
                        }
                    }//end if null

                    unMarkAllAction.handle();

                }
            }
        } catch (Throwable th) {
            JInvErrorService.handleException( getTable().getScene().getWindow(), th);
        }
    }

    /** */
    public void markAll() {
        markAll(true);
    }

    /** */
    public void unMarkAll() {
        markAll(false);
    }

    /** */
    private void markRow( int rowNum, boolean doMark ) {

        if( isEnableMark() && isXXI() )
        {
            try {
                if(doMark)
                    ((XXIDataSet)getDataSet()).markRow(rowNum);
                else
                    ((XXIDataSet)getDataSet()).unMarkRow(rowNum);
            }
            catch( Throwable th ) {
                throw new RuntimeException( Tags.PRODUCT_LABEL + "setMark in MarkerColumn error", th );
            }
        }
    }

    public void markRow( int rowNum ) {
        markRow(rowNum,true);
    }
    public void unMarkRow( int rowNum ) {
        markRow(rowNum,false);
    }

    /**
     * Инвертирует пометку на текущей строчке.
     */
    public void revertMarkCurrentRow() {

        try {

            if( isEnableMark() )
            {
                XXIDataSet xxiDs = (XXIDataSet) getDataSet();

                if( xxiDs.isMarkCurrentRow() )
                    xxiDs.unMarkCurrentRow();
                else
                    xxiDs.markCurrentRow();
            }
        } catch (Throwable th) {
            JInvErrorService.handleException(getTable().getScene().getWindow(), th);
        }
    }

    /** */
    public boolean hasMarkedRows( ) {

        if( isEnableMark() ) {
            XXIDataSet xxiDs = (XXIDataSet) getDataSet();
            return xxiDs.hasMarkedRows();
        }

        return false;
    }

    /** */
    public long computeNumberMarkedRows() {
        if( isEnableMark() ) {
            XXIDataSet xxiDs = (XXIDataSet) getDataSet();
            return xxiDs.computeNumberMarkedRows();
        }
        return -1L;
    }

    /** */
    public Long getMarkerID() {

        if( isEnableMark() ) {
            XXIDataSet xxiDs = (XXIDataSet) getDataSet();
            return xxiDs.getMarkerID();
        }

        return null;
    }

    /** */
    public MarkModeEnum getMarkMode() {

        if (isEnableMark()) {
            XXIDataSet xxiDs = (XXIDataSet) getDataSet();
            return xxiDs.getMarkMode();
        }

        return MarkModeEnum.NONE;
    }

    /** */
    public void clearMark() {
        try {
            if (isEnableMark()) {
                XXIDataSet xxiDs = (XXIDataSet) getDataSet();
                xxiDs.clearMark();
            }
        } catch (Throwable th) {
            JInvErrorService.handleException(getTable().getScene().getWindow(), th);
        }
    }

    /** Удаление текущей записи */
    public void removeCurrentRow( ) {
        try {

            if( isEnableMark() )
            {
                XXIDataSet xxiDs = (XXIDataSet) getDataSet();

                if( xxiDs.isMarkCurrentRow() )
                    xxiDs.unMarkCurrentRow();
            }

        } catch (Throwable th) {
            JInvErrorService.handleException( getTable().getScene().getWindow(), th );
        }
        getDataSet().removeCurrentRow();
    }

    /** Привязка столбцов к полям пожо */
    protected void bindColumns( ICellValueChangeListener<T> cellValueChangeListener) throws Exception {

        final Class<T> classEntity = getDataSet().getRowClass();

        if( classEntity == null )
            return;

        final TableView<T> table = getTable( );

        final List<TableColumn<T, ?>> listColumns = Controls.getLeafColumnsFromTable( table );

        if( listColumns == null || listColumns.isEmpty() ) {
            return;
        }

        final IEntityMetaData<T> entityMetaData = EntityMetadataFactory.getEntityMetaData(classEntity);

        for( TableColumn<T,?> column : listColumns )
        {
            String idColumn = Controls.getFieldNameFromTableColumn(column);

            if( S.isNullOrEmpty(idColumn) )
                continue;

            final IEntityProperty<T,?> pd = entityMetaData.getProperty(idColumn);

            if( pd == null )
                continue;

            if( column.isSortable() && orderByManager != null )
            {
                final String orderBy = pd.makeOrderBy( getTaskContext().dialect() );

                if( !S.isNullOrEmpty(orderBy) )
                     column.getProperties().put( COLUMN_ORDERBY, orderBy );
                else
                    column.setSortable( false );
            }

            if( pd.isTransient() )
            {
                column.getProperties().put( COLUMN_TRANSIENT, Boolean.TRUE );

                IEntityProperty<T, ?> proxyFor = pd.getProxyFor();

                if( proxyFor != null )
                    column.getProperties().put( COLUMN_PROXY_FOR, proxyFor.getColumnName() );
            }
            else
                trySetQueryExpr( pd, column );

            if( pd.getType() == Boolean.class )
            {
                final StubBooleanObservableValue obVal = new StubBooleanObservableValue(pd, cellValueChangeListener);

                if (column instanceof JInvTableColumnBoolean) {
                    column.setCellFactory(JInvCellFactoryProvider.getCellFactory(Boolean.class, column));
                } else {
                    column.setCellFactory((index) -> new JInvCheckBoxCellEditor());
                }
                column.setCellValueFactory( (param) -> obVal.setPojoInstance(((TableColumn.CellDataFeatures) param).getValue()));
            }
            else
            {
                Callback cellFactory;
                ContentTypeEnum typeInfo = pd.getContent();

                if( typeInfo != null )
                    cellFactory = JInvCellFactoryProvider.getCellFactory( typeInfo, column);
                else
                    cellFactory = JInvCellFactoryProvider.getCellFactory( pd.getType(), column);

                if( cellFactory != null )
                    column.setCellFactory(cellFactory);

                final StubObservableValue obVal = new StubObservableValue( pd, idColumn, cellValueChangeListener );
                column.setCellValueFactory((param) -> obVal.setPojoInstance(((TableColumn.CellDataFeatures) param).getValue()));
            }

        }//end for

        final AbstractBaseController<?> cntrlr = Controls.getControllerFromControl(table);

        if( cntrlr != null )
        {
            Controls.getAllColumnsFromTable(tableView).forEach( (TableColumn<T, ?> column) -> {
                if( column instanceof IViewChangeable )
                    ((IViewChangeable) column ).setViewPrefSaver( cntrlr.getViewContext().getViewPrefSaver());
            });
        }
        // пометка, если есть
        initMarkColumn(table);

        //columnFieldsSet = null;
    }

    private boolean insideInSetCurrentRow = false;

    /** */
    public void setCurRow(T row) {
        insideInSetCurrentRow = true;
        try {
            dataSet.setCurrentRow(row);
        }
        finally {
            insideInSetCurrentRow = false;
        }
    }

    /** */
    public T getCurRow() {
        return dataSet.getCurrentRow();
    }

    /** */
    public List<T> getRows() {
        return dataSet.getRows();
    }

    @Override
    public void dataSetChanged( DataSetEvent e ) {
        if( e.isAfter() && e.getEventType() == DataSetEvent.DataSetEventType.EXECUTE )
            Platform.runLater( this::refresh );
    }

    /** */
    @Override
    public void navigated(DataSetNavigationEvent<T> e) {

        if( !insideInSetCurrentRow ) {
            positionRow( e.getNewRowIndex() );
        }

        super.navigated(e);
    }

    /** */
    @Override
    public void rowOperation( DataSetRowEvent<T> event ) {

        if( event.getRowOperation() == UPDATE ) {
            positionRow( event.getRowIndex() );
        }

        super.rowOperation( event );


        /*
        Platform.runLater( ()->
            {
                switch(event.getRowOperation()) {
                    case INSERT:
                        rowList.elementsAdded(event.getRowIndex(), event.getRowCount());
                        break;
                    case UPDATE:
                        rowList.elementsReplaced(event.getRowIndex());
                        super.rowOperation(event);
                        break;
                    case DELETE: {
                        if(event.getRowCount() == 1)
                            rowList.elementsRemoved(event.getRowIndex(), (T)event.getOldRow());
                        else
                            rowList.refresh();

                        super.rowOperation(event);
                    }
                    break;
                }
            }
        );
        */
    }

    public void refreshCurrentRowFromDB() {
        if( dataSet != null && dataSet instanceof ISQLDataSet ) {
            try {
                ((ISQLDataSet)dataSet).refreshCurrentRowFromDB();
            } catch(DataSetException e) {
                TableView tv = getTable();
                if( tv != null && tv instanceof JInvTable ) {
                    JInvTable jt = (JInvTable)tv;
                    if( jt.getController() != null )
                        jt.getController().handleException(e);
                    else
                        JInvErrorService.handleException( jt, e );
                }
            }
        }
    }

    /**
     *
     */
    public static <R> DSFXAdapter<R> bind(IDataSet<R> dataSet, TableView<R> tableView, ICellValueChangeListener cellValueChangeListener, boolean enableMark) throws Exception {
        DSFXAdapter adapter = new DSFXAdapter();
        return adapter.bindTable(dataSet, tableView, cellValueChangeListener, enableMark);
    }

    public static <R> DSFXAdapter<R> bind(IDataSet<R> dataSet, TableView<R> tableView) throws Exception {
        DSFXAdapter adapter = new DSFXAdapter();
        return adapter.bindTable(dataSet, tableView, null);
    }

    /** Поддержка DSInfoBar */
    private DSInfoBar infoBar;

    public void initInfoBar( DSInfoBar ib ) {
        if( ib != null ) {
            ib.init( this );
            infoBar = ib;
        }
    }

    protected ViewContext getViewContext()
    {
        ViewContext vc = null;

        if( isJInvTable() )
        {
            JInvFXFormController<?> controller = ((JInvTable)tableView).getController();

            if( controller != null )
                vc = controller.getViewContext();
        }

        if( vc == null && tableView != null )
            vc = ViewContext.of( tableView.getScene().getWindow() );

        return vc;
    }

    /** */
    public DSInfoBar getInfoBar() {
        return infoBar;
    }

    /** Признак, что записей нет */
    private ReadOnlyBooleanWrapper emptyProperty;

    /** Признак, что данных нет, нужно для засеривания контролов связанных с датасет */
    public ReadOnlyBooleanProperty emptyProperty( ) {

        if( emptyProperty == null ) {

            emptyProperty = new ReadOnlyBooleanWrapper( true ) {

                {
                    if( getDataSet() != null )
                    {
                        getDataSet().addRowListener( (event) -> {
                            fire( event.getRowOperation() == DataSetRowEvent.RowOperationEnum.DELETE );
                        });

                        getDataSet().addDataSetListener( (e)-> {
                            if( e.isAfter() )
                                fire(true);
                        });
                    }
                }

                private void fire( boolean delete ) {
                    if( delete )
                        set( getDataSet().isEmpty() );
                    else
                        set( false );
                }

                @Override
                public Object getBean() {
                    return DSFXAdapter.this;
                }
                @Override
                public String getName() {
                    return "emptyProperty";
                }
            };
        }

        return emptyProperty.getReadOnlyProperty();
    }

    /** */
    public BooleanProperty enableMarkProperty() {
        return enableMarkProperty;
    }

    /** */
    public boolean isEnableMark() {
        return enableMarkProperty.get();
    }

    /** */
    public void setEnableMark(boolean enableMark) {
        this.enableMarkProperty.set(enableMark);
    }

    /** */
    public void executeQuery() {
        executeQuery(null);
    }

    /** */
    public void executeQuery( BiConsumer< Boolean, Throwable> clb) {
        executeQuery(clb, null, true);
    }

    /** */
    public void executeQuery( BiConsumer< Boolean, Throwable> clb, IFormStateListener stateListener, boolean parallel ) {
        executeQuery(clb, stateListener, parallel, false);
    }

    /** Проверка того при выполнении запроса возможен FullScan большой таблицы */
    private boolean checkForFullScan() throws DataSetException {

        if( U.containsNull(tableView, getDataSet()) || !(tableView instanceof JInvTable) || !(getDataSet() instanceof XXIDataSet)) {
            return true;
        }

        JInvTable table = (JInvTable) tableView;

        if (!table.isBigTableMode()) {
            return true;
        }

        JInvFXFormController<?> controller = table.getController();
        ViewContext vc = null;

        if (controller != null) {
            vc = controller.getViewContext();
        }

        if( vc != null ) {

            XXIDataSet xds = (XXIDataSet) getDataSet();

            if (xds.checkForFullScan(table.getBigTableName())) {

                if (Alerts.yesNo(vc,
                        null,
                        bundle.getString("FULL_SEARCH_OF_DOCS"),
                        bundle.getString("NUM_OF_DOCS"))) {
                    xds.setOrderBy(null);
                } else {
                    return false;
                }
            } else {

                IAppProperties dbProp = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_GLOBAL);
                int maxRecordCount = dbProp.getIntegerProperty("BIG_T_MAX_RECORD", 100);
                int maxSortedCount = dbProp.getIntegerProperty("BIG_T_MAX_SORTED", 1000);

                IAggregator agr = xds.createAggregatorBuilder(false, false).add("*", AggrFuncEnum.COUNT).build();

                agr.execute();

                int recordCount = TypeConverter.convert(agr.getValues().values().toArray()[0], Integer.class);

                if (recordCount > maxRecordCount) {

                    boolean clearOrderBy = recordCount > maxSortedCount;

                    if (Alerts.yesNo(vc,
                            null,
                            java.text.MessageFormat.format(bundle.getString("QUERY_RET_NREC"), new Object[]{recordCount}),
                            clearOrderBy ? bundle.getString("SORT_CANCELED") : "")) {
                        if (clearOrderBy) {
                            xds.setOrderBy(null);
                        }
                    } else // return
                    {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /** Проверка того при выполнении запроса возможен FullScan большой таблицы */
    private int checkForFullScan( Holder<Integer> recordCount ) throws DataSetException {

        if( U.containsNull( tableView, getDataSet() )
            ||
            !isXXI()
            ||
            !isJInvTable()
        )
            return 0;

        JInvTable table = (JInvTable)tableView;

        if( !table.isBigTableMode() || table.getController() == null )
            return 0;

        XXIDataSet xds = (XXIDataSet) getDataSet();

        if( xds.checkForFullScan( table.getBigTableName() ) ) {
            return 1;
        }
        else
        {
            IAppProperties dbProp = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_GLOBAL);
            int maxRecordCount = dbProp.getIntegerProperty("BIG_T_MAX_RECORD", 100);
            int maxSortedCount = dbProp.getIntegerProperty("BIG_T_MAX_SORTED", 1000);

            IAggregator agr = xds.createAggregatorBuilder(false, false).add("*", AggrFuncEnum.COUNT).build();

            agr.execute();

            int nCount = TypeConverter.convert( agr.getValues().values().toArray()[0], Integer.class );

            if( nCount > maxRecordCount) {

                recordCount.set( nCount );

                return nCount > maxSortedCount ? 3 : 2;
            }
        }

        return 0;
    }

    /** Предупреждение о FullScan */
    private void alertFullScan( ViewContext vc, IFormStateListener stateListener, XXIDataSet xds, int value, int recordCount, Runnable run ) {

        boolean doRun = false;

        if( value == 1 )
        {
            if( Alerts.yesNo( vc, null, bundle.getString("FULL_SEARCH_OF_DOCS"), bundle.getString("NUM_OF_DOCS") ) )
            {
                xds.setOrderBy( null );
                doRun = true;
            }
        }
        else
        {
            if (Alerts.yesNo( vc,
                    null,
                    java.text.MessageFormat.format( bundle.getString("QUERY_RET_NREC"), new Object[]{recordCount}),
                    value == 3 ? bundle.getString("SORT_CANCELED") : ""))
            {
                if( value == 3 )
                    xds.setOrderBy(null);

                doRun = true;
            }
        }

        if( doRun ) {
            new JInvParallelAction( (e)->run.run(), stateListener ).handle();
        }
    }

    /** */
    public void executeQuery(BiConsumer< Boolean, Throwable> clb, IFormStateListener stateListener, boolean parallel, boolean readAllRecords ) {

        if( getDataSet() == null ) {
            return;
        }

        final BiConsumer< Boolean, Throwable> callBack = (clb == null) ? ( t, th ) -> {
            if (!t && th != null) {
                Platform.runLater(() -> JInvErrorService.handleException(null, th));
            }
        } : clb;

        final IFormStateListener frmStateListener = stateListener != null ? stateListener : ((JInvTable) tableView).getController();

        Runnable executeRun = new Runnable() {
            @Override
            public void run() {
                try {
                    getDataSet( ).executeQuery( readAllRecords );
                    callBack.accept( true, null );
                } catch( Throwable th ) {
                    positionRow    ( 0 );
                    callBack.accept( false, th );
                }
            }
        };

        if( parallel )
        {

            // Сбрасываем пометку, перед запросом данных
            getTable().getSelectionModel().clearSelection();

            new JInvParallelAction( (e)->
                {
                    synchronized (DATA_SET_LOCK) {

                        try {

                            final Holder<Integer> recordCount = new Holder<>(0);

                            int value = checkForFullScan( recordCount );

                            if( value > 0 ) {

                                final ViewContext vc = ((JInvTable)getTable()).getController().getViewContext();

                                Platform.runLater (
                                    ()-> alertFullScan( vc, frmStateListener, (XXIDataSet)getDataSet(), value, recordCount.get(), executeRun )
                                );
                            }//end if
                            else
                            {
                                getDataSet( ).executeQuery( readAllRecords );
                                callBack.accept( true, null );
                            }
                        } catch( Throwable th ) {
                            positionRow    ( 0 );
                            callBack.accept( false, th );
                        }
                    }//end sync
                }, frmStateListener
            ).handle();
        }
        else
        {
            synchronized( DATA_SET_LOCK )
            {
                try {

                    if( checkForFullScan() )
                        getDataSet().executeQuery(readAllRecords);
                } catch (Throwable th) {
                    callBack.accept( false, th );
                } finally {
                    positionRow(0);
                }
            }
        }
    }

    /** Обновление записи */
    public void refreshRow( int index ) {
        Platform.runLater( ()->
            {
                if( index >= 0 && index < getTable().getItems().size() ) {
                    int focused = getTable().getSelectionModel().getFocusedIndex();
                    getTable().getItems().set( index, getTable().getItems().get(index) );
                    getTable().getSelectionModel().focus(focused);
                }
            }
        );
    }

    /** Позиционирование текущей записи */
    private void positionRow( int index ) {

        Platform.runLater(() -> {

            //rowList.refresh();

            if( index >= 0 && getTable().getItems().size() > index )
            {
                if( isJInvTable() )
                {
                    JInvTable jtbl = (JInvTable) tableView;

                    if( ignoreCheckScrolling || !jtbl.isVisibleRow(index) ) {
                        tableView.scrollTo(index);
                    }
                }
                else
                {
                    tableView.scrollTo(index);
                }

                if( tableView.getSelectionModel() != null )
                    tableView.getSelectionModel().select(index);
            }
//logger.debug("exit LA positionRow: index - {}", index );
        });
//logger.debug("exit positionRow: index - {}", index );

    }

    /**
     * Установить модификатор фильтра,
     * позволяющий динамически добавлять поля фильтрации и
     * изменять введенные пользователем условия отбора
     *
     * @param filterModifier
     */
    public void setFilterModifier(IFilterModifier filterModifier) {
        this.filterModifier = filterModifier;
    }

    public IFilterModifier getFilterModifier() {
        return this.filterModifier;
    }

    /**
     * Принимает мапу значений для фильтра.
     * @param filterValues
     * @throws ru.inversion.dataset.DataSetException
     */
    public void setFilterValues ( List<? extends IFilterItem> filterValues ) throws DataSetException
    {
        IDataSet ds = getDataSet ();

        if( isSQL() )
        {
            ISQLDataSet<?> sds = (ISQLDataSet) ds;

            int filterId = sds.setFilter( filterValues, false, false );

            if( filterId == 0 )
                sds.clearFilter( false );

            if( !savePrevMarkedRowsProperty.get() )
                clearMark ();

        }//end if
    }


    @Deprecated
    public boolean getFilterMode() {
        return false;
    }

    /**
     * Показывает диалог фильтра F7/F8
     * @param stage
     */
    public void showFilterDialog ( Stage stage ){
        showFilterDialog(ViewContext.of( stage ));
    }

    /**
     * Показывает диалог фильтра F7/F8
     */
    public void showFilterDialog ( ViewContext viewContext )
    {
        if( !isSQL() )
            return;

        new FXFormLauncher<> (getTaskContext (), viewContext, F7DialogFormController.class)
            .dialogMode (VM_EDIT)
            .dataObject (this)
            .callback   ((ok, ctrl)->
                {
                    if( ok == RET_OK )
                    {
                        final TableView< T > table = getTable();
                        table.getSelectionModel().clearSelection();
                        table.getFocusModel().focus(-1);

                        final AbstractBaseController controller = Controls.getControllerFromControl( table );

                        if( controller != null )
                            controller.onRefreshData( this );
                        else
                            executeQuery();
                    }
                })
            .doModal ();
    }

    /** Признак что DataSet is SQLDataSet */
    public boolean isSQL() {
        return getDataSet() != null && getDataSet() instanceof SQLDataSet;
    }

    /** Признак что DataSet is XXIDataSet */
    public boolean isXXI() {
        return getDataSet() != null && getDataSet() instanceof XXIDataSet;
    }

    /** Признак что TableView is JInvTable */
    public boolean isJInvTable() { return tableView != null && tableView instanceof JInvTable; }


    /** */
    @Override
    public void markAction(DataSetMarkEvent<T> event) {
        if( event.getIndex() != -1 ) {
            refreshRow( event.getIndex() );
            //tableView.requestFocus();
        }
    }

    public boolean isEnd() {
        return eof.get();
    }

    public void setEnd(boolean val) {
        eof.set(val);
    }

    /**
     * Свойство конца данных при подкачке
     */
    public BooleanProperty endProperty() {
        return eof;
    }

    /**
     * Метод выполняется после инициализации адаптера.
     * @
     * В частности после первого executeQuery
     */
    public void afterInit() {

        if( !isControlsWithOrderAdded() ) {
            changeControlsOrderFromSceneGraph();
        }

        Collections.sort( controlList );
    }

    private boolean isControlsWithOrderAdded() {
        boolean result = false;
        for (BindControlInfo info : controlList) {
            if (info.getOrder() != 0) {
                result = true;
                break;
            }
        }
        return result;
    }

    private void changeControlsOrderFromSceneGraph() {

        if (tableView != null && tableView instanceof JInvTable) {
            JInvFXFormController controller = ((JInvTable) tableView).getController();
            if (controller != null) {
                Parent parent = controller.getContentPane();
                List<Control> listControls = Controls.getControlList(parent, null);

                int order = 0;
                int countFind = 0;

                for (Control control : listControls) {

                    if (countFind == controlList.size()) {
                        break;
                    }

                    for (BindControlInfo info : controlList) {

                        if (info.getComponent() != null && info.getComponent().equals(control)) {
                            info.setOrder(order);
                            countFind++;
                            order++;
                            break;
                        }
                    }
                }
            }
        }
    }

    public OrderByManager getOrderByManager() {
        return orderByManager;
    }

    public void setOrderByManager(OrderByManager sortButtonManager) {
        this.orderByManager = sortButtonManager;
    }

    /** */
    private void refreshCurrentRow() {

        insideInSetCurrentRow = true;

        try {
            dataSet.setCurrentRowNum( tableView.getSelectionModel().getSelectedIndex() );
        }
        finally {
            insideInSetCurrentRow = false;
        }
    }

    /** */
    private void addColumnToTableByControl( Control control, IEntityProperty<T,?> pd, Callback<T, ? extends Object> clbk )
    {
        if( !U.containsNull( pd, control, tableView )
            && tableView.isTableMenuButtonVisible()
            && tableView.getColumnResizePolicy().equals(UNCONSTRAINED_RESIZE_POLICY)
            //&& !checkColumnWithFieldNameExists( pd.getColumnName() )
            && !columnFieldsSet.contains(pd.getColumnName())
        )
        {
            StringProperty title = new SimpleStringProperty( pd.getPropertyName() );
            StringProperty label = ((IJInvControl) control).labelTextProperty();

            if( label != null)
            {
                AbstractBaseController<?> controller = Controls.getControllerFromControl(control);

                // пробуем вытащить название столбца из бандла по имени поля из БД
                // даже если нет привязанного лейбла
                if ( S.isNullOrEmpty(label.getValue()) && controller != null ){

                    String bundleString = S.EMPTY_STRING;

                    if( pd.getProxyFor() != null )
                        bundleString = controller.getBundleString(pd.getProxyFor().getPropertyName());

                    if( S.isNullOrEmpty(bundleString) )
                        bundleString = controller.getBundleString(pd.getPropertyName());

                    label.setValue(bundleString);
                }

                title = label;
            }

            JInvTableColumn<T,?> column = Controls.getColumnByClass( pd.getType(), null, title );
            column.setFieldName( pd.getPropertyName() );

            String orderBy = pd.makeOrderBy(getTaskContext().dialect());

            if (!S.isNullOrEmpty(orderBy)) {
                column.getProperties().put(COLUMN_ORDERBY, orderBy);
            }

            column.setSortable( !S.isNullOrEmpty(orderBy) );

            if( pd.isTransient() ) {
                column.getProperties().put(COLUMN_TRANSIENT, Boolean.TRUE);
            }

            trySetQueryExpr( pd, column );

            IEntityProperty<T,?> proxyFor = pd.getProxyFor();
            if (proxyFor != null) {
                column.getProperties().put(COLUMN_PROXY_FOR, proxyFor.getColumnName() );
            }

            Callback cellFactory;
            ContentTypeEnum typeInfo = pd.getContent();

            if (typeInfo != null) {
                cellFactory = JInvCellFactoryProvider.getCellFactory(typeInfo, column);
            } else {
                cellFactory = JInvCellFactoryProvider.getCellFactory(pd.getType(), column);
            }

            if (cellFactory != null) {
                column.setCellFactory(cellFactory);
            }

            final StubCallbackValue<T> obVal = new StubCallbackValue<>(clbk);
            column.setCellValueFactory((param) -> obVal.setPojoInstance((T) ((TableColumn.CellDataFeatures) param).getValue()));
            column.setVisible(false);

            tableView.getColumns().add(column);

            column.getProperties().put( COLUMN_SHOW_IN_FILTER, Boolean.FALSE );
            column.setViewPrefSaver(Controls.getControllerFromControl(tableView).getViewContext().getViewPrefSaver());
            column.applyViewPrefs();

        }
    }//end for

    /** */
    private void trySetQueryExpr( final IEntityProperty<T,?> pd, final TableColumn<T,?> column ) {
        if( pd.getColumnInfo() != null && pd.getColumnInfo().getQueryExpr() != null ) {
            column.getProperties().put( COLUMN_QUERY_EXPR, pd.getColumnInfo().makeQueryExpr( getTaskContext().dialect() ) );
        }
    }

    /** */
    private boolean checkColumnWithFieldNameExists( String columnName )
    {
        Predicate<String> fieldPredicate = new Predicate<String>() {
            @Override
            public boolean test( String fieldName ) {
                System.out.println( "col: " + columnName + ", fld: " + fieldName );
                return fieldName.equals(columnName);
            }
        };

        return tableView.getColumns()
                    .stream().filter(c->c instanceof JInvTableColumn)
                    .map(c->((JInvTableColumn)c).getFieldName())
                    .anyMatch( fieldPredicate );
    }

    @Deprecated
    public DSFXAdapter bindControl(Object control, Callback<T, ? extends Object> clbk)
    {
        return (DSFXAdapter) super.bindControl(control, clbk);
    }
    @Deprecated
    public DSFXAdapter bindControl(Object component, String columnName, Callback<T, ? extends Object> callBack) {
        return (DSFXAdapter) super.bindControl(component, columnName, callBack);
    }

    /** Возвращает список групп для F7 фильтра */
    public List<F7FilterGroup> getListFilterGroup() {
        return listFilterGroup;
    }

    /** Устанавливает список групп для F7 фильтра */
    public void setListFilterGroup(List<F7FilterGroup> listFilterGroup) {
        this.listFilterGroup = listFilterGroup;
    }

    /** */
    public void setFilterGroups(F7FilterGroup... args) {
        if( args != null && args.length > 0 )
            listFilterGroup.addAll( Arrays.asList(args) );
    }

    private ProgressTaskExecutor<Void, DSFXAdapter<T>> progressTaskExecutor;

    /** */
    final private BooleanProperty wasCanceled = new SimpleBooleanProperty( this, "wasCanceled", false );

    /** */
    private void check4Valid() {
        if( U.containsNull( getTable(), getDataSet() ) )
            throw new IllegalStateException(Tags.PRODUCT_LABEL + bundle.getString("NOT_VALID_DSFXADAPTER") );
    }

    /** */
    public <P> boolean findRow( BiPredicate<T, P> predicat, P arg2) {
        if (!U.containsNull(getTable(), getDataSet())) {
            return getDataSet().findRow(predicat, arg2);
        }
        throw new IllegalStateException(Tags.PRODUCT_LABEL + bundle.getString("NOT_VALID_DSFXADAPTER"));
    }

    /** */
    @Deprecated
    public <P> boolean findRow(BiPredicate<T, P> predicat, P arg2, boolean ignoreCheckScrolling) {
        return findRow(predicat, arg2);
    }

    /**
     Поиск записи c прогрессбаром по условию в наборе записей внутри DataSet.
     Если запись не найдена, отобразится сообщение о неудаче
     @param predicate условие поиска
     */
    public void findRow( Predicate<T> predicate ) {
        findRow( predicate, null, 0 );
    }

    /**
     Поиск записи c прогрессбаром по условию в наборе записей внутри DataSet.
     @param predicate условие поиска
     @param onSearchResult Результат поиска: найдено или нет
     */
    public void findRow( Predicate<T> predicate, Consumer<Boolean> onSearchResult ) {
        findRow( predicate, onSearchResult, 0 );
    }

    /**
     Поиск записи c прогрессбаром по условию в наборе записей внутри DataSet.
     @param predicate условие поиска
     @param onSearchResult Результат поиска: найдено или нет
     @param startRowIndex Строка, с которой начинать искать
     */
    public void findRow( Predicate<T> predicate, Consumer<Boolean> onSearchResult, int startRowIndex )
    {
        check4Valid( );

        if( progressTaskExecutor != null )
            return;

        try {

            progressTaskExecutor = new ProgressTaskExecutor<>();
            progressTaskExecutor
                .allowCancel(true)
                .stage((Stage)getTable().getScene().getWindow(), bundle.getString("FIND_ROWS"), false)
                .callback( rowFinder(this, predicate, onSearchResult, startRowIndex) )
                .indicator(false)
            .execute();

            progressTaskExecutor.isDone();
        }
        catch( Throwable th ) {
            throw th;
        }
        finally {
            progressTaskExecutor = null;
        }
    }

    /** Запуск закачки данных из DataSet */
    public void moveToLast() {

        check4Valid();

            if( progressTaskExecutor != null )
                return;

            getDataSet().removeNavigationListener(this);

            bulkAppend.setValue(true);

            try {

                progressTaskExecutor = new ProgressTaskExecutor<>();
                progressTaskExecutor
                        .allowCancel(true)
                        .stage((Stage)getTable().getScene().getWindow(), bundle.getString("LOAD_ROWS"), false)
                        .callback(rowLoader(this))
                        .indicator(false)
                        .execute();

                progressTaskExecutor.isDone();
            }
            catch( Throwable th ) {
                throw th;
            }
            finally {
                progressTaskExecutor = null;
                getDataSet().addNavigationListener(this);
            }

        int rowCount = wasCanceled.get() ? getDataSet().getLoadedRowCount() - 51 : getDataSet().getLoadedRowCount() - 1;

        positionRow( rowCount );

        Platform.runLater( ()-> bulkAppend.setValue(false) );
    }

    /** */
    private ProgressCallback<Void, DSFXAdapter<T>> rowLoader( DSFXAdapter<T> adapter )
    {
        return( progress, p ) ->
        {
            long totalRowCount = (long)adapter.getDataSet().getTotalRowCount();

            if( totalRowCount <= 0 )
                progress.before( "wait ..." );
            else
                progress.begin( 0L, totalRowCount, "wait ... " );

            while( !progress.isCancelled() &&  dataSet.swappingData() ) {

                if( totalRowCount < 0L )
                {
                    progress.updateMessage( dataSet.getLoadedRowCount() + " ..." );
                }
                else
                {
                    progress.process( dataSet.getLoadedRowCount(), totalRowCount, dataSet.getLoadedRowCount() + " из " + totalRowCount );
                }
            }

            if( !progress.isCancelled() )
                 eof.set( true );

            progress.end( "end" );

            wasCanceled.setValue( progress.isCancelled() );

            Platform.runLater( () -> {
                tableView.getSelectionModel().selectLast();
                Platform.runLater( () -> {
                    dataSet.setCurrentRow( tableView.getSelectionModel().getSelectedItem() );
                } );
            } );

            return null;
        };
    }


    /** */
    private ProgressCallback<Void, DSFXAdapter<T>> rowFinder( DSFXAdapter<T> adapter, final Predicate<T> predicate,
            final Consumer<Boolean> onSearchResult, final int startRowIndex )  {

        return( progress, p ) ->
        {
            IDataSet<T> dataSet = adapter.getDataSet();
            long totalRowCount = (long) dataSet.getTotalRowCount();

            if( totalRowCount <= 0 )
                progress.before( "wait ..." );
            else
                progress.begin( 0L, totalRowCount, "wait ... " );


            boolean foundInExisting = dataSet.findRow( startRowIndex, predicate );

            final AtomicBoolean found = new AtomicBoolean( foundInExisting );

            IDataSetRowListener<T> rowListener = event -> {
                if ( event.getRowOperation() == INSERT && !found.get() ) {
                    int startIndex = event.getRowIndex()+1; //?, было + 2

                    boolean isFound = dataSet.findRow( startIndex, predicate );
                    if ( isFound ){
                        found.set( true );
                    }
                }
            };
            this.dataSet.addRowListener( rowListener );

            while( !found.get() && !progress.isCancelled() && this.dataSet.swappingData() ) {

                if( totalRowCount < 0L )
                {
                    progress.updateMessage( this.dataSet.getLoadedRowCount() + " ..." );
                }
                else
                {
                    progress.process( this.dataSet.getLoadedRowCount(), totalRowCount, this.dataSet.getLoadedRowCount() + " из " + totalRowCount );
                }
            }

            progress.end( "end" );

            this.dataSet.removeRowListener( rowListener );

            wasCanceled.setValue( progress.isCancelled() );

            if ( onSearchResult == null ){
                if ( !found.get() ){
                    Platform.runLater( () ->
                        Alerts.info( tableView.getScene().getWindow(), bundle.getString( "RECORD_NOT_FOUND" ) ) );
                }
            } else {
                Platform.runLater( () -> {
                    onSearchResult.accept( found.get() );
                } );
            }

            return null;
        };
    }

    public static BiFunction<String,String,XXIDsDao.FilterData> autoFilterMaker( ViewContext vc, TaskContext tc )
    {
        return new BiFunction< String, String, XXIDsDao.FilterData >() {
            @Override
            public XXIDsDao.FilterData apply( String formName, String dataSetName ) {
                return FilterManager.getAutoFilter( vc, tc, formName, dataSetName );
            }
        };
    }

}
