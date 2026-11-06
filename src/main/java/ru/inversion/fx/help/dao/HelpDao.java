package ru.inversion.fx.help.dao;

import ru.inversion.fx.help.entity.PHelp;

import java.sql.Connection;
import java.sql.SQLException;

/**
 *
 * @author perov
 */
public interface HelpDao {
    void insertHelpForm(Connection con, PHelp entity) throws SQLException;
    void updateHelpForm(Connection con, PHelp entity, String oldFormValue) throws SQLException;
    void deleteHelpForm(Connection con, PHelp entity) throws SQLException;
    
}
