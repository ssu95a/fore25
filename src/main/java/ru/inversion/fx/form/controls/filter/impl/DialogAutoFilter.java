package ru.inversion.fx.form.controls.filter.impl;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.WindowEvent;
import ru.inversion.dataset.DLBuilder;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.impl.XXIDsDao;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvLabel;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.filter.entity.PFilterParameter;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilter;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.IconSize;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 * Выбор автофильтра
 */
class DialogAutoFilter extends Dialog< XXIDsDao.FilterData > {

    /** */
    private XXIDsDao.FilterData retFilter;

    final private SQLDataSet< PFrmFilter > dsFilter;
    final private SQLDataSet< PFilterParameter > dsParam = new SQLDataSet<>(PFilterParameter.class);

    final private JInvTable<PFrmFilter>  tbFilter = new JInvTable<>();
    final private JInvTable<PFilterParameter> tblPrm = new JInvTable<>();

    final private TabPane tabs = new TabPane();

    /**
     */
    public DialogAutoFilter( ViewContext vc, TaskContext tc, SQLDataSet<PFrmFilter> dataSet ) {

        dsFilter = dataSet;

        this.setTitle("Автофильтр");

        final DialogPane dialogPane = getDialogPane();

        this.setHeaderText( "Выбор авто фильтра для таблицы");
        this.setGraphic   ( IconFactory.getLabel( new IconDescriptorBuilder().iconId(FontAwesome.fa_filter).iconSize(IconSize.LARGE).build()));

        this.initModality ( Modality.APPLICATION_MODAL );
        this.setResizable ( true );
        this.initOwner    ( vc.getStage() );
        dialogPane.getButtonTypes().addAll(ButtonType.OK);
        dialogPane.setExpandableContent( createDetailsNode(tc) );

        final Button buttonOk = (Button)dialogPane.lookupButton(ButtonType.OK);

        buttonOk.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    retFilter = getFilter();
                    if(retFilter == null)
                        event.consume();
                }
        );

        dialogPane.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, new EventHandler< WindowEvent >() {
            @Override
            public void handle( WindowEvent e ) {
                Alerts.info(vc,"Необходимо выбрать автофильтр!");
                e.consume();
            }
        });

        setResultConverter( (bt) -> retFilter);

        dialogPane.setExpanded(true);

        // hack
        final ButtonBar buttonBar = (ButtonBar)dialogPane.getChildren().get(2);
        buttonBar.getButtons().remove(0);

        tbFilter.requestFocus();
    }

    /** */
    private XXIDsDao.FilterData getFilter() {

        PFrmFilter frmFilter = dsFilter.getCurrentRow();

        if( frmFilter == null )
            return null;

        XXIDsDao.FilterData filter = new XXIDsDao.FilterData( FilterWork.loadFilterBody(frmFilter.getID()), frmFilter.getCWHERENAME(), frmFilter.getID(), true );

        if( !dsParam.isEmpty() )
        {
            for( PFilterParameter p : U.iterable(dsParam.getRowIterator(null)) )
            {
                if( S.isNullOrEmpty( p.getCPARAMDEFAULT_CALC() ) )
                {
                    // Если есть параметры и не заполнены
                    // переходим на вкладку "Параметры"
                    // и встаем на нужную ячейку для редактирования
                    tabs.getSelectionModel().select(1);

                    dsParam.findRow(( fp, pn ) -> U.equals(fp.getIPARAMNUM(), pn), p.getIPARAMNUM());

                    //Platform.runLater(() -> {
                    tblPrm.edit( dsParam.getCurrentRowNum(), tblPrm.getColumns().get(2) );
                    //});
                    return null;
                }

                filter.setParameter( "P" + p.getIPARAMNUM(), p.getCPARAMDEFAULT_CALC() );

            }//end while
        }
        return filter;
    }

    /**
     *
     */
    private Node createDetailsNode( TaskContext tc ) {
        try {

            {
                VBox gp = new VBox(5.0d);
                gp.setPadding(new Insets(5));

                JInvTableColumn< PFrmFilter, Long > colId = new JInvTableColumn<>("Id");
                colId.setId("ID"); colId.setFieldName("ID"); colId.setPrefWidth(60);

                JInvTableColumn< PFrmFilter, String > colName = new JInvTableColumn<>("Фильтр");
                colName.setId("CNAME"); colName.setFieldName("CWHERENAME"); colName.setPrefWidth(450);

                JInvTableColumn< PFrmFilter, String > colUser = new JInvTableColumn<>("Пользователь");
                colUser.setId("CUSER"); colUser.setFieldName("CUSER"); colUser.setPrefWidth(100);

                tbFilter.getColumns().addAll( colId, colName, colUser );

                DSFXAdapter.bind( dsFilter, tbFilter, null, false );

                this.setResultConverter(param -> retFilter);
                gp.getChildren().add(tbFilter);
                tabs.getTabs().add(new Tab("Фильтры", gp));

                tbFilter.requestFocus();
            }

            {
                VBox gp = new VBox(5.0d);
                gp.setPadding(new Insets(10));
                tblPrm.setEditable(true);

                JInvTableColumn< PFilterParameter, Long > colId = new JInvTableColumn<>("Id");
                colId.setId("IPARAMNUM");
                colId.setFieldName("IPARAMNUM");
                JInvTableColumn< PFilterParameter, String > colName = new JInvTableColumn<>("Параметр");
                colName.setId("CPARAMDESCR");
                colName.setFieldName("CPARAMDESCR");
                JInvTableColumn< PFilterParameter, String > colParamValue = new JInvTableColumn<>("Значение");
                colParamValue.setId("colParamValue");
                colParamValue.setFieldName("CPARAMDEFAULT_CALC");
                tblPrm.setEditable(true);
                colParamValue.setEditable(true);

                colParamValue.setOnEditCommit(event -> {
                    event.getTableView().getItems().get(event.getTablePosition().getRow()).setCPARAMDEFAULT(event.getNewValue());
                });

                tblPrm.getColumns().addAll(colId, colName, colParamValue);

                // Редактировать ячейку "Значение" у параметра, при нажатии клавиши Enter
                tblPrm.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) {
                        final TablePosition< PFilterParameter, ? > tablePosition = tblPrm.getFocusModel().getFocusedCell();
                        //Platform.runLater(() -> {
                        tblPrm.edit(tablePosition.getRow(), tablePosition.getTableView().getColumns().get(2));
                        //});
                    }
                });

                dsParam.nativeQueryName("list").taskContext(tc).queryAllRows();

                DLBuilder.<IDataSet<PFrmFilter>, SQLDataSet<PFilterParameter>, PFrmFilter, Long>link( dsFilter, dsParam, PFrmFilter::getID, "IDFILTER", () -> true);
                DSFXAdapter.bind(dsParam, tblPrm, null, false);

                colParamValue.setCellFactory(param -> new ChoiceFilterController.EditingCell(tc));

                gp.getChildren().add(tblPrm);
                tabs.getTabs().add(new Tab("Параметры", gp));
            }

            tabs.getTabs().forEach(( t ) -> t.setClosable(false));

            return tabs;
        } catch(Throwable th) {
            JInvErrorService.handleException(getDialogPane(), th);
        }
        return new JInvLabel("Error getAutoFilter");
    }
}
