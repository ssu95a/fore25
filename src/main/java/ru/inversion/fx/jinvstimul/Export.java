/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvstimul;

import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.jinvstimul.controller.ViewExportController;
import ru.inversion.fx.jinvstimul.controller.ViewPrintController;
import ru.inversion.fx.jinvstimul.property.ExportFormat;
import ru.inversion.fx.jinvstimul.property.ReportProperties;
import ru.inversion.fx.jinvstimul.property.SizeExportedData;
import ru.inversion.tc.TaskContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author polyatykina
 */
public class Export {

    private static ExportSettings defaulfExportSettings = new ExportSettings();

    public static ExportSettings getDeafaultExportSettings() {
        return defaulfExportSettings;
    }

    public static String getReport(JInvTable tw, ExportFormat format, String file, boolean isOpenAfterExport, SizeExportedData sed) {
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

    public static String getTableReport(JInvTable tw, ExportFormat format, String tempDir, SizeExportedData sed) {
        try {
            Report.setExportSettings(defaulfExportSettings);
            return Report.export(tw, format, tempDir, null, sed);
        } catch (Exception ex) {
            JInvErrorService.handleException(null, (Throwable) ex);
        }

        return null;
    }

    public static String getTableReport(JInvTable tw, ExportFormat format, String tempDir, ExportSettings exportSettings, SizeExportedData sed) {
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

        new FXFormLauncher<>(tc, viewContext, "ru/inversion/fx/jinvstimul/fxml/ViewExport_Tx_1.fxml")
                .initProperties(hm)
                .bundle(ViewExportController.g_bundle)
                .modal(true)
                .show();
    }

    /*print*/
    public static void printDialog(JInvTable tw, TaskContext tc, ViewContext viewContext) {
        //printDialog(tw, tc, viewContext, null);
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
