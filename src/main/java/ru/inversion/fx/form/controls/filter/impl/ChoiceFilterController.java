package ru.inversion.fx.form.controls.filter.impl;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.datacall.SQLCallBuilder;
import ru.inversion.dataset.*;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.mark.OARow;
import ru.inversion.dataset.mark.OARowRowIDMarkable;
import ru.inversion.db.DBUniqueResult;
import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.db.rs.RSUtils;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.app.service.ViewPrefDao;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.filter.SQLFilter;
import ru.inversion.fx.form.controls.filter.entity.PFilterParameter;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilter;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterFull;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterGroup;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOV;
import ru.inversion.fx.form.lov.PLovMarkableParam;
import ru.inversion.fx.form.property.JInvLongProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static ru.inversion.dataset.DataSetEvent.DataSetEventType.FIRST_TIME_EXECUTE;
import static ru.inversion.dataset.ISQLDataSet.QUERY_TABLE;
import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.FILTER_EXEC;
import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.SAVE_FILE;

/**
 * Контроллер формы выбора и загрузки фильтра
 * - редактирование параметров фильтра
 * - установка фильтра по умолчанию
 *
 * @author perov
 *         Sulimoff
 * @version 2.0.0
 */
public class ChoiceFilterController extends JInvFXFormController<PFrmFilter> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceFilterController.class);

    private static final String SAVE_GROUP_ID = "GROUP_ID";

    private static final String SAVE_FILTER_ID = "FILTER_ID";

    private static final String SAVE_CB_ALL_USER_SHOW = "CB_ALL_USER_FILTER";

    @FXML
    private JInvTable<PFrmFilter> tblFlt;
    @FXML
    private JInvTable<PFilterParameter> tblPrm;
    @FXML
    private JInvTableColumn<PFilterParameter, String> colParamValue;
    @FXML
    private JInvTextField edGR;
    @FXML
    private CheckBox cbAddCurr;
    @FXML
    private CheckBox cbFix;

    private final XXIDataSet<PFrmFilter> dsFilter = new XXIDataSet<>(PFrmFilter.class);
    private IDataSet<PFilterParameter> dsParam;

    @FXML
    private CheckBox chRun;
    @FXML
    private JInvTextField edDefFilter;

    @FXML
    private JInvButton btSave;
    @FXML
    private JInvButton btSetCurrent;
    @FXML
    private JInvButton btClear;
    @FXML
    private JInvButton btOK;
    @FXML
    private JInvButton btCancel;

    // Текущая выбранная группа фильтра
    private final JInvLongProperty currentGroupIdProperty = new JInvLongProperty( this, "currentGroupId", null );

    // Текущий выбранный фильтр
    private final LongProperty currentFilterId = new SimpleLongProperty();

    // Id фильтра по умолчанию
    private final LongProperty defFilterId = new SimpleLongProperty();

    @FXML
    private CheckBox chFIX2;

    // Выбранный фильтр
    private SQLFilter retFilter;

    @FXML
    private Tab tabParam;

    @FXML
    private Tab tabOption;

    @FXML
    private TabPane tabPane;

    final static private Map<String, Object> paramFilter = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    // Показывать фильтры всех пользователей
    @FXML
    private JInvCheckBox chAllUsersFilters;

    // для сохранения настроек
    private List<PPrefComponent> prefComponents;

    // Перекрыто, чтобы не проверялась версионность хелпового контроллера
    @Override
    protected void beforeInit() throws Exception {
        initFormStateDecorator();
        dataObject = configureDataObject();
        fxEntity = createFXEntity();
    }

    /** */
    @Override
    protected void init() throws Exception {

        super.init();

        setTitle( getBundleString("CHOICE_FILTER_PANE.TITLE") );

        // Загрузка настроек
        loadPreferences();

        // Загрузка фильтра по умолчанию
        loadDefaultFilterFromPref();

        initDataSet();
        initLov();
        initToolbar();
        initTable();

        cbAddCurr.visibleProperty().bind(tabOption.selectedProperty().not());
        cbFix.visibleProperty().bind( tabOption.selectedProperty().not() );

        chAllUsersFilters.setText(getBundleString("FILTER_LIST_PANE.ALL_USERS_FILTERS"));
        chAllUsersFilters.selectedProperty().addListener(( Observable observable) -> refreshDS());

        // Focus on table
        tblFlt.requestFocus();

        refreshDS();
    }

    @Override
    protected void afterInit() throws AppException {
        btOK.visibleProperty().bind( tabOption.selectedProperty().not() );
        btCancel.visibleProperty().bind( tabOption.selectedProperty().not() );
    }

    /** */
    private void loadPreferences() throws Exception
    {
        // Загружаем сохраненный фильтр и checkBox'ы
        prefComponents = ViewPrefDao.loadPreferences (
            getPreferenceKey(),
            tblFlt.getId(),
            Arrays.asList( SAVE_GROUP_ID, SAVE_FILTER_ID, SAVE_CB_ALL_USER_SHOW )
        );

        //
        for( PPrefComponent pc : prefComponents )
        {
            String element = pc.getELEMENT();

            if( S.isNullOrEmpty(element) )
                continue;

            switch( element )
            {
                // чекбокс, показывать фильтры всех пользователей
                case SAVE_CB_ALL_USER_SHOW:
                    chAllUsersFilters.setSelected(pc.getVISIBLE() > 0);
                break;
                // Id выбранной группы фильтров
                case SAVE_GROUP_ID:
                    currentGroupIdProperty.set( pc.getVISIBLE());
                break;
                // Id выбранного фильтра
                case SAVE_FILTER_ID:
                    currentFilterId.set( pc.getVISIBLE());
                break;
            }
        }
    }

    /** */
    static private String getParamName(Long param) {
        return ":P" + param;
    }

    /** */
    private void initDataSet() throws Exception {

        dsFilter.taskContext( getTaskContext() ).queryAllRows().orderBy("cwherename").nativeQueryName(QUERY_TABLE);
        dsFilter.setWherePredicate("( to_number(:IDGROUP) is null or EXISTS (SELECT NULL FROM V_JF_FRM_FILTER_LINK l WHERE l.IDFilter = V_JF_FRM_FILTER.ID AND l.IDGROUP = :IDGROUP )) AND cFormName = :FORM AND cBlockName = :BLOCK AND ( :ALL_USER = ~0~ OR CUSER=USER )");
        dsFilter.setParameter( "ALL_USER", 1 );
        dsFilter.setParameter( "IDGROUP", null );
        dsFilter.setParameter( "FORM" , getInitProperties().get("FORM_NAME" ));
        dsFilter.setParameter( "BLOCK", getInitProperties().get("BLOCK_NAME"));

        dsFilter.addNavigationListener( new IDataSetNavigationListener<PFrmFilter>() {
            @Override
            public void navigated( DataSetNavigationEvent<PFrmFilter> e ) {

                try {

                    if( e.getNewRow() == null ) {
                        dsParam.set("IDFILTER", null );
                        dsParam.clear();
                    }
                    else {
                        dsParam.set("IDFILTER", e.getNewRow().getID() );
                        dsParam.executeQuery();
                    }
                } catch( DataSetException ex ) {
                    handleException( new RuntimeException( "Ошибка при получении списка параметров", ex ) );
                }
            }
        });

        dsFilter.addDataSetListener(new IDataSetListener() {
            @Override
            public void dataSetChanged( DataSetEvent e ) {

                if( e.getEventType() == FIRST_TIME_EXECUTE && e.isAfter() )
                {
                    dsFilter.removeDataSetListener(this);
                    Platform.runLater( ()->dsFilter.findRow( (p)->p.getID() == currentFilterId.get() ) );
                }//end if
            }//end if
        });

        DSFXAdapter.bind( dsFilter, tblFlt) ;

        dsParam = new ReaderDataSet<>( PFilterParameter.class, new Function< IDataSet< PFilterParameter >, IDataReader< PFilterParameter > >() {
            @Override
            public IDataReader< PFilterParameter > apply( IDataSet< PFilterParameter > ds ) {

                final ResultSet rs
                    = SQLCallBuilder.NEW(getTaskContext())
                        .name("getFltParametersValues")
                        .runClass(PFilterParameter.class)
                            .build()
                        .set("filterId", ds.getParameter("IDFILTER") )
                            .execute()
                        .get("cParameters");

                return RSUtils.createResultSetDataReader( rs, PFilterParameter.class );
            }
        });

        DSFXAdapter.bind( dsParam, tblPrm );
    }

    /** */
    private void refreshDS( ) {
        dsFilter.setParameter( "ALL_USER", chAllUsersFilters.isSelected() ? 0 : 1 );
        tblFlt.executeQuery( );
    }

    /** */
    private void initLov() {

        IParameters lovParams = new ParametersByName() {
            @Override
            public Object getParameter(String parameterName) {
                switch (parameterName) {
                    case "FORM":
                        return getInitProperties().get("FORM_NAME");
                    case "BLOCK":
                        return getInitProperties().get("BLOCK_NAME");
                }
                return null;
            }
        };

        // LOV выбора группы фильтров
        JInvLOV lovGR = new JInvLOV();
        lovGR.setTaskContext(getTaskContext());
        lovGR.setSqlSelect("SELECT G.ID || '. ' || G.cName AS GROUP_NAME, G.ID AS ID\n"
                + "  FROM FRM_FILTER_GROUP G,\n"
                + "       (SELECT DISTINCT L.IDGroup\n"
                + "          FROM FRM_FILTER_LINK L, FRM_FILTER F\n"
                + "         WHERE L.IDFilter = F.ID\n"
                + "           AND F.cFormName = :FORM\n"
                + "           AND (F.cBlockName = :BLOCK OR F.cBlockName IS NULL)) D\n"
                + " WHERE G.ID = D.IDGroup");
        lovGR.addColumn( "GROUP_NAME", String.class, getBundleString("GRUPPA"), 300, null );
        lovGR.addColumn( "ID", Long.class, "ID", 0, currentGroupIdProperty);
        edGR.setLOV    ( lovGR );

        lovGR.setParameters( lovParams );

        lovGR.valueProperty().addListener (
            (ObservableValue<?> observable, Object oldValue, Object newValue) -> {
                dsFilter.setParameter( "IDGROUP", currentGroupIdProperty.getValue() );
                refreshDS( );
            }
        );

        if( !currentGroupIdProperty.isNullValue() )
        {
            final DBUniqueResult< PFrmFilterGroup > ur = new DBUniqueResult<>(PFrmFilterGroup.class,PFrmFilterGroup.class, S.EMPTY_STRING );
            PFrmFilterGroup v = ur.execute( getTaskContext(), false, currentGroupIdProperty.getValue() );
            if( v != null )
            {
                edGR.setText(v.getCNAME());
                dsFilter.setParameter( "IDGROUP", currentGroupIdProperty.getValue() );
            }
        }

        // Чтобы можно было очистить фильтр
        edGR.textProperty().addListener((observable, oldValue, newValue) -> {
            // Если textField пустой - сбрасываем "группы фильтров"
            if( newValue.isEmpty() )
            {
                currentGroupIdProperty.setValue( null ); // Сбрасываем groupId
                dsFilter.setParameter( "IDGROUP", null );
                refreshDS();
            }
        });

        // LOV выбора фильтра по умолчанию для таблицы
        final JInvLOV lovDefFilter = new JInvLOV();
        lovDefFilter.setTitle      ( getBundleString("LOV_FILTER") );
        lovDefFilter.setTaskContext( getTaskContext());
        lovDefFilter.setSqlSelect  ( "SELECT ID, cWhereName NAME, cuser FROM FRM_FILTER WHERE cFormName = :FORM AND ( cBlockName = :BLOCK OR cBlockName IS NULL ) ORDER BY cWhereName");
        lovDefFilter.addColumn( "NAME",  String.class, getBundleString("FILTR"), 400, null );
        lovDefFilter.addColumn( "CUSER", String.class, getBundleString("FILTER_LIST_PANE.COL_CUSER"), 100, null );
        lovDefFilter.addColumn( "ID",    Long.class, "ID", 80, defFilterId );
        lovDefFilter.setParameters ( lovParams );

        edDefFilter.setLOV( lovDefFilter );
    }

    private void initToolbar() {

        ActionFactory.initButton (
            btSetCurrent, FILTER_EXEC, a -> doSetCurrent()
        ).setTooltip( new Tooltip( bundle.getString("CHOICE_FILTER_PANE.TB_BTN_EXECUTE") ) );

        ActionFactory.initButton (
            btSave, SAVE_FILE, a -> doSave()
        ).setText("Сохранить");

        ActionFactory.initButton (
            btClear, ActionFactory.ActionTypeEnum.DELETE, a -> doClearDef()
        ).setTooltip( new Tooltip( bundle.getString("CHOICE_FILTER_PANE.TB_BTN_CLEAR") ) );

    }

    /** Установка текущего фильтра из списка - фильтром по умолчанию*/
    private void doSetCurrent()
    {
        if( dsFilter.getCurrentRow() != null )
        {
            edDefFilter.setText( dsFilter.getCurrentRow().getCWHERENAME() );
            defFilterId.set    ( dsFilter.getCurrentRow().getID() );
        }
    }

    /** Очистка фильтра по умолчанию */
    private void doClearDef() {

        // Не сохраняем параметры при нажатии на "очистить"
        // сохраним потом все вместе

        edDefFilter.setText("");
        defFilterId.set    (-1L);
    }

   /**
    * Сохраняем настройки фильтра по умолчанию в Pref
    */
    private void doSave() {
        try {

            StringBuilder sb = new StringBuilder();
            sb.append( chRun.isSelected() ? "Y" : "N").append(chFIX2.isSelected() ? "Y" : "N");

            if( defFilterId.getValue() != null && defFilterId.get() >0L )
                sb.append( defFilterId.getValue().toString() );

            FilterWork.setPreference( getPreferenceKey(), sb.toString() );

        } catch (SQLExpressionException ex) {
            JInvErrorService.handleException(getViewContext(), ex);
        }
    }

    /**
     * Имя для сохранения в Pref
     */
    transient private String preferenceKey = null;

    String getPreferenceKey()
    {
        if( preferenceKey == null )
        {
            preferenceKey = getInitParameter("FILTER_NAME");

            if( preferenceKey == null )
                preferenceKey = FilterWork.getPreferenceKey (getInitParameter ("FORM_NAME"), getInitParameter ("BLOCK_NAME"));
//                preferenceKey = "M_FILTER." + getInitProperties().get("FORM_NAME") + "." + getInitProperties().get("BLOCK_NAME") + ".OPT";
        }

        return preferenceKey;
    }

    /** */
    @Override
    protected boolean onOK( ) {

        try {

            PFrmFilter entity = dsFilter.getCurrentRow();

            if( entity != null )
            {
                retFilter = getFilter();

                if( retFilter != null )
                {
                    this.getInitProperties().put("FILTER", retFilter );
                    this.getInitProperties().put("APPEND", cbAddCurr.isSelected());
                    this.getInitProperties().put("FIXED",  cbFix.isSelected() );
                    this.getInitProperties().put("HASPRM", entity.getHASPRM() );

                    // Переходим на ячейку с параметрами
                    if( !tabPane.getSelectionModel().getSelectedItem().equals(tabParam) && retFilter.hasParameters())
                    {
                        tabPane.getSelectionModel().select(tabParam);
                        return false;
                    }

                    ViewPrefDao.savePreferences( preparePref4Save() );

                    return true;
                }
            }
        } catch (Throwable th) {
            JInvErrorService.handleException( getViewContext(), th );
        }
        return false;
    }

    /** */
    private void initTable()
    {
        // Редактировать ячейку "Значение" у параметра, при нажатии клавиши Enter
        tblPrm.setOnKeyPressed(event -> {
            if( event.getCode() == KeyCode.ENTER ) {
                final TablePosition<PFilterParameter, ?> tablePosition = tblPrm.getFocusModel().getFocusedCell();
                Platform.runLater(() -> {
                    tblPrm.edit(tablePosition.getRow(), tablePosition.getTableView().getColumns().get(2));
                });
            }
        });
        tblPrm.setEditable(true);
        colParamValue.setEditable(true);
        colParamValue.setCellFactory ( param -> new EditingCell( getTaskContext() ) );
        colParamValue.setOnEditCommit( event -> event.getTableView().getItems().get(event.getTablePosition().getRow()).setCPARAMDEFAULT(event.getNewValue()) );
    }

    /** Если в select есть параметры, проверяем что они заполнены */
    private boolean checkParamExist(String strSql, String paramName, String paramValue) {

        if( S.isNullOrEmpty(strSql) || S.isNullOrEmpty(paramName) )
            return true;

        return !(S.isNullOrEmpty(paramValue) && S.indexOfIgnoreCase( strSql, paramName ) != -1);
    }

    /** */
    private SQLFilter getFilter()
    {
        PFrmFilter entity = dsFilter.getCurrentRow();

        if( entity == null )
            return null;

        final SQLFilter filter = new SQLFilter();
        filter.setName( entity.getCWHERENAME());
//        filter.setSQL ( entity.getCWHEREBLK() );
        filter.setSQL ( FilterWork.loadFilterBody( entity.getID() ) );

        if( entity.hasParam() )
        {
            for( PFilterParameter p : U.iterable( dsParam.getRowIterator(null)) )
            {
                if (!checkParamExist( filter.getSQL(), getParamName(p.getIPARAMNUM()), p.getCPARAMDEFAULT_CALC())) {
                    // Если есть параметры - переходим в раздел "Параметры" и встаем на нужную ячейку для редактирования
                    tabPane.getSelectionModel().select(tabParam);

                    dsParam.findRow((fp, pn) -> U.equals(fp.getIPARAMNUM(), pn), p.getIPARAMNUM());

                    final TableColumn<PFilterParameter, ?> column = tblPrm.getColumns().get(2);
                    
                    Platform.runLater(() -> {
                        tblPrm.edit(dsParam.getCurrentRowNum(), column);
                    });
                    return null;
                }

                filter.setParameter("P" + p.getIPARAMNUM(), p.getCPARAMDEFAULT_CALC());

            }//end while
        }
        return filter;
    }

    /**
     * Загрузка фильтра по умолчанию
     */
    private void loadDefaultFilterFromPref() {

        try {

            String v = FilterWork.getPreference( getPreferenceKey()).orElse("NN0");

            chRun.setSelected ( v.charAt(0) == 'Y' );
            chFIX2.setSelected( v.length() > 1 && v.charAt(1) == 'Y' );

            final Long fltrId = U.nvl( TypeConverter.convert ( v.substring(2), Long.class ), 0L );

            if( fltrId != 0 )
            {
                Optional<PFrmFilterFull> filterFull = FilterWork.getFilterById( fltrId );

                filterFull.ifPresent (
                    f -> {
                        edDefFilter.setText   ( f.getCWHERENAME() );
                        this.defFilterId.setValue( fltrId );
                    }
                );

                Platform.runLater( ()->
                    dsFilter.findRow( (p)->p.getID().equals(fltrId)
                ));
            }

        } catch ( SQLExpressionException ex ) {
            JInvErrorService.handleException(getViewContext(), ex);
        }
    }

    /** */
    private List<PPrefComponent> preparePref4Save() {

        final List<PPrefComponent> retList = new ArrayList<>();
        final String formName = getPreferenceKey();

        final PPrefComponent prefAllUserFilter = new PPrefComponent();
        prefAllUserFilter.setFORM_NAME( formName );
        prefAllUserFilter.setCOMPONENT( tblFlt.getId() );
        prefAllUserFilter.setELEMENT  ( SAVE_CB_ALL_USER_SHOW);
        prefAllUserFilter.setVISIBLE  ( chAllUsersFilters.isSelected() ? 1L : 0L);

        retList.add( prefAllUserFilter );

        if( !currentGroupIdProperty.isNullValue() )
        {
            final PPrefComponent prefGroupId = new PPrefComponent();
            prefGroupId.setFORM_NAME( formName );
            prefGroupId.setCOMPONENT( tblFlt.getId() );
            prefGroupId.setELEMENT  ( SAVE_GROUP_ID);
            prefGroupId.setVISIBLE  ( currentGroupIdProperty.get() );

            retList.add( prefGroupId );
        }

        if( dsFilter.getCurrentRow() != null )
        {
            final PPrefComponent prefFilterId = new PPrefComponent();
            prefFilterId.setFORM_NAME( formName );
            prefFilterId.setCOMPONENT( tblFlt.getId() );
            prefFilterId.setELEMENT  ( SAVE_FILTER_ID );
            prefFilterId.setVISIBLE  ( dsFilter.getCurrentRow().getID() );
            retList.add( prefFilterId );
        }

        return retList;
    }

    /** Загрузка текущего фильтра в списке сохраненного с пред вызова */
