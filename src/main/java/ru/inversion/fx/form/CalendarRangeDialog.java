package ru.inversion.fx.form;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.*;
import ru.inversion.utils.Holder;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_DATE_DD_MM_YYYY;
import static ru.inversion.fx.form.controls.renderer.ContentTypeManager.MASK_DATE_DD_MM_YYYY_HH_MIN_SEC;

/**
 * @author antonovdi, foma
 */
@SuppressWarnings( "unchecked" )
public final class CalendarRangeDialog<T> extends GridPane {

//    public static final String START_COMMENT = "/*#";
//    public static final String END_COMMENT = "#*/";
//    public static final String SEPARATOR_COMMENT = "#";

    private static Logger logger = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

    private static ResourceBundle bundle = ResourceBundle.getBundle("calendar");

    private final boolean useTime;

    private final boolean fillBefore;

    private JInvCalendar calendarDateFrom;
    private JInvCalendar calendarDateTo;

    private HBox rangeBox;
    private T initialDateFrom;
    private T initialDateTo;

    private Holder resultDate;

    private String mask;

    final private boolean useTimeStamp;

    public CalendarRangeDialog(JInvCalendar calendar, Holder resultDate, boolean useTimeStamp ) {

        this.useTimeStamp   = useTimeStamp;

        if( usesTime( calendar ) )
        {
            JInvCalendarTime calendarTime = (JInvCalendarTime) calendar;

            useTime         = true;
            initialDateFrom = (T) calendarTime.getDateTimeValue();
            fillBefore      = calendarTime.isRangeable();
            mask            = calendar.getMask();
        }
        else
        {
            useTime         = false;
            initialDateFrom = (T) calendar.getValue();
            fillBefore      = false;
        }

        setHgap(5);
        setVgap(5);
        setPadding(new Insets(5.0));

        addCalendarPane();
        addButtonsBox();
        tryDecode(calendar);
        bindControls();

        setPrefSize(450, 120);

        this.resultDate = resultDate;
    }

    private void addCalendarPane() {

        StackPane stackPane = new StackPane();

        Label labelSince = new Label(bundle.getString("LABEL_SINCE"));
        calendarDateFrom = createNewCalendar();
        JInvChoiceButton calendarSinceButton = createNewCalendarButton( calendarDateFrom );

        Label labelBefore = new Label(bundle.getString("LABEL_BEFORE"));
        calendarDateTo = createNewCalendar();
        JInvChoiceButton calendarBeforeButton = createNewCalendarButton( calendarDateTo );

        HBox calendarSinceBox = new HBox( calendarDateFrom, calendarSinceButton);
        calendarSinceBox.setFillHeight(true);
        HBox calendarBeforeBox = new HBox( calendarDateTo, calendarBeforeButton);
        calendarBeforeBox.setFillHeight(true);

        rangeBox = new HBox(labelSince, calendarSinceBox, labelBefore, calendarBeforeBox);
        rangeBox.setSpacing(5);
        rangeBox.setAlignment(Pos.CENTER);

        stackPane.getChildren().addAll(rangeBox);
        add(stackPane, 0, 0);

        GridPane.setConstraints( stackPane, 0, 0, 2, 1, HPos.LEFT, VPos.CENTER, Priority.SOMETIMES, Priority.SOMETIMES );
    }

    /** */
    private static boolean usesTime( final JInvCalendar calendar ) {
        return calendar instanceof JInvCalendarTime && !calendar.getMask().equals( MASK_DATE_DD_MM_YYYY );
    }

    /** */
    private void addButtonsBox() {
        JInvButton btOk = new JInvButton(bundle.getString("OK"));
        btOk.setOnAction(this::onOk);
        JInvButton btCancel = new JInvButton(bundle.getString("CANCEL"));
        btCancel.setOnAction(this::onCancel);
        HBox buttonBox = new HBox(btOk, btCancel);
        buttonBox.setSpacing(5);
        buttonBox.setAlignment(Pos.BOTTOM_RIGHT);
        add(buttonBox, 1, 2);
        GridPane.setConstraints(buttonBox, 1, 2, 1, 1, HPos.RIGHT, VPos.BOTTOM, Priority.ALWAYS, Priority.ALWAYS);
    }

    private void bindControls() {
        LocalDateTime endOfDay = null;
        if ( initialDateFrom != null ) {
            Controls.setValue( calendarDateFrom, initialDateFrom );
            if ( fillBefore && initialDateTo == null ) {
                endOfDay = ( (LocalDateTime) initialDateFrom )
                        .withHour( 23 )
                        .withMinute( 59 )
                        .withSecond( 59 );
                Controls.setValue( calendarDateTo, endOfDay );
            }
        }
        if ( initialDateTo != null ){
            Controls.setValue( calendarDateTo, initialDateTo );
        }
//        logger.info( "setting initial date to {}, {}", initialDateFrom.toString(),
//                initialDateTo == null ? endOfDay :initialDateTo.toString() );
    }

