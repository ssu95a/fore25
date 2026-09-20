package ru.inversion.fx.form.controls.treetableex;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Control;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import ru.inversion.dataset.fx.ICellValueChangeListener;
import ru.inversion.db.entity.ContentTypeEnum;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.service.IViewPrefSaver;
import ru.inversion.fx.app.service.PPrefComponent;
import ru.inversion.fx.app.service.ViewPrefAppService;
import ru.inversion.fx.form.controls.IFilterControl;
import ru.inversion.fx.form.controls.IViewChangeable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.fx.form.controls.renderer.Colorizer;
import ru.inversion.fx.form.controls.renderer.IColoredCell;
import ru.inversion.fx.form.controls.treetableex.cell.JInvTreeTableCell_Boolean;
import ru.inversion.meta.IEntityProperty;
import ru.inversion.utils.S;
import ru.inversion.utils.Tags;
import ru.inversion.utils.U;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.app.service.ViewPrefAppService.DIMENSION_FRACTIONAL_FACTOR;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_GROUP_ID;
import static ru.inversion.fx.form.controls.Controls.F7FILTER_ORDER_IN_GROUP;
import static ru.inversion.fx.form.controls.JInvTableColumn.*;
import static ru.inversion.fx.form.lov.JInvEntityLov.LOV_CLASS_NAME;
import static ru.inversion.fx.form.lov.JInvEntityLov.LOV_VALIDATE_FROM_LOV;

/**
 * @author antonovdi
 */
public class JInvTreeTableColumnEx<P, T> extends TreeTableColumn<P, T> implements IViewChangeable, IFilterControl {

    private final static Logger logger = getLogger(MethodHandles.lookup().lookupClass());

    private static final StringConverter g_defaultStringConverter = new StringConverter() {
        @Override
        public String toString( Object o ) {
            return o == null ? null : o.toString();
        }
        @Override
        public Object fromString( String s ) {
            return s;
        }
    };

    /** */
    private IViewPrefSaver prefSaver;

    /** */
    private Pos alignment = Pos.CENTER_LEFT;

    /** */
    protected IEntityProperty<P, T> entityProperty;

    /** */
    protected StringConverter<T> converter = g_defaultStringConverter;

    /** */
    private final StringProperty toolTipTextProperty = new SimpleStringProperty();

    /** */
    public JInvTreeTableColumnEx() {
        super();
        setEditable(false);
    }

    /** */
    public JInvTreeTableColumnEx( String name) {
        super(name);
        setEditable(false);
    }

    /** */
    public StringConverter<T> getStringConverter()
    {
        return converter;
    }

    /** */
    public void bind( IEntityProperty<P,T> ep, ICellValueChangeListener<P> cellValueChangeListener )
    {
        if( this.entityProperty != null )
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Column '" + getFieldName() + "' already bind" );

        this.entityProperty = Objects.requireNonNull( ep, "'entityProperty' is null" );

        if( ep.getType() == boolean.class || ep.getType() == Boolean.class )
        {
            final TableBooleanObservableValue<P> bval = new TableBooleanObservableValue(ep);
            setCellFactory     ( param -> new JInvTreeTableCell_Boolean());
            setCellValueFactory( param -> (ObservableValue<T>) bval.pojoInstance(param.getValue() == null ? null : param.getValue().getValue()) );
        }
        else
        {
            ContentTypeEnum content = ep.getContent();

            // TODO:
            // stub
//            if( Number.class.isAssignableFrom( ep.getType() ) )
//                setAlignment(Pos.CENTER_RIGHT);

/*
            if( content != null )
            {
                cellFactory = switch (content) {
                    case MONEY -> param -> new BgTableCell_Money();
//                case DATE -> null;
//                case ID -> null;
//                    case CURRENCY -> null;
                    default -> param -> new BgTableCell(content.getAlignment());
                };
            }
            else
            {
                if( ep.getType() == LocalDate.class )
                    cellFactory = param -> new BgTableCell_Date();
                else if( ep.getType() == LocalDateTime.class )
                    cellFactory = param -> new BgTableCell_DateTime();
                else if( ep.getType() == ImageText.class )
                    cellFactory = param -> new BgTableCell_ImageText();
                else
                    cellFactory = param -> new BgTableCell(Pos.CENTER_LEFT);
            }
 */
            Callback<TreeTableColumn<P,T>, TreeTableCell<P,T>> cellFactory = param -> new JInvTreeTableCell<>( getAlignment() );
            final TableObservableValue bval = new TableObservableValue(ep);
            setCellFactory     ( cellFactory );
            setCellValueFactory( param -> bval.pojoInstance( param.getValue() == null ? null : param.getValue().getValue()) );
        }
    }

