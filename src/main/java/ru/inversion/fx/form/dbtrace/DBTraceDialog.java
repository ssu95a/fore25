package ru.inversion.fx.form.dbtrace;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.inversion.db.DBUniqueResult;
import ru.inversion.db.trace.IDbTrace;
import ru.inversion.db.trace.OracleDbTrace;
import ru.inversion.db.trace.PostgresDbTrace;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.action.JInvParallelAction;
import ru.inversion.fx.form.action.decorator.ProgressFormStateDecorator;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvLabel;
import ru.inversion.fx.form.controls.JInvMenuButton;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.textarea.JInvSearchTextPane;
import ru.inversion.fx.form.controls.textarea.JInvStyleTextArea;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.tc.TCStorage;
import ru.inversion.tc.TaskContext;
import ru.inversion.tc.jdbc.event.EventType;
import ru.inversion.tc.jdbc.trace.JdbcTraceEvent;
import ru.inversion.tc.jdbc.trace.JdbcTraceListener;
import ru.inversion.tc.jdbc.trace.JdbcTraceType;
import ru.inversion.tc.jdbc.trace.JdbcTracer;
import ru.inversion.utils.AutoCloseableList;
import ru.inversion.utils.ConnectionStringFormatEnum;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import javax.persistence.NamedNativeQuery;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 *
 * @author ssu @
 */
public class DBTraceDialog extends GridPane implements IFormStateListener {

    static private String globalName;

    protected ObjectProperty<StateEnum>  activeState    = new SimpleObjectProperty<>(StateEnum.ACTIVE);
    private   ProgressFormStateDecorator stateDecorator;

    @Override
    public ObjectProperty<StateEnum> stateProperty() {
        return activeState;
    }

    protected StringProperty stateText = new SimpleStringProperty( S.EMPTY_STRING );

    @Override
    public StringProperty stateTextProperty() {
        return stateText;
    }

    /** */
    @NamedNativeQuery(name = "gn", query = "SELECT GLOBAL_NAME FROM GLOBAL_NAME")
    private class OraGlobalName extends DBUniqueResult<String> { }

    private final AutoCloseableList closeables =new AutoCloseableList(false);

    /**
      */
    private class DBTracePane extends GridPane implements JdbcTraceListener, AutoCloseable {

        final private JInvSearchTextPane searchTextPane = new JInvSearchTextPane();
        final private JInvStyleTextArea edOutput;
        final private JdbcTracer jdbcTracer;
        final private SimpleBooleanProperty enableProperty = new SimpleBooleanProperty(this, "enable");

        private IDbTrace dbTrace;

        //ora zone
        final private IntegerProperty state = new SimpleIntegerProperty(this, "state", -1);

        final private ButtonBase[] traceButtons = new ButtonBase[4];
        //

        private Path currentTraceFile = null;

        final private TaskContext taskContext;

        public DBTracePane(TaskContext tc) {

            this.taskContext = tc;

            jdbcTracer = this.taskContext.getJdbcTracer();

            enableProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if (newValue) {
                    jdbcTracer.addListener(this);
                } else {
                    jdbcTracer.removeListener(this);
                }
            });

            setVgap(5);

            initToolBar(this, viewContext);

            this.edOutput = searchTextPane.getTextArea();
            initTextArea(this);

