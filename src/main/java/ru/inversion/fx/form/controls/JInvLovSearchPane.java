package ru.inversion.fx.form.controls;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Pair;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.lov.ViewEntityLovController;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.meta.EntityMetadataFactory;
import ru.inversion.meta.IEntityMetaData;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.S;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_FIELD_NAME;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_TRANSIENT;

/**
 * Компонента для усиления предиката для DataSet
 * <p>
 * Компонента создает предикат :lov_parameter вида:
 * upper(COLUMN_1||COLUMN_2||COLUMN_N) like upper(:lov_parameter) Для
 * инициализации метод init() Значения COLUMN берет из Entity. Для случаев где в
 * SELECT NamedNativeQuery уже определен предикат :lov_parameter то значение
 * configPredicat должно быть false
 * <p>
 * При очистке фильтра по умолчанию у DataSet вызывается
 * DataSet.executeQuery(false), если для обновления необходимо выполнять доп.
 * действия, установите callback.
 *
 * @author perov
 */
public class JInvLovSearchPane extends AnchorPane {

    final private static ResourceBundle g_lovBundle = ResourceBundle.getBundle("jinvlovsearchpane");

    public interface State {
        /** Формирование предиката */
        void configPredicate( );
    }

    private final State startState;
    private final State emptyState;
    private final State workState;
    private final State startStateWithFilter;

    private State currentState;

    /** Для ограничения максимального размера выборки из <code>DataSet</code> для маленького лова */
    private int maxPageSize = -1;

    private IFormStateListener listener;

    private SQLDataSet<Object> dataSet;
    private JInvTable<Object> table;
    private Timeline timeline;

    private boolean configPredicate;
    private String filterString;
    private boolean fixed;
    private String initialSearch;
    private String columnValue;

    private int wherePredicateId = -1;

    private final TextField edField = new TextField();
    private final JInvCheckBox cbAutoFilter = new JInvCheckBox(g_lovBundle.getString("CHECK_BOX.LOAD"));

    /** */
    private final BiConsumer<Boolean, Throwable> postExecQuery = new BiConsumer<Boolean, Throwable>()
    {
        @Override
        public void accept(Boolean t, Throwable u) 
        {
            if(!t)
               throw new RuntimeException(u);

            if( maxPageSize > 0 && getDataSet().getTotalRowCount() > maxPageSize )
                throw new RuntimeException( g_lovBundle.getString("ERROR.LOV_IS_NOT_SMALL") );

            selectRow();
            getDataSet().clearFilter(false);
        }
    };

    /** */
    public void setListener( IFormStateListener listener ) {
        this.listener = listener;
    }

    /** */
    private Runnable execQuery = new Runnable ()
    {
        @Override
        public void run () 
        {
            if( maxPageSize > 0 )
                getDataSet().setPageSize(maxPageSize + 1);

            if( table != null ) {
                table.executeQuery( postExecQuery, listener );
                return;
            }

            if( dataSet != null )
            {
                try {

                    dataSet.executeQuery();

                    if( maxPageSize > 0 && getDataSet().getTotalRowCount() > maxPageSize )
                        throw new RuntimeException(g_lovBundle.getString("ERROR.LOV_IS_NOT_SMALL"));

                    selectRow();

                    getDataSet().clearFilter(false);

                } catch (DataSetException ex) {
                    JInvErrorService.handleException(null, ex);
                }
            }
        }
    };

    /** */
    public void setMaxPageSize( int maxPageSize ) {

        if( maxPageSize>0 )
            edField.setText("%");

        this.maxPageSize = maxPageSize;
    }

    /** */
    private void selectRow() {
        if( maxPageSize != -1 )
        { //если максайз не задан не выделяем строку
            int i = 0;
            for (Object r : table.getItems()) {
                @SuppressWarnings("unchecked")
                TableColumn<Object, Object> c = (TableColumn) table.getColumns().get(0);
                String val = TypeConverter.convert(c.getCellData(r), String.class);
                if (val != null && val.equals(initialSearch)) {
                    final int j = i;
                    Platform.runLater(() -> {
                        table.requestFocus();
                        table.getSelectionModel().select(r);
                        getDataSet().setCurrentRowNum(j);
                    });
                    break;
                }
                i++;
            }
        }
    }
    
    /** Действия (по умолчанию DataSet.executeQuery(false)) */
    public void setCallBack(Runnable clb)
    {
        this.execQuery = clb;
    }

    /** */
    public SQLDataSet<?> getDataSet() {
        return dataSet;
    }

