package ru.inversion.fx.form.valid;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import javafx.util.Pair;
import org.slf4j.Logger;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.valid.validators.LovValidator;
import ru.inversion.fx.form.valid.validators.PatternValidator;
import ru.inversion.utils.S;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.AbstractBaseController.CSS_CANCEL_BUTTON_CLASS;
import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;
import static ru.inversion.fx.form.valid.Validator.Result;

/** ValidMan v2
 @author fomishkin on 09.07.2018. */
public class ValidMan implements Serializable {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("valid");
    private static final Logger logger = getLogger( lookup().lookupClass() );

    /** Ключ свойства для компонентов, валидируемых по потере фокуса */
    public static final String CONTROL_FOCUS_LISTENER = "ru.inversion.validman.focuslistener";

    /** Константа для классов внутренних валидаторов компонентов. Используется например в поле ввода валюты */
    public static final String CONTROL_INTERNAL_VALIDATOR_CLASSES = "ru.inversion.valid_classes";

    /** Константа для классов внутренних валидаторов компонентов. Используется например в поле ввода валюты */
    public static final String CONTROL_CHECK_PATTERN = "ru.inversion.check_pattern";

    /** Валидатор обязательности заполнения */
    private final ReqValidator reqValidator = new ReqValidator();

    /** Валидатор состояния компонентов */
    private final StateValidator stateValidator = new StateValidator();
    /** Валидаторы для компонентов */
    private final ObservableList<Pair<Control, Validator>> cvList = FXCollections.observableList(new LinkedList<>());

    /** Валидаторы формы */
    private final ObservableList<Validator> fvList = FXCollections.observableList(new LinkedList<>());

    /** Валидаторы формы переданные извне при инициализации, выполняются последними **/
    private final ObservableList<Validator> externalFvList;

    private final AbstractBaseController<?> controller;

    /** Конструктор. Инициализирует отслеживаемые списки */
    public ValidMan( AbstractBaseController<?> controller, final Validator... externalValidators ) {
        this.controller = controller;
        this.externalFvList = FXCollections.observableList(
                externalValidators == null ? new LinkedList<>() : new LinkedList<>(Arrays.asList(externalValidators))
        );
        initObservableLists();
    }
    private void initObservableLists() {
        cvList.addListener( (ListChangeListener<Pair<Control, Validator>>) change -> {
            change.next();
            change.getAddedSubList().forEach( pair -> {
                Control control = pair.getKey();
//                int validatorCount = getValidators( control ).size();
//                logger.info( "{}: added new validator. total count {}", control, validatorCount );
                initListenersOnControl( control );
                decorator.markAndTrack( control, MarkType.VALIDATABLE );
            } );
            change.getRemoved().stream().map( Pair::getKey ).forEach( control -> {
                //Снимаем окраску только если валидаторов больше не осталось
                int validatorCount = getValidators( control ).size();
                if ( validatorCount == 0 ) {
                    decorator.unmarkAndUntrack( MarkType.VALIDATABLE, control );
                }
//                logger.info( "{}: removed validator. {} validators left", control, validatorCount );
            } );
        } );
    }

    /** При нажатии кнопки событие мыши поглощается и кладётся в буфер с целью проведения валидации,
     * И только если валидация прошла успешно, событие передается компоненту */
    private Pair<Control, MouseEvent> onSuccess = new Pair<>( null, null );

    /** Флаг открытия диалога выбора, в этом случае валидация и калькуляция не запускаются */
    private boolean choiceDialogShown = false;

    /** Флажок для нужд слушателя фокуса. Чтобы два раза не срабатывал */
    private boolean doNotValidate = false;

    /** Калькуляторы */
    private final Map<Calculator, List<Control>> calculatorMap = new LinkedHashMap<>();

    /** Декоратор отвечающий за визуальное представление ошибок и обязательных контролов */
    private static final ValidViewDecorator decorator = ValidViewDecorator.INSTANCE();

    /** Валидатор состояния компонентов */
    public static class StateValidator {
        private List<IStateControl> list = new ArrayList<>();

