package ru.inversion.fx.form.controls;
import java.util.LinkedList;
import java.util.function.Consumer;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Skinnable;
import ru.inversion.fx.form.controls.skin.ISkinPopulatable;

/**
 @author fomishkin on 28.11.2017. */
public interface IContextMenuAppendable extends Skinnable {
    /**
     Позволяет добавить самодельное контекстное меню к системному

     @param contextMenu
     */
    default void setContextMenuToAppend( final ContextMenu contextMenu ) {
        if ( getSkin() instanceof ISkinPopulatable && contextMenu != null ) {
            if ( ( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().isEmpty() ) {
                ( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().add( new SeparatorMenuItem() );
            }
            //Добавляем в обратном порядке
            new LinkedList<>( contextMenu.getItems() ).descendingIterator().forEachRemaining( item -> {
                if ( !( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().contains( item ) ) {
                    ( (ISkinPopulatable) getSkin() ).getListItemsToPopulateContextMenu().add( item );
                }
            } );
        }
    }
    /**
     Позволяет обработать контекстное меню после его наполнения
     */
    default void onPopulateContextMenu( Consumer<ContextMenu> action ) {
        if ( getSkin() instanceof ISkinPopulatable ) {
            ((ISkinPopulatable) getSkin()).onPopulateContextMenu( action );
        }
    }
}
