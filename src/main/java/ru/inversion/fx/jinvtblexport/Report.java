/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 * Полезная документация
 * https://www.stimulsoft.com/ru/documentation/online/user-manual/index.html?report_internals_autosize_automatically_resizing_text.htm
 */
package ru.inversion.fx.jinvtblexport;

import com.stimulsoft.base.StiEnumSet;
import com.stimulsoft.base.drawing.StiBorder;
import com.stimulsoft.base.drawing.StiColor;
import com.stimulsoft.base.drawing.StiColorEnum;
import com.stimulsoft.base.drawing.StiSolidBrush;
import com.stimulsoft.base.drawing.enums.StiBorderSides;
import com.stimulsoft.base.drawing.enums.StiPenStyle;
import com.stimulsoft.base.drawing.enums.StiTextHorAlignment;
import com.stimulsoft.base.drawing.enums.StiVertAlignment;
import com.stimulsoft.base.enums.StiPaperKind;
import com.stimulsoft.base.exception.StiException;
import com.stimulsoft.base.exception.StiExceptionProvider;
import com.stimulsoft.base.localization.StiLocalization;
import com.stimulsoft.base.serializing.StiDeserializationException;
import com.stimulsoft.base.system.*;
import com.stimulsoft.base.system.geometry.StiRectangle;
import com.stimulsoft.base.system.type.StiSystemType;
import com.stimulsoft.report.StiExportManager;
import com.stimulsoft.report.StiNameCreation;
import com.stimulsoft.report.StiReport;
import com.stimulsoft.report.StiSerializeManager;
import com.stimulsoft.report.components.StiComponent;
import com.stimulsoft.report.components.StiMargins;
import com.stimulsoft.report.components.StiPage;
import com.stimulsoft.report.components.bands.StiDataBand;
import com.stimulsoft.report.components.bands.StiFooterBand;
import com.stimulsoft.report.components.bands.StiHeaderBand;
import com.stimulsoft.report.components.bands.StiPageFooterBand;
import com.stimulsoft.report.components.complexcomponents.StiContainer;
import com.stimulsoft.report.components.enums.StiDockStyle;
import com.stimulsoft.report.components.enums.StiPageOrientation;
import com.stimulsoft.report.components.enums.StiPrintOnType;
import com.stimulsoft.report.components.enums.StiShiftMode;
import com.stimulsoft.report.components.simplecomponents.StiHorizontalLinePrimitive;
import com.stimulsoft.report.components.simplecomponents.StiText;
import com.stimulsoft.report.components.textFormats.StiBooleanFormatService;
import com.stimulsoft.report.components.textFormats.StiCustomFormatService;
import com.stimulsoft.report.components.textFormats.StiDateFormatService;
import com.stimulsoft.report.components.textFormats.StiNumberFormatService;
import com.stimulsoft.report.dictionary.StiDataColumn;
import com.stimulsoft.report.dictionary.StiDataColumnsCollection;
import com.stimulsoft.report.dictionary.StiDictionary;
import com.stimulsoft.report.dictionary.StiVariable;
import com.stimulsoft.report.dictionary.dataSources.StiDataSource;
import com.stimulsoft.report.dictionary.dataSources.StiDataTableSource;
import com.stimulsoft.report.dictionary.databases.StiDatabaseCollection;
import com.stimulsoft.report.dictionary.databases.StiJDBCDatabase;
import com.stimulsoft.report.enums.StiReportUnitType;
import com.stimulsoft.report.export.settings.StiCsvExportSettings;
import com.stimulsoft.report.export.settings.StiExcel2007ExportSettings;
import com.stimulsoft.report.export.settings.StiPdfExportSettings;
import com.stimulsoft.report.export.settings.StiTxtExportSettings;
import com.stimulsoft.report.export.tools.StiColorImageFormat;
import com.stimulsoft.report.export.tools.StiMonochromeDitheringType;
import com.stimulsoft.report.export.tools.StiTxtBorderType;
import com.stimulsoft.report.export.tools.pdf.StiPdfImageCompressionMethod;
import com.stimulsoft.report.expressions.StiTagExpression;
import com.stimulsoft.report.print.StiPrintHelper;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.jinvtblexport.property.*;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.S;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.*;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

//import ru.inversion.ds.reflection.JInvReflectionUtils;

/**
 * @author polyatykina
 */
public class Report {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("ru.inversion.fx.jinvstimul.Report");
  // Фонты для специальных отчетов
  private static StiFont captionFont = new StiFont("Courier New", 10F, StiFontStyle.Bold);
  private static StiFont titleFont = new StiFont("Courier New", 8F, StiFontStyle.Regular);
  private static StiFont valuesFont = new StiFont("Courier New", 8F, StiFontStyle.Bold);
  private static StiFont titleCaptionFont = new StiFont("Times New Roman", 7F, StiFontStyle.Bold);
  private static ExportSettings exportSettings = new ExportSettings();
  /*
   * PROPERTIES
   */
  //Свойства отчета
  private JInvTable sourceTable;
  private StiReport report;
  private IDataSet dataSet;
  private Class rowClass;
  private List<Object> listRows = new ArrayList<>();
  private double pageWidth = 0.0;
  private double widthDeltaCoeff = 0.0;
  private Ref<Double> headerWidth = new Ref<>(0.0); // полная ширина колонок грида, пересчитывается при фитировании ширины, и используется
  private Ref<ArrayList> rootBands = new Ref<>(new ArrayList());
  private Ref<ArrayList> rootColumns = new Ref<>(new ArrayList());
  private StiColor bandColor = new StiColor(204, 204, 204);
  private StiFont bandFont = new StiFont("Courier New", 8F);
  private StiColor bandForeColor = StiColor.Black;
  // Brush, border and margins
  private StiSolidBrush bandBrush = new StiSolidBrush(StiColorEnum.Control);
  private StiBorder border = new StiBorder(StiBorderSides.All, StiColor.Black, 1, StiPenStyle.Solid, false, 1, new StiSolidBrush(StiColor.Black));
  private StiBorder borderTop = new StiBorder(new StiEnumSet(StiBorderSides.Top, StiBorderSides.Left, StiBorderSides.Right)
    , StiColor.Black, 1, StiPenStyle.Solid, false, 1, new StiSolidBrush(StiColor.Black));
  private StiBorder borderBottom = new StiBorder(new StiEnumSet(StiBorderSides.Bottom, StiBorderSides.Left, StiBorderSides.Right)
    , StiColor.Black, 1, StiPenStyle.Solid, false, 1, new StiSolidBrush(StiColor.Black));
  private StiBorder borderMiddle = new StiBorder(new StiEnumSet(StiBorderSides.Left, StiBorderSides.Right)
    , StiColor.Black, 1, StiPenStyle.Solid, false, 1, new StiSolidBrush(StiColor.Black));
  private StiMargins margins = new StiMargins(2, 2, 2, 2); // границы для ячейки
  //Свойства по умолчанию для getCellControl
  private double defaultCellHeight = 0.4;
  private StiMargins defaultMargins = new StiMargins(2, 2, 2, 2);
  private StiBorder defaultBorder = new StiBorder(StiBorderSides.All, StiColor.Black, 1, StiPenStyle.Solid);
  private StiBorder cellBorder = new StiBorder(new StiEnumSet(StiBorderSides.Right, StiBorderSides.Left), StiColor.Black, 1, StiPenStyle.Solid);
  private StiFont defaultFont = new StiFont("Courier New", 8F);
  //Пересчет ширин/высот
  private double widthRatio = 80 / 2.54;
  private double heightRatio = 80 / 2.54;
  private double cellHeight = 0;
  private double widthCoeff = 1.26;
  private SizeExportedDataEnum sizeExportedData;

  public static ExportSettings getExportSettings() {
    return exportSettings;
  }

  public static void setExportSettings(ExportSettings value) {
    exportSettings = value;
  }

  public static StiReport getStReport(JInvTable source) {
    Report report = new Report();
    return report.getReport(source);
  }

  public static String getFileOutput(ExportFormatEnum format, String tempDir, String fileName) {
    String exp = "";

    switch (format) {
      case Pdf:
        exp = "pdf";
        break;
      case Xps:
        exp = "xps";
        break;
      case Html:
        exp = "html";
        break;
      case Text:
        exp = "txt";
        break;
      case Rtf:
        exp = "rtf";
        break;
      case Word2007:
        exp = "docx";
        break;
      case Excel:
        exp = "xls";
        break;
      case ExcelXml:
        exp = "xml";
        break;
      case Excel2007:
        exp = "xlsx";
        break;
      case Csv:
        exp = "csv";
        break;
      case Xml:
        exp = "xml";
        break;
      case Sylk:
        exp = "slk";
        break;
      case ImageBmp:
        exp = "bmp";
        break;
      case ImageJpeg:
        exp = "jpeg";
        break;
      case ImagePcx:
        exp = "pcx";
        break;
      case ImagePng:
        exp = "png";
        break;
      case ImageSvg:
        exp = "svg";
        break;
      case ImageSvgz:
        exp = "svgz";
        break;

    }

    if (tempDir.endsWith("\\"))
      return String.format("%s%s.%s", tempDir, fileName, exp);

    return String.format("%s\\%s.%s", tempDir, fileName, exp);
  }

