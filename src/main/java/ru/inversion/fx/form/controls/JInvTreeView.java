package ru.inversion.fx.form.controls;

import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/** */
public class JInvTreeView<T> extends TreeView<T> implements IJInvControl {
    /**
     */
    public JInvTreeView() {
    }

    /**
     */
    public JInvTreeView( TreeItem< T > root ) {
        super(root);

    }
}
