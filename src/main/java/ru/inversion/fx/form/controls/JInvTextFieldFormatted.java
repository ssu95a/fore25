/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.*;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;


/**
 * Компонент для ввода данных по маске
 * @author lukichev
 */
public class JInvTextFieldFormatted extends JInvValueField<String> {

    final private StringProperty formatMaskProperty = new SimpleStringProperty( this, "formatMaskProperty" );
    final private BooleanProperty enableMaskValueProperty = new SimpleBooleanProperty(this, "enableMaskValue");
    final private StringProperty valueProperty = new SimpleStringProperty( this, "value" );


    private static Map<Character, Function<Character,Boolean>> mapOfValidator = new HashMap<>();

    static {
        mapOfValidator.put( '#', ch -> Character.isDigit(ch));
        mapOfValidator.put( '?', ch -> Character.isAlphabetic(ch));
        mapOfValidator.put( 'A', ch -> Character.isAlphabetic(ch) || Character.isDigit(ch));
        mapOfValidator.put( 'U', ch -> Character.isAlphabetic(ch));
        mapOfValidator.put( 'L', ch -> Character.isAlphabetic(ch));
        mapOfValidator.put( '*', ch -> true);
    }

    public JInvTextFieldFormatted() {
        this("");
    }
    public JInvTextFieldFormatted( String t ) {
        super(t);
        formatMaskProperty.addListener((observable, oldValue, newValue) -> {
            formatter = new TextFormatter<>(createPattern(U.nvl(newValue, S.EMPTY_STRING)));
            setTextFormatter(formatter);
        });
        textProperty().addListener((observable, oldValue, newValue) -> {
            if( S.isNullOrEmpty(clearText(newValue)) ) {
                setState(State.NULL);
                valueProperty.set(S.EMPTY_STRING);
            }
            else {
                if( !S.isNullOrEmpty(getFormatMask())  ) {
                    int elementsOfMask = getFormatMask().replaceAll("[^#?AUL*]", "").length();
                    if (!enableMaskValueProperty.get()) {
                        if (elementsOfMask != clearText(newValue).length()) {
                            setState(State.ERROR);
                        } else {
                            setValue(clearText(newValue));
                            setState(State.VALUE);
                        }
                    } else {
                        if (elementsOfMask != clearText(newValue).length()) {
                            setState(State.ERROR);
                        } else {
                            setValue(newValue);
                            setState(State.VALUE);
                        }
                    }

                } else {
                    setValue(newValue);
                    setState(State.VALUE);
                }
            }
        });

        valueProperty.addListener((observable, oldValue, newValue) -> {
            if( S.isNullOrEmpty(newValue))
            {
                setState(State.NULL);
                setText(S.EMPTY_STRING);
            }
            else
            {
                setText(newValue);
            }
        });
    }

    @Override
    public Property<String> valueProperty() {
        return valueProperty;
    }

    @Override
    public Class<String> getClassValue() {
        return String.class;
    }

    private TextFormatter<String> formatter;

    private UnaryOperator<Change> createPattern(String inputMask)
    {
        String maskInTextField = inputMask.replaceAll("[#?AUL*]", "_");
        String clearMask = inputMask.replaceAll("[^#?AUL*]", "");
        int numberOfSymbols = clearMask.length();

        return change -> {
            if(isReadOnly()) return change;
            if(inputMask.isEmpty()) return change;

            String controlNewText = clearText(change.getControlNewText());
            String clearPrefix = clearText(change.getControlNewText().substring(0, change.getAnchor()));
            int prefixLength = clearPrefix.length();

            if(controlNewText.length()==0){
                change.setRange(0, change.getControlText().length());
                change.selectRange(0, 0);
                change.setText(maskInTextField);
            }

            if (controlNewText.length() > numberOfSymbols) {
                if(prefixLength > numberOfSymbols) return null;
                controlNewText = controlNewText.substring(0, numberOfSymbols);
            }

            if (change.getControlText().equals(change.getControlNewText())) return change;

            StringBuilder stringBuilder = new StringBuilder(numberOfSymbols);
            int i = 0;
            int j = 0;
            while (j < controlNewText.length()) {
                char c = maskInTextField.charAt(i);
                if (c == '_') {
                    stringBuilder.append(validChar(clearMask, controlNewText.charAt(j), j));
                    j++;
                } else {
                    stringBuilder.append(c);
                }
                i++;
            }

            int s = stringBuilder.length();
            if(s > 0 && stringBuilder.charAt(s-1)=='_') {
                s--;
            }

            if(change.isDeleted()) s=change.getAnchor();

            stringBuilder.append(maskInTextField.substring(i));
            change.setRange(0, change.getControlText().length());
            change.setText(stringBuilder.toString());
            change.selectRange(s, s);
            return change;
        };
    }

    private String clearText(String input) { return input.replaceAll("[^a-zA-Z0-9а-яёА-ЯЁ$%&@{}]", ""); }

    private char validChar(String mask, char charOfInputText, int positionOfChar ){
        char ch = mask.charAt(positionOfChar);
        if(mapOfValidator.get(ch).apply( charOfInputText ))
            switch (mask.charAt(positionOfChar)){
                case 'U': return Character.toUpperCase(charOfInputText);
                case 'L': return  Character.toLowerCase(charOfInputText);
                default:  return charOfInputText;
            }
        else
            return '_';
    }

    final public StringProperty formatMaskProperty() { return formatMaskProperty; }
    final public String getFormatMask() { return formatMaskProperty.get(); }
    final public void setFormatMask(String v) { this.formatMaskProperty.set(v); }

    public boolean getEnableMaskValue() {
        return enableMaskValueProperty.get();
    }
    public BooleanProperty enableMaskValueProperty() {
        return enableMaskValueProperty;
    }
    public void setEnableMaskValue(boolean enableMaskValueProperty) {
        this.enableMaskValueProperty.set(enableMaskValueProperty);
    }
}