  public static String export(StiReport report,
                              ExportFormatEnum format,
                              String TempDir,
                              String File) throws StiException, IOException {
    if (TempDir == null || TempDir.isEmpty())
      TempDir = ExportSettings.getExpTempDir();


    if (logger.isTraceEnabled()) logger.trace(String.format("TempDir = %s", TempDir));
    if (logger.isTraceEnabled())
      logger.trace(String.format("File = %s", ((File == null || File.isEmpty()) ? "null" : File)));

    if (format == ExportFormatEnum.Mdc)
      return Report.exportMDC(report, TempDir);

    FileOutputStream outputStream = null;

    String fileReport = ((File == null || File.isEmpty()) ? getFileOutput(format, TempDir, report.getReportName()) : File);
    try {

      outputStream = new FileOutputStream(fileReport);

      switch (format) {
        case Pdf:
          StiPdfExportSettings pdfexpst = new StiPdfExportSettings();
          pdfexpst.setExportRtfTextAsImage(false);
          pdfexpst.setImageFormat(StiColorImageFormat.Monochrome);
          pdfexpst.setDitheringType(StiMonochromeDitheringType.FloydSteinberg);
          pdfexpst.setImageCompressionMethod(StiPdfImageCompressionMethod.Flate);

          StiExportManager.exportPdf(report, pdfexpst, outputStream);
          break;
        case Xps:
          StiExportManager.exportXps(report, outputStream);
          break;
        case Html:
          StiExportManager.exportHtml(report, outputStream);
          break;
        case Text:
          StiTxtExportSettings txtexpst = new StiTxtExportSettings();
          txtexpst.setEncoding(Charset.forName("windows-1251"));
          txtexpst.setBorderType(StiTxtBorderType.Simple);
          StiExportManager.exportText(report, txtexpst, outputStream);
          break;
        case Rtf:
          StiExportManager.exportRtf(report, outputStream);
          break;
        case Word2007:
          StiExportManager.exportWord2007(report, outputStream);
          break;
        case Excel:
          StiExportManager.exportExcel(report, outputStream);
          break;
        case ExcelXml:
          StiExportManager.exportExcelXml(report, outputStream);
          break;
        case Excel2007:
          if (Locale.getDefault().getCountry().isEmpty()) {
            Locale.setDefault(new Locale("ru", "RU"));
          }
          StiExcel2007ExportSettings sett = new StiExcel2007ExportSettings();
          sett.setExportPageBreaks(false);
          sett.setOpenAfterExport(false);
          StiExportManager.exportExcel2007(report, sett, outputStream);
          break;
        case Csv:
          StiCsvExportSettings csvexprt = new StiCsvExportSettings();
          csvexprt.setSkipColumnHeaders(true);
          csvexprt.setSeparator(";");
          csvexprt.setEncoding(Charset.forName("windows-1251"));
          StiExportManager.exportCsv(report, csvexprt, outputStream);
          break;
        case Xml:
          StiExportManager.exportXml(report, outputStream);
          break;
        case Sylk:
          StiExportManager.exportSylk(report, outputStream);
          break;
        case ImageBmp:
          StiExportManager.exportImageBmp(report, outputStream);
          break;
        case ImageJpeg:
          StiExportManager.exportImageJpeg(report, outputStream);
          break;
        case ImagePcx:
          StiExportManager.exportImagePcx(report, outputStream);
          break;
        case ImagePng:
          StiExportManager.exportImagePng(report, outputStream);
          break;
        case ImageSvg:
          StiExportManager.exportImageSvg(report, outputStream);
          break;
        case ImageSvgz:
          StiExportManager.exportImageSvgz(report, outputStream);
          break;

      }

    } catch (FileNotFoundException | StiException ex) {
      JInvErrorService.handleException(null, ex);
    } finally {
      if (outputStream != null) {
        try {

          outputStream.close();

          if (exportSettings.getIfOpenAfterExport()) {
            StiFileExecuter.openByExtension(fileReport);
          }

        } catch (IOException ex) {
          JInvErrorService.handleException(null, ex);
        }
      }
    }

    return fileReport;
  }

  public static String export(JInvTable source, ExportFormatEnum format, String tempDir, String file, SizeExportedDataEnum sed) {
//        try {
//            //        try {
////            exportMRT(source, "D:\\");
////        } catch (IOException ex) {
////            ex.printStackTrace();
////        }
//            exportMDC(source, "D:\\");
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
    Report report = new Report();
    StiReport rep = report.getReport(source, sed);

    if (rep != null)

      try {
        return report.export(rep, format, tempDir, file);
      } catch (StiException | IOException ex) {
        JInvErrorService.handleException(null, (Throwable) ex);
      }

    return null;
  }

  public static String exportMRT(JInvTable source, String tempDir) throws IOException {
    Report report = new Report();
    StiReport rep = report.getReport(source);
    String fileReport = null;

    if (rep != null) {
      fileReport = String.format("%s\\%s.mrt", tempDir, rep.getReportName());

      FileOutputStream outputStream = null;

      try {
        outputStream = new FileOutputStream(fileReport);

        StiSerializeManager.serializeReport(rep, outputStream);

      } catch (FileNotFoundException ex) {
        JInvErrorService.handleException(null, ex);
      } finally {
        if (outputStream != null) {
          try {

            outputStream.close();

          } catch (IOException ex) {
            JInvErrorService.handleException(null, ex);
          }
        }
      }
    }

    return fileReport;
  }

  private static String exportMDC(StiReport rep, String tempDir) throws IOException {
    String fileReport = null;

    if (rep != null) {
      fileReport = String.format("%s\\%s.mdc", tempDir, rep.getReportName());

      FileOutputStream outputStream = null;

      try {
        outputStream = new FileOutputStream(fileReport);

        StiSerializeManager.serializeDocument(rep, outputStream);

      } catch (FileNotFoundException ex) {
        JInvErrorService.handleException(null, ex);

      } finally {
        if (outputStream != null) {
          try {

            outputStream.close();

          } catch (IOException ex) {
            JInvErrorService.handleException(null, ex);
          }
        }
      }
    }
    return fileReport;
  }

  public static String exportMDC(JInvTable source, String tempDir) throws IOException {
    Report report = new Report();
    StiReport rep = report.getReport(source);
    String fileReport = null;

    if (rep != null) {
      fileReport = String.format("%s\\%s.mdc", tempDir, rep.getReportName());

      FileOutputStream outputStream = null;

      try {
        outputStream = new FileOutputStream(fileReport);

        StiSerializeManager.serializeDocument(rep, outputStream);

      } catch (FileNotFoundException ex) {
        JInvErrorService.handleException(null, ex);

      } finally {
        if (outputStream != null) {
          try {

            outputStream.close();

          } catch (IOException ex) {
            JInvErrorService.handleException(null, ex);
          }
        }
      }
    }
    return fileReport;
  }

  public static void print(StiReport report) {
    Report.print(report, 1);
  }

  public static void print(StiReport report, int copies) {
    PrinterJob printerJob = StiPrintHelper.preparePrinterJob(report.getRenderedPages());
    printerJob.setCopies(copies);
    try {
      StiPrintHelper.printJob(printerJob, report, true);
    } catch (PrinterException ex) {
      JInvErrorService.handleException(null, ex);
    }
  }

  public static void print(JInvTable source, int copies) {
    Report.print(Report.getStReport(source), copies);
  }

  public static void print(JInvTable source) {
    Report.print(Report.getStReport(source));
  }

  private static StiJDBCDatabase getOracleDatabase(String Host, String Port, String Sid, String UserName, String Password) {
        /*
jdbc.driver=oracle.jdbc.driver.OracleDriver
jdbc.url=jdbc:oracle:thin:@[HOST][:PORT]:SID;
jdbc.username={myUserName };
jdbc.password={ myUserPassword };
         */
    String jdbcDriver = "oracle.jdbc.driver.OracleDriver";
    String jdbcUrl = String.format("jdbc:oracle:thin:@%s%s%s"
      , (Host == null || Host.isEmpty() ? "" : Host)
      , (Port == null || Port.isEmpty() ? "" : String.format(":%s", Port))
      , (Sid == null || Sid.isEmpty() ? "" : String.format(":%s", Sid))
    );

    return new StiJDBCDatabase("OracleConnection", "OracleConnection", jdbcUrl, jdbcDriver, UserName, Password);
  }

