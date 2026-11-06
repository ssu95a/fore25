/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ru.inversion.fx.form.controls.sheet;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import org.controlsfx.control.PropertySheet;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Optional;

/**
 *
 * @author antonovdi
 */
public class JInvPropertySheet extends PropertySheet {

    //private Comparator<? super String> categoryComparator;
    private Comparator<? super String> pageComparator;
    private boolean showPages;
    //private Set<String> pages;
    private final SimpleStringProperty pageProperty = new SimpleStringProperty(this, "page", "");

    public JInvPropertySheet(boolean showPages) {
        super();
        setPropertyEditorFactory( new JInvPropertyEditorFactory() );
        this.showPages = showPages;
    }
    public JInvPropertySheet() {
        this(false);
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JInvPropertySheetSkin(this);
    }

//    public void setCategoryComparator(Comparator<? super String> comparator){
//        this.categoryComparator = comparator;
//    }
//    public Comparator<? super String> getCategoryComparator() {
//        return categoryComparator;
//    }
    /*
    public void setPageComparator(Comparator<? super String> comparator){
        this.pageComparator = comparator;
    }
    public Comparator<? super String> getPageComparator() {
        return pageComparator;
    }
    */

    public void refresh() {
        getProperties().put( "REFRESH", Boolean.TRUE );
    }

    public boolean isShowPages() {
        return showPages;
    }

    public final SimpleStringProperty pageProperty() {
        return this.pageProperty;
    }

    public Optional<Node> getEditorForItem(Item item){
        if ( !getItems().contains( item ) ){
            return Optional.empty();
        }
        JInvPropertySheetSkin skin = (JInvPropertySheetSkin) getSkin();
        if ( skin == null ){
            return Optional.empty();
        }
        HashMap<Item, Node> itemEditorMap = skin.getItemEditorMap();
        if ( !itemEditorMap.containsKey( item ) ){
            return Optional.empty();
        }

        return Optional.of( itemEditorMap.get( item ) );
    }

}
