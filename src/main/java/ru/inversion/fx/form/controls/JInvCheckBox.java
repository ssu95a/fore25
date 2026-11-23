package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;

import java.util.Optional;

import static ru.inversion.fx.form.controls.Controls.*;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_SHOW_IN_FILTER;

/**
 *
 * @author ssu @
 */
public class JInvCheckBox extends CheckBox implements IJInvControl, IValidatableByChangeValueControl, IFilterControl {

    private static final EventHandler<KeyEvent> READ_ONLY_KEY_FILTER = event -> {
        var box = (JInvCheckBox) event.getSource();
        if (box.isReadOnly() && (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER)) {
            if (event.getEventType() == KeyEvent.KEY_PRESSED) {
                event.consume();
            } else if (event.getEventType() == KeyEvent.KEY_RELEASED) {
                // Скин должен сбросить своё состояние нажатой клавиши без вызова fire().
                box.disarm();
            }
        }
    };
    private static final EventHandler<MouseEvent> READ_ONLY_MOUSE_FILTER = event -> {
        var box = (JInvCheckBox) event.getSource();
        if (!box.isReadOnly()) {
            return;
        }
        var type = event.getEventType();
        if ((type == MouseEvent.MOUSE_PRESSED && event.getButton() == MouseButton.PRIMARY)
                || (type == MouseEvent.MOUSE_RELEASED && (event.getButton() == MouseButton.PRIMARY || box.isArmed()))
                || (type == MouseEvent.MOUSE_ENTERED && event.isPrimaryButtonDown())) {
            box.disarm();
            event.consume();
        }
    };
    private static final ChangeListener<Boolean> NATIVE_DISABLED_LISTENER = (property, oldValue, disabled) -> {
        var box = (JInvCheckBox) ((ReadOnlyBooleanProperty) property).getBean();
        box.refreshDisabledStyle();
    };

    private final RequiredState requiredState = new RequiredState(this);

    @Override
    public String getFieldName() {
        //return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
        return IJInvControl.super.getFieldName( );

    }

    @Override
    public void setFieldName(String fieldName) {
        IJInvControl.super.setFieldName(fieldName);
        //getProperties().put(CONTROL_FIELD_NAME, fieldName);
    }

    /**
     * Возвращает признак показывать ли колонку в фильтре
     */
    public boolean isShowInFilter() {
        return (Boolean) getProperties().getOrDefault(COLUMN_SHOW_IN_FILTER, Boolean.TRUE);
    }

    /**
     * Устанавливает признак показывать ли колонку в фильтре
     */
    public void setShowInFilter(boolean val) {
        getProperties().put(COLUMN_SHOW_IN_FILTER, val);
    }
    /**
     *
     */
    public Control setLabel(Label label) {
        if (label != null) {
            label.setLabelFor(this);
            textProperty().bind(label.textProperty());
        }
        return this;
    }

    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }

    public JInvCheckBox() {
        super();
        init();
    }

    public JInvCheckBox(String text) {
        super(text);
        init();
    }

    private void init() {
        readOnlyProperty().addListener((obs, oldValue, readOnly) -> applyReadOnly(readOnly));
        disabledProperty().addListener(NATIVE_DISABLED_LISTENER);
        JInvKeyboardManager.initNaviationOnNode(this);
        addEventFilter(KeyEvent.ANY, READ_ONLY_KEY_FILTER);
        addEventFilter(MouseEvent.ANY, READ_ONLY_MOUSE_FILTER);
        setMnemonicParsing(false);
        mnemonicParsingProperty().addListener( (v,o,n) -> {if (n) setMnemonicParsing(false);} );
    }

    private void applyReadOnly(boolean readOnly) {
        if (readOnly) {
            disarm();
        }
        if (!focusTraversableProperty().isBound()) {
            setFocusTraversable(!readOnly);
        }
        if (!mouseTransparentProperty().isBound()) {
            setMouseTransparent(readOnly);
        }
        refreshDisabledStyle();
    }

    private void refreshDisabledStyle() {
        pseudoClassStateChanged(DISABLED_GREY_STATE, isReadOnly() || isDisabled());
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        if (action != AccessibleAction.FIRE || !isReadOnly()) {
            super.executeAccessibleAction(action, parameters);
        }
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
        IJInvControl.super.setToolTipText(toolTipText);

//        if (toolTipText != null) {
//
//            if (getTooltip() != null) {
//                getTooltip().setText(toolTipText);
//            } else {
//                setTooltip(new Tooltip(toolTipText));
//            }
//        }
    }

    @Override
    public String getToolTipText() {
        return IJInvControl.super.getToolTipText();
    }

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
        return textProperty();
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
