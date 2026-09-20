package ru.inversion.fx.form.controls.filter.impl;

import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.AbstractBaseController.FormModeEnum;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.filter.entity.PFilterParameter;
import ru.inversion.tc.TaskContext;

import java.util.ResourceBundle;
import java.util.function.BiConsumer;

/**
 * 
 * @author perov
 */
abstract class ParameterToolBar extends JInvToolBar {

    private final ResourceBundle bundle;

    ParameterToolBar(ResourceBundle bundle) {
        this.bundle = bundle;
        Button btnPrmCreate = ActionFactory.createButton(ActionFactory.ActionTypeEnum.CREATE, (a) -> {
            doActionPrm(AbstractBaseController.FormModeEnum.VM_INS);
        });
        Button btnPrmUpdate = ActionFactory.createButton(ActionFactory.ActionTypeEnum.UPDATE, (a) -> {
            doActionPrm(AbstractBaseController.FormModeEnum.VM_EDIT);
        });
        Button btnPrmDelete = ActionFactory.createButton(ActionFactory.ActionTypeEnum.DELETE, (a) -> {
            doActionPrm(AbstractBaseController.FormModeEnum.VM_DEL);
        });
        Button btnPrmRefresh = ActionFactory.createButton(ActionFactory.ActionTypeEnum.REFRESH, (a) -> {
            refresh();
        });
        getItems().addAll(btnPrmCreate, btnPrmUpdate, btnPrmDelete, new Separator(Orientation.VERTICAL), btnPrmRefresh);
    }


    private void doActionPrm(AbstractBaseController.FormModeEnum mode)
    {
        PFilterParameter entity = null;

        if( mode.equals(FormModeEnum.VM_INS) )
        {
            entity = new PFilterParameter();
            entity.setIDFILTER(getIDFilter());

        }
        else if (mode == FormModeEnum.VM_EDIT || mode == FormModeEnum.VM_DEL) {
            entity = getEntity();
        }

        if (entity != null && entity.getIDFILTER() != null)
        {
            new FXFormLauncher<>( getTC(), getVC(), ManageParamController.class, bundle )
                .dialogMode(mode)
                .bundle(bundle)
                .dataObject(entity)
                .modal(true)
                .clb( getClb())
            .show();
        }
    }
    
    /**
     * Кнопка обновить
     */
    abstract void refresh();
    /**
     * Текущая строка из датасет для обновления/удаления, или новый экземпляр для вставки
     * @return 
     */
    abstract PFilterParameter getEntity();
    /**
     * Id фильтра для вставки
     * @return 
     */
    abstract Long getIDFilter();
    /**
     * TaskContext
     * @return 
     */
    abstract TaskContext getTC();
    /**
     * ViewContext
     * @return 
     */
    abstract ViewContext getVC();
    /**
     * Колбек
     * @return 
     */
    abstract BiConsumer<JInvFXFormController.FormReturnEnum, JInvFXFormController> getClb();
}
