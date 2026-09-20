package ru.inversion.fx.form.controls.skin;
import java.util.function.Consumer;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

/**
 @author fomishkin on 28.11.2017. */
public interface ISkinPopulatable {
    /**
     Возвращает список пунктов меню к добавлению в конец основного контекстного меню
     */
    ObservableList<MenuItem> getListItemsToPopulateContextMenu();
    /**
     Позволяет обработать контекстное меню после его наполнения
     */
    void onPopulateContextMenu( Consumer<ContextMenu> action );
}
