package ru.inversion.fx.form.lov;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.utils.converter.TypeConverter;

/**
 * @author ssu @
 */
public class JInvLOVChoiceDialog extends ViewLovController<Object[], PLovParam> {

    private SQLDataSet<Object[]> dataSet;

    @Override
    protected void init() throws Exception {
        super.init();
        final String sql = buildSqlFromLov();

        dataSet = new SQLDataSet<>();
        dataSet.setSQL(sql);
        dataSet.setRowClass(Object[].class);
        dataSet.setCallbackParameters(getLov().getParameters());
        dataSet.setTaskContext(getLov().getTaskContext());

        if (getLov().getProperty("ru.inversion.dataset.query_alias") != null) {
            dataSet.setQueryAlias((String) getLov().getProperty("ru.inversion.dataset.query_alias"));
        }

        super.initDialog(dataSet, false);
    }

    /**
     * Returns the currently selected item in the dialog.
     */
    @Override
    public final Object getSelectedItem() {

        if (dataSet.isEmpty()) {
            return null;
        }

        Object[] ao = dataSet.getCurrentRow();
        if (ao == null || ao.length == 0) {
            return null;
        }

        Object value = ao[0];

        int i = -1;

        for (JInvLOVColumn column : getLov().getColumnList()) {

            i++;

            Property p = column.getProperty();

            if (p == null) {
                continue;
            }

            p.setValue(TypeConverter.convert(ao[i], column.getColumnClass()));
        }

        return value;
    }

    @Override
    protected DSFXAdapter<Object[]> initDSFXAdapter() {
        return new DSFXAdapter<Object[]>() {
            @Override
            protected void bindColumns( ICellValueChangeListener cellValueChangeListener) throws Exception {
                int i = -1;

                for (JInvLOVColumn column : getLov().getColumnList()) {
                    i++;
                    if (column.getWidth() == 0)
                        continue;

                    TableColumn<Object[], Object> tableColumn = new TableColumn<>(column.getTitle());
                    tableColumn.setId(column.getColumnName());
                    tableColumn.setUserData(i);

                    if (column.getWidth() != -1) {
                        //tableColumn.setMaxWidth(column.getWidth());
                        tableColumn.setPrefWidth(column.getWidth());
                    }

                    getTable().getColumns().add(tableColumn);
                    tableColumn.setCellValueFactory(param -> {
                        return new SimpleObjectProperty<>(param.getValue()[(Integer) param.getTableColumn().getUserData()]);
                    });
                }//end for

                getTable().setOnSort(new EventHandler<SortEvent<TableView<Object[]>>>() {
                    @Override
                    public void handle(SortEvent<TableView<Object[]>> event) {
                        try {
                            StringBuilder sb = new StringBuilder();
                            event.getSource().getSortOrder().forEach((t) -> {
                                sb.append(t.getId()).append(t.getSortType() == TableColumn.SortType.ASCENDING ? " ASC " : " DESC ").append(',');
                            });
                            if (sb.length() > 0) {
                                sb.deleteCharAt(sb.length() - 1);
                                ((ISQLDataSet) getDataSet()).setOrderBy(sb.toString());
                                dataSet.executeQuery();
                            } else {
                                ((ISQLDataSet) getDataSet()).setOrderBy(null);
                            }
                        } catch (DataSetException ex1) {
                            JInvErrorService.handleException(getTable().getScene().getWindow(), ex1);
                        }
                    }
                });
            }
        };
    }
}