    private void onOk(ActionEvent event) {

        if( rangeBox.isVisible() )
        {
            String resultExp = generateSqlString();

            if(!S.isNullOrEmpty(resultExp) )
                 resultDate.set(resultExp);
        }

        fireEvent (
            new WindowEvent(
                this.getScene().getWindow(),
                WindowEvent.WINDOW_CLOSE_REQUEST
            ));
    }

    private String generateSqlString() {

        T dateFrom = Controls.getValue( calendarDateFrom );
        T dateTo   = Controls.getValue( calendarDateTo );

        StringBuilder sb = new StringBuilder();

        final boolean dateUnsafe = isDateUnsafe( dateFrom, dateTo );

        if( bothDatesPresent( dateFrom, dateTo ) && !dateUnsafe )
        {
            if( useTime ) {
                sb.append("between ")
                  .append( getSqlExpressionFromDate(dateFrom) )
                  .append(" and ")
                  .append( getSqlExpressionFromDate(dateTo) );
            }
            else
            {
                if( dateTo instanceof LocalDate )
                {
                    final LocalDate dateTo1 = ((LocalDate)dateTo).plusDays(1);

                    sb.append(">= ")
                      .append( getSqlExpressionFromDate(dateFrom) )
                      .append(" and # < ")
                      .append( getSqlExpressionFromDate((T)dateTo1) );
                }
                else
                {
                    sb.append("between ")
                      .append( getSqlExpressionFromDate(dateFrom) )
                      .append(" and ")
                      .append( getSqlExpressionFromDate(dateTo) );
                }
            }

        }
        else if( dateFrom != null )
        {
            sb.append( ">=" ).append(getSqlExpressionFromDate(dateFrom));
        }
        else if ( dateTo != null )
        {
            sb.append( "<=" ). append(getSqlExpressionFromDate(dateTo));
        }

        return sb.toString();
    }

    private static final Pattern betweenPattern = Pattern.compile( "(?i)(\\s*between\\s+to_date\\s*\\()(.*)(\\)\\s+and\\s+to_date\\s*\\()(.*)(\\))" );
    private static final Pattern betweenPatternTS = Pattern.compile( "(?i)(\\s*between\\s+to_timestamp\\s*\\()(.*)(\\)\\s+and\\s+to_timestamp\\s*\\()(.*)(\\))" );
    private static final Pattern fromPattern    = Pattern.compile( "(?i)(\\s*>=\\s*to_date\\s*\\()(.*)(\\))" );
    private static final Pattern fromPatternTS  = Pattern.compile( "(?i)(\\s*>=\\s*to_timestamp\\s*\\()(.*)(\\))" );
    private static final Pattern toPattern      = Pattern.compile( "(?i)(\\s*<=\\s*to_date\\s*\\()(.*)(\\))" );
    private static final Pattern toPatternTS    = Pattern.compile( "(?i)(\\s*<=\\s*to_timestamp\\s*\\()(.*)(\\))" );

    /**
        Пытаемся вытащить даты из sql-строки и выставить соответствующие даты
     */
    private void tryDecode(JInvCalendar calendar){
        String query = ( (TextInputControl) calendar
                .getProperties()
                .getOrDefault( "EXPRESSION_TEXTCONTROL", new TextField(""))
        ).getText();
        if ( S.isNullOrEmpty(query) ){
//            logger.info( "Query range is empty, date recovery failed" );
            return;
        }

        Matcher betweenMatcher = useTimeStamp ? betweenPatternTS.matcher(query) : betweenPattern.matcher(query);
        Matcher fromMatcher    = useTimeStamp ? fromPatternTS.matcher(query)    : fromPattern.matcher(query);
        Matcher toMatcher      = useTimeStamp ? toPatternTS.matcher(query)      : toPattern.matcher(query);

        //Определяем тип: оба, левое, правое, или ни одного
        if( betweenMatcher.find() && betweenMatcher.group() != null && betweenMatcher.groupCount() == 5 ){
            //1st Capturing Group (\s*between\s+to_date\s*\()
            //2nd Capturing Group (.*)
            //3rd Capturing Group (\)\s+and\s+to_date\s*\()
            //4th Capturing Group (.*)
            //5th Capturing Group (\))
            initialDateFrom = tryConvertDate(betweenMatcher.group( 2 ));
            initialDateTo = tryConvertDate(betweenMatcher.group( 4 ));
        } else if( fromMatcher.find() && fromMatcher.group() != null && fromMatcher.groupCount() == 3 ){
            //1st Capturing Group (\s*>=\s*to_date\s*\()
            //2nd Capturing Group (.*)
            //3rd Capturing Group (\))
            initialDateFrom = tryConvertDate(fromMatcher.group( 2 ));
        } else if( toMatcher.find() && toMatcher.group() != null && toMatcher.groupCount() == 3 ){
            //1st Capturing Group (\s*<=\s*to_date\s*\()
            //2nd Capturing Group (.*)
            //3rd Capturing Group (\))
            initialDateTo = tryConvertDate(toMatcher.group( 2 ));
        }
//        else {
            //...
//            logger.info( "Decode failed" );
//        }
    }

