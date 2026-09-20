package ru.inversion.fx.form.lov;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.dataset.mark.OARow;
import ru.inversion.dataset.mark.OARowIDMarkable;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.utils.converter.TypeConverter;

import java.sql.ResultSetMetaData;
import java.util.Objects;

public class JInvLOVChoiceDialogMarkable<T extends OARow & IMarkable> extends ViewLovController<T, PLovMarkableParam<T>> {

    private XXIDataSet<T> dataSet;

    private PLovMarkableParam<T> paramObject;

    private Class<T> rowClass;

    @Override
    protected void init() throws Exception {
        super.init();

        this.paramObject = Objects.requireNonNull(getDataObject(), "paramObject can't be null");

        this.rowClass = Objects.requireNonNull(paramObject.getRowClass(), "row class can't be null");

        final String sql = buildSqlFromLov();

        dataSet = new XXIDataSet<T>();
        dataSet.setSQL(sql);
        dataSet.setRowClass(Objects.requireNonNull(rowClass, Tags.PRODUCT_LABEL + "'rowClass' is null"));
        dataSet.setCallbackParameters(getLov().getParameters());
        dataSet.setTaskContext(getLov().getTaskContext());
        final Class classTo = classTo(rowClass);

        dataSet.setRowMapper((rs, rowNum) -> {
            final ResultSetMetaData metaData = rs.getMetaData();

            int nColumn = metaData.getColumnCount();

            Object[] data = new Object[nColumn-1];

            for (int j = 2; j <= nColumn; j++) {
                data[j - 2] = j == 2 ? TypeConverter.convert(rs.getObject(j), classTo) : rs.getObject(j);
            }

            T t = null;
            try {
                t = rowClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(Tags.PRODUCT_LABEL + "Error on create mark support instance obj. class: " + rowClass.getName() );
            }

            t.setData(data);
            return t;
        });

        if (getLov().getProperty("ru.inversion.dataset.query_alias") != null) {
            dataSet.setQueryAlias((String) getLov().getProperty("ru.inversion.dataset.query_alias"));
        }

        super.initDialog(dataSet, true);
    }

    /**
     * Returns the currently selected item in the dialog.
     */
    @Override
    public final Object getSelectedItem() {

        if (dataSet.isEmpty()) {
            return getMarkerId();//null;
        }

        OARow ao = dataSet.getCurrentRow();
        if (ao == null || ao.getData().length == 0) {
            return getMarkerId();//null;
        }

        Object value = ao.get(0);

        int i = -1;

        for (JInvLOVColumn column : getLov().getColumnList()) {

            i++;

            Property p = column.getProperty();

            if (p == null) {
                continue;
            }

            p.setValue(TypeConverter.convert(ao.get(i), column.getColumnClass()));
        }

        //return value;
        return getMarkerId();
    }

    public Long getMarkerId() {
        return dataSet.getMarkerID();
    }

    @Override
    protected DSFXAdapter<T> initDSFXAdapter() {
        return new DSFXAdapter<T>() {
            @Override
            protected void bindColumns( ICellValueChangeListener cellValueChangeListener) throws Exception {
                int i = 0;
                for (JInvLOVColumn column : getLov().getColumnList()) {
                    if (column.getWidth() == 0) continue;

                    TableColumn<T, Object> tableColumn = new TableColumn<>(column.getTitle());
                    tableColumn.setId(column.getColumnName());
                    tableColumn.setUserData(i++);

                    if (column.getWidth() != -1) {
                        //tableColumn.setMaxWidth(column.getWidth());
                        tableColumn.setPrefWidth(column.getWidth());
                    }

                    getTable().getColumns().add(tableColumn);
                    tableColumn.setCellValueFactory(param -> {
                        return new SimpleObjectProperty<>(param.getValue().getData()[(Integer) param.getTableColumn().getUserData()]);
                    });
                }//end for

                getTable().setOnSort(new EventHandler<SortEvent<TableView<T>>>() {
                    @Override
                    public void handle(SortEvent<TableView<T>> event) {
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
                initMarkColumn(getTable());
            }
        };
    }

    /** */
    private Class classTo( Class rowClass ) {
        if( rowClass == OARowIDMarkable.class )
            return Long.class;
        else
            return String.class;
    }
}
