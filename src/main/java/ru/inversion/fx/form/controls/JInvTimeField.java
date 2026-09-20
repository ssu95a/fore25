/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Skin;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import ru.inversion.fx.form.controls.renderer.ContentTypeManager;
import ru.inversion.fx.form.controls.skin.JInvTimeSkin;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.IConverter;
import ru.inversion.utils.converter.TypeConverter;

import java.lang.invoke.MethodHandles;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.function.UnaryOperator;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;

/**
 * @author antonovdi
 */
public class JInvTimeField extends JInvValueField<LocalTime> {
    private final static Logger logger = getLogger( MethodHandles.lookup().lookupClass() );

    private static final char SEPARATION_CHAR = ':';

    private static final String FORMAT_MASK = "##:##:##";

    private static final String FORMAT_MASK_NO_SEC = "##:##";

    private static final String PROPERTY_USING_SECONDS = "ru.inversion.time.using_seconds";

    private final Property<LocalTime> timeProperty = new SimpleObjectProperty<>();
    private final TimeStringConverter timeConverter = new TimeStringConverter();

    private final IConverter<LocalTime, String> withSecondConverter = TypeConverter.<LocalTime>getFormatConverter(LocalTime.class,
            ContentTypeManager.MASK_TIME_MM_SS);
    private final IConverter<LocalTime, String> noSecondConverter = TypeConverter.<LocalTime>getFormatConverter(LocalTime.class,
            ContentTypeManager.MASK_TIME_NO_SECONDS);

    private boolean fromSetText = false;
    private boolean fromSetValue = false;
    private long stepChange = 1;

    /**
     *
     */
    public JInvTimeField() {
        this((String) null);
    }

    /**
     *
     */
    public JInvTimeField(String arg0) {
        super(arg0);
        setAlignment(Pos.BASELINE_RIGHT);
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            if (change.isAdded()) {
                final String text = change.getText();

                // Проверяем введенные символы
                for (int i = 0; i < text.length(); i++) {
                    final char c = text.charAt(i);
                    if (!Character.isDigit(c) && c != SEPARATION_CHAR)
                        return null;
                }

                final String currentText = change.getControlNewText();
                final String mask = getUsingSeconds() ? FORMAT_MASK : FORMAT_MASK_NO_SEC;

                // Проверяем текущую длину
                if (currentText.length() > mask.length()) {
                    return null;
                }

                // Автоматически проставляем разделяющий знак
                final char[] textChars = currentText.toCharArray();
                final char[] maskChars = mask.toCharArray();
                for (int i = 0; i < textChars.length; i++) {
                    if (maskChars[i] == SEPARATION_CHAR && textChars[i] != SEPARATION_CHAR) {
                        change.setText(SEPARATION_CHAR + change.getText());
                        change.setCaretPosition(textChars.length + 1);
                        change.setAnchor(textChars.length + 1);
                    }
                }
            }

            return change;
        };

        final TextFormatter<LocalTime> textFormatter = new TextFormatter<>(filter);

        setTextFormatter(textFormatter);

        textProperty().addListener((Observable observable) -> {
            try {
                if (fromSetValue) {
                    return;
                }
                fromSetText = true;
                String text = getText();
                try {
                    if (S.isNullOrEmpty(text)) {
                        setValue(null);
                    } else {
                        setValue(getConverter().fromString(text));
                    }
                    if (getState() == ERROR) {
                        setState(getValue() == null ? State.NULL : State.VALUE);
                    }
                } finally {
                    fromSetText = false;
                }
            } catch (Throwable th) {
                setState(State.ERROR);
            }
        });

        focusedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (!newValue) {

                if (getState() != ERROR) {

                    fromSetValue = true;

                    try {
                        if (getState() == State.NULL) {
                            setText(null);
                        } else {
                            setText(getConverter().toString(getValue()));
                        }
                    } finally {
                        fromSetValue = false;
                    }
                }
            }
        });

        valueProperty().addListener((Observable observable) -> {
            setState(getValue() == null ? State.NULL : State.VALUE);

            if (!fromSetText) {
                fromSetValue = true;
                try {
                    setText(getConverter().toString(getValue()));
                } finally {
                    fromSetValue = false;
                }
            }
        });

    }

    /**
     *
     */
    public JInvTimeField(LocalTime value) {
        this();
        setValue(value);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JInvTimeSkin(this);
    }

    public void increaseTime() {
        if (!isFocused()){
            requestFocus();
        }
        if (getState().equals(IStateControl.State.VALUE)) {

            int caret = getCaretPosition();
            Integer partOfTime = evaluatePartOfTimeToChange();
            if (partOfTime != null) {
                setValue(increasePartOfTime(partOfTime, getValue()));
                positionCaret(caret);
            }
        } else if (getState().equals(IStateControl.State.NULL)) {
            setValue(LocalTime.of(0, 0, 1));
        }
    }

    public void decreaseTime() {
        if (!isFocused()){
            requestFocus();
        }
        if (getState().equals(IStateControl.State.VALUE)) {

            int caret = getCaretPosition();
            Integer partOfTime = evaluatePartOfTimeToChange();
            if (partOfTime != null) {
                setValue(decreasePartOfTime(partOfTime, getValue()));
                positionCaret(caret);
            }
        }
    }

    private Integer evaluatePartOfTimeToChange() {

        Integer result = null;

        int caretPos = getCaretPosition();

        if (caretPos < 3) {
            result = Calendar.HOUR;
        } else if (caretPos < 6) {
            result = Calendar.MINUTE;
        } else if (caretPos < 9) {
            result = Calendar.SECOND;
        }

        return result;
    }

    private LocalTime increasePartOfTime(int partOfTime, LocalTime value) {

        switch (partOfTime) {
            case Calendar.HOUR:
                return value.plusHours(stepChange);
            case Calendar.MINUTE:
                return value.plusMinutes(stepChange);
            case Calendar.SECOND:
                return value.plusSeconds(stepChange);
            default:
                return null;
        }
    }

    private LocalTime decreasePartOfTime(int partOfTime, LocalTime value) {

        switch (partOfTime) {
            case Calendar.HOUR:
                return value.minusHours(stepChange);
            case Calendar.MINUTE:
                return value.minusMinutes(stepChange);
            case Calendar.SECOND:
                return value.minusSeconds(stepChange);
            default:
                return null;
        }
    }

    private class TimeStringConverter extends StringConverter<LocalTime> {

        @Override
        public String toString(LocalTime object) {

            if (getUsingSeconds()) {
                return withSecondConverter.to(object);
            } else {
                return noSecondConverter.to(object);
            }
        }

        @Override
        public LocalTime fromString(String string) {
            if (getUsingSeconds()) {
                return withSecondConverter.from(string);
            } else {
                return noSecondConverter.from(string);
            }
        }

    }

    @Override
    public Property<LocalTime> valueProperty() {
        return timeProperty;
    }

    @Override
    public Class getClassValue() {
        return LocalTime.class;
    }

    public boolean getUsingSeconds() {
        return (Boolean) getProperties().getOrDefault(PROPERTY_USING_SECONDS, Boolean.TRUE);
    }

    public void setUsingSeconds(boolean val) {
        getProperties().put(PROPERTY_USING_SECONDS, val);
    }

    /**
     *
     */
    public StringConverter<LocalTime> getConverter() {
        return timeConverter;
    }
}
