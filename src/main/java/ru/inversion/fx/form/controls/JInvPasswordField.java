package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tooltip;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;

import java.util.Optional;

import static ru.inversion.fx.form.controls.Controls.*;

/**
 *
 * @author ssu @
 */
public class JInvPasswordField extends PasswordField implements IJInvControl, IValidatableControl {

    private final RequiredState requiredState = new RequiredState(this);
    private StringProperty textLabelProperty = new SimpleStringProperty("");

    @Override
    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    @Override
    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
    }

    public JInvPasswordField() {
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /**
     *
     */
    public Control setLabel(Label label) {
        if (label != null) {
            label.setLabelFor(this);
            textLabelProperty.bind(label.textProperty());
        }
        return this;
    }

    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }

    @Override
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {

        return Controls.<T>getDsAdapterFromControl(this);
    }

    @Override
    public void setToolTipText(String toolTipText) {
        if (toolTipText != null) {

            if (getTooltip() != null) {
                getTooltip().setText(toolTipText);
            } else {
                setTooltip(new Tooltip(toolTipText));
            }
        }
    }
    /*
    @Override
    public Optional<String> getToolTipText() {
        if (getTooltip() != null && !getTooltip().getText().isEmpty()) {
            return Optional.ofNullable(getTooltip().getText());
        } else {
            return Optional.empty();
        }
    }
    */
    @Override
    public void setRequiredState(IValidatableControl.RequiredStateEnum state) {
        requiredState.setState(state);
    }

    @Override
    public IValidatableControl.RequiredStateEnum getRequiredState() {
        return requiredState.getState();
    }

    @Override
    public ObjectProperty<IValidatableControl.RequiredStateEnum> requiredStateProperty() {
        return requiredState.stateProperty();
    }

    @Override
    public void setRequired(boolean val) {
        if (val) {
            setRequiredState(IValidatableControl.RequiredStateEnum.REQUIRED);
        } else {
            setRequiredState(IValidatableControl.RequiredStateEnum.NOT_REQUIRED);
        }
    }

    @Override
    public boolean isRequired() {
        return requiredState.isRequired();
    }

    @Override
    public StringProperty labelTextProperty() {
        return textLabelProperty;
    }

    /**
     * Возвращает Идентификатор группы в F7 фильтре
     *
     * @return
     */
    public String getIdF7FilterGroup() {

        return (String) getProperties().getOrDefault(F7FILTER_GROUP_ID, null);
    }

    /**
     * Устанавливает Идентификатор группы в F7 фильтре
     *
     * @param idF7FilterGroup
     */
    public void setIdF7FilterGroup(String idF7FilterGroup) {
        getProperties().put(F7FILTER_GROUP_ID, idF7FilterGroup);
    }

    /**
     * Возвращает порядок следования в группе F7 фильтра
     *
     * @return
     */
    public int getOrderInF7FilterGroup() {

        return (Integer) getProperties().getOrDefault(F7FILTER_ORDER_IN_GROUP, 0);
    }

    /**
     * Устанавливает порядок следования в группе F7 фильтра
     *
     * @param orderInF7FilterGroup
     */
    public void setOrderInF7FilterGroup(int orderInF7FilterGroup) {
        getProperties().put(F7FILTER_ORDER_IN_GROUP, orderInF7FilterGroup);
    }
}
