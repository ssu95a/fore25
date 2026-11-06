package ru.inversion.fx.report.drjasper;

import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.Date;

/**
 * <p>Description: </p>
 * Date: 02.03.2021
 * Time: 10:46
 * User:  opl
 */
public interface IDRJasperConst {
  String KEY_SOURCE = "JInvTable";
  String KEY_CONTROLLER = "EXP_FORM_CONTROLLER";
  String KEY_TITLE = "TitleReport";

  int c_longStringPrefLengh = 45;
  int c_minStringPrefLength = 12;

  int c_maxHeaderLength = 15;

  LocalDate localDateExample = LocalDate.now();
  LocalDateTime localDateTimeExample = LocalDateTime.now();
  Timestamp timeStampExample = Timestamp.valueOf(localDateTimeExample);
  Date dateExample = new Date();
  java.sql.Date sqlDateExample = java.sql.Date.valueOf(LocalDate.now());
  Boolean booleanExample = new Boolean(true);
  String stringExample = new String();
  Long longExample = new Long(1);
  Integer integerExample = new Integer(1);
  BigDecimal bigDecimalExample = new BigDecimal(1);



}