  public static StiReport openReport(ReportProperties variable) {

    if (variable == null || variable.size() == 0) return null;

    String FileProperties = variable.getProperty(ReportProperties.KEY_FILE_PROPERTIES);

    StiReport report = null;

    try {
      report = StiSerializeManager.deserializeReport(new File(FileProperties));
    } catch (IOException | StiDeserializationException | SAXException ex) {
      JInvErrorService.handleException(null, ex);
    }

    if (report == null)
      return null;

    String UserProperties = variable.getProperty(ReportProperties.KEY_USER_PROPERTIES, "fund_dev");
    String PswProperties = variable.getProperty(ReportProperties.KEY_PSW_PROPERTIES, "fund_dev");
    String HostProperties = variable.getProperty(ReportProperties.KEY_HOST_PROPERTIES, "192.168.0.3");
    String PortProperties = variable.getProperty(ReportProperties.KEY_PORT_PROPERTIES, "1521");
    String SidProperties = variable.getProperty(ReportProperties.KEY_SID_PROPERTIES, "DEV8I");

    StiDatabaseCollection databaseCollection = new StiDatabaseCollection();

    databaseCollection.add(getOracleDatabase(HostProperties, PortProperties, SidProperties, UserProperties, PswProperties));

    report.getDictionary().getDatabases().clear();
    report.getDictionary().getDatabases().addAll(databaseCollection);

    if (report.getDictionary().getVariables() != null && report.getDictionary().getVariables().size() > 0) {
      for (StiVariable var : report.getDictionary().getVariables()) {

        if (!variable.containsKey(var.getName())) continue;

        var.setValue(variable.getProperty(var.getName()));
      }
    }

    report.render();

    return report;
  }

  public static String executeReport(ReportProperties variable, String tempDir) throws IOException {
    StiReport report = openReport(variable);

    String result = null;

    if (report == null)
      return null;

    ReportModeEnum rm = (ReportModeEnum) variable.get(ReportProperties.KEY_REPORT_MODE_PROPERTIES);

    if (rm == null)
      rm = ReportModeEnum.EXPORT;

    try {
      switch (rm) {
        case EXPORT: {
          ExportFormatEnum ef = ExportFormatEnum.valueOf(variable.get(ReportProperties.KEY_EXPORT_FORMAT_PROPERTIES).toString());

          if (ef == null)
            ef = ExportSettings.GetDefaultExportFormat();

          result = Report.export(report, ef, tempDir, null);

          break;
        }
        case VIEW: {

          result = Report.export(report, ExportFormatEnum.Html, tempDir, null);

          if (!exportSettings.getIfOpenAfterExport()) {
            StiFileExecuter.openByExtension(result);
          }

          break;
        }
        case PRINT: {
          Report.print(report);
          break;
        }
      }
    } catch (StiException | IOException ex) {
      JInvErrorService.handleException(null, ex);
    }

    return result;
  }

  // получить информацию с таблицы
  private void getTableInfo() {
    DSFXAdapter adapter = Controls.getDsAdapterFromControl(sourceTable);

    if (adapter == null)
      dataSet = null;
    else
      dataSet = (IDataSet) adapter.getDataSet();

    if (dataSet == null) {
      listRows = sourceTable.getItems();
      rowClass = listRows.getClass();
    } else {
      rowClass = dataSet.getRowClass();
      listRows = dataSet.getRows();
    }
  }

  private StiSystemType getStiSystemType(IEntityProperty pd) {
    if (pd == null || pd.getType().equals(System.class)) {
      return StiSystemType.getSystemType("System.String");
    } else if (pd.getType().equals(Boolean.class)) {
      return StiSystemType.getSystemType("System.Boolean");
    } else if (pd.getType().equals(Double.class)) {
      return StiSystemType.getSystemType("System.Double");
    } else if (pd.getType().equals(BigDecimal.class)) {
      return StiSystemType.getSystemType("System.BigDecimal");
    }
    //else if ( pd.getType().equals(LocalDate.class) )
    //    { return StiSystemType.getSystemType("System.LocalDate"); }
    else if (pd.getType().equals(Date.class) || pd.getType().equals(LocalDate.class)
      || pd.getType().equals(StiDateTime.class) || pd.getType().equals(LocalDateTime.class)) {
      return StiSystemType.getSystemType("System.DateTime");
    } else if (pd.getType().equals(Timestamp.class)) {
      return StiSystemType.getSystemType("System.TimeSpan");
    } else if (pd.getType().equals(Byte.class)) {
      return StiSystemType.getSystemType("System.Byte");
    } else if (pd.getType().equals(Integer.class)) {
      return StiSystemType.getSystemType("System.Int32");
    } else if (pd.getType().equals(Long.class)) {
      return StiSystemType.getSystemType("System.Long");
    }
    //{ return StiSystemType.getSystemType("System.Int64"); }
    else if (pd.getType().equals(Float.class)) {
      return StiSystemType.getSystemType("System.Float");
    } else {
      return StiSystemType.getSystemType("System.Object");
    }
  }

  private String getDefaultReportName() {
    long currentTimeMillis = System.currentTimeMillis();

    //f ( this.sourceTable.getId() == null || this.sourceTable.getId().isEmpty() )
    return String.format("Report%s", currentTimeMillis);
    //else
    //    return String.format("Report%s%s", this.sourceTable.getId(), currentTimeMillis);

  }

  protected File getLocalizationDir() {
    try {
      return new File(Report.class.getClassLoader().getResource("/ru/inversion/fx/jinvstimul/localization").toURI());
      //return new File( Report.class.getClassLoader().getResource("localization").toURI() );

    } catch (URISyntaxException ex) {
      ex.printStackTrace();
    }
    return new File("localization");
  }

  public void setLocalization(String fileName) {

    if (logger.isTraceEnabled()) {
      logger.trace("setLocalization --> ");
    }

    try {

      File file = new File(getLocalizationDir(), fileName);

      if (logger.isTraceEnabled()) logger.trace(file.getAbsoluteFile().toString());

      StiLocalization localization = StiLocalization.load(new BufferedInputStream(
        new FileInputStream(file)));
      StiLocalization.setLocalization(localization);
    } catch (Exception e) {
      logger.error("Error load localization", e);
    }

    if (logger.isTraceEnabled()) logger.trace("<-- setLocalization");

  }

  public StiReport getReport(JInvTable source) {
    return getReport(source, SizeExportedDataEnum.FULL_DATA);
  }

