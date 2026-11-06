package ru.inversion.fx.report.drjasper.util;

import net.sf.jasperreports.engine.*;

/**
 * <p>Description: </p>
 * Date: 21.05.2021
 * Time: 22:03
 * User:  opl
 */
public interface IDataSourceBuilder {
  void buildDataSource(JRRewindableDataSource dataSource) throws Exception;
}
