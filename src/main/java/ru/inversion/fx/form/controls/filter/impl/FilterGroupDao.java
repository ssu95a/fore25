package ru.inversion.fx.form.controls.filter.impl;

import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.db.expr.SQLExpressionFactory;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterGroup;
import ru.inversion.tc.TaskContext;

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FilterGroupDao {

    private final static URL PLSQL_XML = FilterGroupDao.class.getResource("plsql/def.xml");

    private final TaskContext taskContext;

    public FilterGroupDao(TaskContext taskContext) {
        this.taskContext = taskContext;
    }

    public void save(PFrmFilterGroup filterGroup) throws SQLException, SQLExpressionException {
        final Connection connection = taskContext.getConnection();
        final Map<String, Object> params = new HashMap<>();
        params.put("ID", filterGroup.getID());
        params.put("CNAME", filterGroup.getCNAME());
        try {
            SQLExpressionFactory.INSTANCE().execute(PLSQL_XML, "filter.group.save", connection, params);
            connection.commit();
        } catch (SQLException | SQLExpressionException e) {
            connection.rollback();
            throw e;
        }
    }

    public void update(PFrmFilterGroup filterGroup, long id) throws SQLException, SQLExpressionException {
        final Connection connection = taskContext.getConnection();
        final Map<String, Object> params = new HashMap<>();
        params.put("ID", filterGroup.getID());
        params.put("CNAME", filterGroup.getCNAME());
        params.put("OLD_ID", id);
        try {
            SQLExpressionFactory.INSTANCE().execute(PLSQL_XML, "filter.group.update", connection, params);
            connection.commit();
        } catch (SQLException | SQLExpressionException e) {
            connection.rollback();
            throw e;
        }
    }

    public void delete(PFrmFilterGroup filterGroup) throws SQLException, SQLExpressionException {
        final Connection connection = taskContext.getConnection();
        final Map<String, Object> params = new HashMap<>();
        params.put("ID", filterGroup.getID());
        try {
            SQLExpressionFactory.INSTANCE().execute(PLSQL_XML, "filter.group.delete", connection, params);
            connection.commit();
        } catch (SQLException | SQLExpressionException e) {
            connection.rollback();
            throw e;
        }
    }
}
