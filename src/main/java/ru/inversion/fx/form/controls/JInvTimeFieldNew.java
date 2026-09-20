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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.UnaryOperator;


/**
 * Компонент для ввода данных в формате времени
 * @author lukichev
 */
public class JInvTimeFieldNew extends JInvValueField<LocalTime> {

    private final static String FORMAT_MASK_WS = "##:##";
    private final static String FORMAT_MASK = "##:##:##";
    private final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

//    final private StringProperty formatMaskProperty = new SimpleStringProperty( this, "formatMaskProperty" );
    final private Property<LocalTime> valueProperty = new SimpleObjectProperty<>( this, "value" );
    final private BooleanProperty timeWithoutSecondsProperty = new SimpleBooleanProperty(this, "timeWithoutSeconds");

    public JInvTimeFieldNew() {
        this("");
        setPrefColumnCount(6);
        maxWidth(USE_PREF_SIZE);
    }
    public JInvTimeFieldNew( String t ) {
        super(t);
        setPrefColumnCount(6);
        maxWidth(USE_PREF_SIZE);
        timeWithoutSecondsProperty.addListener(observable -> {
            //FORMAT_MASK = "##:##";
            formatter = new TextFormatter<>(createPattern(FORMAT_MASK_WS));
            setTextFormatter(formatter);
        });

        formatter = new TextFormatter<>(createPattern(FORMAT_MASK));
        setTextFormatter(formatter);

        textProperty().addListener((observable, oldValue, newValue) -> {
            if( newValue.charAt(0) == '_' ) {
                setState(State.NULL);
                valueProperty.setValue(null);
            }
            else {
                setValue(LocalTime.parse(newValue));
                setState(State.VALUE);
            }
        });

        valueProperty.addListener((observable, oldValue, newValue) -> {

            if( newValue == null )
            {
                setState( State.NULL );
                setText ( S.EMPTY_STRING );
            }
            else
                setText (
                    newValue.format(timeFormatter).substring( 0, getTimeWithoutSeconds() ?  FORMAT_MASK_WS.length() : FORMAT_MASK.length() )
                );
        });
    }

    @Override
    public Property<LocalTime> valueProperty() {
        return valueProperty;
    }

    @Override
    public Class<LocalTime> getClassValue() {
        return LocalTime.class;
    }

    private TextFormatter<LocalTime> formatter;

    private UnaryOperator<Change> createPattern(String inputMask)
    {
        String showMaskInTextField = inputMask.replaceAll("#", "_");
        int numberOfSymbols = showMaskInTextField.length();

        return change -> {
            int caret = change.getCaretPosition();
            String text = change.getText();
            if (change.isDeleted() && change.getControlNewText().length() == 0) {
                change.setText(showMaskInTextField);
                return change;
            }

            if (!text.isEmpty() && !Character.isDigit(text.charAt(0))) {
                return null;
            }

            String controlNewText = change.getControlNewText();

            if (controlNewText.length() == 0) change.setText(showMaskInTextField);

            if (controlNewText.length() > numberOfSymbols)
                controlNewText = controlNewText.substring(0, numberOfSymbols);

            if (change.isAdded() && caretPosition(caret)) {
                change.setCaretPosition(caret + 1);
                change.setAnchor(caret + 1);
            }

            if (change.isDeleted() && caretPosition(caret - 1)) {
                change.setCaretPosition(caret - 1);
                change.setAnchor(caret - 1);
            }

            String controlText;
            if (change.getControlText().length() > 0) {
                controlText = change.getControlText();
            } else {
                controlText = change.getControlNewText();
            }
            StringBuilder sb = new StringBuilder(controlText);

            if (text.length() == 1 && caret <= numberOfSymbols) {
                if (sb.charAt(caret - 1) == ':') {
                    return null;
                }
                for (int index = 0; index < sb.length(); index++) {
                    if (sb.charAt(index) == '_') {
                        sb.setCharAt(index, '0');
                    }
                }
                sb.setCharAt(caret - 1, text.charAt(0));
            }

            if (change.isDeleted() && !sb.toString().equals(showMaskInTextField) && caret < numberOfSymbols && sb.charAt(caret) != ':') {
                sb.setCharAt(caret, '0');
            }

            if (text.length() > 1) {
                String numberText = text.replaceAll("[^0-9]", "");
                int i = 0;
                int j = 0;
                while (i < sb.length()) {
                    if (i != 2 && i != 5) {
                        if (j < numberText.length()) {
                            sb.setCharAt(i, numberText.charAt(j));
                            j++;
                        } else {
                            sb.setCharAt(i, '0');
                        }
                    } else {
                        sb.setCharAt(i, ':');
                    }
                    i++;
                }
            }

            if (!change.getControlNewText().equals(showMaskInTextField)) {
                if (getNumber(sb.substring(0, 2)) > 23) {
                    return null;
                }

                if (getNumber(sb.substring(3, 5)) > 59) {
                    return null;
                }

                if (numberOfSymbols == 8) {
                    if (getNumber(sb.substring(6, 8)) > 59) {
                        return null;
                    }
                }
            }
//            String noTime = controlNewText.replaceAll("[^0-9]", "");

//            if (!change.isAdded() && !noTime.isEmpty() && getNumber(noTime) == 0 && change.getAnchor() == 0 && change.getCaretPosition() == 0) {
//                if (change.getSelection().getEnd() != 0) {
//                change.setCaretPosition(0);
//                change.setAnchor(0);
//                change.setRange(0, numberOfSymbols);
//                change.setText(showMaskInTextField);
//                System.out.println("bad");
//                return change;
//            }
//        }

            change.setRange(0, change.getControlText().length());
            change.setText(sb.toString());
            return change;
        };
    }

    private boolean caretPosition(int caret){ return caret==2 || caret==5;}

    private int getNumber(String str){ return Integer.parseInt(str);}

    public boolean getTimeWithoutSeconds() {return timeWithoutSecondsProperty.get();}
    public BooleanProperty timeWithoutSecondsProperty() {return timeWithoutSecondsProperty;}
    public void setTimeWithoutSeconds(boolean timeWithoutSecondsProperty) {this.timeWithoutSecondsProperty.set(timeWithoutSecondsProperty);}

//    final public StringProperty formatMaskProperty() { return formatMaskProperty; }
//    final public String getFormatMask() { return formatMaskProperty.get(); }
//    final public void setFormatMask(String v) { this.formatMaskProperty.set(v); }
}