package ru.inversion.fx.form.controls.filter.impl;

import ru.inversion.datacall.IDataCall;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.db.DBUniqueResult;
import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.db.expr.SQLExpressionFactory;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterFull;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityDao;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Всё выполняется в BaseApp.APP().getCommonTaskContext() 
 * Свои соединения передавать смысла нет!
 * @author perov
 */
public class FilterWork {

    public final static URL PLSQL_XML = FilterWork.class.getResource("plsql/def.xml");

    private static TaskContext tc() {
        return BaseApp.APP().getCommonTaskContext();
    }

    /**
     * @param key название параметра
     * @param value новое значение параметра
     */
    public static void setPreference( String key, String value) throws SQLExpressionException {
        final Map<String, Object> params = new HashMap<>();
        params.put("KEY", key);
        params.put("VALUE", value);
        executeProcedure("filter.work.setPreference", params);
    }

    /**
     * Сохранение фильтра
     */
    public static Long saveFilter( Connection con, String formName, String filter, String blockName, String whereName) throws SQLExpressionException {
        if (formName != null && filter != null && blockName != null && whereName != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FORM_NAME", formName);
            params.put("USER", tc().getUserName().toUpperCase());
            params.put("WHERE_NAME", whereName);
            params.put("WHERE_BLK", filter);
            params.put("AUTO_SETUP", null);
            params.put("BLOCK_NAME", blockName);
            executeProcedure("filter.work.saveFilter", params);
            return TypeConverter.convert( params.get("IFF_INSERT"), Long.class);
        }
        return null;

    }

    /**
     * Добавляем фильтр в группу
     */
    public static void addGroupLink(Connection con, Long groupId, Long filterId) throws SQLExpressionException {

        if (groupId != null && filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("GROUP_ID", groupId);
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.addGroupLink", params);
        }
    }

    public static void delGroupLink(Connection con, Long groupId, Long filterId) throws SQLExpressionException {
        if (groupId != null && filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("GROUP_ID", groupId);
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.delGroupLink", params);
        }
    }

    /**
     * Добавляем фильтр в группу
     */
    public static void addAllGroupLink(Connection con, Long filterId) throws SQLExpressionException {
        if (filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.addAllGroupLink", params);
        }
    }

    public static void delAllGroupLink(Connection con, Long filterId) throws SQLExpressionException {
        if (filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.delAllGroupLink", params);
        }
    }

    /**
     * Добавляем ODB группу
     */
    public static void addODBGrp(Connection con, Long groupId, Long filterId) throws SQLExpressionException {
        if (groupId != null && filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("GROUP_ID", groupId);
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.addODBGrp", params);
        }
    }

    /**
     * Удаляем группу
     */
    public static void delODBGrp(Connection con, Long groupId, Long filterId) throws SQLExpressionException {
        if (groupId != null && filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("GROUP_ID", groupId);
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.delODBGrp", params);
        }
    }

    public static void addAllODBGrp(Connection con, Long filterId) throws SQLExpressionException {
        if (filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.addAllODBGrp", params);
        }
    }

    public static void delAllODBGrp(Connection con, Long filterId) throws SQLExpressionException {
        if (filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.delAllODBGrp", params);
        }
    }

    /**
     * Формирование имени свойства для сохранения в Pref
     */
    public static String getPreferenceKey(String formName, String blockName) 
    {
// сюда приходят подрезанные до 30 названия, причем с хэшами        
        if (formName.length () > 23)
            formName = formName.substring (formName.length () - 23);
        if (blockName.length () > 23)
            blockName = blockName.substring (blockName.length () - 23);
        
        return "M_FILTER." + formName + "." + blockName + ".OPT";
//        try {
//            return "M_FILTER." + S.trimLongString(formName, 23) + "." + S.trimLongString(blockName, 23) + ".OPT";
//        } catch (Exception ex) {
//        }
//        return S.EMPTY_STRING;
    }

    public static boolean isRunOnForm(Connection con, String formName, String blockName) throws SQLExpressionException {
        return getPreference(
                getPreferenceKey(formName, blockName)
        )
                .filter(s -> s.charAt(0) == 'Y')
                .isPresent();
    }

    public static void editFilter(Connection con, PFrmFilterFull pflt) throws SQLExpressionException {
        if (pflt != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FORM_NAME", pflt.getCFORMNAME());
            params.put("USER", pflt.getCUSER());
            params.put("WHERE_BLK", pflt.getCWHEREBLK());
            params.put("WHERE_NAME", pflt.getCWHERENAME());
            params.put("BLOCK_NAME", pflt.getCBLOCKNAME());
            params.put("ID", pflt.getID());
            executeProcedure("filter.work.editFilter", params);
        }
    }

    public static void delFilter(Connection con, Long filterId) throws SQLExpressionException {
        if( filterId != null ) {
            final Map<String, Object> params = new HashMap<>();
            params.put( "FILTER_ID", filterId );
            executeProcedure( "filter.work.delFilter", params );
        }
    }

