/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.StringConverter;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import static ru.inversion.fx.form.controls.IStateControl.State.ERROR;
import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_DATE_DD_MM_YYYY_HH_MIN;
import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_DATE_DEFAULT;
import ru.inversion.utils.scheck.JInvStringWorker;
import ru.inversion.utils.scheck.JInvStringWorkerException;
import ru.inversion.utils.scheck.JInvStringWorkerValidationException;

/**
 *
 * @author antonovdi
 */
public class JInvCalendarTime extends JInvCalendar {

    // Основное свойство LocalDateTime
    private ObjectProperty<LocalDateTime> dateTimeValue = new SimpleObjectProperty<>(LocalDateTime.now());
    private final DateConverter simpleDateConverter = new DateConverter();

    private boolean rangeable = false;
    private String lastTimeMask = MASK_DATE_DD_MM_YYYY_HH_MIN;
    static private final int centurySeparatorYear = 1950; //Year.now().getValue() - 80;
    static private final DateTimeFormatter ddMMyyyy = DateTimeFormatter.ofPattern("ddMMyyyy");
    static private final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyyMMdd");
    static private final DateTimeFormatter ddMMyy
            = new DateTimeFormatterBuilder()
            .appendPattern("ddMM")
            .appendValueReduced(ChronoField.YEAR, 2, 2, centurySeparatorYear)
            .toFormatter();
    static private final DateTimeFormatter yyMMdd
            = new DateTimeFormatterBuilder()
            .appendValueReduced(ChronoField.YEAR, 2, 2, centurySeparatorYear)
            .appendPattern("MMdd")
            .toFormatter();
    static private final DateTimeFormatter ddMMyyyyHHmmss = DateTimeFormatter.ofPattern("ddMMyyyy HHmmss");
    static private final DateTimeFormatter ddMMyyyyHHmm = DateTimeFormatter.ofPattern("ddMMyyyy HHmm");
    static private final DateTimeFormatter ddMMyyHHmmss = DateTimeFormatter.ofPattern("ddMMyy HHmmss");
    static private final DateTimeFormatter ddMMyyHHmm = DateTimeFormatter.ofPattern("ddMMyy HHmm");
    static private final DateTimeFormatter yyyyMMddHHmmss = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");
    static private final DateTimeFormatter yyyyMMddHHmm = DateTimeFormatter.ofPattern("yyyyMMdd HHmm");
    static private final DateTimeFormatter yyMMddHHmmss = DateTimeFormatter.ofPattern("yyMMdd HHmmss");
    static private final DateTimeFormatter yyMMddHHmm = DateTimeFormatter.ofPattern("yyMMdd HHmm");

    private DateTimeFormatter inputFormatter;
    private boolean dateOnlyMode = false;

    /**
     Режим "Только дата", поведение аналогично обычному JInvCalendar
     */
    public boolean isDateOnlyMode() {
        return dateOnlyMode;
    }

    /**
     Режим "Только дата", поведение аналогично обычному JInvCalendar
     */
    public void setDateOnlyMode( final boolean dateOnlyMode ) {
        this.dateOnlyMode = dateOnlyMode;
        setMask( dateOnlyMode ? MASK_DATE_DEFAULT : lastTimeMask );
    }

    /**
     Возможность ввода даты без указания времени (выставляется начало дня)
     При наличии данного флага CalendarRangeDialog поставит датой окончания конец дня
     */
    public boolean isRangeable() {
        return rangeable;
    }

    /**
     * Возможность ввода даты без указания времени. Были получены пожелания по
     * вводу даты без времени, при этом необходимо оторбражать системное время.
     * При наличии данного флага будет отображаться системное время. При
     * указывании неполного времени(без секунд), отображаются системные секунды.
     */
    public void setRangeable(final boolean rangeable) {
        this.rangeable = rangeable;
    }

    public JInvCalendarTime() {
        super();
        getEditor().setPrefColumnCount(10);
        getStyleClass().add("datetime-picker");
        setMask(lastTimeMask);
        setConverter(new InternalConverter());

        valueProperty().addListener((observable, oldValue, newValue) -> {

//            System.out.println("valuePropertyPropertyListener oldvalue " + oldValue + " newValue " + newValue);
            if (newValue == null) {
                dateTimeValue.set(null);
                setState(State.NULL);
            } else if (dateTimeValue.get() == null) {
                setState(State.VALUE);
                dateTimeValue.set(LocalDateTime.of(newValue, LocalTime.now()));
            } else {
                setState(State.VALUE);
                LocalTime time = dateTimeValue.get().toLocalTime();
                dateTimeValue.set(LocalDateTime.of(newValue, time));
            }
        }
        );

        dateTimeValue.addListener(
            (observable, oldValue, newValue) -> {

                if (newValue == null) {
                    setValue(null);
                } else {

                    LocalDate date = newValue.toLocalDate();
                    if (getValue() != null && date.equals(getValue())) {
                        getEditor().setText(getConverter().toString(date));
                    } else {
                        setValue(date);
                    }

                }
            }
        );

    }

