package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static ru.inversion.fx.form.controls.Controls.*;

/**
 *
 * @author ssu @
 * @param <T>
 * @param <R>
 */
public class JInvChoiceBox<T, R> extends ChoiceBox<T> implements IJInvControl, IValidatableByChangeValueControl, IFilterControl {

    private Function<T, R> valueFactory;
    private final RequiredState requiredState = new RequiredState(this);
    private StringProperty textLabelProperty = new SimpleStringProperty("");

    public Function<T, R> getValueFactory() {
        return valueFactory;
    }

    public void setValueFactory(Function<T, R> valueFactory) {
        this.valueFactory = valueFactory;
    }

    public void setSelectedValue(R value) {

        if (value != null) {

            for (T entry : getItems()) {

                if (valueFactory != null) {
                    R currentEntryValue = valueFactory.apply(entry);
                    if (currentEntryValue != null && currentEntryValue.equals(value)) {
                        getSelectionModel().select(entry);
                        break;
                    }
                } else if (entry.equals(value)) {
                    getSelectionModel().select(entry);
                    break;
                }
            }
        }
    }

    @Override
    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    @Override
    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
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

    public JInvChoiceBox() {
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /**
     *
     */
    public JInvChoiceBox(ObservableList<T> items) {
        super(items);
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
    }

    /**
     *
     */
    public <E extends Enum<E>> void setEnumItems(Class<T> enumClass) {
        setItems(FXCollections.observableList(Arrays.asList(enumClass.getEnumConstants())));
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
    public void setRequiredState(RequiredStateEnum state) {
        requiredState.setState(state);
    }

    @Override
    public RequiredStateEnum getRequiredState() {
        return requiredState.getState();
    }

    @Override
    public ObjectProperty<RequiredStateEnum> requiredStateProperty() {
        return requiredState.stateProperty();
    }

    @Override
    public void setRequired(boolean val) {
        if (val) {
            setRequiredState(RequiredStateEnum.REQUIRED);
        } else {
            setRequiredState(RequiredStateEnum.NOT_REQUIRED);
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
    public Integer getOrderInF7FilterGroup() {

        return (Integer) getProperties().getOrDefault(F7FILTER_ORDER_IN_GROUP, null);
    }

    /**
     * Устанавливает порядок следования в группе F7 фильтра
     *
     * @param orderInF7FilterGroup
     */
    public void setOrderInF7FilterGroup(Integer orderInF7FilterGroup) {
        getProperties().put(F7FILTER_ORDER_IN_GROUP, orderInF7FilterGroup);
    }

    @Override
    public void setIndexSearchAllowed(boolean value) {
        getProperties().put(INDEX_SEARCH_ALLOWED, value);
    }

    @Override
    public boolean isIndexSearchAllowed() {
             return (Boolean) getProperties().getOrDefault(INDEX_SEARCH_ALLOWED, Boolean.FALSE);

    }
}