    /** */
    public Pos getAlignment( ) {
        return alignment;
    }
    public void setAlignment(Pos alignment) {
        this.alignment = alignment;
    }

    /** */
    public Boolean isTransient() {
        return entityProperty == null || entityProperty.isTransient();
    }

    /** */
    public String getFieldName() {
        return (String) getProperties().getOrDefault(COLUMN_FIELD_NAME, S.EMPTY_STRING );
    }
    public void setFieldName(String fieldName) { getProperties().put(COLUMN_FIELD_NAME, fieldName); }

    /** Возвращает Идентификатор группы в F7 фильтре */
    public String getIdF7FilterGroup() {
        return (String) getProperties().getOrDefault(F7FILTER_GROUP_ID, null);
    }

    /** Устанавливает Идентификатор группы в F7 фильтре */
    public void setIdF7FilterGroup(String idF7FilterGroup) {
        getProperties().put(F7FILTER_GROUP_ID, idF7FilterGroup);
    }

    /** Возвращает порядок следования в группе F7 фильтра */
    public Integer getOrderInF7FilterGroup() {
        return (Integer) getProperties().getOrDefault(F7FILTER_ORDER_IN_GROUP, null);
    }

    /** Устанавливает порядок следования в группе F7 фильтра */
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

    @Override
    public void setViewPrefSaver(IViewPrefSaver saver) {
        this.prefSaver = saver;
    }


    /** */
    private class PrefFilter implements Predicate<PPrefComponent> {

        final String componentId;
        final String formName;
        final String elementName;

        {
            componentId = U.nvl( getTreeTableView().getId(), S.EMPTY_STRING );

            if( getTreeTableView() instanceof JInvTreeTableEx)
            {
                final JInvTreeTableEx tex = (JInvTreeTableEx)getTreeTableView();
                formName = U.nvl( tex.getController().getViewContext().getFormName(), S.EMPTY_STRING );
            }
            else
                formName = S.EMPTY_STRING;

            elementName = U.firstNotNull( S.nullIfEmpty(getFieldName()), S.nullIfEmpty(getId()), S.EMPTY_STRING );
        }

        @Override
        public boolean test( PPrefComponent pref ) {
            return pref.getFORM_NAME().equals(formName)
                   &&
                   componentId.equals( pref.getCOMPONENT() )
                   &&
                   elementName.equals(pref.getELEMENT());
        }
    }

    @SuppressWarnings("unchecked")
    private ObservableList<TreeTableColumn<P, ?>> getOwnerColumns( JInvTreeTableEx<P> treeTable )
    {
        final TreeTableColumn<P, ?> parent = (TreeTableColumn<P, ?>) getParentColumn();
        return parent == null ? treeTable.getColumns() : parent.getColumns();
    }

    /** */
    private void applyColumnOrder( JInvTreeTableEx<P> treeTable, PPrefComponent pref )
    {
        final Integer order = pref.getORDBY();

        if( order == null || order < 0 )
            return;

        final ObservableList<TreeTableColumn<P, ?>> columns = getOwnerColumns(treeTable);

        final int currentIndex = columns.indexOf(this);

        if( currentIndex < 0 )
            return;

        columns.remove(currentIndex);

        final int targetIndex = Math.min( order, columns.size() );

        columns.add( targetIndex, this );
    }


