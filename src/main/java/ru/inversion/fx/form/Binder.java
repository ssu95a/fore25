package ru.inversion.fx.form;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.IDataSet;
import ru.inversion.db.entity.CheckPattern;
import ru.inversion.db.entity.CheckPatterns;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.IValidatableControl.RequiredStateEnum;
import ru.inversion.fx.form.controls.choicebox.DSChoiceBoxAdapter;
import ru.inversion.fx.form.controls.combobox.DSComboBoxAdapter;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.JInvEntityLov;
import ru.inversion.fx.form.valid.*;
import ru.inversion.fx.form.valid.validators.LengthValidator;
import ru.inversion.fx.form.valid.validators.LovValidator;
import ru.inversion.fx.form.valid.validators.PatternValidator;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;
import ru.inversion.utils.scheck.JInvStringWorkerException;

import java.lang.annotation.Annotation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_EDIT;
import static ru.inversion.fx.form.AbstractBaseController.FormModeEnum.VM_INS;
import static ru.inversion.fx.form.valid.ValidMan.CONTROL_INTERNAL_VALIDATOR_CLASSES;

/**
 *
 * @author ssu @
 */
public class Binder {

    private static final Logger logger = LoggerFactory.getLogger(Binder.class);

    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");

    /** */
    public static void bind( IFXEntity fxEntity, List<? extends Node> controlList, AbstractBaseController ctrl) throws Exception {

        final List<Exception> listException = new ArrayList<>();

        if( !U.containsNull( controlList, fxEntity ) )
        {
            controlList.stream().filter((t) -> (t != null)).forEach((t) -> {
                try {
                    bindControl( ctrl, fxEntity, t );
                } catch (Exception ex) {
                    listException.add(ex);
                }
            });
        }

        bindNonControlComponent( fxEntity, ctrl );

        if( !listException.isEmpty() ) {
            throw new Exception( bundle.getString("OSHIBKA_PRI_SVYAZYVANII_COMPONENTA_I_POLYA_IS_ZAPROSE"), listException.get(0) );
        }
    }

    /** */
    private static <P> void bindControl( AbstractBaseController<P> formController, IFXEntity<P> fxEntity, Node node )
            throws Exception
    {
        if (node instanceof JInvValueField) {
            bindValueControl(formController, fxEntity, (JInvValueField<?>) node);
        } else if (node instanceof DatePicker) {
            bindDatePicker(formController, fxEntity, (DatePicker) node);
        } else if (node instanceof CheckBox) {
            bindCheckBox(formController, fxEntity, (CheckBox) node);
        } else if (node instanceof TextInputControl) {
            bindTextField(formController, fxEntity, (TextInputControl) node);
        } else if (node instanceof JInvComboBox) {
            bindComboBox( formController, fxEntity, (JInvComboBox) node );
        } else if (node instanceof JInvChoiceBox) {
            bindChoiceBox(fxEntity, (JInvChoiceBox) node);
        } else if (node instanceof JInvListView) {
            bindListView(formController, fxEntity, (JInvListView) node);
        }
    }

    /** */
    private static <P> void bindValueControl(AbstractBaseController<P> ctrl, IFXEntity<P> fxEntity, JInvValueField<?> control )
            throws Exception
    {
        final String fieldName = Controls.getFieldNameFromControl(control);

        if( !S.isNullOrEmpty(fieldName) )
        {
            control.valueProperty().bindBidirectional( fxEntity.getProperty(fieldName) );
            generateLov( control, ctrl.getTaskContext() );
            connectValidMan( ctrl, fxEntity.getPropertyDescriptor(fieldName), control  );
        }
        else
            generateLov( control, ctrl.getTaskContext() );
    }

    /** */
    static final private class TFSCnv extends StringConverter< Object > {

        static final private TFSCnv instance = new TFSCnv();

        @Override
        public String toString( Object v ) {

            if( v != null && v.getClass().isEnum() )
                return v.toString();

            return TypeConverter.convert( v, String.class );
        }

        @Override
        public Object fromString( String s ) {
            return s;
        }
    }

    /** */
    private static <P> void bindTextField( AbstractBaseController<P> ctrl, IFXEntity<P> fxEntity, TextInputControl control ) throws Exception
    {
        final String fieldName = Controls.getFieldNameFromControl(control);

        if( !S.isNullOrEmpty(fieldName) )
        {
            control.textProperty().bindBidirectional( fxEntity.getProperty(fieldName), TFSCnv.instance );

            if( control instanceof ILovValueControl )
                generateLov( (ILovValueControl)control, ctrl.getTaskContext() );

            connectValidMan(ctrl, fxEntity.getPropertyDescriptor(fieldName), control);
        }
        else
        if( control instanceof ILovValueControl )
            generateLov( (ILovValueControl)control, ctrl.getTaskContext() );

    }

