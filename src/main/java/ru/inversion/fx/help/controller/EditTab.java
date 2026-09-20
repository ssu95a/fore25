package ru.inversion.fx.help.controller;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.HTMLEditor;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.DataSetNavigationEvent;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.frame.menu.PropertyItemValue;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.progress.ProgressCallback;
import ru.inversion.fx.form.controls.progress.ProgressTaskExecutor;
import ru.inversion.fx.help.dao.HelpDao;
import ru.inversion.fx.help.dao.impl.HelpDaoImpl;
import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.fx.help.store.AdapterHelp;
import ru.inversion.fx.help.store.HelpReporter;
import ru.inversion.fx.help.store.IStoreHelp;
import ru.inversion.fx.help.store.StoreHelpImpl;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.*;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.*;
import static ru.inversion.fx.help.controller.HelpController.HELP_COLOR_TINT;

/**
 Внутренний класс. Реализация Tab - редактирования справочной информации. Реализует проверку прав на редактирование.
 Экспорт/Импорт в XML
 @author perov
 @version 1.0.0 */
public class EditTab extends AbstractTab {
    public static final int ACCESS_EDIT = 3454;
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );
    private static final String PROPERTY_FX_HELP_IMPORT_PATH = "PROPERTY_FX_HELP_IMPORT_PATH";
    private static final String PROPERTY_FX_HELP_EXPORT_PATH = "PROPERTY_FX_HELP_EXPORT_PATH";
    private final TaskContext tc;
    private final XXIDataSet<PHelp> dsHelp = new XXIDataSet<>();
    private final String defaultFilename;
    private final String homeDir;
    private String formName;
    private JInvTable<PHelp> tblList;
    private JInvTextField edForm;
    private HtmlEditorToolBar edHTML;
    //    private JInvTextArea taHtml;
    private HelpDao helpDao;
    private SplitPane sp;
    private VBox left;
    private JInvToolBar tb;
    private ButtonBase btnIns;
    private Button btnDel;
    private Button btnUpd;
    private Button btnExp;
    private Button btnImp;
    private JInvTableColumn descColumn;
    private JInvTableColumn verColumn;
    private SplitPane right;
    private HTMLEditor edHtmlWrap;
    private VBox vbEdit;
    private JInvToolBar tbEdit;
    private Button btnReload;
    private JInvTextArea taHtml;
    private ProgressTaskExecutor<Void, Void> progressTaskExecutor;
    private JInvComboBox<Locale,String> cbCurrentLocale;

    //    private boolean firstExecute = true;
    EditTab( String title, JInvFXFormController controller, TaskContext tc, ResourceBundle bundle ) {
        super( title, controller, bundle );
        this.tc = tc;
        this.defaultFilename = "help.xml";
        this.homeDir = System.getProperty( "user.home" );
        init();
        initWebViewDisabler( (WebView) ((HTMLEditor) edHTML).lookup("WebView") );
    }

    @Override
    public final void init() {
        try {
            initComboBox();
            initComponent();
            initDS();
            initListener();
            helpDao = new HelpDaoImpl();
            progressTaskExecutor = new ProgressTaskExecutor<>();
        } catch ( Exception ex ) {
            JInvErrorService.handleException( null, ex );
        }
    }

    private void initComboBox() {
        cbCurrentLocale = new JInvComboBox<>();
        cbCurrentLocale.setNullable(false);

        StringConverter<Locale> stringConverter = new StringConverter< Locale >() {
            @Override
            public String toString( Locale l ) { return String.format("%s (%s)",l.getDisplayName(), l.toString());}
            @Override
            public Locale fromString( String string ) { return null; }
        };
        cbCurrentLocale.setConverter(stringConverter);

        ObservableList<Locale> locales = cbCurrentLocale.getItems();
        locales.addAll(PropertyItemValue.g_locales.get());
//        currentLocale.setConverter();
        Locale locale = BaseApp.APP().getLocale();
        logger.info("current locale: {}", locale);
        locales.remove(locale);
        locales.add(0,locale);

        cbCurrentLocale.getSelectionModel().select(locale);
        cbCurrentLocale.getSelectionModel().selectedItemProperty().addListener((v,o,n)->{
            if (o != null && o != n){
                logger.info("locale {} -> {}", o, n);
                doSaveChangeBeforeChangeRow();
                refreshDS();
            }
        });
    }

    @Override
    public void lossSelect() {
        doSaveChangeBeforeChangeRow();
    }

    /**
     Сохраняем изменения после смены строки или вкладки
     */
    void doSaveChangeBeforeChangeRow() {
        if ( currentDsRowTextNotNull() && textChanged() ) {
            if ( Alerts.yesNo( this.getController(), getBundle().getString( "SAVE_CHANGE" ) ) ) {
                doSave();
            }
            tryRefreshSelectionAfterModal();
        }
    }

    private boolean textChanged() {
        return !dsHelp.getCurrentRow().getHTML_TEXT().equals( taHtml.getText() );
    }

    private boolean currentDsRowTextNotNull() {
        return dsHelp.getCurrentRow() != null && dsHelp.getCurrentRow().getHTML_TEXT() != null;
    }

    private void tryRefreshSelectionAfterModal() {
        final PHelp selectedItem = tblList.getSelectionModel().getSelectedItem();
        if ( selectedItem != null ) {
            dsHelp.setCurrentRow( selectedItem );
            refreshHtml( dsHelp.getCurrentRow().getHTML_TEXT() );
        }
    }

    /**
     Инициализируем компоненты
     */
    private void initComponent() {
        //left
        SplitPane sp = new SplitPane();
        sp.setOrientation( Orientation.HORIZONTAL );
        VBox left = new VBox();
        initLeftToolbar( left );
        initTable( left );
        initInfoParameters( left );
        sp.getItems().add( left );
        //right
        SplitPane right = new SplitPane();
        right.setOrientation( Orientation.VERTICAL );
        //HTMLEditor
        initHtmlEditor( right );
        //TextArea + toolbar
        initTextAreaAndRightToolbar( right );
        sp.getItems().add( right );
        this.setContent( sp );
    }

    private void initLeftToolbar( VBox left ) {
        tb = new JInvToolBar();
        btnIns = ActionFactory.createButton( ActionFactory.ActionTypeEnum.CREATE, ( a ) -> {
            doEditList( VM_INS, false );
        } );
        btnDel = ActionFactory.createButton( ActionFactory.ActionTypeEnum.DELETE, ( a ) -> {
            doEditList( VM_DEL, false );
        } );
        btnUpd = ActionFactory.createButton( ActionFactory.ActionTypeEnum.UPDATE, ( a ) -> {
            doEditList( VM_EDIT, false );
        } );
        btnExp = ActionFactory.createButton( ActionFactory.ActionTypeEnum.EXPORT, ( a ) -> {
            doExport();
        }/*, getBundle().getString("EXPORT_TO_XML")*/ );
        btnImp = ActionFactory.createButton( ActionFactory.ActionTypeEnum.IMPORT, ( a ) -> {
            try {
                doImport();
            } catch ( SQLException | IOException ex ) {
                JInvErrorService.handleException( null, ex );
            }
        }/*, getBundle().getString("IMPORT_TO_XML")*/ );
        tb.getItems().addAll( btnIns, btnUpd, btnDel, new Separator( Orientation.VERTICAL ),
                btnExp, btnImp, new Separator( Orientation.VERTICAL ),
                cbCurrentLocale);
        left.getChildren().addAll( tb );
    }

    /**
     экспорт в xml
     */
    private void doExport() {
//        new FXFormLauncher<>( tc, getViewContext(), "ru/inversion/fx/help/fxml/ExportHelp.fxml" ).dataObject( new PHelp() )
//                .bundle( getBundle() )
//                .modal( true )
//                .dialogMode( AbstractBaseController.FormModeEnum.VM_NONE )
//                .show();
        FileChooser fileChooser = new FileChooser();
        File selectedFile = null;
        fileChooser.setInitialDirectory( getPreviousPath().toFile() );
        fileChooser.setInitialFileName( defaultFilename );
        fileChooser.getExtensionFilters()
                .addAll( new ExtensionFilter( "XML Files", "*.xml" ), new ExtensionFilter( "All Files", "*.*" ) );
        fileChooser.setTitle( getBundle().getString( "EXPORT_TO_XML" ) );
        selectedFile = fileChooser.showSaveDialog( getViewContext().getStage() );
        if ( selectedFile == null || !progressTaskExecutor.isDone() ) {
            return;
        }
        final File finalSelectedFile = selectedFile;
        progressTaskExecutor.allowCancel( true )
                .callback( saveExport( selectedFile ) )
                .stage( getViewContext().getStageOrPrimaryStage() )
                .resultReceiver( aVoid -> {
                    // Открыть файл после экспорта
                    try {
                        Desktop.getDesktop().open( finalSelectedFile );
                    } catch ( IOException ignored ) {
                    }
                } )
                .execute();
        savePreviousPath( selectedFile.toString() );
    }

    private ProgressCallback<Void, Void> saveExport(File selectedFile ) {
        return ( progress, param ) -> {
            try {
                List<AdapterHelp> list = new ArrayList<>();
                IStoreHelp save = new StoreHelpImpl();
                if ( dsHelp.hasMarkedRows() ){
                    Iterable<PHelp> markedProducts = U.iterable( dsHelp.createMarkedRSIterator( true ) );
                    int size = dsHelp.getLoadedRowCount();
                    int i = 0;
                    progress.begin( i, size, null );
                    for ( PHelp pHelp : markedProducts ) {
                        i++;
                        list.add( new AdapterHelp(pHelp) );
                        progress.process( i, size, String.format( getBundle().getString( "text.export.progressText" ), i, size ) );
                    }
                } else {
                    //Если никакие не выделены, выгружаем только текущий
                    logger.info( "No help rows explicitly marked, exporting current row only" );
                    final PHelp currentRow = dsHelp.getCurrentRow();
                    if ( currentRow != null ){
                        list.add( new AdapterHelp( currentRow ) );
                    }
                }
                save.saveHelpToFile( selectedFile, list );
                progress.end( "ok" );
                return null;
            } catch ( Throwable th ) {
                throw new RuntimeException( th );
            }
        };
    }

    private static void savePreviousPath( String path ) {
        BaseApp.APP().getProperties( PropertiesTypeEnum.LOCAL_USER ).
                setProperty( PROPERTY_FX_HELP_EXPORT_PATH, Paths.get( path ).getParent().toString() );
    }

    private Path getPreviousPath() {
        // Восстанавливаем сохраненный путь из настроек
        String exportPath = BaseApp.APP().
                getProperties( PropertiesTypeEnum.LOCAL_USER ).getStringProperty( PROPERTY_FX_HELP_EXPORT_PATH, null );
        if ( S.isNotNullOrEmpty( exportPath ) )
        {
            final Path path = Paths.get(exportPath);

            if( path != null && Files.exists(path) && Files.isDirectory(path) )
                return path;
        }
        return Paths.get( homeDir );
    }

    /**
     импорт из xml
     */
    private void doImport() throws SQLException, IOException {
        List<File> selectedFiles = selectFilesForImport();
        if ( selectedFiles != null && !selectedFiles.isEmpty() ) {
            IStoreHelp loader = new StoreHelpImpl();
            HelpReporter reporter = null;
            int countErrorFile = 0;
            for ( File selectedFile : selectedFiles ) {
                StringBuilder errorProtocol = new StringBuilder();
                List<AdapterHelp> list = loader.loadHelpFromFile( selectedFile, errorProtocol );
                reporter = saveListHelpToDbAndWriteProtocol( list, errorProtocol, selectedFile );
                if ( errorProtocol.length() != 0 ) {
                    countErrorFile++;
                }
            }
            // Сохраняем каталог выбранного файла в свойства на локальной машине
            BaseApp.APP()
                    .getProperties( PropertiesTypeEnum.LOCAL_USER )
                    .setProperty( PROPERTY_FX_HELP_IMPORT_PATH, selectedFiles.get( 0 )
                            .toPath()
                            .getParent()
                            .toString() );
            if ( selectedFiles.size() == 1 && reporter != null ) {
                showImportFinalAlertForSingleImport( reporter );
            } else {
                showImportFinalAlertForMultipleImport( selectedFiles.size(), countErrorFile );
            }
            refreshDS();
        }
    }

    private List<File> selectFilesForImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle( getBundle().getString( "IMPORT_TO_XML" ) );
        fileChooser.getExtensionFilters()
                .addAll( new ExtensionFilter( "XML Files", "*.xml" ), new ExtensionFilter( "All Files", "*.*" ) );
        // Восстанавливаем сохраненный путь из настроек
        String importPath = BaseApp.APP()
                .getProperties( PropertiesTypeEnum.LOCAL_USER )
                .getStringProperty( PROPERTY_FX_HELP_IMPORT_PATH, null );
        if ( S.isNotNullOrEmpty( importPath ) ) {
            File directory = new File( importPath );
            if ( directory.exists() ) {
                fileChooser.setInitialDirectory( directory );
            }
        }
        return fileChooser.showOpenMultipleDialog( getViewContext().getStage() );
    }

    private HelpReporter saveListHelpToDbAndWriteProtocol( List<AdapterHelp> list, StringBuilder errorProtocol, File helpFile ) throws SQLException, IOException {
//        HelpControlWork controlWork;
        Connection con = tc.getConnection();
        boolean flagCommit = false;
        int countDocuments = 0;
        int countDoneDocuments = 0;
        LocalTime startProcessingTime = LocalTime.now();
        for ( AdapterHelp adapterHelp : list ) {
            try {
                countDocuments++;
                if ( adapterHelp.getpHelp() != null ) {
                    helpDao.insertHelpForm( con, adapterHelp.getpHelp() );
                    flagCommit = true;
                    countDoneDocuments++;
                } else {
                    errorProtocol.append( "Пустой элемент помощи\n" );
                }
//                if (adapterHelp.getComponents() != null) {
//                    for (PHelpBundle pHelpBundle : adapterHelp.getComponents()) {
//
//                        controlWork = new HelpControlWork(AbstractWorkBase.UPDATE, pHelpBundle);
//                        controlWork.execute(con);
//                        flagCommit = true;
//                    }
//                }
            } catch ( SQLException ex ) {
                flagCommit = false;
                errorProtocol.append( "Ошибка при записи в базу данных: " ).append( ex.getMessage() ).append( "\n" );
                if ( adapterHelp.getpHelp() != null ) {
                    errorProtocol.append( adapterHelp.getpHelp().getHTML_TEXT() ).append( "\n" );
                }
            }
        }
        if ( flagCommit ) {
            con.commit();
        }
        HelpReporter reporter = new HelpReporter();
        reporter.setHelpFile( helpFile );
        reporter.setCountDocuments( countDocuments );
        reporter.setCountDoneDocuments( countDoneDocuments );
        reporter.setStartProcessing( startProcessingTime );
        reporter.setErrorProtocol( errorProtocol );
        String url = con.getMetaData().getURL();
        String[] urlParts = url.split( "@" );
        if ( urlParts.length == 2 ) {
            reporter.setUserName( urlParts[1] );
        }
        reporter.createAndWriteReport();
        return reporter;
    }

    private void showImportFinalAlertForSingleImport( HelpReporter reporter ) {
        final String sb = String.format( getBundle().getString( "IMPORT_ALL_PROCESSED_COUNT" ), reporter.getCountDocuments() ) +
                "\n" +
                String.format( getBundle().getString( "IMPORT_SUCCESSED_COUNT" ), reporter.getCountDoneDocuments() ) +
                "\n" +
                String.format( getBundle().getString( "IMPORT_REFUSED_COUNT" ), reporter.getCountDocuments() -
                        reporter.getCountDoneDocuments() ) +
                "\n" +
                String.format( getBundle().getString( "IMPORT_TIME" ), TypeConverter.convertToString( reporter.getTotalTimeProcessing(), null ) ) +
                "\n";
        Alerts.info( getViewContext(), "", getBundle().getString( "IMPORT_FINISHED" ), sb );
    }

    private void showImportFinalAlertForMultipleImport( int countProcessedFiles, int countErrorFiles ) {
        final String sb = String.format( getBundle().getString( "IMPORT_ALL_PROCESSED_COUNT_FILES" ), countProcessedFiles ) +
                "\n" +
                String.format( getBundle().getString( "IMPORT_REFUSED_COUNT_FILES" ), countErrorFiles ) +
                "\n";
        Alerts.info( getViewContext(), "", getBundle().getString( "IMPORT_FINISHED" ), sb );
    }

    private void refreshDS() {
        try {
            dsHelp.setParameter( 0, formName );
            dsHelp.setParameter( 1, cbCurrentLocale.getValue().toString() );
            dsHelp.setParameter( 2, formName );
            dsHelp.setParameter( 3, cbCurrentLocale.getValue().toString() );
            dsHelp.executeQuery( false );
            askAddNewHelp();
        } catch ( DataSetException ex ) {
            JInvErrorService.handleException( null, ex );
        }
    }

    private void askAddNewHelp() {
        if ( !helpExistsInDs() ) {
            if ( Alerts.yesNo( this.getController(), getBundle().getString( "HELP_NOT_SET" ) ) ) {
                Platform.runLater( () -> {
                    doEditList( VM_INS, true );
//                    refreshDS();
                } );
            }
        }
    }

    private boolean helpExistsInDs() {
        for ( final PHelp pHelp : dsHelp.getRows() ) {
            if ( pHelp.getFORM().equals( formName ) ) {
                dsHelp.setCurrentRow( pHelp );
                return true;
            }
        }
        return false;
    }

    private void doEditList( AbstractBaseController.FormModeEnum mode, boolean firstExecute ) {
        PHelp pHelp = new PHelp();
        if ( mode == JInvFXDialogController.FormModeEnum.VM_DEL ||
                mode == JInvFXDialogController.FormModeEnum.VM_EDIT ) {
            pHelp = dsHelp.getCurrentRow();
        }
        if ( pHelp != null ) {
            Map<String, Object> prp = new HashMap<>();
            if ( firstExecute && !formName.isEmpty() && mode.equals( JInvFXDialogController.FormModeEnum.VM_INS ) ) {
                pHelp.setFORM( formName );
                pHelp.setDESCR( formName );
                pHelp.setCLOCALE( cbCurrentLocale.getValue().toString() );
                prp.put( "BLOCK_FORM_NAME", true );
            } /*else if ( mode.equals( JInvFXDialogController.FormModeEnum.VM_EDIT ) ) {
//                prp.put("BLOCK_FORM_NAME", true);
            }*/
            final PHelp finalPojo = pHelp;
            new FXFormLauncher<PHelp>( tc, getViewContext(), "ru/inversion/fx/help/fxml/ManageHelp.fxml" ).dataObject( finalPojo )
                    .bundle( getBundle() )
                    .modal( true )
                    .dialogMode( mode )
                    .initProperties( prp )
                    .clb( ( AbstractBaseController.FormReturnEnum t, JInvFXFormController u ) -> {
                        if ( t == AbstractBaseController.FormReturnEnum.RET_OK ) {
                            switch ( u.getFormMode() ) {
                                case VM_INS:
                                    dsHelp.insertRow( finalPojo, IDataSet.InsertRowModeEnum.FIRST, true );
                                    break;
                                case VM_EDIT:
                                    dsHelp.updateCurrentRow( finalPojo );
                                    break;
                                case VM_DEL:
                                    dsHelp.removeCurrentRow();
                                    break;
                            }
                        }
                    } )
                    .show();
        }
    }

    private void initTable( VBox left ) {
        tblList = new JInvTable<>();
        tblList.setColumnResizePolicy( TableView.UNCONSTRAINED_RESIZE_POLICY );
        descColumn = new JInvTableColumn<>( getBundle().getString( "COLUMN_DESC" ) );
        descColumn.setFieldName( "DESCR" );
        descColumn.setPrefWidth( 100 );
        verColumn = new JInvTableColumn<>( getBundle().getString( "COLUMN_VER" ) );
        verColumn.setFieldName( "VER" );
        verColumn.setPrefWidth( 60 );
        //
        descColumn.prefWidthProperty()
                .bind( tblList.widthProperty().subtract( verColumn.widthProperty() ).subtract( 70 ) );
        tblList.getColumns().addAll( descColumn, verColumn );
        left.getChildren().addAll( tblList );
        VBox.setVgrow( tblList, Priority.ALWAYS );
    }

    private void initInfoParameters( VBox left ) {
        edForm = new JInvTextField();
        edForm.setEditable( false );
        left.getChildren().add( edForm );
    }

    private void initHtmlEditor( SplitPane right ) {
        edHTML = new HTMLEditorDecorator();
        edHTML.addItem( ActionFactory.createButton( ru.inversion.fx.form.ActionFactory.ActionTypeEnum.SAVE_FILE, ( a ) -> {
            doSave();
        } ) );
//        edHTML.addItem( ActionFactory.createButton( new IconDescriptorBuilder<>( FontAwesome.fa_link ).build(), ( ActionEvent a ) -> {
//            doAddLink();
//        }, getBundle().getString( "ADD_LINK" ) ) );
        edHTML.getSetTextProperty()
                .addListener( ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) -> {
                    if ( newValue ) {
                        taHtml.setText( edHTML.getText() );
                    }
                } );
        edHTML.getKeyPressedProperty()
                .addListener( ( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) -> {
                    if ( newValue ) {
                        taHtml.setText( edHTML.getText() );
                    }
                } );
        VBox htmlContainerVbox = new VBox();
        htmlContainerVbox.setBlendMode( BlendMode.MULTIPLY );
        htmlContainerVbox.setStyle( "-fx-background-color:" + HELP_COLOR_TINT + ";" );
        htmlContainerVbox.getChildren().add( (Node) edHTML );
        right.getItems().add( htmlContainerVbox );
    }

    /**
     сохраняем в бд
     */
    private void doSave() {
        if ( dsHelp.getCurrentRow() != null ) {
            try {
                pushTextToHtml();
                PHelp entity = dsHelp.getCurrentRow();
                entity.setHTML_TEXT( edHTML.getText() );
                helpDao.updateHelpForm( tc.getConnection(), entity, null );
                refreshHtml( edHTML.getText() );
            } catch ( SQLException ex ) {
                JInvErrorService.handleException( null, ex );
            }
        }
    }

    private void refreshHtml( final String referenceText ) {
        if ( referenceText != null ) {
            /*Platform.runLater( () -> */
            edHTML.setText( dsHelp.getCurrentRow().getHTML_TEXT() ) /*)*/;
        } else {
            /*Platform.runLater( () -> */
            edHTML.setText( "" ) /*)*/;
        }
        taHtml.setText( edHTML.getText() );
    }

    private void pushTextToHtml() {
        edHTML.setText( taHtml.getText() );
    }

