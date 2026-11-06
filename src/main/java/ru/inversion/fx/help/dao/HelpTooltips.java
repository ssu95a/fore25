package ru.inversion.fx.help.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.help.entity.PHelpBundle;

/**
 *
 * @author perov
 */
public class HelpTooltips {

    public static String getControlHelpText(Connection con, String formName, String controlName) {

        if (con != null) {
            final String sqlStr = "SELECT form, cntr_name, html_text FROM JF_HELP_BUNDLE where form=? and cntr_name=?";

            try (PreparedStatement pstmt = con.prepareStatement(sqlStr)) {

                pstmt.setString(1, formName);
                pstmt.setString(2, controlName);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("html_text");
                    }
                }

            } catch (Throwable ex) {

                JInvErrorService.handleException(null, ex);
            }
        }

        return null;
    }

    public static PHelpBundle getControlTooltip(Connection con, String formName, String controlName) {

        if (con != null) {
            final String sqlStr = "SELECT form, cntr_name, html_text FROM JF_HELP_BUNDLE where form=? and cntr_name=?";

            try (PreparedStatement pstmt = con.prepareStatement(sqlStr)) {

                pstmt.setString(1, formName);
                pstmt.setString(2, controlName);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        PHelpBundle pojo = new PHelpBundle();
                        pojo.setFORM(rs.getString("form"));
                        pojo.setCNTR_NAME(rs.getString("cntr_name"));
                        pojo.setHTML_TEXT(rs.getString("html_text"));
                        return pojo;
                    }
                }

            } catch (Throwable ex) {

                JInvErrorService.handleException(null, ex);
            }
        }

        return null;
    }

    public static List<PHelpBundle> getListControls(Connection con, String formName)
    {
        if (con != null) {
            final String sqlStr = "SELECT form, cntr_name, html_text FROM JF_HELP_BUNDLE where form=?";

            try (PreparedStatement pstmt = con.prepareStatement(sqlStr)) {

                pstmt.setString(1, formName);

                try (ResultSet rs = pstmt.executeQuery()) {

                    List<PHelpBundle> list = new LinkedList<>();

                    while (rs.next()) {
                        PHelpBundle pojo = new PHelpBundle();
                        pojo.setFORM(rs.getString("form"));
                        pojo.setCNTR_NAME(rs.getString("cntr_name"));
                        pojo.setHTML_TEXT(rs.getString("html_text"));
                        list.add(pojo);
                    }
                    return list;
                }

            } catch (Throwable ex) {

                JInvErrorService.handleException(null, ex);
            }
        }

        return null;
    }

    public void commit(Connection con) throws SQLException
    {
        if (con!=null) {
            con.commit();
        }
    }

    public void rollback(Connection con) throws SQLException
    {
        if (con!=null) {
            con.rollback();
        }
    }
}
