package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

import java.util.LinkedList;
import java.util.List;

/**
 * Меню бар, который инициализируется ядром
 * и подгружает пункты меню из базы, исходя из указанных в FXML mnaMenu и mnbItem
 * подгружаются только пункты, прошедшие проверку прав (таблица xmnb)
 *
 * Пример:
 * <pre>{@code
 *     <JInvMenuBar fx:id="menuBar" mnaMenu="AP_MENU">
 *         <JInvMenu mnbItem="MAIN.STRUCT">
 *             <JInvMenuItem mnbItem="STRUCT.REPORT"/>
 *         </JInvMenu>
 *         <JInvMenu mnbItem="MAIN.ADMIN">
 *             <JInvMenuItem mnbItem="ADMIN.GROUP_TYPE"/>
 *             <JInvMenuItem mnbItem="ADMIN.GROUP_CAT"/>
 *             <JInvMenu mnbItem="ADMIN.ALL">
 *                 <JInvMenuItem mnbItem="ALL.SETUP"/>
 *                 <JInvMenuItem mnbItem="ALL.SPR_USR"/>
 *             </JInvMenu>
 *         </JInvMenu>
 *     </JInvMenuBar>
 * }</pre>
 *
 * @see ru.inversion.xxi.impl.FxmlMenuLoader
 * @see JInvMenu
 * @see JInvMenuItem
 * @author fomishkin
 * @since 22.04.2022
 */
public class JInvMenuBar extends MenuBar implements IJInvControl {
    private final static String INITIALIZED = "ru.inversion.menubar.initialized";

    private final StringProperty mnaMenu = new SimpleStringProperty();

    public String getMnaMenu() {
        return mnaMenu.get() != null ? mnaMenu.get().toUpperCase() : null;
    }

    public void setMnaMenu(String mnaMenu) {
        this.mnaMenu.set(mnaMenu);
    }

    public StringProperty mnaMenuProperty() {
        return mnaMenu;
    }

    public void setInitialized(boolean initialized){
        setProperty(INITIALIZED,initialized);
    }

    public boolean isInitialized(){
        return getProperty(INITIALIZED,false);
    }

    public List<MenuItem> getAllItems(){
        List<MenuItem> menuItems = new LinkedList<>();
        getMenus().forEach(menu -> getMenuItemsRecursive(menu, menuItems));
        return menuItems;
    }

    private void getMenuItemsRecursive(Menu menu, List<MenuItem> resultList){
        resultList.add(menu);
        menu.getItems().forEach(menuItem -> {
            if (menuItem instanceof Menu) {
                getMenuItemsRecursive((Menu) menuItem, resultList);
            } else {
                resultList.add(menuItem);
            }
        });
    }
}
