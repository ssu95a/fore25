package ru.inversion.fx.form.controls;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.AccessibleAttribute;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.action.decorator.ValueFieldDecorator;
import ru.inversion.fx.form.valid.JInvValidTooltip;
import ru.inversion.utils.scheck.JInvStringWorker;
import ru.inversion.utils.scheck.JInvStringWorkerException;
import ru.inversion.utils.scheck.JInvStringWorkerValidationException;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Optional;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.controls.Controls.*;
import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_SHOW_IN_FILTER;
import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_DATE_DEFAULT;

/**
 *
 * @author antonovdi
 */
public class JInvCalendar extends DatePicker implements IJInvControl, IStateControl, IValidatableControl, IFilterControl, IContextMenuAppendable {

    // private static final Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    // private static final ResourceBundle fore = ResourceBundle.getBundle("fore");
    private static final ResourceBundle bundle = ResourceBundle.getBundle("calendar");

    /*
    public static final CalendarTypeViewEnum DEFAULT_TYPE = CalendarTypeViewEnum.KERNEL;

    public enum CalendarTypeEnum {

        KERNEL, MODULE, ADDITIONAL;

        @Override
        public String toString() {
            switch (this) {
                case KERNEL:
                    return bundle.getString("KERNEL");
                case MODULE:
                    return bundle.getString("MODULE");
                case ADDITIONAL:
                    return bundle.getString("ADDITIONAL");
            }
            return "CalendarTypeEnum{" + '}';
        }
    }


    // К сожалению приходится дублировать этот энум, так как нельзя переопределять toString для использования в Scene Builder
    public static enum CalendarTypeViewEnum {

        KERNEL, MODULE, ADDITIONAL;

        public CalendarTypeEnum toType() {

            switch (this) {
                case KERNEL:
                    return CalendarTypeEnum.KERNEL;
                case MODULE:
                    return CalendarTypeEnum.MODULE;
                case ADDITIONAL:
                    return CalendarTypeEnum.ADDITIONAL;
                default:
                    return null;
            }
        }
    }
    */

    final private ObjectProperty<IStateControl.State> stateProperty = new ValueFieldDecorator(this);

    public static final boolean DEFAULT_FX_CALENDAR_DATE_PRIORITET = false;
    public static final String PROPERTY_FX_CALENDAR_DATE_PRIORITET = "FX_CALENDAR_DATE_PRIORITET";

    static private final int centurySeparatorYear = 1950; //Year.now().getValue() - 80;
    static private final DateTimeFormatter ddMMyyyy = DateTimeFormatter.ofPattern( "ddMMyyyy" );
    static private final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern( "yyyyMMdd" );
    static private final DateTimeFormatter ddMMyy =
            new DateTimeFormatterBuilder()
                    .appendPattern("ddMM")
                    .appendValueReduced( ChronoField.YEAR, 2, 2, centurySeparatorYear )
                    .toFormatter();
    static private final DateTimeFormatter yyMMdd =
            new DateTimeFormatterBuilder()
                    .appendValueReduced( ChronoField.YEAR, 2, 2, centurySeparatorYear )
                    .appendPattern("MMdd")
                    .toFormatter();

    protected DateTimeFormatter formatter;
    private DateTimeFormatter inputFormatter;
    private DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(MASK_DATE_DEFAULT);
    private final RequiredState requiredState = new RequiredState(this);
    private StringProperty textLabelProperty = new SimpleStringProperty("");
/*
    private static PCurrency DEFAULT_CURRENCY = new PCurrency( "RUR" );
    private static boolean IS_INIT_CURRENCY = false;

    private PCurrency currency;
    private static final CalendarTypeViewEnum DEFAULT_TYPE = CalendarTypeViewEnum.KERNEL;
    private CalendarTypeViewEnum type;
    private String idModule;
*/

    private static final String HIDE_FEATURES_PROPERTY = "ru.inversion.hidefeatures";
    /**
     Свойство, отвечающее за сокрытие блока выбора типов валюты и календаря
     @return
     */
    public BooleanProperty hideFeaturesProperty() {
        BooleanProperty bp = this.getProperty( HIDE_FEATURES_PROPERTY );
        if( bp == null ) {
            bp = new SimpleBooleanProperty( this, "hidefeatures" );
            setProperty( HIDE_FEATURES_PROPERTY, bp );
        }
        return bp;
    }
    /**
     Скрывать ли блок выбора типов валюты и календаря (по умолчанию false - не скрывать)
     */
    public void setHideFeatures( boolean enabled ){
        this.hideFeaturesProperty().setValue( enabled );
    }
    /**
     Скрыт ли блок выбора типов валюты и календаря
     */
    public boolean isHideFeatures(){
        return this.hideFeaturesProperty().getValue();
    }