    /** */
    private void doInit( boolean configPredicate, String filterString, boolean fixed, String initialSearch, String columnValue )
    {
        this.configPredicate = configPredicate;
        this.filterString    = filterString;
        this.fixed           = fixed;
        this.initialSearch   = initialSearch;
        this.columnValue     = columnValue;
        
        if( S.isNotNullOrEmpty(initialSearch) )
        {
            if( S.lastChar(initialSearch) != '%' )
                this.initialSearch += "%";

            this.edField.setText(this.initialSearch);
        }

        /*
         *  Если перед вызовом у датасета установлен фильтр – не перетирать его установкой filterString
         *  Если initialSearch не пуст – не устанавливать filterString если fixed == false
         */
        if( S.isNotNullOrEmpty(filterString) && ( initialSearch == null || initialSearch.isEmpty()) )
        {
            this.dataSet.setFilter( filterString, fixed, false);
        }
        
        Platform.runLater(() -> actionFind());
    }

    
    /**
     * Метод инициализации компонента.
     * Компонент создает предикат для :lov_parameter вида:
     * upper(COLUMN_1||COLUMN_2||COLUMN_N) like upper(:lov_parameter)
     * Значения COLUMN берет из Entity. Для случаев где в SELECT
     * NamedNativeQuery определен предикат :lov_parameter то значение
     * configPredicat должно быть false
     *
     * @param table
     * @param configPredicate true - для создания предиката :lov_parameter, false
     * - если предикат :lov_parameter уже определен
     * @param filterString - дополнительный фильтр, текст
     * @param fixed - дополнительный фильтр, фиксированный
     * @param initialSearch - входное значение из TextField
     * @param columnValue - имя первого COLUMN в предикате
     */
    public void init( TableView<?> table, boolean configPredicate, String filterString, boolean fixed, String initialSearch, String columnValue )
    {
        if( this.table != null )
            throw new IllegalStateException("table is already set!");

        if( table instanceof JInvTable)
        {
            this.table   = (JInvTable)table;
            this.dataSet = (SQLDataSet)((JInvTable)table).getDataSetAdapter().getDataSet();
            
            //JAVAKERNEL-559 JInvLovSearchPane -- не устанавливает setChoiceControl
            JInvFXFormController<?> controllerFromControl = Controls.getControllerFromControl(table);
            if( controllerFromControl != null )
                controllerFromControl.setChoiceControl(this.table);

        }

        doInit( configPredicate, filterString, fixed, initialSearch, columnValue );
    }
    

     /**
     * Метод инициализации компонента.
     * <p>
     * Компонента создает предикат :lov_parameter
     * вида: upper(COLUMN_1||COLUMN_2||COLUMN_N) like upper(:lov_parameter)
     * Значения COLUMN берет из Entity. Для случаев где в SELECT
     * NamedNativeQuery определен предикат :lov_parameter то значение
     * configPredicat должно быть false
     *
     *     
     * @param dataSet
     * @param configPredicat true - для создания предиката :lov_parameter, false
     * - если предикат :lov_parameter уже определен
     * @param filterString - дополнительный фильтр, текст
     * @param fixed - дополнительный фильтр, фиксированный
     * @param initialSearch - входное значение из TextField
     * @param columnValue
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public void init(SQLDataSet<?> dataSet, boolean configPredicat, String filterString, boolean fixed, String initialSearch, String columnValue)
    {
        if( this.dataSet != null )
            throw new IllegalStateException("table is already set!");
        
        this.dataSet = (SQLDataSet<Object>) dataSet;

        doInit( configPredicat,filterString,fixed,initialSearch,columnValue );
    }

    /**
     * См выше, чисто для удобства передачи в вызов AbstractEntityLovBase.getChoiceFilter ()
     */
    public void init( TableView<?> table, boolean configPredicate, Pair<String, Boolean> lovChoiceFilter, String initialSearch, String columnValue )
    {
        if( lovChoiceFilter != null )
            init (table, configPredicate, lovChoiceFilter.getKey (), lovChoiceFilter.getValue (), initialSearch, columnValue);
        else
            init (table, configPredicate, S.EMPTY_STRING, false, initialSearch, columnValue);
    }

    public JInvLovSearchPane() {
        startState = new StartState();
        emptyState = new EmptySearchStringState();
        workState = new WorkState();
        startStateWithFilter = new StartStateWithFilter();        
        setState(startState);
        initComponents();
    }