    /** */
    @Override
    public void applyViewPrefs( )
    {
        if( prefSaver == null )
            return;

        if( this.<Boolean>getProperty( COLUMN_MARK, false) )
            return;

        if( !getColumns().isEmpty() )
            return;

        final TreeTableView<P> view = getTreeTableView();

        if( !(view instanceof JInvTreeTableEx) )
            return;

        final JInvTreeTableEx<P> treeTable = (JInvTreeTableEx<P>) view;

            try {

                PPrefComponent p = prefSaver.getInitialPrefs().stream().filter( new PrefFilter() ).findFirst().orElse(null);

                if( p == null )
                    return;

                //если ширина уже связана, то пропускаем
                if( !this.prefWidthProperty().isBound() )
                {
                    if ( treeTable.getColumnResizePolicy() != JInvTreeTableEx.CONSTRAINED_RESIZE_POLICY )
                    {
                        if( p.getWIDTH() <= 0 )
                            this.setPrefWidth(Control.USE_PREF_SIZE);

                        Long defaultFontWidth = p.getWIDTH();

                        double fontSize = BaseApp.APP().getViewPrefService().getFont().getSize();

                        if( fontSize != ViewPrefAppService.DEFAULT_FONT_SIZE && !getProperties().containsKey(JInvTableColumn.COLUMN_FONT_SIZE_ADJUST))
                        {
                            getProperties().put(JInvTableColumn.COLUMN_FONT_SIZE_ADJUST, true);
                            double correctedWidth = defaultFontWidth * (fontSize / ViewPrefAppService.DEFAULT_FONT_SIZE);
                            if (ViewPrefAppService.DEBUG_FORM_PARAMETERS){
                                logger.info("{}: corrected width {} -> {}",getId(), defaultFontWidth, correctedWidth);
                            }
                            this.setPrefWidth(correctedWidth / DIMENSION_FRACTIONAL_FACTOR);
                        } else {
                            this.setPrefWidth(defaultFontWidth / DIMENSION_FRACTIONAL_FACTOR);
                        }
                    }
                    treeTable.refresh();
                }

                this.setVisible( p.getVISIBLE() == 1L );

                applyColumnOrder( treeTable, p );

            } catch (Exception ex) {
                JInvErrorService.handleException(null, ex);
            }
    }


    private BiConsumer<JInvTreeTableCell<P, T>, T> userRenderer;
    private boolean colorCleanupRequired;

    void setCellRenderer( BiConsumer<JInvTreeTableCell<P, T>, T> renderer )
    {
        userRenderer = renderer;
        rebuildCellRenderer();
    }

    @SuppressWarnings("unchecked")
    private void rebuildCellRenderer()
    {
        BiConsumer<JInvTreeTableCell<P, T>, T> renderer = null;

        if( colorCleanupRequired )
            renderer = (cell, value) -> cell.clearColor();

        if( userRenderer != null )
        {
            renderer = renderer == null ? userRenderer : renderer.andThen(userRenderer);
        }

        final BiConsumer<JInvTreeTableCell<P, T>, T> colorRenderer =
                (BiConsumer<JInvTreeTableCell<P, T>, T>)
                        getProperties().get(COLUMN_COLOR_RENDERER);

        if( colorRenderer != null )
        {
            renderer = renderer == null ? colorRenderer : renderer.andThen(colorRenderer);
        }

        if( renderer == null )
            getProperties().remove(COLUMN_USER_RENDERER);
        else
            getProperties().put(COLUMN_USER_RENDERER, renderer);
    }


    /**
     Добавить правило раскраски для определённой колонки
     */
    public void addColor( Function<IColoredCell<P>, Colorizer> styleExpr )
    {
        BiConsumer<JInvTreeTableCell<P, T>, T> colorRenderer = (BiConsumer<JInvTreeTableCell<P, T>, T>)getProperties().get(COLUMN_COLOR_RENDERER);

        final BiConsumer<JInvTreeTableCell<P, T>, T> newColorizer = (cell, value) -> cell.addColor(styleExpr);

        colorRenderer = colorRenderer == null ? newColorizer : colorRenderer.andThen(newColorizer);

        getProperties().put( COLUMN_COLOR_RENDERER, colorRenderer );

        rebuildCellRenderer();
    }

    // Убрать всю раскраску, заданную через addColor() в данном столбце
    public void clearColor()
    {
        getProperties().remove(COLUMN_COLOR_RENDERER);

        colorCleanupRequired = true;
        rebuildCellRenderer();

        if( getTreeTableView() != null )
            getTreeTableView().refresh();
    }

    final public String getToolTipText() {
        return toolTipTextProperty.get();
    }
    final public void setToolTipText( String t ) {
        toolTipTextProperty.set(t);
    }
    public StringProperty toolTipTextProperty() {
        return toolTipTextProperty;
    }
}