        void init( Parent form ) {
            this.list = Controls.getControlList(form, (Control t) -> t instanceof IStateControl).stream()
                    .map((Control t) -> (IStateControl) t).collect(Collectors.toList());
        }
        public boolean validate() {
            for (IStateControl control : list) {
                if (control.stateProperty().get().equals(ERROR) && !((IJInvControl)control).isReadOnly() ) {
                    control.showError();
                    return false;
                }
            }
            return true;
        }
    }

    /** Инициализирует StateValidator для всех подходящих компонентов с переданной формы */
    public void initStateValidator(Parent form) {
        stateValidator.init(form);
    }

    /** Возвращает изменяемый список валидаторов по переданному контролу.
     При добавлении/удалении валидаторов из списка оные автоматически привязываются/отвязываются от контрола*/
    public ObservableList<Validator> getValidators( final Control control ) {
        final ObservableList<Validator> list = FXCollections.observableList( new LinkedList<>() );
        if ( control != null) {
            cvList.forEach(t -> {
                if (t.getKey().equals( control )) {
                    list.add(t.getValue());
                }
            });
        }
        list.addListener( (ListChangeListener<? super Validator>) change -> {
            change.next();
            change.getAddedSubList().forEach( validator -> {
                if ( validator != null ){
                    cvList.add( new Pair<>( control, validator ) );
                }
            } );
            change.getRemoved().forEach( validator -> {
                cvList.removeIf( pair -> pair.getValue().equals( validator ) );
            });
        } );
        return list;
    }

    /** Возвращает декоратор */
    public ValidViewDecorator getDecorator() {
        return decorator;
    }

    /** Пометка контролов */
    public void markControls() {
        reqValidator.markControls();
    }

    /** Регистрация компонентов с обязательным наполнением */
    public void addRequiredControl( Control... controls ) {
        reqValidator.addControls(controls);
    }

    /**
     * Установить кастомный валидатор, обязательности заполнения.
     * Вызывается после срабатывания штатного валидатора
     *
     * @param сontrol компонент к которому устанавливается валидатор
     * @param vldtr логика проверки, на вход - проверяемый компонент, возврат true - валидное состояние, false - нет
     *              если vldtr null, то очищается валидатор
     */
    public void setCustomReqValidator( Control control, Function<Control,Boolean> vldtr )
    {
        reqValidator.setCustomValidator( control, vldtr );
    }

    /** Снятие признака обязательности с компонентов */
    public void removeRequiredControl(Control... controls) {
        reqValidator.removeControls(controls);
        decorator.unmarkAndUntrack(MarkType.REQUIRED, controls);
    }

    /** Если фактический обладатель фокуса и проверяемый контрол совпадают - проверять не нужно. */
    private boolean isFocusOwner( final Control control )
    {
        return control.getScene() != null && control.equals( control.getScene().getFocusOwner() );
    }

    private boolean isInErrorState( final Control control ) {
        return control instanceof IStateControl && ((IStateControl) control)
                .stateProperty().get().equals(ERROR);
    }

    private boolean isReadOnly( final Control control ) {
        return control instanceof IJInvControl && ( (IJInvControl) control ).isReadOnly();
    }

    /** Возвращает признак проведенной валидации у компонента */
    public boolean isControlValidated( Control control ) {
        return Controls.isControlValidated(control);
    }

    /** Устанавливает признак успешно проведенной валидации на компоненте */
    public void setControlValidated(Control control, boolean val) {
        Controls.setControlValidated(control, val);
    }

    /**
     Возвращает контроллер
     */
    public AbstractBaseController<?> getController() {
        return controller;
    }

    /** Флаг запущенной процедуры валидации */
    private boolean isInValidation = false;
    /** Флаг провала валидации вне основной процедуры валидации, нужен в редких случаях */
    private boolean isExternalOk = true;

    /** Запуск процедуры валидации на форме */
    public boolean validate() {
        isExternalOk = true;
        isInValidation = true;
        // 0. Если сообщение об ошибке валидации уже отображено,
        // значит запускать валидацию не нужно, выходим из метода
        boolean isOk = !JInvValidTooltip.isShowed();

//        logger.trace("isOk = {}", isOk);

        // 1. Валидируем состояние компонентов stateValidator
        if ( isOk ){
            isOk = stateValidator.validate();
        }
        // 2. Валидируем обязательность заполнения reqValidator
        if ( isOk ){
            isOk = reqValidator.validate( getDecorator() );
        }
        // 3. Запускаем валидацию пар "Контрол-Валидатор"
        if ( isOk ){
            isOk = validateCvPairs();
        }
        // 4. Запускаем валидаторы формы
        if ( isOk ){
            isOk = validateFormValidators();
        }

        if ( !isExternalOk ){
            isOk = isExternalOk;
            isExternalOk = true;
        }

        isInValidation = false;
        return isOk;
    }

