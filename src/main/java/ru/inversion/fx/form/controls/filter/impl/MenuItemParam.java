package ru.inversion.fx.form.controls.filter.impl;

import javafx.scene.control.MenuItem;

/**
 *
 * @author perov
 */
class MenuItemParam extends MenuItem {

    public MenuItemParam(String text) {
        super(text,null);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        MenuItemParam menuItemParam = (MenuItemParam) obj;
        return menuItemParam.getText().equals(getText());
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.getText().hashCode();
        return result;
    }

}
