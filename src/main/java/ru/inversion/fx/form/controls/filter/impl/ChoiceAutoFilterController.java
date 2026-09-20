package ru.inversion.fx.form.controls.filter.impl;

import javafx.fxml.FXML;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilter;

import java.util.ResourceBundle;

/**
 * Выбор автофильтра при запуске формы с таблицей
 *
 * вызывается в том случае, если на таблице установлено несколько автофильтров
 * */
public class ChoiceAutoFilterController extends JInvFXFormController<PFrmFilter> {

    @FXML
    private JInvTable tblFlt;

    private final XXIDataSet<PFrmFilter> dsFilter = new XXIDataSet<>();

    protected void init( ResourceBundle rb )
    {
        ((Stage)tblFlt.getScene().getWindow()).initModality(Modality.APPLICATION_MODAL);
    }

    /** */
    private void initDS() throws Exception {

        dsFilter.taskContext( getTaskContext() ).rowClass( PFrmFilter.class ).nativeQueryName("autoList").queryAllRows();
        dsFilter.setParameter  ( "FORM_NAME",  getInitProperties().get("FORM_NAME") );
        dsFilter.setParameter  ( "BLOCK_NAME", getInitProperties().get("BLOCK_NAME"));
        dsFilter.setOrderBy    ( "cwherename" );

        DSFXAdapter.bind( dsFilter, tblFlt) ;
    }

    /** */
    @Override
    protected void init() throws Exception {
        initDS();
        tblFlt.executeQuery();
    }

    /** */
    @Override
    protected boolean onOK() {
        getInitProperties().put( "FILTER_ID", dsFilter.getCurrentRow().getID() );
        return true;
    }
}
