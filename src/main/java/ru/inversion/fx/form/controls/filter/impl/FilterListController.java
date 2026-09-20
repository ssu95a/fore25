package ru.inversion.fx.form.controls.filter.impl;

import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import ru.inversion.dataset.*;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.db.expr.SQLExpressionFactory;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.filter.entity.*;
import ru.inversion.fx.form.controls.renderer.Colorizer;
import ru.inversion.fx.form.controls.renderer.IColoredCell;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOV;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.FontAwesomeSolid;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_NONE;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_SHOW;
import static ru.inversion.fx.form.AbstractBaseController.FormReturnEnum.RET_OK;
import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.*;
import static ru.inversion.fx.form.controls.filter.impl.FilterWork.PLSQL_XML;
import static ru.inversion.fx.form.controls.filter.impl.FilterWork.loadFilterBody;

/**
 * @author perov
 */
public class FilterListController extends JInvFXFormController<PFrmFilterFull> {

    private static final ResourceBundle filterBundle = ResourceBundle.getBundle("filter");

    public static final String EDIT_ALL = "EDIT_ALL";

    @FXML
    private JInvToolBar tbFilter;
    @FXML
    private JInvTable<PFrmFilterFull> tblFilter;
    private final XXIDataSet<PFrmFilterFull> dsFilter = new XXIDataSet<>();

    @FXML
    private JInvTextArea edCWHEREBLK;

    // Параметры
    @FXML
    private JInvTable<PFilterParameter> tblParams;
    private final XXIDataSet<PFilterParameter> dsParams = new XXIDataSet<>();
    @FXML
    private Tab tabParams;
    @FXML
    private VBox paneParam;

    // Связь
    @FXML
    private JInvTable<PFrmFilterLinkFrom> tbFLinkFrom;
    private final XXIDataSet<PFrmFilterLinkFrom> dsFLinkFrom = new XXIDataSet<>();
    @FXML
    private JInvTable<PFrmFilterLinkTo> tbFLinkTo;
    private final XXIDataSet<PFrmFilterLinkTo> dsFLinkTo = new XXIDataSet<>();
    @FXML
    private JInvToolBar tbLink;
    @FXML
    private Tab tabGroup;


    @FXML
    private JInvTable<PFilterOdbGroupFrom> tblOdbGrpFrom;
    private final XXIDataSet<PFilterOdbGroupFrom> dsODBFrom = new XXIDataSet<>();
    @FXML
    private JInvToolBar tbODBGroup;
    @FXML
    private JInvTable<PFilterOdbGroupTo> tblOdbGrpTo;
    private final XXIDataSet<PFilterOdbGroupTo> dsODBTo = new XXIDataSet<>();
    @FXML
    private Tab tabAccess;


    private JInvCheckBox cbCurrenUserFilter = new JInvCheckBox();

    // Перекрыто, чтобы не проверялась версионность
    protected void beforeInit() throws Exception {
        initFormStateDecorator();
        dataObject = configureDataObject();
        fxEntity   = createFXEntity();
    }

    @Override
    protected void init() throws Exception {

        final String title;

        if( S.isNullOrEmpty(getFormName()) )
            title = getBundleString("FILTER_LIST_PANE.TITLE");
        else
            title = getBundleString("FILTER_LIST_PANE.TITLE_FORM") + " " + getFormName();

        setTitle(title);

        boolean editAll = S.isNullOrEmpty(getFormName()) && S.isNullOrEmpty(getBlockName());
        getInitProperties().put(EDIT_ALL, editAll);

        initToolBar();
        initDataSet();

        tblFilter.addColor(new Function<IColoredCell<PFrmFilterFull>, Colorizer>() {
            final private Colorizer green =
                new Colorizer( Color.GREEN.deriveColor(1,1,1,0.3) );
            @Override
            public Colorizer apply(IColoredCell<PFrmFilterFull> cell) {
                return (cell != null && cell.getPojo() != null && cell.getPojo().getISAUTO())
                        ? green : null;
            }
        });

        // крыж для сброса фильтра по пользователю
        cbCurrenUserFilter.setText(getBundleString("FILTER_LIST_PANE.ALL_USERS_FILTERS"));
        cbCurrenUserFilter.selectedProperty().addListener((Observable observable) -> refreshDS());
    }

