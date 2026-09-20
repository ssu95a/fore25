/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvtblexport.controller;

import com.sun.javafx.scene.control.skin.*;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Toggle;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

import org.slf4j.*;
//import ru.inversion.bicomp.pref.*;
import ru.inversion.db.expr.*;
import ru.inversion.fx.app.*;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.jinvtblexport.ExportSettings;
import ru.inversion.fx.jinvtblexport.property.*;
import ru.inversion.fx.jinvtblexport.property.PageOrientEnum;
import ru.inversion.fx.report.drjasper.*;
import ru.inversion.util.*;

import javax.persistence.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.stream.Stream;

import static ru.inversion.fx.app.property.PropertiesTypeEnum.DB_USER;
import static ru.inversion.util.TU.format;

/**
 * FXML Controller class
 *
 * @author polyatykina
 */
public class ViewExportController extends JInvFXFormController implements IDRJasperConst {

  protected static Logger log = LoggerFactory.getLogger(ViewExportController.class);

  //  public ChoiceBox<PageSizeEnum> pageSizeCIB;
  public ComboBox<PageSizeEnum> pageSizeCMB;
  public ChoiceBox<RecordSourceEnum> recSourceCIB;
  @FXML
  private JInvTextField fileExp;
  @FXML
  private JInvButton dlgOpenFileChoicer;
  @FXML
  private JInvCheckBox ifOpenAfterExport;
  @FXML
  private RadioButton rbExcel;
  @FXML
  public JInvRadioButton rbExcel2007;
  @FXML
  private RadioButton rbRtf;
  @FXML
  private RadioButton rbHtml;
  @FXML
  private RadioButton rbPdf;
  @FXML
  private RadioButton rbWord;
  @FXML
  private RadioButton rbWord2007;
  @FXML
  private RadioButton rbTxt;
  @FXML
  private RadioButton rbJpeg;
  //  @FXML
//  private RadioButton rbSylk;
  @FXML
  private JInvButton dlgOK;
  @FXML
  private JInvButton dlgCancel;

  @FXML
  private RadioButton IF_VISIBLE_COLUMN;
  @FXML
  private RadioButton IF_ALL_COLUMN;
  @FXML
  private RadioButton IF_ALL_DATA;
  @FXML
  public JInvRadioButton portraitRB;
  @FXML
  public JInvRadioButton landscapeRB;

  private UpdateableListViewSkin<PageSizeEnum> pageSizeSkin;

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger("ru.inversion.fx.jinvtblexport.ViewExportController");

