/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.mdi;

import javafx.stage.Window;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.form.ViewContext;

import java.util.List;

/**
 *
 * @author antonovdi
 */
public interface IWindowManager {

    void addWindow(String title, JInvWindowMdi window);

    void removeWindow( Window window);

    public List< JInvWindowMdi > getWindows();

    void selectWindow(Window window);

    void tileWindows();

    void cascadeWindows();

    void positionWindow( JInvWindowMdi window, ViewContext vc) throws AppException;
}
