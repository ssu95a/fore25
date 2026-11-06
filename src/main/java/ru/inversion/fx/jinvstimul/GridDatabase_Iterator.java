/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvstimul;

import com.stimulsoft.base.exception.*;
import com.stimulsoft.report.dictionary.data.*;
import com.stimulsoft.report.dictionary.dataSources.*;
import com.stimulsoft.report.dictionary.databases.*;
import javafx.collections.*;
import javafx.scene.control.*;
import org.slf4j.*;
import ru.inversion.dataset.*;
import ru.inversion.dataset.fx.*;
import ru.inversion.dataset.mark.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.jinvstimul.property.*;
import ru.inversion.meta.*;
import ru.inversion.utils.*;

import java.time.*;
import java.util.*;
//import ru.inversion.ds.reflection.JInvReflectionUtils;

/**
 *
 * @author polyatykina
 */
class GridDatabase_Iterator extends StiDatabase {

    protected static Logger log = LoggerFactory.getLogger( GridDatabase_Iterator.class );

    private static final boolean D = false;

    public GridDatabase_Iterator(JInvTable grid , SizeExportedData sed) {
        super("grid.data");// Database name
        data = grid;
        sizeExportedData = sed;
    }
    public GridDatabase_Iterator( ) {
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

        ObservableList<TableColumn> listColumns = data.getColumns();
        List<String> listIdColumns = new ArrayList<>();
        String idColumns = null;

        if (listColumns == null || listColumns.isEmpty() )
            return;

        if(D) logger.trace("listColumns.size = "+listColumns.size());

        columnsToListID(listColumns, new Ref<>(listIdColumns));

        if( sizeExportedData == SizeExportedData.FULL_DATA )
        {
            Class row = data.getItems().get(0).getClass();

            final Collection<IEntityProperty> values = EntityMetadataFactory.getEntityMetaData(row).getPropertiesMap().values();

            for( IEntityProperty fld : values )
            {
                if( !listIdColumns.contains(fld.getColumnName()) )
                {
                    listIdColumns.add(fld.getColumnName() );
                }
            }
        }

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
//        }

        Object row;

       //DSFXAdapter da = data.getDataSetAdapter();
        //PropertyDescriptor pd = null;
        IEntityProperty pd = null;

        boolean isAddData = false;
        int sizeData = (cashedData==null)?listRow.size():cashedData.size();
//        ((ISQLDataSet) data.getDataSetAdapter().getDataSet()).createRSIterator(false);
        log.debug( "****************** * USING ISQLDataSet ITERATOR * **********************" );
        int i = 0;
        for (Iterator it = ((ISQLDataSet) data.getDataSetAdapter().getDataSet()).createRSIterator(false); it.hasNext();) {
        //for( int i = 0; i< sizeData; i++ )
//        {
           i++;

            //isAddData = false;
            //data.getDataSetAdapter().getRows().;
//            row = (cashedData==null)?listRow.get(i):cashedData.get(i);
            row = it.next();
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