    // Маска. Когда изменяется свойство, меняем форматтер.
    private ObjectProperty<String> mask = new SimpleObjectProperty<String>() {
        public void set(String newValue) {
            super.set(newValue);
            formatter = DateTimeFormatter.ofPattern(newValue);
        }
    };

    public JInvCalendar() {

        super();
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, newV ) );

//        //
//        if(!IS_INIT_CURRENCY && BaseApp.APP() != null ) {
//            DEFAULT_CURRENCY = new PCurrency( BaseApp.APP().getProperties(SMR)
//                                                           .getStringProperty("ru.inversion.app.csmrcur") ); //"RUR"
//            IS_INIT_CURRENCY = true;
//        }

        initEditor();
        initKeyBoard();

        setConverter(new DateConverter());
        initFocusListener();



    }

    protected void initKeyBoard() {

        JInvKeyboardManager.initNaviationOnNode(this);

        addEventFilter( KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            //Не даём нажатию F4 открывать календарь
            if ( event.getCode() == KeyCode.F4 ){
                event.consume();
            }

            if (event.getCode() == KeyCode.F9) {

//                JInvDataPickerSkin skin =
//                        getSkin() instanceof JInvDataPickerSkin
//                        ? (JInvDataPickerSkin) getSkin()
//                        : null;

                if ( isEditable() && !this.stateProperty().get().equals(State.ERROR)) {

                    if (getController() != null && getController().getValidMan() != null) {
                        getController().getValidMan().setFlagOnShowChoiceDialog(false);
                    }
                    event.consume();

//                    if ( skin == null ){
//                        logger.info( "Skin is null!" );
//                        return;
//                    }
                      show();
//                    if ( !skin.isPopupShown() ) {
//                        show();
//                    }
                }
            }
        });
    }

    @Override
    protected Skin< ? > createDefaultSkin() {
        Skin< ? > skin = ( BaseApp.APP() == null ) ? null : BaseApp.APP().foreXXISupport().getFeature( "JInvDataPickerSkin", this );
        return skin == null ? super.createDefaultSkin() : skin;
    }

    @Override
    public void show() {
        if (isEditable()){
            super.show();
        }
    }

    /**
     *
     */
    public JInvCalendar(LocalDate localDate) {
        super(localDate);
        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );
        //this.setEditable(true);
        getEditor().setPrefColumnCount(6);
        initKeyBoard();
    }