    private void reportValidationFailure(){
        if (isInValidation){
            isExternalOk = false;
        }
    }

    /** Нужно, чтобы ошибкам в validateFormValidators() было, куда прицепиться */
    private Window lastUsedWindow = null;

    /** Валидация всех пар "контрол-валидатор" */
    private boolean validateCvPairs(){
        lastUsedWindow = null;

        List<Pair<Control, Validator>> cvList = new ArrayList<>(this.cvList);
        for( Pair<Control, Validator> pair : cvList )
        {
            Control   control   = pair.getKey();
            Validator validator = pair.getValue();
            if ( lastUsedWindow == null && control.getScene() != null ) {
                lastUsedWindow = control.getScene().getWindow();
            }
            // Сделано для динамического включения валидации полей с ловом
            if ( isDisabledStateValidator( validator ) ) {
                continue;
            }
            Result errorResult = null;
            if ( validator instanceof LovValidator ) {
                errorResult = ( (LovValidator) validator ).validateOnOk( Controls.getValue( control ) );
            }
//          Сделано сознательно (JAVAKERNEL-1055):
//          else {
//              errorResult = validator.validate( Controls.getValue( control ) );
//          }
            if ( errorResult != null ) {
                decorator.markControlOnError( control, errorResult );
                return false;
            }
        }
        return true;
    }

    /** Запуск валидаторов формы */
    private boolean validateFormValidators() {
        if ( !internalValidateFormValidators( fvList ) ) {
            return false;
        }
        if ( !internalValidateFormValidators( externalFvList ) ) {
            return false;
        }
        return true;
    }

    private boolean internalValidateFormValidators( final ObservableList<Validator> formValidators ) {
        for( Validator v : formValidators ) {
            // Сделано для динамического включения валидации полей с ловом
            if( isDisabledStateValidator( v ) ) {
                continue;
            }

            Result resultValidate = v.validate(controller);

            if( resultValidate != null ) {
                Exception ex = resultValidate.getException();

                if( ex == null ){
                    Alerts.error( lastUsedWindow, bundle.getString("ERROR_INPUT_TITLE"), resultValidate.getDescription() );
                } else {
                    JInvErrorService.handleException( lastUsedWindow, ex );
                }

                // Пост обработчик если нуна
                if( resultValidate.getUserData() != null && resultValidate.getUserData() instanceof Runnable )
                {
                    Platform.runLater( (Runnable)resultValidate.getUserData() );
                }
                return false;
            }
        }
        return true;
    }

    /** Флажок, который говорит о том, чтобы не сбрасывать флаг измененности значения после валидации */
    private boolean keepChangedState;

    /** Оставлять ли (не изменять) флаг измененности значения после валидации */
    public void setKeepChangedState(boolean keep) {
        keepChangedState = keep;
    }

    /** Валидация конкретного компонента */
    public void validateControl(Control control) {
        Validator.Result[] result = new Validator.Result[1];

        // Валидируем компонент
        if ( validateControlInternal( control, result ) ) {
            if (!choiceDialogShown) {
                //logger.trace("Validation is ok for {}", control);

                // В случае если вызывается диалог выбора, например ЛОВ то валидация точно пройдет, но признак что валидация прошла успешно не ставим
                // Калькуляцию в этом случае не запускаем.
                if ( !keepChangedState ) {
                    setControlValueChanged(control, false);
                }

                setControlValidated(control, true);

                // после конвертации, проверки лова и валидаторов ищем и запускаем калькуляторы
                reCalculate( control );
            }

            choiceDialogShown = false;
            fireOnSuccess();
        } else {
            // Если валидация не прошла
            //logger.trace("Validation is not ok for {}", control);

            // Возвращаем фокус
            doNotValidate = true;
            control.requestFocus();
            doNotValidate = false;

            // Показываем сообщение об ошибке
            if (result[0] != null) {
                getDecorator().markFocusedControlOnError(control, result[0]);
            }
            reportValidationFailure();
        }
    }

