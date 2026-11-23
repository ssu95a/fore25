package ru.inversion.fx.form.controls;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.skin.JInvComboBoxListViewSkin;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static ru.inversion.fx.form.controls.Controls.*;
import static ru.inversion.fx.form.controls.IStateControl.State.NULL;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_SHOW_IN_FILTER;

/**
 *
 * @author ssu
 */
public class JInvComboBox<T, R> extends ComboBox<T> implements IJInvControl, IValidatableByChangeValueControl,
        IFilterControl, IStateControl {

    private final static Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
    private static final ResourceBundle foreBundle = ResourceBundle.getBundle("fore");
    private static final EventHandler<MouseEvent> READ_ONLY_MOUSE_FILTER = Event::consume;
    private static final EventHandler<KeyEvent> READ_ONLY_KEY_FILTER = event -> {
        var combo = (JInvComboBox<?, ?>) event.getSource();
        if (!combo.isReadOnly()) {
            return;
        }
        boolean changesSelection = event.getEventType() == KeyEvent.KEY_PRESSED
                && (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN)
                && !event.isAltDown() && !event.isControlDown() && !event.isMetaDown() && !event.isShiftDown();
        boolean commitsEditor = event.getEventType() == KeyEvent.KEY_RELEASED
                && event.getCode() == KeyCode.ENTER && combo.isEditable();
        if (changesSelection || commitsEditor) {
            event.consume();
        }
    };

    private Function<T, R> valueFactory;
    private final RequiredState requiredState = new RequiredState(this);
    private StringProperty textLabelProperty = new SimpleStringProperty(S.EMPTY_STRING);
    final private ObjectProperty<State> stateProperty = new ReadOnlyObjectWrapper<>(this, "stateProperty", NULL);
    private BooleanProperty nullable = new SimpleBooleanProperty();
    private StringProperty nullText = new SimpleStringProperty(foreBundle.getString("COMBOBOX_NULL"));
    private boolean restoreEditorEditable;

    public Function<T, R> getValueFactory() {
        return valueFactory;
    }

    public void setValueFactory(Function<T, R> valueFactory) {
        this.valueFactory = valueFactory;
    }

    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
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

    public JInvComboBox() {
        super();
        init();
    }

    public JInvComboBox(ObservableList items) {
        super(items);
        init();
    }

    @Override protected Skin<?> createDefaultSkin() {
        return new JInvComboBoxListViewSkin<T>(this);
    }

    @Override
    public void show() {
        if (!isReadOnly()) {
            super.show();
        }
    }

    /** Переносит значение из editor в value*/
    public void forceCommitValue()
    {
        JInvComboBoxListViewSkin skin = (JInvComboBoxListViewSkin)getSkin();
        skin.forceCommitValue();
    }

    private void init() {
        //setSkin(new JInvComboBoxListViewSkin<>(this));
        readOnlyProperty().addListener((obs, oldValue, readOnly) -> applyReadOnly(readOnly));
        editableProperty().addListener((obs, oldValue, editable) -> {
            if (editable && isReadOnly()) {
                lockEditor();
            }
        });
        addEventFilter(KeyEvent.ANY, READ_ONLY_KEY_FILTER);
        JInvKeyboardManager.initNaviationOnNode(this);
        nullableProperty().addListener((v,o,n) -> {
//            logger.info("{}: setting nullable to {}",this,n);
            if (n){
                getItems().add(0, null);
                promptTextProperty().bindBidirectional(nullTextProperty());
            } else {
                getItems().remove(null);
                promptTextProperty().unbindBidirectional(nullTextProperty());
            }
        });
        setConverter(new NullableConverterDecorator(getConverter()));

    }

    private void applyReadOnly(boolean readOnly) {
        if (!focusTraversableProperty().isBound()) {
            setFocusTraversable(!readOnly);
        }
        if (readOnly) {
            if (isEditable()) {
                lockEditor();
            }
            disarm();
            hide();
            addEventFilter(MouseEvent.MOUSE_CLICKED, READ_ONLY_MOUSE_FILTER);
            addEventFilter(MouseEvent.MOUSE_PRESSED, READ_ONLY_MOUSE_FILTER);
            addEventFilter(MouseEvent.MOUSE_RELEASED, READ_ONLY_MOUSE_FILTER);
        } else {
            removeEventFilter(MouseEvent.MOUSE_CLICKED, READ_ONLY_MOUSE_FILTER);
            removeEventFilter(MouseEvent.MOUSE_PRESSED, READ_ONLY_MOUSE_FILTER);
            removeEventFilter(MouseEvent.MOUSE_RELEASED, READ_ONLY_MOUSE_FILTER);
            if (restoreEditorEditable) {
                restoreEditorEditable = false;
                if (!getEditor().editableProperty().isBound()) {
                    getEditor().setEditable(true);
                }
            }
        }
        pseudoClassStateChanged(DISABLED_GREY_STATE, readOnly || isDisabled());
    }

    private void lockEditor() {
        // Черновик, введённый до блокировки, не должен стать значением при потере фокуса.
        cancelEdit();
        TextField editor = getEditor();
        if (editor.isEditable() && !editor.editableProperty().isBound()) {
            restoreEditorEditable = true;
            editor.setEditable(false);
        }
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

    /** */
    public void setSelectedValue(R value) {

        if( value != null )
        {
            boolean nofFound = true;

            for( T entry : getItems() )
            {
                if( entry == U.EMPTY || (isNullable() && entry == null) )
                {
                    getSelectionModel().select(entry);
                    continue;
                }

                if( valueFactory != null )
                {
                    R currentEntryValue = valueFactory.apply(entry);

                    if (currentEntryValue != null && currentEntryValue.equals(value)) {
                        getSelectionModel().select(entry);
                        nofFound = false;
                        break;
                    }

                } else if ( value.equals(entry) )
                {
                    getSelectionModel().select(entry);
                    nofFound = false;
                    break;
                }
            }//end for

            if( nofFound && isEditable() )
            {
                // no cast for valueFactory
                //
                this.setValue((T)value);
//                getItems().add( 0,(T)value );
//                getSelectionModel().clearAndSelect(0);
            }

        } else
        {
            if (isEditable())
                this.setValue(null);
            else
                getSelectionModel().clearSelection();
        }
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

    public State getState() {
        return stateProperty.getValue();
    }

    public void setState(State state) {
        stateProperty.setValue(state);
    }

    @Override
    public ObjectProperty<State> stateProperty() {
        return stateProperty;
    }

    /**
     * Включена ли поддержка null-ового значения первым пунктом (U.EMPTY)
     */
    public boolean isNullable() {
        return nullable.get();
    }

    /**
     * Поддержка null-ового значения первым пунктом (U.EMPTY)
     */
    public BooleanProperty nullableProperty() {
        return nullable;
    }

    /**
     * Включить/выключить поддержка null-ового значения первым пунктом (U.EMPTY)
     */
    public void setNullable(boolean nullable) {
        this.nullable.set(nullable);
    }

    public String getNullText() {
        return nullText.get();
    }

    public StringProperty nullTextProperty() {
        return nullText;
    }

    public void setNullText(String nullText) {
        this.nullText.set(nullText);
    }

    private class NullableConverterDecorator extends StringConverter {

        private final StringConverter converter;

        public NullableConverterDecorator(StringConverter converter) {
            this.converter = converter;
        }

        @Override
        public String toString(Object object) {

            if (object == null) {
                return getNullText();
            } else if (converter != null) {
                return converter.toString(object);
            } else {
                return object.toString();
            }
        }

        @Override
        public Object fromString(String string) {
            if (string != null && string.equals(getNullText())) {
                return null;
            } else if (converter != null) {
                return converter.fromString(string);
            } else {
                return string;
            }
        }

    }
}