    @Override
    public void setMask( final String format ) {
        super.setMask( format );
        if ( isMaskContainsTime(format) ){
            lastTimeMask = format;
        }
    }

    public LocalDateTime getDateTimeValue() {
        return dateTimeValue.get();
    }

    public void setDateTimeValue(LocalDateTime dateTimeValue) {
        this.dateTimeValue.set(dateTimeValue);
    }

    public ObjectProperty<LocalDateTime> dateTimeValueProperty() {
        return dateTimeValue;
    }

    class InternalConverter extends StringConverter<LocalDate> {

        public String toString(LocalDate object) {

//            System.out.println("toString " + object);
            if (!getState().equals(ERROR)) {
                LocalDateTime value = getDateTimeValue();
                return (value != null) ? value.format(formatter) : "";
            } else {
//                System.out.println("text " + getEditor().getText());
                return getEditor().getText();
            }
        }

        @Override
        public LocalDate fromString(String value) {

//            System.out.println("fromString " + value);
            if (value == null || value.isEmpty()) {
                dateTimeValue.set(null);
                setState(State.NULL);
                return null;
            }

            try {
                LocalDateTime dateTime = null;

                if(!isRangeable()) {
                    if ( isMaskContainsTime() ) {
                        parseDateTime(value);
                        if (inputFormatter == ddMMyyyy || inputFormatter == yyyyMMdd
                                || inputFormatter == ddMMyy || inputFormatter == yyMMdd) {
                            dateTime = LocalDate.parse(prepareDateString(value), inputFormatter).atTime(LocalTime.now());
                        } else {
                            if (inputFormatter == ddMMyyyyHHmm || inputFormatter == ddMMyyHHmm
                                    || inputFormatter == yyyyMMddHHmm || inputFormatter == yyMMddHHmm) {
                                dateTime = LocalDateTime.parse(prepareDateString(value), inputFormatter).plusSeconds(LocalTime.now().getSecond());
                            } else {
                                dateTime = LocalDateTime.parse(prepareDateString(value), inputFormatter);
                            }
                        }
                    } else {
                        dateTime = LocalDate.parse( value, formatter ).atStartOfDay();
                    }
                } else {
                    if ( isStringContainsTime(value) ) {
                        parseDateTime(value);
                        if (inputFormatter == ddMMyyyy || inputFormatter == yyyyMMdd
                                || inputFormatter == ddMMyy || inputFormatter == yyMMdd) {
                            //if (!rangeable)
                            dateTime = LocalDate.parse(prepareDateString(value), inputFormatter).atStartOfDay();
                        } else {
                            if (inputFormatter == ddMMyyyyHHmm || inputFormatter == ddMMyyHHmm
                                    || inputFormatter == yyyyMMddHHmm || inputFormatter == yyMMddHHmm) {
                                dateTime = LocalDateTime.parse(prepareDateString(value), inputFormatter).plusSeconds(LocalTime.now().getSecond());
                            } else {
                                dateTime = LocalDateTime.parse(prepareDateString(value), inputFormatter);
                            }
                        }
                    } else {
                        dateTime = simpleDateConverter.fromString(value).atStartOfDay();
                    }
                }

                dateTimeValue.set(dateTime);
                setState(State.VALUE);
                return dateTimeValue.get().toLocalDate();
            } catch (Throwable ex) {
                return getValue();
            }
        }
    }