    /**
     */
    private static void bindCheckBox(AbstractBaseController dialogController, IFXEntity fxEntity, CheckBox control) throws Exception {

        String fieldName = Controls.getFieldNameFromControl(control);

        if (!S.isNullOrEmpty(fieldName)) {

            try {

                if (control instanceof JInvCheckBoxString) {
                    ((JInvCheckBoxString) control).valueProperty().bindBidirectional(fxEntity.getProperty(fieldName));
                } else {
                    control.selectedProperty().bindBidirectional(fxEntity.getProperty(fieldName));
                }

                connectValidMan(dialogController, fxEntity.getPropertyDescriptor(fieldName), control);

            } catch (Throwable th) {
                throw new RuntimeException(Tags.PRODUCT_LABEL + "Error on bindCheckBox for field '" + fieldName + "'", th);
            }
        }
    }

    /**
     */
    private static void bindDatePicker(AbstractBaseController dialogController, IFXEntity fxEntity, DatePicker control) throws JInvStringWorkerException, NoSuchMethodException {
        String fieldName = Controls.getFieldNameFromControl(control);
        if (!S.isNullOrEmpty(fieldName)) {

            IEntityProperty descriptor = fxEntity.getPropertyDescriptor(fieldName);
            if (descriptor != null && descriptor.getType() != null && !(descriptor.getType().equals(LocalDate.class) || descriptor.getType().equals(LocalDateTime.class))) {
                throw new RuntimeException("Некорректный тип в модели при использовании календаря " + descriptor.getType());
            }

            if (control instanceof JInvCalendarTime) {
                ((JInvCalendarTime) control).dateTimeValueProperty().bindBidirectional(fxEntity.getProperty(fieldName));
            } else {
                control.valueProperty().bindBidirectional(fxEntity.getProperty(fieldName));
                control.valueProperty().addListener(new ValidInvalidationListener(control));
            }
            connectValidMan(dialogController, fxEntity.getPropertyDescriptor(fieldName), control);
        }
    }

    /**
     */
    public static <T, P> void bindComboBox(JInvComboBox<T, P> comboBox, IDataSet<T> dataSet, IFXEntity fxEntity, Function<T, P> valueGetter) throws Exception {
        DSComboBoxAdapter a = new DSComboBoxAdapter(dataSet, comboBox, fxEntity, valueGetter);
    }

