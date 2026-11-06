package ru.inversion.fx.form.controls.filter.impl;

import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.filter.entity.PFilterParameter;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author perov
 */
class ParamWork implements IParamWork {

    private final Connection con;

    public ParamWork(Connection con) {
        this.con = con;
    }

    @Override
    public void doInsert(PFilterParameter pprm) throws SQLException {
        final String strSql = "INSERT INTO V_JF_FRM_FILTER_PARAMETER VALUES (?, ?, ?, ?, ?, ?)";
        if (con != null && pprm != null) {

            try (CallableStatement ps = con.prepareCall(strSql)) {

                int i = 1;
                ps.setLong(i++, pprm.getIDFILTER());
                ps.setLong(i++, pprm.getIPARAMNUM());
                ps.setString(i++, pprm.getCPARAMDESCR());
                ps.setObject(i++, pprm.getCPARAMDEFAULT());
                ps.setObject(i++, pprm.getIDCURSOR());
                ps.setObject(i++, pprm.getBPARAMSAVE() ? "Y" : null);
                ps.execute();

                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    @Override
    public void doUpdate(PFilterParameter pprm) throws SQLException {
        final String strSql = "UPDATE V_JF_FRM_FILTER_PARAMETER\n"
                + "	SET cparamdescr = ?, cparamdefault = ?, idcursor = ?, cparamsave = ? \n"
                + " WHERE idfilter = ? and iparamnum = ?";
        if (con != null && pprm != null) {
            try (CallableStatement ps = con.prepareCall(strSql)) {
                int i = 1;
                //  ps.setLong(1, pprm.getIPARAMNUM());
                ps.setString(i++, pprm.getCPARAMDESCR());
                ps.setObject(i++, pprm.getCPARAMDEFAULT());
                ps.setObject(i++, pprm.getIDCURSOR());
                ps.setObject(i++, pprm.getBPARAMSAVE() ? "Y" : null);
                ps.setLong(i++, pprm.getIDFILTER());
                ps.setLong(i++, pprm.getIPARAMNUM());
                ps.execute();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    @Override
    public void doDelete(PFilterParameter pprm) throws SQLException {
        final String strSql = "DELETE FROM V_JF_FRM_FILTER_PARAMETER a WHERE a.idfilter = ?	and a.iparamnum = ?";
        if (con != null && pprm != null) {
            try (CallableStatement ps = con.prepareCall(strSql)) {

                ps.setLong(1, pprm.getIDFILTER());
                ps.setLong(2, pprm.getIPARAMNUM());
                ps.execute();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    static void updParamValue(Connection con, Long filterId, Long paramNum, String cParamValue) throws SQLException {
        if (con != null && filterId != null && paramNum != null && cParamValue != null) {
            final String strSQL = "UPDATE V_JF_FRM_FILTER_PARAMETER a\n"
                    + "	SET a.cparamdefault = ? \n"
                    + " WHERE a.idfilter = ? \n"
                    + "	and a.iparamnum = ?";
            try (CallableStatement ps = con.prepareCall(strSQL)) {
                
                ps.setString(1, cParamValue);
                ps.setLong(2, filterId);
                ps.setLong(3, paramNum);
                ps.execute();
                con.commit();                
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /**
     * сохранение очередного значения редактируемого параметра
     *
     * @param cParamPref
     * @param cParamValue
     * @throws SQLException
     */
    static void saveParamVal(Connection con, String cParamPref, String cParamValue) throws SQLException {
        if (con != null && cParamPref != null && cParamValue != null) {
            try (CallableStatement cs = con.prepareCall("{ call AP_SERVER.SaveParamVal(?, ?) }")) {

                cs.setString(1, cParamPref);
                cs.setString(2, cParamValue);
                cs.execute();
                con.commit();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    /** */
    static List<String> getListPrefParam( TaskContext tc, String cParamPref ) throws SQLException {

        if( S.isNullOrEmpty(cParamPref) )
            throw new IllegalArgumentException("'cParamPref' is null");

        final String sql = tc.isPostgreSql() ?
                                "select Column_Value from unnest ( PREF.Get_PreferenceTab(?) ) Column_Value"
                                :
                                "SELECT Column_Value FROM TABLE( PREF.Get_PreferenceTab(?) )";

        final List<String> list = new ArrayList<>(10);
        try( PreparedStatement ps = tc.getConnection().prepareStatement(sql) )
        {
            ps.setString( 1, cParamPref );
            
            try( ResultSet rs = ps.executeQuery() ) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
                return list;
            }
        }
    }

    /**
     * Формирование имени для поиска параметра
     *
     * @param filterId - индентификатор фильтра
     * @param paramNum - номер параметра
     * @return имя параметра
     */
    static String getCParamPref(Long filterId, Long paramNum) {

        if( filterId == null || paramNum == null )
            throw new IllegalArgumentException("'filterId' or 'paramNum' is null");

        return "FRM_FLT.BLK_LOAD_PARAM." + filterId + "." + paramNum;
    }

    /** */
    static String getSQLCursor( TaskContext tc, Long cursorId) throws SQLException {

        if( cursorId == null )
            throw new IllegalArgumentException("'cursorId' is null");

        final String sql = tc.isPostgreSql() ?
                "SELECT Cursor_Text str FROM AP_CURSOR_TYPE WHERE Cursor_ID = ?"
                :
                "SELECT DBMS_LOB.SubStr(Cursor_Text) str FROM AP_CURSOR_TYPE WHERE Cursor_ID = ?";

        String result = S.EMPTY_STRING;

        try( PreparedStatement ps = tc.getConnection().prepareStatement(sql))
        {
             ps.setLong( 1, cursorId );
             try ( ResultSet rs = ps.executeQuery() )
             {
                if( rs.next() ) {
                    result = rs.getString(1);
                }
            }
        }
        return result;
    }

    /** */
    static public Validator.Result validateValue(Connection con, String value) {
        if (con != null && value != null){
            try(CallableStatement cs = con.prepareCall("{ ? = call UTIL.CallMacro(SUBSTR(?, 2), 0) }")){
                
                cs.registerOutParameter(1, Types.VARCHAR);
                cs.setString(2, value);
                cs.execute();
            
            } catch (Exception ex) {
                return new Validator.Result(ex.getMessage(), "Ошибка выполнения макроса: \n"+ value);
            }
        }
        return null;
    }

}
