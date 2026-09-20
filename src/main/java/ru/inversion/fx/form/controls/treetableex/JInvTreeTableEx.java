package ru.inversion.fx.form.controls.treetableex;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.fx.form.controls.JInvMenuItem;
import ru.inversion.fx.form.controls.renderer.Colorizer;
import ru.inversion.fx.form.controls.renderer.IColoredCell;
import ru.inversion.fx.form.controls.treetableex.infobar.TSInfoBar;
import ru.inversion.fx.form.search.SearchParam;
import ru.inversion.fx.form.search.TextSearchDialog;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tds.ITreeDataSet;
import ru.inversion.tds.TreeDataSetSearchParam;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_DATA_SET_ADAPTER;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.*;
import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.*;

public class JInvTreeTableEx<P> extends TreeTableView<P> implements IJInvControl, IChoiceControl {

    /** */
    private final BooleanProperty visibleStatusBarProperty = new SimpleBooleanProperty(this, "visibleStatusBarProperty", false) {
        @Override
        protected void invalidated() {
            //перерисовываем таблицу
            refresh();
        }
    };

    /** */
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    public JInvTreeTableEx() {
        init();
    }

    public JInvTreeTableEx( TreeItem<P> root) {
        super(root);
        init();
    }

    private void initPlaceHolder() {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);

        Label labelSmallPart = new Label(fore.getString("LABEL_TABLE_PLACEHOLDER_SMALL_PART"));
        labelSmallPart.setTextAlignment(TextAlignment.CENTER);
        labelSmallPart.getStyleClass().add("table-placeholder-small-part");

        Label labelBigPart = new Label(fore.getString("LABEL_TABLE_PLACEHOLDER_BIG_PART"));
        labelBigPart.setTextAlignment (TextAlignment.CENTER);
        labelBigPart.getStyleClass().add("table-placeholder-big-part");
        box.getChildren().addAll(labelSmallPart, labelBigPart);
        setPlaceholder(box);

