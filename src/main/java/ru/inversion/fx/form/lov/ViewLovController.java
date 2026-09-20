package ru.inversion.fx.form.lov;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.StateEnum;
import ru.inversion.fx.form.controls.JInvLovSearchPane;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.utils.S;

import java.util.Objects;

import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;

/**
 * @param <T> тип записи DataSet
 * @param <P> тип параметра, который будет передаваться котроллеру
 */
public abstract class ViewLovController<T, P extends PLovParam> extends JInvFXFormController<P> {

    @FXML
    private AnchorPane rootAnchorPane;
    @FXML
    private VBox root;
    @FXML
    private JInvToolBar toolBar;
    @FXML
    private Pane tablePane;

    private PLovParam paramObject;
    private JInvLOV lov;
    private String filter;
    private JInvLovSearchPane lovSearchPane;
    private DSFXAdapter<T> dsfx;
    private JInvTable<T> table;

    @Override
    protected void init() throws Exception {
        this.paramObject = Objects.requireNonNull(getDataObject(), "lov param can't be null");

        this.lov = Objects.requireNonNull(paramObject.getLov(), "lov can't be null");
        this.filter = paramObject.getFilter();
        this.lovSearchPane = new JInvLovSearchPane();
        this.dsfx = Objects.requireNonNull(initDSFXAdapter(), "DSFXAdapter can't be null");
        this.table = new JInvTable<>();

        setTitle(lov.getTitle());
        getViewContext().getStageOrPrimaryStage().setResizable(true);
    }

    @Override
    protected boolean onOK() {
        paramObject.setResult(getSelectedItem());
        return true;
    }

    protected void initDialog(SQLDataSet<T> dataSet, boolean enableMark) {
        try {

            dsfx.bindTable(dataSet, table, null, enableMark);
            table.setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);

            final Button btOK = getStandartButton(ButtonType.OK);
            if( btOK != null )
                btOK.disableProperty().bind( dsfx.emptyProperty() );

/*            
            int width = table.getColumns().stream().mapToInt((c) -> (int) c.getPrefWidth()).sum();

            if (width > 1200) {
                width = 1200;
                table.setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);

                rootAnchorPane.setPrefWidth(width);
            } else {
                table.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
            }
*/
            lovSearchPane.init(table, false, null, true, filter, null);
            if (lov.isSmallLov()) {
                lovSearchPane.setMaxPageSize(lov.getMaxCountRow());
            }
            lovSearchPane.setListener(this);

            lovSearchPane.prefWidthProperty().bind(toolBar.widthProperty().subtract(15L));

            toolBar.getItems().add(lovSearchPane);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        setChoiceControl(table);
if( 0 > 1)
        for (TableColumn<T, ?> pTableColumn : table.getColumns()) {
            pTableColumn.setCellFactory(tc -> {
                TableCell cell = new TableCell();
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

        tablePane.getChildren().add(table);
    }

    protected abstract DSFXAdapter<T> initDSFXAdapter();

    public abstract Object getSelectedItem();

    protected String buildSqlFromLov() {
        int columnNum = -1, i = 0;

        StringBuilder sb = new StringBuilder();
        StringBuilder sbFilter = new StringBuilder("where upper(");

        sb.append("select ");

        for (JInvLOVColumn c : lov.getColumnList()) {

            if (columnNum == -1 && c.getWidth() != 0) {
                columnNum = i;
                //break;
            }

            if (i > 0) {
                sb.append(',');
                sbFilter.append("||");
            }

            sb.append(c.getColumnName());
            sbFilter.append(c.getColumnName());
            i++;
        }

        sbFilter.append(") like upper(:lov_parameter)");
        sb.append(" from (")
                .append(lov.getSqlSelect())
                .append(") qrslt ").append(sbFilter);

        return sb.toString();
    }

    protected ObjectProperty<StateEnum> state = new SimpleObjectProperty<>(StateEnum.ACTIVE);

    @Override
    public ObjectProperty<StateEnum> stateProperty() {
        return state;
    }

    protected StringProperty stateText = new SimpleStringProperty( S.EMPTY_STRING );

    @Override
    public StringProperty stateTextProperty() {
        return stateText;
    }

    public String getFilter() {
        return filter;
    }

    public JInvTable getTable() {
        return table;
    }

    public JInvLOV getLov() {
        return lov;
    }

    public JInvLovSearchPane getLovSearchPane() {
        return lovSearchPane;
    }

    public DSFXAdapter<T> getDsfxAdapter() {
        return dsfx;
    }
}
