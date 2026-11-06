package ru.inversion.fx.form.controls.filter.impl;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterGroup;

import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.*;

public class ViewFilterGroupController extends JInvFXFormController<PFrmFilterGroup> {

    @FXML
    private JInvToolBar toolBar;
    @FXML
    private JInvTable<PFrmFilterGroup> filterGroupTable;
    private final XXIDataSet<PFrmFilterGroup> dsFilterGroup = new XXIDataSet<>();

    @Override
    protected void init() throws Exception {

        setTitle(getBundleString("FILTER_GROUP.TITLE"));

        initDateSet();
        initToolBar();

        filterGroupTable.executeQuery();
    }

    private void initDateSet() throws Exception {
        dsFilterGroup.setTaskContext(getTaskContext());
        dsFilterGroup.setRowClass(PFrmFilterGroup.class);
        dsFilterGroup.setNativeQueryName("list");
        DSFXAdapter.bind(dsFilterGroup, filterGroupTable, null, false);
    }

    private void initToolBar() {
        toolBar.setStandartActions(CREATE, UPDATE, DELETE, REFRESH);
        filterGroupTable.setToolBar(toolBar);
        filterGroupTable.setAction(CREATE, (ActionEvent event) -> {
            doOperation(FormModeEnum.VM_INS);
        });

        filterGroupTable.setAction(UPDATE, (ActionEvent event) -> {
            doOperation(FormModeEnum.VM_EDIT);
        });

        filterGroupTable.setAction(DELETE, (ActionEvent event) -> {
            doOperation(FormModeEnum.VM_DEL);
        });

        filterGroupTable.setAction(REFRESH, (ActionEvent event) -> {
            filterGroupTable.executeQuery();
        });
    }

    private void doOperation(JInvFXFormController.FormModeEnum mode) {
        PFrmFilterGroup p = null;

        switch (mode) {
            case VM_INS:
                p = new PFrmFilterGroup();
                break;
            case VM_EDIT:
            case VM_DEL:
                p = dsFilterGroup.getCurrentRow();
                break;
        }

        if (p != null) {
            openEditFilterGroup(p, mode);
        }
    }

    private void openEditFilterGroup(PFrmFilterGroup filterGroup, FormModeEnum mode) {
        new FXFormLauncher<>(getTaskContext(), getViewContext(), EditFilterGroupController.class, bundle )
            .dataObject( filterGroup )
            .modal     ( true )
            .dialogMode( mode )
            .callback  ( this::doModalResult )
        .show();
    }

    private void doModalResult(JInvFXFormController.FormReturnEnum ok, JInvFXFormController<PFrmFilterGroup> dctl) {

        try {
            if (JInvFXFormController.FormReturnEnum.RET_OK == ok) {
                switch (dctl.getFormMode()) {
                    case VM_INS: {
                        dsFilterGroup.insertRow(dctl.getDataObject(), IDataSet.InsertRowModeEnum.AFTER_CURRENT, true);
                        dsFilterGroup.refreshCurrentRowFromDB();
                    }
                    break;
                    case VM_EDIT: {
                        dsFilterGroup.updateCurrentRow(dctl.getDataObject());
                        dsFilterGroup.refreshCurrentRowFromDB();
                    }
                    break;
                    case VM_DEL:
                        dsFilterGroup.removeCurrentRow();
                        break;
                    default:
                        break;
                }
            }
        } catch (Throwable ex) {
            handleException(ex);
        }
    }
}