//    @Override
//    protected Skin<?> createDefaultSkin() {
//        return new JInvDataPickerSkin(this);
//    }

    /**
     *
     */
    public IStateControl.State getState() {
        return stateProperty.getValue();
    }

    public void setState(IStateControl.State state) {
        stateProperty.setValue(state);
    }

    /**
     * Принудительная установка значения из редактора в компонент.
     * Нужно выполнять в момент нажатия кнопки, конвертер не отрабатывает, если кнопка не фокусируемая
     */
    public void forceCommitValue() {
        if (getEditor() != null) {
            StringConverter c = getConverter();
            if (c != null) {
                Object oldValue = getValue();
                Object value = oldValue;
                String text = getEditor().getText();

                // conditional check here added due to RT-28245
                if (oldValue == null && (text == null || text.isEmpty())) {
                    value = null;
                } else {
                    try {
                        value = c.fromString(text);
                    } catch (Exception ex) {
                        // Most likely a parsing error, such as DateTimeParseException
                    }
                }

                if ((value != null || oldValue != null) && (value == null || !value.equals(oldValue))) {
                    // no point updating values needlessly if they are the same
                    setValue((LocalDate)value);
                }
            }
        }
    }

    public ObjectProperty<IStateControl.State> stateProperty() {
        return stateProperty;
    }

    public String getMask() {
        return mask.get();
    }

    public ObjectProperty<String> maskProperty() {
        return mask;
    }

    public void setMask(String format) {
        this.mask.set(format);
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

    /**
     *
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
     *
     */
    @Override
    public Label getLabel() {
        return (Label) this.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY);
    }

    @Override
    public void setAction(IAction action) {
        JInvKeyboardManager.addAction(this, action);
    }

    public boolean isFromText = false;

    boolean focusState = false;

    // вводим неправильное значение. Нажимаем Tab. Вызывается fromString. Возвращаем прошлое значение. Вызывается toString.
    protected void initEditor() {
        valueProperty().addListener( (v,o,n) -> {
            if ( o != n && getController() != null && getController().getValidMan() != null ) {
//                logger.trace( "validating on value change! ({})", n );
                Platform.runLater( () -> {
                    //на практике за это время обстоятельства могут поменяться
                    if ( getController() != null && getController().getValidMan() != null ) {
                        getController().getValidMan().validateControl(this);
                    }
                } );
            }
        } );

        getEditor().setPrefColumnCount(6);
        getEditor().textProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String value) -> {
//            System.out.println("editorListener. textValue " + value + " oldValue" + oldValue);
            if (JInvValidTooltip.isShowed()) {
                JInvValidTooltip.closeValidTooltip();
            }

            if ((value == null || value.isEmpty())) {
                setState(IStateControl.State.NULL);
                inputFormatter = null;
            } else {
//
                try {
                    parseDate(value);
                    setState(IStateControl.State.VALUE);
                } catch (Throwable ex) {
                    setState(IStateControl.State.ERROR);
                    inputFormatter = null;
                }
            }
        });
    }

    class DateConverter extends StringConverter<LocalDate> {

        public String toString(LocalDate object) {

//            System.out.println("toString " + object);
            // Важен флаг isFromText. Если его убрать при выборе даты через кнопку будет возвращаться старое значение.
            // Старое значение должно сбрасываться если выбираем по кнопке.
            if (isFromText && getState().equals(ERROR)) {
                isFromText = false;
                return getEditor().getText();
            }

            if (object == null) {
                return null;
            }

//            System.out.println("toString return " + object.format(outputFormatter));
            return object.format(outputFormatter);
        }

        public LocalDate fromString(String value) {

            isFromText = true;

//            System.out.println("fromString " + value);
            if (value == null || value.isEmpty()) {
                setState(State.NULL);
                return null;
            }

            try {
                parseDate(value);
                setState(State.VALUE);

//                System.out.println("endFormatter " + inputFormatter);
                if (inputFormatter != null) {

//                    System.out.println("fromString parseLocalDate " + LocalDate.parse(prepareDateString(value), inputFormatter));
                    return LocalDate.parse(prepareDateString(value), inputFormatter);
                } else {
                    return null;
                }
            } catch (Throwable ex) {

//                System.out.println("fromString error. Get value " + getValue());
                return getValue();
            }
        }
    }

    private String prepareDateString(String text) {
        text = text.trim();
        text = text.replaceAll("\\D", "");
        return text;
    }

    protected void parseDate(String text) throws JInvStringWorkerException, JInvStringWorkerValidationException, ParseException, AppException {

        String year = "(\\d\\d\\d\\d)";
        String yearShort = "\\d\\d";
        String mounth = "(0\\d|1[012])";
        String day = "(0\\d|1\\d|2\\d|3[01])";

        String begin = "^";
        String end = "$";
        text = prepareDateString(text);
        if (text.length() == 6 || text.length() == 8) {

            JInvStringWorkerValidationException ex = JInvStringWorker.INSTANCE().checkRegExp(begin + year + mounth + day + end, 0, text, false);
            if (ex == null) {
                inputFormatter = yyyyMMdd;
            }

            ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + year + end, 0, text, false);
            if (ex == null) {
                inputFormatter = ddMMyyyy;
            }

            // 6 символов иногда бывают неоднозначны. Тогда приходится распознавать исходя из приоритета пользователя на это распознавание
            if (BaseApp.APP() != null && BaseApp.APP().getViewPrefService().getDatePrioritet()) {

                ex = JInvStringWorker.INSTANCE().checkRegExp(begin + yearShort + mounth + day + end, 0, text, false);

                if (ex == null) {
                    inputFormatter = yyMMdd;
                } else {
                    ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + yearShort + end, 0, text, false);
                    if (ex == null) {
                        inputFormatter = ddMMyy;
                    }
                }
            } else {

                ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + yearShort + end, 0, text, false);
                if (ex == null) {
                    inputFormatter = ddMMyy;
                } else {
                    ex = JInvStringWorker.INSTANCE().checkRegExp(begin + yearShort + mounth + day + end, 0, text, false);
                    if (ex == null) {
                        inputFormatter = yyMMdd;
                    }
                }
            }

            if (inputFormatter == null) {
                throw new JInvStringWorkerException("Date has invalid format");
            }
        } else {
            throw new JInvStringWorkerException("Text has invalid length");
        }

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
        return textLabelProperty;
    }


    /*

    public void setDefaultCurrency(String val) {
        //currency = new PCurrency(val);
        setProperty("defaultCurrency", val);
    }
    public String getDefaultCurrency() {
        //return currency != null ? currency.getCCURISO() : DEFAULT_CURRENCY.getCCURISO();
        return getProperty("defaultCurrency", "RUR");
    }

    public void setDefaultType(CalendarTypeViewEnum val) {
        //type = val;
        setProperty("defaultType", val);
    }
    public CalendarTypeViewEnum getDefaultType() {
        //return type != null ? type : DEFAULT_TYPE;
        return getProperty("defaultType", DEFAULT_TYPE);
    }

    public void setDefaultModuleId(String val) {
        //idModule = val;
        setProperty( "defaultModuleId", val );
    }
    public String getDefaultModuleId() {
        return getProperty( "defaultModuleId", S.EMPTY_STRING );
    }
    */

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