  private ToggleGroup columnModeGroup;
  private ToggleGroup exportFormatGroup;
  //    private ToggleGroup markModeGroup;
  private ToggleGroup pageOrientationTG;

//  private To


//  @Override
//  public void initialize(URL url, ResourceBundle rb) {
////        logger.trace( "initialize");
//
////    Tooltip ttExel = new Tooltip();
////    ttExel.setText("Excel");
////    rbExcel.setTooltip(ttExel);
////
////    Tooltip ttRtf = new Tooltip();
////    ttRtf.setText("Rtf");
////    rbRtf.setTooltip(ttRtf);
////
////    Tooltip ttHtml = new Tooltip();
////    ttHtml.setText("Html");
////    rbHtml.setTooltip(ttHtml);
////
////    Tooltip ttPdf = new Tooltip();
////    ttPdf.setText("Pdf");
////    rbPdf.setTooltip(ttPdf);
////
////    Tooltip ttDocx = new Tooltip();
////    ttDocx.setText("Docx");
////    rbDocx.setTooltip(ttDocx);
////
////    Tooltip ttTxt = new Tooltip();
////    ttTxt.setText("Txt");
////    rbTxt.setTooltip(ttTxt);
////
////    Tooltip ttJpeg = new Tooltip();
////    ttJpeg.setText("Jpeg");
////    rbJpeg.setTooltip(ttJpeg);
//////    Tooltip ttSylk = new Tooltip();
//////    ttSylk.setText("Sylk");
//////    rbSylk.setTooltip(ttSylk);
////    exportFormatGroup = new ToggleGroup();
//////    Stream.of(rbExcel, rbRtf, rbHtml, rbPdf, rbDocx, rbTxt, rbJpeg, rbSylk).forEach(ef -> ef.setToggleGroup(exportFormatGroup));
////    Stream.of(rbExcel, rbRtf, rbHtml, rbPdf, rbDocx, rbTxt, rbJpeg).forEach(ef -> ef.setToggleGroup(exportFormatGroup));
////    exportFormatGroup.selectedToggleProperty().addListener((obserableValue, oldToggle, newToggle) -> {
////      log.debug(TU.format("exportFormatGroup.selectedToggleProperty()= {0}", newToggle));
////      if (newToggle != rbExcel && pageSizeCMB.getValue() == PageSizeEnum.AUTO) pageSizeCMB.setValue(PageSizeEnum.A4);
////      pageSizeSkin.refresh();
////
//////        if (group.getSelectedToggle() != null) {
//////            System.out.println("You think that stackoverflow is " + group.getSelectedToggle().getUserData().toString());
//////        }
////    });
////
////    columnModeGroup = new ToggleGroup();
////    Stream.of(IF_VISIBLE_COLUMN, IF_ALL_COLUMN, IF_ALL_DATA).forEach(em -> em.setToggleGroup(columnModeGroup));
////
////    //PageSize/Orietntation
////    pageSizeCMB.getItems().setAll(PageSizeEnum.AUTO, PageSizeEnum.A4, PageSizeEnum.A3, PageSizeEnum.A2, PageSizeEnum.A1);
////    pageSizeCMB.setValue(PageSizeEnum.AUTO);
////    pageSizeCMB.setCellFactory(lv -> new ListCell<PageSizeEnum>() {
////      @Override
////      public void updateItem(PageSizeEnum item, boolean empty) {
////        super.updateItem(item, empty);
////        if (item == null || empty) {
////          setText(null);
////          setGraphic(null);
////        } else {
////          setText(item.toString());
////          log.debug(format("item= {0}; exportFormatGroup= {1}", item, exportFormatGroup.getSelectedToggle()));
////          boolean b = (item == PageSizeEnum.AUTO && exportFormatGroup.getSelectedToggle() != rbExcel);
//////                setDisable(item == PageSizeEnum.AUTO);
////          log.debug(format("pageSizeCMB.Disable Item *{0}* {1}", item.name(), b));
////          setDisable(b);
////        }
////      }
////    });
////
////    recSourceCIB.getItems().setAll(RecordSourceEnum.LOADED, RecordSourceEnum.MARKED, RecordSourceEnum.ALL);
////    recSourceCIB.setValue(RecordSourceEnum.LOADED);
////
////    pageOrientationRG = new ToggleGroup();
////    Stream.of(portraitRB, landscapeRB).forEach(em -> em.setToggleGroup(pageOrientationRG));
////    portraitRB.setSelected(true);
////
//////        markModeGroup = new ToggleGroup();
//////        Stream.of( IF_MARKED_DATA, IF_ALL_DATA ).forEach( em -> em.setToggleGroup( markModeGroup ) );
////
////    rbExcel.setSelected(true);
////    ifOpenAfterExport.setSelected(true);
////
////    fileExport();
////
////    exportFormatGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) -> {
////      if (exportFormatGroup.getSelectedToggle() != null) {
////        fileExport();
////      }
////    });
////
////    dlgOK.setOnAction((ActionEvent event) -> {export();});
////    dlgCancel.setOnAction((ActionEvent event) -> {Close();});
////    dlgOpenFileChoicer.setOnAction((ActionEvent event) -> {fileChoose();});
////
////    if (rb.containsKey("DIALOG.TITLE") && rb.getString("DIALOG.TITLE") != null && !rb.getString("DIALOG" +
////      ".TITLE").isEmpty()) {
////      setTitle(rb.getString("DIALOG.TITLE"));
////    }
//  }

