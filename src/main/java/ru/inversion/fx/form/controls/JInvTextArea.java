package ru.inversion.fx.form.controls;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.app.service.exteditor.ExtensionType;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_GROUP_ID;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_ORDER_IN_GROUP;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_SHOW_IN_FILTER;
import static ru.inversion.fx.form.controls.JInvTextField.CONTROL_CASE_SENSITIVE_MODE;

import ru.inversion.fx.form.controls.skin.ISkinPopulatable;
import ru.inversion.fx.form.controls.skin.JInvTextAreaSkin;
import ru.inversion.fx.form.controls.textfield.impl.CaseSensitiveFormatter;
import ru.inversion.fx.form.controls.textfield.impl.CaseSensitiveOperator;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

/**
 *
 * @author antonovdi
 */
public class JInvTextArea extends TextArea implements ITextFieldBase {

    private final RequiredState requiredState = new RequiredState(this);
    private StringProperty textLabelProperty = new SimpleStringProperty("");

    private BooleanProperty toolBarVisibleProperty = new SimpleBooleanProperty(false);
    private ObjectProperty<ExtensionType> extensionTypeProperty = new SimpleObjectProperty<>(ExtensionType.TXT);
    private final BooleanProperty butonFEVisibleProperty = new SimpleBooleanProperty(this, "butonFeVisibleProperty", true);

    private final BooleanProperty appendFromLOV = new SimpleBooleanProperty( this, "appendFromLOV", false );
    final private SimpleBooleanProperty validateFromLOV = new SimpleBooleanProperty(this, "validateFromLOV", false);
    final private SimpleObjectProperty<AbstractLovBase> lov = new SimpleObjectProperty(this, "lov");
    private String lovClassName;
    private Font cachedFont;

    public JInvTextArea() {
        this("");
    }

    public JInvTextArea(String text) {
        super(text);
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
        setSkin(new JInvTextAreaSkin(this));
        setWrapText( true );
        initKeyBoard();
        initTrimmer();
        initFontPerExtension();
        initContextMenu();
        // Устанавлием форматтер для контроля режима ввода на каждый контрол. Увы нельзя поставить один на все, вылетает исключение
        setTextFormatter(new CaseSensitiveFormatter(new CaseSensitiveOperator()));
    }

    /** Для JAVAKERNEL-1286 */
    private void initFontPerExtension() {
        extensionTypeProperty.addListener( (v,o,n) -> {
            try {
                ViewPrefAppService service = BaseApp.APP().getViewPrefService();

                if ( n == ExtensionType.SQL ){
                    cachedFont = getFont();
                    setFont( service.getCodeFont() );
                } else {
                    setFont( cachedFont != null ? cachedFont : service.getFont() );
                }
                service.refreshFontNode( this, getFont() );
            } catch ( AppException ignored ) {}
        } );
    }

