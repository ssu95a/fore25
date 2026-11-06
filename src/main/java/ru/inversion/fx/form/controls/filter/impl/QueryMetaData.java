package ru.inversion.fx.form.controls.filter.impl;

import ru.inversion.dataset.parser.SQLParser;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.scheck.JInvStringWorker;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author perov
 */

class QueryMetaData {
    /*
    final String query;

    final Connection con;

    private List<String> columns;

    public  List<String> getColumns() {
        return columns;
    }
   
    public QueryMetaData(Connection con, String query) {

        if( con == null || query == null ) {
            throw new IllegalArgumentException( "con or query is null" );
        }
        this.query = query;
        this.con   = con;
    }
    
    public QueryMetaData getMetaData() throws SQLException {

        SQLParser sp = new SQLParser(query);

        String metaDataQuery = sp.prepareSql4MetaData();

        try( PreparedStatement ps = con.prepareCall(metaDataQuery); ResultSet rs = ps.executeQuery() )
        {
            ResultSetMetaData rsmd = rs.getMetaData();

            columns = new ArrayList<>(rsmd.getColumnCount());

            for( int i=1; i <= rsmd.getColumnCount(); i++ ) {

                 String columnName = rsmd.getColumnName(i);

                 if( JInvStringWorker.INSTANCE().checkRegExp( "[а-яА-Я\\s]", 0, columnName )  )
                     columnName = S.quote(columnName);

                 columns.add( columnName );
            }
        }
        return this;
    }
    */
    /** */
    static public List<String> getQueryColumnsNames( TaskContext tc, String sqlQuery ) throws Exception
    {
        if( S.isNullOrEmpty(sqlQuery) )
            return Collections.emptyList();

        final String metaDataQuery;
        {
            final SQLParser sp = new SQLParser(sqlQuery);
            metaDataQuery = sp.prepareSql4MetaData();
        }

        List<String> columnsNames;

        try( PreparedStatement ps = tc.getConnection().prepareStatement(metaDataQuery); ResultSet rs = ps.executeQuery() )
        {
            final ResultSetMetaData rsmd = rs.getMetaData();

            columnsNames = new ArrayList<>(rsmd.getColumnCount());

            for( int i = 1; i <= rsmd.getColumnCount(); i++ ) {

                String columnName = rsmd.getColumnName(i);

                if( JInvStringWorker.INSTANCE().checkRegExp( "[а-яА-Я\\s]", 0, columnName )  )
                    columnName = S.quote(columnName);

                columnsNames.add( columnName );
            }
        }
        return columnsNames;
    }
}
