package ru.inversion.fx.form.lov;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;
import static javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.StateEnum;
import ru.inversion.fx.form.controls.JInvLovSearchPane;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.utils.S;

/**
 * @param <T> тип записи DataSet
 */
public abstract class BaseChoiceDialog<T> extends Dialog<Object> implements IFormStateListener {

    private final Window parent;
    private final String filterValue;
    public final TableView<T> tableView;
    public final JInvLOV lov;
    private final JInvLovSearchPane lovSearchPane;
    private final DialogPane dialogPane;
    private final DSFXAdapter<T> dsfxAdapter;

    public BaseChoiceDialog(Window parent, JInvLOV lov, String filterValue) {
        this.parent = parent;
        this.filterValue = filterValue;
        this.tableView = new JInvTable<>();
        this.lov = Objects.requireNonNull(lov, "lov can't be null");
        this.lovSearchPane = new JInvLovSearchPane();
        this.dialogPane = getDialogPane();
        this.dsfxAdapter = Objects.requireNonNull(initDSFXAdapter(), "DSFXAdapter can't be null");


        setTitle(lov.getTitle());
        initOwner(parent);
        setResizable(true);
        updateGrid();
        setResultConverter(dialogButton -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? getSelectedItem() : null;
        });
    }

    protected abstract DSFXAdapter<T> initDSFXAdapter();

    public abstract Object getSelectedItem();

    protected void startDialog(SQLDataSet<T> dataSet, boolean enableMark) {
        try {
            dsfxAdapter.bindTable(dataSet, tableView, null, enableMark);
            initDialog();
            ViewPrefAppService.localizeDialog(dialogPane);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    private void initDialog() {
        int width = tableView.getColumns().stream().mapToInt((c) -> (int) c.getPrefWidth()).sum();

        if (width > 1200) {

            width = 1200;

            tableView.setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);

            dialogPane.setPrefWidth((double) width);
        } else {
            tableView.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        }

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final Button btOK = (Button) dialogPane.lookupButton(ButtonType.OK);
        btOK.setDefaultButton(false);

        dialogPane.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if ((event.isControlDown() && event.getCode() == KeyCode.ENTER) || event.getCode() == KeyCode.F9) {
                    btOK.fire();
                }
            }
        });

        tableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.getClickCount() > 1) {
                    btOK.fire();
                }
            }//end if
        });

        showingProperty().addListener(new ChangeListener<Boolean>() {
            //Сработали разок – и хватит
            boolean triggeredOnce = false;

            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                if ( triggeredOnce ){
                    return;
                }

                try {
                    BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(dialogPane.getScene().getRoot());
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
                triggeredOnce = true;
            }
        });

        lovSearchPane.init(tableView, false, null, true, filterValue, null);
        if (lov.isSmallLov()) {
            lovSearchPane.setMaxPageSize(lov.getMaxCountRow());
        }
        lovSearchPane.setListener(this);
    }

    private void updateGrid() {
        VBox vb = new VBox();
        vb.getChildren().addAll(lovSearchPane, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getDialogPane().setContent(vb);

        Platform.runLater(tableView::requestFocus);
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

    public Window getParent() {
        return parent;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public TableView<T> getTableView() {
        return tableView;
    }

    public JInvLOV getLov() {
        return lov;
    }

    public JInvLovSearchPane getLovSearchPane() {
        return lovSearchPane;
    }

    public DSFXAdapter<T> getDsfxAdapter() {
        return dsfxAdapter;
    }
}