  public StiReport getReport(JInvTable source, SizeExportedDataEnum sed) {
    sourceTable = source;
    sizeExportedData = sed;

    getTableInfo();

    report = StiReport.newInstance();

    //setLocalization("en.xml");

    report.setReportUnit(StiReportUnitType.Centimeters);
    report.setNeedsCompiling(true);
    report.setReportAlias("Report");
    report.setReportName(getDefaultReportName());

    StiPage page;

    if (report.getPages() != null && report.getPages().size() > 0)
      page = report.getPages().get(0);
    else {
      page = new StiPage(report);
      report.getPages().add(page);
    }

    page.setName(StiNameCreation.createName(report, StiNameCreation.generateName(page)));

    if (source.getVisibleLeafColumns().size() > 9)
      page.setOrientation(StiPageOrientation.Landscape);

    else
      page.setOrientation(StiPageOrientation.Portrait);

    page.setMargins(new StiMargins(1, 1, 2, 1));
    page.setPaperSize(StiPaperKind.A4);
    double pw = page.getWidth();
    page.setPaperSize(StiPaperKind.Custom);
    page.setPageWidth(pw * 10.0);

    pageWidth = page.getWidth();

    // Band&Column
    switch (sed) {
      case DISPLAYED_TABLE:
        getBandHeaderInfo(false);
        break;
      case FULL_TABLE:
        getBandHeaderInfo(true);
        break;
      case FULL_DATA:
        //page.setPaperSize(StiPaperKind.A2);

        //pageWidth = page.getWidth();
        getBandHeaderInfo(true);
    }

    //DataSource components

    report.setDictionary(new StiDictionary(report));
    report.getDictionary().getDatabases().add(new GridDatabase_Iterator(this.sourceTable, sed));

    List<StiDataTableSource> tableSources = new ArrayList<>();

    // parent
    StiDataTableSource tSource = new StiDataTableSource("grid.data", "data", "data");
    tSource.setColumns(new StiDataColumnsCollection());

    ReportColumnInfo colInfo = null;

    if (logger.isTraceEnabled()) logger.trace("ADD COLUMN IN DATATABLE:");
    if (logger.isTraceEnabled()) logger.trace("rootColumns.size = " + rootColumns.get().size());

    for (Iterator it = rootColumns.get().iterator(); it.hasNext(); ) {
      colInfo = (ReportColumnInfo) it.next();

      if (S.isNullOrEmpty(colInfo.fieldName.get())) {
        continue;
      }

      final StiSystemType stiSystemType = getStiSystemType(EntityMetadataFactory.getEntityMetaData(this.rowClass).getProperty(colInfo.fieldName.get()));
      if (logger.isTraceEnabled()) {
        logger.trace("Add: " + colInfo.fieldName.get() + ", " + colInfo.fieldName.get() + ", " + stiSystemType.toString());
        logger.trace(EntityMetadataFactory.getEntityMetaData(this.rowClass).getProperty(colInfo.fieldName.get()).getType().toString());
      }
      tSource.getColumns().add(new StiDataColumn(colInfo.fieldName.get(), colInfo.fieldName.get(), stiSystemType));
      //new StiDataColumn( colInfo.fieldName.get(), colInfo.fieldName.get(),  getStiSystemType(JInvReflectionUtils.getPropertyDescriptor( this.rowClass, colInfo.fieldName.get()))));

    }

    tSource.setDictionary(report.getDictionary());
    report.getDictionary().getDataSources().add(tSource);
    tableSources.add(tSource);

    if (tSource.getColumns() == null || tSource.getColumns().size() == 0)
      report.getDictionary().getVariables().add("DataCount", 0);
    else
      report.getDictionary().getVariables().add("DataCount", tSource.getColumns().size());

    // HeaderBand
    StiHeaderBand reportHeaderBand = new StiHeaderBand(new StiRectangle(0, 0, headerWidth.get(), cellHeight)); //pageWidth, cellHeight) );
    reportHeaderBand.setName("reportHeaderBand");
    reportHeaderBand.setPrintIfEmpty(true);
    reportHeaderBand.setCanShrink(true);
    reportHeaderBand.setCanGrow(true);
    reportHeaderBand.setGrowToHeight(true);
    reportHeaderBand.setPrintOnAllPages(exportSettings.getIfHeaderBandPrintOnAllPages());//печатать на всех страницах

    page.getComponents().add(reportHeaderBand);

    addHeaderBandTexts(reportHeaderBand, rootBands.get(), 0);

    for (StiComponent comp : reportHeaderBand.getComponents()) {
      if (comp.getRight() > reportHeaderBand.getRight())
        reportHeaderBand.setWidth(comp.getRight() - reportHeaderBand.getLeft());
    }

    reportHeaderBand.setHeight(cellHeight + 0.01);

    StiHeaderBand reportHeaderBand2 = new StiHeaderBand(new StiRectangle(0, 2 * cellHeight, headerWidth.get(), 0.03)); //pageWidth, 0.03));
    reportHeaderBand2.setName("reportHeader2");
    reportHeaderBand2.setPrintIfEmpty(true);
    reportHeaderBand2.setCanShrink(true);
    reportHeaderBand2.setCanGrow(false);
    reportHeaderBand2.setGrowToHeight(false);
    reportHeaderBand2.setPrintOnAllPages(exportSettings.getIfHeaderBandPrintOnAllPages());//печатать на всех страницах
    reportHeaderBand2.setDockStyle(StiDockStyle.Top);

    report.getPages().get(0).getComponents().add(reportHeaderBand2);

    StiHorizontalLinePrimitive lineHeader = new StiHorizontalLinePrimitive(new StiRectangle(0, 0, headerWidth.get(), 0.03)); //pageWidth,0.03));

    reportHeaderBand2.getComponents().add(lineHeader);

    lineHeader.setName("lineHeader");
    lineHeader.setColor(StiColor.Black);


    //DataBand

    if (cellHeight < defaultCellHeight)
      cellHeight = defaultCellHeight;

    StiDataBand reportDataBand = new StiDataBand(new StiRectangle(0, cellHeight * 4, headerWidth.get(), cellHeight)); //pageWidth, cellHeight));
    reportDataBand.setDataSourceName(tSource.getName());
    reportDataBand.setName("reportDataBand");
    reportDataBand.setCanGrow(true); // может расти
    reportDataBand.setPrintIfDetailEmpty(true);
    reportDataBand.setHeight(cellHeight + 0.01);
    reportDataBand.setCanShrink(false);

    if (!exportSettings.getIfWordWrap()) {
      reportDataBand.setCanGrow(false);
      reportDataBand.setGrowToHeight(false);
    }

    report.getPages().get(0).getComponents().add(reportDataBand);

    if (logger.isTraceEnabled()) {
      logger.trace("Add data text field ---->");
      logger.trace("text field count = " + rootColumns.get().size());
    }

    for (Iterator it = rootColumns.get().iterator(); it.hasNext(); ) {
      colInfo = (ReportColumnInfo) it.next();

      if (colInfo.getСolumn() == null && (colInfo.fieldName.get().isEmpty() && sed != SizeExportedDataEnum.FULL_DATA))
        continue;

      StiText cellText2 = new StiText(); //Report.this.getCellControl(colInfo, tSource );

      cellText2.setName(String.format("cell_%s", colInfo.fieldName.get()));
      cellText2.setText(String.format("{%s.%s}", tSource.getName(), colInfo.fieldName.get()));
      //cellText2.setBorder( cellBorder );
      //cellText2.getBorder().setSide( new StiEnumSet( StiBorderSides.Right, StiBorderSides.Left ) );
      cellText2.setClientRectangle(new StiRectangle(colInfo.left.get(), 0, colInfo.width.get(), cellHeight));
      //cellText2.setClientRectangle( new StiRectangle(colInfo.left.get(), 0, colInfo.optimumWidth.get(), cellHeight) );
      cellText2.setHideZeros(exportSettings.getIfHideZero());
      if (logger.isTraceEnabled())
        logger.trace("---------->" + String.format("{%s.%s}", tSource.getName(), colInfo.fieldName.get()));
      cellText2.setMargins(margins);
      cellText2.setFont(titleFont);
      cellText2.setVertAlignment(StiVertAlignment.Top);
      if (colInfo.getСolumn() != null) {
        switch (((JInvTableColumn) colInfo.getСolumn()).getAlignment()) {
          case CENTER:
            cellText2.setHorAlignment(StiTextHorAlignment.Center);
            break;
          case TOP_LEFT:
          case BOTTOM_LEFT:
          case CENTER_LEFT:
            cellText2.setHorAlignment(StiTextHorAlignment.Left);
            break;
          case TOP_RIGHT:
          case BOTTOM_RIGHT:
          case CENTER_RIGHT:
            cellText2.setHorAlignment(StiTextHorAlignment.Right);
            break;
          default:
            IEntityProperty pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(colInfo.fieldName.get());
            if (pd.getType() == String.class) {
              cellText2.setHorAlignment(StiTextHorAlignment.Left);
            }
            if (pd.getType() == LocalDate.class) {
              cellText2.setHorAlignment(StiTextHorAlignment.Center);
            }
            if (pd.getType() == BigDecimal.class) {
              cellText2.setHorAlignment(StiTextHorAlignment.Right);
            }


        }
      } else {
        IEntityProperty pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(colInfo.fieldName.get());
        if (logger.isTraceEnabled()) logger.trace("pd    : " + pd.getType().toString());
        if (pd.getType() == String.class) {
          cellText2.setHorAlignment(StiTextHorAlignment.Left);
        } else {
          cellText2.setHorAlignment(StiTextHorAlignment.Right);
        }
      }
      ///cellText2.getTextOptions().setWordWrap( exportSettings.getIfWordWrap() );
      ///cellText2.setCanShrink(false);
      //cellText2.setExcelDataValue(colInfo.getСolumn().getId());
      //logger.trace("cellText2.setExcelDataValue = "+colInfo.toString());
      if (!exportSettings.getIfWordWrap()) {
        cellText2.setCanGrow(false);
        cellText2.setGrowToHeight(false);
        cellText2.setHeight(reportDataBand.getHeight());
      }
      String fn = (colInfo.getСolumn() == null) ? colInfo.fieldName.get() : Controls.getFieldNameFromTableColumn(colInfo.getСolumn());
      IEntityProperty pd = null;
      pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(fn);
      //logger.trace("pd    : "+pd.getType().toString());
      if (pd.getType() == BigDecimal.class
      ) {
        //logger.trace("SET NUMBER FORMAT FOR FIELD");
        int digits = 4;
        int groupSize = 3;
        if (colInfo.getСolumn() instanceof JInvTableColumnMoney) {
          digits = ((JInvTableColumnMoney) colInfo.getСolumn()).getPrecision();
          groupSize = ((JInvTableColumnMoney) colInfo.getСolumn()).getDecimalFormat().getGroupingSize();
        }
        cellText2.setTextFormat(new StiNumberFormatService(1, ",", digits, " ", groupSize, groupSize > 0, false, " "));
        cellText2.setExcelDataValue(String.format("{%s.%s}", tSource.getName(), colInfo.fieldName.get()));
        cellText2.setHorAlignment(StiTextHorAlignment.Right);
        cellText2.setNullValue(" ");
        cellText2.setHideZeros(false);

      }
      if (pd.getType() == Long.class
      ) {
        //logger.trace("SET NUMBER FORMAT FOR FIELD");
        String groupSep = "";
        int groupSize = 0;
        if (colInfo.getСolumn() instanceof JInvTableColumnMoney) {
          groupSep = " ";
          groupSize = 3;
        }
        cellText2.setTextFormat(new StiNumberFormatService(1, "", 0, groupSep, groupSize, groupSize > 0, false, " "));
        cellText2.setExcelDataValue(String.format("{%s.%s}", tSource.getName(), colInfo.fieldName.get()));
        cellText2.setHorAlignment(StiTextHorAlignment.Right);
        cellText2.setHideZeros(false);
//                    StiExcelValueExpression ss = new StiExcelValueExpression();
//                    ss.setValue(fn);
//                    cellText2.setExcelValue(value);
//
      }
      if (pd.getType() == java.time.LocalDate.class
      ) {
        //logger.trace("SET DATE FORMAT FOR FIELD   " +String.format( "{%s.%s}", tSource.getName(), colInfo.fieldName.get() ));
        String maskDate = "dd.MM.yyyy";
        if (colInfo.getСolumn() instanceof JInvTableColumnDate) {
          String m = ((JInvTableColumnDate) colInfo.getСolumn()).getMask();
          String m1 = ((JInvTableColumnDate) colInfo.getСolumn()).getDateFormat().getMask();
          if (logger.isTraceEnabled()) {
            logger.trace("m = " + m);
            logger.trace("m1 = " + m1);
          }
          maskDate = (m == null || m.isEmpty()) ? ((m1 == null || m1.isEmpty()) ? maskDate : m1) : m;
          if (logger.isTraceEnabled()) {
            logger.trace("maskDate = " + maskDate);
          }
        }
        cellText2.setTextFormat(new StiDateFormatService(maskDate, ""));
        //cellText2.setExcelDataValue(String.format( "{%s.%s}", tSource.getName(), colInfo.fieldName.get() ));
        cellText2.setHorAlignment(StiTextHorAlignment.Center);
      }
      if (pd.getType() == LocalDateTime.class) {
        final JInvTableColumnDate columnDate = (JInvTableColumnDate) colInfo.getСolumn();
        final String mask = columnDate.getDateFormat().getMask();
        cellText2.setTextFormat(new StiDateFormatService(mask, ""));
        cellText2.setHorAlignment(StiTextHorAlignment.Center);
      }
      if (pd.getType() == Boolean.class) {
        cellText2.setTextFormat(new StiBooleanFormatService("false", "true", "0", "1", "0"));
      }
      //cellText2.setTextFormat(new StiNumberFormatService());
      //cellText2.setExcelValue( new StiExcelValueExpression(String.format( "{%s.%s}", tSource.getName(), colInfo.fieldName.get() )) );
      //cellText2.setExcelDataValue(String.format( "{%s.%s}", tSource.getName(), colInfo.fieldName.get() ));
      reportDataBand.getComponents().add(cellText2);

      if (cellText2.getRight() > reportDataBand.getRight())
        reportDataBand.setWidth(cellText2.getRight() - reportDataBand.getLeft());
    }

    //FooterBand
    StiFooterBand reportFooterBand = new StiFooterBand(new StiRectangle(0, cellHeight * 6, headerWidth.get(), 0.04)); //pageWidth, 0.04));
    reportFooterBand.setName("reportFooterBand");
    reportFooterBand.setCanGrow(false);// может расти

    report.getPages().get(0).getComponents().add(reportFooterBand);

    StiHorizontalLinePrimitive lineFooter = new StiHorizontalLinePrimitive(new StiRectangle(0.0, 0.0, headerWidth.get(), 0.04)); //pageWidth,0.03));
    reportFooterBand.getComponents().add(lineFooter);
    lineFooter.setName("lineFooter");
    lineFooter.setColor(StiColor.Black);
    lineFooter.setPrintOn(StiPrintOnType.AllPages);

    if (exportSettings.getIfPrintPageNofM()) {
      StiPageFooterBand pageFooterBand = new StiPageFooterBand(new StiRectangle(report.getPages().get(0).getHeight() - 1, 0.0, headerWidth.get(), 0.5));

      pageFooterBand.setName("PageFooterBand");
      pageFooterBand.SetDockStyle(StiDockStyle.Bottom);

      report.getPages().get(0).getComponents().add(pageFooterBand);

      StiText TextFooter = getCellControl("TextFooter", "{PageNofM}", StiTextHorAlignment.Right,
        titleCaptionFont, StiColor.Black, StiColor.White, defaultMargins, StiBorderSides.None, false);

      TextFooter.setClientRectangle(new StiRectangle(headerWidth.get() - 5, 0.0, 5.0, 0.5));
      TextFooter.setTag(new StiTagExpression("Страница #PageNumber# из #TotalPageCount#"));
      TextFooter.setDockStyle(StiDockStyle.Fill);
      TextFooter.setHorAlignment(StiTextHorAlignment.Right);

      pageFooterBand.getComponents().add(TextFooter);
    }

    try {
      report.render();

    } catch (Exception e) {
      StiExceptionProvider.show(e, null);
    }
//        try{
//			            	FileOutputStream fos = new FileOutputStream("c:\\Temp\\REPORT_TEST_SAVE_GRID.mrt");
//			        		StiSerializeManager.serializeReport(report, fos);
//			        		fos.close();
//			            } catch (Exception e){
//			                e.printStackTrace();
//			            }


    return report;

  }

