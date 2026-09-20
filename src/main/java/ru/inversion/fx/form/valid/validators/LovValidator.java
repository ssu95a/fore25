/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.valid.validators;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import ru.inversion.fx.form.controls.ILovValueControl;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.valid.JInvValidator;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

/**
 *
 * @author antonovdi
 */
public class LovValidator extends JInvValidator {
    public static final char DELIMITER = ',';
    private static ResourceBundle bundle = ResourceBundle.getBundle("valid");
    private ILovValueControl lovControl;
    /**
     режим валидации нескольких значений, написанных через разделитель в одном текстовом поле
     */
    private final BooleanProperty appendFromLOV = new SimpleBooleanProperty( this, "appendFromLOV", false );

    // Флаг нужен чтобы отключать валидацию временно. Например когда выбрали из диалога валидировать не нужно.
     // Ну уровня ValidMan обращать внимания на LovValidator не хочется поэтому сделал так
    private boolean disableNextValidation;

    public boolean isDisableNextValidation() {
        return disableNextValidation;
    }

    public void setDisableNextValidation(boolean disableNextValidation) {
        this.disableNextValidation = disableNextValidation;
    }

    public LovValidator(ILovValueControl textField) {
        this.lovControl = textField;
        if (textField != null) {
            disabled.set(!textField.isValidateFromLOV());
            disabled.bind(textField.validateFromLOVProperty().not());
            appendFromLOV.set( textField.isAppendFromLOV() );
            appendFromLOV.bind( textField.appendFromLOVProperty() );
        }
    }

    @Override
    public Result validate(Object value) {
        return validate(value, true);
    }

    public Result validateOnOk(Object value) {

        return validate(value, false);
    }

    private Result validate( Object value, boolean doSetValue )
    {

        if( disableNextValidation) {
            disableNextValidation = false;
            return null;
        }

        if( lovControl != null)
        {

            //проверка каждого значения из списка
            if ( appendFromLOV.get() && value instanceof String ){
                String[] splitValues = splitValues( (String) value );

                for ( final String splitValue : splitValues ) {

                    Result result = internalValidate( splitValue, doSetValue );
                    if ( result != null ){
                        return result;
                    }
                }
                return null;
            }
            return internalValidate( value, doSetValue );
        }

        return null;
    }

    private Result internalValidate( final Object value, final boolean doSetValue ) {
        final boolean valid;
        AbstractLovBase lov = lovControl.getLOV();
        if (lov != null){
            try {
                valid = lov.checkValue(value, doSetValue);
            } catch (Exception th) {
                return new Result(th);
            }
            if (!valid) {
                return new Result(null, bundle.getString("LOV_VALIDATE"));
            }
        }
        return null;
    }

    /**
     Активно ли дописывание выбранного содержимого из лова в конец текстового поля
     (вместо перезаписи содержимого этого текстового поля)
     */
    public final boolean isAppendFromLOV( ) {
        return appendFromLOV.get();
    }

    private static String[] splitValues( String s ) {

        if( S.isNullOrEmpty(s) ) {
            return new String[]{s};
        }

        boolean wasDelim = true;
        boolean inQuotas = false;

        StringBuilder currentItem = new StringBuilder();
        final List<String> items = new ArrayList<>();

        char currentChar;

        for( int i = 0; i < s.length(); i++ )
        {
            currentChar = s.charAt(i);

            if( currentChar == '"' )
            {
                inQuotas = !inQuotas;
                continue;
            }

            if ( inQuotas ) {
                wasDelim = false;
            } else {
                if( U.inChar( currentChar, DELIMITER ) )
                {
                    if( wasDelim )
                        continue;

                    wasDelim = true;
                }
                else
                    wasDelim = false;
            }

            if( wasDelim )
            {
                if( currentItem.length() > 0 )
                {
                    String item = currentItem.toString().trim();

                    if( !item.isEmpty() )
                    {
                        items.add( item );
                    }

                    currentItem = new StringBuilder();
                }

                continue;
            }

            currentItem.append(currentChar);

        }//end for

        if( currentItem.length() != 0 )
        {
            String item = currentItem.toString().trim();

            if( !item.isEmpty() )
            {
                items.add( item );
            }
        }//end if

        return items.toArray( new String[0] );
    }

    public static void main( String[] args ) {
        String test1 = "test1";
        String test2 = "test2,test2";
        String test3 = "test3,,test3";
        String test4 = ",, ,";
        String test5 = "test5,";
        String test6 = "\"test6\"";
        String test7 = "\"test7\",\"test7\"";
        String test8 = "\"test8\",test8";
        String test9 = "\"test9,\",,test9";
        String test10 = "\"test10,\"test10,test10";
        String test11 = "\"test11\"";
        String test12 = ",test12,test12";
        String test13 = ",,,\"test13,test13\",test13";
        String test14 = "test14,\"test14\"";
        System.out.println(
                Arrays.toString(splitValues(test1))+"\n"+
                Arrays.toString(splitValues(test2))+"\n"+
                Arrays.toString(splitValues(test3))+"\n"+
                Arrays.toString(splitValues(test4))+"\n"+
                Arrays.toString(splitValues(test5))+"\n"+
                Arrays.toString(splitValues(test6))+"\n"+
                Arrays.toString(splitValues(test7))+"\n"+
                Arrays.toString(splitValues(test8))+"\n"+
                Arrays.toString(splitValues(test9))+"\n"+
                Arrays.toString(splitValues(test10))+"\n"+
                Arrays.toString(splitValues(test11))+"\n"+
                Arrays.toString(splitValues(test12))+"\n"+
                Arrays.toString(splitValues(test13))+"\n"+
                Arrays.toString(splitValues(test14))+"\n"+
                ""
        );
    }
}
