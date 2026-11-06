package ru.inversion.fx.form.controls;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.utils.S;

import java.util.Optional;
import java.util.ResourceBundle;

/**
 *
 * @author ssu
 */
public class JInvMenuItem extends MenuItem implements IJInvControl, IMnbItem {

    //private final static ResourceBundle bundle = ResourceBundle.getBundle("fore");

    public JInvMenuItem() {
    }
    /** */
    public JInvMenuItem(String text) {
        super(text);
    }
    /** */
    public JInvMenuItem(String text, Node graphic) {
        super(text, graphic);
    }
    /** */
    public JInvMenuItem(String text, Node graphic, Node parentNode) {
        super(text, graphic);
        this.parentNode = parentNode;
    }
    /** */
    public JInvMenuItem(IAction action) {
        setAction(action);
    }

    private Node parentNode;
    private IAction lastAction;
    private final StringProperty mnbItem = new SimpleStringProperty();

    @Override
    public Control setLabel( Label label ) {
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
    public void setAction(IAction action) {
        this.lastAction = action;

        //серим кнопку меню, если привязанный экшн отключают
        action.enabledProperty().ifPresent(actionEnabledProperty -> {

            final boolean disabledOnInit = isDisable();

            actionEnabledProperty.addListener((v,o,n)->{
                if (disabledOnInit){
                    //избегаем включения изначально выключенной кнопки
                    if (!n) setDisable(true);
                } else {
                    setDisable(!n);
                }
            });
        });
        disableProperty().set( !action.isEnabled() );

        internalSetOnAction(action);
    }

    public IAction getLastAction() {
        return lastAction;
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

    /**
     JAVAKERNEL-912 Нужно оборачивать событие в JInvEvent
     @see  JInvButton#internalSetOnAction(IAction)
      */
    private void internalSetOnAction(IAction action)
    {
        SecurityStrategyEnum strategy = action.getSecurityStrategy();

        boolean isEnabledOnInit = action.isEnabledBySecurity();

        if (getId() != null && !isEnabledOnInit){
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
                    //do nothing for ERROR_ON_ACTION, check will be performed in runtime
                    break;
            }
        }

        if( action.getIcon() != null )
            setGraphic( action.getIcon().getLabel() );

        if( S.isNullOrEmpty( getText() ) )
            setText( action.getTitle() );

        setOnAction( event -> {
            JInvEvent wrapEvent = new JInvEvent<>(parentNode == null ? this.getLabel() : parentNode, JInvEvent.PlaceType.FILTER, event);
            wrapEvent.setAction(action);
            action.handle(wrapEvent);
        } );
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
