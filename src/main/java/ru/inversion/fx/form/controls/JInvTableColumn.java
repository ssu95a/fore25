package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.IViewPrefSaver;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.renderer.*;
import ru.inversion.utils.ConsumerWithException;

import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Function;


import static ru.inversion.fx.app.service.ViewPrefAppService.DIMENSION_FRACTIONAL_FACTOR;
import static ru.inversion.fx.app.service.ViewPrefAppService.isMarkColumn;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_GROUP_ID;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_ORDER_IN_GROUP;
import static ru.inversion.fx.form.lov.JInvEntityLov.LOV_CLASS_NAME;
import static ru.inversion.fx.form.lov.JInvEntityLov.LOV_VALIDATE_FROM_LOV;

/**
 * @author antonovdi
 */
public class JInvTableColumn<S, T> extends TableColumn<S, T> implements IViewChangeable, IFilterControl {

//    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ResourceBundle FORE = ResourceBundle.getBundle("fore");

    public static final String COLUMN_TRANSIENT = "ru.inversion.column.transient";
    public static final String COLUMN_QUERY_EXPR = "ru.inversion.column.query_expr";
    public static final String COLUMN_PROXY_FOR = "ru.inversion.column.proxy_for";

    public static final String COLUMN_SHOW_IN_FILTER = "ru.inversion.column.show_in_filter";

    public static final String COLUMN_ORDERBY = "ru.inversion.order_by";
    public static final String COLUMN_MARK = "ru.inversion.mark_column";
    public static final String COLUMN_LOV = "ru.inversion.lov_column";
    public static final String COLUMN_FIELD_NAME = "ru.inversion.field_name";
    public static final String COLUMN_USER_RENDERER = "ru.inversion.renderer.clb";
    public static final String COLUMN_COLOR_RENDERER = "ru.inversion.renderer.color";
    public static final String COLUMN_PREF_WIDTH = "ru.inversion.pref_width";
    public static final String COLUMN_FONT_SIZE_ADJUST = "ru.inversion.column_font_size_adjust";
    public static final String COLUMN_USER_COMMIT = "ru.inversion.column.usercommit";

    private static final double COLUMN_MIN_WIDTH = 1000000000.0d;//10.0d * DIMENSION_FRACTIONAL_FACTOR;
                                                                 //Это десять пикселей, умноженные на DIMENSION_FRACTIONAL_NUMBER
    protected String fieldName;    // Имя поля в таблице
    //    protected Boolean transientColumn;
    protected Pos alignment = ContentTypeManager.ALLGN_DEFAULT;
    protected StringProperty mask = new SimpleStringProperty();
    protected StringProperty toolTipTextProperty = new SimpleStringProperty();
    //Для сортировки столбцов таблицы
//    protected int indexColumn;

    private IViewPrefSaver prefSaver;

    public JInvTableColumn() {
        super();
        setEditable(false);
    }

    public JInvTableColumn(String name) {
        super(name);
        setEditable(false);
    }

    //    public int getIndexColumn() {
//        return indexColumn;
//    }
//
//    public void setIndexColumn(int indexColumn) {
//
//        this.indexColumn = indexColumn;
//    }
    public StringProperty toolTipTextProperty() {

        return toolTipTextProperty;
    }
    public String getToolTipText() {

        return toolTipTextProperty.get();
    }
    public void setToolTipText(String toolTipText) {
        toolTipTextProperty.set(toolTipText);
    }

    public final String getMask() {
        return mask.get();
    }

    /**
     Своя реализация коммита по изменению ячейки.
     Используется в JInvCheckBoxCellEditor
     @see ru.inversion.fx.form.controls.renderer.JInvCheckBoxCellEditor
     @param userCommit своя реализация коммита, кидающая Exception. самому ловить нет необходимости
     */
    public void setUserCommit( ConsumerWithException<CellEditorInfo<S, T>> userCommit ) {
        getProperties().put(COLUMN_USER_COMMIT, userCommit);
    }

    public final void setMask(String value) {
        mask.set(value);
    }

    public final StringProperty maskProperty() {
        return mask;
    }

    public Pos getAlignment() {
        return alignment;
    }

    public void setAlignment(Pos alignment) {
        this.alignment = alignment;
    }

    /**
     * Возвращает признак показывать ли колонку в фильтре
     */
    public boolean isShowInFilter() {
        return (Boolean) getProperties().getOrDefault(COLUMN_SHOW_IN_FILTER, Boolean.TRUE);
    }