    private T tryConvertDate( final String initialGroup ) {
        T result = null;
        final String group = initialGroup.trim();

        final char apostrophe = '\'';
        if ( S.countOf( group, apostrophe ) != 4 ){
//            logger.info( "apostrophe count not equal to 4" );
            return result;
        }
        int dStart = group.indexOf( apostrophe ) + 1;
        int dEnd = group.indexOf( apostrophe, dStart + 1 );
        if ( dStart > dEnd ){
//            logger.info( "index error (dStart > dEnd)" );
            return result;
        }
//        int fStart = group.indexOf( apostrophe, dEnd + 1 ) + 1;
//        int fEnd = group.lastIndexOf( apostrophe );
//        if ( fStart > fEnd ){
//            logger.info( "index error (fStart > fEnd)" );
//            return result;
//        }

        String date = group.substring( dStart, dEnd );
//        String format = group.substring( fStart, fEnd );
//        logger.info( "date:{} format:{}", date, format );
        if ( useTime ) {
            try {
                final LocalDateTime parse = LocalDateTime.parse( date, DateTimeFormatter.ofPattern( MASK_DATE_DD_MM_YYYY_HH_MIN_SEC ) );
                result = (T) parse;
            } catch ( Exception e ){
                logger.info( "Cannot parse LocalDateTime {}:{}", date, e.getMessage() );
            }
        } else {
            try {
                final LocalDate parse = LocalDate.parse( date, DateTimeFormatter.ofPattern( MASK_DATE_DD_MM_YYYY ));
                result = (T) parse;
            } catch ( Exception e ){
                logger.info( "Cannot parse LocalDate {}:{}", date, e.getMessage() );
            }
        }
        return result;
    }

    /**
     Проверка на некорректный диапазон (Дата С > Дата ПО)
     */
    private boolean isDateUnsafe( final T dateFrom, final T dateTo ) {
        final boolean bothDatesPresent = bothDatesPresent( dateFrom, dateTo );
        return !bothDatesPresent || ( (Comparable<T>) dateFrom ).compareTo( dateTo ) > 0;
    }

    private boolean bothDatesPresent( final T dateFrom, final T dateTo ) {
        return dateFrom != null && dateTo != null;
    }

    private String getSqlExpressionFromDate( T date ) {

        StringBuilder sb = new StringBuilder( useTimeStamp ? "to_timestamp('" : "to_date('" )
                                            .append( TypeConverter.convertToString(date, null) );
        if(useTime)
        {
            sb.append("','dd.mm.yyyy HH24:mi:ss')");
        }
        else
        {
            sb.append("','dd.mm.yyyy')");
        }
        return sb.toString();
    }

    private void onCancel(ActionEvent event) {
        fireEvent(new WindowEvent(
            this.getScene().getWindow(),
            WindowEvent.WINDOW_CLOSE_REQUEST
        ));

    }

    private JInvCalendar createNewCalendar() {
        if (useTime) {
            JInvCalendar cal = (JInvCalendar) Controls.getControlByClass(LocalDateTime.class, null);
            if (mask != null) {
                cal.setMask(mask);
            }
            return cal;
        } else {
            return (JInvCalendar) Controls.getControlByClass(LocalDate.class, null);
        }
    }

    public void initKeyboard() {

        // Выход по Escape
        JInvKeyboardManager.addAction(getScene(), new ActionBuilder().
            setKeyCombination(new KeyCodeCombination(KeyCode.ESCAPE)).setHandler((ActionEvent event) -> {
            onCancel(null);
        }).build());

        // Выход по OK
        JInvKeyboardManager.addAction(getScene(), new ActionBuilder().
            setKeyCombination(new KeyCodeCombination(KeyCode.ENTER, KeyCombination.CONTROL_DOWN)).setHandler((ActionEvent event) -> {
            onOk(null);
        }).build());

    }

    private JInvChoiceButton createNewCalendarButton(JInvCalendar calendar) {

        JInvChoiceButton button = new JInvChoiceButton();
        button.setTooltip(new Tooltip(bundle.getString("CURRENT_DAY")));
        button.setOnAction((ActionEvent event) -> {
            if (calendar instanceof JInvCalendarTime) {
                ((JInvCalendarTime) calendar).setDateTimeValue(LocalDateTime.now());
            } else {
                calendar.setValue(LocalDate.now());
            }
        });
        return button;
    }
}