  //Инициализация банда-заголовка
  private void addHeaderBandTexts(StiContainer rootContainer, ArrayList bands, int depth) {
    if (logger.isTraceEnabled()) logger.trace("Size BANDS LIST = " + bands.size());
    for (Iterator it = bands.iterator(); it.hasNext(); ) {


      ReportHeaderBandInfo bandInfo = ((Ref<ReportHeaderBandInfo>) it.next()).get();
      if (logger.isTraceEnabled()) logger.trace("band caption = " + bandInfo.caption.get());

      StiText bandText2;

      if (bandInfo.bandHeader != null) {
        if (Controls.getFieldNameFromTableColumn(bandInfo.bandHeader) == null || Controls.getFieldNameFromTableColumn(bandInfo.bandHeader).isEmpty())
          bandText2 = retCellControl("band_" + (rootContainer.getComponents().size() + 1)
            , bandInfo.caption.get().replaceAll("\n", " ")
            , StiTextHorAlignment.Center
            , bandFont, bandForeColor, bandColor, false);
        else
          bandText2 = retCellControl("band_" + Controls.getFieldNameFromTableColumn(bandInfo.bandHeader).replace(" ", "")
            , bandInfo.caption.get().replaceAll("\n", " ")
            , StiTextHorAlignment.Center
            , bandFont, bandForeColor, bandColor, false);
      } else {
        if (logger.isTraceEnabled())
          logger.trace("DATA PROPERTY CREATING " + "band_" + bandInfo.bandHeader4Field.getPropertyName().replace(" ", "") + "   " + bandInfo.caption.get());
        bandText2 = retCellControl("band_" + bandInfo.bandHeader4Field.getPropertyName().replace(" ", "")
          , bandInfo.caption.get().replaceAll("\n", " ")
          , StiTextHorAlignment.Center
          , bandFont, bandForeColor, bandColor, false);

      }
      bandText2.getBorder().setSide(StiBorderSides.All);
      if (logger.isTraceEnabled())
        logger.trace("band left = " + bandInfo.left.get() + ", width = " + bandInfo.width.get());
      //bandText2.setClientRectangle( new StiRectangle(bandInfo.left.get(), depth * cellHeight, bandInfo.optimumWidth.get(), cellHeight) );
      bandText2.setClientRectangle(new StiRectangle(bandInfo.left.get(), depth * cellHeight, bandInfo.width.get(), cellHeight));
      bandText2.setShiftMode(new StiEnumSet(StiShiftMode.IncreasingSize, StiShiftMode.OnlyInWidthOfComponent));
      bandText2.setFont(valuesFont);

      if (bandInfo.bandsHeader.get().size() > 0)
        bandText2.setGrowToHeight(false);
      bandText2.setHorAlignment(StiTextHorAlignment.Center);
      rootContainer.getComponents().add(bandText2);

      addHeaderBandTexts(rootContainer, bandInfo.bandsHeader.get(), depth + 1);
    }
  }

  private StiText getCellControl(ReportColumnInfo colInfo, StiDataSource source) {
    StiText cellText = Report.this.getCellControl(colInfo, source, defaultMargins, StiBorderSides.Right);
    cellText.getBorder().getSide().add(StiBorderSides.Left);
    return cellText;
  }

