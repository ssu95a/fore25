package ru.inversion.fx.form.controls;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.LifeCycleStateEnum;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.skin.ISkinPopulatable;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.controls.textfield.impl.CaseSensitiveFormatter;
import ru.inversion.fx.form.controls.textfield.impl.CaseSensitiveOperator;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.fx.form.valid.ValidMan;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.fx.form.valid.validators.LovValidator;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.icons.enums.MaterialDesign;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.controls.Controls.*;
import static ru.inversion.fx.form.controls.IStateControl.State.NULL;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_SHOW_IN_FILTER;

/**
 * Базовый контрол для ввода текстовых значений. Поддерживает валидацию.
 *
 * @author antonovdi
 * @author sulimoff
 */
public class JInvTextField extends TextField implements ITextFieldBase, IStateControl {

    public static final String CONTROL_CASE_SENSITIVE_MODE = "ru.inversion.control.case_sensitive_mode";
    final private SimpleBooleanProperty validateFromLOV = new SimpleBooleanProperty(this, "validateFromLOV", true);
    private final BooleanProperty appendFromLOV = new SimpleBooleanProperty( this, "appendFromLOV", false );
    final private SimpleObjectProperty<AbstractLovBase> lov = new SimpleObjectProperty(this, "lov");
    private final RequiredState requiredState = new RequiredState(this);
    private String lovClassName;
    final private ObjectProperty<State> stateProperty = new ReadOnlyObjectWrapper<>(this, "stateProperty", NULL);

    private StringProperty textLabelProperty = new SimpleStringProperty("");
    private StringProperty buttonTooltipTextProperty = new SimpleStringProperty("");
    private BooleanProperty buttonIgnoresEditableProperty = new SimpleBooleanProperty(false);
    /** Если контрол editable или имеет ловокнопку с buttonIgnoresEditable */
    private BooleanProperty editableOrLovEditableProperty = new SimpleBooleanProperty(buttonIgnoresEditableProperty.or(editableProperty()).getValue());

    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    private final ObjectProperty<Button> externalButton = new SimpleObjectProperty<>();
    private BooleanProperty externalButtonExists = new SimpleBooleanProperty(false);
    private IndexRange cachedSelection;