    /**
     * Устанавливает признак показывать ли колонку в фильтре
     */
    public void setShowInFilter(boolean val) {
        getProperties().put(COLUMN_SHOW_IN_FILTER, val);
    }

    public Boolean getTransientColumn() {
        return (Boolean) getProperties().getOrDefault(COLUMN_TRANSIENT, Boolean.FALSE);
    }

    public void setTransientColumn(Boolean val) {
        if (val != null) {
            getProperties().put(COLUMN_TRANSIENT, val);
        }
    }

    public String getFieldName() {
        return (String) getProperties().getOrDefault(COLUMN_FIELD_NAME, "");
    }

    public void setFieldName(String fieldName) {
        getProperties().put(COLUMN_FIELD_NAME, fieldName);
    }

    public void setCellRenderer(BiConsumer<JInvTableCell<S, T>, T> clb) {
        getProperties().put(COLUMN_USER_RENDERER, clb);
    }

    /**
     Добавить правило раскраски для определённой колонки
     */
    public void addColor( final Function<IColoredCell<S>, Colorizer> styleExpr ) {
        BiConsumer<JInvTableCell<S, T>, T> existingColor = (BiConsumer<JInvTableCell<S, T>, T>) getProperties().getOrDefault( COLUMN_COLOR_RENDERER, null );

        BiConsumer<JInvTableCell<S, T>, T> colorizer = ( cell, value ) -> cell.addColor( styleExpr );

        boolean isClean = existingColor == null;

        if ( !isClean ) {
            colorizer = existingColor.andThen( colorizer );
        }

        setCellRenderer( colorizer );
        getProperties().put( COLUMN_COLOR_RENDERER, colorizer );
    }

    /**
     Убрать всю раскраску, заданную через addColor() в данном столбце
     */
    public void clearColor(){
        BiConsumer<JInvTableCell<S, T>, T> colorizer = ( cell, value ) -> cell.clearColor();
        setCellRenderer( colorizer );
        getProperties().put( COLUMN_COLOR_RENDERER, null );
        setCellRenderer( null );
    }

    @Override
    public void setViewPrefSaver(IViewPrefSaver saver) {
        this.prefSaver = saver;
    }


    /** */
    private String getElementName( )
    {
        if( this.getFieldName() != null && !this.getFieldName().isEmpty() ) {
            return this.getFieldName();
        }
        if( this.getId() != null && !this.getId().isEmpty() ) {
            return this.getId();
        }
        if (!this.getColumns().isEmpty()) {
            return null;
        }
        throw new RuntimeException(FORE.getString("ERROR_LOAD_COLUMN_SETTINGS") + " " + this.getText());
    }


    /** */
    private String getFormName() {

        if (this.getTableView() instanceof JInvTable) {
            if (((JInvTable) this.getTableView()).getController().getViewContext().getFormName() != null
                    && !((JInvTable) this.getTableView()).getController().getViewContext().getFormName().isEmpty()) {

                return ((JInvTable) this.getTableView()).getController().getViewContext().getFormName();
            }
        }
        throw new RuntimeException(FORE.getString("ERROR_LOAD_COLUMN_SETTINGS") + " " + this.getText());
    }

    /** */
    private String getComponentName() {

        if( this.getTableView().getId() != null && !this.getTableView().getId().isEmpty()) {
            return this.getTableView().getId();
        }
        throw new RuntimeException(FORE.getString("ERROR_LOAD_COLUMN_SETTINGS") + " " + this.getText());
    }

