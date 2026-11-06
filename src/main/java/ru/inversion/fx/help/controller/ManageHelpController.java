package ru.inversion.fx.help.controller;

import javafx.fxml.FXML;
import ru.inversion.dataset.ParametersByName;
import ru.inversion.db.rs.RSUtils;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.help.dao.HelpDao;
import ru.inversion.fx.help.dao.impl.HelpDaoImpl;
import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.utils.S;

import java.sql.SQLException;

/**
 * Класс котроллер
 *
 * @author perov
 * @version 1.0.0
 */
public class ManageHelpController extends JInvFXFormController<PHelp> {

    @FXML
    private JInvTextField edForm;

    private HelpDao helpDao = new HelpDaoImpl();

    private String oldFormValue;

    // Перекрыто, чтобы не проверялась версионность хелпового контроллера
    protected void beforeInit() throws Exception {

        initFormStateDecorator();
        dataObject = configureDataObject();
        fxEntity = createFXEntity();
    }

    @Override
    protected void init() throws Exception {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        Boolean blockFormName = (Boolean) getInitParameter("BLOCK_FORM_NAME", Boolean.FALSE);
        if (blockFormName) {
            edForm.setEditable(!blockFormName);
        }

        if (getFormMode() == FormModeEnum.VM_EDIT
            &&
            getDataObject() != null
            &&
            S.isNotNullOrEmpty(getDataObject().getFORM())) {
            oldFormValue = getDataObject().getFORM();
        }
    }

    @Override
    protected boolean onOK() {
        try {

            getFXEntity().commit();

            switch (this.getFormMode()) {
                case VM_INS:
                    if (insert()) {
                        break;
                    } else {
                        return false;
                    }
                case VM_EDIT:
                    update();
                    break;
                case VM_DEL:
                    delete();
                    break;
            }

            return true;
        } catch (Throwable th) {
            JInvErrorService.handleException(null, th);
        }
        return false;

    }

    private boolean insert() throws SQLException {

        // Если такая запись уже есть в таблице, то спрашиваем заменить ли
        String formName = getDataObject().getFORM();
        String locale = getDataObject().getCLOCALE();
        Iterable<PHelp> iter = RSUtils.createIterable(getTaskContext().getConnection(), PHelp.class,
                "form = :FORM_NAME and CLOCALE = :CLOCALE", null, (ParametersByName) parameterName -> {
                    switch (parameterName) {
                        case "FORM_NAME":
                            return formName;
                        case "CLOCALE":
                            return locale;
                    }
                    return null;
                });
        if (iter.iterator().hasNext()) {
            boolean result = Alerts.yesNo(getViewContext(), "",
                String.format(bundle.getString("ALERT_FORM_ALREADY_EXISTS"), formName));
            if (!result) {
                return false;
            } else {
                // Делаем это для того, чтобы произошла не вставка записи, а ее замена в таблице
                formMode = FormModeEnum.VM_EDIT;
            }
        }

        helpDao.insertHelpForm(getTaskContext().getConnection(), getDataObject());
        return true;
    }

    private void delete() throws SQLException {
        helpDao.deleteHelpForm(getTaskContext().getConnection(), getDataObject());
    }

    private void update() throws SQLException {

        helpDao.updateHelpForm(getTaskContext().getConnection(), getDataObject(), oldFormValue);
    }
}
