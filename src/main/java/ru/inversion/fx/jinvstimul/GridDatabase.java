/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvstimul;

import com.stimulsoft.base.exception.StiException;
import com.stimulsoft.report.dictionary.data.DataRow;
import com.stimulsoft.report.dictionary.data.DataTable;
import com.stimulsoft.report.dictionary.dataSources.StiDataStoreSource;
import com.stimulsoft.report.dictionary.databases.StiDatabase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.IParameters;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.StubBooleanObservableValue;
import ru.inversion.dataset.fx.StubObservableValue;
import ru.inversion.dataset.mark.IMarkable;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.jinvstimul.property.Ref;
import ru.inversion.fx.jinvstimul.property.SizeExportedData;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
//import ru.inversion.ds.reflection.JInvReflectionUtils;

/**
 *
 * @author polyatykina
 */
class GridDatabase extends StiDatabase {

    private static final boolean D = false;

    public GridDatabase( JInvTable grid , SizeExportedData sed) {
        super("grid.data");// Database name
        data = grid;
        sizeExportedData = sed;
    }
    public GridDatabase( ) {
        super("grid.data");// Database name
    }

    private SizeExportedData sizeExportedData = SizeExportedData.FULL_TABLE;
    public JInvTable data;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("ru.inversion.fx.jinvstimul.GridDatabase");

    private void columnsToListID(List<TableColumn> listColumns, Ref<List<String>> listIdColumns){
        for(TableColumn column : listColumns){
            if (!column.isVisible() && sizeExportedData == SizeExportedData.DISPLAYED_TABLE )
                continue;
            if(column.getColumns().isEmpty()){
                String idColumns = Controls.getFieldNameFromTableColumn(column);

                if (S.isNullOrEmpty(idColumns))
                    continue;

                listIdColumns.get().add(idColumns);
            }
            else{
                List<TableColumn> listCol = column.getColumns();
                columnsToListID(listCol, listIdColumns);
            }
        }
    }

