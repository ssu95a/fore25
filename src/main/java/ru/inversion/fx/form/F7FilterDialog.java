package ru.inversion.fx.form;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import javafx.util.StringConverter;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.dataset.IFilterItem;
import ru.inversion.dataset.fx.BindControlInfo;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.fx.F7FilterGroup;
import ru.inversion.dataset.fx.F7FilterItem;
import ru.inversion.db.entity.RegisterEnum;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.form.controls.autocomplete.JInvBindings;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.JInvEntityLov;
import ru.inversion.fx.form.valid.ValidInvalidationListener;
import ru.inversion.fx.form.valid.ValidMan;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.Holder;
import ru.inversion.utils.Jt2St;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static ru.inversion.dataset.IFilterItem.ValueTypeEnum.*;
import static ru.inversion.dataset.fx.F7FilterItem.TypeLocation.INFO;
import static ru.inversion.dataset.fx.F7FilterItem.TypeLocation.TABLE;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_IGNORE_95_SYMB_IN_LIKE;
import static ru.inversion.fx.form.controls.JInvTable.PROPERTY_FILTER;
import static ru.inversion.fx.form.controls.JInvTableColumn.*;
import static ru.inversion.fx.form.valid.ValidMan.setControlValueChanged;

/**
 * Диалог фильтра на таблице, вызываемом по F7/F8
 *
 * @author antonovdi
 *         sulimoff
 */
public class F7FilterDialog extends GridPane {

    static private final Logger logger = LoggerFactory.getLogger(F7FilterDialog.class);

    private static final ResourceBundle bundle = ResourceBundle.getBundle("fore");

    public static final String GROUP_TABLE_ID = "ru.inversion.F7FilterDialog.group_table_id";
    public static final String GROUP_INFO_ID  = "ru.inversion.F7FilterDialog.group_info_id";

    private static final String EXPRESSION_CONTROL_PROPERTY = "EXPRESSION_CONTROL";
    private static final String EXPRESSION_CONTROL = "EXPRESSION_TEXTCONTROL";
    private static final String INITIAL_CONTROL = "INITIAL_CONTROL";

    private static final int MIN_COLUMN_WIDTH = 75;

    private static final PseudoClass labelDecorator = PseudoClass.getPseudoClass("include_filter");

    private final AbstractBaseController<DSFXAdapter<?>> ctrl;
    private final DSFXAdapter dsAdapter;
    private final IEntityMetaData metadata;

    private final Map<F7FilterGroup, Set<F7FilterItem>> mapGroupItems = new TreeMap<>();
    private final Map<String, Node> filterControlMap = new LinkedHashMap<>();
    private final Map<String, F7FilterTextField> exprControlMap = new LinkedHashMap<>();

    private ProxyForValueHelper proxyForValueHelper;

    //Текущая максимальная ширина левой части (описание свойства). Обнуляется после использования.
    private double maxWidth = MIN_COLUMN_WIDTH;
    private final FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();

    private final JInvCheckBoxConverter converterForCheckBox = new JInvCheckBoxConverter();
    private final List<F7SearchModel>   searchDictionary     = new ArrayList<>();
    private JInvComboBox edSearch;
    private final TaskContext taskContext;
    private ValidMan validMan;// = new ValidMan();
    private AutoCompletionBinding bindingSearch;

    /** */
    public F7FilterDialog ( AbstractBaseController<DSFXAdapter<?>> ctrl )
    {
        this.ctrl        = ctrl;
        this.dsAdapter   = ctrl.getDataObject ();
        this.taskContext = ctrl.getTaskContext ();

        if( dsAdapter == null || dsAdapter.getTable () == null )
            throw new IllegalArgumentException( bundle.getString("ERROR_FILTERDIALOG_EMPTY_ADAPTER") );

        validMan = ctrl.getValidMan ();
        validMan.setKeepChangedState(true);

        Class cl = dsAdapter.getDataSet().getRowClass();
        metadata = EntityMetadataFactory.getEntityMetaData(cl);
        proxyForValueHelper = new ProxyForValueHelper( this.metadata );

        initItems();
        initFilterPane();

        setVgap(5);
        setHgap(5);
    }

    /** */
    private boolean isUse_to_timestamp()
    {
        return taskContext.isPostgreSql();
    }


    /** */
    private void initItems() {

        mapGroupItems.put( new F7FilterGroup( GROUP_TABLE_ID, bundle.getString("PARAMETRY_IZ_TABLICY"), 0), new TreeSet<>() );
        mapGroupItems.put( new F7FilterGroup( GROUP_INFO_ID,  bundle.getString("INFORMACIONNYE_PARAMETRY"), 1), new TreeSet<>() );

        fillNamedTableProperties( (JInvTable) dsAdapter.getTable());
        fillNamedInfoProperties ( dsAdapter.getBindedControls()   );

        if( dsAdapter.getFilterModifier() != null )
            dsAdapter.getFilterModifier().modifyBefore( mapGroupItems );
    }

    private void initFilterPane( ) {

        GridPane scrollPaneContent = this;

        if( mapGroupItems != null && !mapGroupItems.isEmpty() )
        {
            U.forEachWithIndex( mapGroupItems.entrySet(), (Map.Entry<F7FilterGroup, Set<F7FilterItem>> entry, Integer index ) -> {

                try {

                    if (!entry.getValue().isEmpty())
                    {
                        F7FilterGroup group = entry.getKey();
                        TitledPane tablePane = new TitledPane();
                        tablePane.setAnimated(false);
                        tablePane.setExpanded(!group.isCollapsed());
                        tablePane.setFocusTraversable(false);
                        tablePane.setText(group.getTitle());
                        scrollPaneContent.add(tablePane, 0, index);
                        GridPane.setConstraints(tablePane, 0, index, 1, 1, HPos.LEFT, VPos.TOP, Priority.ALWAYS, Priority.NEVER);

                        GridPane contentTablePane = generatePaneByMapFilterItems( entry.getValue(), group.getTitle() );
                        contentTablePane.setId( "contentTablePane"+index );
                        tablePane.setContent(contentTablePane);

                        setCommonLabelWidth(contentTablePane);
                    }
                } catch (Throwable ex) {
                    JInvErrorService.handleException(null, ex);
                }
            });
        }
    }

    /** Устанавливает общую, наибольшую ширину лэйблов */
    private void setCommonLabelWidth(GridPane contentTablePane) {
        contentTablePane.getChildrenUnmodifiable().stream()
                //только лэйблы, которые идут в первом столбце gridPane
                .filter( node -> GridPane.getColumnIndex( node ) == 0 && node instanceof Label )
                .map( node -> (Label)node )
                .forEach( label -> {
                    double labelWidth = fontLoader.computeStringWidth(label.getText(), label.getFont());
                    if ( labelWidth > maxWidth ){
                        maxWidth = labelWidth;
                    }
                } );

        //Deleting old constraints!
        contentTablePane.getColumnConstraints().clear();

        for (int colIndex = 0; colIndex < 2; colIndex++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow( Priority.ALWAYS ); // allow column to grow
            cc.setFillWidth( true ); // ask nodes to fill space for column

            double commonWidth = this.maxWidth + 10; // some more space
            if ( colIndex == 0 ){
                cc.setMaxWidth( commonWidth );
                cc.setPrefWidth( commonWidth );
            }
            contentTablePane.getColumnConstraints().add(cc);
        }
        maxWidth = MIN_COLUMN_WIDTH;
    }

    protected Map<F7FilterGroup, Set<F7FilterItem>> getMapGroupItems () {
        return mapGroupItems;
    }

