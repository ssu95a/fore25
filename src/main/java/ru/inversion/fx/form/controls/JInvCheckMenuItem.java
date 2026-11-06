package ru.inversion.fx.form.controls;

import java.util.Optional;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvEvent;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author bayurov
 */
public class JInvCheckMenuItem extends CheckMenuItem implements IJInvControl, IMnbItem {

    //private Node parentNode;

    private final StringProperty mnbItem = new SimpleStringProperty();

    public JInvCheckMenuItem() {
    }

    public JInvCheckMenuItem(String text) {
        super(text);
    }

    public JInvCheckMenuItem(String text, Node graphic) {
        super(text, graphic);
    }

    @Override
    public Control setLabel(Label label) {
        return null;
    }

    @Override
    public Label getLabel() {
        return null;
    }

    @Override
    public StringProperty labelTextProperty() {
        return this.textProperty();
    }

    @Override
    public <T> DSFXAdapter<T> getDataSetAdapter() {
        return null;
    }

    @Override
    public String getFieldName() {
        return null;
    }
    @Override
    public void setFieldName(String fieldName) {
    }

    @Override
    public Tooltip getTooltip() {
        return null;
    }
    @Override
    public void setTooltip(Tooltip value) {
    }
    @Override
    public void setToolTipText(String toolTipText) {
    }
    @Override
    public String getToolTipText() {
        return null;
    }

    /*
    private void internalSetOnAction(IAction action) {
        SecurityStrategyEnum strategy = action.getSecurityStrategy();

        boolean isEnabledOnInit = action.isEnabledBySecurity();

        if (getId() != null && !isEnabledOnInit) {
            switch (strategy) {
                case DISABLE: {
                    setDisable(true);
                    break;
                }
                case HIDE: {
                    setVisible(false);
                    break;
                }
                default:
                    break;
            }
        }

        if (action.getIcon() != null) {
            setGraphic(action.getIcon().getLabel());
        }

        setOnAction(event -> {
            JInvEvent wrapEvent = new JInvEvent<>(parentNode == null ? this.getLabel() : parentNode, JInvEvent.PlaceType.FILTER, event);
            wrapEvent.setAction(action);
            action.handle(wrapEvent);
        });
    }
    */
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