    protected void initKeyBoard() {
        JInvKeyboardManager.initNaviationOnNode(this);

        addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.F9) {
                if ((isEditable() || getButtonIgnoresEditable()) && !this.stateProperty().get().equals(State.ERROR)) {

                    if (getController() != null && getController().getValidMan() != null) {
                        getController().getValidMan().setFlagOnShowChoiceDialog(false);
                    }

                    event.consume();
                    commitValue();
                    showLOV((t, u) -> {
                        if (t) {
//                            setDisableNextValidation убрано
                            JInvKeyboardManager.fireForwardEvent(null, JInvTextField.this);
                        } else {
                            requestFocus();
                        }
                    });

                }
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

    /** Отныне для модификации того, что попадёт в буфер обмена используется getClipboardContent() */
    @Override
    public final void copy() {
        ClipboardContent content = getClipboardContent();
        if (!content.isEmpty()) {
            Clipboard.getSystemClipboard().setContent(content);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {

        return Controls.<T>getDsAdapterFromControl(this);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setToolTipText(String toolTipText) {
        if (toolTipText != null) {

            if (getTooltip() != null && !(getTooltip() instanceof JInvTooltipValue)) {
                getTooltip().setText(toolTipText);
            } else {
                setTooltip(new Tooltip(toolTipText));
            }
        }

    }

    /* * {@inheritDoc } */
    @Override
    public String getToolTipText() {
        return ITextFieldBase.super.getToolTipText();
    }

    /** */
    private void initDefaultToolTip( )
    {
        setTooltip( new JInvTooltipValue(() -> Controls.getValue(JInvTextField.this),
           () -> {
            return !isEditable() && Controls.getValue(JInvTextField.this) != null && checkExcessTextLength();
           })
        );
    }

    /** */
    protected void initTrimmer( )
    {
        focusedProperty().addListener( ( observable, oldValue, newValue ) -> {

            if( !newValue && !isReadOnly() )
            {
                if( getLOV() == null || !validateFromLOV.get() )
                {
                    String trimmedText = textProperty().getValueSafe().trim();

                    if( textProperty().getValueSafe().equals( trimmedText ) )
                        return;

                    textProperty().setValue( trimmedText );
                }
            }//end if
        }
        );
    }

    /**
     * Устанавливает конвертер для строк в сплывающей подсказке, в случае если класс всплывающей подсказки {@link JInvTooltipValue}
     *
     * @param converter конвертер строк для всплывающей подсказки
     */
    public void setTooltipConverter( StringConverter converter )
    {
        if( getTooltip() != null && getTooltip() instanceof JInvTooltipValue)
            ( (JInvTooltipValue) getTooltip() ).setToolTipConverter(converter);
    }

    /**
     * Создает обьект со значением текста null
     */
    public JInvTextField() {
        this(null);
    }

    /**
     * Создает объект с переданным значением текста
     *
     * @param arg0 начальное значение текста
     */
    public JInvTextField(String arg0) {
        super(arg0);
        readOnlyProperty().addListener((obs, oldV, newV) -> Controls.disableControl(this, obs.getValue()));
        this.setSkin(createDefaultSkin());
        initContextMenu();
        initDefaultToolTip();
        initKeyBoard();
        initFocusListener();
        //initTrimmer();

        //не стоит кэшировать выделение когда фокус только-только получен
        final boolean[] dontCacheSelection = {false};
        focusedProperty().addListener( (v,o,n)-> {
            if (n){
                dontCacheSelection[0] = true;
                Platform.runLater( () ->{
                    dontCacheSelection[0] = false;
                } );
            }

        } );

        selectionProperty().addListener( (v,o,n) -> {
            boolean shouldCache = isFocused() && !dontCacheSelection[0];
            if ( shouldCache ){
                cacheSelection();
            }
//            logger.info( "selection changed from {} to {}, shouldCache = {}", o, n, shouldCache );
        } );

        // Устанавлием форматтер для контроля режима ввода на каждый контрол. Увы нельзя поставить один на все, вылетает исключение
        setTextFormatter(new CaseSensitiveFormatter(new CaseSensitiveOperator()));

        //обратная совместимость со старой boolean-пропертью externalButton()
        externalButtonExists.bind(externalButton.isNotNull());

        // Если установлена внешняя кнопка, то внутреннюю кнопку удаляем
        externalButton.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                removeInnerButton();
            }
        });

        editableOrLovEditableProperty.bind(buttonIgnoresEditableProperty.or(editableProperty()));
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

    /** {@inheritDoc} */
    public Optional<EventHandler<ActionEvent>> getEditDialogAction() {
        return Optional.of(a -> {
            //если есть febutton, используем его
            if (getInnerButton().isPresent() && getInnerButton().get() instanceof JInvFEButton) {
               getInnerButton().get().fire();
            } else {
            //иначе делаем временный
                new JInvFEButton(this).fire();
            }
        });
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JInvTextFieldButtonSkin(this);
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

    /**
     * Устанавливает диалог выбора значений LOV (list of values) на текущее поле
     *
     * @param lovObj          список LOV (list of values)
     * @param validateFromLOV признак, регулирующий валидацию текстового поля относительно наличия значения в LOV. Если значение true, то происходит блокировка фокуса, пока значение не будет введено
     *                        верно или стерто.
     */
    public void setLOV(AbstractLovBase lovObj, boolean validateFromLOV) {

        if (lovObj == null) {
            unbindLOV();
            return;
        }

        this.lov.set(lovObj);

        //Иной раз в специфическом лове требуются знания о контроле к которому оный привязан
        ((ILov) lovObj).onSetLov(this);

        setValidateFromLOV(validateFromLOV);

        addLovValueListener(lovObj);

        if (S.isNullOrEmpty(lovObj.getTitle()) && getLabel() != null) {

            lovObj.setTitle(
                    getLabel().getText()
            );
        }
        FontAwesome customIcon = (FontAwesome) lovObj.getProperty( JInvChoiceButton.PROPERTY_CUSTOM_ICON );
        IBaseIconDescriptor customComplexIcon = customIcon == null ? null : IconDescriptor.of( customIcon );

        if ( customComplexIcon == null && isAppendFromLOV() ){
            customComplexIcon = new IconDescriptorBuilder<>(MaterialDesign.mdi_table_large_plus).build();
        }

        // Устанавливаем кнопку внутрь внутренней кнопки филда
        JInvLOVButton button = customComplexIcon == null ? new JInvLOVButton() : new JInvLOVButton( customComplexIcon );

        if (getSkin() != null && getSkin() instanceof JInvTextFieldButtonSkin) {
            ((JInvTextFieldButtonSkin) getSkin()).getButton().setInnerButton(button);
        }
        button.setTextField(this);
        button.tooltipProperty().bind( new TooltipBinding( buttonTooltipTextProperty ));
        button.disableProperty().bind(Bindings.or(readOnlyProperty(),
                editableProperty().not().and(buttonIgnoresEditableProperty().not())));
//        button.visibleProperty().bind( editableProperty() );

        // Привязываем валидатор, если он не привязан
        if (canBeLovValidated()) {
            ValidMan validMan = getController().getValidMan();
            Validator validator = validMan.getValidators(this)
                    .stream()
                    .filter(t -> t instanceof LovValidator)
                    .findFirst()
                    .orElse(null);
            if (validator == null) {
                validMan.bindValidators2Control(this, new LovValidator(this));
            }
        }
    }

    /**
     * Убирает LOV кнопку с textField'а.
     */
    public final void unbindLOV() {
        this.lov.set(null);
        removeInnerButton();
    }

    /**
     * Устанавливает внутреннюю кнопку. LOV кнопку устанавливайте через {@link #setLOV(AbstractLovBase)}
     */
    public void setInnerButton(JInvButton button) {
        if (button == null) {
            throw new IllegalArgumentException("button can't be null");
        }
        if (getSkin() != null || getSkin() instanceof JInvTextFieldButtonSkin) {
            ((JInvTextFieldButtonSkin) getSkin()).getButton().setInnerButton(button);
            button.setTextField(this);
            button.disableProperty().bind(Bindings.or(readOnlyProperty(),
                    editableProperty().not().and(buttonIgnoresEditableProperty().not())));
        }
    }

    /**
     * Удаляет внутреннюю кнопку. Если хотите удалить LOV кнопку - воспользуйтесь методом {@link #unbindLOV()}
     */
    public void removeInnerButton() {
        if (getSkin() != null && getSkin() instanceof JInvTextFieldButtonSkin) {
            JInvWrapButton button = ((JInvTextFieldButtonSkin) getSkin()).getButton();
            JInvButton innerButton = button.getInnerButton();
            if (innerButton != null) {
                button.setInnerButton(null);
            }
        }
    }

    private static class TooltipBinding extends ObjectBinding<Tooltip> {
        StringProperty text;
        TooltipBinding( StringProperty text ) {
            bind(text);
            this.text = text;
        }
        @Override
        protected Tooltip computeValue() {
            // buttonTooltipText в приоритете
            String tooltipText = text.getValueSafe();
            if ( S.isNullOrEmpty( tooltipText ) ){
            // А если его нет, то заглушка из fore.properties
                tooltipText = ResourceBundle.getBundle("fore").getString( "LOV_BUTTON" );
            }
            return new Tooltip( tooltipText );
        }
    }

    private boolean canBeLovValidated() {
        return getController() != null && getController().getValidMan() != null && isValidateFromLOV()
                && (getController().getLifeCycleState() == LifeCycleStateEnum.AFTER_INIT ||
                getController().getLifeCycleState() == LifeCycleStateEnum.RUNTIME);
    }

    /**
     * Для установки значения по AbstractEntityLovBase.checkValue (.., true) JIRA.JAVAKERNEL-593
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
        String valueFromLov = TypeConverter.convert( getLOV().getValue(), String.class );

        if ( isAppendFromLOV() ){
            if ( S.isNotNullOrEmpty( valueFromLov ) ) {

                if (valueFromLov.contains( String.valueOf(LovValidator.DELIMITER) )){
                    valueFromLov = '\"'+valueFromLov+'\"';
                }
                if ( shouldPrependDelimiter() )
                {
                    valueFromLov = LovValidator.DELIMITER + valueFromLov;
                }
            }

            boolean shouldReplace = cachedSelection != null
                                 && cachedSelection.getLength() > 0
                                 && this.getLength() <= cachedSelection.getEnd();

            if ( shouldReplace ){
                replaceText( cachedSelection, valueFromLov );
            } else {
                appendText( valueFromLov );
            }
        } else {
            setText( valueFromLov );
        }
        cacheSelection();
    }

    private boolean shouldPrependDelimiter(){
        int selectionStart = 0;
        if ( cachedSelection != null ){
            selectionStart = Math.max( 0, cachedSelection.getStart() - 1);
        }
        return S.isNotNullOrEmpty( getText() )
            && selectionStart > 0
            && getText().charAt( selectionStart ) != LovValidator.DELIMITER;
    }

    /**
     * {@link #setLOV(ru.inversion.fx.form.lov.AbstractLovBase, boolean) }
     */
    public void setLOV(AbstractLovBase lov) {
        setLOV(lov, isValidateFromLOV());
    }

    /**
     * @return список значений LOV (list of values)
     */
    public AbstractLovBase getLOV() {
        return lov.get();
    }

    /**
     * Метод задающий признак, регулирующий валидацию текстового поля относительно наличия значения в LOV. Если значение true, то происходит блокировка фокуса, пока значение не будет введено верно или
     * стерто.
     *
     * @param valFromLOV
     */
    public void setValidateFromLOV(boolean valFromLOV) {

        this.validateFromLOV.set(valFromLOV);
    }

    /**
     * @return признак валидируемости текстового поля по значениеям в LOV
     */
    public boolean isValidateFromLOV() {
        return validateFromLOV.get();
    }

    public BooleanProperty validateFromLOVProperty() {
        return validateFromLOV;
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

    /** */
    protected String getTextForLov( )
    {
        if ( isAppendFromLOV() ){
            return S.EMPTY_STRING;
        }
        return getText();
    }

    private void cacheSelection(){
        cachedSelection = getSelection();
//        logger.info( "cached selection is now {}", cachedSelection );
    }

    /** */
    @Override
    public void showLOV(BiConsumer<Boolean, ILov> clb) {

        try {
            if (getLOV() != null) {

                if (getLOV().isSmallLov()) {
                    Bounds boundsInLocal = this.getBoundsInLocal();
                    Bounds localToScreen = this.localToScreen(boundsInLocal);
                    getLOV().setPosition((int) localToScreen.getMinX(), (int) localToScreen.getMinY());
                }
                getLOV().showChoiceList( ViewContext.of(getScene().getWindow()), getTextForLov(), new BiConsumer<Boolean, ILov>() {
                    @Override
                    public void accept(Boolean t, ILov u) {

                        if( t.booleanValue() ) {

                            // setTextValueFromLov();

                            /**
                             Закоментировано 08.04.2020, Sulimoff
                             Происходит дублированный вызова setText() для TextField
                             т.к. еще висит слушатель на valueProperty()

                             @see addLovValueListener(...)

                            */
                        }

                        if( clb != null )
                            clb.accept( t, u );
                    }
                });
            }
        } catch (Throwable th) {
            JInvErrorService.handleException(ViewContext.of(getScene().getWindow()), th);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Control setLabel(Label label) {
        if (label != null) {
            label.setLabelFor(this);
            textLabelProperty.bind(label.textProperty());
        }
        return this;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getFieldName() {
        return (String) getProperties().getOrDefault(CONTROL_FIELD_NAME, null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setFieldName(String fieldName) {
        getProperties().put(CONTROL_FIELD_NAME, fieldName);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    /**
     * @return имя класса для LOV
     * @see#setLovClassName(java.lang.String)
     */
    public String getLovClassName() {
        return lovClassName;
    }

    /**
     * Установка имени класса для LOV. Если имя задано происходит автоматическое создание LOV по имени класса
     *
     * @param lovClassName
     */
    public void setLovClassName(String lovClassName) {
        this.lovClassName = lovClassName;
    }

    /**
     * @return превышает ли текст размеры текстового поля
     */
    public boolean checkExcessTextLength() {

        if (getChildren() != null && !getChildren().isEmpty()) {

            Pane pane = (Pane) getChildren().get(0);
            ObservableList<Node> children = pane.getChildren();
            if (children.size() > 1 && children.get(1) instanceof Text) {
                Text text = (Text) children.get(1);

                double textWidth = text.getLayoutBounds().getWidth() + 15;
                double width = getWidth();

                JInvWrapButton button = ((JInvTextFieldButtonSkin) getSkin()).getButton();
                JInvButton innerButton = button.getInnerButton();

                if (button.isVisible() && innerButton != null) {
                        width = width - button.getWidth();
                }
//                logger.info("textfield id "+getId()+" textWidth " + textWidth + " width " + width +
//                        " paneWidth " + pane.getWidth() + " result " + (textWidth > width) + " buttonWidth "+ button.getWidth());
                return width != 0.0 && textWidth > width;
            }
        }

        return false;
    }
    /**
     * {@inheritDoc }
     */
    @Override
    public void setRequiredState(RequiredStateEnum state) {
        requiredState.setState(state);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public RequiredStateEnum getRequiredState() {
        return requiredState.getState();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ObjectProperty<RequiredStateEnum> requiredStateProperty() {
        return requiredState.stateProperty();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void setRequired(boolean val) {
        if (val) {
            setRequiredState(RequiredStateEnum.REQUIRED);
        } else {
            setRequiredState(RequiredStateEnum.NOT_REQUIRED);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isRequired() {
        return requiredState.isRequired();
    }

    @Override
    public StringProperty labelTextProperty() {
         return textLabelProperty;
    }

    public SimpleObjectProperty<AbstractLovBase> lovProperty() {
        return lov;
    }

    /**
     * Теперь read-only.
     * Используйте externalButtonProperty и передавайте конкретную кнопку
     * Если это свойство true, то считается что к полю привязано внешняя кнопка. В этом случае внутреннюю кнопку скрываем.
     */
    @Deprecated
    public BooleanProperty externalButton() {
        return externalButtonExists;
    }

    /**
     * Если в это свойство передана кнопка, то считается что к полю привязано внешняя кнопка.
     * В этом случае внутреннюю кнопку скрываем.
     */
    public ObjectProperty<Button> externalButtonProperty() {
        return externalButton;
    }

    public BooleanProperty buttonIgnoresEditableProperty() {
        return buttonIgnoresEditableProperty;
    }

    public Boolean getButtonIgnoresEditable() {
        return buttonIgnoresEditableProperty.get();
    }

    public void setButtonIgnoresEditable(Boolean buttonIgnoreEditable) {
        this.buttonIgnoresEditableProperty.set(buttonIgnoreEditable);
    }

    /**
     *
     */
    public State getState() {
        return stateProperty.getValue();
    }

    public void setState(State state) {
        stateProperty.setValue(state);
    }

    public ObjectProperty<State> stateProperty() {
        return stateProperty;
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

    /**
     * @param value
     */
    public void setCaseSensitiveMode(RegisterEnum value) {
        getProperties().put(CONTROL_CASE_SENSITIVE_MODE, value);
    }

    /**
     * @return
     */
    public RegisterEnum getCaseSensitiveMode() {
        return (RegisterEnum) getProperties().getOrDefault(CONTROL_CASE_SENSITIVE_MODE, RegisterEnum.UNKNOWN_CASE);
    }

    private Optional<JInvWrapButton> getWrapButton(){
        if ( externalButton.get() == null
                && getSkin() != null
                && getSkin() instanceof JInvTextFieldButtonSkin ) {

            JInvWrapButton wrapButton = ((JInvTextFieldButtonSkin) getSkin()).getButton();

            return Optional.ofNullable(wrapButton);
        }
        return Optional.empty();
    }

    private Optional<JInvButton> getInnerButton(){
        if ( externalButton.get() == null
                && getSkin() != null
                && getSkin() instanceof JInvTextFieldButtonSkin ) {

            Optional<JInvWrapButton> wrapButton = getWrapButton();

            if (wrapButton.isPresent()){
                JInvButton innerButton = wrapButton.get().getInnerButton();
                return Optional.ofNullable(innerButton);
            }
        }
        return Optional.empty();
    }

    public boolean isEditableOrLovEditable() {
        return editableOrLovEditableProperty.get();
    }

    public BooleanProperty editableOrLovEditableProperty() {
        return editableOrLovEditableProperty;
    }

    /**
     * Устанавливает текст всплывающей подсказки над кнопкой
     *
     * @param toolTipText
     */
    public void setButtonToolTipText(String toolTipText) {
        if (toolTipText != null) {
            buttonTooltipTextProperty.set(toolTipText);
        }
    }

    /**
     * Возвращает текст всплывающей подсказки над кнопкой
     *
     * @return
     */
    public String getButtonToolTipText() {
        return buttonTooltipTextProperty.get();
    }

    /**
     * Возвращает свойство текста всплывающей подсказки над кнопкой
     *
     * @return
     */
    public StringProperty buttonTooltipTextProperty() {
        return buttonTooltipTextProperty;
    }

}