    private void initToolBar()
    {
        tblFilter.setToolBar(tbFilter);
        tbFilter.setStandartActions(CREATE, UPDATE, DELETE, CREATE_BY, REFRESH);
        tblFilter.setAction( CREATE, a->doAction(FormModeEnum.VM_INS) );
        tblFilter.setAction( UPDATE, a->doAction(FormModeEnum.VM_EDIT));
        tblFilter.setAction( DELETE, a->doAction(FormModeEnum.VM_DEL) );
        tblFilter.setAction( CREATE_BY, a->doCopyFilter()             );
        tblFilter.setAction( REFRESH, a->refreshDS()                  );

        ButtonBase btnUser = ActionFactory.createButton(FontAwesome.fa_user, null, (a) -> doAutoSetup(), filterBundle.getString("FILTER_LIST_PANE.TB_MAIN_USER"));

        ButtonBase btnGROUP = ActionFactory.createButton(FontAwesome.fa_group, null, (a) -> doActionGroup(), filterBundle.getString("FILTER_LIST_PANE.TB_MAIN_GROUP"));

        ButtonBase btnExport = ActionFactory.createButton(IconDescriptor.of(FontAwesomeSolid.fas_file_export), (a) -> doFilterExport(), filterBundle.getString("FILTER_EXPORT_BTN"));

        if (getDataSet() instanceof XXIDataSet && ((XXIDataSet) getDataSet()).isEnableAutoFilter()) {
            tbFilter.getItems().addAll(new Separator(Orientation.VERTICAL), btnUser, btnGROUP, new Separator(Orientation.VERTICAL));
        } else {
            tbFilter.getItems().addAll(new Separator(Orientation.VERTICAL), btnGROUP, new Separator(Orientation.VERTICAL));
        }

        if( JInvSecurityService.isCanAccessIsAction( getTaskContext(), 4097 ) )
            tbFilter.getItems().addAll( btnExport, new Separator(Orientation.VERTICAL), cbCurrenUserFilter);

        ParameterToolBar tbParam = new ParameterToolBarImpl(filterBundle);
        paneParam.getChildren().add(0, tbParam);

        //----------------------------------
        ButtonBase btnTo      = ActionFactory.createButton(FontAwesome.fa_angle_right, null, (a) -> doSingleTo(), getBundleString("BTN.SINGLE_ADD"));
        ButtonBase btnFrom    = ActionFactory.createButton(FontAwesome.fa_angle_left, null, (a) -> doSingleFrom(), getBundleString("BTN.SINGLE_REMOVE"));
        ButtonBase btnAllTo   = ActionFactory.createButton(FontAwesome.fa_angle_double_right, null, (a) -> doAllTo(), getBundleString("BTN.MULT_ADD"));
        ButtonBase btnAllFrom = ActionFactory.createButton(FontAwesome.fa_angle_double_left, null, (a) -> doAllFrom(), getBundleString("BTN.MULT_REMOVE"));

        tbLink.getItems().addAll(btnTo, btnFrom, btnAllTo, btnAllFrom);
        tbLink.getItems().stream().filter((n) -> n instanceof Button).forEach((Node b) -> ((Button) b).setMaxWidth(Double.MAX_VALUE));

        // -----------------------------------------------------
        ButtonBase btnOdbTo = ActionFactory.createButton(FontAwesome.fa_angle_right, null, (a) -> doSingleOdbTo(), getBundleString("BTN.SINGLE_ADD"));
        ButtonBase btnOdbFrom = ActionFactory.createButton(FontAwesome.fa_angle_left, null, (a) -> doSingleOdbFrom(), getBundleString("BTN.SINGLE_REMOVE"));
        ButtonBase btnAllODBTo = ActionFactory.createButton(FontAwesome.fa_angle_double_right, null, (a) -> doAllODBTo(), getBundleString("BTN.MULT_ADD"));
        ButtonBase btnAllODBFrom = ActionFactory.createButton(FontAwesome.fa_angle_double_left, null, (a) -> doAllODBFrom(), getBundleString("BTN.MULT_REMOVE"));

        tbODBGroup.getItems().addAll(btnOdbTo, btnOdbFrom, btnAllODBTo, btnAllODBFrom);
        tbODBGroup.getItems().stream().filter((n) -> n instanceof Button).forEach((Node b) -> ((Button) b).setMaxWidth(Double.MAX_VALUE));
    }