//    private void loadSelectedFilter()
//    {
//        for( PPrefComponent prefComponent : prefComponents)
//        {
//            if( prefComponent.getELEMENT().equalsIgnoreCase(SAVE_GROUP_ID)) {
//                if (prefComponent.getVISIBLE() <= 0) {
//                    currentGroupIdProperty.set( null );
//                } else {
//                    currentGroupIdProperty.set( prefComponent.getVISIBLE());
//                }
//            }
//        }
//    }

    /*
    private class ParameterToolBarImpl extends ParameterToolBar {

        public ParameterToolBarImpl(ResourceBundle bundle) {
            super(bundle);
        }

        @Override
        void refresh() {
            refreshDS();
        }

        @Override
        PFilterParameter getEntity() {
            return dsParam.getCurrentRow();
        }

        @Override
        Long getIDFilter() {
            return dsFilter.getCurrentRow().getID();
        }

        @Override
        BiConsumer<FormReturnEnum, JInvFXFormController> getClb() {
            return (t, u) -> {
                if (t.equals(FormReturnEnum.RET_OK)) {

                    switch (u.getFormMode()) {
                        case VM_INS:
                            dsParam.insertRow((PFilterParameter) u.getDataObject(), IDataSet.InsertRowModeEnum.FIRST, true);
                            break;
                        case VM_EDIT:
                            dsParam.updateCurrentRow((PFilterParameter) u.getDataObject());
                            break;
                        case VM_DEL:
                            dsParam.removeCurrentRow();
                            break;
                        default:
                            break;
                    }
                }
            };
        }

        @Override
        TaskContext getTC() {
            return getTaskContext();
        }

        @Override
        ViewContext getVC() {
            return getViewContext();
        }
    }
    */

    public static class EditingCell extends TableCell<PFilterParameter, String> {

        private final ParamCellTextField textField;
        private final TaskContext tc;

        EditingCell(TaskContext tc) {
            this.tc = tc;
            textField = new ParamCellTextField(getString()) {
                @Override
                public void commit() {
                    commitEdit(textField.getText());
                }

                @Override
                public void cancel() {
                    super.cancelEdit();
                    setText((String) getItem());
                    setGraphic(null);
                }
            };
        }

        @Override
        public void startEdit() {
            super.startEdit();
            textField.setText(getItem()); // Устанавливаем текст который уже есть в ячейке
            setText(null);
            setGraphic(textField);
            textField.selectAll();
            refreshMenu();
            textField.requestFocus();
            textField.requestLayout();
            initLOV();
        }

        @Override
        public void commitEdit(String newValue) {
            super.commitEdit(newValue);
            updateItem(newValue, false); // JAVAKERNEL-888. Приходится два раза вызывать updateItem
            saveParamValue(newValue);
        }

        private void saveParamValue(String newValue) {
            try {
                getFilterParameter().setCPARAMDEFAULT_CALC(newValue);
                paramFilter.put(getParamName(getFilterParameter().getIPARAMNUM()), newValue);
                if (getSaveParam()) {
                    ParamWork.saveParamVal(tc.getConnection(), ParamWork.getCParamPref(getFilterId(), getParamNum()), newValue);
                }
            } catch (SQLException ex) {
                JInvErrorService.handleException( null, ex);
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText( getItem() );
            setGraphic(null);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                if (textField != null) {
                    textField.setText(getString());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(getString());
                setGraphic(null);
            }
        }

        /** */
        private Class<? extends OARow> getModeFromSQL(String strSQL) {

            try (PreparedStatement ps = tc.getConnection().prepareStatement(strSQL)) {

                final ResultSetMetaData metaData = ps.getMetaData();

                if (isContainsRowId(metaData)) {
                    return OARowRowIDMarkable.class;//MarkModeEnum.MRK;
                }

            } catch (SQLException e) {
                LOGGER.error(String.format("Can't determine the mode from SQL - %s", strSQL), e);
            }

            return null;
        }

        private boolean isContainsRowId(ResultSetMetaData metaData) throws SQLException {
            final int columnCount = metaData.getColumnCount();
            if (columnCount > 2) {
                for (int i = 1; i <= columnCount; i++) {
                    final String columnName = metaData.getColumnName(i);
                    if (columnName.equalsIgnoreCase("ROWID") || columnName.equalsIgnoreCase("Метка")) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** */
        private void initLOV( ) {
            try
            {
                //Если есть курсор добавляем лов
                if( getFilterParameter() != null && getFilterParameter().getIDCURSOR() != null )
                {

                    //String sqlCursor = paramDao.getSQLCursor(getFilterParameter().getIDCURSOR());
                    String sqlCursor = ParamWork.getSQLCursor( tc, getFilterParameter().getIDCURSOR() );
                    final Class<? extends OARow> modeFromSQL = getModeFromSQL(sqlCursor);

                    final JInvLOV lov;
                    if( modeFromSQL != null )
                        lov = new JInvLOVMarkable(modeFromSQL);
                    else
                        lov = new JInvLOV();

                    QueryMetaData.getQueryColumnsNames( tc, sqlCursor).forEach((String s) -> {
                        lov.addColumn(s, String.class, s, -1, textField.textProperty());
                    });

                    lov.setTaskContext(tc);
                    lov.setSqlSelect(sqlCursor);
                    lov.setParameters(new IParameters() {
                        @Override
                        public Object getParameter(String parameterName) {
                            if (paramFilter.containsKey(":" + parameterName)) {
                                return paramFilter.get(":" + parameterName);
                            }
                            return null;
                        }

                        @Override
                        public Object getParameter(int parameterIndex) {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    });
                    lov.valueProperty().addListener((observable, oldValue, newValue) -> {
                        String value = "";
                        // Возвращаем marker id, если lov markable
                        if (lov instanceof JInvLOVMarkable) {
                            final Long markerId = ((JInvLOVMarkable) lov).getMarkerId();
                            if (markerId != null && markerId > 0) {
                                value = String.valueOf(markerId);
                            } else {
                                value = newValue.toString();
                            }
                        } else {
                            value = newValue.toString();
                        }
                        if (S.isNotNullOrEmpty(value)) {
                            setText(value);
                            commitEdit(value);
                        }
                        //tblPrm.requestFocus();
                    });
                    textField.setLOV(lov, false);
                }
            } catch (Exception ex) {
                JInvErrorService.handleException(null, ex);
            }

        }

        private Long getFilterId() {
            return getFilterParameter().getIDFILTER();
        }

        private Long getParamNum() {
            return getFilterParameter().getIPARAMNUM();
        }

        private Boolean getSaveParam() {
            return getFilterParameter() != null && getFilterParameter().getCPARAMSAVE() != null && getFilterParameter().getCPARAMSAVE().equals("Y");
        }

        private PFilterParameter getFilterParameter() {
            return tableViewProperty().getValue().getItems().get(tableRowProperty().getValue().getIndex());
        }

        /** */
        private List<MenuItem> getMenuLastParam(Long filterId, Long paramNum) throws SQLException
        {
            final String cParamPref = ParamWork.getCParamPref(filterId, paramNum);
            List<String> listParamValue = ParamWork.getListPrefParam( tc, cParamPref );
            List<MenuItem> list = new ArrayList<>(11);
            listParamValue.forEach((String param) -> {
                MenuItem item = new MenuItemParam(param);
                item.setOnAction((ActionEvent event) -> {
                    textField.setText(param);
                    commitEdit(textField.getText());
                });
                list.add(item);
            });

            Collections.reverse(list);

            return list;
        }

        /** */
        private String getString( ) {
            return getItem() == null ? S.EMPTY_STRING : getItem();
        }

        private void refreshMenu() {
            try {

                //Добавляем в контекстное меню 10 последних значений
                final List<MenuItem> list = getMenuLastParam(getFilterId(), getParamNum());

                if(!list.isEmpty())
                    list.forEach((MenuItem item) -> textField.addItemToContextMenu(item) );

            } catch( SQLException ex ) {
                JInvErrorService.handleException(ViewContext.tryGetWindow(textField), ex);
            }
        }
    }

    private static final class JInvLOVMarkable extends JInvLOV {
        private final Class<? extends OARow> rowClass;
        private Long markerId;

        public JInvLOVMarkable(Class<? extends OARow> rowClass) {
            this.rowClass = Objects.requireNonNull(rowClass, "rowClass can't be null");
        }

        @Override
        public void showChoiceList(ViewContext vc, String filterString, BiConsumer<Boolean, ILov<Object>> clb) {
            try {
                if (getColumnList().isEmpty()) {
                    throw new IllegalStateException(fore.getString("NE_OPREDELENY_STOLBCY_DLYA_LOV"));
                }

                final PLovMarkableParam<? extends OARow> markableParam = new PLovMarkableParam<>(rowClass, this, filterString);

                new FXFormLauncher<PLovMarkableParam>(getTaskContext(), vc, "ru/inversion/fx/form/lov/fxml/ViewMarkableLov.fxml")
                        .bundle(BaseApp.APP().getCommonResourceBundle())
                        .dataObject(markableParam)
                        .dialogMode(FormModeEnum.VM_CHOICE)
                        .callback((formReturnEnum, controller) -> {
                            if (formReturnEnum == FormReturnEnum.RET_OK)
                            {
                                // Считаем, если выбор пометки
                                // то вертается markerId
                                setValue( TypeConverter.convert (
                                            controller.getDataObject().getResult(),
                                            Long.class
                                          )
                                );

                                clb.accept(true, this);
                            }
                        })
                        .modal(true)
                        .show();
            } catch (Throwable th) {
                JInvErrorService.handleException(vc.getStage(), th);
            }

        }

        public Long getMarkerId() {
            return (Long)getValue(); //lovDialog.getMarkerId();
        }
    }

}
