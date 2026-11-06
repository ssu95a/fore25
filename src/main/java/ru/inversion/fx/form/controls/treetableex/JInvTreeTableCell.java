package ru.inversion.fx.form.controls.treetableex;

import javafx.geometry.Pos;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import ru.inversion.fx.form.controls.renderer.Colorizer;
import ru.inversion.fx.form.controls.renderer.IColoredCell;
import ru.inversion.utils.S;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_USER_RENDERER;

/** */
public class JInvTreeTableCell<P,T> extends TreeTableCell<P,T> implements IColoredCell<P> {

    public JInvTreeTableCell( Pos alignment ) {
        setAlignment(alignment);
    }
    public JInvTreeTableCell( ) { }

    protected void superUpdateItem( T item, boolean empty )
    {
        super.updateItem( item, empty );
    }

    @Override
    protected void updateItem(T item, boolean empty) {

        super.updateItem(item, empty);

        if( empty || item == null ) {
            clearCell();
            return;
        }

        setText( getTreeTableColumn().getStringConverter().toString(item) );
        setGraphic(null);

        applyRenderer(item, false);
    }

    /** */
    protected void clearCell() {

        setText(null);
        setGraphic(null);
        setTooltip(null);

        setStyle(S.EMPTY_STRING);

        setId(null);

//        for( PseudoClass pseudoClass : new HashSet<>(getPseudoClassStates()) )
//             pseudoClassStateChanged( pseudoClass, false );

        clearColor();
    }

    /** */
    public JInvTreeTableColumnEx getTreeTableColumn() {
        return (JInvTreeTableColumnEx)this.getTableColumn();
    }

    /** */
    public P getPojo( )
    {
        final TreeTableRow<P> row = getTreeTableRow();

        if( row == null )
            return null;

        return row.getItem();
    }

    protected BiConsumer<JInvTreeTableCell<P,T>, Object> getRenderer() {
        return (BiConsumer<JInvTreeTableCell<P,T>, Object>) getTableColumn().getProperties().getOrDefault(COLUMN_USER_RENDERER, null);
    }

    /** */
    protected void applyRenderer(T item, boolean empty)
    {
        if( empty )
        {
            clearCell();
            return;
        }

        setTooltip(null);
        setStyle(S.EMPTY_STRING);
        setId(null);

        final BiConsumer<JInvTreeTableCell<P,T>, Object> renderer = getRenderer();

        if( renderer != null )
            renderer.accept(this, item);
    }


    private Map<Function<IColoredCell<P>, Colorizer>, Colorizer> colorMap;

    private Map<Function<IColoredCell<P>, Colorizer>, Colorizer> getColorMap() {
        if( colorMap == null )
            colorMap = new LinkedHashMap<>();
        return colorMap;
    }

    @Override
    public void addColor( Function<IColoredCell<P>, Colorizer> styleExpr )
    {
        final Colorizer color = styleExpr.apply(this);

        if( color != null )
            getColorMap().put(styleExpr, color);

        else if( colorMap != null )
        {
            final Colorizer oldColor = colorMap.remove(styleExpr);

            if( oldColor != null )
                removeColorStyle(oldColor.toString());
        }

        setStyleInternal();
    }

    private void setStyleInternal(){
        setStyle( getColorStyleString() );
        pseudoClassStateChanged( COLORIZED_BACKGROUND, check( IS_CUSTOM_BACKGROUND ) );
        pseudoClassStateChanged( COLORIZED_BACKGROUND_SECONDARY, check( IS_CUSTOM_BACKGROUND_SECONDARY ) );
        pseudoClassStateChanged( COLORIZED_TEXT, check( IS_CUSTOM_TEXT ) );
        pseudoClassStateChanged( COLORIZED_TEXT_SECONDARY, check( IS_CUSTOM_TEXT_SECONDARY ) );
    }

    private boolean check(Predicate<Colorizer> predicate)
    {
        if( colorMap == null || colorMap.isEmpty() )
            return false;

        for( Colorizer colorizer : colorMap.values() )
        {
            if( predicate.test(colorizer) )
                return true;
        }

        return false;
    }

    private String getColorStyleString()
    {
        if( colorMap == null || colorMap.isEmpty() )
            return S.EMPTY_STRING;

        return colorMap.values()
                .stream()
                .map(Colorizer::toString)
                .collect(Collectors.joining(" "));
    }

    @Override
    public void clearColor()
    {
        if( colorMap != null && !colorMap.isEmpty() )
        {
            removeColorStyle();
            colorMap.clear();
        }

        pseudoClassStateChanged(
                COLORIZED_BACKGROUND,
                false
        );
        pseudoClassStateChanged(
                COLORIZED_BACKGROUND_SECONDARY,
                false
        );
        pseudoClassStateChanged(
                COLORIZED_TEXT,
                false
        );
        pseudoClassStateChanged(
                COLORIZED_TEXT_SECONDARY,
                false
        );
    }

    private void removeColorStyle() {
        removeColorStyle( null );
    }
    private void removeColorStyle(String colors)
    {
        String style = getStyle();

        if( S.isNullOrEmpty(colors) )
            colors = getColorStyleString();

        if( S.isNullOrEmpty(style) || S.isNullOrEmpty(colors) )
            return;

        final int index = style.indexOf(colors);

        if( index < 0 )
            return;

        style = ( style.substring(0, index) + style.substring(index + colors.length()) ).trim();

        setStyle(style);
    }
}