    protected void parseDateTime(String text) throws JInvStringWorkerException, JInvStringWorkerValidationException, ParseException, AppException {
        String year = "(\\d\\d\\d\\d)";
        String yearShort = "\\d\\d";
        String mounth = "(0\\d|1[012])";
        String day = "(0\\d|1\\d|2\\d|3[01])";
        String hours = "(0\\d|1\\d|2[0123])";
        String minuts = "(0\\d|1\\d|2\\d|3\\d|4\\d|5\\d)";
        String secunds = "(0\\d|1\\d|2\\d|3\\d|4\\d|5\\d)";
        String space = "\\s";
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
        } else if (text.length() == 11 || text.length() == 13 || text.length() == 15) {
            JInvStringWorkerValidationException ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + year + space + hours + minuts + secunds + end, 0, text, false);
            if (ex == null) {
                inputFormatter = ddMMyyyyHHmmss;
            }
            ex = JInvStringWorker.INSTANCE().checkRegExp(begin + year + mounth + day + space + hours + minuts + secunds + end, 0, text, false);
            if (ex == null) {
                inputFormatter = yyyyMMddHHmmss;
            }

            ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + year + space + hours + minuts + end, 0, text, false);
            if (ex == null) {
                inputFormatter = ddMMyyyyHHmm;
            } else {
                ex = JInvStringWorker.INSTANCE().checkRegExp(begin + year + mounth + day + space + hours + minuts + end, 0, text, false);
                if (ex == null) {
                    inputFormatter = yyyyMMddHHmm;
                } else {
                    //при вводе коротекого года с полным временем(с секундами), присходят неоднозначные ситуации.
                    //приходится распознавать исходя из приоритета пользователя на это распознавание
                    if (BaseApp.APP() != null && BaseApp.APP().getViewPrefService().getDatePrioritet()) {

                        ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + yearShort + space + hours + minuts + secunds + end, 0, text, false);
                        if (ex == null) {
                            inputFormatter = yyMMddHHmmss;
                        } else {
                            ex = JInvStringWorker.INSTANCE().checkRegExp(begin + yearShort + mounth + day + space + hours + minuts + secunds + end, 0, text, false);
                            if (ex == null) {
                                inputFormatter = ddMMyyHHmmss;
                            }
                        }
                    } else {
                        ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + yearShort + space + hours + minuts + secunds + end, 0, text, false);
                        if (ex == null) {
                            inputFormatter = ddMMyyHHmmss;
                        } else {
                            ex = JInvStringWorker.INSTANCE().checkRegExp(begin + yearShort + mounth + day + space + hours + minuts + secunds + end, 0, text, false);
                            if (ex == null) {
                                inputFormatter = yyMMddHHmmss;
                            }
                        }
                    }
                }
            }
            if (BaseApp.APP() != null && BaseApp.APP().getViewPrefService().getDatePrioritet()) {

                ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + yearShort + space + hours + minuts + end, 0, text, false);
                if (ex == null) {
                    inputFormatter = yyMMddHHmm;
                } else {
                    ex = JInvStringWorker.INSTANCE().checkRegExp(begin + yearShort + mounth + day + space + hours + minuts + end, 0, text, false);
                    if (ex == null) {
                        inputFormatter = ddMMyyHHmm;
                    }
                }
            } else {
                ex = JInvStringWorker.INSTANCE().checkRegExp(begin + yearShort + mounth + day + space + hours + minuts + end, 0, text, false);
                if (ex == null) {
                    inputFormatter = ddMMyyHHmm;
                } else {
                    ex = JInvStringWorker.INSTANCE().checkRegExp(begin + day + mounth + yearShort + space + hours + minuts + end, 0, text, false);
                    if (ex == null) {
                        inputFormatter = yyMMddHHmm;
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

    private String prepareDateString(String text) {
        text = text.replaceAll("\\p{Punct}", "");
        text = text.trim();
        return text;
    }

    private boolean isStringContainsTime(String value) {
        //introduce proper regex if necessary
        return value.trim().length() >= 12;
    }

    @Override
    protected void parseDate(String value) throws JInvStringWorkerException, JInvStringWorkerValidationException, ParseException, AppException {
        if(!isRangeable()) {
            if ( isMaskContainsTime() ) {
                parseDateTime(value);
                if (inputFormatter == ddMMyyyy || inputFormatter == yyyyMMdd
                        || inputFormatter == ddMMyy || inputFormatter == yyMMdd) {
                    LocalDate.parse(prepareDateString(value), inputFormatter).atTime(LocalTime.now());
                } else {
                    if (inputFormatter == ddMMyyyyHHmm || inputFormatter == ddMMyyHHmm
                            || inputFormatter == yyyyMMddHHmm || inputFormatter == yyMMddHHmm) {
                        LocalDateTime.parse(prepareDateString(value), inputFormatter).plusSeconds(LocalTime.now().getSecond());
                    } else {
                        LocalDateTime.parse(prepareDateString(value), inputFormatter);
                    }
                }
            } else {
                LocalDate.parse(value, formatter);
            }
        } else {
            if ( isStringContainsTime(value) ) {
                parseDateTime(value);
                if (inputFormatter == ddMMyyyy || inputFormatter == yyyyMMdd
                        || inputFormatter == ddMMyy || inputFormatter == yyMMdd) {
                    LocalDate.parse(prepareDateString(value), inputFormatter).atTime(LocalTime.now());
                } else {
                    if (inputFormatter == ddMMyyyyHHmm || inputFormatter == ddMMyyHHmm
                            || inputFormatter == yyyyMMddHHmm || inputFormatter == yyMMddHHmm) {
                        LocalDateTime.parse(prepareDateString(value), inputFormatter).plusSeconds(LocalTime.now().getSecond());
                    } else {
                        LocalDateTime.parse(prepareDateString(value), inputFormatter);
                    }
                }
            } else {
                super.parseDate(value);
            }
        }

    }

    private boolean isMaskContainsTime() {
        return isMaskContainsTime( getMask() );
    }

    private static boolean isMaskContainsTime(String mask){
        boolean result = true;
        if (mask != null) {
            result = mask.contains("HH") || mask.contains("hh") || mask.contains("mm") || mask.contains("ss");
        }
        return result;
    }
}