  private StiText getCellControl(ReportColumnInfo colInfo, StiDataSource source, StiMargins margin, StiBorderSides sides) {
    TableColumn column = colInfo.getСolumn();

    StiColor foreColor = StiColor.Black;
    StiColor backColor = StiColor.White;

    StiText cellText = getCellControl(String.format("cell_%s", colInfo.fieldName.get())
      , String.format("{%s.%s}", source.getName(), colInfo.fieldName.get())
      , getStiHorAlignment(column)
      , defaultFont
      , foreColor, backColor
      , margin, sides, false);

    cellText.setShiftMode(new StiEnumSet(StiShiftMode.IncreasingSize, StiShiftMode.DecreasingSize, StiShiftMode.OnlyInWidthOfComponent));

    if (this.getColMask(column) != null && !this.getColMask(column).isEmpty()) {

      Class colType;

      if (column instanceof JInvTableColumnBigDecimal)
        colType = BigDecimal.class;
      else if (column instanceof JInvTableColumnDate)
        colType = Date.class;
      else
        colType = this.getColType(column);

      if (colType == Date.class) {
        cellText.setTextFormat(new StiDateFormatService(this.getColMask(column), ""));
      } else if (colType == BigDecimal.class || colType == Double.class || colType == Float.class || colType == Long.class) {
        if (this.getColMask(column).indexOf(".") > -1) {
          int digits = this.getColMask(column).substring(this.getColMask(column).indexOf(".") + 1).trim().length();

          if (column instanceof JInvTableColumnBigDecimal) {
            if(((JInvTableColumnBigDecimal) column).getPrecision() >= 0)
              digits = ((JInvTableColumnBigDecimal) column).getPrecision();
          }
          cellText.setTextFormat(new StiNumberFormatService(1, ",", digits, " ", 3, true, false, " "));
        } else
          cellText.setTextFormat(new StiCustomFormatService(this.getColMask(column)));

      } else if (this.getColMask(column) != null && !this.getColMask(column).isEmpty())
        cellText.setTextFormat(new StiCustomFormatService(this.getColMask(column)));
    } else {
      if (logger.isTraceEnabled()) logger.trace("Type data : " + colInfo.getСolumn().toString());
      IEntityProperty pd = null;

      if (rowClass != null) {
        //pd = JInvReflectionUtils.getPropertyDescriptor( rowClass, Controls.getFieldNameFromTableColumn(column) );
        pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(Controls.getFieldNameFromTableColumn(column));
        if (logger.isTraceEnabled()) logger.trace("pd : " + pd.getType().toString());
        if (pd.getType() == BigDecimal.class) {
          if (logger.isTraceEnabled()) logger.trace("-------->    set StiNumberFormatService");
          cellText.setTextFormat(new StiNumberFormatService(1, ",", 4, " ", 3, true, false, " "));
        }
      }
    }

    return cellText;
  }

  private StiText retCellControl(String name, String text, StiTextHorAlignment horAlignment
    , StiFont font, StiColor foreColor, StiColor backColor
    , boolean setExcelValue) {
    return getCellControl(name, text, horAlignment
      , font, foreColor, backColor
      , defaultMargins, StiBorderSides.All, setExcelValue);
  }

  private StiText getCellControl(String name, String text, StiTextHorAlignment horAlignment
    , StiFont font, StiColor foreColor, StiColor backColor
    , StiMargins margin, StiBorderSides sides, boolean setExcelValue) {
    StiText cellText = new StiText();
    if (logger.isTraceEnabled()) logger.trace("getCellControl");
    cellText.setName(name);

    if (sides != StiBorderSides.All)
      cellText.setBorder(new StiBorder(sides, StiColor.Black, 1, StiPenStyle.Solid));

    else
      cellText.setBorder(defaultBorder);

    cellText.setBrush(new StiSolidBrush(backColor));
    cellText.setTextBrush(new StiSolidBrush(foreColor));
    cellText.setFont(font);

    //cellText.setHorAlignment( horAlignment );
    cellText.setVertAlignment(StiVertAlignment.Center);

    cellText.setText(text);

        /*if (setExcelValue)
            cellText.setExcelValue( new StiExcelValueExpression(text) );*/

    cellText.getTextOptions().setWordWrap(true);

    cellText.setCanGrow(true);
    cellText.setCanShrink(false);
    cellText.setGrowToHeight(true);
    cellText.setMargins(margin);

    return cellText;
  }

  /**
   * Начало построения шапки
   *
   * @param ifExportInvisibeColumns
   */
  public void getBandHeaderInfo(boolean ifExportInvisibeColumns) {

    if (sourceTable.getColumns() == null) return;

    if (logger.isTraceEnabled())
      logger.trace("getBandHeaderInfo ifExportInvisibeColumns = " + ifExportInvisibeColumns);
    //Начальное получение информации о шапках и колонках
    if (sourceTable.getColumns() != null)
      for (TableColumn column : (List<TableColumn>) sourceTable.getColumns()) {
        if ((ifExportInvisibeColumns || column.isVisible()) && column instanceof JInvTableColumn)
          addBandHeaderInfo(rootBands, 0, rootColumns, headerWidth, column, ifExportInvisibeColumns);
      }

    if (this.sizeExportedData == SizeExportedDataEnum.FULL_DATA) {

      Class row = sourceTable.getItems().get(0).getClass();

      logger.debug("row_class= " + row);

      final Collection<IEntityProperty> values = EntityMetadataFactory.getEntityMetaData(row).getPropertiesMap().values();

      for (IEntityProperty fld : values) {
        if (logger.isTraceEnabled())
          logger.trace("row.fld_name = " + fld.getPropertyName());

        boolean if_exist = false;
        for (Iterator it = rootColumns.get().iterator(); it.hasNext(); ) {
          ReportColumnInfo colInfo = (ReportColumnInfo) it.next();
          String idColumns = ((colInfo.getСolumn() == null) ? colInfo.fieldName.get() : Controls.getFieldNameFromTableColumn(colInfo.getСolumn()));

          if (idColumns == null ? fld.getColumnName() == null : idColumns.equals(fld.getColumnName())) {
            if_exist = true;
            break;
          }
        }
        if (!if_exist) {
          addBandHeaderInfoDS(rootBands, this.pageWidth / 30.0, rootColumns, headerWidth, fld, ifExportInvisibeColumns);
        }
      }
    }
    //1. Если есть запас по ширине - заменяем ширины колонок с минимальных на оптимальные
    if (headerWidth.get() < pageWidth) {
      ReportColumnInfo colInfo;
      double delta = 0.0;

      for (Iterator it = rootColumns.get().iterator(); it.hasNext(); ) {
        colInfo = (ReportColumnInfo) it.next();

        if (colInfo.optimumWidth.get() > colInfo.width.get()
          && headerWidth.get() + colInfo.optimumWidth.get() - colInfo.width.get() < pageWidth) {
          delta = colInfo.optimumWidth.get() - colInfo.width.get();

          // пересчитываем ширину родительских бандов
          widenBand(colInfo.band, delta, false);

          colInfo.width.set(colInfo.optimumWidth.get());

          headerWidth.set(headerWidth.get() + delta);
        }
      }
    }

    //2. Если все еще есть запас по ширине - то же самое делаем с бандами
    if (headerWidth.get() < pageWidth)
      shiftHeaderWidth(rootBands.get(), headerWidth);

    //3.
    if (headerWidth.get() < pageWidth && false) {
      if (rootColumns.get() != null && rootColumns.get().size() > 0) {
        widthDeltaCoeff = (int) (0.5 + 100 * (pageWidth - headerWidth.get()) / rootColumns.get().size()) / 100.0;
        Report.this.shiftHeaderWidth(rootBands.get(), headerWidth, widthDeltaCoeff);

      }
    }

  }

  private void shiftHeaderWidth(ArrayList headers, Ref<Double> left, double delta) {
    ReportHeaderBandInfo headerInfo = null;

    for (Iterator it = headers.iterator(); it.hasNext(); ) {
      headerInfo = ((Ref<ReportHeaderBandInfo>) it.next()).get();

      if (delta > 0 && (left.get() + delta) < pageWidth) {
        // сдвиг следующих
        widenBand(headerInfo, delta, true);
        left.set(left.get() + delta);
      }

      if (headerInfo.bandsHeader.get().size() > 0)
        Report.this.shiftHeaderWidth(headerInfo.bandsHeader.get(), left, delta);
    }
  }

  private void shiftHeaderWidth(ArrayList headers, Ref<Double> left) {
    ReportHeaderBandInfo headerInfo = null;

    for (Iterator it = headers.iterator(); it.hasNext(); ) {
      headerInfo = ((Ref<ReportHeaderBandInfo>) it.next()).get();

      if (headerInfo.width.get() < headerInfo.optimumWidth.get()
        && (left.get() + headerInfo.optimumWidth.get() - headerInfo.width.get()) < pageWidth) {
        double delta = headerInfo.optimumWidth.get() - headerInfo.width.get();

        // сдвиг следующих
        widenBand(headerInfo, delta, true);
        left.set(left.get() + delta);
      }

      if (headerInfo.bandsHeader.get().size() > 0)
        shiftHeaderWidth(headerInfo.bandsHeader.get(), left);
    }
  }

