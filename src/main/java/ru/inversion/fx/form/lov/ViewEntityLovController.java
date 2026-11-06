/*
 * ЦАБС Банк XXI Век
 * Компания ИНВЕРСИЯ
 */
package ru.inversion.fx.form.lov;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvLovSearchPane;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.utils.S;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Контроллер энтити лова
 *
 * @param <P>
 * @author psh
 */
public class ViewEntityLovController<P> extends JInvFXFormController<PEntityLov<P>> {
    //
//
//
    @FXML
    private AnchorPane rootAnchorPane;
    @FXML
    private VBox root;
    @FXML
    private JInvToolBar toolBar;
    @FXML
    private Pane tablePane;

    private SQLDataSet<P> dataSet;
    private JInvTable<P> table;

    //
// JInvEntityLov
//
    private JInvEntityLov<P, ?> getLov() {
        return getDataObject().getLovObject();
    }

    //
// initDataSet
//
    private void initDataSet () throws Exception 
    {
/* psh 07.03.2019
        dataSet = new SQLDataSet<>(getTaskContext(), getLov().getEntityClass());
        dataSet.setCallbackParameters(getLov().getParameters());

        if (getLov().getProperty("ru.inversion.dataset.query_alias") != null)
            dataSet.setQueryAlias((String) getLov().getProperty("ru.inversion.dataset.query_alias"));

        dataSet.setWherePredicat(getLov().getWherePredicat());
        dataSet.setFilter(getLov().getFilter(), true, true);
*/
        dataSet = getLov ().createDataSet ();

        String orderBy = getLov().getChoiceOrderBy();
        if( S.isNotNullOrEmpty(orderBy) )
            dataSet.setOrderBy(orderBy);

        String queryName = getLov().getNativeQueryName();
        if( S.isNotNullOrEmpty(queryName) )
            dataSet.setNativeQueryName( queryName );

        String queryAlias = getLov().getQueryAlias();
        if( S.isNotNullOrEmpty(queryAlias) )
            dataSet.setQueryAlias( queryAlias );

        if( S.isNullOrEmpty(dataSet.getOrderBy()) )
        {
            ResourceBundle rs = getLov().getResourceBundle();

            if (rs != null && rs.containsKey(LOV_ORDER_BY) && !LOV_ORDER_BY.equals(rs.getString(LOV_ORDER_BY)))
                dataSet.setOrderBy(rs.getString(LOV_ORDER_BY));
            else
                dataSet.setOrderBy(getLov().getValueColumnName());
        }
    }

//
// createTableView
//
    private void createTableView() throws Exception {

        table = new JInvTable<>();
        table.setTableMenuButtonVisible(true);
        table.setId("LOV_TABLE");

        int width = 0;
        for (JInvEntityLovColumn<P> column : getLov().getColumnList()) {
            String title = column.getTitle();
            if (S.isNullOrEmpty(title))
                continue;

            JInvTableColumn<P, ?> tableColumn = new JInvTableColumn<>(title);
            tableColumn.setFieldName(column.getName());
            tableColumn.setSortable (column.isSortable());

            if( column.getWidth() > 0 )
            {
                width += column.getWidth() + 1;
                tableColumn.setPrefWidth(column.getWidth());
            }
            table.getColumns().add(tableColumn);
        }

        int sum = 15 + 10 + 2 + width;
        root.setPrefWidth(sum > AbstractBaseController.MAX_WIDTH ? AbstractBaseController.MAX_WIDTH : sum);

        if (table.getColumns().size() == 1)
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tablePane.getChildren().add(table);
    }

