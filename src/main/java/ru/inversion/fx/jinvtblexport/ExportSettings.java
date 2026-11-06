package ru.inversion.fx.jinvtblexport;

import ru.inversion.fx.jinvtblexport.property.ExportFormatEnum;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;

import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.report.drjasper.*;
import ru.inversion.util.*;
import sun.security.action.GetPropertyAction;

/**
 * @author polyatykina
 */
public class ExportSettings implements IDRJasperConst  {

  //Печать футера с количесвом сторок Page N из M
  private boolean ifPrintPageNofM = false;

  public void setIfPrintPageNofM(boolean value) {
    ifPrintPageNofM = value;
  }

  public boolean getIfPrintPageNofM() {
    return ifPrintPageNofM;
  }

  //Печать заголовка данных на каждой новой странице
  private boolean ifHeaderBandPrintOnAllPages = false;

  public void setIfHeaderBandPrintOnAllPages(boolean value) {
    ifHeaderBandPrintOnAllPages = value;
  }

  public boolean getIfHeaderBandPrintOnAllPages() {
    return ifHeaderBandPrintOnAllPages;
  }

  //Открыть после экспорта
  private boolean ifOpenAfterExport = true;

  public void setIfOpenAfterExport(boolean value) {
    ifOpenAfterExport = value;
  }

  public boolean getIfOpenAfterExport() {
    return ifOpenAfterExport;
  }

  // Формат экспорта по-умолчанию
  private static ExportFormatEnum defaultExportFormat = ExportFormatEnum.Excel;

  public void setDefaultExportFormat(ExportFormatEnum value) {
    defaultExportFormat = value;
  }

  public ExportFormatEnum getDefaultExportFormat() {
    return defaultExportFormat;
  }

  public static ExportFormatEnum GetDefaultExportFormat() {
    return defaultExportFormat;
  }

  // Перенос строки
  private static boolean ifWordWrap = false;

  public void setIfWordWrap(boolean value) {
    ifWordWrap = value;
  }

  public boolean getIfWordWrap() {
    return ifWordWrap;
  }

  // Скрывать нулевые
  private static boolean ifHideZero = false;

  public void setIfHideZero(boolean value) {
    ifHideZero = value;
  }

  public boolean getIfHideZero() {
    return ifHideZero;
  }

  // Временная директория для сохранения файлов

  private static final File javaFileTmpDir = new File(AccessController.doPrivileged(new GetPropertyAction("java.io.tmpdir")));

  private static final String javaTmpDir = System.getProperty("java.io.tmpdir");

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("ru.inversion.fx.jinvtblexport.ExportSettings");

  private static String tempDir = null;

  public String getTempDir() {
    return tempDir;
  }

  public void setTempDir(String value) {
    tempDir = value;
  }

  public static String getExpTempDir() {

    if (tempDir == null) {
      tempDir = new File(javaTmpDir, "xxitblexport").getAbsolutePath();
      return tempDir;
    } else
      return tempDir;
  }

  public static String getExportExtension(ExportFormatEnum format) {
    if (format != null) return '.' + format.getFileExtention();
//      switch (format) {
//        case Pdf:
//          return ".pdf";
//        case Xps:
//          return ".xps";
//        case Html:
//          return ".html";
//        case Text:
//          return ".txt";
//        case Rtf:
//          return ".rtf";
//        case Word2007:
//          return ".docx";
//        case Excel:
//          return ".xls";
//        case ExcelXml:
//          return ".xml";
//        case Excel2007:
//          return ".xlsx";
//        case Csv:
//          return ".csv";
//        case Xml:
//          return ".xml";
//        case Sylk:
//          return ".slk";
//        case ImageBmp:
//          return ".bmp";
//        case ImageJpeg:
//          return ".jpg";
//        case ImagePcx:
//          return ".pcx";
//        case ImagePng:
//          return ".png";
//        case ImageSvg:
//          return ".svg";
//        case ImageSvgz:
//          return ".svgz";
//      }

    logger.trace("export format has not found !!");

    return null;
  }

  public static String getDefaultReportName() {
//    return String.format("Report%s", System.currentTimeMillis());
    return "TE_" + RandomGUID.getBase64guid();
  }

  public static String getPathFileExport(ExportFormatEnum format) {
    return getPathFileExport(format, null);
  }

  public static String getPathFileExport(ExportFormatEnum format, String fileDir) {

    logger.trace("GetPathFileExport");

    logger.trace(javaFileTmpDir.getAbsolutePath());
    logger.trace(ExportSettings.getDefaultReportName());
    logger.trace(ExportSettings.getExportExtension(format));

    if (fileDir == null || fileDir.isEmpty()) {
      return new File(new File(javaFileTmpDir, "#XXI_BlockExp"), getDefaultReportName() +  ExportSettings.getExportExtension(format)).getAbsolutePath();
//      return new File(javaFileTmpDir, getDefaultReportName() +  ExportSettings.getExportExtension(format)).getAbsolutePath();
    }
    return new File(fileDir, getDefaultReportName() + ExportSettings.getExportExtension(format)).getAbsolutePath();
  }


  public static File getTempFileExport(ExportFormatEnum format, String prefix) {
    return getTempFileExport(format, prefix, null);
  }

  public static File getTempFileExport(ExportFormatEnum format, String prefix, String tempDir) {
    try {

      String exp = ExportSettings.getExportExtension(format);

      logger.trace(String.format("Extencion = %s", exp));
      //Logger.getLogger("ExportSettings").log(Level.SEVERE, null, ex);

      String dir = ExportSettings.getExpTempDir();

      logger.trace(String.format("Temporary Directory = %s", dir));

      if (!exp.isEmpty()) {
        if (dir == null || dir.isEmpty())
          return (prefix == null || prefix.isEmpty() ? File.createTempFile("report_", exp) : new File(javaFileTmpDir, String.format("%s%s", prefix, exp)));
        else
          return (prefix == null || prefix.isEmpty() ? File.createTempFile("report_", exp, new File(dir)) : new File(new File(dir), String.format("%s%s", prefix, exp)));
      }

    } catch (IOException ex) {
      JInvErrorService.handleException(null, ex);
    }

    return null;
  }

//  public static final String KEY_SOURCE = "JInvTable";
//  public static final String KEY_TITLE = "TitleReport";
//  public static final String KEY_CONTROLLER = "EXP_FORM_CONTROLLER";

}
