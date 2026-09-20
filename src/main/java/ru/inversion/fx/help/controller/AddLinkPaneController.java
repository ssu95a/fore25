package ru.inversion.fx.help.controller;

import javafx.fxml.FXML;
import ru.inversion.fx.form.JInvFXDialogController;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.lov.JInvLOV;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.fx.help.entity.PHelp;

/**
 * Внутренний класс.
 * Класс реализует контроллер для добавления ссылок.
 *
 * @author perov
 * @version 1.0.0
 */
public class AddLinkPaneController extends JInvFXDialogController<PHelp> {

    @FXML
    private JInvTextField edForm;

    @FXML
    private JInvLOVButton btnLovForm;
    private JInvLOV lovForm;

    @Override
    protected boolean onOK() {
        getDataObject().setFORM(edForm.getText());
        return true;
    }

    @Override
    protected void init() throws Exception {
        super.init(); //To change body of generated methods, choose Tools | Templates.
        initLov();
        getValidMan().addRequiredControl(edForm);
    }

    private void initLov() {
        lovForm = new JInvLOV();
        btnLovForm.setTextField(edForm);
        lovForm.setSqlSelect("SELECT form FROM JF_HELP order by form");
        lovForm.setTaskContext(getTaskContext());
        lovForm.addColumn("FORM", String.class, getBundleString("NAIMENOVANIE"), -1, edForm.textProperty());
        edForm.setLOV(lovForm, true);
    }
}
