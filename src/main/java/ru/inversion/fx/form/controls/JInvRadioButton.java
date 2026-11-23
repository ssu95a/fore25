/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.NodeOrientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;

import java.util.Optional;

import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;

/**
 *
 * @author mik
 */
public class JInvRadioButton extends RadioButton implements IJInvControl {

    private static final EventHandler<KeyEvent> READ_ONLY_KEY_FILTER =
            event -> ((JInvRadioButton) event.getSource()).filterReadOnlyKey(event);
    private static final EventHandler<MouseEvent> READ_ONLY_MOUSE_FILTER = event -> {
        JInvRadioButton button = (JInvRadioButton) event.getSource();
        if (button.isEffectivelyReadOnly()) {
            button.disarm();
            event.consume();
        }
    };
    private static final EventHandler<KeyEvent> GROUP_ARROW_FILTER = event -> {
        ToggleButton source = (ToggleButton) event.getSource();
        if (isUnmodified(event) && blocksToggleTraversal(source, event.getCode())) {
            event.consume();
        }
    };
    private static final ListChangeListener<Toggle> GROUP_TOGGLES_LISTENER = JInvRadioButton::updateGroupArrowFilters;

    private StringProperty textLabelProperty = new SimpleStringProperty("");

    /**
     * Значение, при котором кнопка активна.
     */
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JInvRadioButton() {
        this(null);
    }

    public JInvRadioButton(String text) {
        super(text);
        init();
    }

    private void init() {
        readOnlyProperty().addListener((obs, oldValue, newValue) -> refreshReadOnly());
        toggleGroupProperty().addListener((obs, oldGroup, newGroup) -> {
            if (isReadOnlyGroup(oldGroup) || isReadOnlyGroup(newGroup)) {
                refreshReadOnly();
            }
            if (newGroup != null && !(newGroup instanceof JInvRadioGroup)) {
                var toggles = newGroup.getToggles();
                toggles.forEach(toggle -> setGroupArrowFilter(toggle, true));
                toggles.removeListener(GROUP_TOGGLES_LISTENER);
                toggles.addListener(GROUP_TOGGLES_LISTENER);
            }
        });
        JInvKeyboardManager.initNaviationOnNode(this);
        addEventFilter(KeyEvent.ANY, READ_ONLY_KEY_FILTER);
        addEventFilter(MouseEvent.MOUSE_PRESSED, READ_ONLY_MOUSE_FILTER);
        addEventFilter(MouseEvent.MOUSE_RELEASED, READ_ONLY_MOUSE_FILTER);
        setMnemonicParsing(false);
        mnemonicParsingProperty().addListener( (v,o,n) -> {if (n) setMnemonicParsing(false);} );
    }

    private static boolean isReadOnlyGroup(ToggleGroup group) {
        return group instanceof JInvRadioGroup legacyGroup && legacyGroup.isReadOnly();
    }

    private boolean isEffectivelyReadOnly() {
        return isReadOnly() || isReadOnlyGroup(getToggleGroup());
    }

    void refreshReadOnly() {
        boolean readOnly = isEffectivelyReadOnly();
        if (readOnly) {
            disarm();
        }
        if (!focusTraversableProperty().isBound()) {
            setFocusTraversable(!readOnly);
        }
        if (!mouseTransparentProperty().isBound()) {
            setMouseTransparent(readOnly);
        }
        pseudoClassStateChanged(Controls.DISABLED_GREY_STATE, readOnly || isDisabled());
    }

    private void filterReadOnlyKey(KeyEvent event) {
        if (!isUnmodified(event)) {
            return;
        }
        if (event.getEventType() == KeyEvent.KEY_RELEASED && isEffectivelyReadOnly()) {
            // Скин должен получить отпускание клавиши и сбросить своё состояние.
            if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER) {
                disarm();
            }
            return;
        }
        if (event.getEventType() != KeyEvent.KEY_PRESSED) {
            return;
        }
        boolean block = event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER
                ? isEffectivelyReadOnly() : blocksToggleTraversal(this, event.getCode());
        if (block) {
            event.consume();
        }
    }

    private static boolean isUnmodified(KeyEvent event) {
        return !event.isShiftDown() && !event.isControlDown() && !event.isAltDown() && !event.isMetaDown();
    }

    private static boolean blocksToggleTraversal(ToggleButton source, KeyCode key) {
        ToggleGroup group = source.getToggleGroup();
        if (group == null || !key.isArrowKey()) {
            return false;
        }
        // Стрелка в скине JavaFX выбирает соседний toggle напрямую, без fire().
        boolean rightToLeft = source.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT;
        int direction = switch (key) {
            case LEFT -> rightToLeft ? 1 : -1;
            case RIGHT -> rightToLeft ? -1 : 1;
            case UP -> -1;
            default -> 1;
        };
        var toggles = group.getToggles();
        int index = toggles.indexOf(source);
        for (int step = 1; index >= 0 && step < toggles.size(); step++) {
            var next = toggles.get(Math.floorMod(index + step * direction, toggles.size()));
            if (next instanceof Node node && node.isDisabled()) {
                continue;
            }
            return isLockedRadio(source) || isLockedRadio(next);
        }
        return false;
    }

    private static boolean isLockedRadio(Toggle toggle) {
        return toggle instanceof JInvRadioButton radio && radio.isEffectivelyReadOnly();
    }

    private static void updateGroupArrowFilters(ListChangeListener.Change<? extends Toggle> change) {
        var toggles = change.getList();
        boolean hasLegacyRadio = toggles.stream().anyMatch(JInvRadioButton.class::isInstance);
        while (change.next()) {
            change.getRemoved().stream().filter(toggle -> !toggles.contains(toggle))
                    .forEach(toggle -> setGroupArrowFilter(toggle, false));
            if (hasLegacyRadio) {
                change.getAddedSubList().forEach(toggle -> setGroupArrowFilter(toggle, true));
            }
        }
        if (!hasLegacyRadio) {
            toggles.forEach(toggle -> setGroupArrowFilter(toggle, false));
            toggles.removeListener(GROUP_TOGGLES_LISTENER);
        }
    }

    private static void setGroupArrowFilter(Toggle toggle, boolean add) {
        if (toggle instanceof ToggleButton button && !(button instanceof JInvRadioButton)) {
            if (add) {
                button.addEventFilter(KeyEvent.KEY_PRESSED, GROUP_ARROW_FILTER);
            } else {
                button.removeEventFilter(KeyEvent.KEY_PRESSED, GROUP_ARROW_FILTER);
            }
        }
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        if (action != AccessibleAction.FIRE || !isEffectivelyReadOnly()) {
            super.executeAccessibleAction(action, parameters);
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

    @Override
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {

        return Controls.<T>getDsAdapterFromControl(getToggleGroup());
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
    public StringProperty labelTextProperty() {
        return textLabelProperty;
    }
}