    public static void autoSetupFlt(Connection con, Long filterId, String autoSetup) throws SQLExpressionException {

        cleanAutoSetupFlt(null, autoSetup);

        if (filterId != null) {
            final Map<String, Object> params = new HashMap<>();
            params.put("FILTER_ID", filterId);
            params.put("AUTO_SETUP", autoSetup);
            executeProcedure("filter.work.autoSetupFlt", params);
        }
    }

    public static void cleanAutoSetupFlt(Connection con, String autoSetup) throws SQLExpressionException {
        final Map<String, Object> params = new HashMap<>();
        params.put("AUTO_SETUP", autoSetup);
        executeProcedure("filter.work.cleanAutoSetupFlt", params);
    }

    public static void copyFilter(Connection con, Long filterId, String user) throws SQLExpressionException {
        if (filterId != null && user != null && !user.isEmpty()) {
            final Map<String, Object> params = new HashMap<>();
            params.put("USER", user);
            params.put("FILTER_ID", filterId);
            executeProcedure("filter.work.copyFilter", params);
        }
    }

    static Optional<String> getPreference(String value) throws SQLExpressionException {

        if (S.isNotNullOrEmpty(value))
        {

            final IDataCall callGetPreference = SQLCallBuilder.NEW(tc()).url(PLSQL_XML).name("filter.work.getPreference").build();
            callGetPreference.set("UPPER_PREF", value);
            callGetPreference.set("DEF_VALUE" , null );

            callGetPreference.execute();

            return Optional.ofNullable( callGetPreference.getReturnValue() );

//            final Map<String, Object> params = new HashMap<>();
//            params.put("UPPER_PREF", value);
//            params.put("DEF_VALUE", null );
//
//            SQLExpressionFactory.INSTANCE().execute(PLSQL_XML, "filter.work.getPreference", tc().getConnection(), params);
//
//            return Optional.ofNullable((String) params.get("RES") );

        }

        return Optional.empty();
    }

    static void createBufferAndExecute(String procedureName, Map<String, Object> params) throws SQLExpressionException {
        SQLExpressionFactory.INSTANCE().execute(PFrmFilterFull.class, procedureName, tc().getConnection(), params);
    }

    static Optional<String> getBufferTab() throws SQLException {
        String strSQL = "SELECT COLUMN_VALUE FROM V_IE_BUFER_TAB";
        try (Statement stmt = tc().getConnection().createStatement();
                ResultSet rs = stmt.executeQuery(strSQL)) {
            StringBuilder sb = new StringBuilder();
            while (rs.next()) {
                sb.append(rs.getObject("COLUMN_VALUE"));
            }
            return Optional.of(sb.toString());
        }
    }

    /**
     * Метод создан для избавления от дублируешего кода
     */
    private static void executeProcedure(String procedureName, Map<String, Object> params) throws SQLExpressionException {
        try {
            SQLExpressionFactory.INSTANCE().execute(PLSQL_XML, procedureName, tc().getConnection(), params);
            tc().commit();
        } catch (SQLExpressionException e) {
            tc().rollback();
            throw e;
        }
    }

    static void cleanAllBuffer() throws SQLExpressionException {
        try {
            SQLExpressionFactory.INSTANCE().execute(PLSQL_XML, "filter.work.cleanAllBuffer", tc().getConnection());
            tc().commit();
        } catch (SQLExpressionException e) {
            tc().rollback();
            throw e;
        }
    }

    static Optional<String> getGroupNameById(Long id) throws SQLException {
        final String sql = "SELECT G.cName AS GROUP_NAME FROM FRM_FILTER_GROUP G WHERE G.ID = ?";
        try (final PreparedStatement ps = tc().getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (final ResultSet resultSet = ps.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.ofNullable(resultSet.getString("GROUP_NAME"));
                }
            }
        }
        return Optional.empty();
    }

    static Optional<PFrmFilterFull> getFilterById(Long ID) {

        if (ID != null) {
            DBUniqueResult<PFrmFilterFull> ur = new DBUniqueResult<>(PFrmFilterFull.class, PFrmFilterFull.class, "getById");
            return Optional.ofNullable(ur.execute(tc(), false, ID));
        }
        return Optional.empty();
    }

    @Entity
    @Table(name = "V_JF_FRM_FILTER")
    public static class PFilterBody {
        private long   ID;
        private String CWHEREBLK;
        @Id
        @Column(name="ID")
        public Long getID() {
            return ID;
        }
        public void setID(Long val) {
            ID = val;
        }
        @Column(name = "CWHEREBLK")
        public String getCWHEREBLK() {
            return CWHEREBLK;
        }
        public void setCWHEREBLK(String v) {
            this.CWHEREBLK = v;
        }
    }

    /** */
    private static IEntityDao<PFilterBody,Serializable> filterDao;

    /** */
    public static String loadFilterBody( Long filterId ) {

        if( filterId == null )
            return null;

        if( filterDao == null )
            filterDao = EntityMetadataFactory.getEntityMetaData(PFilterBody.class).getEntityDao( tc() );

        final PFilterBody filterBody = filterDao.getById(filterId);
        if( filterBody != null )
            return filterBody.getCWHEREBLK();
        return null;
    }

}