    /** */
    private void initComponents() {

        edField.setText("%");

        VBox vbox = new VBox();
        HBox hbox = new HBox();

        ButtonBase btFind = ActionFactory.createButton(FontAwesome.fa_search, (ActionEvent event) -> {
            actionFind();
        });
        ButtonBase btClear = ActionFactory.createButton(FontAwesome.fa_close, (ActionEvent event) -> {
            clearFilter();
        });
         btFind.setTooltip(new Tooltip(g_lovBundle.getString("TOOLTIP.BT_FIND")));
        btClear.setTooltip(new Tooltip(g_lovBundle.getString("TOOLTIP.BT_CLEAR")));

        hbox.getChildren().addAll(new JInvLabel(g_lovBundle.getString("LABEL.FIND")), edField, btFind, btClear);
        HBox.setHgrow(edField, Priority.ALWAYS);
        hbox.setSpacing(5.0);
        hbox.setFillHeight(true);
        hbox.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(this, Insets.EMPTY);
        vbox.getChildren().addAll(hbox, cbAutoFilter);
        vbox.setSpacing(5.0);
        vbox.setFillWidth(true);

        AnchorPane.setTopAnchor(vbox, 5.0);
        AnchorPane.setRightAnchor(vbox, 5.0);
        AnchorPane.setBottomAnchor(vbox, 5.0);
        AnchorPane.setLeftAnchor(vbox, 5.0);

        this.getChildren().add(vbox);

        //текстфилд
        //запрос по нажатию на Enter
        edField.addEventHandler (KeyEvent.KEY_PRESSED, (KeyEvent event) ->
        {
            if( event.getCode () == KeyCode.ENTER )
            {
                event.consume ();
                actionFind ();                
            }
        });

        //Чекбокс
        cbAutoFilter.setFocusTraversable(false);

        cbAutoFilter.selectedProperty().addListener(new ChangeListener<Boolean>() {

                final ChangeListenerImpl changeListener = new ChangeListenerImpl( );

                @Override
                public void changed( ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue ) {

                    if( newValue )
                    {
                        edField.textProperty().addListener(changeListener);
                        // таймер для задержки автозапроса
                        timeline = new Timeline(new KeyFrame(Duration.millis(500), (ActionEvent ae) -> {
                            actionFind();
                            timeline.stop();
                        }));

                    }
                    else
                    {
                        edField.textProperty().removeListener(changeListener);
                        timeline = null;
                    }
                }

                class ChangeListenerImpl implements ChangeListener<String> {

                    public ChangeListenerImpl() {
                    }

                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        // Запускаем таймер actionFind();
                        timeline.stop();
                        timeline.play();
                    }
                }
            }
        );
    }

    /** */
    private TaskContext getTaskContext(){
        return dataSet.getTaskContext();
    }

    /**
     * Текст для поиска.
     */
    private String getTextSearch()
    {
        String value = edField.getText();
        if( S.lastChar(value) != '%')
            value += "%";
        return value;
    }
    
  
    /**
     * Усиливаем предикат
     */
    private void actionFind() {
        Objects.requireNonNull(getDataSet());        
        if(this.configPredicate) {
            currentState.configPredicate();
        } else {
            getDataSet().setParameter("lov_parameter", getTextSearch());
        }
        refreshDS();    
    }

    /**
     * Очистка фильтра и обновление dataSet. По умолчанию у DataSet вызывается
     * DataSet.executeQuery(false), если для обновления необходимо выполнять доп. действия, установите callback.
     */
    private void clearFilter() {
        edField.setText("%");
        actionFind();        
    }

    /** */
    private void refreshDS() {
        execQuery.run();
    }

    /** */
    private void setWherePredicate( String predicate, String value )
    {
        if( wherePredicateId != -1 )
        {    
            getDataSet().removeWherePredicate( wherePredicateId );
            wherePredicateId = -1;
        }    
        
        if( predicate != null )
        {    
            wherePredicateId = getDataSet ().setWherePredicate(predicate, true);
            getDataSet().setParameter( "lov_parameter", value );
        }
    }        
    
    /**
     * Установка текущего состояния
     * @param state 
     */
    public final void setState ( State state ) {
        this.currentState = state;
    }
    
    public State getEmptyState () {
        return this.emptyState;
    }
    
    public State getWorkState () {
        return this.workState;    
    }
    
    public State getStartState () {
        return startState;
    }
    
    public State getStartStateWithFilter () {
        return startStateWithFilter;
    }

