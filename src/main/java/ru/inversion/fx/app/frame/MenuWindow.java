package ru.inversion.fx.app.frame;

import javafx.scene.control.MenuBar;

import java.util.ResourceBundle;

public abstract class MenuWindow {

    public abstract String getMenuID();

    public String getTitle() {
        return null;
    }

    public ResourceBundle getResourceBundle() {
        return null;
    }

    protected void afterCreateMenu(MenuBar menubar) {}

    public final void show() {
        final String title = initTitle();
        final JInvMainFrame frame = new JInvMainFrame(this::afterCreateMenu, getMenuID(), title, this.getClass());
        final Boolean minimizeFx = Boolean.valueOf(System.getProperty("fx_minimize_on_start", "false"));
        if (minimizeFx) {
            frame.setIconified(true);
            System.setProperty("fx_minimize_on_start", "false");
        }
        frame.show();
    }

    private String initTitle() {
        String title;
        if (getTitle() != null) {
            title = getTitle();
        } else if (getResourceBundle() != null && getResourceBundle().containsKey("APP_NAME")) {
            title = getResourceBundle().getString("APP_NAME");
        } else {
            title = "No title";
        }
        return title;
    }

}