    public static void connectValidMan(AbstractBaseController dialogController, IEntityProperty pd, Control control) {

        if (U.containsNull(dialogController, dialogController.getValidMan(), pd)) {
            return;
        }

        if (!(control instanceof IValidatableControl)) {
            return;
        }

        ValidMan validMan = dialogController.getValidMan();

//        if (control instanceof IStateControl) {
//            validMan.addValueControl((IStateControl) control);
//        }

        List<Validator> listValidators = new ArrayList<>();

        List<CheckPattern> checkPatternList = new ArrayList<>();

        Annotation annotationPattern = pd.getAnnotation( CheckPattern.class );
        if( annotationPattern != null )
            checkPatternList.add( (CheckPattern) annotationPattern );

        Annotation annotationPatterns = pd.getAnnotation( CheckPatterns.class );
        if(annotationPatterns != null)
            checkPatternList.addAll( Arrays.asList(((CheckPatterns) annotationPatterns).value()) );

        if( !checkPatternList.isEmpty() ){

            for( CheckPattern cp : checkPatternList) {

                //Использовать ли хранимые паттерны из StringWorker'а
                boolean useStoredPatterns = /*cp.pattern().isEmpty() &&*/ !cp.id().isEmpty();
                String pattern = useStoredPatterns ? cp.id() : cp.pattern();

                String errorMes = null;//JInvValidLocalized.getLocalMessage("REGEX_PATTERN_NOT_MATCHED");
                //Использовать ли своё сообщение об ошибке
                final boolean useBundleErrors = S.isNotNullOrEmpty(cp.descr_key()) && S.isNotNullOrEmpty(cp.descr_bundle());

                if( useBundleErrors)
                {
                    final ResourceBundle bundle = ResourceBundle.getBundle(cp.descr_bundle());

                    if (bundle != null && S.isNotNullOrEmpty(bundle.getString(cp.descr_key())))
                    {
                        final String description = bundle.getString(cp.descr_key());
                        errorMes = description;
                    }
                }

                if (S.isNotNullOrEmpty(pattern)) {
                    listValidators.add(new PatternValidator(pattern, cp.group(), useStoredPatterns, errorMes, cp.descr_key()));
                }
            }
            if (control instanceof IJInvControl) {
                IJInvControl ijInvControl = ((IJInvControl) control);
                ijInvControl.setToolTipText(((PatternValidator)listValidators.get(0)).getDescription());
                ijInvControl.setProperty(ValidMan.CONTROL_CHECK_PATTERN, ((PatternValidator)listValidators.get(0)).getDescr_key());
            }
        }
        listValidators.addAll(getInternalValidatorsFromControl(control));

        if (control instanceof JInvTextField) {

            JInvTextField textField = (JInvTextField) control;
            if (textField.isValidateFromLOV() && textField.getLOV() != null) {
                listValidators.add(new LovValidator(textField));
            }
        }

        IValidatableControl validatableControl = (IValidatableControl) control;
        RequiredStateEnum required = validatableControl.getRequiredState();
        switch (required) {
            case MODEL: {
                if (pd.isRequired()) {
                    validMan.addRequiredControl(control);
                }
            }
            break;
            case REQUIRED:
                validMan.addRequiredControl(control);
                break;
        }
        ObjectProperty<RequiredStateEnum> requiredStateProperty = validatableControl.requiredStateProperty();
        requiredStateProperty.addListener((ObservableValue<? extends RequiredStateEnum> ov, RequiredStateEnum t, RequiredStateEnum newValue) -> {
            try {
                switch (newValue) {
                    case MODEL: {
                        if (pd.isRequired()) {
                            validMan.addRequiredControl(control);
                            if (U.in(dialogController.getFormMode(), VM_INS, VM_EDIT)) {
                                validMan.getDecorator().markAndTrack(control, MarkType.REQUIRED );
                            }
                        }
                    }
                    break;
                    case REQUIRED:
                        validMan.addRequiredControl(control);
                        if (U.in(dialogController.getFormMode(), VM_INS, VM_EDIT)) {
                            validMan.getDecorator().markAndTrack(control, MarkType.REQUIRED );
                        }
                        break;
                    case NOT_REQUIRED: {
                        validMan.removeRequiredControl(control);
                        validMan.getDecorator().unmarkAndUntrack(MarkType.REQUIRED, control);
                    }
                    break;

                }
            } catch (Throwable ex) {
                JInvErrorService.handleException("", ex);
            }

        });

        //Валидатор на длину добавляется только если валидаторов на паттерн нету
        if ( pd.getLength() > 0 ) {
            listValidators.add(new LengthValidator(pd.getLength()));
        }

        if (!listValidators.isEmpty()) {
            validMan.getValidators( control ).addAll(listValidators);
        }
    }

    private static <T, R> void bindComboBox( AbstractBaseController dialogController, IFXEntity fxEntity, JInvComboBox<T, R> cb ) {

        String fieldName = cb.getFieldName();

        if (!U.containsNull(fieldName, fxEntity)) {

            if (cb.getValueFactory() != null) {

                cb.valueProperty().addListener((Observable observable) -> {
                    Function<T, R> valueFactory = cb.getValueFactory();
                    fxEntity.getProperty(fieldName).setValue(valueFactory.apply((T) ((ObjectProperty) observable).get()));

                });

                fxEntity.getProperty(fieldName).addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                    cb.setSelectedValue((R) newValue);
                });

            } else {
                cb.valueProperty().bindBidirectional(fxEntity.getProperty(fieldName));
            }