    public void setDataStoreSource( StiDataStoreSource stiDataStoreSource ) throws StiException, DataSetException
    {
        if(D)logger.trace("\nSetDataStoreSource -->");

        if ( data == null || data.getItems()==null || data.getItems().size() == 0 )
        {
            if(D) logger.trace("\nSource is null");
            if(D) logger.trace("\nSetDataStoreSource <--");
            return;
        }

        DataTable dataTable = stiDataStoreSource.createNewTable();

        Class classEntity = null;

        List<TableColumn> listColumns = data.getColumns();
        List<String> listIdColumns = new ArrayList<>();
        String idColumns = null;

        if (listColumns == null || listColumns.isEmpty() )
            return;

        if(D) logger.trace("listColumns.size = "+listColumns.size());

        columnsToListID(listColumns, new Ref<>(listIdColumns));
        if(sizeExportedData == SizeExportedData.FULL_DATA){
            Class row = data.getItems().get(0).getClass();

            final Collection<IEntityProperty> values = EntityMetadataFactory.getEntityMetaData(row).getPropertiesMap().values();

            for( IEntityProperty fld : values )
            {
                if(!listIdColumns.contains(fld.getColumnName())){
                    listIdColumns.add(fld.getColumnName());
                }
            }
        }

//        for (TableColumn column : listColumns)
//        {
//            if (!column.isVisible() && sizeExportedData == SizeExportedData.DISPLAYED_TABLE )
//                continue;
//            if(column.getColumns().isEmpty()){
//                idColumns = Controls.getFieldNameFromTableColumn(column);
//
//                if (S.isNullOrEmpty(idColumns))
//                    continue;
//
//                listIdColumns.add(idColumns);
//            }
//            else{
//                List<TableColumn> listColumnsDet = column.getColumns();
//
//                for(TableColumn col_det : listColumnsDet){
//                    idColumns = Controls.getFieldNameFromTableColumn(col_det);
//
//                    if (S.isNullOrEmpty(idColumns))
//                        continue;
//
//                    listIdColumns.add(idColumns);
//
//                }
//            }
//        }

        ObservableList<Object> listRow = data.getItems();
        List cashedData = null;
        boolean if_markable = false;

        for(int i = 0; i< listRow.size(); i++ ){
            Object r = listRow.get(i);
            if(r instanceof IMarkable ){
                if(((IMarkable)r).isMark()){
                    if_markable = true;
                    break;
                }
            }
        }

        if(D) logger.trace("if_markable  = "+if_markable);


        boolean isSwappingData = false;
//        if(!if_markable)
//        {
            if(Controls.getDsAdapterFromControl(data).getDataSet() instanceof XXIDataSet)
            {
                XXIDataSet baseDataSet = (XXIDataSet)Controls.getDsAdapterFromControl(data).getDataSet();

                XXIDataSet cashedDataSet = new XXIDataSet();
                cashedDataSet.setTaskContext( baseDataSet.getTaskContext() );
                cashedDataSet.setSQL        ( baseDataSet.getSQL() );
                cashedDataSet.setWherePredicat( baseDataSet.getCompletedWherePredicat() );
                cashedDataSet.setOrderBy    ( baseDataSet.getOrderBy() );
                cashedDataSet.setRowClass   ( baseDataSet.getRowClass());
                cashedDataSet.setQueryAlias ( baseDataSet.getQueryAlias() );
                cashedDataSet.setRowMapper  ( baseDataSet.getRowMapper() );

                cashedDataSet.setCallbackParameters(new IParameters() {

                    final IParameters baseParameters = U.nvl( baseDataSet.getCallbackParameters(), IParameters.EMPTY );

                    @Override
                    public Object getParameter( String parameterName ) {
                        if( baseParameters.hasParameter( parameterName ) )
                            return baseParameters.getParameter( parameterName );
                        return baseDataSet.getParameter( parameterName );
                    }

                    @Override
                    public Object getParameter( int parameterIndex ) {
                        if( baseParameters.hasParameter( parameterIndex ) )
                            return baseParameters.getParameter( parameterIndex );
                        return baseDataSet.getParameter( parameterIndex );
                    }
                });

                try {
                    cashedDataSet.executeQuery(true);
                } catch (Exception ex) {
                    throw new DataSetException( "JInvStimul: error on preparer and execute DataSet.", ex );
                }

                cashedData = cashedDataSet.getRows();
            }
            else
            {
                while( true )
                {
                    if (!isSwappingData){
                        if(D) logger.trace("\nswappingData");
                        isSwappingData = true;
                    }

                    try
                        {
                            if (!Controls.getDsAdapterFromControl(data).getDataSet().swappingData())
                             break;
                        }
                    catch(DataSetException ex){ break; }
                }
            }
//        }

        Object row;

       //DSFXAdapter da = data.getDataSetAdapter();
        //PropertyDescriptor pd = null;
        IEntityProperty pd = null;

        boolean isAddData = false;
        int sizeData = (cashedData==null)?listRow.size():cashedData.size();
        for( int i = 0; i< sizeData; i++ )
        {

            //isAddData = false;
            //data.getDataSetAdapter().getRows().;
            row = (cashedData==null)?listRow.get(i):cashedData.get(i);
//            boolean if_row_marked = false;
//            if(row instanceof IMarkable ){
//                if(((IMarkable)row).isMark()){
//                    if_row_marked = true;
//                }
//            }
//
//            if(D) logger.trace("if_row_marked  = "+if_row_marked);
//
//            if(!if_markable || if_row_marked){


                DataRow dataRow = dataTable.createNewRow();


                if (classEntity == null )
                    classEntity = row.getClass();

                if(D) logger.trace("listIdColumns.size = "+listIdColumns.size());

                for( String idColumn : listIdColumns)
                {
                    //pd = JInvReflectionUtils.getPropertyDescriptor(classEntity, idColumn);
                    pd = EntityMetadataFactory.getEntityMetaData( classEntity ).getProperty( idColumn );


                    if (pd == null){
                        //logger.trace("pd==null");
                        continue;
                    }

                    /*if( pd.isTransient() )
                    {
                        column.setSortable(false);
                        column.getProperties().put(COLUMN_TRANSIENT, Boolean.TRUE );
                    }*/
                    if ( pd.getType() == Boolean.class )
                    {
                        //final StubBooleanObservableValue obVal = new StubBooleanObservableValue(pd.getReadMethod(), pd.getWriteMethod(), idColumn, null );
                        final StubBooleanObservableValue obVal = new StubBooleanObservableValue( pd, null );
                        obVal.setPojoInstance( row );
                        Boolean val = obVal.getValue();
                        if(D) logger.trace(idColumn+"  *  "+pd.getType().toString()+"  *  "+val.toString() + "  *  "+ val.getClass().toString());
                        if ( val!=null && !val.toString().isEmpty() ){
                                dataRow.addCell( idColumn, val.toString() );
                                isAddData = true;
                        }

                    }
                    else if ( pd.getType() == String.class )
                    {
                        //final StubObservableValue obVal = new StubObservableValue(pd.getReadMethod(), pd.getWriteMethod(), idColumn, null );
                        final StubObservableValue obVal = new StubObservableValue( pd, idColumn, null );
                        obVal.setPojoInstance( row );

                        Object val = obVal.getValue();
                        //logger.trace(idColumn+"  |  "+pd.getType().toString()+"  |  "+val == null ? "NULL":val.toString());
                        if(D) logger.trace(idColumn+"  |  ");
                        if(val == null){
                            if(D) logger.trace("val = null");
                            val = "";
                        }
                        else{
                            if(D) logger.trace("val = "+val);
                        }

                       // if ( val!=null && !val.toString().isEmpty()
                             //   && val.toString().trim() != null && !val.toString().isEmpty() ){
                                //logger.trace("begin");
                                //logger.trace("idColumn = "+idColumn.toString());
                                //logger.trace("val = "+val.toString());
                                dataRow.addCell( idColumn, val );
                                isAddData = true;
                                //logger.trace("end");
                        //}
                    }
//                    else if(pd.getType() == LocalDate.class){
//                        final StubObservableValue obVal = new StubObservableValue( pd, idColumn, null );
//                        obVal.setPojoInstance( row );
//                        Object val = obVal.getValue();
//                        StiDateTime sdt = new StiDateTime();
//                        LocalDate ld = ((LocalDate) val);
//
//                        sdt.set(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
//                        dataRow.addCell( idColumn, sdt );
//                        isAddData = true;
//
//                    }
                    else
                    {
                        //final StubObservableValue obVal = new StubObservableValue(pd.getReadMethod(), pd.getWriteMethod(), idColumn, null );
                        final StubObservableValue obVal = new StubObservableValue( pd, idColumn, null );
                        obVal.setPojoInstance( row );
                        Object val = obVal.getValue();
                        if(LocalDate.class == pd.getType()){
                            //ateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                            //val = ((LocalDate)obVal.getValue()).format(formatter);
                            if(val != null){
                                val = asDate((LocalDate)val);
                            }
                            else{
                                val = null;
                            }
                        }
                        if ( pd.getType() == LocalDateTime.class ) {
                            if ( val != null ) {
                                val = asDate( (LocalDateTime) val );
                            } else {
                                val = null;
                            }
                        }
//                        if(val == null){
//                            val = "";
//                        }
//
//try{
//                        logger.trace("COLUMN = "+idColumn);
//                        logger.trace("pd.getType()="+pd.getType().toString());
//                        if(val != null){
//                        logger.trace("val class =  "+ val==null?"<NULL-type>":val.getClass().toString());
//                        logger.trace("val = "+val==null?"<NULL>":val.toString());
//                        }
//}catch(Exception ex){
//logger.trace(idColumn+"-> EXCEPTION");
//}

//
                        if ( val!=null && !val.toString().isEmpty() && S.isNotNullOrEmpty(idColumn) ){
                                dataRow.addCell( idColumn, val );

                                isAddData = true;
                        }
//                        else
//                        {
//                            if(pd.getType() == BigDecimal.class){
//                                dataRow.addCell( idColumn, "" );
//                            }
//                        }
                    }
                }
//            }
        }
        if (isAddData)
            stiDataStoreSource.setDataTable(dataTable);

        if(D)logger.trace("\nSetDataStoreSource <--");
    }

    public static Date asDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    private static Date asDate( LocalDateTime localDate ) {
        return Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public void connect(StiDataStoreSource stiDataStoreSource) throws StiException {

        if ( stiDataStoreSource == null )
        {
            if(D)logger.trace("\nstiDataStoreSource = NULL" );
            return;
        }

        if ( stiDataStoreSource.getColumns() == null || stiDataStoreSource.getColumns().size() == 0 )
        {
            if(D)logger.trace("\nstiDataStoreSource getColumns = NULL" );
            return;
        }

        try {
            setDataStoreSource( stiDataStoreSource );
        } catch (DataSetException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
    }

}
