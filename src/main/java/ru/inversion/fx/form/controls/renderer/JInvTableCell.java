/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls.renderer;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import ru.inversion.fx.form.controls.JInvTableColumn;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_USER_RENDERER;

/**
 *
 * @author antonovdi
 */
public class JInvTableCell<S, T> extends TableCell<S, T> implements IColoredCell<S> {

    protected String mask;

    public JInvTableCell(String mask) {
        super();
        this.mask = mask;
    }

    /** hack */
    protected void super_updateItem(T item, boolean empty) {
        super.updateItem(item, empty); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void updateItem(T item, boolean empty) {

        super.updateItem(item, empty); //To change body of generated methods, choose Tools | Templates.

//        System.out.println("table row" + getTableRow().getIndex());
        setAlignment(Pos.CENTER_LEFT);

        if( item == null || empty )
            setText(null);
        else
            setText(item.toString());

        // Маску вытаскиваем из колонки до вызова метода updateItem детей. Чтобы до момента конвертации маску можно было перекрывать
            JInvTableColumn invColumn = getJInvTableColumn();
            if (invColumn != null
                    && invColumn.getMask() != null
                    && !invColumn.getMask().isEmpty()) {
                mask = invColumn.getMask();
            }
    }

    public final JInvTableColumn<S, T> getJInvTableColumn(){
        TableColumn<S, T> fxColumn = getTableColumn();
        if (fxColumn != null && fxColumn instanceof JInvTableColumn) {
            return (JInvTableColumn<S, T>) fxColumn;
        }
        return null;
    }

    protected BiConsumer<JInvTableCell, Object> getRenderer() {
        return (BiConsumer<JInvTableCell, Object>) getTableColumn().getProperties().getOrDefault(COLUMN_USER_RENDERER, null);
    }

    protected void applyRenderer(Object item, boolean empty) {

        // Вытаскиваем свойства, которые не повлияют на конвертацию значения
        JInvTableColumn invColumn = getJInvTableColumn();
        if (invColumn == null) return;

        Pos alignment = invColumn.getAlignment();
        if (alignment != null && !alignment.equals(ContentTypeManager.ALLGN_DEFAULT)) {
            setAlignment(alignment);
        }
        setId( invColumn.getId() );

        if (getRenderer() != null) {

            if( empty )
            {
                setStyle("");
                setId(null);

                getPseudoClassStates().forEach((PseudoClass t) -> {
                    pseudoClassStateChanged(t, false);
                });
                setTooltip(null);
                setGraphic(null);
            } else
            {
                getRenderer().accept( this, item);
            }
        }
    }

    @Override
    public S getPojo() {
        TableRow tableRow = getTableRow();
        if ( tableRow == null ){
            return null;
        }
        return (S) tableRow.getItem();
    }

    //region Colorizer (copy in JInvTreeTableCell)
    private Map<Function<IColoredCell<S>, Colorizer>, Colorizer> colorMap;

    private Map<Function<IColoredCell<S>, Colorizer>, Colorizer> getColorMap() {
        if ( colorMap == null ){
            colorMap = new LinkedHashMap<>();
        }
        return colorMap;
    }

    @Override
    public void addColor( final Function<IColoredCell<S>, Colorizer> styleExpr ) {
        Colorizer color = styleExpr.apply( this );
//        boolean isDebug = toString().contains( "erh" );
//        String styleBefore = "";
//        String mapBefore = "";
//        if ( isDebug ){
//            styleBefore = getStyle();
//            mapBefore = getColorMap().toString();
//        }

        Map<Function<IColoredCell<S>, Colorizer>, Colorizer> colorMap = getColorMap();
        if ( color != null ){
            colorMap.remove( styleExpr );
            colorMap.put( styleExpr, color );
//            if ( isDebug )
//            logger.info( "{}: added color {}", this, color );
        } else {

            if ( !colorMap.isEmpty() ){
                final Colorizer colorizer = colorMap.get(styleExpr);
                if( colorizer != null )
                {
                    String s = colorizer.toString();
                    removeColorStyle( s );
                    colorMap.remove( styleExpr );
                }

//                if ( isDebug )
//                logger.info( "{}: removed style {}", this, s );
            }
        }
        setStyleInternal();
//        if ( isDebug ){
//            logger.info( "Style |{}|\n" +
//                         "   -> |{}|\n" +
//                         "Map   |{}|\n" +
//                         "   -> |{}|", styleBefore, getStyle(), mapBefore, getColorMap().toString()
//            );
//        }
    }

    private void setStyleInternal(){
        setStyle( getColorStyleString() );
        pseudoClassStateChanged( COLORIZED_BACKGROUND, check( IS_CUSTOM_BACKGROUND ) );
        pseudoClassStateChanged( COLORIZED_BACKGROUND_SECONDARY, check( IS_CUSTOM_BACKGROUND_SECONDARY ) );
        pseudoClassStateChanged( COLORIZED_TEXT, check( IS_CUSTOM_TEXT ) );
        pseudoClassStateChanged( COLORIZED_TEXT_SECONDARY, check( IS_CUSTOM_TEXT_SECONDARY ) );
    }


    private boolean check(Predicate<Colorizer> predicate) {
        return getColorMap().values().stream()
                .anyMatch( predicate );
    }

    private String getColorStyleString() {
        return getColorMap().values().stream()
                .map( Colorizer::toString )
                .collect( Collectors.joining( " " ) );
    }

    @Override
    public void clearColor() {
        Map<Function<IColoredCell<S>, Colorizer>, Colorizer> colorMap = getColorMap();

        if ( !colorMap.isEmpty() ){
            removeColorStyle();
            colorMap.clear();
        }

        pseudoClassStateChanged( COLORIZED_BACKGROUND, check( IS_CUSTOM_BACKGROUND ) );
        pseudoClassStateChanged( COLORIZED_BACKGROUND_SECONDARY, check( IS_CUSTOM_BACKGROUND_SECONDARY ) );
        pseudoClassStateChanged( COLORIZED_TEXT, check( IS_CUSTOM_TEXT ) );
        pseudoClassStateChanged( COLORIZED_TEXT_SECONDARY, check( IS_CUSTOM_TEXT_SECONDARY ) );
    }

    private void removeColorStyle() {
        removeColorStyle( null );
    }
    private void removeColorStyle( String colors ) {
        String style = getStyle();
        if ( ru.inversion.utils.S.isNullOrEmpty( colors ) ){
            colors = getColorStyleString();
        }

        if ( style.contains( colors ) ) {
            style = style.substring( 0, style.indexOf( style ) );
        }
        setStyle( style );
    }
    //endregion
}
