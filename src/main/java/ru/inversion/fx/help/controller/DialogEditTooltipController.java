package ru.inversion.fx.help.controller;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.form.JInvFXEntityController;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.help.entity.PHelpBundle;

/**
 * FXML Controller class
 *
 * @author perov
 */
public class DialogEditTooltipController extends JInvFXEntityController<PHelpBundle> {
    public JInvTextArea helpText;

    @Override
    protected void afterInit() throws AppException {
        super.afterInit();
        helpText.requestFocus();
    }
}