    private <T> boolean validateControlInternal( Control control, Validator.Result[] result ) {
        Result validResult = null;
        T value = (T)Controls.getValue( control );

        List<Pair<Control, Validator>> cvList = new ArrayList<>(this.cvList);
        String checkPattern = (String)control.getProperties()
                .getOrDefault(ValidMan.CONTROL_CHECK_PATTERN, null);
        for( Pair<Control, Validator> p : cvList ) {

            if( p.getKey().equals(control) ) {

                if( !choiceDialogShown ) {
                    Validator<T> validator = p.getValue();

                    if( isDisabledStateValidator( validator ) ) {
                        continue;
                    }

                    if(validator instanceof PatternValidator
                            && !checkPattern.equals( ((PatternValidator)validator).getDescr_key() ))
                    {
                        continue;
                    }

                    validResult = validator.validate(value);

                    if( validResult != null ) {
                        result[0] = validResult;
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Является ли валидатор отключённым валидатором состояния?
     Сделано для динамического включения/отключения валидации полей с ловом */
    private boolean isDisabledStateValidator( final Validator validator ) {
        return validator instanceof IStateValidator && ((IStateValidator) validator).isDisabled();
    }

    public void setFlagOnShowChoiceDialog( boolean newFlag ) {
        choiceDialogShown = newFlag;
    }

    /** Инициализация слушателя на изменение значения. Если значение не было изменено, то и валидировать не нужно. */
    private void initChangeListener(Control control) {
        ValidInvalidationListener changeListener = new ValidInvalidationListener(control, this);

        if (control instanceof JInvValueField ) {
            ((JInvValueField) control).valueProperty().addListener(changeListener);
        } else if (control instanceof TextInputControl ) {
            ((TextInputControl) control).textProperty().addListener(changeListener);
        } else if (control instanceof JInvCheckBoxString ) {
            ((JInvCheckBoxString) control).valueProperty().addListener(changeListener);
        } else if (control instanceof JInvCheckBox ) {
            ((JInvCheckBox) control).selectedProperty().addListener(changeListener);
        } else if (control instanceof JInvCalendarTime ) {
            ((JInvCalendarTime) control).dateTimeValueProperty().addListener(changeListener);
            addCalendarFocusListener( control );
        } else if (control instanceof JInvCalendar ) {
            ((JInvCalendar) control).valueProperty().addListener(changeListener);
            addCalendarFocusListener( control );
        } else if (control instanceof JInvComboBox ) {
            ((JInvComboBox) control).valueProperty().addListener(changeListener);
        } else if (control instanceof JInvChoiceBox ) {
            ((JInvChoiceBox) control).valueProperty().addListener(changeListener);
        } else if(control instanceof JInvListView) {
            ((JInvListView) control).valueProperty().addListener(changeListener);
        }

    }
    /** Передёргивание фокуса контрола по факту изменения состояния компонента, если слушатель изменений запоздал
     Актуально для динамически добавляемых календарей, см. JAVAKERNEL-967 */
    private void addCalendarFocusListener( final Control control ) {
        getValueChangedProperty( control ).addListener( ( obs, wasChanged, isChanged ) -> {
            final Scene scene = control.getScene();
            if ( !isChanged || wasChanged || scene == null ) {
                return;
            }
            Platform.runLater( () -> {
                if ( !getValueChangedProperty( control ).getValue() ) {
//                    logger.trace( "Already checked!" );
                    return; //Отступаем, коли уже валидировано
                }
                Node focusOwner = scene.getFocusOwner();
                if ( focusOwner != null && !control.equals( focusOwner ) ){
                    control.requestFocus();
                    focusOwner.requestFocus();
                }
            } );
        } );
    }

    /** Устанавливает компонент, которому нужно передать событие мыши, в случае если валидация завершится успешно */
    public void setControlAfterValidation(Control control, MouseEvent event) {
        onSuccess = new Pair<>( control, event );
    }

    /** Метод возвращает установлены ли для компонента валидаторы */
    boolean isControlInValidation( Node node ) {
        return cvList.stream().anyMatch((Pair<Control, Validator> t) -> t.getKey().equals(node));
    }

    /** Запуск события, которое должно было произойти,
     но было перехвачено чтобы провалидировать уход фокуса с компонента */
    private void fireOnSuccess() {
        Control control = onSuccess.getKey();
        MouseEvent event = onSuccess.getValue();
        if ( control != null && event != null) {
//            logger.info( "fireOnSuccess: {}", event.getEventType() );
            control.fireEvent( event.copyFor( event.getSource(), event.getTarget() ) );

            if ( event.getEventType().equals(MouseEvent.MOUSE_PRESSED) ){
                MouseEvent releaseEvent = event.copyFor( event.getSource(), event.getTarget(), MouseEvent.MOUSE_RELEASED );
                Platform.runLater( () -> {
//                    logger.info( "fireOnSuccess additional: {}", releaseEvent.getEventType() );
                    control.fireEvent( releaseEvent );
                    releaseEvent.consume();
                } );
            }

            event.consume();

            clearSuccessEvent();
        }
    }

    /** Привязка валидаторов к компоненту.
     @see #getValidators(Control) */
    public void bindValidators2Control( Control c, Validator... validators ) {
        getValidators( c ).addAll( validators );
    }

    /** Отвязка валидаторов от компонента.
     @see #getValidators(Control) */
    public void unbindValidators( Control c, Validator... validators ) {
        getValidators( c ).removeAll( validators );
    }

    /** Привязка компонентов к валидатору */
    public void bindControls2Validator( Validator v, Control... controls ) {
        if( v == null ){
            return;
        }

        for( Control c1 : controls ) {
            if( c1 != null ){
                cvList.add( new Pair<>(c1, v) );
            }
        }
    }

    /** Привязка валидатора к форме */
    public void addFormValidator(Validator... vs) {
        if ( vs == null || vs.length == 0 ) {
            return;
        }
        for ( Validator v : vs ) {
            if ( v != null ) {
                fvList.add(v);
            }
        }
    }

    /** Отвязка валидатора от формы */
    public void removeFormValidator(Validator... vs) {
        if ( vs == null || vs.length == 0 ) {
            return;
        }
        for ( Validator v : vs ) {
            if ( v != null ) {
                fvList.remove(v);
            }
        }
    }

    /** Инициализация слушателей для валидации */
    private void initListenersOnControl( Control control ) {
        // Не добавляем слушатель фокуса повторно
        if( control == null || Controls.<Boolean>getProperty( control, CONTROL_FOCUS_LISTENER, false ) ){
            return;
        }

        Controls.setProperty( control, CONTROL_FOCUS_LISTENER, true );

        //Регистрирование слушателя на изменение значения
        initChangeListener( control );

        // Слушатель на потерю фокуса.
        ChangeListener<Boolean> focusLostListener = ( v, o, newValue ) -> {
            // Не запускаем валидацию когда фокус пришёл, а не ушёл
            // или когда это компонент IStateControl и имеет состояние ERROR
            // или когда он только для чтения
            if ( newValue || doNotValidate || isInErrorState(control) || isReadOnly(control) || isFocusOwner(control) ){
                clearSuccessEvent();
                return;
            }
            try {

                Scene scene = control.getScene();
                // Случается если контрол остался на другом, ныне скрытом табе
                if( scene == null )
                    return;

                // Не запрашиваем фокус, если окно уже закрыто
                boolean isWindowClosed = !scene.getWindow().showingProperty().get();
                if( isWindowClosed )
                    return;

                // Не запрашиваем фокус, если пользователь щелкает по кнопке отмены
                boolean isPressingCancel = scene.getFocusOwner().getStyleClass().contains( CSS_CANCEL_BUTTON_CLASS );
                if ( isPressingCancel ) {
                    return;
                }

            } catch ( Throwable ex ) {
                logger.error( "focusLostListener exception", ex );
            }
//            logOnControlFocusChanged(control);
            //Если контрол не проверен, не изменялся и пустой, то не проверяем его по смене фокуса
            boolean noCheckNeeded = !isControlValueChanged( control ) && !isControlValidated( control ) && Controls.getValue( control ) == null;

            // Если фокус ушел с компонента и он еще не проверем, то проверяем его
            if ( !noCheckNeeded && ( isControlValueChanged( control ) || !isControlValidated( control ) ) ) {
                validateControl( control );
            } else {
                fireOnSuccess();
            }
            clearSuccessEvent();
            choiceDialogShown = false;
        };
        control.focusedProperty().addListener( focusLostListener );
    }

//    /** Щёлкает ли пользователь по кнопкам заголовка в режиме MDI в данный момент? */
//    private boolean isMdiElement() throws AppException {
//        boolean isMDI = BaseApp.APP().getViewPrefService().getFrameMode().equals(MDI);
//        if ( !isMDI ) {
//            return false;
//        }
//
//        Optional<ObservableList<String>> focusOwnerStyle = getFocusOwnerStyle();
//        if (!focusOwnerStyle.isPresent()){
//            return false;
//        }
//        return focusOwnerStyle.get().contains( CSS_MDI_ICON_CLASS );
//    }
//
//    private static Optional<ObservableList<String>> getFocusOwnerStyle(){
//        JInvMainFrame mainFrame = BaseApp.APP().getMainFrame();
//        if( mainFrame != null )
//        {
//            Pane mdiPane = mainFrame.getMdiPane();
//
//            if (mdiPane != null)
//            {
//                Scene scene = mdiPane.getScene();
//                if (scene != null)
//                {
//                    Node focusOwner = scene.getFocusOwner();
//                    if (focusOwner != null){
//                        return Optional.ofNullable(focusOwner.getStyleClass());
//                    }
//                }
//            }
//        }
//        return Optional.empty();
//    }

    /** Уничтожение события */
    private void clearSuccessEvent() {
        onSuccess = new Pair<>( null, null );
    }

    //Calculator zone
    /** Привязка к калькулятору компонентов */
    public <T> void bindControls2Calculator(Calculator<T> calculator, Control... controls) {
        if (calculator == null || controls == null) {
            return;
        }

        if (calculatorMap.containsKey(calculator)) {
            calculatorMap.get(calculator).addAll(Arrays.asList(controls));
        } else {
            calculatorMap.put(calculator, new ArrayList<>( Arrays.asList(controls) ));

            if (calculator instanceof AbstractCalculator) {
                ((AbstractCalculator) calculator).setValidMan(this);
            }
        }
        for( Control c : controls ) {
            initListenersOnControl(c);
        }
    }

    /** после концертации, проверки лова и валидаторов ищем и запускаем калькуляторы */
    private void reCalculate(Control control) {
        calculatorMap.keySet().stream()
                .filter( (Calculator t) -> calculatorMap.get(t).contains(control) )
                .forEach((Calculator t) -> {
                    try {
                        t.calculate(Controls.getValue(control), control);
                    } catch (Throwable ex) {
                        JInvErrorService.handleException(null, ex);
                    }
                });
    }

    /** Возвращает лист контролов, привязанных к данному калькулятору */
    public List<Control> getControlListByCalculator(Calculator cal) {
        return calculatorMap.getOrDefault( cal, null );
    }

    /** Запуск калькуляторов, у которых включен признак runOnStartForm */
    public void runCalculators( ) {
        calculatorMap.keySet().stream()
                .filter(Calculator::runOnStartForm)
                .forEach((Calculator calculator) -> {
                    if (calculatorMap.get(calculator).size() == 1) {
                        Control control = calculatorMap.get(calculator).get(0);
                        Object value = Controls.getValue(control);
                        calculator.calculate(value, control);
                    } else {
                        calculator.calculate();
                    }
                });
    }

    //Static zone
    /** Возвращает признак изменённого значения на переданной Node */
    public static boolean isControlValueChanged(Node control) {
        return Controls.isControlValueChanged(control);
    }

    /** Возвращает свойство с признаком изменённого значения на переданной Node */
    public static BooleanProperty getValueChangedProperty( Node control ) {
        return Controls.getValueChangedProperty(control);
    }

    /** Устанавливает признак измененного значение на переданной Node */
    public static void setControlValueChanged(Node control, boolean val) {
        Controls.setControlValueChanged(control, val);
    }
}