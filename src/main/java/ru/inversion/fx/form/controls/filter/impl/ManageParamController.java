package ru.inversion.fx.form.controls.filter.impl;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.filter.entity.PFilterParameter;
import ru.inversion.fx.form.lov.JInvLOV;
import ru.inversion.fx.form.valid.Validator;

import java.sql.SQLException;

/**
 * FXML Controller class Добавление редактирование параметров
 *
 * @author perov
 * @version 1.0.1
 */
public class ManageParamController extends JInvFXFormController<PFilterParameter> {

    @FXML
    private JInvTextField edNum;

    @FXML
    private JInvTextField edName;

    @FXML
    private JInvTextField edValue;

    @FXML
    private JInvTextField edCursor;

    @FXML
    private JInvCheckBox edSave;

    private IParamWork paramWork;

    // Перекрыто, чтобы не проверялась версионность хелпового контроллера
    protected void beforeInit() throws Exception {

        initFormStateDecorator();
        dataObject = configureDataObject();
        fxEntity = createFXEntity();
    }

    @Override
    protected boolean onOK() {
        try {
            getFXEntity().commit();

            dbOperation();

            return true;
        } catch (SQLException th) {
            JInvErrorService.handleException(null, th);
        }
        return false;
    }

    @Override
    protected void init() throws Exception {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        paramWork = new ParamWork(getTaskContext().getConnection());
        getValidMan().addRequiredControl(edNum, edName);

        getValidMan().bindValidators2Control(edValue, new Validator() {
            @Override
            public Validator.Result validate(Object value) {
                if (value == null || value.toString().isEmpty()) {
                    return null;
                }
                if (value.toString().startsWith("&")) {
                    return ParamWork.validateValue(getTaskContext().getConnection(), (String) value);
                }
                return null;
            }
        }
        );

        initLov();
    }

    private void dbOperation() throws SQLException {

        switch (getFormMode()) {
            case VM_INS:
                paramWork.doInsert(getDataObject());
                break;
            case VM_EDIT:
                paramWork.doUpdate(getDataObject());
                break;
            case VM_DEL:
                paramWork.doDelete(getDataObject());
                break;
            default:
                break;
        }

    }

    private void initLov() {
        JInvLOV lovCursor = new JInvLOV();
        lovCursor.setTaskContext(getTaskContext());
        lovCursor.setSqlSelect("select AP_Cursor_Type.cursor_id id,\n"
            + "       AP_Cursor_Type.cursor_name name\n"
            + "  from AP_Cursor_Type\n"
            + "  where AP_Cursor_Type.cursor_type='P'\n"
            + "  order by 1");
        lovCursor.addColumn("ID", String.class, "ID", -1, edCursor.textProperty());
        lovCursor.addColumn("NAME", String.class, "NAME", -1, new SimpleStringProperty());
        edCursor.setLOV(lovCursor);
    }

}