    protected Map<String, Node> getControls () {
        return filterControlMap;
    }
/*
    protected Map<String, Node> getExpressionControls () {
        return mapExprControls;
    }
*/
    public void initKeyboard(Scene scene) {
        initKeyboard( scene.getRoot() );
    }
    public void initKeyboard(Node scopeNode) {

        // Выход по OK
        JInvKeyboardManager.addAction(scopeNode, new ActionBuilder()
                .setKeyCombination(new KeyCodeCombination(KeyCode.F8))
                .setHandler ((ActionEvent event) -> {
                //Даём примениться написанному значению (например, в календаре):
                edSearch.requestFocus();
                ctrl.close (AbstractBaseController.FormReturnEnum.RET_OK);
        }).build());

        // Открываем диалог диапазона дат календаря по F9, не мешая также открывать ловы
        addEventFilter( KeyEvent.KEY_RELEASED, event -> {
            if ( event.getCode() != KeyCode.F9 ) {return;}
            Node node = (Node) event.getTarget();
            if (node != null && (node instanceof JInvCalendar || isNodeExpControlForCalendar(node))) {
                    showCalendarDialog((Control) node);
                }
        } );

        // Выход по Escape
        JInvKeyboardManager.addAction(scopeNode, new ActionBuilder().
            setKeyCombination(new KeyCodeCombination(KeyCode.ESCAPE)).setHandler((ActionEvent event) -> {
            onCancel();
        }).build());

        // Восстанавливаем предыдущий заполненный фильтр

        getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F7) {
                new F7KeyHandler().handle(null);
                event.consume();
            }
        });

        // Показываем диалог ввода списка
        getScene().addEventFilter( KeyEvent.KEY_PRESSED, event -> {
            if( event.getCode() == KeyCode.L && event.isControlDown() )
            {
                Node node = (Node) event.getTarget();

                if( node != null )
                {
                    F7FilterTextField f7_txt = null;

                    if( node instanceof F7FilterTextField )
                        f7_txt = (F7FilterTextField)node;

                    if( f7_txt == null )
                        f7_txt =  (F7FilterTextField)node.getProperties().get(EXPRESSION_CONTROL_PROPERTY);

                    if( f7_txt != null )
                        f7_txt.showListDialog();
                }

                event.consume();
            }
        });

        // Показываем диалог для заполнения значения
        getScene().addEventFilter( KeyEvent.KEY_PRESSED, event -> {

            if( event.getCode() == KeyCode.E && event.isControlDown() )
            {
                Node node = (Node) event.getTarget();

                if( node != null )
                {
                    F7FilterTextField f7_txt = null;

                    if( node instanceof F7FilterTextField )
                        f7_txt = (F7FilterTextField)node;

                    if( f7_txt == null )
                        f7_txt =  (F7FilterTextField)node.getProperties().get(EXPRESSION_CONTROL_PROPERTY);

                    if( f7_txt != null )
                        f7_txt.showExpressionDialog();
                }
                event.consume();
            }
        });

        // Показываем диалог для заполнения значения
        // Не совсем диалог, просто переключаем на режим ввода значения
        getScene().addEventFilter( KeyEvent.KEY_PRESSED, event -> {
            if( event.getCode() == KeyCode.T && event.isControlDown() ) {
                Node node = (Node) event.getTarget();
                if (node != null && (node instanceof F7FilterTextField ))
                {
                    ((F7FilterTextField)node ).showValueDialog();
                }
                event.consume();
            }
        });

    }

    /** */
    private TitledPane getGroupPaneOfControl(Node control) {
        return (TitledPane) Controls.getParentStreamOfControl(control).filter((Parent t) -> t instanceof TitledPane).findFirst().orElse(null);
    }

    /*
    protected boolean onOk1( )
    {
        try {

            if( !checkValueFields() ) {
                return false;
            }

            for( F7FilterItem t : mapGroupItems.values().stream().flatMap( Collection::stream ).collect( Collectors.toList() ) )
            {
                Node control        = filterControlMap.get  ( t.getColumn() );
                Object controlValue = escapeApostrophe ( Controls.getValue( control ));
                Object expValue     = Controls.getValue( exprControlMap.get(t.getColumn()) );

                if( t.isExpression() && !t.isGeneratedExpression( ) )
                {
                    t.setControlValue( expValue );
                    //t.setValue( generateExpressionValueByExpression(t.getColumn(), expValue) );
                    t.setValue( String.join( " ", t.getColumn(), expValue.toString() ) );

                    if ( t.getProxyFor() != null )
                    {
                        F7FilterItem realItem = t.getProxyFor( );
                        //realItem.expression( true ).value( generateExpressionValueByExpression( realItem.getColumn(), expValue ) );
                        realItem.expression( true ).value( String.join( " ", realItem.getColumn(), expValue.toString() ) );
                    }

                    changedFilterItems.add(t);
                }
                else
                {
                    if( ValidMan.isControlValueChanged(control) && controlValue != null )
                    {
                        t.setControlValue(controlValue);

                        // Форсируем режим выражения и генерируем предикат сами в случае lower/upper регистра
                        // т.к. требуется оборачивать ими column
                        if( control instanceof JInvTextField &&
                                (
                                        t.getCaseSensetiveMode() != RegisterEnum.UNKNOWN_CASE ||
                                                controlValue.toString().contains("_") ||
                                                controlValue.toString().contains("%"))
                        )
                        {
                            t.setExpression(true);
                            t.setGeneratedExpression(true);
                            t.setValue(getPredicateExpression(t.isIndexSearchAllowed(), t.getCaseSensetiveMode(), t.getColumn(), controlValue, t.getType()));
                        }
                        else
                        {
                            if(control instanceof JInvCalendarTime)
                            {
                                // В случае отличия от стандартной маски компонента календаря со временем
                                t.setExpression(true);
                                t.setGeneratedExpression(true);
                                t.setValue(getPredicatForCalendarTime(((JInvCalendar)control).getMask(), t.getColumn(), (LocalDateTime)controlValue));
                            }
                            else
                            {
                                t.setValue(controlValue);
                            }
                        }

                        if( t.getProxyFor() != null)
                        {
                            F7FilterItem realItem = t.getProxyFor();
                            Object helperValue = proxyForValueHelper.getValue(t.getColumn(), realItem.getColumn(), controlValue);

                            if( helperValue != null )
                                realItem.value(helperValue);
                        }

                        changedFilterItems.add(t);
                    }//end if
                }
            }

            if (dsAdapter.getTable() != null)
                dsAdapter.getTable().getProperties().put( PROPERTY_FILTER, changedFilterItems );

            List<? extends IFilterItem> changedFilterItemsList;

            if( dsAdapter.getFilterModifier() != null )
            {
                changedFilterItemsList = dsAdapter.getFilterModifier().modifyAfter (changedFilterItems);
            }
            else
            {
                changedFilterItemsList = changedFilterItems;
            }

            dsAdapter.setFilterValues (changedFilterItemsList);

            return true;

        } catch (Throwable ex) {
            JInvErrorService.handleException (null, ex);
            return false;
        }
    }
    */

    /** */
    protected boolean onOk( )
    {
        try {

            if( !checkValueFields( ) ) {
                return false;
            }

            List<F7FilterItem> changedFilterItems = new ArrayList<>();

            for( F7FilterItem t : U.iterable( mapGroupItems.values().stream().flatMap( Collection::stream ).iterator() ) )
            {
                final Node control = filterControlMap.get( t.getColumn() );

                if( t.getValueType() == EXPRESSION && !t.isGeneratedExpression() )
                {
                    F7FilterTextField f7txt = ( control instanceof F7FilterTextField ) ? (F7FilterTextField)control : exprControlMap.get( t.getColumn() );
                    if( f7txt == null )
                        continue;

                    final String controlValue = f7txt.getText();

                    if( !S.isNullOrEmpty(controlValue) )
                    {
                        t.setControlValue( controlValue );
                        //t.setGeneratedExpression( false );

                        /*
                            Проверка на наличие спец. символов!
                            Если они есть, то применяем логику если
                            нет, то оставляем как есть(подзапрос)
                        */
                        if( containsExprSymb(controlValue) )
                        {
                            t.setValue( getPredicateExpression (
                                        t.isIndexSearchAllowed(),
                                        t.getCaseSensetiveMode(),
                                        "#", //t.getColumn(),
                                        controlValue,
                                        t.getType(),
                                        t.isIgnore95LikeSym()
                                        )
                                );
                        }
                        else{
                            t.setValue(controlValue);
                        }

                        changedFilterItems.add(t);
                    }
                }
                else if( t.getValueType() == LIST )
                {
                    F7FilterTextField f7txt = ( control instanceof F7FilterTextField ) ? (F7FilterTextField)control : exprControlMap.get( t.getColumn() );
                    if( f7txt == null )
                        continue;

                    String controlValue = f7txt.getText();

                    t.setControlValue( controlValue );

                    final Object listInValues = F7FilterTextField.toF7List( controlValue, Jt2St.get(t.getType()) );

                    if( t.getCaseSensetiveMode() != RegisterEnum.UNKNOWN_CASE && t.getType() == String.class && listInValues instanceof String )
                    {
                        t.setGeneratedExpression( true );

                        StringBuilder sb = new StringBuilder();

                        // Для НЕ индексных полей всегда upper.
                        // Для индексных смотрим из метаданных
                        switch( t.getCaseSensetiveMode() )
                        {
                            case UPPER_CASE:
                                sb.append("UPPER(#) IN (").append(listInValues).append(')');
                                //sb.append("UPPER(").append( t.getColumn() ).append(") IN ").append(listInValues);
                            break;
                            case LOWER_CASE:
                                sb.append("LOWER(#) IN (").append(listInValues).append(')');
                            break;
                        }

                        t.setValue( sb.toString() );
                    }
                    else
                    {
                        t.setGeneratedExpression( false );
                        t.setValue( listInValues );
                    }

                    if( t.getProxyFor() != null )
                    {
                        F7FilterItem realItem = t.getProxyFor( );
                        realItem.setValueType( LIST );
                        realItem.value( listInValues ).setGeneratedExpression( true );
                    }

                    changedFilterItems.add(t);
                }
                else // VALUE, конкретное значение
                {
                    Object controlValue = Controls.getValue( control );

                    if( ValidMan.isControlValueChanged(control) && controlValue != null )
                    {
                        t.setControlValue( controlValue );

                        if( control instanceof JInvTextField )
                        {
                            String textValue = (String)controlValue;
                            // Форсируем режим выражения и генерируем предикат сами в случае lower/upper регистра
                            // т.к. требуется оборачивать ими column
                            if( t.getCaseSensetiveMode() != RegisterEnum.UNKNOWN_CASE
                                || S.contains( textValue,  t.isIgnore95LikeSym() ? '%' : '_', '%' )
                            )
                            {
                                //t.setExpression         (true);
                                t.setGeneratedExpression(true);
                                t.setValue( getPredicateExpression (
                                        t.isIndexSearchAllowed(),
                                        t.getCaseSensetiveMode(),
                                        "#", //t.getColumn(),
                                        controlValue,
                                        t.getType(),
                                        t.isIgnore95LikeSym()
                                        )
                                );
                            }
                            else
                            {
                                t.setValue(controlValue);
                                t.setGeneratedExpression( false );
                            }
                        }
                        else
                        {
                            if( control instanceof JInvCalendarTime )
                            {
                                // В случае отличия от стандартной маски компонента календаря со временем
                                t.setValueType( EXPRESSION );
                                t.setGeneratedExpression(true);
                                t.setValue    ( getPredicateForCalendarTime( ((JInvCalendar)control).getMask(), "#"/*t.getColumn()*/, (LocalDateTime)controlValue) );
                            }
                            else
                            {
                                t.setValue    ( controlValue );
                                t.setGeneratedExpression(false);
                            }
                        }

                        if( t.getProxyFor() != null )
                        {
                            F7FilterItem realItem = t.getProxyFor();
                            Object helperValue = proxyForValueHelper.getValue( t.getColumn(), realItem.getColumn(), controlValue );
                            if( helperValue != null )
                                realItem.value(helperValue);
                        }


                        changedFilterItems.add(t);

                    }//end if

                }// end VALUE

            }//end for

            if( dsAdapter.getTable() != null )
                dsAdapter.getTable().getProperties().put( PROPERTY_FILTER, changedFilterItems );

            final List<? extends IFilterItem> changedFilterItemsList;

            if( dsAdapter.getFilterModifier() != null )
                changedFilterItemsList = dsAdapter.getFilterModifier().modifyAfter( changedFilterItems );
            else
                changedFilterItemsList = changedFilterItems;

            dsAdapter.setFilterValues( changedFilterItemsList );

            return true;

        } catch( Throwable ex ) {
            JInvErrorService.handleException( null, ex );
            return false;
        }
    }

    private static Object escapeApostrophe( Object controlValue ) {
        if( controlValue instanceof String ) {
            controlValue = controlValue.toString().replace( "'", "''" );
        }
        return controlValue;
    }
    private static Object unescapeApostrophe( Object controlValue ) {
        if( controlValue instanceof String ) {
            controlValue = controlValue.toString().replace( "''", "'" );
        }
        return controlValue;
    }

    private void onCancel() {
        fireEvent(new WindowEvent(
            this.getScene().getWindow(),
            WindowEvent.WINDOW_CLOSE_REQUEST
        ));
    }

    /** */
    private AbstractLovBase getLovFromTableColumn( JInvTableColumn invColumn, String title )
    {
        AbstractLovBase lov = null;

        if( S.isNotNullOrEmpty(invColumn.getLovClassName()) )
        {
            try {
                lov = JInvEntityLov.generateLov( invColumn.getLovClassName(), taskContext, title, invColumn );
            } catch( ClassNotFoundException ex ) {
                logger.error(Tags.PRODUCT_LABEL + "Error on generate LOV for column", ex);
            }
        }
        return lov;
    }

    private AbstractLovBase getLovFromControl(IJInvControl control, String title) {

        AbstractLovBase lov = null;

        if (control instanceof JInvTextField) {

            JInvTextField field = (JInvTextField) control;
            if (S.isNotNullOrEmpty(field.getLovClassName())) {
                try {
                    lov = JInvEntityLov.generateLov(field.getLovClassName(), taskContext, title);
                } catch (ClassNotFoundException ex) {
                    logger.error("", ex);
                }
            }
        }
        return lov;

    }

    /** */
    private F7FilterItem createFilterItemFromControl( Integer index, String propertyName, IFilterControl control )
    {
        // Порядок в группе
        Integer orderInGroup = U.nvl( control.getOrderInF7FilterGroup(), index );

        // Индексный поиск
        boolean indexSearchAllowed = control.isIndexSearchAllowed();

        RegisterEnum register = RegisterEnum.UPPER_CASE;

        final IEntityProperty entityProperty = Objects.requireNonNull( metadata.getProperty(propertyName), "No property for '" + propertyName + "'");

        final Class type = entityProperty.getType();

        if( indexSearchAllowed )
            register = entityProperty.getCaseSensitiveSearchMode();

        F7FilterItem item = new F7FilterItem();
        item.orderInGroup(orderInGroup)
                .indexSearchAllowed(indexSearchAllowed)
                    .type(type)
                        .caseSensetiveMode(register);

        item.setFactoryList( control.getFactoryList() );

        return item;
    }


    /** */
    static final private Predicate<TableColumn> columnFilter = new Predicate< TableColumn>() {
        @Override
        public boolean test( TableColumn c ) {

                if( c instanceof JInvTableColumn )
                {
                    final ObservableMap p = c.getProperties();

                    if (
                        !(Boolean)p.getOrDefault( COLUMN_MARK, Boolean.FALSE )
                        &&
                        (Boolean) p.getOrDefault( COLUMN_SHOW_IN_FILTER, Boolean.TRUE )
                        && (
                            !(Boolean) p.getOrDefault( COLUMN_TRANSIENT, Boolean.FALSE )
                             ||
                             p.getOrDefault( COLUMN_PROXY_FOR, null ) != null
                        )
                        &&
                        S.isNullOrEmpty( (String)p.getOrDefault( COLUMN_QUERY_EXPR, S.EMPTY_STRING ) )
                    )
                        return true;

                }

            return false;
        }
    };

    /** */
    static final private Predicate<Object> controlFilter = new Predicate< Object>() {
        @Override
        public boolean test( Object c ) {

            if( c != null && c instanceof IJInvControl && c instanceof IFilterControl )
            {
                final IJInvControl  ctrl = (IJInvControl)c;
                final ObservableMap p    = ctrl.getProperties();

                if(
                    (Boolean)(p.getOrDefault(COLUMN_SHOW_IN_FILTER, Boolean.TRUE) )
                    &&
                    (
                        !(Boolean) p.getOrDefault(COLUMN_TRANSIENT, Boolean.FALSE)
                        ||
                        p.getOrDefault(COLUMN_PROXY_FOR, null) != null
                    )
                    &&
                    S.isNullOrEmpty( (String)p.getOrDefault( COLUMN_QUERY_EXPR, S.EMPTY_STRING ) )
                )
                    return true;

            }

            return false;
        }
    };

    /** */
    private void fillNamedTableProperties(JInvTable table) {

        List<TableColumn> list = table.getAllBottomColumnsWithFieldName();

        Holder<Integer> index = new Holder<>(0);

        list.stream()
            .filter( columnFilter )
//            filter( column -> {
//                ObservableMap p = column.getProperties();
//                return column instanceof JInvTableColumn
//                    && !(Boolean) p.getOrDefault( COLUMN_MARK, Boolean.FALSE )
//                    &&  (Boolean) p.getOrDefault( COLUMN_SHOW_IN_FILTER, Boolean.TRUE )
//                    && ( !(Boolean) p.getOrDefault( COLUMN_TRANSIENT, Boolean.FALSE ) || p.getOrDefault( COLUMN_PROXY_FOR, null ) != null )
//                    && S.isNullOrEmpty((String) p.getOrDefault( COLUMN_QUERY_EXPR, S.EMPTY_STRING ));
//                }
//            )
             .forEach( ( TableColumn column ) ->
             {
                JInvTableColumn invColumn = (JInvTableColumn) column;
                String propertyName = Controls.getFieldNameFromTableColumn(column);

                // Группа
                String idGroup = invColumn.getIdF7FilterGroup();

                // Маска для календаря
                String mask = null;
                if( column instanceof JInvTableColumnDate )
                    mask = ((JInvTableColumnDate) column).getDateFormat().getMask();

                // Заголовок
                String title = U.nvl( invColumn.getF7Title(), Controls.getTitleFromTableColumn(invColumn) );

                // Подсказка
                final String tooltip = S.nz( invColumn.getToolTipText() );

                // LOV
                AbstractLovBase lov = getLovFromTableColumn( invColumn, title );

                F7FilterItem item = createFilterItemFromControl( index.get(), propertyName, invColumn );
                item.label(title).toolTip(tooltip).column(propertyName).
                    typeLocation(TABLE).mask(mask).lov(lov);//.validateFromLov(validateFromLov);

                if( (Boolean)column.getProperties().getOrDefault( F7FILTER_IGNORE_95_SYMB_IN_LIKE,Boolean.FALSE ) )
                    item.setIgnore95LikeSym(true);
    //
                String proxyForName = (String) column.getProperties().getOrDefault(COLUMN_PROXY_FOR, null);
                if (proxyForName != null) {
                    F7FilterItem proxyForItem = new F7FilterItem();
                    proxyForItem.label(title).column(proxyForName).type(metadata.getProperty(proxyForName).getType()).typeLocation(TABLE);
                    item.proxyFor(proxyForItem);
                }
    //
                addItemToMap(item, idGroup);

                index.set( index.get() + 1 );
            });
    }

    private void fillNamedInfoProperties(List<BindControlInfo> bindedControls) {

        Holder<Integer> index = new Holder<>(0);

        bindedControls.stream().forEach((info) -> {

            String propertyName = info.getDataSetColumn();

            if( propertyName != null && !propertyName.isEmpty() )
            {
                String title = propertyName;

//                if( info.getComponent() instanceof IJInvControl)
//                {
//                    IJInvControl control = (IJInvControl) info.getComponent();
//                    if (control != null && control instanceof IFilterControl
//                        && (Boolean) (control.getProperties().getOrDefault(COLUMN_SHOW_IN_FILTER, Boolean.TRUE))
//                        && (!(Boolean) control.getProperties().getOrDefault(COLUMN_TRANSIENT, Boolean.FALSE)
//                        || control.getProperties().getOrDefault(COLUMN_PROXY_FOR, null) != null))
//                    {
                if( controlFilter.test( info.getComponent() ) )
                {
                    String mask = null;

                    final IJInvControl   control       = (IJInvControl  )info.getComponent();
                    final IFilterControl filterControl = (IFilterControl)control;
                    // Заголовок
                    String titleFromControl = U.nvl( filterControl.getF7Title(), getTitleFromControl(control) );

                    if( !S.isNullOrEmpty( titleFromControl ))
                        title = titleFromControl;

                    // Группа
                    String idGroup = filterControl.getIdF7FilterGroup();

                    // Маска
                    if (control instanceof JInvCalendarTime) {
                        mask = ((JInvCalendarTime) control).getMask();
                    }

                    // LOV
                    AbstractLovBase lov = getLovFromControl(control, title);

                    F7FilterItem item = createFilterItemFromControl(index.get(), propertyName, filterControl);
                    item.label(title).column(propertyName)
                        .typeLocation( GROUP_TABLE_ID.equals(idGroup) ? TABLE : INFO )
                            //.typeLocation( INFO )
                            .mask(mask).lov(lov);

                    if( (Boolean)control.getProperties().getOrDefault( F7FILTER_IGNORE_95_SYMB_IN_LIKE,Boolean.FALSE ) )
                        item.setIgnore95LikeSym(true);

                    if(    control instanceof JInvComboBox
                        || control instanceof JInvRadioGroup
                        || control instanceof JInvCheckBox )
                    {

                        JInvComboBox comboBoxNew = getComboBoxFromControl(control);
                        item.setControl(comboBoxNew);
                    }

                    String proxyForName = (String) control.getProperties().getOrDefault(COLUMN_PROXY_FOR, null);
                    if (proxyForName != null) {
                        F7FilterItem proxyForItem = new F7FilterItem();
                        proxyForItem.label(title).column(proxyForName).type(metadata.getProperty(proxyForName).getType()).typeLocation(INFO);
                        item.proxyFor(proxyForItem);
                    }

                    addItemToMap(item, GROUP_TABLE_ID == idGroup ? null : idGroup );
                    index.set(index.get() + 1);
                }
            }
        }
        );
    }