    private void refreshDS() {

        dsFilter.setParameter( "FORM_NAME",  getFormName() );
        dsFilter.setParameter( "BLOCK_NAME", getBlockName());

        //предикат показывать фильтры всех пользователей если null
        dsFilter.setParameter( "ALL_USERS_FILTERS", cbCurrenUserFilter.isSelected() ? 0 : 1 );

        tblFilter.executeQuery();
    }

    /**
     * Добавление/редактирование/удаление фильтра в новом окне
     */
    private void doAction(FormModeEnum mode)
    {
        if( dsFilter.hasMarkedRows() )
        {
            boolean deleted = deleteMarked();
            if( deleted )
                refreshDS();
            return;
        }

        PFrmFilterFull pojo = null;

        switch(mode) {
            case VM_INS:
                pojo = new PFrmFilterFull();
                pojo.setCFORMNAME ( getFormName() );
                pojo.setCUSER     ( getTaskContext().getUserName());
                pojo.setCBLOCKNAME( getBlockName());
                break;
            case VM_EDIT:
            case VM_DEL:
                pojo = dsFilter.getCurrentRow();
            break;
        }

        if( pojo != null )
        {
            new FXFormLauncher<> (
                getTaskContext (),
                getViewContext (),
                ManageFilterController.class,
                filterBundle
            )
                .dataObject( pojo )
                .dialogMode( mode )
                .initProperties( getInitProperties() )
                .callback  ( this::doFormResult )
                .modal     ( true )
            .show( );
            /*
            FXFormLauncher<PFrmFilterFull> formLauncher = new FXFormLauncher<>(getTaskContext(), getViewContext(),
                    "ru/inversion/fx/xxi/form/controls/filter/ManageFilter.fxml");
            formLauncher.bundle(filterBundle)
                    .dataObject(pojo)
                    .initProperties(getInitProperties())
                    .dialogMode(mode)
                    .modal(true)
                    .clb((t, u) -> {
                        if (t.equals(FormReturnEnum.RET_OK)) {
                            refreshDS();
                        }
                    })
                    .show();
             */
        }
    }

    /** */
    private void doFormResult( FormReturnEnum formReturn, JInvFXFormController< PFrmFilterFull> dctl ) {

        if( formReturn == RET_OK )
        {
            try {

                switch(dctl.getFormMode()) {
                    case VM_INS:
                        dsFilter.insertRow( dctl.getDataObject(), IDataSet.InsertRowModeEnum.AFTER_CURRENT, true );
                        break;
                    case VM_EDIT:
                        dsFilter.updateCurrentRow( dctl.getDataObject() );
                        break;
                    case VM_DEL:
                        dsFilter.removeCurrentRow();
                        break;
                    default:
                        break;
                }
            }
            catch( Throwable th ) {
                handleException(th);
            }
        }
    }

