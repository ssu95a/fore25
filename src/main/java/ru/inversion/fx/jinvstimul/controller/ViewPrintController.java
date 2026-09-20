/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvstimul.controller;

import javafx.beans.property.Property;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.StageStyle;
import ru.inversion.fx.form.JInvFXBrowserController;
import ru.inversion.fx.form.ResultForm;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvBigDecimalSpinner;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvLabel;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.jinvstimul.Export;
import ru.inversion.fx.jinvstimul.ExportSettings;
import ru.inversion.fx.jinvstimul.property.ExportFormat;
import ru.inversion.fx.jinvstimul.property.SizeExportedData;
import ru.inversion.tc.TaskContext;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
/**
 * FXML Controller class
 *
 * @author polyatykina
 */
public class ViewPrintController extends JInvFXBrowserController {

    @FXML
    private JInvLabel titleReport;

    @FXML
    private JInvBigDecimalSpinner copies;

    @FXML
    private JInvButton btPRINT;

    @FXML
    private JInvButton btVIEW;

    @FXML
    private JInvButton btFILE;

    @FXML
    private JInvButton btCLOSE;
    
    static final public ResourceBundle g_bundle = ResourceBundle.getBundle("ru/inversion/fx/jinvstimul/res/ViewPrint");
    
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        btCLOSE.setOnAction((ActionEvent event) -> { Close(); });
        btFILE.setOnAction((ActionEvent event) -> { toFile(); });
        btVIEW.setOnAction((ActionEvent event) -> { view(); });
        btPRINT.setOnAction((ActionEvent event) -> { print(); });
       
        copies.setText( "1" );
        copies.setMinValue( new BigDecimal( 1 )  );
        copies.setMaxValue( new BigDecimal( 1000 ) );
        copies.setStepwidth( new BigDecimal( 1 ) );
        
        if ( rb.containsKey( "DIALOG.TITLE" ) && rb.getString("DIALOG.TITLE") != null && !rb.getString ("DIALOG.TITLE").isEmpty() )
            setTitle( rb.getString("DIALOG.TITLE"));
        
    } 
    
    private void Close()
    {
        super.close();
    }
    
    private void print()
    {        
        if ( this.getInitProperties().get(ExportSettings.KEY_SOURCE) instanceof JInvTable );
            Export.printTable( ( (JInvTable)this.getInitProperties().get(ExportSettings.KEY_SOURCE) ), Integer.valueOf( copies.getText() ) );
             
        Close();
               
    }
    
    private void view()
    {
        if ( this.getInitProperties().get(ExportSettings.KEY_SOURCE) instanceof JInvTable );
        {
            ExportSettings exportSettings = new ExportSettings();
            exportSettings.setIfOpenAfterExport(true);
            Export.getTableReport( ( (JInvTable)this.getInitProperties().get(ExportSettings.KEY_SOURCE) ), ExportFormat.Html, ExportSettings.getExpTempDir(), exportSettings, SizeExportedData.DISPLAYED_TABLE );
        }
        
        Close();
    }
    
    private void toFile()
    {        
        Export.exportDialog( ( (JInvTable)this.getInitProperties().get(ExportSettings.KEY_SOURCE) ), this.getTaskContext(), this.getViewContext() );
        
        Close();
    }
    
    @Override
    protected void init() throws Exception
    {        
        if ( this.getInitProperties().containsKey(ExportSettings.KEY_TITLE )  )
            titleReport.setText((String) this.getInitProperties().get(ExportSettings.KEY_TITLE ));
        
        if ( getViewContext().getWindow() == null )
        {
            if ( getViewContext().getStage().isMaximized() )
                getViewContext().getStage().setMaximized( false );
            
            if ( getViewContext().getStage().isResizable() )
                getViewContext().getStage().setResizable( false );
        }
    }

    @Override
    public void initFormController(TaskContext tc, ViewContext vc, FormModeEnum dialogMode, Object dataObject, Property<FormReturnEnum> returnProperty, Map<String, Object> properties, Consumer<ResultForm<Object>> clb, ViewContext parentViewContext) {
    //public void initFormController(TaskContext tc, ViewContext vc, FormModeEnum dialogMode, Object dataObject, Property<FormReturnEnum> returnProperty, Map<String, Object> initProperties, BiConsumer<FormReturnEnum, JInvFXFormController> clb, ViewContext parentViewContext) {
        super.initFormController(tc, vc, dialogMode, dataObject, returnProperty, initProperties, clb, parentViewContext); //To change body of generated methods, choose Tools | Templates.
        if ( vc.getStage() != null )
            vc.getStage().initStyle(StageStyle.UTILITY);
        
    }
//    @Override
//    public void initBrowserController(TaskContext tc, ViewContext vc, Map<String, Object> initProperties, BiConsumer<JInvFXDialogController.DialogReturnEnum, JInvFXFormController> clb) {
//        super.initBrowserController(tc, vc, initProperties, clb); //To change body of generated methods, choose Tools | Templates.
//        if ( vc.getStage() != null )
//            vc.getStage().initStyle(StageStyle.UTILITY);
//    }
    
//    @Override отвалилось при обновлении ядра
//    public void initBrowserController(TaskContext tc, ViewContext vc, Map initProperties, Consumer<JInvFXBrowserController> clb, boolean modal)
//    {
//        super.initBrowserController(tc, vc, initProperties, clb, modal);
//        
//        if ( vc.getStage() != null )
//            vc.getStage().initStyle(StageStyle.UTILITY);
//    }
    
        
}
