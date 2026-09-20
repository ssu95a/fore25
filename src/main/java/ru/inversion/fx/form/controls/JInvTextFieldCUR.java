package ru.inversion.fx.form.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import org.slf4j.Logger;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.LifeCycleStateEnum;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.skin.JInvTextFieldButtonSkin;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;
import ru.inversion.fx.form.lov.JInvEntityLov;
import ru.inversion.fx.form.lov.JInvLOVButton;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.utils.ResourceBundleFactory;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.lov.JInvEntityLov.LOV_VALIDATE_FROM_LOV;
import static ru.inversion.fx.form.valid.ValidMan.CONTROL_INTERNAL_VALIDATOR_CLASSES;

/**
 *
 * @author ssu
 */
public class JInvTextFieldCUR extends JInvTextField {

    // private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass());

    private static final ResourceBundle bundle = ResourceBundle.getBundle("valid");

    private JInvEntityLov<PCurLov,String> lovCur;
    private boolean fromSetText  = false;

    public JInvTextFieldCUR() {

        readOnlyProperty().addListener( (obs, oldV, newV) -> Controls.disableControl( this, obs.getValue() ) );

        this.setAlignment(Pos.CENTER);
        this.prefColumnCountProperty().set(3);
        this.setMaxWidth(USE_PREF_SIZE);

        super.setCaseSensitiveMode(RegisterEnum.UPPER_CASE);

        // Устанавливаем кнопку внутрь внутренней кнопки филда
        final JInvLOVButton button = new JInvLOVButton();
        ((JInvTextFieldButtonSkin) getSkin()).getButton().setInnerButton(button);
        button.setTextField(this);
        button.setToolTipText(ResourceBundle.getBundle("fore").getString( "LOV_BUTTON" ) );

        List<Class> validatorList = new ArrayList<>();
        validatorList.add(CURValidator.class);
        super.setValidateFromLOV(false);

        getProperties().put(CONTROL_INTERNAL_VALIDATOR_CLASSES, validatorList);

        // Принимаем написанное в textProperty до afterInit за чистую монету
        textProperty().addListener(  (v,o,n) -> {
            if ( getController() != null && U.in(getController().getLifeCycleState(), LifeCycleStateEnum.BEFORE_INIT, LifeCycleStateEnum.INIT) ){
                updateProperty();
            }
        } );
    }

    /*
    @Override
    protected void initTrimmer( )
    {
        focusedProperty().addListener( ( observable, oldValue, newValue ) -> {

                    if( !newValue && !isReadOnly() )
                    {
                        if( getLOV() == null || !isValidateFromLOV() )
                        {
                            String trimmedText = textProperty().getValueSafe().trim();

                            if( !S.isNullOrEmpty(trimmedText) && trimmedText.length() < 3 )
                                trimmedText += S.space( 3 - trimmedText.length(), ' ' );

                            if( textProperty().getValueSafe().equals( trimmedText ) )
                                return;

                            textProperty().setValue( trimmedText );
                        }
                    }//end if
                }
        );
    }
    */

    final private StringProperty curValueProperty = new StringPropertyBase() {
        @Override
        public Object getBean() {
            return JInvTextFieldCUR.this;
        }

        @Override
        public String getName() {
            return "curValue";
        }

        @Override
        public void set(String newValue) {

            String value = newValue == null ? S.EMPTY_STRING : newValue.trim();

            super.set(value);

            if( !fromSetText )
                setText(value);
        }
    };

    /** */
    private void updateProperty( ) {
        fromSetText = true;
        try {
            curValueProperty.set( getText() );
        } finally {
            fromSetText = false;
        }
    }

    /** */
    public StringProperty curValueProperty() {
        return curValueProperty;
    }

    /**
     *
     */
    public String getCurValue() {
        return curValueProperty().get();
    }

    public void setCurValue(String cur) {
        curValueProperty().set(cur);
    }

    /** */
    @Override
    public void showLOV(BiConsumer<Boolean, ILov> clb) {

        if( getLOV() != null )
        {
            getLOV().setSkipFilterString( isSkipFilter() );
            getLOV().showChoiceList     ( ViewContext.of(getScene().getWindow()), getText(), new BiConsumer<Boolean, ILov>() {
                @Override
                public void accept( Boolean t, ILov u )
                {
                    if (t)
                    {
                        setText( TypeConverter.convert( getLOV().getValue(), String.class) );
                        updateProperty();
                    }

                    if( clb != null )
                        clb.accept( t, u);
                }
            });
        }
    }

    @Override
    public AbstractLovBase getLOV() {
        if ( super.getLOV() == null ){
            return getLovInternal();
        }
        return super.getLOV();
    }

