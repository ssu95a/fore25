package ru.inversion.fx.form.controls.filter.impl;

import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterGroup;

public class EditFilterGroupController extends JInvFXFormController<PFrmFilterGroup> {

    private FilterGroupDao filterGroupDao;

    @Override
    protected void init() throws Exception {
        filterGroupDao = new FilterGroupDao(getTaskContext());
    }

    @Override
    protected boolean onOK() {
        boolean result = true;

        try {
            final Long oldId = getDataObject().getID();
            getFXEntity().commit();
            PFrmFilterGroup frmFilterGroup = getDataObject();

            switch (getFormMode()) {
                case VM_INS:
                    filterGroupDao.save(frmFilterGroup);
                    break;
                case VM_EDIT:
                    filterGroupDao.update(frmFilterGroup, oldId);
                    break;
                case VM_DEL:
                    filterGroupDao.delete(frmFilterGroup);
                    break;
            }

        } catch (Throwable ex) {
            JInvErrorService.handleException(getViewContext(), ex);
            result = false;
        }

        return result;
    }
}
