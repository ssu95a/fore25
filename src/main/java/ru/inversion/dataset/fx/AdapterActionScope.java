package ru.inversion.dataset.fx;

import javafx.beans.InvalidationListener;
import javafx.collections.MapChangeListener;
import javafx.scene.Node;
import ru.inversion.fore.form.action.ActionMap;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.controls.Controls;

/** Привязывает реестр адаптера к существующему контексту представления. */
public final class AdapterActionScope {
    private final Node view;
    private final Object adapter;
    private final ActionMap actions = new ActionMap();
    private final InvalidationListener placement = ignored -> refresh(false);
    private final MapChangeListener<Object, Object> attachment = change -> {
        if (DSFXAdapter.PROPERTY_DATA_SET_ADAPTER.equals(change.getKey()) && !isAttached()) {
            retire();
        }
    };
    private boolean retired;

    public AdapterActionScope(Node view, Object adapter) {
        this.view = view;
        this.adapter = adapter;
        if (!isAttached()) {
            retire();
            return;
        }
        view.getProperties().addListener(attachment);
        view.sceneProperty().addListener(placement);
        view.parentProperty().addListener(placement);
    }

    public ActionMap actions(boolean createParent) {
        refresh(createParent);
        return retired && !createParent ? null : actions;
    }

    private boolean isAttached() {
        return view.getProperties().get(DSFXAdapter.PROPERTY_DATA_SET_ADAPTER) == adapter;
    }

    private void refresh(boolean createParent) {
        if (retired) {
            return;
        }
        AbstractBaseController<?> owner = view.getScene() == null ? null : Controls.getControllerFromControl(view);
        ActionMap parent = owner == null ? null : createParent ? owner.getActionMap() : owner.peekActionMap();
        if (actions.getParent() != parent) {
            actions.setParent(parent);
        }
    }

    public void retire() {
        if (!retired) {
            retired = true;
            actions.retire();
            view.getProperties().removeListener(attachment);
            view.sceneProperty().removeListener(placement);
            view.parentProperty().removeListener(placement);
        }
    }
}