    /** */
    private JInvEntityLov<PCurLov,String> getLovInternal( ) {

        if( lovCur == null ) {
            lovCur = new JInvEntityLov<>(PCurLov.class);
            lovCur.setTaskContext   ( BaseApp.APP().getCommonTaskContext() );
            lovCur.setResourceBundle( ResourceBundleFactory.INSTANCE().getBundle(PCurLov.class) );
        }
        return lovCur;
    }

    @Override
    public boolean isValidateFromLOV() {
        return true;
    }

    @Override
    public void setValidateFromLOV(boolean valFromLOV) {
        //getLovInternal().setProperty(LOV_VALIDATE_FROM_LOV, valFromLOV);
    }

    /** */
    @Deprecated
    public void setWherePredicat( String predicat ) {
        getLovInternal().setWherePredicat( predicat );
    }

    /**
     *
     */
    public String getWherePredicat() {
        return getLovInternal().getWherePredicat();
    }

    public void setFilter( String filterString ) {
        getLovInternal().setFilter( filterString );
    }

    public void setOrderBy( String orderBy ) {
        getLovInternal().setChoiceOrderBy( orderBy );
    }

    public String getFilter() {
        return getLovInternal().getFilter();
    }

    /**
     *
     */
    public String num2Code(int num) throws SQLException {

        //JInvEntityLov lov = getLovInternal();

        //String where = lov.getWherePredicat();

        String retValue = null;

        final String strSql ="SELECT cCurISO FROM cur WHERE cCurISO = ? \n" +
                "UNION ALL \n" +
                "SELECT cCurISO FROM cur WHERE iCurISO = ? \n" +
                "UNION ALL \n" +
                "SELECT cCurISO FROM cur WHERE cCurCode= ?";

        try( PreparedStatement ps = BaseApp.APP().getCommonTaskContext().getConnection().prepareStatement(strSql) ) {
            ps.setString( 1, Integer.toString(num) );
            ps.setInt   ( 2, num );
            ps.setString( 3, num > 99 ? Integer.toString(num) : "0" + num );
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    retValue = rs.getString(1);
                }
            }
        }

        return retValue;
    }

    public static class CURValidator implements Validator<String> {

        private JInvTextFieldCUR control;

        public CURValidator( Control control ) {

            if (control instanceof JInvTextFieldCUR)
                this.control = (JInvTextFieldCUR) control;
        }

        @Override
        public Validator.Result validate( String value ) {

            try {

                Validator.Result result = null;
                boolean valid = false;

                if( control != null )
                {
                    if( S.isNullOrEmpty(value)
                        ||
                        (  U.equals( control.getText(), control.getCurValue() ) )
                    )
                    {
                        valid = true;
                    }
                    else
                    {
                        /*
                        if (isValueHasOnlyDigitCharacters(value))
                        {
                            value = control.num2Code(Integer.parseInt(value));
                            if (value != null) {
                                control.setText(value);
                                valid = true;
                            }
                        }
                        else
                        {
                            value = value.toUpperCase();

                            if (control.getLOV().checkValue(value)) {
                                control.setText(value);
                                valid = true;
                            }
                        }
                        */


                        if( isValueHasOnlyDigitCharacters(value) )
                        {
                            // Если ввели все цифровые символы.
                            // Считаем что ввели цифровой ISO код валюты
                            // пытаемся по нему определить символьный
                            String s = control.num2Code(Integer.parseInt(value));
                            if ( s != null)
                                value = s;
                        }
                        else
                            value = value.toUpperCase();

                        if( !control.isValidateFromLOV() || control.getLOV().checkValue(value) ) {
                            control.setText(value);
                            valid = true;
                        }
                    }

                    if (!valid) {
                        return new Result(null, bundle.getString("LOV_VALIDATE"));
                    } else {
                        control.updateProperty();
                    }
                }
                return result;
            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
                return new Result(null, bundle.getString("LOV_VALIDATE"));
            }
        }

        private boolean isValueHasOnlyDigitCharacters(String value) {
            boolean result = true;
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    result = false;
                    break;
                }
            }
            return result;
        }

    }

    /** */
    public BooleanProperty skipFilterProperty() {
        return getProperty( "skipFilter", (o) ->new SimpleBooleanProperty( this, "skipFilter", false ) );
    }
    /** */
    public boolean isSkipFilter() {
        return hasProperty("skipFilter") && skipFilterProperty().get();
    }
    /** */
    public void setSkipFilter( boolean v ) {
        skipFilterProperty().set(v);
    }
}