  @Override
  protected void init() throws Exception {
    logger.trace("init");

// ------------------------------------------------
    {
      Tooltip ttExel = new Tooltip();
      ttExel.setText("Excel");
      rbExcel.setTooltip(ttExel);
      rbExcel.setUserData(ExportFormatEnum.Excel);

      Tooltip tt = new Tooltip();
      tt.setText("Excel 2007");
      rbExcel2007.setTooltip(tt);
      rbExcel2007.setUserData(ExportFormatEnum.Excel2007);


      Tooltip ttRtf = new Tooltip();
      ttRtf.setText("Rtf");
      rbRtf.setTooltip(ttRtf);
      rbRtf.setUserData(ExportFormatEnum.Rtf);

      Tooltip ttHtml = new Tooltip();
      ttHtml.setText("Html");
      rbHtml.setTooltip(ttHtml);
      rbHtml.setUserData(ExportFormatEnum.Html);

      Tooltip ttPdf = new Tooltip();
      ttPdf.setText("Pdf");
      rbPdf.setTooltip(ttPdf);
      rbPdf.setUserData(ExportFormatEnum.Pdf);

      tt = new Tooltip();
      tt.setText("Word");
      rbWord.setTooltip(tt);
      rbWord.setUserData(ExportFormatEnum.Word);

      tt = new Tooltip();
      tt.setText("Word 2007");
      rbWord2007.setTooltip(tt);
      rbWord2007.setUserData(ExportFormatEnum.Word2007);

      Tooltip ttTxt = new Tooltip();
      ttTxt.setText("Txt");
      rbTxt.setTooltip(ttTxt);
      rbTxt.setUserData(ExportFormatEnum.Text);

      Tooltip ttJpeg = new Tooltip();
      ttJpeg.setText("Jpeg");
      rbJpeg.setTooltip(ttJpeg);
      rbJpeg.setUserData(ExportFormatEnum.ImageJpeg);


//    Tooltip ttSylk = new Tooltip();
//    ttSylk.setText("Sylk");
//    rbSylk.setTooltip(ttSylk);
      exportFormatGroup = new ToggleGroup();
//      rbExcel.setText();
//    Stream.of(rbExcel, rbRtf, rbHtml, rbPdf, rbDocx, rbTxt, rbJpeg, rbSylk).forEach(ef -> ef.setToggleGroup(exportFormatGroup));
      Stream.of(rbExcel, rbExcel2007, rbRtf, rbHtml, rbPdf, rbWord, rbWord2007, rbTxt, rbJpeg).forEach(ef -> ef.setToggleGroup(exportFormatGroup));
      exportFormatGroup.selectedToggleProperty().addListener((obserableValue, oldToggle, newToggle) -> {
        log.debug(TU.format("exportFormatGroup.selectedToggleProperty()= {0}", newToggle));
//        if (( newToggle != rbExcel ) && pageSizeCMB.getValue() == PageSizeEnum.AUTO) pageSizeCMB.setValue(PageSizeEnum.A4);
        if (( !Arrays.asList(rbExcel, rbExcel2007).contains(newToggle) ) && pageSizeCMB.getValue() == PageSizeEnum.AUTO) pageSizeCMB.setValue(PageSizeEnum.A4);
//        pageSizeSkin.refresh();

//        if (group.getSelectedToggle() != null) {
//            System.out.println("You think that stackoverflow is " + group.getSelectedToggle().getUserData().toString());
//        }
      });

      //PageSize/Orietntation
      pageSizeCMB.getItems().setAll(PageSizeEnum.AUTO, PageSizeEnum.A4, PageSizeEnum.A3, PageSizeEnum.A2, PageSizeEnum.A1);
      pageSizeCMB.setValue(PageSizeEnum.AUTO);
      pageSizeCMB.setCellFactory(lv -> new ListCell<PageSizeEnum>() {
        @Override
        public void updateItem(PageSizeEnum item, boolean empty) {
          super.updateItem(item, empty);
          if (item == null || empty) {
            setText(null);
            setGraphic(null);
          } else {
            setText(item.toString());
            log.debug(format("item= {0}; exportFormatGroup= {1}", item, exportFormatGroup.getSelectedToggle()));
            boolean b = (item == PageSizeEnum.AUTO && exportFormatGroup.getSelectedToggle() != rbExcel);
//                setDisable(item == PageSizeEnum.AUTO);
            log.debug(format("pageSizeCMB.Disable Item *{0}* {1}", item.name(), b));
            setDisable(b);
          }
        }
      });

      recSourceCIB.getItems().setAll(RecordSourceEnum.LOADED, RecordSourceEnum.MARKED, RecordSourceEnum.ALL);
      recSourceCIB.setValue(RecordSourceEnum.LOADED);

      pageOrientationTG = new ToggleGroup();
      portraitRB.setUserData(PageOrientEnum.PORTRAIT);
      landscapeRB.setUserData(PageOrientEnum.LANDSCAPE);

      Stream.of(portraitRB, landscapeRB).forEach(em -> em.setToggleGroup(pageOrientationTG));
      portraitRB.setSelected(true);

//        markModeGroup = new ToggleGroup();
//        Stream.of( IF_MARKED_DATA, IF_ALL_DATA ).forEach( em -> em.setToggleGroup( markModeGroup ) );

      rbExcel.setSelected(true);
      ifOpenAfterExport.setSelected(true);

      fileExport();

      exportFormatGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) -> {
        if (exportFormatGroup.getSelectedToggle() != null) {
          fileExport();
        }
      });

      dlgOK.setOnAction((ActionEvent event) -> {export();});
      dlgCancel.setOnAction((ActionEvent event) -> {Close();});
      dlgOpenFileChoicer.setOnAction((ActionEvent event) -> {fileChoose();});

      if (bundle.containsKey("DIALOG.TITLE") && bundle.getString("DIALOG.TITLE") != null && !bundle.getString("DIALOG" +
        ".TITLE").isEmpty()) {
        setTitle(getBundleString("DIALOG.TITLE"));
      }

    }

    final Map<Integer, Boolean> mapAct = new TreeMap<>();

    mapAct.put(3950, true);
    mapAct.put(3951, true);
    mapAct.put(3952, true);

    JInvSecurityService.isCanAccessList(getTaskContext(), mapAct);

//    *columnModeGroup*
    IF_VISIBLE_COLUMN.setDisable(!mapAct.get(3950));
    IF_ALL_COLUMN.setDisable(!mapAct.get(3951));
    IF_ALL_DATA.setDisable(!mapAct.get(3952));