/**
 * Стартовое состояние.
 * <p>
 * Тонкий момент – хотелось бы чтоб то, что передано в init в initialSearch
 * искалось только в поле columnValue, по LIKE конечно же,
 * но без регистрозависимости, и только при старте лова.
 * Если юзер его изменяет, то далее уже как сделано сейчас.
 * Цель – быстрый старт лова при уже правильном введенном руками значения или его начальной части
 */
class StartState implements State {

    @Override
    public void configPredicate( ) {

        final String textSearch = getTextSearch();

        if( S.isNullOrEmpty(textSearch) || "%".equals(textSearch) )
        {
            setState( getEmptyState() );
            currentState.configPredicate();
        }
        else
            if( S.isNotNullOrEmpty(columnValue) && configPredicate )
            {
                final IEntityMetaData<?> em = EntityMetadataFactory.getEntityMetaData( getDataSet().getRowClass() );
                IEntityProperty<?, ?> pl = em.getProperty(columnValue);
                setWherePredicate( getTaskContext().dialect().forLovPredicate(pl,"lov_parameter"), textSearch );

                setState(getWorkState());
            }
    }
}

/** */
class StartStateWithFilter implements State {

    @Override
    public void configPredicate() {
        
        if (S.isNullOrEmpty(getTextSearch()) || getTextSearch().equals("%")) {
            setState(getEmptyState());
            currentState.configPredicate();
        } else if (S.isNotNullOrEmpty(columnValue) && configPredicate) {

            final IEntityMetaData<?> em = EntityMetadataFactory.getEntityMetaData( getDataSet().getRowClass() );
            IEntityProperty<?, ?> pl = em.getProperty(columnValue);
            setWherePredicate( getTaskContext().dialect().forLovPredicate( pl,"lov_parameter"), getTextSearch() );

            if( S.isNotNullOrEmpty(filterString) )
                getDataSet().setFilter( filterString, fixed, true );

            setState(getWorkState());
        }
    }
    
}

/** */
class EmptySearchStringState implements State {

    @Override
    public void configPredicate() {
        setWherePredicate( null, null );
        setState( getWorkState() );
    }
}

    /** */
    private class WorkState implements State {

        @Override
        public void configPredicate()
        {
            if( S.isNullOrEmpty( getTextSearch() ) || getTextSearch().equals("%") )
            {
                setState(getEmptyState());
                currentState.configPredicate();
            }
            else
            {
                final IEntityMetaData<?> entityMetaData = EntityMetadataFactory.getEntityMetaData(getDataSet().getRowClass());
                final List<String> columnList = new ArrayList<>();

                //если задан ключевой столбец, то он должен быть первым
                //if( !S.isNullOrEmpty(columnValue) )
                //    columnList.add(columnValue);

//                if( table.getController( ) instanceof ViewEntityLovController )
                {
                    //JAVAKERNEL-1800 Поиск производим только по колонкам, которые находятся в properties
                    table.getColumns( ).forEach (
                        (c) -> {
                            final Object o = c.getProperties().get(COLUMN_TRANSIENT);
                            if( Boolean.TRUE.equals(o) || !c.getProperties().containsKey(COLUMN_FIELD_NAME) )
                                ;
                            else
                                columnList.add( c.getProperties().get(COLUMN_FIELD_NAME).toString() );
                        }
                    );
                }
//                else
//                {
//                    entityMetaData.getColumnsMap()
//                            .values()
//                                .stream().filter( p ->!"ROWID".equalsIgnoreCase(p.getColumnInfo().getName()) )
//                                    .map(p->p.getColumnInfo().getName())
//                                        .collect( Collectors.toCollection( ()->columnList) );
//                }

                final String strSQL= getTaskContext().dialect().forLovPredicate( "lov_parameter", columnList );
                /*
                if( getTaskContext().isPostgreSql() )
                {
                    //Объединяем в ф-цию concat
                    strSQL = columnList.stream().collect( Collectors.joining("::varchar),upper(", "CONCAT(upper(", "::varchar)) like upper(:lov_parameter)"));
                }
                else
                    //Разделяем значения через пробел чтобы, результат поиска не выдавал значения из пересечения колонок
                    //strSQL = columnList.stream().collect(Collectors.joining("||' '||", "upper(", ") like upper(:lov_parameter)"));
                    strSQL = getTaskContext().dialect().forLovPredicate(entityMetaData,"lov_parameter");
                */
                setWherePredicate( strSQL, getTextSearch ());
            }
        }
    }

}