    private void doActionGroup()
    {
        new FXFormLauncher<> (
                getTaskContext (),
                getViewContext (),
                ViewFilterGroupController.class,
                filterBundle
        )
            .dialogMode( VM_SHOW )
            .callback  (( ret, c ) -> tblFilter.executeQuery())
            .modal     ( true )
            .show( );
        /*
        new FXFormLauncher<>(getTaskContext(), getViewContext(),
                "ru/inversion/fx/xxi/form/controls/filter/ViewFilterGroup.fxml")
                .bundle(filterBundle)
                .dialogMode(VM_SHOW)
                .clb((formReturnEnum, controller) -> {
                    tblFilter.executeQuery();
                })
                .modal(true)
                .show();
         */
    }

    private void initDataSet() throws Exception {

        boolean hasFormName = !S.isNullOrEmpty( getInitParameter("FORM_NAME" ) );
        boolean hasBlockName= !S.isNullOrEmpty( getInitParameter("BLOCK_NAME") );

        dsFilter.setTaskContext( getTaskContext() );
        dsFilter.setRowClass   ( PFrmFilterFull.class );

        dsFilter.setWherePredicate("(:ALL_USERS_FILTERS ~ = 0 OR cuser = USER )~");
        if( hasBlockName )
            dsFilter.setWherePredicat ( "(:BLOCK_NAME is null OR cblockname = :BLOCK_NAME)", true );
        if( hasFormName  )
            dsFilter.setWherePredicat ( "(:FORM_NAME  is null OR cformname  = :FORM_NAME )", true );

        dsFilter.setName("FILTER");

        DSFXAdapter bind = DSFXAdapter.bind( dsFilter, tblFilter, null, true);
        bind.setEnableFilter( true, "FRM_FLT", "FILTER" );
        dsFilter.addNavigationListener(new IDataSetNavigationListener<PFrmFilterFull>() {
            @Override
            public void navigated(DataSetNavigationEvent<PFrmFilterFull> e) {
                if( e.getNewRow() != null )
                    edCWHEREBLK.setText( loadFilterBody(e.getNewRow().getID()) );
                else
                    edCWHEREBLK.setText( S.EMPTY_STRING );
            }
        });

        edCWHEREBLK.setFont( BaseApp.APP().viewPrefService().getCodeFont() );

        dsParams.setTaskContext    ( getTaskContext()       );
        dsParams.setRowClass       ( PFilterParameter.class );
        dsParams.setNativeQueryName( ISQLDataSet.QUERY_TABLE);

        DSFXAdapter.bind(dsParams, tblParams);
        DataLinkBuilder.linkDataSet (
            dsFilter, dsParams, PFrmFilterFull::getID, "IDFILTER"
        ).setAutoRequery( tabParams::isSelected );
        tabParams.selectedProperty().addListener(( o, oldValue, newValue ) -> {
            if( newValue )
                tblParams.executeQuery();
        });


        dsFLinkFrom.setTaskContext(getTaskContext());
        dsFLinkFrom.setRowClass   (PFrmFilterLinkFrom.class);
        dsFLinkFrom.setOrderBy("IDGROUP");
        DSFXAdapter.bind(dsFLinkFrom, tbFLinkFrom);
        DataLinkBuilder.linkDataSet(dsFilter, dsFLinkFrom, PFrmFilterFull::getID, "IDFILTER").setAutoRequery( tabGroup::isSelected );

        dsFLinkTo.setTaskContext(getTaskContext());
        dsFLinkTo.setRowClass   (PFrmFilterLinkTo.class);
        dsFLinkTo.setOrderBy("IDGROUP");
        DSFXAdapter.bind(dsFLinkTo, tbFLinkTo);
        DataLinkBuilder.linkDataSet(dsFilter, dsFLinkTo, PFrmFilterFull::getID, "IDFILTER").setAutoRequery( tabGroup::isSelected );
        tabGroup.selectedProperty().addListener(( o, oldValue, newValue ) -> {
            if( newValue ) {
                tbFLinkFrom.executeQuery(); tbFLinkTo.executeQuery();
            }
        });


        dsODBFrom.setTaskContext(getTaskContext());
        dsODBFrom.setRowClass(PFilterOdbGroupFrom.class);
        dsODBFrom.orderBy("IDGROUP");
        DSFXAdapter.bind(dsODBFrom, tblOdbGrpFrom);
        DataLinkBuilder.linkDataSet(dsFilter, dsODBFrom, PFrmFilterFull::getID, "IDFILTER").setAutoRequery( tabAccess::isSelected );

        dsODBTo.setTaskContext(getTaskContext());
        dsODBTo.setRowClass(PFilterOdbGroupTo.class);
        dsODBTo.orderBy("IDGROUP");
        DSFXAdapter.bind(dsODBTo, tblOdbGrpTo);
        DataLinkBuilder.linkDataSet(dsFilter, dsODBTo, PFrmFilterFull::getID, "IDFILTER").setAutoRequery( tabAccess::isSelected );
        tabAccess.selectedProperty().addListener(( o, oldValue, newValue ) -> {
            if( newValue ) {
                tblOdbGrpFrom.executeQuery(); tblOdbGrpTo.executeQuery();
            }
        });

        refreshDS();
    }