    protected void initKeyBoard() {
        JInvKeyboardManager.initNaviationOnNode(this);

        addEventFilter( KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.F9) {
                if (isEditable() && !(this instanceof IStateControl && ((IStateControl) this)
                        .stateProperty()
                        .get()
                        .equals( IStateControl.State.ERROR ))) {

                    if (getController() != null && getController().getValidMan() != null) {
                        getController().getValidMan().setFlagOnShowChoiceDialog(true);
                    }

                    event.consume();
                    commitValue();
                    showLOV( ( t, u ) -> {
                        if(t) {
//                            setDisableNextValidation убрано
                            JInvKeyboardManager.fireForwardEvent(null, JInvTextArea.this);
                        } else {
                            requestFocus();
                        }
                    } );

                }
            }
        });
    }

    private void initTrimmer() {
        focusedProperty().addListener( ( observable, oldValue, newValue ) -> {
            if (!newValue  && !isReadOnly()){
                textProperty().setValue( textProperty().getValueSafe().trim() );
            }
        } );
    }

    private void initContextMenu() {
        ObservableList<MenuItem> itemList =
                getSkin() instanceof ISkinPopulatable ? ((ISkinPopulatable) getSkin()).getListItemsToPopulateContextMenu() : null;
        Optional<EventHandler<ActionEvent>> editDialogAction = getEditDialogAction();

        if (itemList != null && editDialogAction.isPresent()){
            MenuItem feMenuItem = new MenuItem(fore.getString( "OTKRYT_V_DIALOGOVOM_OKNE" ));
            feMenuItem.setGraphic(IconDescriptor.of(FontAwesome.fa_ellipsis_h).getLabel());
            feMenuItem.setOnAction(editDialogAction.get());
            itemList.add(new SeparatorMenuItem());
            itemList.add(feMenuItem);
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

    /**
     *
     */
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
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
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {

        return Controls.getDsAdapterFromControl(this);
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
     * Возвращает признк показывать ли колонку в фильтре
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

    public void setToolBarVisible(boolean val) {
        toolBarVisibleProperty.set(val);
    }

    public boolean getToolBarVisible() {
        return toolBarVisibleProperty.get();
    }

    public BooleanProperty toolBarVisibleProperty() {
        return toolBarVisibleProperty;
    }

    public void setExtensionType(ExtensionType type) {
        extensionTypeProperty.set(type);
    }

    public ExtensionType getExtensionType() {
        return extensionTypeProperty.get();
    }

    public ObjectProperty<ExtensionType> extensionTypeProperty() {
        return extensionTypeProperty;
    }

    public boolean getButtonFEVisible() {
        return butonFEVisibleProperty.get();
    }

    public void setButtonFEVisible(boolean val) {
        this.butonFEVisibleProperty.set(val);
    }

    public BooleanProperty buttonFEVisibleProperty() {
        return butonFEVisibleProperty;
    }

    /**
     * @param value
     */
    public void setCaseSensitiveMode( RegisterEnum value) {
        getProperties().put(CONTROL_CASE_SENSITIVE_MODE, value);
    }

    /**
     * @return
     */
    public RegisterEnum getCaseSensitiveMode() {
        return (RegisterEnum) getProperties().getOrDefault(CONTROL_CASE_SENSITIVE_MODE, RegisterEnum.UNKNOWN_CASE);
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

    @Override
    public AbstractLovBase getLOV() {
        return lov.get();
    }

    /**
     * Устанавливает диалог выбора значений LOV (list of values) на текущее поле
     *
     * @param lovObj список LOV (list of values)
     * @param validateFromLOV игнорируется
     */
    @Override
    public void setLOV(AbstractLovBase lovObj, boolean validateFromLOV) {

        this.lov.set(lovObj);

        if (lovObj == null) {
            return;
        }

        //Иной раз в специфическом лове требуются знания о контроле к которому оный привязан
        lovObj.onSetLov(this);

//        setValidateFromLOV(validateFromLOV);

        addLovValueListener(lovObj);

        if ( S.isNullOrEmpty(lovObj.getTitle()) && getLabel() != null) {

            lovObj.setTitle(
                    getLabel().getText()
            );
        }

        // Устанавливаем кнопку на тулбар
        JInvLOVButton button = new JInvLOVButton();
        getToolBar().getItems().add( 0, button );//.setInnerButton(button);
        button.setTextField(this);
        button.setToolTipText( fore.getString( "LOV_BUTTON" ) );
    }

    /**
     * {@link #setLOV(ru.inversion.fx.form.lov.AbstractLovBase, boolean) }
     */
    @Override
    public void setLOV( final AbstractLovBase lov ) {
        setLOV(lov, isValidateFromLOV());
    }

    @Override
    public SimpleObjectProperty<AbstractLovBase> lovProperty() {
        return lov;
    }

    @Override
    public String getLovClassName() {
        return lovClassName;
    }

    @Override
    public void setLovClassName( final String lovClassName ) {
        this.lovClassName = lovClassName;
    }

    @Override
    public void setValidateFromLOV( final boolean valFromLOV ) {
        this.validateFromLOV.set(valFromLOV);
    }

    @Override
    public boolean isValidateFromLOV() {
//        return validateFromLOV.get();
        return false;
    }

    public ToolBar getToolBar(){
        if ( getSkin() instanceof JInvTextAreaSkin ){
            return ( (JInvTextAreaSkin) getSkin() ).getToolBar();
        }
        return new ToolBar();
    }

    @Override
    public BooleanProperty validateFromLOVProperty() {
        return validateFromLOV;
    }

    @Override
    public void showLOV( final BiConsumer<Boolean, ILov> clb ) {
        try {
            if (getLOV() != null) {

                if (getLOV().isSmallLov()) {
                    Bounds boundsInLocal = this.getBoundsInLocal();
                    Bounds localToScreen = this.localToScreen(boundsInLocal);
                    getLOV().setPosition((int) localToScreen.getMinX(), (int) localToScreen.getMinY());
                }
                getLOV().showChoiceList( ViewContext.of(getScene().getWindow()), getText(), new BiConsumer<Boolean, ILov>() {
                    @Override
                    public void accept(Boolean t, ILov u) {
                        if (t.booleanValue()) {
                            setTextValueFromLov();
                        }
                        if (clb != null) {
                            clb.accept(t, u);
                        }
                    }

                });
            }
        } catch (Throwable th) {
            JInvErrorService.handleException(ViewContext.of(getScene().getWindow()), th);
        }    }

    /**
     * Для установки значения по AbstractEntityLovBase.checkValue (.., true)
     *
     * @param lov
     */
    private void addLovValueListener(AbstractLovBase lov) {

        lov.valueProperty().addListener( o -> setTextValueFromLov() );
    }

    /**
     * Установка значения по AbstractEntityLovBase.checkValue (.., true)
     */
    protected void setTextValueFromLov() {
        setText( TypeConverter.convert(getLOV().getValue(), String.class));
    }

    /** {@inheritDoc} */
    public Optional<EventHandler<ActionEvent>> getEditDialogAction() {
        return Optional.of(a -> {
            Skin<?> skin = getSkin();
            if (skin instanceof JInvTextAreaSkin){
                ((JInvTextAreaSkin) skin).getFeButton().fire();
            }
        });
    }
    @Override
    public ClipboardContent getClipboardContent() {
        final ClipboardContent content = new ClipboardContent();
        final String selectedText = getSelectedText();
        if (selectedText.length() > 0) {
            content.putString(selectedText);
        }
        return content;
    }

    /** Отныне для модификации того, что попадёт в буфер обмена при копировании используется getClipboardContent() */
    @Override
    public final void copy() {
        ClipboardContent content = getClipboardContent();
        if (!content.isEmpty()) {
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    @Override
    public final boolean isAppendFromLOV() {
        return appendFromLOV.get();
    }
    @Override
    public final void setAppendFromLOV( boolean appendFromLOV ) {
        this.appendFromLOV.set(appendFromLOV);
    }
    @Override
    public final BooleanProperty appendFromLOVProperty() {
        return appendFromLOV;
    }
}
