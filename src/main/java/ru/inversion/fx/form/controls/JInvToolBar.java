/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Skin;
import javafx.scene.control.ToolBar;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.ActionFactory.ActionTypeEnum;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.controls.skin.JInvToolBarSkin;

/**
 *
 * @author antonovdi
 */
public class JInvToolBar extends ToolBar {

    private Boolean multipleUse = Boolean.FALSE;
    private BiConsumer<ActionEvent, IAction> beforeActionHandler;

    public Boolean getMultipleUse() {
        return multipleUse;
    }

    public void setMultipleUse(Boolean multipleUse) {
        this.multipleUse = multipleUse;
    }

    public JInvToolBar() {
        super();
        init();
    }

    public JInvToolBar(Node... items) {
        super(items);
        init();
    }

    private void init(){
        getItems().addListener( ( ListChangeListener.Change<? extends Node> change) -> {
            while (change.next()) {
                if ( change.wasRemoved() ) {
                    change.getRemoved().forEach( removed -> removed.visibleProperty().unbindBidirectional( removed.managedProperty() ) );
                }
                if ( change.wasAdded() ){
                    change.getAddedSubList().forEach( added -> added.visibleProperty().bindBidirectional( added.managedProperty() ) );
                }
            }
        } );
    }

    /** */
    public void setStandartActions( ActionTypeEnum ... types )
    {
        if( types == null || types.length == 0 )
            return;

        if( types.length == 1 )
            getItems().add( types[0] == null ? new Separator() : ActionFactory.createButton( types[0], null) );
        else
        {
            List<Node> items = new ArrayList<>(types.length);

            for( ActionTypeEnum type : types )
            {
                if( type == null )
                    items.add( new Separator());
                else
                    items.add( ActionFactory.createButton(type, null));
            }
            getItems().addAll( items );
        }
    }

    public void update() {
        super.layoutChildren();
    }

    public BiConsumer<ActionEvent, IAction> getBeforeActionHandler() {
        return beforeActionHandler;
    }

    public void setBeforeActionHandler(BiConsumer<ActionEvent, IAction> beforeActionHandler) {
        this.beforeActionHandler = beforeActionHandler;
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new JInvToolBarSkin(this);
    }
}
