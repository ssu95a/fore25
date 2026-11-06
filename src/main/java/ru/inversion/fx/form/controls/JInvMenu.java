package ru.inversion.fx.form.controls;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Menu;

/**
 * Пункт меню, содержащий подпункты
 * @author fomishkin
 * @since 22.04.2022
 */
public class JInvMenu extends Menu implements IMnbItem {
    private final StringProperty mnbItem = new SimpleStringProperty();

    public JInvMenu() {
    }

    public JInvMenu(String name) {
        super(name);
    }

    public String getMnbItem() {
        return mnbItem.get() != null ? mnbItem.get().toUpperCase() : null;
    }

    public StringProperty mnbItemProperty() {
        return mnbItem;
    }

    public void setMnbItem(String mnbItem) {
        this.mnbItem.set(mnbItem);
    }
}
