package ru.inversion.fx.form.controls.treetableex;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.DataSetRowEvent;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.controls.treetableex.cell.JInvTreeTableCell_Mark;
import ru.inversion.fx.form.controls.treetableex.column.JInvTreeTableColumnEx_Date;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tds.*;
import ru.inversion.utils.Checks;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static ru.inversion.dataset.fx.DSFXAdapter.MARK_COLUMN_WIDTH_DEFAULT;
import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_DATA_SET_ADAPTER;
import static ru.inversion.fx.app.es.JInvErrorService.logger;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_MARK;

public class TSFXAdapter<P>
        extends TSControlAdapter<P>
        implements ITreeDataSetListener<P>, ITreeDataSetRowsListener<P>, ITreeDataSetMarkListener<P>
{
    public enum ItemMarkMode {
        NONE,
        ALL_ITEMS,
        LEAF_ONLY
    }

    public static final String PROPERTY_TITLE_COLUMN    = "ru.inv.title_column";
    public static final String PROPERTY_TITLE_EXTRACTOR = "ru.inv.title_extractor";
    /** */
    public static class Builder<P> {

        private ITreeDataSet<P>    treeDataSet;
        private JInvTreeTableEx<P> treeTable;
        private int                expandLevel = 0;
        private String             titleColumn;
        private TSFXAdapter.ItemMarkMode markMode = ItemMarkMode.ALL_ITEMS;
        private ICellValueChangeListener<P> cellValueChangeListener;

        public Builder<P> expandLevel( int expandLevel ) {
            this.expandLevel = expandLevel;
            return this;
        }

        public Builder<P> markMode( TSFXAdapter.ItemMarkMode markMode ) {
            this.markMode = markMode;
            return this;
        }

        /** */
        public Builder() {
        }

        /** */
        public Builder( ITreeDataSet< P > treeDataSet, JInvTreeTableEx<P> treeTable ) {
            this.treeDataSet = treeDataSet;
            this.treeTable = treeTable;
        }

        /** */
        public Builder< P > treeDataSet( ITreeDataSet< P > treeDataSet ) {
            this.treeDataSet = treeDataSet;
            return this;
        }

        /** */
        public Builder< P > treeTable( JInvTreeTableEx<P> treeTable ) {
            this.treeTable = treeTable;
            return this;
        }


        /** */
        public Builder< P > titleColumn( String s ) {
            this.titleColumn = s;
            return this;
        }

        /** */
        public Builder< P > cellValueChangeListener( ICellValueChangeListener<P> cl ) {
            this.cellValueChangeListener = cl;
            return this;
        }

        /** */
        public TSFXAdapter<P> bind()
        {

            Checks.Require.object( treeDataSet, "treeDataSet" );
            Checks.Require.object( treeTable,   "treeTable" );

            if( !S.isNullOrEmpty(titleColumn) ) {

                treeTable.setProperty( PROPERTY_TITLE_COLUMN, titleColumn );

                final IEntityProperty< ? super P, ? > ep = EntityMetadataFactory.getEntityMetaData(treeDataSet.getRowClass()).getProperty(titleColumn);

                if( ep != null )
                {
                    Function<P,String> titleExtractor = p -> U.callIfNotNull( ep.invokeGetter(p), Object::toString );
                    treeTable.setProperty( PROPERTY_TITLE_EXTRACTOR, titleExtractor );
                }
            }

            Checks.Require.object(markMode,"markMode");

            if( markMode != ItemMarkMode.NONE )
            {
                if( !(treeDataSet instanceof XXITreeDataSet) )
                {
                    logger.warn( "Marker not working, TreeDataSet is not XXITreeDataSet. Set 'markMode' to NONE!" );
                    markMode = ItemMarkMode.NONE;
                }
                else
                {
                    final XXITreeDataSet<P> xxiDs = (XXITreeDataSet<P>) treeDataSet;

                    if( !xxiDs.isSupportMark() )
                    {
                        logger.warn( "Marker not working, the TreeDataSet with rowClass '" + xxiDs.getRowClass() + "' does not support it! Set 'markMode' to NONE!" );
                        markMode = ItemMarkMode.NONE;
                    }
                }
            }
            return new TSFXAdapter<>( treeDataSet, treeTable, expandLevel, markMode, cellValueChangeListener );
        }
    }

    public static <T> Builder<T> newBuilder() {
        return new Builder<>();
    }

    /** */
    final private JInvTreeTableEx<P> treeTableView;

    /** */
    private boolean insideInSetCurrentRow = false;

    /** */
    private int expandRefreshLevel = 0;

    final private ItemMarkMode markMode;

    final private ICellValueChangeListener<P> cellValueChangeListener;

    private final static Object TREEDATA_SET_LOCK = new Object();

    /** */
    private TSFXAdapter( ITreeDataSet<P> dataSet, JInvTreeTableEx<P> treeTableView, int expandLevel, ItemMarkMode markMode, ICellValueChangeListener<P> cellValueChangeListener ) {

        super(dataSet);

        this.markMode      = markMode;
        this.treeTableView = Objects.requireNonNull( treeTableView, "'tableView' is null" );

        this.treeTableView.setProperty( PROPERTY_DATA_SET_ADAPTER, this );

        this.treeDataSet.addDataSetListener(this);
        this.treeDataSet.addRowsListener   (this);

        this.treeTableView.setShowRoot( false );
        this.treeTableView.setRoot( new TreeItem<>() );

        this.cellValueChangeListener = cellValueChangeListener;

        //
        bindColumns( );

        final JInvTreeTableEx.TreeTableViewSelectionModel<P> selectionModel = treeTableView.getSelectionModel();

        selectionModel.setSelectionMode(SelectionMode.SINGLE);
        selectionModel.selectedItemProperty().addListener(new ChangeListener<TreeItem<P>>() {
            @Override
            public void changed ( ObservableValue<? extends TreeItem<P>> observable, TreeItem<P> oldValue, TreeItem<P> newValue )
            {
                if (!insideInSetCurrentRow) {
                    setCurrentFromTreeItem(newValue);
                }
            }
        });

        this.expandRefreshLevel = expandLevel;
    }

    /** */
    public JInvTreeTableEx<P> getTreeTable() {
        return treeTableView;
    }

    /** */
    public boolean isEnableMark() {
        return markMode != ItemMarkMode.NONE;
    }

    /**
     * Установка режима пометки перенесена в Builder
     *
     * @see Builder
     * */
    @Deprecated
    public void setItemMarkMode(ItemMarkMode itemMarkMode) {
    }
    @Deprecated
    public TSFXAdapter<P> enableMark( boolean enable) {
        return this;
    }
    @Deprecated
    public TSFXAdapter<P> itemMarkMode( ItemMarkMode itemMarkMode) {
        return this;
    }
    /** */
    public ItemMarkMode getItemMarkMode( ) {
        return this.markMode;
    }

    @SuppressWarnings("unchecked")
    private TreeItem<P> asTreeItem(ITreeDataSetItem<P> item) {

        if (!(item instanceof TreeItem)) {
            throw new IllegalStateException( Tags.PRODUCT_LABEL + "TreeDataSet item is not JavaFX TreeItem: " + item );
        }

        return (TreeItem<P>) item;
    }

    /** */
    private void expandToLevel(
            TreeItem<?> treeItem,
            int level
    ) {
        if (treeItem == null || treeItem.isLeaf()) {
            return;
        }

        treeItem.setExpanded(true);

        if (level >= expandRefreshLevel) {
            return;
        }

        for (TreeItem<?> child : treeItem.getChildren()) {
            expandToLevel(child, level + 1);
        }
    }

    /** Признак что DataSet is SQLDataSet */
    public boolean isSQL() {
        return getTreeDataSet() != null && getTreeDataSet() instanceof SQLTreeDataSet;
    }

    /** Признак что DataSet is XXIDataSet */
    public boolean isXXI() {
        return getTreeDataSet() != null && getTreeDataSet() instanceof XXITreeDataSet;
    }

    /** Признак что TableView is JInvTreeTableEx */
    public boolean isJInvTreeTableEx() { return  treeTableView instanceof JInvTreeTableEx; }

    /** Признак, что записей нет */
    private ReadOnlyBooleanWrapper emptyProperty;
    
    /** Признак, что данных нет, нужно для засеривания контролов связанных с датасет */
    public ReadOnlyBooleanProperty emptyProperty() {

        if (emptyProperty == null) {
            emptyProperty = new ReadOnlyBooleanWrapper(
                    this,
                    "emptyProperty",
                    getTreeDataSet() == null || getTreeDataSet().isEmpty()
            );
        }
        return emptyProperty.getReadOnlyProperty();
    }

    /** */
    private void updateEmptyProperty( boolean delete )
    {
        if( emptyProperty == null || getTreeDataSet() == null )
            return;

        if( delete )
            emptyProperty.set( getTreeDataSet().isEmpty() );
        else
            emptyProperty.set(false);
    }

    /** */
    private Window getOwnerWindow() {

        if( getTreeTable() == null || getTreeTable().getScene() == null )
            return null;

        return getTreeTable().getScene().getWindow();
    }


    private boolean insideMarkItem;

    /** */
    @Override
    public void markAction( TreeDataSetMarkEvent<P> event )
    {
        if( !event.isAfter() || insideMarkItem || event.getItem() == null )
            return;

        if( Platform.isFxApplicationThread() )
            treeTableView.refresh();
        else
            Platform.runLater( treeTableView::refresh );
    }

    @SuppressWarnings("unchecked")
    private ITreeDataSetItem<P> asTreeDataSetItem( TreeItem<P> treeItem )
    {
        if( treeItem == null )
            return null;

        if( !( treeItem instanceof ITreeDataSetItem ) )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "TreeItem is not ITreeDataSetItem: " + treeItem.getClass().getName() );

        final ITreeDataSetItem<P> dataSetItem = (ITreeDataSetItem<P>) treeItem;

        if( dataSetItem.getDataSet() != getTreeDataSet() )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "TreeItem belongs to another TreeDataSet" );

        return dataSetItem;
    }

    private void markItem( TreeItem<P> treeItem, boolean doMark )
    {
        if( !isEnableMark() || !isXXI() )
            return;

        final ITreeDataSetItem<P> dataSetItem = asTreeDataSetItem(treeItem);

        if( dataSetItem == null )
            return;

        insideMarkItem = true;

        try
        {
            final XXITreeDataSet<P> dataSet = (XXITreeDataSet<P>) getTreeDataSet();

            if( doMark )
                dataSet.markItem(dataSetItem);
            else
                dataSet.unMarkItem(dataSetItem);
        }
        catch( Throwable th )
        {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "setMark in MarkerColumn error", th );
        }
        finally
        {
            insideMarkItem = false;
        }

        if( Platform.isFxApplicationThread() )
            treeTableView.refresh();
        else
            Platform.runLater( treeTableView::refresh );
    }

    public void markItem  ( TreeItem<P> treeItem ) {
        markItem(treeItem,true);
    }
    public void unMarkItem( TreeItem<P> treeItem ) {
        markItem(treeItem,false);
    }

    private JInvParallelAction markAllAction = null, unMarkAllAction = null;

    /** */
    private void markAll( boolean doMark ) {

        if( !isEnableMark() || getTreeDataSet() == null || getTreeDataSet().isEmpty() || !isJInvTreeTableEx() )
            return;

        try {

            final XXITreeDataSet<P> xxiDs = (XXITreeDataSet<P>)getTreeDataSet();

            if( doMark )
            {
                if( markAllAction == null )
                {
                    IFormStateListener fsl = getTreeTable().getController();

                    markAllAction = new JInvParallelAction( new EventHandler< ActionEvent >() {
                        @Override
                        public void handle(ActionEvent event) {

                            try {

                                xxiDs.markAll( getItemMarkMode() == ItemMarkMode.LEAF_ONLY );
                                //refreshImpl();

                            } catch (Throwable ex) {
                                Platform.runLater(() -> {
                                    JInvErrorService.handleException( getOwnerWindow(), ex );
                                });
                            }
                        }
                    }, fsl );
                }//end if null

                markAllAction.handle();
            }
            else
            {
                if( unMarkAllAction == null )
                {
                    IFormStateListener fsl = getTreeTable().getController();

                        unMarkAllAction = new JInvParallelAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {

                                try {

                                    xxiDs.unMarkAll();

                                    //refreshImpl( );

                                } catch (Throwable ex) {
                                    Platform.runLater(() -> {
                                        JInvErrorService.handleException(getOwnerWindow(), ex);
                                    });
                                }
                            }
                        }, fsl);
                }//end if null

                unMarkAllAction.handle();

            }
        } catch (Throwable th) {
            JInvErrorService.handleException( getOwnerWindow(), th);
        }
    }// end markAll

    /** */
    public void markAll() {
        markAll(true);
    }

    /** */
    public void unMarkAll() {
        markAll(false);
    }

    /** */
    private boolean hasMarkColumn() {

        for (TreeTableColumn<P, ?> column : getTreeTable().getColumns())
        {
            if( Boolean.TRUE.equals( column.getProperties().get(COLUMN_MARK)) )
                return true;
        }
        return false;
    }

    /** */
    protected void initMarkColumn( ) {

        if( hasMarkColumn() )
            return;

        ITreeDataSet<P> ds = getTreeDataSet();

        if( ds != null && isEnableMark() )
        {
            final XXITreeDataSet<P> xxiDs = (XXITreeDataSet<P>)ds;

            if( !xxiDs.isSupportMark() ) {
                logger.warn( "Marker not working, the DataSet with rowClass '" + ds.getRowClass() + "' does not support it!" );
                return;
            }

            xxiDs.addMarkListener(this);
            JInvTreeTableColumnEx<P,Boolean> markColumn = new JInvTreeTableColumnEx<>();
            markColumn.setSortable(false);
            markColumn.getProperties().put(COLUMN_MARK, Boolean.TRUE);
            final MarkBooleanObservableValue mbov = new MarkBooleanObservableValue();
            markColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<P, Boolean> param) -> {
                Object value = null;

                if (param != null && param.getValue() != null) {
                    value = param.getValue().getValue();
                }

                mbov.setMarkable(value instanceof IMarkable ? (IMarkable) value : null);

                return mbov;
            });

            markColumn.setMaxWidth (MARK_COLUMN_WIDTH_DEFAULT);
            markColumn.setPrefWidth(MARK_COLUMN_WIDTH_DEFAULT);
            markColumn.setMinWidth (MARK_COLUMN_WIDTH_DEFAULT);

            markColumn.setCellFactory( col -> new JInvTreeTableCell_Mark<P>(this, getItemMarkMode() == ItemMarkMode.LEAF_ONLY) );

            ButtonBase btMarkAll = ActionFactory.createButton(FontAwesome.fa_check_square_o, e -> markAll());
            btMarkAll.setStyle(btMarkAll.getStyle().concat("-fx-padding:3;"));

            ButtonBase btUnMarkAll = ActionFactory.createButton(FontAwesome.fa_square_o, e -> unMarkAll());
            btUnMarkAll.setStyle(btUnMarkAll.getStyle().concat("-fx-padding:3;"));

            HBox box = new HBox(btMarkAll, btUnMarkAll);
            box.setStyle("-fx-alignment:center;");
            markColumn.setGraphic(box);

            box.disableProperty().bind( emptyProperty() );

            try {

                if( BaseApp.APP().getViewPrefService().isMarkLeft() )
                    getTreeTable().getColumns().add(1, markColumn);
                else
                    getTreeTable().getColumns().add( markColumn );

                BaseApp.APP().getViewPrefService().refreshTableColumn( markColumn );

            } catch(AppException aex ) {
                aex.printStackTrace();
            }
        }//end if
    }


    /** Привязка столбцов к полям пожо */
    protected void bindColumns( ) throws TreeDataSetException {

        final Class<P> classEntity = getTreeDataSet().getRowClass();

        if( classEntity == null )
            return;

        JInvTreeTableEx<P> treeTable = getTreeTable( );

        if( treeTable.getColumns().isEmpty() )
            return;


        IEntityMetaData<P> em = EntityMetadataFactory.getEntityMetaData(classEntity);

        for( TreeTableColumn<P, ?> column : treeTable.getLeafColumns() )
        {
            if( !(column instanceof JInvTreeTableColumnEx) )
                continue;

            final JInvTreeTableColumnEx<P, ?> invColumn = (JInvTreeTableColumnEx<P, ?>) column;

            final IEntityProperty<P, ?> property = em.getProperty(invColumn.getFieldName());

            if( property != null )
                invColumn.bind( (IEntityProperty) property, cellValueChangeListener );
        }

        initMarkColumn();
    }


    /** */
    private void refresh() {

        final JInvTreeTableEx<P> treeTable = getTreeTable();

        TreeItem<P> root = treeTable.getRoot();

        if (root != null) {
            root.getChildren().clear();
        }

        root = new TreeItem<>();

        for( ITreeDataSetItem<P> item : treeDataSet.getRootList() )
             root.getChildren().add(asTreeItem(item));

        treeTable.setRoot(root);

        if( expandRefreshLevel > 0 )
            expandToLevel(root, 0);

        treeTable.refresh();
    }


    /** */
    private void refreshImpl( )
    {
        if(!Platform.isFxApplicationThread() )
            Platform.runLater( this::refresh );
        else
            refresh();
    }


    /** */
    @Override
    public void dataSetChanged(TreeDataSetEvent<P> e) {

        if( !e.isAfter() )
            return;

        updateEmptyProperty(true);

        if( e.getEventType() == DataSetEvent.DataSetEventType.EXECUTE )
            refreshImpl();
    }


    /** */
    private void setCurrentFromTreeItem(TreeItem<P> item) {

        if (!(item instanceof TreeViewItemAdapter)) {
            return;
        }

        if (getTreeDataSet() == null) {
            return;
        }

        insideInSetCurrentRow = true;

        try {
            getTreeDataSet().setCurrentItem((TreeViewItemAdapter<P>) item);
        }
        finally {
            insideInSetCurrentRow = false;
        }
    }


    /** */
    private void refreshCurrentRow()
    {
        setCurrentFromTreeItem(
            treeTableView.getSelectionModel().getSelectedItem()
        );
    }


    /** Позиционирование текущей записи */
    private void positionRow(ITreeDataSetItem<P> item) {

        if (item == null)
        {
            if (getTreeDataSet().getRootList().isEmpty()) {
                return;
            }

            item = getTreeDataSet().getRootList().get(0);
        }

        if(!(item instanceof TreeItem) ) {
            return;
        }

        final TreeItem<P> treeItem = (TreeItem<P>) item;

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> treeTableView.selectItem(treeItem));
        }
        else {
            treeTableView.selectItem(treeItem);
        }
    }

    /** */
    @Override
    public void navigated( TreeDataSetNavigationEvent<P> event ) {

        if( !insideInSetCurrentRow )
             positionRow( event.getNewItem() );

        super.navigated( event );
    }

    /** */
    public void executeQuery( )
    {
        executeQuery( null, null, false );
    }

    /** */
    public void executeQuery( BiConsumer< Boolean, Throwable> clbk, IFormStateListener stateListener, boolean parallel ) {

        final BiConsumer< Boolean, Throwable> callBack = (clbk == null) ? ( t, th ) -> {
            if (!t && th != null) {
                Platform.runLater(() -> JInvErrorService.handleException(null, th));
            }
        } : clbk;

        Runnable executeRun = new Runnable() {
            @Override
            public void run() {
                try {
                    getTreeDataSet( ).executeQuery( );
                    callBack.accept( true, null );
                } catch( Throwable th ) {
                    callBack.accept( false, th );
                } finally {
                    // positionRow( null );
                    // positionRow вызовется на обработку NavigationEvent
                }
            }
        };

        if( parallel )
        {
            final IFormStateListener frmStateListener = stateListener != null ? stateListener : getTreeTable().getController();

            // Сбрасываем пометку, перед запросом данных
            getTreeTable().getSelectionModel().clearSelection();

            new JInvParallelAction( ( e)->
            {
                synchronized (TREEDATA_SET_LOCK) {
                    executeRun.run();
                }//end sync
            }, frmStateListener
            ).handle();
        }
        else
        {
            synchronized (TREEDATA_SET_LOCK) {
                executeRun.run();
            }//end sync
        }
    }

    /** */
    @Override
    public void rowsOperation(TreeDataSetRowsEvent<P> event)
    {
        if( event.isAfter() )
        {
            updateEmptyProperty( event.getItemOperation() == DataSetRowEvent.RowOperationEnum.DELETE );
            refreshImpl();
        }
    }


    /** */
    @Override
    public void close()
    {
        final ITreeDataSet<P> dataSet = getTreeDataSet();

        if( dataSet == null )
            return;

        dataSet.removeDataSetListener(this);
        dataSet.removeRowsListener(this);

        if( dataSet instanceof XXITreeDataSet )
            ((XXITreeDataSet<P>)dataSet).removeMarkListener(this);

        super.close();
    }
}
