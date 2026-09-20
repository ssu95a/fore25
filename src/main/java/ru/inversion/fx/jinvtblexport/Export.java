/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvtblexport;

import org.slf4j.*;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.jinvtblexport.controller.ViewExportController;
import ru.inversion.fx.jinvtblexport.controller.ViewPrintController;
import ru.inversion.fx.jinvtblexport.property.ExportFormatEnum;
import ru.inversion.fx.jinvtblexport.property.ReportProperties;
import ru.inversion.fx.jinvtblexport.property.SizeExportedDataEnum;
import ru.inversion.tc.TaskContext;
import ru.inversion.util.*;

import java.util.HashMap;
import java.util.Map;

import static java.text.MessageFormat.format;

/**
 * @author polyatykina
 */
public class Export {

  protected static Logger log = LoggerFactory.getLogger(Export.class);

  private static ExportSettings defaulfExportSettings = new ExportSettings();

  public static ExportSettings getDeafaultExportSettings() {
    return defaulfExportSettings;
  }

  public static String getReport(JInvTable tw,
                                 ExportFormatEnum format,
                                 String file,
                                 boolean isOpenAfterExport,
                                 SizeExportedDataEnum sed) {
    try {
      ExportSettings exports = new ExportSettings();
      exports.setIfOpenAfterExport(isOpenAfterExport);
      Report.setExportSettings(exports);
      return Report.export(tw, format, null, file, sed);
    } catch (Exception ex) {
      JInvErrorService.handleException(null, (Throwable) ex);
    }

    return null;

  }

  public static String getTableReport(JInvTable tw, ExportFormatEnum format, String tempDir, SizeExportedDataEnum sed) {
    try {
      Report.setExportSettings(defaulfExportSettings);
      return Report.export(tw, format, tempDir, null, sed);
    } catch (Exception ex) {
      JInvErrorService.handleException(null, (Throwable) ex);
    }

    return null;
  }

  public static String getTableReport(JInvTable tw,
                                      ExportFormatEnum format,
                                      String tempDir,
                                      ExportSettings exportSettings,
                                      SizeExportedDataEnum sed) {
    try {
      Report.setExportSettings(defaulfExportSettings);
      return Report.export(tw, format, tempDir, null, sed);
    } catch (Exception ex) {
      JInvErrorService.handleException(null, (Throwable) ex);
    }
    return null;

  }

  public static String getTableMRT(JInvTable tw, String tempDir) {
    try {
      Report.setExportSettings(defaulfExportSettings);
      return Report.exportMRT(tw, tempDir);
    } catch (Exception ex) {
      JInvErrorService.handleException(null, (Throwable) ex);
    }

    return null;
  }

  public static String getTableMDC(JInvTable tw, String tempDir) {
    try {
      Report.setExportSettings(defaulfExportSettings);
      return Report.exportMDC(tw, tempDir);
    } catch (Exception ex) {
      JInvErrorService.handleException(null, (Throwable) ex);
    }

    return null;
  }

  public static void printTable(JInvTable tw) {
    Report.print(tw);
  }

  public static void printTable(JInvTable tw, int copies) {
    Report.print(tw, copies);
  }

  /*export из *.Mrt*/
  public static String openToExport(ReportProperties variable) {
    return Export.openToExport(variable, ExportSettings.getExpTempDir());
  }

  public static String openToExport(ReportProperties variable, String tempDir) {
    try {
      Report.setExportSettings(defaulfExportSettings);
      return Report.executeReport(variable, tempDir);
    } catch (Exception ex) {
      JInvErrorService.handleException(null, (Throwable) ex);
    }

    return null;
  }

  /*export*/
  public static void exportDialog(JInvTable tw, TaskContext tc, ViewContext viewContext) {
    //JInvAction
    Map hm = new HashMap();
    hm.put(ExportSettings.KEY_SOURCE, tw);

    //nvFXBrowserController.show(tc, viewContext, "ru/inversion/fx/jinvstimul/fxml/ViewExport.fxml", true,  ViewExportController.g_bundle,  hm, null);
    log.debug(TU.format("Export Controller Class= {0}", ViewExportController.class.getName()));

    new FXFormLauncher(tc, viewContext, ViewExportController.class)
//                .bundle(ViewExportController.g_bundle)
      .initProperties(hm)
//                .dialogMode(AbstractBaseController.FormModeEnum.VM_NONE)
      .modal(true)
      .show();
  }

  public static void exportDialog(AbstractBaseController formCtrl, JInvTable tw, TaskContext tc, ViewContext viewContext) {
    //JInvAction
    Map hm = new HashMap();
    hm.put(ExportSettings.KEY_SOURCE, tw);
    hm.put(ExportSettings.KEY_CONTROLLER, formCtrl);
    log.debug( format("exportDialog().parameterMap= {0} ", hm) );

    log.debug( format("ExportSettings.KEY_CONTROLLER= {0}", formCtrl ) );

    //nvFXBrowserController.show(tc, viewContext, "ru/inversion/fx/jinvstimul/fxml/ViewExport.fxml", true,  ViewExportController.g_bundle,  hm, null);
    log.debug(TU.format("Export Controller Class= {0}", ViewExportController.class.getName()));

    new FXFormLauncher(tc, viewContext, ViewExportController.class)
//                .bundle(ViewExportController.g_bundle)
      .initProperties(hm)
//                .dialogMode(AbstractBaseController.FormModeEnum.VM_NONE)
      .modal(true)
      .show();
  }

  /*print*/
  public static void printDialog(JInvTable tw, TaskContext tc, ViewContext viewContext) {
    printDialog(tw, tc, viewContext, null);
  }

  public static void printDialog(JInvTable tw, TaskContext tc, ViewContext viewContext, String title) {
    Map hm = new HashMap();
    hm.put(ExportSettings.KEY_SOURCE, tw);

    if (title != null && !title.isEmpty())
      hm.put(ExportSettings.KEY_TITLE, title);

    new FXFormLauncher<>(tc, viewContext, "ru/inversion/fx/jinvstimul/fxml/ViewPrint.fxml")
      .initProperties(hm)
      .bundle(ViewPrintController.g_bundle)
      .modal(true)
      .show();
  }
}