//    Stream.of(IF_VISIBLE_COLUMN, IF_ALL_COLUMN, IF_ALL_DATA).filter(em -> !em.isDisabled()).findFirst().map(em -> {
//      em.setSelected(true); return em;
//    });
    IF_VISIBLE_COLUMN.setUserData(SizeExportedDataEnum.DISPLAYED_TABLE);
    IF_ALL_COLUMN.setUserData(SizeExportedDataEnum.FULL_TABLE);
    IF_ALL_DATA.setUserData(SizeExportedDataEnum.FULL_DATA);

    columnModeGroup = new ToggleGroup();
    Stream.of(IF_VISIBLE_COLUMN, IF_ALL_COLUMN, IF_ALL_DATA).forEach(em -> em.setToggleGroup(columnModeGroup));

    Stream.of(IF_VISIBLE_COLUMN, IF_ALL_COLUMN, IF_ALL_DATA).filter(em -> !em.isDisabled()).findFirst()
      .ifPresent(em -> {
        em.setSelected(true);
        log.debug(format("INIT.ColumnModeGroup.defaultSelectedValue= {0}", em.getUserData() + ""));
      });
//    map(em -> {
//      em.setSelected(true); return em;
//    });

    ListView<PageSizeEnum> lv = ((ComboBoxListViewSkin) this.pageSizeCMB.getSkin()).getListView();
    pageSizeSkin = new UpdateableListViewSkin<>(lv); // Injected by FXML
    lv.setSkin(pageSizeSkin);//
