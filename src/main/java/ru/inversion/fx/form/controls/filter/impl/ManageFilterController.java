package ru.inversion.fx.form.controls.filter.impl;

import javafx.application.Platform;
import javafx.fxml.FXML;
import ru.inversion.db.expr.SQLExpressionException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.JInvTextArea;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilterFull;
import ru.inversion.fx.form.lov.JInvEntityLov;
import ru.inversion.fx.form.lov.internal.PUsrInternal;

import java.sql.SQLException;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import static ru.inversion.fx.form.controls.filter.impl.FilterListController.EDIT_ALL;

/**
 * Контроллер для добавления, редактирования, удаления фильтров
 *
 * @author perov
 */
public class ManageFilterController extends JInvFXFormController<PFrmFilterFull> {
    final private static ResourceBundle BUNDLE_FILTER = ResourceBundle.getBundle("filter");

    @FXML private JInvTextField CFORMNAME;
    @FXML private JInvTextField CBLOCKNAME;
    @FXML private JInvTextField CUSER;
    @FXML private JInvTextField CWHERENAME;
    @FXML private JInvTextArea edCWHEREBLK;
    private boolean isEditAll;

    @Override
    protected void init() throws Exception {

        super.init();

        if( getInitProperties().containsKey(EDIT_ALL) )
            isEditAll = (Boolean) getInitProperties().get(EDIT_ALL);

        JInvEntityLov<PUsrInternal, String> lov = new JInvEntityLov<>(PUsrInternal.class);
        lov.setResourceBundle(BUNDLE_FILTER);
        CUSER.setLOV(lov);
        CUSER.setValidateFromLOV(true);

        Stream.of(CFORMNAME, CBLOCKNAME, CUSER).forEach(c -> c.setReadOnly(!isEditAll));

        edCWHEREBLK.setFont( BaseApp.APP().viewPrefService().getCodeFont() );

        if( getFormMode() == FormModeEnum.VM_INS )
            Platform.runLater( ()->CWHERENAME.requestFocus() );
        else if( getFormMode() == FormModeEnum.VM_EDIT )
            Platform.runLater( ()->edCWHEREBLK.requestFocus() );
    }

    // Перекрыто, чтобы не проверялась версионность хелпового контроллера
    protected void beforeInit() throws Exception {

        initFormStateDecorator();

        dataObject = configureDataObject();
          fxEntity = createFXEntity();
    }
//
//    @Override
//    protected boolean onOK() {
//        getFXEntity().commit();
//        initLongOperation(() -> {
//            try {
//                dbOperation(getFormMode());
//            } catch (SQLException | SQLExpressionException e) {
//                JInvErrorService.handleException(null, e);
//            } finally {
//                closeNow(FormReturnEnum.RET_OK);
//            }
//        });
//        return false;
//    }

    private void dbOperation(FormModeEnum formMode) throws SQLException, SQLExpressionException {
        switch (getFormMode()) {
            case VM_INS:
                FilterWork.saveFilter(getTaskContext().getConnection(),
                    getDataObject().getCFORMNAME(),
                    getDataObject().getCWHEREBLK(),
                    getDataObject().getCBLOCKNAME(),
                    getDataObject().getCWHERENAME());
                break;
            case VM_EDIT:
                FilterWork.editFilter(getTaskContext().getConnection(), getDataObject());
                break;
            case VM_DEL:
                FilterWork.delFilter(getTaskContext().getConnection(), getDataObject().getID());
                break;
            default:
                break;
        }
    }

}