    @Override
    public void applyViewPrefs() {

        if( prefSaver == null || isMarkColumn(this) )
            return;

        final TableView<S> table = this.getTableView();

        if( table instanceof JInvTable )
        {
            if (!((JInvTable)table).isEnableColumnManager())
                return;
        }

        try {

            final Iterator< PPrefComponent > prefIter = prefSaver.getInitialPrefs(getComponentName(), getElementName());

            final PPrefComponent p = prefIter.hasNext() ? prefIter.next() : null;

            if( p != null )
            {
                // если ширина уже связана, то пропускаем
                if( !this.prefWidthProperty().isBound() )
                {
                    if( table.getColumnResizePolicy() != TableView.CONSTRAINED_RESIZE_POLICY )
                    {
                        if( p.getWIDTH() <= COLUMN_MIN_WIDTH )
                        {
                            if( ViewPrefAppService.DEBUG_FORM_PARAMETERS) {
                                //logger.info("{}: ignoring saved width {}", getId(), p.getWIDTH());
                            }
                        }
                        else
                        {
                            Long defaultFontWidth = p.getWIDTH();

                            double fontSize = BaseApp.APP().getViewPrefService().getFont().getSize();

                            boolean alreadyApplied = getProperties().getOrDefault( JInvTableColumn.COLUMN_FONT_SIZE_ADJUST, "none" ).equals("stage2");

                            if( !alreadyApplied )
                            {
                                getProperties().put( JInvTableColumn.COLUMN_FONT_SIZE_ADJUST, "stage2" );

                                if( fontSize != ViewPrefAppService.DEFAULT_FONT_SIZE && !isFixedSize() )
                                {
                                    double correctedWidth = defaultFontWidth * ( fontSize / ViewPrefAppService.DEFAULT_FONT_SIZE );
                                    double prefWidth      = correctedWidth  / DIMENSION_FRACTIONAL_FACTOR;
                                    this.setPrefWidth( prefWidth );
                                }
                                else
                                {
                                    this.setPrefWidth( defaultFontWidth / DIMENSION_FRACTIONAL_FACTOR);
                                }
                            }
                        }
                    }
                    //table.refresh();
                }

                this.setVisible( p.getVISIBLE() == 1L );

                if( 1 < 0 && p.getORDBY() != null && p.getORDBY() != -1 && table != null )
                {
                    int totalColumns = table.getColumns().size();

                    if( totalColumns > 0 )
                    {
                        table.getColumns().remove(this);
                        totalColumns = table.getColumns().size();

                        boolean hasMarkColumn = table instanceof JInvTable && ((JInvTable<S>) table).getMarkColumn() != null;

                        if( BaseApp.APP().getViewPrefService().isMarkLeft() && hasMarkColumn )
                        {
                            int colIndex = Math.min( p.getORDBY() + 1, totalColumns );
                            table.getColumns().add( colIndex, this );
//                                logger.info("column '{}' added to index {}", getId(), colIndex);
                        }
                        else
                        {
                            int colIndex = Math.min( p.getORDBY(), totalColumns );
                            table.getColumns().add( colIndex, this );
//                                logger.info("column '{}' added to index {}", getId(), colIndex);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    /**
     * Возвращает Идентификатор группы в F7 фильтре
     */
    public String getIdF7FilterGroup() {

        return (String) getProperties().getOrDefault(F7FILTER_GROUP_ID, null);
    }

    /**
     * Устанавливает Идентификатор группы в F7 фильтре
     */
    public void setIdF7FilterGroup(String idF7FilterGroup) {
        getProperties().put(F7FILTER_GROUP_ID, idF7FilterGroup);
    }

    /**
     * Возвращает порядок следования в группе F7 фильтра
     */
    public Integer getOrderInF7FilterGroup() {

        return (Integer) getProperties().getOrDefault(F7FILTER_ORDER_IN_GROUP, null);
    }

    /**
     * Устанавливает порядок следования в группе F7 фильтра
     */
    public void setOrderInF7FilterGroup(Integer orderInF7FilterGroup) {
        getProperties().put(F7FILTER_ORDER_IN_GROUP, orderInF7FilterGroup);
    }

    @Override
    public void setIndexSearchAllowed(boolean value) {
        getProperties().put(INDEX_SEARCH_ALLOWED, value);
    }

    @Override
    public boolean isIndexSearchAllowed() {
        return (Boolean) getProperties().getOrDefault(INDEX_SEARCH_ALLOWED, Boolean.FALSE);
    }

    /**
     * @return имя класса для LOV в фильтре
     */
    public String getLovClassName() {
        return (String) getProperties().getOrDefault(LOV_CLASS_NAME, null);
    }

    /**
     * Установка имени класса для LOV. Если имя задано происходит автоматическое создание LOV по имени класса в фильтре
     */
    public void setLovClassName(String lovClassName) {
        getProperties().put( LOV_CLASS_NAME, lovClassName);
    }

    /**
     * Возвращает признак проверяемости значения по лову в фильтре, если такой имеется
     */
    public boolean isValidateFromLov() {
        return (Boolean) getProperties().getOrDefault(LOV_VALIDATE_FROM_LOV, Boolean.FALSE);
    }

    /**
     * Устанавливает признак проверяемости значения по лову в фильтре, если такой имеется
     */
    public void setValidateFromLov(boolean validateFromLov) {

        getProperties().put(LOV_VALIDATE_FROM_LOV, validateFromLov);
    }

    public boolean isFixedSize() {
        return !isResizable() || getMinWidth() == getMaxWidth();
    }
}