  // Увеличивает ширину банда, сдвигает соседей и родителей, а также детей, если третий аргумент = true
  private void widenBand(ReportHeaderBandInfo header, double delta, boolean widenChilds) {
    // 1. Уширение банда
    Ref<ReportHeaderBandInfo> headerInfo = new Ref<>(header);
    headerInfo.get().width.set(headerInfo.get().width.get() + delta);

    // 2.1 Сдвижка всех правых соседей. Алгоритм: берется родительский банд, расширяется и сдвигаются все соседи
    // слева от банда. Потом берется следующий родительский банд...
    int headerIndex = -1;
    while (headerInfo.get().parent != null) {
      headerIndex = -1;

      Ref<ReportHeaderBandInfo> parentHeaderInfo = new Ref<>(headerInfo.get().parent);
      parentHeaderInfo.get().width.set(parentHeaderInfo.get().width.get() + delta);

      for (int i = 0; i < parentHeaderInfo.get().bandsHeader.get().size(); i++) {
        if (((Ref<ReportHeaderBandInfo>) parentHeaderInfo.get().bandsHeader.get().get(i)).get() == headerInfo.get()) {
          headerIndex = i;
          break;
        }
      }

      if (headerIndex > -1)
        for (headerIndex = headerIndex + 1; headerIndex < parentHeaderInfo.get().bandsHeader.get().size(); headerIndex++)
          shiftBands(((Ref<ReportHeaderBandInfo>) parentHeaderInfo.get().bandsHeader.get().get(headerIndex)).get(), delta);

      headerInfo.set(parentHeaderInfo.get());
    }

    // 2.2 То же для верхнего уровня
    headerIndex = -1;
    for (int i = 0; i < rootBands.get().size(); i++) {
      if (((Ref<ReportHeaderBandInfo>) rootBands.get().get(i)).get() == headerInfo.get()) {
        headerIndex = i;
        break;
      }
    }

    if (headerIndex > -1) {
      for (headerIndex = headerIndex + 1; headerIndex < rootBands.get().size(); headerIndex++)
        shiftBands(((Ref<ReportHeaderBandInfo>) rootBands.get().get(headerIndex)).get(), delta);
    }

    // 3. Уширение потомков. Вызываемые функции пересчитают сдвижку для каждого потомка.
    if (widenChilds) {
      if (header.bandsHeader.get().size() > 0)
        widenBandChilds(header.bandsHeader.get(), delta);
      else
        widenColumnChilds(header.bandsData.get(), delta);
    }
  }

  private void widenBandChilds(ArrayList headers, double delta) {
    double bandShift = 0.0;
    double deltaBand = (int) (0.5 + 100 * delta / headers.size()) / 100.0;

    for (Iterator it = headers.iterator(); it.hasNext(); ) {

      Ref<ReportHeaderBandInfo> header = (Ref<ReportHeaderBandInfo>) it.next();

      if (this.getIndex(headers, header) == headers.size() - 1)
        deltaBand = delta - deltaBand * (headers.size() - 1);

      header.get().width.set(header.get().width.get() + deltaBand);

      shiftBands(header.get(), bandShift);

      bandShift += deltaBand;

      if (header.get().bandsHeader.get().size() > 0)
        widenBandChilds(header.get().bandsHeader.get(), deltaBand);
      else
        widenColumnChilds(header.get().bandsData.get(), deltaBand);
    }
  }

  private int getIndex(ArrayList listItem, Object item) {
    for (int i = 0; i < listItem.size(); i++)
      if (listItem.get(i) == item)
        return i;
    return -1;
  }

  private void widenColumnChilds(ArrayList columns, double delta) {
    if (columns.size() == 0)
      return;

    double colShift = 0.0;
    double deltaColumn = (int) (0.5 + 100 * delta / columns.size()) / 100.0;

    for (Iterator it = columns.iterator(); it.hasNext(); ) {
      ReportColumnInfo colInfo = (ReportColumnInfo) it.next();

      if (getIndex(columns, colInfo) == columns.size() - 1)
        deltaColumn = delta - deltaColumn * (columns.size() - 1);

      colInfo.width.set(colInfo.width.get() + deltaColumn);
      colInfo.left.set(colInfo.left.get() + colShift);

      colShift += deltaColumn;
    }
  }

  // Сдвигает все дочерние банды банда parent и колонки в них на delta, включая сам банд parent
  private void shiftBands(ReportHeaderBandInfo parent, double delta) {
    parent.left.set(parent.left.get() + delta);

    for (Iterator it = parent.bandsHeader.get().iterator(); it.hasNext(); )
      shiftBands(((Ref<ReportHeaderBandInfo>) it.next()).get(), delta);

    for (Iterator itc = parent.bandsData.get().iterator(); itc.hasNext(); ) {
      ReportColumnInfo colInfo = (ReportColumnInfo) itc.next();
      colInfo.left.set(colInfo.left.get() + delta);
    }
  }

  //Получение информации о шапке колонки
  private ReportHeaderBandInfo addBandHeaderInfo(Ref<ArrayList> bandsHeader,
                                                 double minBandWidth,
                                                 Ref<ArrayList> columns,
                                                 Ref<Double> left,
                                                 TableColumn column,
                                                 boolean ifExportInvisibeColumns) {

    if (column == null || (!column.isVisible() && !ifExportInvisibeColumns))
      return null;
    if (logger.isTraceEnabled()) logger.trace("COLUMN text = " + column.getText());
    //Получение информации о шапке колонки
    Ref<ReportHeaderBandInfo> bandInfo = new Ref<>(new ReportHeaderBandInfo(left.get().doubleValue(), column.getText(), column));

    calcOwnBandWidth(bandInfo, minBandWidth);


    if (column.getColumns() != null && column.getColumns().size() > 0) {
      //Получение информации о дочерних бандах
      for (TableColumn childColumn : (List<TableColumn>) column.getColumns()) {
        if (childColumn.isVisible() || ifExportInvisibeColumns) {
          if (childColumn instanceof JInvTableColumn) {
          } else {
            if (childColumn.getId() == null || childColumn.getId().isEmpty())
              continue;
          }

          ReportHeaderBandInfo childHeaderInfo = addBandHeaderInfo(bandInfo.get().bandsHeader, bandInfo.get().minimumWidth.get() / column.getColumns().size(), columns, left, childColumn, ifExportInvisibeColumns);

          if (childHeaderInfo != null) {
            bandInfo.get().width.set(bandInfo.get().width.get() + childHeaderInfo.width.get());
            childHeaderInfo.parent = bandInfo.get();
          }
        }
      }
    } else {
      //Получение информации о дочерних колонках
      addColumnInfo(bandInfo.get().bandsData, column, left, bandInfo.get().minimumWidth.get() / column.getColumns().size(), ifExportInvisibeColumns);
      if (logger.isTraceEnabled()) logger.trace("..........Adding.........");

      for (Iterator it = bandInfo.get().bandsData.get().iterator(); it.hasNext(); ) {
        ReportColumnInfo colInfo = (ReportColumnInfo) it.next();

        colInfo.band = bandInfo.get();

        bandInfo.get().width.set(bandInfo.get().width.get() + colInfo.width.get());
        if (logger.isTraceEnabled()) logger.trace("..........Added.........");
        columns.get().add(colInfo);
      }
    }

    if (bandInfo.get().width.get() > 0) {
      if (bandInfo.get().minimumWidth.get() > bandInfo.get().optimumWidth.get() / widthCoeff)
        bandInfo.get().minimumWidth.set(bandInfo.get().optimumWidth.get());

      bandsHeader.get().add(bandInfo);

      return bandInfo.get();
    } else
      return null;
  }

  //Получение информации о коллекции колонок
  private void addColumnInfo4Field(Ref<ArrayList> columns,
                                   IEntityProperty field,
                                   Ref<Double> left,
                                   double minWidth,
                                   boolean ifExportInvisibeColumns) {

    ReportColumnInfo colInfo = new ReportColumnInfo();

    colInfo.left.set(left.get());
    colInfo.width.set(minWidth);

    if (colInfo.width.get() > colInfo.optimumWidth.get() / widthCoeff)
      colInfo.width.set(colInfo.optimumWidth.get());

    colInfo.fieldName.set(field.getColumnName());

    colInfo.setСolumn(null);
    colInfo.band = null;

    left.set(left.get() + colInfo.width.get());

    columns.get().add(colInfo);
  }

  private ReportHeaderBandInfo addBandHeaderInfoDS(Ref<ArrayList> bandsHeader,
                                                   double minBandWidth,
                                                   Ref<ArrayList> columns,
                                                   Ref<Double> left,
                                                   IEntityProperty field,
                                                   boolean ifExportInvisibeColumns) {
    //Получение информации о шапке колонки
    Ref<ReportHeaderBandInfo> bandInfo = new Ref<>(new ReportHeaderBandInfo(left.get().doubleValue(), field.getColumnName(), field));
    calcOwnBandWidth(bandInfo, minBandWidth);
    if (bandInfo.get().width.get() > 0) {
      bandInfo.get().minimumWidth.set(minBandWidth);
    }
    //Получение информации о дочерних колонках
    addColumnInfo4Field(bandInfo.get().bandsData, field, left, bandInfo.get().minimumWidth.get(), ifExportInvisibeColumns);
    if (logger.isTraceEnabled()) logger.trace("..........Adding field.........");

    for (Iterator it = bandInfo.get().bandsData.get().iterator(); it.hasNext(); ) {
      ReportColumnInfo colInfo = (ReportColumnInfo) it.next();

      colInfo.band = bandInfo.get();

      bandInfo.get().width.set(bandInfo.get().width.get() + colInfo.width.get());
      if (logger.isTraceEnabled()) logger.trace("..........Added.........");
      columns.get().add(colInfo);
    }


    bandsHeader.get().add(bandInfo);
    return bandInfo.get();

  }