        // Показываем холдер, если линейные размеры таблицы превышают размеры холдера в два раза
        box.visibleProperty().bind(labelSmallPart.widthProperty().multiply(2).
                lessThan(widthProperty()).and(labelBigPart.heightProperty().add(labelSmallPart.heightProperty()).
                multiply(2).lessThan(heightProperty())));

    }

    /**
     Добавить правило раскраски для всей таблицы.
     Цвета, добавленные позже, перекрывают старые в случае коллизии

     @param styleExpr условие раскраски; если цвет менять не надо – возвращать null
     */
    public void addColor( final Function<IColoredCell<P>, Colorizer> styleExpr ) {
        forEachColumn(column -> column.addColor( styleExpr ) );
    }

    /**
     Убрать всю раскраску, заданную через addColor() во всей таблице
     */
    public void clearColor(){
        forEachColumn( JInvTreeTableColumnEx::clearColor );
    }

    private void forEachColumn(Consumer<JInvTreeTableColumnEx> operation ){
        getAllColumns().forEach( column -> {
            if (column instanceof JInvTreeTableColumnEx){
                operation.accept( (JInvTreeTableColumnEx) column );
            }
        } );
    }

    public List<TreeTableColumn> getAllColumns() {
        List<TreeTableColumn> list = new ArrayList<>();
        fillListColumnsRecursive(getColumns(), list, t -> true, t -> true );

        return list;
    }

    /**
     * Установка видимости СтатусБара
     */
    public BooleanProperty visibleStatusBarProperty() {
        return visibleStatusBarProperty;
    }


    /** */
    protected boolean isItemVisible( int index )
    {
        return ((JInvTreeTableViewSkin)getSkin()).isIndexVisible(index);
    }


    /** */
    public void selectItem(TreeItem<P> item)
    {
        if( item == null || getRoot() == null || getRoot().getChildren().isEmpty() )
            return;

        final TreeItem<P> target = item == getRoot() ? getRoot().getChildren().get(0) : item;

        Platform.runLater(() -> {

            if( getRoot() == null || getRoot().getChildren().isEmpty() )
                return;

            expandParents(target);

            final int index = getRow(target);

            /*
             * Узел может быть уже удалён либо дерево
             * ещё не содержит его после полной перестройки.
             * В таком случае строку 0 не выбираем.
             */
            if( index < 0 )
                return;

            if( getSelectionModel().getSelectedItem() != target )
                getSelectionModel().clearAndSelect(index);

            final TreeTablePosition<P, ?> focusedCell = getFocusModel().getFocusedCell();

            final TreeTableColumn<P, ?> focusedColumn = focusedCell == null ? null : focusedCell.getTableColumn();

            if( getFocusModel().getFocusedIndex() != index )
            {
                if( focusedColumn == null )
                    getFocusModel().focus(index);
                else
                    getFocusModel().focus(index, focusedColumn);
            }

            if( !isItemVisible(index) )
                scrollTo(index);
        });
    }


    /** */
    private void expandParents(TreeItem<P> item) {

        TreeItem<P> parent = item == null ? null : item.getParent();

        final List<TreeItem<P>> path = new ArrayList<>();

        while (parent != null && parent.getParent() != null) {
            path.add(parent);
            parent = parent.getParent();
        }

        Collections.reverse(path);

        for (TreeItem<P> treeItem : path) {
            treeItem.setExpanded(true);
        }
    }


    private void expandHelper( TreeItem<P> item )
    {
        if( item != null && !item.isLeaf() )
        {
            if( getRoot() != item  )
                item.setExpanded(true);

            item.getChildren().forEach(this::expandHelper);
        }
    }

    /** */
    public void expand(TreeItem<P> item) {

        if (item == null) {
            return;
        }

        expandHelper(item);
        selectItem(item);
    }

    /** */
    private void collapseHelper( TreeItem<P> item )
    {
        if( item != null && !item.isLeaf() )
        {
            if( getRoot() != item  )
                item.setExpanded(false);

            item.getChildren().forEach(this::collapseHelper);
        }
    }

    /** */
    public void collapse(TreeItem<P> item) {

        if( item == null )
            return;

        collapseHelper(item);
        selectItem(item);
    }

    /** */
    public void expandAll( )
    {
        expand( getRoot() );
    }

    /** */
    public void collapseAll( )
    {
        collapse( getRoot() );
    }

    /** */
    public void expandCurrentItem( )
    {
        expand( getSelectionModel().getSelectedItem() );
    }

    /** */
    public void collapseCurrentItem( )
    {
        collapse( getSelectionModel().getSelectedItem() );
    }

    /** */
    @Override
    public void initChoiceBehavior( AbstractBaseController< ? > controller ) {
    }

    /** */
    private void getLeafColumnsHelper( TreeTableColumn<P,?> column, List<TreeTableColumn<P,?>> list )
    {
        if( column.getColumns().isEmpty() )
            list.add(column);
        else
            column.getColumns().forEach( (c)->getLeafColumnsHelper( c, list ) );
    }

    /** */
    public List<TreeTableColumn<P, ?>> getLeafColumns()
    {
        final List<TreeTableColumn<P, ?>> list =
                new ArrayList<>();

        getColumns().forEach( column -> getLeafColumnsHelper(column, list) );

        return list;
    }


    /** /
    public void initActionList( Object ... actionList )
    {
        this.actionList = actionList;
    }
    */

    private boolean contextMenuInitialized = false;

    /** */
    private void init()
    {
        setSkin( new JInvTreeTableViewSkin(this) );

        this.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                if (!contextMenuInitialized) {
                    createAndInitContextMenu();
                    removeEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this);
                }
            }
        });

        initPlaceHolder();

        getSelectionModel()
            .selectedItemProperty()
                .addListener( (observable, oldItem, newItem) -> scheduleFocusSynchronization(newItem) );
    }

    private void scheduleFocusSynchronization(
            TreeItem<P> expectedItem
    )
    {
        if( expectedItem == null )
            return;

        Platform.runLater(() -> {

            /*
             * За время ожидания selection мог измениться ещё раз.
             */
            if( getSelectionModel().getSelectedItem()
                    != expectedItem )
            {
                return;
            }

            final int selectedIndex =
                    getRow(expectedItem);

            if( selectedIndex < 0 )
                return;

            final int focusedIndex =
                    getFocusModel().getFocusedIndex();

            final TreeItem<P> focusedItem =
                    focusedIndex < 0
                            ? null
                            : getTreeItem(focusedIndex);

            if( focusedItem == expectedItem )
                return;

            final TreeTablePosition<P, ?> focusedCell =
                    getFocusModel().getFocusedCell();

            final TreeTableColumn<P, ?> focusedColumn =
                    focusedCell == null
                            ? null
                            : focusedCell.getTableColumn();

            if( focusedColumn == null )
                getFocusModel().focus(selectedIndex);
            else
                getFocusModel().focus(
                        selectedIndex,
                        focusedColumn
                );
        });
    }

    /** */
    public TSInfoBar getInfoBar() {
        return ((JInvTreeTableViewSkin)getSkin()).getInfoBar();
    }

    private void initMouse()
    {
        addEventFilter(
                MouseEvent.MOUSE_PRESSED,
                event -> {
                    Node node = event.getTarget() instanceof Node ? (Node) event.getTarget() : null;

                    while( node != null && !(node instanceof TreeTableCell) )
                    {
                        node = node.getParent();
                    }

                    if( !(node instanceof TreeTableCell) )
                        return;

                    final TreeTableCell<P, ?> cell = (TreeTableCell<P, ?>) node;

                    final TreeTableRow<P> row = cell.getTreeTableRow();

                    if( row == null || row.isEmpty() )
                        return;

                    final int index = row.getIndex();

                    if( index < 0 )
                        return;

                    final TreeTableColumn<P, ?> column =
                            cell.getTableColumn();

                    if( column == null )
                        getFocusModel().focus(index);
                    else
                        getFocusModel().focus(index, column);

                    // selection здесь не трогаем
                }
        );
    }


    /** Показывает диалог поиска по столбцу */
    public void showColumnSearchDialog( ) {
        findByColumnValue();
    }

    /** */
    protected void findByColumnValue( )
    {
        try {

            final TreeTableColumn<P,?> tc = getFocusModel().getFocusedCell().getTableColumn();

            if( ! (tc instanceof JInvTreeTableColumnEx) )
                return;

            final JInvTreeTableColumnEx treeTableColumn = (JInvTreeTableColumnEx)tc;
            final String fieldName = treeTableColumn.getFieldName();

            // Если поле не привязано, не ищем
            if( S.isNullOrEmpty( fieldName ) )
            {
                Alerts.info( getScene().getWindow(), null, "Для столбца \"" + treeTableColumn.getText() + "\" поиск не доступен.",
                             "К столбцу не привязано поле из набора данных. fieldName is null" );
                return;
            }

            final ITreeDataSet< P > treeDataSet = getTSFXAdapter().getTreeDataSet();
            final Class<? super P> rowClass     = treeDataSet.getRowClass();

            final IEntityProperty<? super P,?> property = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(fieldName);

            if( property != null )
            {
                TextSearchDialog tsd = new TextSearchDialog(treeTableColumn.getId(), S.EMPTY_STRING );
                tsd.setTitle      ( "Поиск по столбцу");
                tsd.setHeaderText ( "Поиск по значению столбца \"" + treeTableColumn.getText() + "\"" );
                tsd.setContentText( "Введите значения для поиска. Допускаются символы '_' и '%'");
                tsd.initOwner     ( getScene().getWindow() );
                final Optional<SearchParam<String>> opSearchParam = tsd.showAndWait();
                if( opSearchParam.isPresent() )
                {
                    final SearchParam<String> searchParam = opSearchParam.get();
                    final StringConverter sc = treeTableColumn.getStringConverter();

                    Predicate<P> finder = p -> {
                        Object o = property.invokeGetter(p);
                        if( o == null )
                            return false;
                        return searchParam.getPredicate().test( sc.toString(o) );
                    };

                    treeDataSet.findItem (
                        new TreeDataSetSearchParam (
                            finder,
                            searchParam.isLeafOnly(),
                            searchParam.isBegin(),
                            searchParam.wrapAround()
                        )
                    );
                }
            }
        }
        catch( Throwable th ) {
            JInvErrorService.handleException( getScene().getWindow(), th);
        }
    }

    /** */
    private ObjectProperty<Consumer<ContextMenu>> contextMenuInitializer;

    public ObjectProperty< Consumer< ContextMenu > > contextMenuInitializerProperty() {
        if( contextMenuInitializer == null )
            contextMenuInitializer = new SimpleObjectProperty<>(this, "contextMenuInitializer");
        return contextMenuInitializer;
    }

    final public Consumer<ContextMenu> getContextMenuInitializer() {
        return contextMenuInitializer == null ? null : contextMenuInitializer.get();
    }
    final public void setContextMenuInitializer( Consumer< ContextMenu > contextMenuInitializer ) {
        contextMenuInitializerProperty().set(contextMenuInitializer);
    }

    /** */
    private boolean containsActionItem(
            Menu menu,
            IAction action
    ) {
        return !menu.getItems()
                .filtered(item ->
                        item instanceof JInvMenuItem
                                && ((JInvMenuItem) item).getLastAction() == action
                )
                .isEmpty();
    }

    /** */
    private void appendSearchAction(Menu treeTableMenu) {

        final JInvMenuItem findMenuItem =
                new JInvMenuItem("Поиск по столбцу…");

        findMenuItem.setAction(
                ActionFactory.createAction(SEARCH, new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        findByColumnValue();
                    }
                })
        );

        treeTableMenu.getItems().add(findMenuItem);
    }

    /** */
    private void appendEditActions(Menu treeTableMenu) {

        final Iterator<IAction> iter =
                JInvKeyboardManager.getActionListIterator(this);

        final boolean allowEditOperations =
                getController() == null
                        || U.in(getController().getFormMode(), VM_NONE, VM_INS, VM_EDIT);

        boolean first = true;

        while (iter.hasNext()) {
            final IAction action = iter.next();

            if (action.getActionType() == null) {
                continue;
            }

            if (!allowEditOperations
                    || U.notIn(action.getActionType(), CREATE, CREATE_BY, UPDATE, DELETE, CLEAR)) {
                continue;
            }

            if (containsActionItem(treeTableMenu, action)) {
                continue;
            }

            if (first) {
                first = false;
                treeTableMenu.getItems().add(new SeparatorMenuItem());
            }

            JInvMenuItem item =
                    new JInvMenuItem(
                            action.getTitle(),
                            ActionFactory.getLabel(action.getIcon()),
                            this
                    );

            item.setAction(action);

            treeTableMenu.getItems().add(item);
        }
    }

    /** */
    protected void createAndInitContextMenu() {

        if( contextMenuInitialized )
            return;

        ContextMenu contextMenu = getContextMenu();

        if (contextMenu == null) {
            contextMenu = new ContextMenu();
            setContextMenu(contextMenu);
        }

        Consumer<ContextMenu> cmi = getContextMenuInitializer();

        if (cmi != null) {
            cmi.accept(contextMenu);
        }

        final Menu treeTableMenu =
                JInvTreeTableContextMenuFactory.createTreeTableMenu(this);

        appendEditActions(treeTableMenu);
        appendSearchAction(treeTableMenu);

        contextMenu.getItems().add(treeTableMenu);

        contextMenuInitialized = true;
    }


    /** */
    public TSFXAdapter<P> getTSFXAdapter() {
        return getProperty( PROPERTY_DATA_SET_ADAPTER);
    }

    public void showDsInfoDialog() {

        TSFXAdapter adapter = getTSFXAdapter();
        if (adapter != null)
        {
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

    private void fillListColumnsRecursive(ObservableList<TreeTableColumn<P, ?>> listColums, List<TreeTableColumn> resultList,
                                          Predicate<TreeTableColumn> predicateForParent, Predicate<TreeTableColumn> predicateForLeaves) {

        listColums.stream().forEach((TreeTableColumn t) -> {
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

}