    /**
     * Имя формы
     * @return FORM_NAME
     */
    private String getFormName() {
        return (String) getInitProperties().get("FORM_NAME");
    }

    /** Имя блока (датасет) */
    private String getBlockName() {
        return (String) getInitProperties().get("BLOCK_NAME");
    }

    private IDataSet<?> getDataSet() {
        return (IDataSet<?>) getInitProperties().get("DATA_SET");
    }

    /**
     * Привязка к группам фильтров
     * Добавить группу
     */
    private void doSingleTo() {
        PFrmFilterFull filterCurrentRow = dsFilter.getCurrentRow();
        PFrmFilterLinkFrom groupCurrentRow = dsFLinkFrom.getCurrentRow();
        if (filterCurrentRow != null && groupCurrentRow != null) {
            initLongOperation(() -> {
                try {
                    FilterWork.addGroupLink(getTaskContext().getConnection(), groupCurrentRow.getIDGROUP(), filterCurrentRow.getID());
                    dsFLinkFrom.removeCurrentRow();
                    PFrmFilterLinkTo entity = new PFrmFilterLinkTo();
                    entity.setGRP_NAME(groupCurrentRow.getGRP_NAME());
                    entity.setIDFILTER(groupCurrentRow.getIDFILTER());
                    entity.setIDGROUP(groupCurrentRow.getIDGROUP());
                    dsFLinkTo.insertRow(entity, IDataSet.InsertRowModeEnum.FIRST, true);

                } catch (SQLExpressionException ex) {
                    JInvErrorService.handleException(null, ex);
                }
            });
        }
    }

    /**
     * Привязка к группам фильтров
     * Удалить группу
     */
    private void doSingleFrom() {
        PFrmFilterLinkTo groupCurrentRow = dsFLinkTo.getCurrentRow();
        if (groupCurrentRow != null) {
            initLongOperation(() -> {
                try {
                    FilterWork.delGroupLink(getTaskContext().getConnection(), groupCurrentRow.getIDGROUP(), groupCurrentRow.getIDFILTER());
                    dsFLinkTo.removeCurrentRow();
                    PFrmFilterLinkFrom entity = new PFrmFilterLinkFrom();
                    entity.setGRP_NAME(groupCurrentRow.getGRP_NAME());
                    entity.setIDFILTER(groupCurrentRow.getIDFILTER());
                    entity.setIDGROUP(groupCurrentRow.getIDGROUP());
                    dsFLinkFrom.insertRow(entity, IDataSet.InsertRowModeEnum.FIRST, true);
                } catch (SQLExpressionException ex) {
                    JInvErrorService.handleException(null, ex);
                }
            });
        }
    }