//    ((MySkin) listView.getSkin()).refresh(); // This is how you use it
    // !! Restore Items Values
    restoreConfigFromDB2();
  }

  /**
   * Item Names ENUM
   */
  enum INe {
    EXPORT_FORMAT, PAGE_ORIENTATION, SIZE_EXPORTED_DATA, PAGE_SIZE, RECORD_SOURCE, OPEN_AFTER_EXPORT
  }

  @Override
  protected void onCloseWindow() {
    super.onCloseWindow();
    saveConfig2DB2();
  }

  /**
   * Вызывается для кнопки ОК
   */
  private void export() {
    new JInvParallelAction((ActionEvent event) -> {
      try {
        AbstractBaseController formCtrl = (AbstractBaseController) this.getInitProperties().get(KEY_CONTROLLER);
        log.debug(format("export().formCtrl= {0}", formCtrl));
        boolean b = this.getInitProperties().get(ExportSettings.KEY_SOURCE) instanceof JInvTable;
        log.debug(format("( this.getInitProperties().get(ExportSettings.KEY_SOURCE) instanceof JInvTable )= {0}", b));
        if (b) {
          //        Export.getReport((JInvTable) this.getInitProperties().get(ExportSettings.KEY_SOURCE)
          //          , getExportFormat()
          //          , fileExp.getText()
          //          , this.ifOpenAfterExport.isSelected()
          //          , getSizeExportedDate()
          //        );

          File expFile = new File(fileExp.getText());

          RepJInvTableUtil rjitu = new RepJInvTableUtil(
            formCtrl
            , (JInvTable) this.getInitProperties().get(KEY_SOURCE)
            , pageSizeCMB.getValue()
            , getPageOrientation()
            , getSizeExportedData()
            , recSourceCIB.getValue()
            , expFile);
          rjitu.init();
          rjitu.exportJInvTableData();

          showExpFile(expFile);

        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }, ((JInvTable) this.getInitProperties().get(ExportSettings.KEY_SOURCE)).getController()).handle();
    Close();
  }

  void showExpFile( File expFile) throws IOException {
    if (ifOpenAfterExport.isSelected()) {
      Desktop dt = Desktop.getDesktop();
      dt.open(expFile);
    }
  }

  private void Close() {
    super.close();
  }

  //static final public ResourceBundle g_bundle = ResourceBundle.getBundle("ru/inversion/fx/jinvstimul/res/ViewExportGraph");
//  static final public ResourceBundle g_bundle = ResourceBundle.getBundle("ru/inversion/fx/jinvtblexport/controller/res/ViewExport");

  private void fileExport() {
    if (fileExp.getText() == null || fileExp.getText().isEmpty())
      fileExp.setText(ExportSettings.getPathFileExport(getExportFormat()));

    else {
      fileExp.setText(
        fileExp.getText().replace(
          fileExp.getText().substring(
            fileExp.getText().lastIndexOf(".")), ExportSettings.getExportExtension(getExportFormat())
        ));
    }
  }

  private ExportFormatEnum getExportFormat() {
//    if (rbExcel.isSelected())
//      return ExportFormatEnum.Excel2007;
//    else if (rbRtf.isSelected())
//      return ExportFormatEnum.Rtf;
//    else if (rbHtml.isSelected())
//      return ExportFormatEnum.Html;
//    else if (rbPdf.isSelected())
//      return ExportFormatEnum.Pdf;
//    else if (rbDocx.isSelected())
//      return ExportFormatEnum.Word2007;
//    else if (rbTxt.isSelected())
//      return ExportFormatEnum.Text;
//    else if (rbJpeg.isSelected())
//      return ExportFormatEnum.ImageJpeg;
////    else if (rbSylk.isSelected())
////      return ExportFormat.Sylk;

    if (exportFormatGroup.getSelectedToggle() != null) {
      return (ExportFormatEnum) exportFormatGroup.getSelectedToggle().getUserData();
    }

    return null;
  }

  private SizeExportedDataEnum getSizeExportedData() {
//    Optional<Toggle> tgOpt
    return (SizeExportedDataEnum) Optional.ofNullable(columnModeGroup.getSelectedToggle())
      .orElse(IF_VISIBLE_COLUMN).getUserData();


//    if (IF_VISIBLE_COLUMN.isSelected())
//      return SizeExportedDataEnum.DISPLAYED_TABLE;
//    if (IF_ALL_COLUMN.isSelected())
//      return SizeExportedDataEnum.FULL_TABLE;
//    //todo: tell the difference <->
//    if (IF_ALL_DATA.isSelected())
//      return SizeExportedDataEnum.FULL_DATA;

//    return SizeExportedDataEnum.DISPLAYED_TABLE;
  }

  PageOrientEnum getPageOrientation() {
//    if (portraitRB.isSelected()) return PageOrientEnum.PORTRAIT;
//    else return PageOrientEnum.LANDSCAPE;
    return (PageOrientEnum) Optional.ofNullable(pageOrientationTG.getSelectedToggle())
      .orElse(portraitRB).getUserData();
  }

//  void restoreConfigFromDB() {
//    String enumStr = null;
//    log.debug("*********************** * ViewExportController.restoreConfigFromDB() .. IN * **********************");
//    try {
//      ExportConfigSettings ecs = new ExportConfigSettings();
//      PreferencesWorker.load(getTaskContext(), ecs);
//      try {
//        enumStr = ecs.pageOrientation;
//        log.debug( format("ecs.pageOrientation= {0}", enumStr ) );
//        if (enumStr != null) {
//          PageOrientEnum poe = PageOrientEnum.valueOf(enumStr);
//          pageOrientationTG.getToggles().stream().filter(t -> (PageOrientEnum) t.getUserData() == poe)
//            .findFirst().ifPresent(t -> {
//              t.setSelected(true);
//              log.debug(format("RESTORE.Selected.pageOrientation.Toggle= {0}", t.getUserData() + ""));
//            });
//        }
//      } catch (IllegalArgumentException e) {
//        log.debug(format("PageOrientation_Enum Value for= {0} NOT Found !!", enumStr));
//      }
//
//      try {
//        enumStr = ecs.exportFormat;
//        log.debug( format("ecs.exportFormat= {0}", enumStr) );
//        if (enumStr != null) {
//          ExportFormatEnum exportFormatEnum = ExportFormatEnum.valueOf(enumStr);
//          exportFormatGroup.getToggles().stream().filter(t -> (ExportFormatEnum) t.getUserData() == exportFormatEnum)
//            .findFirst().ifPresent(t -> {
//              t.setSelected(true);
//              log.debug(format("RESTORE.Selected.exportFormat.Toggle= {0}", t.getUserData() + ""));
//            });
//        }
//      } catch (IllegalArgumentException e) {
//        log.debug(format("ExportFormat_Enum Value for= {0} NOT Found !!", enumStr));
//      }
//
//      try {
//        enumStr = ecs.columnMode;
//        log.debug( format("ecs.columnMode= {0}", enumStr) );
//        if (enumStr != null) {
//          SizeExportedDataEnum sede = SizeExportedDataEnum.valueOf(enumStr);
//          columnModeGroup.getToggles().stream().filter(t -> (SizeExportedDataEnum) t.getUserData() == sede)
//            .findFirst().ifPresent(t -> {
//              t.setSelected(true);
//              log.debug(format("RESTORE.Selected.columnModeGroup.Toggle= {0}", t.getUserData() + ""));
//            });
//        }
//      } catch (IllegalArgumentException e) {
//        log.debug(format("SizeExportedData_Enum Value for= {0} NOT Found !!", enumStr));
//      }
//
//      try {
//        enumStr = ecs.pageSize;
//        log.debug( format("ecs.pageSize= {0}", ecs.pageSize) );
//        if (enumStr != null) {
//          PageSizeEnum pse = PageSizeEnum.valueOf(enumStr);
//          log.debug(format("RESTORE.PageSizeCMB= {0}", pse.name() + ""));
//          pageSizeCMB.setValue(pse);
//        }
//      } catch (IllegalArgumentException e) {
//        log.debug(format("PageSize_Enum Value for= {0} NOT Found !!", enumStr));
//      }
//
//      try {
//        enumStr = ecs.recordSource;
//        log.debug( format("ecs.recordSource= {0}", ecs.recordSource) );
//        if (enumStr != null) {
//          RecordSourceEnum rse = RecordSourceEnum.valueOf(enumStr);
//          log.debug(format("RESTORE.RecordSourceCMB= {0}", rse.name()));
//          recSourceCIB.setValue(rse);
//        }
//      } catch (IllegalArgumentException e) {
//        log.debug(format("PageSize_Enum Value for= {0} NOT Found !!", enumStr));
//      }
//
//      enumStr = ecs.openAfterExport;
//      log.debug(format("RESTORE.IfOpenAfterExport.CHK= {0}", enumStr));
//      if (enumStr != null && enumStr.equals("1") ) ifOpenAfterExport.setSelected(true);
//      else if (enumStr != null && enumStr.equals("0") ) ifOpenAfterExport.setSelected(false);
//
//
//    } catch (SQLExpressionException e) {
//      throw new RuntimeException(e);
//    }
//
//  }

  void restoreConfigFromDB2() {
//    BaseApp.APP().getProperties(DB_USER).setProperty(INe.EXPORT_FORMAT.name(), );
    log.debug("*** * ViewExportController.restoreConfigFromDB() -BaseApp.APP().getProperties- .. IN * ***");
    try {
      // Page Orientation
//      ExportConfigSettings ecs = new ExportConfigSettings();
//      PreferencesWorker.load(getTaskContext(), ecs);
      String enumStr = null;
      try {
        enumStr = BaseApp.APP().getProperties(DB_USER).getStringProperty( INe.PAGE_ORIENTATION.name() );
        if (enumStr != null) {
          PageOrientEnum poe = PageOrientEnum.valueOf(enumStr);
          pageOrientationTG.getToggles().stream().filter(t -> (PageOrientEnum) t.getUserData() == poe)
            .findFirst().ifPresent(t -> {
              t.setSelected(true);
              log.debug(format("RESTORE.Selected.pageOrientation.Toggle= {0}", t.getUserData() + ""));
            });
        }
      } catch (IllegalArgumentException e) {
        log.debug(format("PageOrientation_Enum Value for= {0} NOT Found !!", enumStr));
      }
      // Export Format
      try {
        enumStr = BaseApp.APP().getProperties(DB_USER).getStringProperty(INe.EXPORT_FORMAT.name());
        if (enumStr != null) {
          ExportFormatEnum exportFormatEnum = ExportFormatEnum.valueOf(enumStr);
          exportFormatGroup.getToggles().stream().filter(t -> (ExportFormatEnum) t.getUserData() == exportFormatEnum)
            .findFirst().ifPresent(t -> {
              t.setSelected(true);
              log.debug(format("RESTORE.Selected.exportFormat.Toggle= {0}", t.getUserData() + ""));
            });
        }
      } catch (IllegalArgumentException e) {
        log.debug(format("ExportFormat_Enum Value for= {0} NOT Found !!", enumStr));
      }
// Column Mode Group
      try {
        enumStr = BaseApp.APP().getProperties(DB_USER).getStringProperty(INe.SIZE_EXPORTED_DATA.name());
        if (enumStr != null) {
          SizeExportedDataEnum sede = SizeExportedDataEnum.valueOf(enumStr);
          log.debug(format("SavedExportedDataEnum= {0}", sede + ""));
          columnModeGroup.getToggles().stream().filter(t -> (SizeExportedDataEnum) t.getUserData() == sede)
            .findFirst().ifPresent(t -> {
              t.setSelected(true);
              log.debug(format("RESTORE.Selected.columnModeGroup.Toggle= {0}", t.getUserData() + ""));
            });
        }
      } catch (IllegalArgumentException e) {
        log.debug(format("SizeExportedData_Enum Value for= {0} NOT Found !!", enumStr));
      }

      //PageSize/Orientation
      try {
        enumStr = BaseApp.APP().getProperties(DB_USER).getStringProperty(INe.PAGE_SIZE.name());
        if (enumStr != null) {
          PageSizeEnum pse = PageSizeEnum.valueOf(enumStr);
          log.debug(format("RESTORE.PageSizeCMB= {0}", pse.name() + ""));
          pageSizeCMB.setValue(pse);
        }
      } catch (IllegalArgumentException e) {
        log.debug(format("PageSize_Enum Value for= {0} NOT Found !!", enumStr));
      }
      // Data Source
      try {
        enumStr = BaseApp.APP().getProperties(DB_USER).getStringProperty(INe.RECORD_SOURCE.name());
        if (enumStr != null) {
          RecordSourceEnum rse = RecordSourceEnum.valueOf(enumStr);
          log.debug(format("RESTORE.RecordSourceCMB= {0}", rse.name()));
          recSourceCIB.setValue(rse);
        }
      } catch (IllegalArgumentException e) {
        log.debug(format("PageSize_Enum Value for= {0} NOT Found !!", enumStr));
      }
//    pageSizeCMB.getItems().setAll(PageSizeEnum.AUTO, PageSizeEnum.A4, PageSizeEnum.A3, PageSizeEnum.A2, PageSizeEnum.A1);

      String dbVal = BaseApp.APP().getProperties(DB_USER).getStringProperty(INe.OPEN_AFTER_EXPORT.name());
      log.debug(format("RESTORE.IfOpenAfterExport.CHK= {0}", dbVal));
      if (dbVal != null && dbVal.equals("1") ) ifOpenAfterExport.setSelected(true);
      else if (dbVal != null && dbVal.equals("0") ) ifOpenAfterExport.setSelected(false);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
//    Size
  }

//  void saveConfig2DB() {
////    BaseApp.APP().getProperties(DB_USER).setProperty(INe.EXPORT_FORMAT.name(), );
//    String enumStr = null;
//    boolean configParamsChanged = false;
//    try {
//      String newEnumName = null;
//      Toggle toggle = pageOrientationTG.getSelectedToggle();
//      if (toggle != null) {
//        log.debug(format("SAVING.PageOrientationTG.toggle= {0}", ((PageOrientEnum) toggle.getUserData()).name()));
//        newEnumName = ((PageOrientEnum) toggle.getUserData()).name();
//        if ( !newEnumName.equals(ecs.pageOrientation) ) {
//          ecs.pageOrientation = newEnumName;
//          configParamsChanged = true;
//        }
//      }
//      toggle =  exportFormatGroup.getSelectedToggle();
//      if (toggle != null) {
//        log.debug(format("SAVING.ExportFormatGroup.toggle= {0}", ((ExportFormatEnum) toggle.getUserData()).name()));
//        newEnumName = ((ExportFormatEnum) toggle.getUserData()).name();
//        if ( !newEnumName.equals(ecs.exportFormat) ) {
//          ecs.exportFormat = newEnumName;
//          configParamsChanged = true;
//        }
//      }
//      toggle =  columnModeGroup.getSelectedToggle();
//      if (toggle != null) {
//        log.debug(format("SAVING.СolumnModeGroup.toggle= {0}", ((SizeExportedDataEnum) toggle.getUserData()).name()));
//        newEnumName = ((SizeExportedDataEnum) toggle.getUserData()).name();
//        if ( !newEnumName.equals(ecs.columnMode) ) {
//          ecs.columnMode = newEnumName;
//          configParamsChanged = true;
//        }
//      }
//// ---------------------------------------------------------------------------
//      PageSizeEnum pse = pageSizeCMB.getValue();
//      if (pse != null) {
//        log.debug(format("SAVING.PageSizeCMB.combobox= {0}", pse.name()));
//        newEnumName = pse.name();
//        if ( !newEnumName.equals(ecs.pageSize) ) {
//          ecs.pageSize = newEnumName;
//          configParamsChanged = true;
//        }
//      }
//      // Data Source
//      RecordSourceEnum rse = recSourceCIB.getValue();
//      if (rse != null) {
//        log.debug(format("SAVING.RecordSourceCIB.choiceBox= {0}", rse.name()));
//        newEnumName = rse.name();
//        if ( !newEnumName.equals(ecs.recordSource) ) {
//          ecs.recordSource = newEnumName;
//          configParamsChanged = true;
//        }
//      }
//      // Open after Export
//      log.debug(format("SAVING.IfOpenAfterExport.isSelected()= {0}", ifOpenAfterExport.isSelected()));
//      newEnumName = ifOpenAfterExport.isSelected() ? "1" : "0";
//      if ( !newEnumName.equals(ecs.openAfterExport) ) {
//        ecs.openAfterExport = newEnumName;
//        configParamsChanged = true;
//      }
//
//      if (configParamsChanged) {
//        PreferencesWorker.save(getTaskContext(), ecs);
//      }
//    } catch (SQLExpressionException e) {
//      throw new RuntimeException(e);
//    }
//  }

  void saveConfig2DB2() {
//    BaseApp.APP().getProperties(DB_USER).setProperty(INe.EXPORT_FORMAT.name(), );
    String enumStr = null;
    Toggle toggle = pageOrientationTG.getSelectedToggle();
    if (toggle != null) {
      log.debug(format("SAVING.PageOrientationTG.toggle= {0}", ((PageOrientEnum) toggle.getUserData()).name()));
      BaseApp.APP().getProperties(DB_USER).setProperty(INe.PAGE_ORIENTATION.name()
        , ((PageOrientEnum) toggle.getUserData()).name());
    }
    toggle = exportFormatGroup.getSelectedToggle();
    if (toggle != null) {
      log.debug(format("SAVING.ExportFormatGroup.toggle= {0}", ((ExportFormatEnum) toggle.getUserData()).name()));
      BaseApp.APP().getProperties(DB_USER).setProperty(INe.EXPORT_FORMAT.name()
        , ((ExportFormatEnum) toggle.getUserData()).name());
    }
    toggle = columnModeGroup.getSelectedToggle();
    if (toggle != null) {
      log.debug(format("SAVING.СolumnModeGroup.toggle= {0}", ((SizeExportedDataEnum) toggle.getUserData()).name()));
      BaseApp.APP().getProperties(DB_USER).setProperty(INe.SIZE_EXPORTED_DATA.name()
        , ((SizeExportedDataEnum) toggle.getUserData()).name());
    }
    //PageSize/Orientation
    PageSizeEnum pse = pageSizeCMB.getValue();
    if (pse != null) {
      log.debug(format("SAVING.PageSizeCMB.combobox= {0}", pse.name()));
      BaseApp.APP().getProperties(DB_USER).setProperty(INe.PAGE_SIZE.name()
        , pse.name());
    }
    // Data Source
    RecordSourceEnum rse = recSourceCIB.getValue();
    if (rse != null) {
      log.debug(format("SAVING.RecordSourceCIB.choiceBox= {0}", rse.name()));
      BaseApp.APP().getProperties(DB_USER).setProperty(INe.RECORD_SOURCE.name()
        , rse.name());
    }
    // Open after Export
    log.debug(format("SAVING.IfOpenAfterExport.isSelected()= {0}", ifOpenAfterExport.isSelected()));
    BaseApp.APP().getProperties(DB_USER).setProperty(INe.OPEN_AFTER_EXPORT.name()
      , ifOpenAfterExport.isSelected() ? "1" : "0");

  }

  private void fileChoose() {
    FileChooser fileChooser = new FileChooser();

    fileChooser.setTitle(getBundleString("FILE_CHOOSER.TITLE"));

    if (getExportFormat() != null)
      fileChooser.getExtensionFilters().add(new ExtensionFilter(ExportSettings.getExportExtension(getExportFormat()), String.format("*%s", ExportSettings.getExportExtension(getExportFormat()))));

    fileChooser.getExtensionFilters().addAll(new ExtensionFilter("All Files", "*.*"));

    if (fileExp.getText() != null && !fileExp.getText().isEmpty())
      fileChooser.setInitialDirectory(new File(fileExp.getText()).getParentFile());

    File selectedFile = fileChooser.showOpenDialog(getViewContext().getStage());

    if (selectedFile != null)
      fileExp.setText(selectedFile.getAbsolutePath());
  }

    /*@Override
    public void initBrowserController(TaskContext tc, ViewContext vc, Map initProperties, Consumer<JInvFXBrowserController> clb, boolean modal)
    {
        super.initBrowserController(tc, vc, initProperties, clb, modal);

        if ( vc.getStage() != null )
            vc.getStage().initStyle(StageStyle.UTILITY);

    }*/
//    public static class UpdateableListViewSkin<T> extends ListViewSkin<T> {

  static class UpdateableListViewSkin<T> extends ListViewSkin<T> {

    public UpdateableListViewSkin(ListView<T> arg0) {
      super(arg0);
    }

    public void refresh() {
      super.flow.rebuildCells();
    }

    @SuppressWarnings("unchecked")
    static <T> UpdateableListViewSkin<T> cast(Object obj) {
      return (UpdateableListViewSkin<T>) obj;
    }

  }

//  @Entity(name = "ru.inversion.fx.jinvtblexport.controller.ViewExportController.ExportConfigSettings")
//  public static class ExportConfigSettings {
//
//    /* ********************************* Properties (Getters/Setters) ******************************************/
//    @Id
//    @Column(name = "pageOrientation")
//    @Preference(dbName = "FORE_EXPORT_TABLE.PAGE_ORIENTATION")
//    public String getPageOrientation() {return pageOrientation;}
//
//    public void setPageOrientation(String pageOrientation) {
//      this.pageOrientation = pageOrientation;
//    }
//
//    @Column(name = "exportFormat")
//    @Preference(dbName = "FORE_EXPORT_TABLE.EXPORT_FORMAT")
//    public String getExportFormat() {return exportFormat;}
//
//    public void setExportFormat(String exportFormat) {
//      this.exportFormat = exportFormat;
//    }
//
//    @Column(name = "columnMode")
//    @Preference(dbName = "FORE_EXPORT_TABLE.COLUMN_MODE")
//    public String getColumnMode() {return columnMode;}
//
//    public void setColumnMode(String columnMode) {
//      this.columnMode = columnMode;
//    }
//
//    @Column(name = "pageSize")
//    @Preference(dbName = "FORE_EXPORT_TABLE.PAGE_SIZE")
//    public String getPageSize() {return pageSize;}
//
//    public void setPageSize(String pageSize) {
//      this.pageSize = pageSize;
//    }
//
//    @Column(name = "recordSource")
//    @Preference(dbName = "FORE_EXPORT_TABLE.RECORD_SOURCE")
//    public String getRecordSource() {return recordSource;}
//
//    public void setRecordSource(String recordSource) {
//      this.recordSource = recordSource;
//    }
//
//    @Column(name = "openAfterExport")
//    @Preference(dbName = "FORE_EXPORT_TABLE.OPEN_AFTER_EXPORT")
//    public String getOpenAfterExport() {return openAfterExport;}
//
//    public void setOpenAfterExport(String openAfterExport) {
//      this.openAfterExport = openAfterExport;
//    }
//
//    /* ********************************* End  (Properties Getters/Setters) *************************************/
//    private void abeginPropsMarker() {}
//
//    String pageOrientation;
//    String exportFormat;
//    String columnMode;
//    String pageSize;
//    String recordSource;
//    String openAfterExport;
//  }

  /* ********************************* Properties (Getters/Setters) ******************************************/
  /* ********************************* End  (Properties Getters/Setters) *************************************/
  private void abeginPropsMarker() {}

//  private ExportConfigSettings ecs = new ExportConfigSettings();


}