            Platform.runLater(() -> enableProperty.set(true));
        }

        private String getGlobalName() {
            if (globalName == null) {
                OraGlobalName dbu = new OraGlobalName();
                globalName = dbu.execute(taskContext, true, (rs, rowNum) -> rs.getString(1));
            }
            return globalName;
        }

        /** */
        private void onChangeState(int state) {

            final boolean[] st = {false, false, false, false};//new boolean[traceButtons.length];

            switch (state) {
                case -1:
                    break;
                case 0:
                    st[0] = true;
                    break;
                case 1:
                    st[1] = true;
                    break;
                case 2:
                    st[0] = true;
                    st[2] = true;
                    break;
                case 3:
                case 4:
                    st[0] = true;
                    st[2] = true;
                    st[3] = true;
                    break;
            }

            for (int i = 0; i < traceButtons.length; i++) {
                if( traceButtons[i].disableProperty().get() == st[i] )
                    traceButtons[i].disableProperty().set(!st[i]);
            }
        }

        /** */
        private void initTextArea(GridPane grid) {
            Platform.runLater(() -> edOutput.setStyle("-fx-background-color:#FFFFE0"));
            edOutput.setEditable(false);
            edOutput.setWrapText(true);
            grid.add(searchTextPane, 0, 1, 1, 1);
            GridPane.setConstraints(searchTextPane, 0, 1, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);
            searchTextPane.getScrollPane().addEventFilter(ScrollEvent.ANY, event -> {
                resumeOrPauseScroll();
            });
            searchTextPane.getScrollPane().addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
                resumeOrPauseScroll();
            });
        }

        /**
         *
         */
        private void initToolBar(GridPane grid, ViewContext vc)
        {
            final ToolBar tb = new JInvToolBar();

            CheckBox ch = new CheckBox(fore.getString("VKLYUCHIT"));
            ch.selectedProperty().bindBidirectional(enableProperty);
            tb.getItems().addAll(ch, new Separator());

            for( JdbcTraceType e : JdbcTraceType.values())
            {
                if (e == JdbcTraceType.TECH)
                    continue;

                CheckBox ch1 = new CheckBox(e.toString());
                ch1.setSelected(jdbcTracer.isEnabled(e));
                ch1.selectedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    jdbcTracer.setEnabled(e, newValue);
                });

                tb.getItems().add(ch1);

            }//end for

            MenuButton jdbcEventsButton =
                    new MenuButton("JDBC events");

            for( EventType type : EventType.values() )
            {
                CheckMenuItem item = new CheckMenuItem(type.toString());

                item.setSelected( jdbcTracer.isJdbcEventEnabled(type) );

                item.selectedProperty().addListener( (observable, oldValue, newValue) -> jdbcTracer.setJdbcEventEnabled( type,newValue  ));

                jdbcEventsButton.getItems().add(item);
            }

            tb.getItems().addAll( new Separator(), jdbcEventsButton );

            ButtonBase btnRun;

            if( taskContext.isOracle() )
            {
                final JInvMenuButton buttonTraceRun = new JInvMenuButton();
                buttonTraceRun.getItems().addAll(
                        new MenuItem(" 1 - Basic"),
                        new MenuItem(" 4 - Bind variables"),
                        new MenuItem(" 8 - Wait events"),
                        new MenuItem("12 - Binds and Waits")
                );

                final String[] si = {"1", "4", "8", "12"};

                U.forEachWithIndex(buttonTraceRun.getItems(), (MenuItem mi, Integer i) -> {
                    mi.setId(si[i]);
                    mi.setOnAction((e) -> runTrace(Integer.parseInt(mi.getId())));
                });

                btnRun = buttonTraceRun;
            }
            else
            {
                btnRun = new JInvButton();
                btnRun.setOnAction(e -> runTrace(0));
            }

            ActionFactory.assignButtonStyleSilent(ActionFactory.ActionTypeEnum.RUN, btnRun);

            Button buttonTraceStop = ActionFactory.createButton(ActionFactory.ActionTypeEnum.STOP, (e) -> stopTrace());
            final Node stopBtnGraphic = buttonTraceStop.getGraphic();
            stopBtnGraphic.setStyle(stopBtnGraphic.getStyle() + ";" + "-fx-text-fill:red;");

            Button buttonTraceDown = ActionFactory.createButton(ActionFactory.ActionTypeEnum.DOWNLOAD, (e) -> downloadTrace());

            Button buttonTraceOpen =
                    ActionFactory.createButton(ActionFactory.ActionTypeEnum.EDIT_EXTERNAL,
                        this::viewTrace
                    );

            tb.getItems().addAll(
                    new Separator(),
                    ActionFactory.createButton(ActionFactory.ActionTypeEnum.CLEAR, (e) -> edOutput.clear()),
                    new Separator(),
                    ActionFactory.createButton(ActionFactory.ActionTypeEnum.SAVE_FILE, (e) -> saveToFile()),
                    new Separator(),
                    new JInvLabel("Trace:"),
                    btnRun,
                    buttonTraceStop,
                    buttonTraceDown,
                    buttonTraceOpen
            );

            if( taskContext.isOracle() )
            {
                tb.getItems().addAll( new Separator(),
                        ActionFactory.createButton (
                        FontAwesome.fa_folder_o,
                        (e) -> makeOracleDir(), "Create Directory B21$USER_DUMP_DEST_DIR"
                    )
                );
            }

            traceButtons[0] = btnRun;
            traceButtons[1] = buttonTraceStop;
            traceButtons[2] = buttonTraceDown;
            traceButtons[3] = buttonTraceOpen;

            dbTrace = this.taskContext.isOracle()
                    ? new OracleDbTrace(this.taskContext)
                    : new PostgresDbTrace(BaseApp.APP().getCommonTaskContext());

            dbTrace.addListener(new Consumer<IDbTrace.TraceEvent>() {
                @Override
                public void accept(IDbTrace.TraceEvent traceEvent) {
                    if (traceEvent.type() == IDbTrace.TraceEvent.Type.Trace_On)
                        onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO, "TRACE:>> STARTED" ));
                    else if (traceEvent.type() == IDbTrace.TraceEvent.Type.Trace_Off)
                        onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO, "TRACE:>> FINISHED"));
                    else if (traceEvent.type() == IDbTrace.TraceEvent.Type.Download_Begin)
                        onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO, "TRACE:>> Begin download to " + traceEvent.fileName() + " ..."));
                    else if (traceEvent.type() == IDbTrace.TraceEvent.Type.Download_End)
                        onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO,  "TRACE:>> Finish download " + traceEvent.fileName()));
                    else if (traceEvent.type() == IDbTrace.TraceEvent.Type.Set_FileName)
                        onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO, "TRACE:>> Download to: " + traceEvent.fileName()));
                }
            });

            state.addListener((observable, oldValue, newValue) -> onChangeState(newValue.intValue()));

            Platform.runLater(() -> state.set(0));

            /*
            {
                tb.getItems().addAll (
                    new Separator(),
                    ActionFactory.createButton(ActionFactory.ActionTypeEnum.CLEAR, (e) -> edOutput.clear()),
                    new Separator(),
                    ActionFactory.createButton(ActionFactory.ActionTypeEnum.SAVE_FILE, (e) -> saveToFile())
                );
            }
            */
            grid.add(tb,0,0,1,1);
        }

        /** */
        private void viewTrace(ActionEvent e) {
            if( taskContext.isOracle() )
                viewOraTrace();
            else
                viewPgTrace();
        }

        /** */
        private void handleException(Throwable th) {
            JInvErrorService.handleException( getScene().getWindow(), th);
        }

        /** */
        private void makeOracleDir( ) {
            try {
                dbTrace.makeRemoteDir();
                Alerts.info( getScene().getWindow(), "Directory created!" );
            }
            catch( Throwable th ) {
                handleException(th);
            }
        }

        /** */
        private void runTrace(int level) {

            try {

                dbTrace.on(level);

                currentTraceFile = null;

                state.set(1);

            } catch (Throwable th) {
                handleException(th);
            }
        }

        /** */
        private void stopTrace() {

            try {
                dbTrace.off();
                state.set( taskContext.isOracle() ? 2 : 3 );
            } catch (Throwable th) {
                handleException(th);
            }
        }

        /** */
        private void downloadTrace() {

            try {

                if( stateDecorator == null )
                    stateDecorator = new ProgressFormStateDecorator( DBTraceDialog.this, getViewContext());

                JInvParallelAction a = new JInvParallelAction (
                        (ActionEvent event) -> {

                            currentTraceFile = dbTrace.download();

                            Platform.runLater(
                                ()->{
                                    Alerts.info (
                                        DBTraceDialog.this.getScene().getWindow(),
                                        "Загрузка trace файла",
                                        "Выгружен trace файл. Файл выгрузки:", currentTraceFile.toString()
                                    );
                                    state.set(3);
                                }
                            );
                        },
                        DBTraceDialog.this
                );

                a.handle();

            } catch (Throwable th) {
                handleException(th);
            }
        }

        /** */
        private void viewPgTrace( ) {

            try {

                final String traceName = dbTrace.getTraceFileName();

                if( traceName == null )
                    throw new IllegalStateException(Tags.PRODUCT_LABEL + "Trace file is not downloaded");

                try {

                    //trace( QueryDBTraceEvent.info( this, "showPGTrace", traceName ) );
                    onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO, traceName ) );


                    BaseApp.APP().foreXXISupport().getFeature("showPGTrace", traceName );
                }
                catch( Throwable th ) {
                    throw new IllegalStateException(Tags.PRODUCT_LABEL + "Error on run '" + traceName + "'", th );
                }

            } catch ( Throwable th ) {
                handleException(th);
            }
        }


        /** */
        private void viewOraTrace( ) {

            try {

                if( currentTraceFile == null )
                    throw new IllegalStateException(Tags.PRODUCT_LABEL + "Trace file is not downloaded");

                if(!Files.exists(currentTraceFile) || !Files.isRegularFile(currentTraceFile) )
                    throw new FileNotFoundException( Tags.PRODUCT_LABEL + "File does not exist: " + currentTraceFile );

                String viewStatBat = BaseApp.APP().getProperties(PropertiesTypeEnum.DB_USER).getStringProperty("FX_TRACE_PROC");

                StringBuilder sb = new StringBuilder();
                sb.append( viewStatBat ).append(" ").append( currentTraceFile ).append(" ").append( getGlobalName() );

                String cmd = sb.toString();

                try {
                    //trace( QueryDBTraceEvent.info( this, "viewOracleTrace", cmd ) );

                    onJdbcTrace( JdbcTraceEvent.custom( this, JdbcTraceType.INFO, cmd ) );

                    Runtime.getRuntime().exec( cmd );
                }
                catch( Throwable th ) {
                    throw new IllegalStateException(Tags.PRODUCT_LABEL + "Error on run '" + sb.toString() + "'", th );
                }

            } catch ( Throwable th ) {
                handleException(th);
            }
        }

        /** */
        private void saveToFile() {
            try {
                File file = JInvFileChooser.showSaveDialog(getScene().getWindow(),
                        new File(System.getProperty("user.home")), "db_queries_tracer.txt", new FileChooser.ExtensionFilter[]{
                        new FileChooser.ExtensionFilter("Файл txt", "*.txt"),
                                new FileChooser.ExtensionFilter("Файл html", "*.html")});
                if (file != null) {
                    try (FileWriter fw = new FileWriter(file)) {
                        fw.write(edOutput.getText());
                    }
                }
            } catch (Throwable th) {
                JInvErrorService.handleException(getScene().getWindow(), th);
            }
        }

        private ScrollBar scrollBar;

        /** */
        @Override
        public void onJdbcTrace( JdbcTraceEvent event ) {

            final String text;

            {
                StringWriter sw = new StringWriter();
                event.print(sw);
                text = sw.toString();
            }

            if( S.isNotNullOrEmpty(text) )
                Platform.runLater(() -> edOutput.appendText(text) );
        }

        /** */
        private void resumeOrPauseScroll() {
           edOutput.pauseScroll( !searchTextPane.getScrollPane().isScrollYMax() );
        }


        @Override
        public void close()
        {
            enableProperty.set(false);
            jdbcTracer.removeListener(this);
        }
    }//end class

    private static final ResourceBundle fore = ResourceBundle.getBundle("fore");

    private ViewContext viewContext = new ViewContext(null);

    /**
     *      */
    public DBTraceDialog( TaskContext taskContext, String startTitle ) {

        setId(getClass().getName());

        setVgap(5);
        setPrefSize ( AbstractBaseController.MAX_WIDTH, AbstractBaseController.MAX_HEIGHT );
        setMaxWidth ( Double.MAX_VALUE );
        setAlignment( Pos.CENTER_LEFT  );

        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab commonTab = null;
        Tab currentTab = null;

        List<TaskContext> tcList = TCStorage.INSTANCE().getList();

        for( TaskContext tc : tcList)
        {
            DBTracePane dbTPane = new DBTracePane(tc);
            closeables.add( dbTPane::close );

            Tab tab = new Tab(tc.getConnectionString(ConnectionStringFormatEnum.SQL_INFO), dbTPane);

            if( tc == BaseApp.APP().getCommonTaskContext())
                commonTab = tab;
            else
                if( tc == taskContext)
                    currentTab = tab;
                else
                    tp.getTabs().add(tab);
        }

        if (commonTab != null)
        {
            commonTab.setText(commonTab.getText() + "/common/");
            tp.getTabs().add(commonTab);
        }

        if (currentTab != null) {

            StringBuilder sb = new StringBuilder(currentTab.getText());
            if (S.isNotNullOrEmpty(startTitle)) {
                sb.append("/");
                if (startTitle.length() > 30) {
                    startTitle = startTitle.substring(0, 29) + "…";
                }
                sb.append(startTitle);
                sb.append("/");
            }
            currentTab.setText(sb.toString());

            tp.getTabs().add(0, currentTab);
            tp.getSelectionModel().select(0);
        }
        add(tp, 0, 1, 1, 1);
        setConstraints(tp, 0, 1, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS);

//        autoRefreshProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
//            if( newValue )
//                timer.play();
//            else
//                timer.pause();
//        });
//        autoRefreshInterval.addListener( new ChangeListener<Number>() {
//            @Override
//            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
//
//                if( !enableProperty.get() )
//                    return;
//
//                timer.stop();
//                timer.getKeyFrames().remove(0);
//                timer.getKeyFrames().add( new KeyFrame( Duration.seconds( autoRefreshInterval.doubleValue() ), (a)->refresh() ) );
//                if( autoRefreshProperty.get() )
//                    timer.play();
//            }
//        } );
//
//        this.setOnHiding( (a)->enableProperty.set(false) );
    }

    /** */
    public ViewContext getViewContext() {
        return viewContext;
    }

    /** */
    public void setViewContext( ViewContext vc ) {
        this.viewContext = vc;
    }

    /** */
    public static void showDbTraceDialog( ViewContext vc, TaskContext tc ) {

        // Проверка доступности роли

        if( !JInvSecurityService.hasRole( tc, "ODB_DEBUG" ) )
        {
            Alerts.error( vc, "Нет доступа к окну отладки", "Не достаточно прав для выполнения действия" );
            return;
        }

        try {

            String  titleInfo = vc.getStage().titleProperty().get();

            DBTraceDialog pane = new DBTraceDialog( tc, titleInfo);

            String title = "DB queries tracer";

            Stage showStage = AbstractBaseController.getShowStage(null, pane, false); //!!! null

            if (showStage != null)
            {
                pane.getViewContext().setStage   (showStage);
                pane.getViewContext().setFormName(pane.getId());
                BaseApp.APP().getViewPrefService().refreshViewSettingsRoot(showStage.getScene().getRoot());

                showStage.showingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                    if (newValue) {
                        showStage.titleProperty().set(title);
                    }
                });

                showStage.setOnCloseRequest((WindowEvent event) -> {
                    pane.onClose();
                });

                showStage.show();
            }
        } catch (Throwable th) {
            JInvErrorService.handleException(vc, th);
        }
    }

    /** */
    public void onClose() {
        try {

            closeables.close();

            ViewPrefAppService.saveFormParameters( viewContext, viewContext.getFormName(), true );
        } catch (Throwable ex) {
            JInvErrorService.handleException(viewContext, ex);
        }
    }


}