    /**
     * Привязка к группам фильтров
     * Добавить все группы
     */
    private void doAllTo() {
        PFrmFilterFull currentRow = dsFilter.getCurrentRow();
        if (currentRow != null) {
            try {
                FilterWork.addAllGroupLink(getTaskContext().getConnection(), currentRow.getID());
                dsFLinkFrom.executeQuery();
                dsFLinkTo.executeQuery();
            } catch (DataSetException | SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }

    /**
     * Привязка к группам фильтров
     * Удалить все группы
     */
    private void doAllFrom() {
        PFrmFilterFull currentRow = dsFilter.getCurrentRow();
        if (currentRow != null) {
            try {
                FilterWork.delAllGroupLink(getTaskContext().getConnection(), currentRow.getID());
                dsFLinkFrom.executeQuery();
                dsFLinkTo.executeQuery();
            } catch (DataSetException | SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }

    /**
     * Автоустановка для групп доступа
     * Добавить группу
     */
    private void doSingleOdbTo() {
        PFrmFilterFull filterCurrentRow = dsFilter.getCurrentRow();
        PFilterOdbGroupFrom fromCurrentRow = dsODBFrom.getCurrentRow();
        if (filterCurrentRow != null && fromCurrentRow != null) {
            try {
                FilterWork.addODBGrp(getTaskContext().getConnection(), fromCurrentRow.getIDGROUP(), filterCurrentRow.getID());
                dsODBFrom.removeCurrentRow();
                PFilterOdbGroupTo entity = new PFilterOdbGroupTo();
                entity.setGRP_NAME(fromCurrentRow.getGRP_NAME());
                entity.setIDFILTER(filterCurrentRow.getID());
                entity.setIDGROUP(fromCurrentRow.getIDGROUP());
                dsODBTo.insertRow(entity, IDataSet.InsertRowModeEnum.FIRST, true);
            } catch (SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }

    /**
     * Автоустановка для групп доступа
     * Удалить группу
     */
    private void doSingleOdbFrom() {
        PFilterOdbGroupTo groupCurrentRow = dsODBTo.getCurrentRow();
        if (groupCurrentRow != null) {
            try {
                FilterWork.delODBGrp(getTaskContext().getConnection(), groupCurrentRow.getIDGROUP(), groupCurrentRow.getIDFILTER());
                dsODBTo.removeCurrentRow();
                PFilterOdbGroupFrom entity = new PFilterOdbGroupFrom();
                entity.setGRP_NAME(groupCurrentRow.getGRP_NAME());
                entity.setIDFILTER(groupCurrentRow.getIDFILTER());
                entity.setIDGROUP(groupCurrentRow.getIDGROUP());
                dsODBFrom.insertRow(entity, IDataSet.InsertRowModeEnum.FIRST, true);
            } catch (SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }

    /**
     * Автоустановка для групп доступа
     * Добавить все группы
     */
    private void doAllODBTo() {
        PFrmFilterFull currentRow = dsFilter.getCurrentRow();
        if (currentRow != null) {
            try {
                FilterWork.addAllODBGrp(getTaskContext().getConnection(), currentRow.getID());
                dsODBFrom.executeQuery();
                dsODBTo.executeQuery();
            } catch (DataSetException | SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }


    /**
     * Автоустановка для групп доступа
     * Удалить все группы
     */
    private void doAllODBFrom() {
        PFrmFilterFull currentRow = dsFilter.getCurrentRow();
        if (currentRow != null) {
            try {
                FilterWork.delAllODBGrp(getTaskContext().getConnection(), currentRow.getID());
                dsODBFrom.executeQuery();
                dsODBTo.executeQuery();
            } catch (DataSetException | SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }

    private void doAutoSetup() {
        if (dsFilter.getCurrentRow() != null && dsFilter.getCurrentRow().getISAUTO() != null) {
            dsFilter.getCurrentRow().setISAUTO(!dsFilter.getCurrentRow().getISAUTO());
            try {
                FilterWork.autoSetupFlt(getTaskContext().getConnection(), dsFilter.getCurrentRow().getID(), dsFilter.getCurrentRow().getCAUTOSETUP());
                tblFilter.executeQuery();
            } catch (SQLExpressionException ex) {
                JInvErrorService.handleException(null, ex);
            }
        }
    }

    private void doCopyFilter() {
        PFrmFilterFull currentRow = dsFilter.getCurrentRow();
        if (currentRow != null) {
            SimpleStringProperty user = new SimpleStringProperty();
            JInvLOV lov = new JInvLOV();
            lov.setTaskContext(getTaskContext());
            lov.setSqlSelect("SELECT cusrlogname, cusrname FROM usr");
            lov.addColumn("cusrlogname", String.class, getBundleString("FILTER_LIST_PANE.LOGIN"), 100, user);
            lov.addColumn("cusrname", String.class, getBundleString("FILTER_LIST_PANE.FIO"), 260, null);
            lov.showChoiceList(getViewContext(), "", new BiConsumer<Boolean, ILov<Object>>() {
                @Override
                public void accept(Boolean t, ILov u) {
                    if (t) {
                        try {
                            FilterWork.copyFilter(getTaskContext().getConnection(), currentRow.getID(), user.get());
                        } catch (SQLExpressionException ex) {
                            JInvErrorService.handleException(null, ex);
                        }
                    }
                }
            });
        }
    }

    /** */
    private void doFilterExport( )
    {

        initProperties.put("dsFilter", dsFilter );

        new FXFormLauncher<> (
                getTaskContext (),
                getViewContext (),
                FilterExportController.class,
                filterBundle
        )
                .dialogMode( VM_NONE )
                .callback  (( ret, c ) -> tblFilter.executeQuery())
                .modal     ( true )
                .initProperties(initProperties)
                .show( );

        /*
        new FXFormLauncher<>(getTaskContext(), getViewContext(),
                "ru/inversion/fx/xxi/form/controls/filter/FilterExport.fxml")
                .bundle(filterBundle)
                .dialogMode(VM_NONE)
                .modal(true)
                .initProperties(initProperties)
                .clb((t, u) -> {
                    if (t.equals(RET_OK)) {
                        refreshDS();
                    }
                })
                .show();
        */
    }

    private boolean deleteMarked() {

        boolean yesOrNo = Alerts.yesNo(getViewContext(), getBundleString("FILTER_DELETE_ALERT_TITLE"), String.format(getBundleString("FILTER_DELETE_ALERT_DESC"), dsFilter.computeNumberMarkedRows()) );

        if( yesOrNo )
        {
            try {
                deleteByMark(dsFilter.getMarkerID());
                dsFilter.clearMark();
            } catch( Exception e ) {
                JInvErrorService.handleException(getViewContext(), e);
                return false;
            }
        }
        return true;
    }

    private void deleteByMark(Long markerId) throws SQLException {
        Connection connection = getTaskContext().getConnection();
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("markerId", markerId);
            SQLExpressionFactory.INSTANCE().execute( PLSQL_XML, "filter.work.deleteByMark", connection, params);
            connection.commit();
        } catch (Throwable th) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            throw new SQLException("Error on delete filter by mark", th);
        }
    }

    private class ParameterToolBarImpl extends ParameterToolBar {

        public ParameterToolBarImpl(ResourceBundle bundle) {
            super(bundle);
        }

        @Override
        void refresh() {
            tblParams.executeQuery();
        }

        @Override
        PFilterParameter getEntity() {
            return dsParams.getCurrentRow();
        }

        @Override
        Long getIDFilter() {
            return dsFilter.getCurrentRow().getID();
        }

        @Override
        BiConsumer<FormReturnEnum, JInvFXFormController> getClb() {
            return (t, u) -> {
                if (t.equals(RET_OK)) {
                        switch (u.getFormMode()) {
                            case VM_INS:
                                dsParams.insertRow((PFilterParameter) u.getDataObject(), IDataSet.InsertRowModeEnum.FIRST, true);
                                break;
                            case VM_EDIT:
                                dsParams.updateCurrentRow((PFilterParameter) u.getDataObject());
                                break;
                            case VM_DEL:
                                dsParams.removeCurrentRow();
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
}
