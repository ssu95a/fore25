/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.treetable;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.fx.StubObservableValue;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.S;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.controls.treetable.AdapterTreeItem.PROPERTY_ADAPTER_TREE_ITEM;
import static ru.inversion.fx.form.controls.treetable.JInvTreeTableColumn.COLUMN_ORDERBY;
import static ru.inversion.fx.form.controls.treetable.JInvTreeTableColumn.COLUMN_TRANSIENT;

/**
 *
 * @author perov
 * @param <Z> The type of the TreeItem instances used in this TreeTableView.
 */
public class JInvTreeTable<Z> extends TreeTableView<Z> implements IJInvControl {
    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    private StringProperty textLabelProperty = new SimpleStringProperty("");

    /**
     *
     * @return AdapterTreeItem
     */
    public AdapterTreeItem getAdapterTreeItem() {
        AdapterTreeItem result = null;
        Object adapter = getProperties().getOrDefault(PROPERTY_ADAPTER_TREE_ITEM, null);
        if (adapter != null && adapter instanceof AdapterTreeItem) {
            result = (AdapterTreeItem) adapter;
        }
        return result;
    }

    @Override
    public Control setLabel(Label label) {
        if (label != null) {
            label.setLabelFor(this);
            textLabelProperty.bind(label.textProperty());
        }
        return this;
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

    public JInvTreeTable() {
        super();
        //readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
//        setPlaceholder(new Label(""));
        initPlaceHolder();
    }

    public JInvTreeTable(TreeItem<Z> root) {
        super(root);
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    @Override
    public Label getLabel() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getFieldName() {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        return null;
    }

    @Override
    public void setFieldName(String fieldName) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sortColumns(int col) {
        List<TreeTableColumn<Z, ?>> listColumns = this.getColumns().filtered((p) -> p instanceof JInvTreeTableColumn);
        if (listColumns != null || !listColumns.isEmpty()) {
            getSortOrder().add(listColumns.get(col));
        }
    }

    public void bindColums(Class pojoClass) {

        //получаем columns
        List<TreeTableColumn<Z, ?>> listColumns = this.getColumns().filtered((p) -> {
            return p instanceof JInvTreeTableColumn;
        });

        if (listColumns == null || listColumns.isEmpty()) {
            return;
        }
        for (TreeTableColumn column : listColumns) {

            String idColumn = getFieldNameFromTableColumn((JInvTreeTableColumn) column);

            if (S.isNullOrEmpty(idColumn)) {
                continue;
            }
            //получаем метаданные entity
            IEntityProperty pd = EntityMetadataFactory.getEntityMetaData(pojoClass).getProperty(idColumn);

            if (pd == null) {
                continue;
            }

//            if (pd.isTransient()) {
//                column.setSortable(false);
//                column.getProperties().put(COLUMN_TRANSIENT, Boolean.TRUE);
//            }
//
//            if (pd.getColumnInfo() != null && !S.isNullOrEmpty(pd.getColumnInfo().getOrderBy())) //                column.getProperties().put(COLUMN_ORDERBY, pd.getValue("order_by") );
//            {
//                column.getProperties().put(COLUMN_ORDERBY, pd.getColumnInfo().getOrderBy());
//            }
            String orderBy = S.EMPTY_STRING; //pd.getOrderBy( tc .dialect());

            if (!S.isNullOrEmpty(orderBy)) {
                column.getProperties().put(COLUMN_ORDERBY, orderBy);
            }

            if (pd.isTransient()) {
                column.setSortable(!S.isNullOrEmpty(orderBy));
                column.getProperties().put(COLUMN_TRANSIENT, Boolean.TRUE);
            }

            final StubObservableValue obVal = new StubObservableValue(pd, idColumn, null);
            column.setCellValueFactory((Object param) -> obVal.setPojoInstance(((JInvTreeTableColumn.CellDataFeatures) param).getValue().getValue()));

        }

    }

    /**
     * Получаем FieldName
     *
     * @param column
     * @return
     */
    private String getFieldNameFromTableColumn(JInvTreeTableColumn column) {

        String fieldName = null;
        if (column instanceof JInvTreeTableColumn) {
            fieldName = ((JInvTreeTableColumn) column).getFieldName();
        }

        if (fieldName == null || fieldName.isEmpty()) {
            fieldName = column.getId();
        }
        return fieldName;

    }

    @Override
    public void setToolTipText(String toolTipText) {
    }

    @Override
    public String getToolTipText() {
        return null;
    }

    /**
     * Развернуть все элементы
     */
    public void setExpandedAll() {
        expandTree(this.getRoot());
    }

    /**
     * Свернуть все элементы
     */
    public void setUnExpandedAll() {
        collapseTree(this.getRoot());
    }

    public void setUnExpandedAll(boolean excludeRoot) {
        collapseTree(this.getRoot(), true);
    }


    private void expandTree(TreeItem<?> item) {
        if (item != null && !item.isLeaf()) {
            item.setExpanded(true);
            for (TreeItem<?> child : item.getChildren()) {
                expandTree(child);
            }
        }
    }

    private void collapseTree(TreeItem<?> item) {
        collapseTree(item, false);
    }

    private void collapseTree(TreeItem<?> item, boolean rootExclude) {
        if (item != null && !item.isLeaf()) {
            if (rootExclude && item.getParent() == null) {
                item.setExpanded(true); // root всегда expanded
            } else {
                item.setExpanded(false);
            }
            for (TreeItem<?> child : item.getChildren()) {
                collapseTree(child);
            }
        }
    }

    @Override
    public StringProperty labelTextProperty() {
        return textLabelProperty;
    }

}
