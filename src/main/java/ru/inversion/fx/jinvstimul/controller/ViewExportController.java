/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvstimul.controller;

import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.jinvstimul.Export;
import ru.inversion.fx.jinvstimul.ExportSettings;
import ru.inversion.fx.jinvstimul.property.ExportFormat;
import ru.inversion.fx.jinvstimul.property.SizeExportedData;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * FXML Controller class
 *
 * @author polyatykina
 */
public class ViewExportController extends JInvFXFormController {

    @FXML
    private JInvTextField fileExp;
    @FXML
    private JInvButton dlgOpenFileChoicer;
    @FXML
    private JInvCheckBox ifOpenAfterExport;
    @FXML
    private RadioButton rbExcel;
    @FXML
    private RadioButton rbRtf;
    @FXML
    private RadioButton rbHtml;
    @FXML
    private RadioButton rbPdf;
    @FXML
    private RadioButton rbDocx;
    @FXML
    private RadioButton rbTxt;
    @FXML
    private RadioButton rbJpeg;
    @FXML
    private RadioButton rbSylk;
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


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("ru.inversion.fx.jinvstimul.ViewExportController");
    private ToggleGroup columnModeGroup;
    private ToggleGroup exportFormatGroup;
//    private ToggleGroup markModeGroup;



    @Override
    public void initialize(URL url, ResourceBundle rb)
    {
//        logger.trace( "initialize");

        Tooltip ttExel = new Tooltip();
        ttExel.setText("Excel");
        rbExcel.setTooltip(ttExel);

        Tooltip ttRtf = new Tooltip();
        ttRtf.setText("Rtf");
        rbRtf.setTooltip(ttRtf);

        Tooltip ttHtml = new Tooltip();
        ttHtml.setText("Html");
        rbHtml.setTooltip(ttHtml);

        Tooltip ttPdf = new Tooltip();
        ttPdf.setText("Pdf");
        rbPdf.setTooltip(ttPdf);

        Tooltip ttDocx = new Tooltip();
        ttDocx.setText("Docx");
        rbDocx.setTooltip(ttDocx);

        Tooltip ttTxt = new Tooltip();
        ttTxt.setText("Txt");
        rbTxt.setTooltip(ttTxt);

        Tooltip ttJpeg = new Tooltip();
        ttJpeg.setText("Jpeg");
        rbJpeg.setTooltip(ttJpeg);

        Tooltip ttSylk = new Tooltip();
        ttSylk.setText("Sylk");
        rbSylk.setTooltip(ttSylk);

        exportFormatGroup = new ToggleGroup();
        Stream.of(rbExcel, rbRtf, rbHtml, rbPdf, rbDocx, rbTxt, rbJpeg, rbSylk).forEach( ef -> ef.setToggleGroup( exportFormatGroup ) );

        columnModeGroup = new ToggleGroup();
        Stream.of( IF_VISIBLE_COLUMN, IF_ALL_COLUMN, IF_ALL_DATA ).forEach( em -> em.setToggleGroup( columnModeGroup ) );

//        markModeGroup = new ToggleGroup();
//        Stream.of( IF_MARKED_DATA, IF_ALL_DATA ).forEach( em -> em.setToggleGroup( markModeGroup ) );

        rbExcel.setSelected(true);
        ifOpenAfterExport.setSelected( true );

        fileExport();

        exportFormatGroup.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) -> {
            if ( exportFormatGroup.getSelectedToggle() != null) {
                fileExport();
            }
        });

        dlgOK.setOnAction((ActionEvent event) -> { export(); });
        dlgCancel.setOnAction((ActionEvent event) -> { Close(); });
        dlgOpenFileChoicer.setOnAction((ActionEvent event) -> { fileChoose(); });

        if ( rb.containsKey( "DIALOG.TITLE" ) && rb.getString("DIALOG.TITLE") != null && !rb.getString ("DIALOG" +
                ".TITLE").isEmpty() ){
            setTitle( rb.getString("DIALOG.TITLE"));
        }

    }

    @Override
    protected void init() throws Exception
    {
        logger.trace("init");

        if ( getViewContext().getStage().isMaximized() )
            getViewContext().getStage().setMaximized( false );

        if ( getViewContext().getStage().isResizable() )
            getViewContext().getStage().setResizable( false );

        final Map<Integer,Boolean> mapAct = new TreeMap<>();

        mapAct.put( 3950, true );
        mapAct.put( 3951, true );
        mapAct.put( 3952, true );

        JInvSecurityService.isCanAccessList( getTaskContext(), mapAct );

        IF_VISIBLE_COLUMN.setDisable( !mapAct.get(3950) );
        IF_ALL_COLUMN.setDisable( !mapAct.get(3951) );
        IF_ALL_DATA.setDisable( !mapAct.get(3952) );

        Stream.of( IF_VISIBLE_COLUMN, IF_ALL_COLUMN, IF_ALL_DATA ).filter( em -> !em.isDisabled() ).findFirst().map(em->{em.setSelected(true);return em;});

    }

    private void export()
    {
        new JInvParallelAction((ActionEvent event) -> {
            if ( this.getInitProperties().get(ExportSettings.KEY_SOURCE) instanceof JInvTable ){
                Export.getReport( (JInvTable)this.getInitProperties().get(ExportSettings.KEY_SOURCE)
                                , getExportFormat()
                                , fileExp.getText()
                                , this.ifOpenAfterExport.isSelected()
                                , getSizeExportedDate()
                );
            }

        }, ((JInvTable)this.getInitProperties().get(ExportSettings.KEY_SOURCE)).getController()).handle();
        Close();
    }

    private void Close()
    {
        super.close();
    }

    static final public ResourceBundle g_bundle = ResourceBundle.getBundle("ru/inversion/fx/jinvstimul/res/ViewExport");

    private void fileExport()
    {
        if ( fileExp.getText() == null || fileExp.getText().isEmpty()  )
            fileExp.setText(ExportSettings.getPathFileExport(getExportFormat()));

        else
        {
            fileExp.setText(
                    fileExp.getText().replace(
                            fileExp.getText().substring(
                                    fileExp.getText().lastIndexOf(".") ), ExportSettings.getExportExtension( getExportFormat() )
                    ));
        }
    }

    private ExportFormat getExportFormat()
    {
        if ( rbExcel.isSelected() )
            return ExportFormat.Excel2007;
        else if ( rbRtf.isSelected() )
            return ExportFormat.Rtf;
        else if ( rbHtml.isSelected() )
            return ExportFormat.Html;
        else if ( rbPdf.isSelected() )
            return ExportFormat.Pdf;
        else if ( rbDocx.isSelected() )
            return ExportFormat.Word2007;
        else if ( rbTxt.isSelected() )
            return ExportFormat.Text;
        else if ( rbJpeg.isSelected() )
            return ExportFormat.ImageJpeg;
        else if ( rbSylk.isSelected() )
            return ExportFormat.Sylk;

        return null;
    }

    private SizeExportedData getSizeExportedDate(){
        if(IF_VISIBLE_COLUMN.isSelected())
            return SizeExportedData.DISPLAYED_TABLE;
        if(IF_ALL_COLUMN.isSelected())
            return SizeExportedData.FULL_TABLE;
        //todo: tell the difference <->
        if(IF_ALL_DATA.isSelected())
            return SizeExportedData.FULL_DATA;

        return SizeExportedData.DISPLAYED_TABLE;
    }

    private void fileChoose()
    {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle( g_bundle.getString("FILE_CHOOSER.TITLE") );

        if ( getExportFormat() != null )
            fileChooser.getExtensionFilters().add( new ExtensionFilter(ExportSettings.getExportExtension( getExportFormat() ), String.format("*%s", ExportSettings.getExportExtension( getExportFormat() ) ) ) );

        fileChooser.getExtensionFilters().addAll( new ExtensionFilter("All Files", "*.*") );

        if ( fileExp.getText() != null && !fileExp.getText().isEmpty() )
            fileChooser.setInitialDirectory( new File( fileExp.getText() ).getParentFile() );

        File selectedFile = fileChooser.showOpenDialog( getViewContext().getStage() );

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

}
