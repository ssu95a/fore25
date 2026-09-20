package ru.inversion.fx.form.controls;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Control;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_EDIT;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_INS;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_NONE;
import ru.inversion.fx.form.IFXEntity;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.valid.MarkType;
import ru.inversion.fx.form.valid.ValidMan;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.U;

public class RequiredState {

    private final ObjectProperty<IValidatableControl.RequiredStateEnum> state;

    private final IJInvControl control;

    RequiredState(IJInvControl control) {
        this.control = control;
        this.state = new SimpleObjectProperty<IValidatableControl.RequiredStateEnum>(IValidatableControl.RequiredStateEnum.MODEL){
            @Override
            public Object getBean() {
                return control;
            }
        };

        state.addListener((observable, oldValue, newValue) -> {
            if ( !(control instanceof Control) ) {
                return;
            }

            Platform.runLater( () -> {
                final JInvFXFormController<?> controller = control.getController();

                if ( controller != null ) {
                    tryBindController( (Control) control, observable, controller );
                } else {
                    //Если контроллера нет, то попробуем привязаться снова по смене сцены
                    ( (Control) control ).sceneProperty().addListener( ( v,o,n ) -> {
                        final JInvFXFormController<?> newController = control.getController();
                        if ( newController != null ){
                            tryBindController( (Control) control, observable, newController );
                        }
                    } );
                }
            } );
        });

    }

    private void tryBindController(Control control, ObservableValue<? extends IValidatableControl.RequiredStateEnum> state, JInvFXFormController<?> controller ) {
        // Прерываем дальнейшие действия если текущий fromMode ни один из указанных.
        // Причина в том, что изменения stat'а уже отслеживается(подробности в классе Binder)
        if ( U.notIn( controller.getFormMode(), VM_INS, VM_EDIT, VM_NONE ) ) return;
        final ValidMan validMan = controller.getValidMan();
        if ( state.getValue() == IValidatableControl.RequiredStateEnum.REQUIRED) {
            validMan.addRequiredControl( control );
            //Раскрашиваем контрол
            validMan.getDecorator().markAndTrack( control, MarkType.REQUIRED );
        } else {
            validMan.removeRequiredControl( control );
        }
    }

    public IValidatableControl.RequiredStateEnum getState() {
        return state.get();
    }

    public ObjectProperty<IValidatableControl.RequiredStateEnum> stateProperty() {
        return state;
    }

    public void setState(IValidatableControl.RequiredStateEnum state) {
        this.state.set(state);
    }

    public void setState(boolean val) {
        if (val) {
            state.set(IValidatableControl.RequiredStateEnum.REQUIRED);
        } else {
            state.set(IValidatableControl.RequiredStateEnum.NOT_REQUIRED);
        }
    }

    public boolean isRequired() {
        boolean defValue = false;

        switch (state.get()) {
            case NOT_REQUIRED:
                return false;
            case REQUIRED:
                return true;
            case MODEL: {
                String fieldName = control.getFieldName();
                if (fieldName != null && !fieldName.isEmpty()) {
                    JInvFXFormController controller = control.getController();
                    if (controller != null) {
                        IFXEntity entity = controller.getFXEntity();
                        if (entity != null) {
                            IEntityProperty descriptor = entity.getPropertyDescriptor(fieldName);
                            if (descriptor != null) {
                                return descriptor.isRequired();
                            }
                        }
                    }
                }
                return false;
            }
            default:
                return defValue;
        }
    }

}