  private void calcOwnBandWidth(Ref<ReportHeaderBandInfo> headerInfo, double minimumWidth) {
    Report.this.getMaxLength(headerInfo.get().caption.get().split(" "), StiTextHorAlignment.Center, headerInfo.get().minimumWidth);

    if (headerInfo.get().minimumWidth.get() < minimumWidth)
      headerInfo.get().minimumWidth.set(minimumWidth);

    Report.this.getMaxLength(new String[]{headerInfo.get().caption.get()}, StiTextHorAlignment.Center, headerInfo.get().optimumWidth);
  }

  // мин. и опт. ширина для набора текстовых строк parts
  private double getMaxLength(String[] parts, StiTextHorAlignment ha, Ref<Double> maxLength) {
    StiText cellText;

    for (String part : parts) {
      cellText = new StiText();
      cellText.setName("tempCell");
      cellText.setHorAlignment(ha);
      cellText.setVertAlignment(StiVertAlignment.Center);
      cellText.setOnlyText(true);
      cellText.setText(part);
      cellText.getTextOptions().setWordWrap(false);
      cellText.setMargins(new StiMargins(0, 0, 0, 0));
      cellText.setCanGrow(true);//может расти
      cellText.setCanShrink(false);//может сжиматься
      cellText.setGrowToHeight(true);//может расти в высоту
      cellText.setAutoWidth(true);
      cellText.setBorder(new StiBorder(StiBorderSides.All, StiColor.Red, 1, StiPenStyle.Double));

      report.getPages().get(0).getComponents().add(cellText);

      StiSize size = cellText.getActualSize();

      if (size.getHeight() > 1.16 * cellHeight)
        cellHeight = 1.16 * size.getHeight();

      //double refWidth = (int) (0.5 + 100 * (size.getWidth() / 0.85 + 4.0 / widthRatio)) / 100.0;
      double refWidth = Math.ceil(100 * widthCoeff * size.getWidth() + report.getUnit().ConvertFromHInches(margins.getLeft() + margins.getRight())) / 100.0;

      if (maxLength.get() < refWidth)
        maxLength.set(refWidth);

      report.getPages().get(0).getComponents().remove(cellText);
    }

    return maxLength.get();

  }

  // мин. и опт. ширина колонки
  private double getMaxLength(TableColumn column, Ref<Double> optLength) {
    if (this.listRows == null || this.rowClass == null) return optLength.get();

    Ref<Double> maxLength = new Ref<>(0.0);
    StiTextHorAlignment ha = getStiHorAlignment(column);

    double minLength = Report.this.getMaxLength(new String[]{"test"}, ha, maxLength);

    if (minLength > optLength.get())
      optLength.set(minLength);

    String text = null;

    for (Object listRow : this.listRows) {
      if (column.getCellData(listRow) == null) continue;

      text = column.getCellData(listRow).toString();
      //logger.trace("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"+column.getId()+"|"+column.getCellData(listRows.get(i)).getClass().getName()+" = "+text);
      if (column.getCellData(listRow).getClass() == BigDecimal.class) {
        int l = text.length();
        int lc = (int) (l * 1.25);
        int pp = text.indexOf(".");
        int d = lc - 1;
        for (int ii = 0; ii < d; ii++) {
          text = "0" + text;
        }
        if (pp < 0) {
          text = text + ".00";
        }

      }
      Report.this.getMaxLength(text.split(" "), ha, maxLength);
      Report.this.getMaxLength(new String[]{text}, ha, optLength);
    }

    if (maxLength.get() < optLength.get())
      return optLength.get();

    return maxLength.get();
  }

  private StiTextHorAlignment getStiHorAlignment(TableColumn column) {
    //1/по настройкам с колонок
    if (column instanceof JInvTableColumn) {
      Pos hAlignment = ((JInvTableColumn) column).getAlignment();

      if (hAlignment != null) {
        if (hAlignment == Pos.CENTER)
          return StiTextHorAlignment.Center;

        switch (hAlignment.getHpos()) {
          case LEFT:
            return StiTextHorAlignment.Left;
          case CENTER:
            return StiTextHorAlignment.Center;
          case RIGHT:
            return StiTextHorAlignment.Right;
        }
      }

    }

    //2/по данным pojo
    IEntityProperty pd = null;

    if (rowClass != null) {
      //pd = JInvReflectionUtils.getPropertyDescriptor( rowClass, Controls.getFieldNameFromTableColumn(column) );
      pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(Controls.getFieldNameFromTableColumn(column));
            /*Method method = U.firstNotNull(pd.getReadMethod(), pd.getWriteMethod());


            if (method != null && method.isAnnotationPresent(Content.class)) {

                Content typeInfo = method.getAnnotation(Content.class);

                TextAlignment ta = ContentTypeManager.getAlignment( typeInfo.type() );

                if ( ta != null)
                    switch( ta ){
                        case CENTER: return StiTextHorAlignment.Center;
                        case LEFT: return StiTextHorAlignment.Left;
                        case JUSTIFY: return StiTextHorAlignment.Width;
                        case RIGHT: return StiTextHorAlignment.Right;
                    }
            } */
    }

    //3/по идеалогическим строки на лево, числа на право, даты по центру
    if (column instanceof JInvTableColumnDate)
      return StiTextHorAlignment.Center;

    else if (column instanceof JInvTableColumnBigDecimal)
      return StiTextHorAlignment.Right;

    else if (pd != null && pd.getType() != null) {
      if (pd.getType() == BigDecimal.class
        || pd.getType() == Double.class
        || pd.getType() == Integer.class
        || pd.getType() == Long.class
        || pd.getType() == Float.class)
        return StiTextHorAlignment.Right;
      else if (pd.getType() == Date.class
        || pd.getType() == Timestamp.class)
        return StiTextHorAlignment.Center;
    }

    return StiTextHorAlignment.Left;
  }

  private Class getColType(TableColumn column) {
    if (rowClass != null) {
      //PropertyDescriptor pd = JInvReflectionUtils.getPropertyDescriptor( rowClass, Controls.getFieldNameFromTableColumn(column) );
      IEntityProperty pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(Controls.getFieldNameFromTableColumn(column));
      if (pd != null) return pd.getType();
    }

    return String.class;
  }

  private String getColMask(TableColumn column) {
    String maskColumn = null;

    if (column instanceof JInvTableColumnDate)
      maskColumn = ((JInvTableColumnDate) column).getMask();

    if (column instanceof JInvTableColumn)
      maskColumn = ((JInvTableColumn) column).getMask();

    if (maskColumn != null && !maskColumn.isEmpty())
      return maskColumn;

    IEntityProperty pd = null;
    if (rowClass != null) {
      //pd = JInvReflectionUtils.getPropertyDescriptor( rowClass, Controls.getFieldNameFromTableColumn(column) );
      pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(Controls.getFieldNameFromTableColumn(column));
    }

    if (pd == null) return null;

        /*Method method = U.firstNotNull(pd.getReadMethod(), pd.getWriteMethod());

        if (method != null && method.isAnnotationPresent(Content.class)) {

            Content typeInfo = method.getAnnotation(Content.class);
             return ContentTypeManager.getFormatMask( typeInfo.type() );
        }
        */

    return null;
  }

  //Получение информации о коллекции колонок
  private void addColumnInfo(Ref<ArrayList> columns,
                             TableColumn tabColumn,
                             Ref<Double> left,
                             double minWidth,
                             boolean ifExportInvisibeColumns) {
    if ((tabColumn.isVisible() || ifExportInvisibeColumns) && tabColumn instanceof JInvTableColumn) {

      ReportColumnInfo colInfo = new ReportColumnInfo();

      colInfo.left.set(left.get());
      colInfo.width.set(Math.max(getMaxLength(tabColumn, colInfo.optimumWidth), minWidth));

      if (colInfo.width.get() > colInfo.optimumWidth.get() / widthCoeff)
        colInfo.width.set(colInfo.optimumWidth.get());

      colInfo.fieldName.set(Controls.getFieldNameFromTableColumn(tabColumn));

      colInfo.setСolumn(tabColumn);
      colInfo.band = null;

      left.set(left.get() + colInfo.width.get());

      columns.get().add(colInfo);
    }
  }
//    private StiDataSource getSourceDataTable(JInvTable grid){
//        StiDataSource source = null;
//	DataTable sourceTable = null;
//
//        grid.getDataSetAdapter().getDataSet().;
//
//
//    }
//    public static void showGridReport(JInvTable grid){
//        StiReport report = new StiReport();
//        StiDataSource source = null;
//
//        if(grid.getDataSetAdapter().getDataSet() instanceof IDataSet ){
//            source = getSourceDataTable(grid);
//        }
//
//
//    }

}
