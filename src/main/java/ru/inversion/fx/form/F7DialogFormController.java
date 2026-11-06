/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.fx.F7FilterGroup;
import ru.inversion.dataset.fx.F7FilterItem;
import ru.inversion.fx.form.controls.JInvComboBox;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author psh
 */
public class F7DialogFormController extends JInvFXFormController<DSFXAdapter<?>>
{
    @FXML private ScrollPane pane;
    @FXML private JInvComboBox find;

    private F7FilterDialog f7FilterDialog;

    @Override
    protected void init () throws Exception
    {
        setTitle (getBundleString ("TITLE"));

        f7FilterDialog = new F7FilterDialog (this);
        pane.setContent (f7FilterDialog);

        f7FilterDialog.initKeyboard (pane);
        f7FilterDialog.initSearchControl (find);

        Node first = f7FilterDialog.getControls().values().iterator ().next ();
        Platform.runLater (first::requestFocus);
    }

    public Map<F7FilterGroup, Set<F7FilterItem>> getMapGroupItems () {
        return f7FilterDialog.getMapGroupItems ();
    }

    public Node getControl ( String columnName ) {
        return f7FilterDialog.getControls ().get (columnName);
    }

//    public Node getExpressionControl ( String columnName ) {
//        return f7FilterDialog.getExpressionControls ().get (columnName);
//    }

    @Override
    protected boolean onOK ()
    {
        return f7FilterDialog.onOk ();
    }
}
