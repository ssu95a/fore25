/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.control.*;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.action.IAction;

import java.util.NoSuchElementException;
import java.util.Optional;

import static ru.inversion.fx.form.controls.Controls.*;

/**
 *
 * @author psh
 */
public final class JInvRadioGroup extends ToggleGroup implements IJInvControl, IFilterControl, ICustomValueControl<String> {
    private final static String INITIALIZED = "ru.inversion.radiogroup.initialized";

    private final StringProperty valueProperty = new SimpleStringProperty (this, "value");
    private Label label;

    public StringProperty valueProperty() {
        return valueProperty;
    }

    public String getValue() {
        return valueProperty.get();
    }

    public void setValue(String value) {
        valueProperty.set(value);
    }

    public JInvRadioGroup(String fieldName, Parent contentPane) {
        super();
        setFieldName(fieldName);

        Controls.addNonControlComponentToContainer(this, contentPane);

        selectedToggleProperty().addListener((ObservableValue<? extends Toggle> observable, Toggle oldValue, Toggle newValue) -> {
            if (newValue != null) {

                JInvRadioButton button = (JInvRadioButton) newValue;
                String value = button.getValue();
                if (value == null || !value.isEmpty()) {
                    valueProperty.set(value);
                }
            }
        });

        valueProperty.addListener((ObservableValue<? extends Object> observable, Object oldValue, Object newValue) -> {
            refreshSelection();
        });

        readOnlyProperty().addListener( (obs, oldV, newV) ->
                getToggles().forEach(t ->{
                    if (t instanceof Control) {
                        Controls.disableControl( (Control)t, obs.getValue());
                    }
                }));
    }

    @Override
    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    @Override
    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
    }

    @Override
    public Tooltip getTooltip() {
        return null;
    }

    @Override
    public void setTooltip(Tooltip value) {

    }

    public final String getId() {
        return (String) getProperties().getOrDefault(CONTROL_ID, null);
    }

    public final void setId(String id) {
        getProperties().put(CONTROL_ID, id);
    }

    public void refreshSelection() {

        String groupValue = valueProperty.get();

        Optional<JInvRadioButton> optionalButton =
        getToggles().stream()
                    .map((Toggle t) -> (JInvRadioButton) t)
                    .filter((JInvRadioButton t) -> {
                        String buttonValue = t.getValue();
                        return (buttonValue == null && groupValue == null)
                            || (buttonValue != null && buttonValue.equals(groupValue)); })
                    .findFirst();

        if (optionalButton.isPresent()) {
            optionalButton.get().setSelected(true);
        } else {
            if (groupValue == null || !isInitialized()) {
                selectToggle(null);
            } else {
                JInvErrorService.handleException( getController(),  new NoSuchElementException(getFieldName() + " – value not found: " + groupValue));
            }
        }
    }

    @Override
    public Control setLabel(Label label) {
        this.label = label;
        return null;
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public void setAction(IAction action) {
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {
        return Controls.<T>getDsAdapterFromControl(this);
    }

    @Override
    public void setToolTipText(String toolTipText) {

    }

    @Override
    public String getToolTipText() {
        return null;
    }

    @Override
    public StringProperty labelTextProperty() {
        return null;
    }

    public void setText(String text) {
        setLabel(new Label(text));
    }

   /**
     * Возвращает Идентификатор группы в F7 фильтре
     *
     * @return
     */
    @Override
    public String getIdF7FilterGroup() {

        return (String) getProperties().getOrDefault(F7FILTER_GROUP_ID, null);
    }

    /**
     * Устанавливает Идентификатор группы в F7 фильтре
     *
     * @param idF7FilterGroup
     */
    @Override
    public void setIdF7FilterGroup(String idF7FilterGroup) {
        getProperties().put(F7FILTER_GROUP_ID, idF7FilterGroup);
    }

    /**
     * Возвращает порядок следования в группе F7 фильтра
     *
     * @return
     */
    @Override
    public Integer getOrderInF7FilterGroup() {

        return (Integer) getProperties().getOrDefault(F7FILTER_ORDER_IN_GROUP, null);
    }

    /**
     * Устанавливает порядок следования в группе F7 фильтра
     *
     * @param orderInF7FilterGroup
     */
    @Override
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

    public void setInitialized(boolean initialized){
        setProperty(INITIALIZED,initialized);
    }

    public boolean isInitialized(){
        return getProperty(INITIALIZED,false);
    }

    @Override
    public String getCustomValue() {
        return getValue ();
    }

    @Override
    public void setCustomValue(String valueToBeSet) {
        setValue (valueToBeSet);
    }
    
    public static JInvRadioGroup create (String fieldName, Parent contentPane)
    {
        return new JInvRadioGroup (fieldName, contentPane);
    }        
}