    //
// loadTableView
//
    @SuppressWarnings("unchecked")
    private void loadTableView(URL fxml) throws Exception {
        FXMLLoader loader = new FXMLLoader();

        loader.setLocation(fxml);
        loader.setResources(getLov().getResourceBundle());

        Region rgn;
        try {
            rgn = loader.load();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        table = Objects.requireNonNull((JInvTable) rgn.lookup("JInvTable"), String.format("JInvTable not found in %s", fxml));

        if (S.isNullOrEmpty(table.getId()))
            table.setId("LOV_TABLE");

        double width = 10 + 10; // http://jira.inversion.ru:8080/browse/JAVAKERNEL-993
        if (table.getPrefWidth() != Region.USE_COMPUTED_SIZE)
            width += table.getPrefWidth();
        else {
            width += 15 + 2;
            for (TableColumn<P, ?> c : table.getColumns())
                width += c.getPrefWidth() + 1;
        }

        if (width > AbstractBaseController.MAX_WIDTH)
            width = AbstractBaseController.MAX_WIDTH;

        table.setTableMenuButtonVisible(true);
        root.setPrefWidth(width);

        tablePane.getChildren().add(rgn);
    }

    //
// initTableView
//
    private void initTableView() throws Exception {

        Class<?> cl = getLov().getEntityClass();

        URL fxml;
        if (S.isNullOrEmpty(getLov().getSceneFileName()))
            fxml = cl.getResource(String.format("fxml/%s.fxml", cl.getSimpleName().replace("Controller", "")));
        else
            fxml = cl.getClassLoader().getResource(getLov().getSceneFileName());

        if( fxml != null )
            loadTableView(fxml);
        else
            createTableView();

        setChoiceControl(table);
        DSFXAdapter.bind(dataSet, table);

        /*
        if (fxml == null)
            for (TableColumn<P, ?> pTableColumn : table.getColumns()) {
                pTableColumn.setCellFactory(tc -> {
                    TableCell cell = new TableCell() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            if (item == null || empty) {
                                setGraphic(null);
                                setText(null);
                            }
                        }
                    };
                    cell.itemProperty().addListener((observable, itemOldValue, itemNewValue) -> {
                        if (itemNewValue == null) return;
                        Text text = new Text(itemNewValue.toString());
                        text.getStyleClass().add("cell-text");
                        cell.setGraphic(text);
                        cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
                        pTableColumn.widthProperty().addListener((observable1, widthOldValue, widthNewValue) -> {
                            if (widthNewValue.doubleValue() > 100) {
                                text.setWrappingWidth(widthNewValue.doubleValue());
                            }
                        });
                        text.setWrappingWidth(pTableColumn.getWidth());
                    });
                    return cell;
                });
            }
         */
    }

    //
// initSearchPane
//
    private void initSearchPane() {
        JInvLovSearchPane lovSearchPane = new JInvLovSearchPane();

        String strSQL = dataSet.getSQL();

        lovSearchPane.init(table,
            S.isNullOrEmpty(strSQL) ? true : !strSQL.contains(":lov_parameter"),
            getLov().getChoiceFilter(),
            getDataObject().getFilterSting(),
            getLov().getColumnValue().getDBColumnName(getTaskContext().dialect())
        );

        lovSearchPane.prefWidthProperty().bind(toolBar.widthProperty().subtract(15L));

        if (getLov().isSmallLov())
            lovSearchPane.setMaxPageSize(getLov().getMaxCountRow());

        toolBar.getItems().add(lovSearchPane);
    }

    //
// initLovSize
//
    private void initLovSize() {
        int size;

        ResourceBundle rs = getLov().getResourceBundle();
        if (rs != null) {
            if (rs.containsKey(LOV_WIDTH)) {
                try {
                    size = Integer.parseInt(rs.getString(LOV_WIDTH));
                    root.setPrefHeight(size); // почему не setPrefWidth?
                } catch (NumberFormatException th) {
                }
            }

            if (rs.containsKey(LOV_HEIGHT)) {
                try {
                    size = Integer.parseInt(rs.getString(LOV_HEIGHT));
                    root.setPrefWidth(size);
                } catch (NumberFormatException th) {
                }
            }
        }

        if (getLov().isSmallLov()) {
            logger.warn("Small lov position in progress...");
//                Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
//                lovDialog.setX(primaryScreenBounds.getMinX() + this.getPosition().getKey());
//                lovDialog.setY(primaryScreenBounds.getMinY() + this.getPosition().getValue());
        }
    }

    //
// setViewContext
//
    @Override
    public void setViewContext(ViewContext viewContext) {
        super.setViewContext(viewContext);

        viewContext.setFormName(getLov().getEntityClass().getCanonicalName());
    }

    //
// init
//
    @Override
    protected void init() throws Exception {
        setName(getLov().getEntityClass().getCanonicalName());
        setTitle(getLov().getTitle());

        initDataSet();
        initTableView();
        initSearchPane();
        initLovSize();

        table.requestFocus();
    }

    //
// afterInit
//
    @Override
    protected void afterInit() throws AppException {
        try {
            if (ViewPrefAppService.getSavedDimensions(root) == null)
                initViewSettings();
        } catch (Exception ex) {
            throw new AppException(ex);
        }
    }

    //
// onOK
//
    @Override
    protected boolean onOK() {
        if (dataSet.getCurrentRow() == null)
            return false;

        getDataObject().setChoiceValue(dataSet.getCurrentRow());

        return true;
    }

    //
//
//
    public static final String LOV_ORDER_BY = "LOV_ORDER_BY";
    public static final String LOV_HEIGHT = "LOV_HEIGHT";
    public static final String LOV_WIDTH = "LOV_WIDTH";
//
//
//
}
