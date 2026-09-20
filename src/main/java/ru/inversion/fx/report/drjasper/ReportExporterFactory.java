package ru.inversion.fx.report.drjasper;

import net.sf.dynamicreports.jasper.builder.*;
import net.sf.dynamicreports.jasper.builder.export.*;
import net.sf.dynamicreports.jasper.constant.*;
import net.sf.dynamicreports.report.constant.*;
import org.slf4j.*;
import ru.inversion.util.TU;

import java.io.*;
import java.util.*;

import static net.sf.dynamicreports.report.builder.DynamicReports.*;

/**
 * <p>Description: </p>
 * Date: 29.05.2018
 * Time: 16:20
 * User:  opl
 */
public class ReportExporterFactory implements IReportExporterExtention {

  protected static Logger log = LoggerFactory.getLogger(ReportExporterFactory.class);

  /* ********************* Class Body *****************************/


  public static AbstractJasperExporterBuilder getBuilderInstance(String fileExt,
                                                                 File outFileDir,
                                                                 String outFileName,
                                                                 Optional<PageType> pageSize,
                                                                 JasperReportBuilder jrb) {
    AbstractJasperExporterBuilder reportExporter = null;
    File outFile = new File(outFileDir, outFileName + '.' + fileExt);
//    log.debug(TU.format("AbstractJasperExporterBuilder.OutFilePath= {0}", outFile.getAbsolutePath()));
    // pdf
    return getBuilderInstance(fileExt, outFile, pageSize, jrb);
  }

  public static AbstractJasperExporterBuilder getBuilderInstance(String fileExt,
                                                                 File outFile,
                                                                 Optional<PageType> pageSize,
                                                                 JasperReportBuilder jrb) {
    AbstractJasperExporterBuilder reportExporter = null;
//    File outfile = new File(outFileDir, outFileName + '.' + fileExt);
    log.debug(TU.format("AbstractJasperExporterBuilder.OutFilePath= {0}", outFile.getAbsolutePath()));
    // pdf
    reportExporter = export.pdfExporter(outFile);
    Properties repProp = new Properties();
    if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_XLS)) {
      // xls
      // Не используется
      reportExporter = export.xlsExporter(outFile)
        .setDetectCellType(true)
//        .setIgnorePageMargins(true)
        .setIgnorePageMargins(false)
        .setWhitePageBackground(false)
        .setFontSizeFixEnabled(false)
        .setRemoveEmptySpaceBetweenColumns(true)
        .setRemoveEmptySpaceBetweenRows(true)
        .setWrapText(true)
        .setFontSizeFixEnabled(true)
      ;
      if (true || !pageSize.isPresent()) jrb.ignorePagination();
//      jrb.setParameter("net.sf.jasperreports.print.keep.full.text", "true");
    } else if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_XLSX)) {
      // xlsx
      log.debug("XLSX Exptorter ....");
      reportExporter = export.xlsxExporter(outFile)
        .setDetectCellType(true)
//        .setIgnorePageMargins(true) !!
        .setIgnorePageMargins(false)
        .setWhitePageBackground(false)
//        .setFontSizeFixEnabled(false)
        .setRemoveEmptySpaceBetweenColumns(true)
        .setRemoveEmptySpaceBetweenRows(true)
//        .setWrapText(true)
        .setFontSizeFixEnabled(true)
//        .setIgnoreCellBorder(false)
      ;
//      if (true || !pageSize.isPresent()) jrb.ignorePagination();
      jrb.ignorePagination();

      repProp.setProperty("net.sf.jasperreports.print.keep.full.text", "true");
//      repProp.setProperty( "net.sf.jasperreports.export.xls.shrink.to.fit", "true" );

//      repProp.setProperty("net.sf.jasperreports.export.xls.auto.fit.row", "true"); // Для отдельной колонки

    } else if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_DOCX)) {
      // rtf
      reportExporter = export.docxExporter(outFile);
    } else if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_RTF)) {
      // rtf
      reportExporter = export.rtfExporter(outFile);
    } else if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_HTML)) {
      // html
      reportExporter = export.htmlExporter(outFile);
      jrb.ignorePagination();
//      repProp.setProperty(" net.sf.jasperreports.virtual.page.element.size", "10000");
    } else if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_JPEG)) {
      // jpeg
      reportExporter = export.imageExporter(outFile, ImageType.JPG);
//      reportExporter = export.imageExporter(outFile, ImageType.PNG);
      jrb.ignorePagination();
    } else if (fileExt != null && fileExt.toLowerCase().equals(C_EXT_TXT)) {
      //txt
      reportExporter = export.textExporter(outFile);
//            .setDetectCellType(true)
//            .setIgnorePageMargins(true)
//            .setWhitePageBackground(false)
//            .setRemoveEmptySpaceBetweenColumns(true);
    }
    // All Export Types
//    20 pages in Swap Virtualizer
    repProp.setProperty(" net.sf.jasperreports.virtual.page.element.size", "2000"); //++
//    repProp.setProperty(" net.sf.jasperreports.virtual.page.element.size", "1000");
    jrb.setProperties(repProp);
    return reportExporter;
  }
  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/


}