//    private void doAddLink() {
//        new FXFormLauncher<>( tc, getViewContext(), "ru/inversion/fx/help/fxml/AddLinkPane.fxml" )
//                .dataObject( new PHelp() )
//                .bundle( getBundle() )
//                .modal( true )
//                .dialogMode( AbstractBaseController.FormModeEnum.VM_NONE )
//                .closeCallback( (c) -> {
//                    if ( !c.getFormReturn().equals( AbstractBaseController.FormReturnEnum.RET_OK ) ) return;
//                    WebView webView = (WebView) ((HTMLEditor) edHTML).lookup("WebView");
//                    String selection = (String) webView.getEngine().executeScript("window.getSelection().toString();");
//                    String hyperlink = "<a href=\"" + ( (PHelp) c.getController().getDataObject() ).getFORM() +
//                            "\" >" + selection + "</a>";
//                    webView.getEngine().executeScript(getInsertHtmlAtCursorJS(hyperlink));
//                } )
//                .show();
//    }

    private void initTextAreaAndRightToolbar( SplitPane right ) {
        VBox vbEdit = new VBox();
        JInvToolBar tbEdit = new JInvToolBar();
        // обновляем текст в htmledit из textarea
        ButtonBase btnReload = ActionFactory.createButton( ActionFactory.ActionTypeEnum.UP, ( a ) -> pushTextToHtml() );
        tbEdit.getItems().add( btnReload );
        taHtml = new JInvTextArea();
        taHtml.setWrapText( true );
        VBox.setVgrow( taHtml, Priority.ALWAYS );
        vbEdit.getChildren().addAll( tbEdit, taHtml );
        right.getItems().add( vbEdit );
    }

    private void initListener() {
        dsHelp.addNavigationListener( ( DataSetNavigationEvent<PHelp> e ) -> {
            if ( e != null && e.getNewRow() != null ) {
                Platform.runLater( () -> {
                    String text = e.getNewRow().getHTML_TEXT();
                    refreshHtml( text );
                } );
            }
        } );
        tblList.getSelectionModel().selectedItemProperty().addListener( ( observable, oldValue, newValue ) -> {
            if ( oldValue != null && dsHelp.getRows().contains( oldValue ) && !oldValue.equals( newValue ) ) {
                Platform.runLater( this::doSaveChangeBeforeChangeRow );
            }
        } );
    }

    private void initDS() throws Exception {
        dsHelp.setTaskContext( tc );
        dsHelp.setRowClass( PHelp.class );
        dsHelp.setSQL( "SELECT form, \n" +
                "descr, \n" +
                "ver, \n" +
                "CLOCALE, \n" +
                "html_text, 1 grpby \n" +
                "FROM JF_HELP WHERE form=? and CLOCALE=?\n" +
                "UNION ALL\n" +
                "SELECT form, \n" +
                "descr, \n" +
                "ver, \n" +
                "CLOCALE, \n" +
                "html_text, 2 grpby \n" +
                "FROM JF_HELP WHERE form!=? and CLOCALE=?\n" +
                "ORDER BY grpby" );
        DSFXAdapter<PHelp> bind = DSFXAdapter.bind( dsHelp, tblList, null, true );
        bind.bindControl( edForm, "FORM", PHelp::getFORM, 0 );
    }

    @Override
    public void draw( String formName ) {
        this.formName = formName;
        refreshDS();
        setTitle();
    }

    private void setTitle() {
        StringBuilder sb = new StringBuilder();
        sb.append( getBundle().getString( "HELP_TITLE_HOT_EDIT" ) );
        sb.append( ( (HelpController) getController() ).getConstantPartOfTitle() );
        getController().setTitle( sb.toString() );
    }

    private String getInsertHtmlAtCursorJS( String html ) {
        return "insertHtmlAtCursor('" +
                html +
                "');function insertHtmlAtCursor(html) {\n" +
                " var range, node;\n" +
                " if (window.getSelection && window.getSelection().getRangeAt) {\n" +
                " window.getSelection().deleteFromDocument();\n" +
                " range = window.getSelection().getRangeAt(0);\n" +
                " node = range.createContextualFragment(html);\n" +
                " range.insertNode(node);\n" +
                " } else if (document.selection && document.selection.createRange) {\n" +
                " document.selection.createRange().pasteHTML(html);\n" +
                " document.selection.clear(); }\n" +
                "}";
    }
}