            if (fxEntity.getProperty(fieldName) != null && fxEntity.getProperty(fieldName).getValue() != null) {
                cb.setSelectedValue((R) fxEntity.getProperty(fieldName).getValue());
            }
            connectValidMan(dialogController, fxEntity.getPropertyDescriptor(fieldName), cb);
        }
    }

    private static <T> void bindListView(AbstractBaseController dialogController, IFXEntity fxEntity, JInvListView<T> cb) {

        String fieldName =cb.getFieldName();
        if (!U.containsNull(fieldName, fxEntity)) {
            if (cb.getValueFactory() != null) {

                cb.valueProperty().addListener((Observable observable) -> {
                    Function<T, T> valueFactory = cb.getValueFactory();
                    fxEntity.getProperty(fieldName).setValue(valueFactory.apply((T) ((ObjectProperty) observable).get()));

                });

                fxEntity.getProperty(fieldName).addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                    cb.setSelectedValue((T) newValue);
                });
            } else {
                cb.valueProperty().bindBidirectional(fxEntity.getProperty(fieldName));
            }

            if (fxEntity.getProperty(fieldName) != null && fxEntity.getProperty(fieldName).getValue() != null) {
                cb.setSelectedValue((T) fxEntity.getProperty(fieldName).getValue());
            }

            connectValidMan(dialogController, fxEntity.getPropertyDescriptor(fieldName), cb);
        }
    }

    public static <T, P> void bindChoiceBox(JInvChoiceBox<T, P> choiceBox, IDataSet<T> dataSet, IFXEntity fxEntity, Function<T, P> valueGetter) throws Exception {
        DSChoiceBoxAdapter a = new DSChoiceBoxAdapter(dataSet, choiceBox, fxEntity, valueGetter);
    }

    private static <T, R> void bindChoiceBox(IFXEntity fxEntity, JInvChoiceBox<T, R> cb) {
        String fieldName = cb.getFieldName();
        if (!U.containsNull(fieldName, fxEntity)) {

            if (cb.getValueFactory() != null) {

                cb.valueProperty().addListener((Observable observable) -> {
                    Function<T, R> valueFactory = cb.getValueFactory();
                    fxEntity.getProperty(fieldName).setValue(valueFactory.apply((T) ((ObjectProperty) observable).get()));

                });

                fxEntity.getProperty(fieldName).addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
                    cb.setSelectedValue((R) newValue);
                });

            } else {
                cb.valueProperty().bindBidirectional(fxEntity.getProperty(fieldName));
            }

            if (fxEntity.getProperty(fieldName) != null && fxEntity.getProperty(fieldName).getValue() != null) {
                cb.setSelectedValue((R) fxEntity.getProperty(fieldName).getValue());
            }
        }
    }

    /** */
    public static void generateLov( ILovValueControl control, TaskContext tc ) throws ClassNotFoundException {

        if( control instanceof JInvTextField )
        {
            final JInvTextField field = (JInvTextField) control;

            if( field.getLOV() == null && field.getLovClassName() != null && !field.getLovClassName().trim().isEmpty() ) {
                final AbstractLovBase<?> lov = JInvEntityLov.generateLov( field, tc, null );
                field.setLOV(lov);
            }
        }
    }

    /**
     * Связывание прикладных компонентов, которые есть на contentPane
     *
     * @param fxEntity
     * @param dialogController
     */
    private static void bindNonControlComponent(IFXEntity fxEntity, AbstractBaseController dialogController) {

        if (!U.containsNull(fxEntity, dialogController)) {

            List<Object> listNonControlComponents = Controls.getNonControlComponentFromContainer(dialogController.getContentPane());
            listNonControlComponents.forEach((Object t) -> {
                if (t instanceof JInvRadioGroup) {
                    JInvRadioGroup group = ((JInvRadioGroup) t);

                    String fieldName = group.getFieldName();
                    Class clazz = fxEntity.getPropertyDescriptor(fieldName).getType();

                    group.valueProperty().bindBidirectional(fxEntity.getProperty(fieldName), new TypeStringConverter(clazz));

                    //чтобы не ругаться до привязки всех кнопок
                    group.setInitialized(true);
                }
            });

        }
    }

    public static Collection<Validator> getInternalValidatorsFromControl(Control control) {
        List<Validator> result = new ArrayList<>();

        if (control != null
                && control.getProperties().getOrDefault(CONTROL_INTERNAL_VALIDATOR_CLASSES, null) != null
                && control.getProperties().getOrDefault(CONTROL_INTERNAL_VALIDATOR_CLASSES, null) instanceof List) {

            List<Class> listClasses = (List<Class>) control.getProperties().getOrDefault(CONTROL_INTERNAL_VALIDATOR_CLASSES, null);
            listClasses.forEach(new Consumer<Class>() {
                @Override
                public void accept(Class t) {

                    try {
                        Object instance = t.getConstructor(Control.class).newInstance(control);
                        if (instance instanceof Validator) {
                            result.add((Validator) instance);
                        }
                    } catch (Throwable ex) {
                        Object instance;
                        try {
                            instance = t.newInstance();
                            if (instance instanceof Validator) {
                                result.add((Validator) instance);
                            }
                        } catch (Throwable ex1) {
                            logger.error("Error while getInternalValidatorsFromControl", ex1);
                        }

                    }
                }
            });
        }
        return result;
    }
}