
package ru.inversion.fx.form.controls.filter.impl;

import ru.inversion.fx.form.controls.filter.entity.PFilterParameter;

import java.sql.SQLException;

/**
 *
 * @author perov
 */
interface IParamWork {
    void doDelete(PFilterParameter pprm) throws SQLException;

    void doInsert(PFilterParameter pprm) throws SQLException;

    void doUpdate(PFilterParameter pprm) throws SQLException;
}