/*
    public void afterShow () {
        if (!mapControls.isEmpty()) {
            mapControls.forEach((String t, Node u) -> {
                if (u instanceof JInvTextField) {
                    ((Control) u).requestLayout();
                }
            });
            mapControls.values().iterator().next().requestFocus();
        }
    }
*/

    private Label initLabelAndSearchTitle( F7FilterItem item, GridPane gridPane, int rowIndex, String groupTitle ) {

        String textForLabel = item.getLabel().replaceAll( "\n", "" );
        Label label = new Label(textForLabel);
        final String toolTip = item.getToolTip();
        if(S.isNotNullOrEmpty( toolTip )) {
            label.setTooltip( new Tooltip( toolTip ) );
        }
        gridPane.add(label, 0, rowIndex);
        GridPane.setConstraints(label, 0, rowIndex, 1, 1, HPos.RIGHT, VPos.CENTER, Priority.ALWAYS, Priority.SOMETIMES);

        if( groupTitle != null )
            searchDictionary.add( new F7SearchModel( groupTitle + "." + textForLabel, label) );
        return label;
    }

    /** */
    private JInvComboBox createComboBoxForList( Supplier<List<Pair<?,String>>> factoryList )
    {
        final Map<Comparable,String> itemMap = new HashMap<>();
        final List< Pair<?, String > > pairs = factoryList.get();

        pairs.forEach( p -> itemMap.put( (Comparable)p.getKey(), p.getValue() ) );

        JInvComboBox<Object,Object> comboBox = new JInvComboBox<>();
        comboBox.getItems().add(0,U.EMPTY );
        pairs.stream().map( Pair::getKey ).map((k)->(Comparable)k).collect( Collectors.toCollection( comboBox::getItems ));

        comboBox.setConverter(new StringConverter() {
            @Override
            public String toString( Object comparable ) {
                if( comparable == U.EMPTY ) {
                    return bundle.getString("FILTER_EMPTY");
                }
                return itemMap.get(comparable);
            }
            @Override
            public Object fromString( String string ) {
                return null;
            }
        });

        comboBox.setSelectedValue(U.EMPTY);

        return comboBox;
    }

    /** */
    private Node initMainControl( F7FilterItem item ) {

        Node control;

        if( item.getControl() != null )
        {
            control = item.getControl();
        }
        else
            if( item.getFactoryList() != null )
            {
                control = createComboBoxForList( item.getFactoryList() );
            }
        else
            if( U.in( item.getType(), LocalDateTime.class, LocalTime.class,  LocalDate.class ) )
            {
                control = Controls.getControlByClass( item.getType(), item.getColumn() );
            }
        else
            if( item.getType() == Boolean.class )
            {
                control = getFilterComboBoxForClass( Boolean.class );
            }
        else
            if( Enum.class.isAssignableFrom( item.getType() ) )
            {
                control = getFilterComboBoxForClass( item.getType() );
            }
        else
            {
                control = new F7FilterTextField( item, false );
            }

        //
        if( control instanceof JInvTextField )
        {
            if( item.getLov() != null )
            {
                JInvTextField field = (JInvTextField) control;
                field.setLOV( item.getLov(), item.isValidateFromLov() );
            }

            if( item.getCaseSensetiveMode() != null )
            {
                ( (JInvTextField)control ).setCaseSensitiveMode( item.getCaseSensetiveMode() );
            }
        }

        if( control != null && !(control instanceof F7FilterTextField) )
        {
            if( item.isIndexSearchAllowed() )
                control.setStyle( F7FilterTextField.indexSearchStyle );
        }

        return control;
    }

    /** */
    private F7FilterTextField initExpressionControl(F7FilterItem item) {

        if( item.getType() == Boolean.class )
            return null;

        return new F7FilterTextField( item, true );
        /*
        JInvTextField exprControl = new JInvTextField();
        exprControl.getStyleClass().add("expression");
        exprControl.setFieldName(item.getColumn());
        return exprControl;
        */
    }

    /** */
    private GridPane generatePaneByMapFilterItems( Set<F7FilterItem> filterItemsSet, final String title ) throws AppException {

        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);

        // Ширина лейбла делится поровну с правой частью
        ColumnConstraints cc = new ColumnConstraints( -1, 250, Double.MAX_VALUE );
        cc.setHgrow( Priority.ALWAYS );
        gridPane.getColumnConstraints().add( 0, cc );
        gridPane.getColumnConstraints().add( 1, cc );

        int i = 0;

        for( F7FilterItem item : U.iterable( filterItemsSet.stream().filter( (f)->!filterControlMap.containsKey( f.getColumn()) ).iterator() ) )
        {
            Label label = initLabelAndSearchTitle(item, gridPane, i, title);
            Node mainControl = initMainControl(item);
            Node exprControl = mainControl instanceof F7FilterTextField ? null : initExpressionControl( item );

            label.setLabelFor ( mainControl );

            filterControlMap.put( item.getColumn(), mainControl );

            if( exprControl != null )
                exprControlMap.put( item.getColumn(), (F7FilterTextField)exprControl );

            StackPane stackPane = null;

            if( exprControl != null )
                mainControl.getProperties().put( EXPRESSION_CONTROL_PROPERTY, exprControl );

            if( mainControl instanceof JInvCalendar ) {
                mainControl = initCalendar( mainControl, /*exprControl,*/ item.getMask() );
                item.setGeneratedExpression(true);
            }

            if( exprControl != null ) {
                exprControl.setVisible(false);
                stackPane = new StackPane( mainControl, exprControl );
            }
            else
                stackPane = new StackPane( mainControl );

            stackPane.setAlignment( Pos.CENTER_LEFT );
            gridPane.add( stackPane, 1, i );
            GridPane.setConstraints( stackPane, 1, i, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER );

            initChangeListener( mainControl );

            if( mainControl instanceof F7FilterTextField )
            {
                final Node toggleNode = ((F7FilterTextField)mainControl).createToggleNode();
                gridPane.add( toggleNode, 2, i);
                GridPane.setConstraints( toggleNode, 2, i, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
            }
            else
            {
                if( exprControl != null /*&& !(mainControl instanceof JInvCalendar)*/ )
                {
                    final F7FilterTextField f7_txt = (F7FilterTextField)exprControl;
                    final Node toggleNode = f7_txt.createToggleNode();
                    f7_txt.valueTypeProperty().addListener( new F7ExpressionChangeListener( mainControl, f7_txt ) );

                    gridPane.add(toggleNode, 2, i);
                    GridPane.setConstraints(toggleNode, 2, i, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
                }
/*
                    else
                    {
                        if( item.getType() != Boolean.class )
                        {
                            JInvCheckBox expressionBox = new JInvCheckBox();
                            expressionBox.setFocusTraversable(false);

                            if( stackPane.getChildren() != null && stackPane.getChildren().size() == 2 ) {
                                expressionBox.selectedProperty().addListener(new ExpressionChangeListener(stackPane.getChildren().get(0), stackPane.getChildren().get(1), item));
                            }

                            if( exprControl != null)
                            {
                                mainControl.getProperties().put(EXPRESSION_CONTROL_PROPERTY, expressionBox);
                                mainControl.getProperties().put(EXPRESSION_CONTROL, exprControl);
                                exprControl.getProperties().put(EXPRESSION_CONTROL_PROPERTY, expressionBox);
                                exprControl.getProperties().put(INITIAL_CONTROL, mainControl);
                            }

                            gridPane.add(expressionBox, 2, i);
                            GridPane.setConstraints(expressionBox, 2, i, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER);
                        }
                    }
 */
                }

                i++;

        }//end for

        return gridPane;
    }
/*
    private void switchInvertControlExpressionMode(Node node) {

        if (node != null && node.getProperties() != null) {
            JInvCheckBox box = (JInvCheckBox) node.getProperties().getOrDefault(EXPRESSION_CONTROL_PROPERTY, null);
            if (box != null) {
                box.selectedProperty().set(!box.selectedProperty().get());
            }
        }
    }

    private void switchControlExpressionMode(Node node, boolean value) {
        if (node != null && node.getProperties() != null) {
            JInvCheckBox box = (JInvCheckBox) node.getProperties().getOrDefault(EXPRESSION_CONTROL_PROPERTY, null);
            if (box != null) {
                box.selectedProperty().set(value);
            }
        }
    }
*/
    private boolean isNodeExpControlForCalendar(Node node) {
        return node.getProperties().getOrDefault(INITIAL_CONTROL, null) != null && node.getProperties().get(INITIAL_CONTROL) instanceof JInvCalendar;
    }

    private void doSearch() {
        if (edSearch.getValue() != null) {
            Label label = ((F7SearchModel) edSearch.getValue()).getLabel();
            if (label != null) {
                GridPane pane = (GridPane) label.getParent();
                int rowIndex = GridPane.getRowIndex(label);

                Node stackPane = pane.getChildren().stream().
                    filter((Node t) -> GridPane.getRowIndex(t).equals(rowIndex) && GridPane.getColumnIndex(t).equals(1)).
                    findFirst().orElse(null);

                if (stackPane != null && stackPane instanceof StackPane) {
                    List<Control> listControl = Controls.getControlList((Parent) stackPane, (Control t) -> t instanceof IFilterControl && t.isVisible());
                    if (listControl != null && !listControl.isEmpty()) {
                        Platform.runLater(() -> {
                            Control control = listControl.get(0);
                            if (getGroupPaneOfControl(control) != null && !getGroupPaneOfControl(control).isExpanded()) {
                                getGroupPaneOfControl(control).setExpanded(true);
                            }
                            Controls.scrollToNode(control);
                            control.requestFocus();
                        });

                    }
                }
            }
        }
    }

    // Поле для ввода имени компонента, для поиска его в списке фильтра
    protected void initSearchControl ( JInvComboBox ctrl )
    {
        edSearch = ctrl;
        edSearch.setItems(FXCollections.observableArrayList(searchDictionary));
        edSearch.setConverter(new StringConverter() {
            @Override
            public String toString(Object object) {
                return object == null ? null : object.toString();
            }

            @Override
            public Object fromString(String string) {
                return string == null ? null
                    : edSearch.getItems().stream().filter((Object t) -> ((F7SearchModel) t).getTitle().equals(string)).findFirst().orElse(null);
            }
        });

        bindingSearch = JInvBindings.bindAutoCompletion(edSearch);

        bindingSearch.minWidthProperty().bind(edSearch.widthProperty());

        edSearch.valueProperty().addListener((ObservableValue observable, Object oldValue, Object newValue) -> {
            doSearch();
        });

    }
/*
    private void initSearchControls(GridPane footerPane) {

        Label lbSearch = new Label(bundle.getString("F7FILTER_SEARCH"));
        footerPane.add(lbSearch, 0, 0);
        GridPane.setConstraints(lbSearch, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.NEVER, Priority.NEVER, new Insets(5));

        edSearch = new JInvComboBox ();

        footerPane.add(edSearch, 1, 0);
        GridPane.setConstraints(edSearch, 1, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.NEVER, new Insets(5, 0, 5, 5));
    }
*/
    /*
    private Object generateExpressionValueByExpression(String column, Object expValue) {
        return new StringBuilder(column).append(" ").append( expValue ).toString();

    }
    */

    /** Обработчик нажатия F7, для загрузки предшествующего фильтра */
    private class F7KeyHandler implements EventHandler<ActionEvent> {

        @Override
        public void handle( ActionEvent event )
        {
            if( dsAdapter.getTable() == null || !dsAdapter.getTable().getProperties().containsKey(PROPERTY_FILTER) )
                return;

            final List<F7FilterItem> list = (List<F7FilterItem>)dsAdapter.getTable().getProperties().getOrDefault( PROPERTY_FILTER, null );

            if( list == null )
                return;

            // Обнуляем текущее состояние всех полей для выражений
            exprControlMap.forEach( ( String t, Node u) -> Controls.setValue( u, null ) );

            // Сбрасываем значение компонентов фильтра и стиль с лабелов
            filterControlMap.forEach( ( String t, Node u ) -> {

                if( u instanceof JInvComboBox )
                    ((JInvComboBox) u).setValue(U.EMPTY);
                else
                    Controls.setValue( u, null );

                setFilterControlDecorator( u, false );
            });

            for( F7FilterItem item : U.iterable( mapGroupItems.values().stream().flatMap(coll -> coll.stream()).iterator() ) )
            {
                final String fieldName = item.getColumn();

                if( S.isNullOrEmpty(fieldName) )
                    continue;

                F7FilterItem savedItem = list.stream().filter((F7FilterItem t1)->t1.getColumn().equals(fieldName)).findFirst().orElse(null);

                if( savedItem != null )
                {
                    Node control = filterControlMap.get( fieldName );

                    item.setValue( savedItem.getValue() );
                    item.setGeneratedExpression( savedItem.isGeneratedExpression() );

                    if( control instanceof F7FilterTextField )
                    {
                        F7FilterTextField f7_txt = (F7FilterTextField)control;
                        f7_txt.setValue( savedItem.getValueType(), S.nz( savedItem.getControlValue() ) );
                    }
                    else
                    {
                        item.setValueType( savedItem.getValueType() );
                        //if( savedItem.isExpression() && !savedItem.isGeneratedExpression() )
                        if( savedItem.getValueType() != VALUE && !savedItem.isGeneratedExpression() )
                        {
                            F7FilterTextField exprControl = exprControlMap.get(fieldName);
                            if( exprControl != null )
                                exprControl.setValue( savedItem.getValueType(), (String)savedItem.getControlValue() );
                        }
                        else
                        {
                            setFilterControlDecorator  ( control, true );
//                            item.setGeneratedExpression( savedItem.isGeneratedExpression() );
                            Controls.setValue( control, savedItem.getControlValue() );
                        }
                        /*
                        if( savedItem.isExpression() && !savedItem.isGeneratedExpression())  {
                            item.setExpression(true);
                            control = exprControlMap.get(fieldName);
                            ((JInvCheckBox) control.getProperties().get(EXPRESSION_CONTROL_PROPERTY)).selectedProperty().set(true);

                        }
                        else
                        {
                            item.setExpression(false);
                            control = filterControlMap.get(fieldName);
                            setFilterControlDecorator(control, true);
                            ((JInvCheckBox) control.getProperties().get(EXPRESSION_CONTROL_PROPERTY)).selectedProperty().set(false);
                        }

                        Controls.setValue( control, unescapeApostrophe( savedItem.getControlValue() ) );
                        */
                    }

                    if( getGroupPaneOfControl(control) != null && !getGroupPaneOfControl(control).isExpanded() )
                        getGroupPaneOfControl(control).setExpanded(true);
                }

            }//end for

        }
    }

    private class ComboBoxStringConverterDecorator extends StringConverter<Object> {

        private final StringConverter converter;

        public ComboBoxStringConverterDecorator(StringConverter converter) {
            this.converter = converter;
        }

        @Override
        public String toString(Object object) {

            if (object == U.EMPTY) {
                return bundle.getString("FILTER_EMPTY");
            } else if (converter != null) {
                return converter.toString(object);
            } else {
                return object.toString();
            }
        }

        @Override
        public Object fromString(String string) {
            if (string != null && string.equals(bundle.getString("FILTER_EMPTY"))) {
                return U.EMPTY;
            } else if (converter != null) {
                return converter.fromString(string);
            } else {
                return string;
            }
        }

    }

    private JInvComboBox getComboBoxFromControl(IJInvControl control) {
        JInvComboBox result = new JInvComboBox();
        if (control instanceof JInvComboBox) {
            JInvComboBox comboBox = ((JInvComboBox) control);
            result.setValueFactory(comboBox.getValueFactory());
            result.setItems(FXCollections.observableArrayList(comboBox.getItems()));
            result.getItems().add(0, U.EMPTY);
            result.setConverter(new ComboBoxStringConverterDecorator(comboBox.getConverter()));

        } else if (control instanceof JInvRadioGroup) {

            JInvRadioGroup group = (JInvRadioGroup) control;
            ObservableList list = FXCollections.observableArrayList();
            list.add(U.EMPTY);
            final Map<String, String> mapValues = new HashMap<>();
            group.getToggles().forEach((Toggle t) -> {
                if (t instanceof JInvRadioButton) {
                    JInvRadioButton button = (JInvRadioButton) t;
                    if (button.getText() != null && !button.getText().isEmpty()) {
                        list.add(button.getValue());
                        mapValues.put(button.getText(), button.getValue());
                    } else {
                        list.add(button.getValue());
                        mapValues.put(button.getValue(), button.getValue());
                    }
                }
            });

            result.setItems(list);

            result.setConverter(new StringConverter() {
                @Override
                public String toString(final Object object) {
                    if (object == null) {
                        return null;
                    } else if (object == U.EMPTY) {
                        return bundle.getString("FILTER_EMPTY");
                    } else {
                        return mapValues.entrySet().stream().
                            filter((Map.Entry<String, String> t) -> t.getValue().equals(object)).
                            map(Map.Entry::getKey).
                            collect(Collectors.toSet()).
                            stream().
                            findFirst().
                            orElse(null);
                    }
                }

                @Override
                public Object fromString(String string) {
                    if (string == null) {
                        return null;
                    } else if (string.equals(bundle.getString("FILTER_EMPTY"))) {
                        return U.EMPTY;
                    } else {
                        return mapValues.get(string);
                    }
                }
            });
            result.setFieldName(group.getFieldName());
        } else if (control instanceof JInvCheckBox) {

            if (control instanceof JInvCheckBoxString) {
                JInvCheckBoxString boxString = (JInvCheckBoxString) control;
                ObservableList list = FXCollections.observableArrayList();
                list.add(U.EMPTY);
                if (boxString.getValueChecked() != null) {
                    list.add(boxString.getValueChecked());
                }
                if (boxString.getValueUnchecked() != null) {
                    list.add(boxString.getValueUnchecked());
                }
                result.setItems(list);
                result.setConverter(new JInvCheckBoxStringConverter(boxString));
            } else {
                result = getFilterComboBoxForClass(Boolean.class);
            }
        }
        result.setSelectedValue(U.EMPTY);
        return result;
    }

    private JInvComboBox getFilterComboBoxForClass(Class clazz) {
        JInvComboBox result = new JInvComboBox();
        if (clazz != null) {

            if (clazz == Boolean.class) {
                ObservableList list = FXCollections.observableArrayList();
                list.add(U.EMPTY);
                list.addAll(Boolean.TRUE, Boolean.FALSE);
                result.setItems(list);
                result.setConverter(converterForCheckBox);
            } else if (Enum.class.isAssignableFrom( clazz )) {
                ObservableList list = FXCollections.observableArrayList();
                list.add(U.EMPTY);
                list.addAll(clazz.getEnumConstants());
                result.setItems(list);
                result.setConverter(converterForCheckBox);
            }
            result.setSelectedValue(U.EMPTY);
        }
        return result;
    }

    private class JInvCheckBoxConverter extends StringConverter<Object> {

        @Override
        public String toString(Object object) {
            if (object == null) {
                return null;
            } else if (object == U.EMPTY) {
                return bundle.getString("FILTER_EMPTY");
            } else if (object.equals(Boolean.TRUE)) {
                return bundle.getString("FILTER_TRUE");
            } else if (object.equals(Boolean.FALSE)) {
                return bundle.getString("FILTER_FALSE");
            } else {
                return object.toString();
            }
        }

        @Override
        public Object fromString(String string) {
            if (string == null) {
                return null;
            } else if (string.equals(bundle.getString("FILTER_EMPTY"))) {
                return U.EMPTY;
            } else if (string.equals(bundle.getString("FILTER_TRUE"))) {
                return Boolean.TRUE;
            } else if (string.equals(bundle.getString("FILTER_FALSE"))) {
                return Boolean.FALSE;
            } else {
                return string;
            }
        }
    }

    private class JInvCheckBoxStringConverter extends StringConverter<Object> {

        private final JInvCheckBoxString box;

        public JInvCheckBoxStringConverter(JInvCheckBoxString box) {
            this.box = box;
        }

        @Override
        public String toString(Object object) {

            if (object == U.EMPTY) {
                return bundle.getString("FILTER_EMPTY");
            } else if ((object == null && box.getValueChecked() == null)
                || (object != null && box.getValueChecked() != null && object.equals(box.getValueChecked()))) {
                return bundle.getString("FILTER_TRUE");
            } else if ((object == null && box.getValueUnchecked() == null)
                || (object != null && box.getValueUnchecked() != null && object.equals(box.getValueUnchecked()))) {
                return bundle.getString("FILTER_FALSE");
            } else if (box.getOtherValues() != null) {
                switch (box.getOtherValues()) {
                    case CHECKED:
                        return bundle.getString("FILTER_TRUE");
                    case UNCHECKED:
                        return bundle.getString("FILTER_FALSE");
                    default:
                        throw new AssertionError(box.getOtherValues().name());
                }
            } else {
                return null;
            }
        }

        @Override
        public Object fromString(String string) {

            if (string == null) {
                return null;
            } else if (string.equals(bundle.getString("FILTER_EMPTY"))) {
                return U.EMPTY;
            } else if (string.equals(bundle.getString("FILTER_TRUE"))) {
                return box.getValueChecked();
            } else if (string.equals(bundle.getString("FILTER_FALSE"))) {
                return box.getValueUnchecked();
            } else {
                return new AssertionError(string);
            }
        }
    }

    private String getTitleFromControl( IJInvControl control )
    {
        String result = null;

        // Наименование из привязанного label
        Label label = control.getLabel();

        if( label != null )
        {
            String labelText = label.getText();
            if (labelText != null && !labelText.isEmpty()) {
                result = labelText;
            }
        } else if (control instanceof JInvCheckBox && S.isNotNullOrEmpty(((JInvCheckBox) control).getText())) {
            JInvCheckBox box = (JInvCheckBox) control;
            result = box.getText();
        }

        // Наименование из бандла
        if (result == null && control.getController() != null && S.isNotNullOrEmpty(control.getFieldName())) {
            String fromBundle = control.getController().getBundleString(control.getFieldName());
            if (!(S.isNullOrEmpty(fromBundle) || fromBundle.equals(control.getFieldName()))) {
                result = fromBundle;
            }
        }

        // Наименование из тултипа
        if( result == null && S.isNotNullOrEmpty(control.getToolTipText())) {
            result = control.getToolTipText();
        }
        return result;

    }

    /** */
    private class F7ExpressionChangeListener implements ChangeListener< IFilterItem.ValueTypeEnum > {

        private final Node valueControl;
        private final F7FilterTextField expressionControl;

        public F7ExpressionChangeListener( Node baseControl, F7FilterTextField expressionControl ) {
            this.valueControl      = baseControl;
            this.expressionControl = expressionControl;

        }
        @Override
        public void changed( ObservableValue<? extends IFilterItem.ValueTypeEnum> observable, IFilterItem.ValueTypeEnum oldValue, IFilterItem.ValueTypeEnum newType )
        {
            if( newType != VALUE )
            {
                valueControl     .setVisible(false);
                expressionControl.setVisible(true );
                expressionControl.requestFocus();

            }
            else
            {
                valueControl.setVisible(true);
                valueControl.requestFocus();
                expressionControl.setVisible(false);
            }
        }

    }


    private class ExpressionChangeListener implements ChangeListener<Boolean> {

        private final Node baseControl;
        private final Node expressionControl;
        private final F7FilterItem item;

        private Node expressionControlInner;
        private Node baseControlInner;

        public ExpressionChangeListener(Node baseControl, Node expressionControl, F7FilterItem item) {
            this.baseControl = baseControl;
            this.expressionControl = expressionControl;
            this.item = item;

            if (baseControl instanceof HBox) {
                expressionControlInner = ((HBox) expressionControl).getChildren().get(0);
                baseControlInner = ((HBox) baseControl).getChildren().get(0);
            }

        }

        @Override
        public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

            if (newValue) {
                baseControl.setVisible(false);
                expressionControl.setVisible(true);
                expressionControl.requestFocus();
                item.setExpression(true);

                if (!(baseControl instanceof CheckBox)) {

                    if (baseControlInner != null && expressionControlInner != null) {
                        expressionControlInner.requestFocus();
                        Controls.setValue(expressionControlInner,
                            Controls.getStringValueFromEditor(baseControlInner));
                    } else {
                        Controls.setValue(expressionControl, Controls.getStringValueFromEditor(baseControl));
                    }
                }
                if (baseControlInner != null) {
                    setFilterControlDecorator(baseControlInner, true);
                } else {
                    setFilterControlDecorator(baseControl, true);
                }

            } else {
                baseControl.setVisible(true);
                expressionControl.setVisible(false);

                if (baseControlInner != null) {
                    baseControlInner.requestFocus();
                } else {
                    baseControl.requestFocus();
                }
                item.setExpression(false);

                if (baseControlInner != null) {

                    if (ValidMan.isControlValueChanged(baseControlInner)) {
                        setFilterControlDecorator(baseControlInner, true);
                    } else {
                        setFilterControlDecorator(baseControlInner, false);
                    }

                } else if (ValidMan.isControlValueChanged(baseControl)) {
                    setFilterControlDecorator(baseControl, true);
                } else {
                    setFilterControlDecorator(baseControl, false);
                }
            }
        }

    }

    /** */
    private boolean checkValueFields() {

        return filterControlMap.
                values().
                stream().
                noneMatch (
                        (Node t) -> t.isVisible()
                        &&
                        t instanceof IStateControl
                        && ((IStateControl) t).stateProperty().get() == IStateControl.State.ERROR
                       );
    }

    public static class ProxyForValueHelper<P> {

        private final IEntityMetaData<P> metadata;
        P rowInstance;

        public ProxyForValueHelper( IEntityMetaData<P> metadata ) {
            this.metadata = metadata;
            try {
                rowInstance = this.metadata.getEntityClass().newInstance();
            } catch (Throwable ex) {
                JInvErrorService.handleException(null, ex);
            }
        }

        public Object getValue(String fieldNameProxy, String fieldName, Object proxyValue) {

            IEntityProperty<P,?> proxyFieldProperty = metadata.getProperty( fieldNameProxy );
            IEntityProperty<P,?> fieldProperty      = metadata.getProperty( fieldName );

            if ( proxyFieldProperty != null && fieldProperty != null )
            {
                proxyFieldProperty.invokeSetter  ( rowInstance, proxyValue );
                return fieldProperty.invokeGetter( rowInstance );
            }
            return null;
        }
    }

    /** */
    private void initChangeListener( Node control ) {

        Property valueProperty = null;

        if( control instanceof JInvValueField )
        {
            valueProperty = ((JInvValueField) control).valueProperty();
        }
        else
            if( control instanceof DatePicker )
            {
                if( control instanceof JInvCalendarTime )
                    valueProperty = ((JInvCalendarTime) control).dateTimeValueProperty();
                else
                    valueProperty = ((DatePicker) control).valueProperty();
            }
            else if (control instanceof CheckBox) {
                valueProperty = ((CheckBox) control).selectedProperty();
            } else if (control instanceof TextInputControl) {
                valueProperty = ((TextInputControl) control).textProperty();
            } else if (control instanceof JInvComboBox) {
                valueProperty = ((JInvComboBox) control).valueProperty();
            }

        if( valueProperty != null )
            valueProperty.addListener( new FilterControlChangeListener( control, valueProperty ) );
    }

    private class FilterControlChangeListener extends ValidInvalidationListener {

        public FilterControlChangeListener(Node control, Property property) {
            super(control);
        }

        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
            super.changed(observable, oldValue, newValue); //To change body of generated methods, choose Tools | Templates.
            setFilterControlDecorator(node, true);

            if (node instanceof JInvComboBox) {
                JInvComboBox box = (JInvComboBox) node;
                if (box.getValue() == U.EMPTY) {
                    setControlValueChanged(node, false);
                    setFilterControlDecorator(node, false);
                }
            } else if (node instanceof JInvTextField) {
                String value = ((JInvTextField) node).getText();
                if (value == null || value.isEmpty()) {
                    setControlValueChanged(node, false);
                    setFilterControlDecorator(node, false);
                }
            }
        }

    }

    /** Выделяем лейблы с заполненными полями для фильтров */
    private void setFilterControlDecorator( Node control, boolean val )
    {
        if( control != null && control instanceof IJInvControl )
        {
            Label label = ((IJInvControl) control).getLabel();
            if( label != null ) {
                label.pseudoClassStateChanged( labelDecorator, val );
            }
        }
    }

    /*
    private void setTextToExpressionControl( Control control, String text ) {
        JInvTextField field = (JInvTextField) control.getProperties().getOrDefault(EXPRESSION_CONTROL, null);
        Controls.setValue(field, text);
    }
    */

    /**
     * Показывает диалог календаря с диапазоном
     */
    public void showCalendarDialog( Control control )
    {
        try {

            Stage calendarStage = new Stage();
            calendarStage.initOwner( getScene().getWindow() );
            calendarStage.setTitle ( bundle.getString("FILTR_CALENDAR_RANGE") );
            if( control instanceof IJInvControl )
            {
                Label label = ((IJInvControl) control).getLabel();
                if( label != null)
                    calendarStage.setTitle( label.getText() );
            }

            CalendarRangeDialog dialog = null;
            Holder result = new Holder();

            JInvCalendar calendar;

            if (control instanceof JInvCalendar) {
                calendar = (JInvCalendar) control;
            } else {
                calendar = (JInvCalendar) control.getProperties().getOrDefault(INITIAL_CONTROL, null);
            }

            if ( calendar != null ){
                dialog = new CalendarRangeDialog<>( calendar, result, isUse_to_timestamp() );
            }

            if( dialog != null )
            {
                Scene scene = new Scene( dialog );
                calendarStage.setScene ( scene  );
                calendarStage.getScene ( ).getStylesheets().add("css/general.css");
                BaseApp.APP().getViewPrefService().refreshViewSettingsRoot( scene.getRoot() );
                calendarStage.initModality(Modality.WINDOW_MODAL);
                dialog.initKeyboard( );
                calendarStage.showAndWait( );

                if( result.get() != null )
                {
                    Object resultDate = result.get();

                    if( resultDate instanceof String )
                    {
                        F7FilterTextField f7_txt = (F7FilterTextField)control.getProperties().get(EXPRESSION_CONTROL_PROPERTY);
                        if( f7_txt != null )
                        {
                            f7_txt.setValue( EXPRESSION, (String)resultDate );
                            f7_txt.getFilterItem().setExpression         (true );
                            f7_txt.getFilterItem().setGeneratedExpression(false);
                        }
                        /*
                        switchControlExpressionMode(control, true);
                        if (control instanceof JInvTextField) {
                            Controls.setValue(control, (String) resultDate);
                        } else {
                            setTextToExpressionControl(control, (String) resultDate);
                        }
                        */
                    }
                    else
                    {
                        //switchControlExpressionMode(control, false);
                        Controls.setValue( calendar, resultDate );
                    }
                }
            }
        } catch( Throwable ex ) {
            JInvErrorService.handleException(null, ex);
        }
    }

    private void addItemToMap(F7FilterItem item, String idGroup) {

        List<F7FilterGroup> listGroup = dsAdapter.getListFilterGroup();

        F7FilterGroup group = null;

        if (listGroup != null && idGroup != null) {
            group = listGroup.stream().filter((F7FilterGroup t) -> t.getId().equals(idGroup)).findFirst().orElse(null);
        } else if (item.getTypeLocation().equals(TABLE)) {
            group = mapGroupItems.keySet().stream().filter((F7FilterGroup t) -> t.getId().equals(GROUP_TABLE_ID)).findFirst().orElse(null);
        } else if (item.getTypeLocation().equals(INFO)) {
            group = mapGroupItems.keySet().stream().filter((F7FilterGroup t) -> t.getId().equals(GROUP_INFO_ID)).findFirst().orElse(null);
        }

        if (group != null) {

            Set<F7FilterItem> listItems;
            if (mapGroupItems.containsKey(group)) {
                listItems = mapGroupItems.get(group);
            } else {
                listItems = new TreeSet<>();
                mapGroupItems.put(group, listItems);
            }
            listItems.add(item);
        }
    }

    /** */
    private HBox initCalendar( Node control, /*Node exprControl, */ String mask )
    {
        final JInvCalendar finalCalendar = (JInvCalendar)control;

        JInvChoiceButton calendarChoiceButton = new JInvChoiceButton( );
        calendarChoiceButton.setFocusTraversable(false);

        HBox calendarBox;

        if( control instanceof JInvCalendarTime )
        {
            if( mask != null )
                finalCalendar.setMask( mask );

            ( (JInvCalendarTime)finalCalendar ).setDateOnlyMode( true );
            ( (JInvCalendarTime)finalCalendar ).setRangeable( true );

            //Галка "Показать время", только для календарей со временем
            JInvCheckBox calendarDayToggle = new JInvCheckBox( bundle.getString( "CAL_TIME_TOGGLE" ) );
            calendarDayToggle.setFocusTraversable( false );
            calendarDayToggle.selectedProperty().addListener( ( ob, oldVal, newVal ) -> {
                if(!Objects.equals(newVal, oldVal))
                {
                    ( (JInvCalendarTime) finalCalendar ).setDateOnlyMode( !newVal );
                    calendarDayToggle.requestFocus();
                    finalCalendar.requestFocus();
                    calendarDayToggle.requestFocus();
                    finalCalendar.requestFocus();
                }
            } );
            calendarBox = new HBox( finalCalendar, calendarChoiceButton, calendarDayToggle );
        }
        else
        {
            calendarBox = new HBox( finalCalendar, calendarChoiceButton );
        }

        calendarBox.setSpacing(5.0);

        calendarChoiceButton.setOnAction((ActionEvent event) -> {
            calendarChoiceButton.requestFocus();
            showCalendarDialog(finalCalendar);
        });

        calendarBox.setAlignment(Pos.CENTER_LEFT);

        JInvChoiceButton calendarExpChoiceButton = new JInvChoiceButton();
        calendarExpChoiceButton.setFocusTraversable(false);

        calendarExpChoiceButton.setOnAction((ActionEvent event) -> {
            showCalendarDialog(finalCalendar);
        });

        initChangeListener( finalCalendar );

        /*
        HBox calendarExpBox = new HBox(exprControl, calendarExpChoiceButton);
        calendarExpBox.setVisible(false);
        HBox.setHgrow(exprControl, Priority.ALWAYS);
        calendarExpBox.setAlignment(Pos.CENTER_LEFT);
        calendarExpBox.setFillHeight(true);
        return new StackPane(calendarBox, calendarExpBox);
        */

        return calendarBox;

    }

    /**
     * Сущность модели комбобокса поиска
     */
    private class F7SearchModel {

        private String title;
        private Label label;

        public F7SearchModel(String title, Label label) {
            this.title = title;
            this.label = label;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Label getLabel() {
            return label;
        }

        public void setLabel(Label label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return title;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.title);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final F7SearchModel other = (F7SearchModel) obj;
            if (!Objects.equals(this.title, other.title)) {
                return false;
            }
            return true;
        }
    }

    /** */
    private String getPredicateExpression( boolean indexSearch, RegisterEnum caseMode, String column, Object controlValue, Class type, boolean ignore95LikeSym )
    {
        final String stringValue = controlValue.toString();

        StringBuilder sb = new StringBuilder();

        //проверка на '#' если содержит то оставляем как есть
        if( U.inChar( stringValue.charAt(0), '#' ) )
        {
            sb.append(stringValue);
            return sb.toString();
        }

        if( type == String.class )
        {
            // Для НЕ индексных полей всегда upper.
            // Для индексных смотрим из метаданных
            switch( caseMode )
            {
                case UPPER_CASE:
                    sb.append("UPPER(").append(column).append(")");
                    break;
                case LOWER_CASE:
                    sb.append("LOWER(").append(column).append(")");
                    break;
                case UNKNOWN_CASE:
                    sb.append(column);
                    break;
                default:
                    break;
            }
        }
        else
        {
            sb.append(column);
        }


        // Если значение содержит % и _ то оставляем как есть и ставим like
        if( !Temporal.class.isAssignableFrom( type ) && S.contains( stringValue, '%', ( ignore95LikeSym ? (char)255 : '_' ) ))
        {
            sb.append(" like('").append( stringValue ).append("')");
        }

        //Если значение содержит знаки = или != и тип не String, то оставляем как есть
        else
            if( type != String.class && stringValue.contains("!=")
                 ||
                 type != String.class && stringValue.contains("=" ) )
        {
            sb.append( stringValue );
        }

        //Если тип String и значение содержит знаки = или != в начале строки, 
        //то оборачиваем в одинарные кавычки и подставляем IN или NOT IN
        else if(type == String.class && stringValue.charAt(0)=='!'&& stringValue.charAt(1)=='='||
                type == String.class && stringValue.charAt(0)=='=')
        {
            if(stringValue.contains("!="))
                sb.append(" NOT IN ('").append( stringValue.substring(2)).append("')");
            else
                sb.append(" IN ('").append( stringValue.substring(1)).append("')"); 
        }
        else
        {
            // Если значение содержит знаки < или > и тип не String, то оставляем их как есть
            if( type != String.class && S.contains( stringValue, '>', '<' ) ) // stringValue.indexOf('>') != -1 || stringValue.indexOf('<') != -1 ) )
            {
                sb.append( stringValue );
            }

            //Если значение содержит знаки < или > и тип String, то оборачиваем в одинарные кавычки
            else if(type == String.class && U.inChar(stringValue.charAt(0), '>', '<' ))
            {
                //Подразумеваем что само значение не содержит > или < в начале (первые два символа это <>)
                if( U.inChar(stringValue.charAt(0),'>', '<') && U.inChar(stringValue.charAt(1),'>', '<') )
                    sb.append( stringValue.charAt(0)).append( stringValue.charAt(1)).append(S.apstrph(stringValue.substring(2)));
                else
                    sb.append( stringValue.charAt(0)).append(S.apstrph(stringValue.substring(1)));
            }
            else
                if( indexSearch )
                {
                    // Если индексный поиск и значение
                    // не содержит спец символов, то ставим строго равно
                    if( type == String.class )
                    {
                        sb.append(" = '").append(stringValue).append("'");
                    } else
                    {
                        sb.append(" = ").append(stringValue);
                    }
                }
                else
                    // Если НЕ индексный поиск и значение не содержит спец. символы, то для СТРОК ставим like и обрамляем %
                    if( type == String.class && !ignore95LikeSym )
                    {
                        sb.append(" like ('").append("%").append(stringValue).append("%").append("')");
                    }
                    else
                    {
                        sb.append(" = ").append(stringValue);
                    }
            }
        
        return sb.toString();
    }

    /** */
    private Object getPredicateForCalendarTime( String mask, String column, LocalDateTime controlValue) {

        StringBuilder sb = new StringBuilder(column);
        sb.append(" between ").append( isUse_to_timestamp() ? " to_timestamp('" : " to_date('");

        LocalDate valueDate = controlValue.toLocalDate();
        LocalDateTime valueDateWithZeroTime;
        LocalDateTime valueDateWithFinishTime;

        if( !isMaskContainsTime(mask) || !isMaskContainsHours(mask) )
        {
            valueDateWithZeroTime   = LocalDateTime.of(valueDate, LocalTime.MIDNIGHT);
            valueDateWithFinishTime = LocalDateTime.of(valueDate, LocalTime.MAX);

        } else if (!isMaskContainsMinutes(mask)) {

            valueDateWithZeroTime = LocalDateTime.of(valueDate, controlValue.toLocalTime().truncatedTo(ChronoUnit.HOURS));
            valueDateWithFinishTime = valueDateWithZeroTime.plusHours(1);

        } else if (!isMaskContainsSeconds(mask)) {
            valueDateWithZeroTime = LocalDateTime.of(valueDate, controlValue.toLocalTime().truncatedTo(ChronoUnit.MINUTES));
            valueDateWithFinishTime = valueDateWithZeroTime.plusMinutes(1);
        }
        else
        {
            return new StringBuilder(column).append(" = ").append( isUse_to_timestamp() ? " to_timestamp('" : " to_date('")//append(" = to_date('")
                .append( TypeConverter.convertToString(controlValue, null) )
                .append( "','dd.mm.yyyy HH24:mi:ss')")
            .toString();
        }

        if( valueDateWithZeroTime != null && valueDateWithFinishTime != null )
        {
            sb.append(TypeConverter.convertToString(valueDateWithZeroTime, null)).append("','dd.mm.yyyy HH24:mi:ss')");
            sb.append( isUse_to_timestamp() ? "and to_timestamp('" : "and to_date('");//sb.append(" and to_date('");
            sb.append(TypeConverter.convertToString(valueDateWithFinishTime, null)).append("','dd.mm.yyyy HH24:mi:ss')");
        }

        return sb.toString();
    }

    private boolean isMaskContainsTime(String mask) {
        // HH:mm:ss
        return mask.contains("HH") || mask.contains("mm") || mask.contains("ss");
    }

    private boolean isMaskContainsHours(String mask) {
        return mask.contains("HH");
    }

    private boolean isMaskContainsMinutes(String mask) {
        return mask.contains("mm");
    }

    private boolean isMaskContainsSeconds(String mask) {
        return mask.contains("ss");
    }
    
    /** Проверка на спец. символы */
    private boolean containsExprSymb( String s )
    {
        if( S.isNullOrEmpty(s) )
            return false;
        //Проверяем наличие в '!='
        if(U.in(s.charAt(0),'!')){
            if(s.length()>1)
                if(U.in(s.charAt(1),'='))
                    return true;
            return false;
        }
        return U.in(s.charAt(0), '#', '>', '<','=' );
    }
}
