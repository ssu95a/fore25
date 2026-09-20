package ru.inversion.fx.report.drjasper.extracter;

import java.util.*;

/**
 * <p>Description: </p>
 * Date: 07.05.2020
 * Time: 14:43
 * User:  opl
 */
public interface IColumnInfoExtractor {

//  ReportColumnInfo extactAllInfo();

  String extractColumnLabel();
  default Optional<String> extractFormatMask() {
    return Optional.empty();
  }
  double extractFieldWidth();


}
