/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.ActionFactory.ActionTypeEnum;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.icons.IBaseIconDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 *
 * @author antonovdi
 */
public class ActionBuilder {

    private List<KeyCodeCombination> hotKey = new ArrayList<>();
//    private Object iconId, compatibleIconId;
    private IBaseIconDescriptor icon;
    private Integer id;
    private ActionTypeEnum actionType;
    private String toolTip;
    private String toolTipKeys = "";
    private String description;
    private String title;
    private SecurityStrategyEnum secStrategy = SecurityStrategyEnum.ERROR_ON_ACTION;
    private boolean enable = true;
    private EventHandler<ActionEvent> handler;
    private boolean parallel;
    private IFormStateListener formStateListener;
    private Consumer<Throwable> exceptionHandler;
    private StringProperty messageProperty;
    private StringProperty stateMessageProperty;
    private IAction nextAction;
    private BiConsumer<ActionEvent, IAction> handlerWithNextAction;
    private boolean forceFxThreadForNextAction;
    private EventType<KeyEvent> keyEventType = KeyEvent.KEY_PRESSED;

    public IAction getNextAction() {
        return nextAction;
    }

    public ActionBuilder setNextAction(IAction nextAction) {
        this.nextAction = nextAction;
        return this;
    }
    /**
     * Задать следующее действие
     * @param nextAction следующее действие
     * @param forceFxThread форсировать выполнение следующего действия в fx-потоке или нет
     */
    public ActionBuilder setNextAction(IAction nextAction, boolean forceFxThread) {
        this.nextAction = nextAction;
        this.forceFxThreadForNextAction = forceFxThread;
        return this;
    }

    public BiConsumer<ActionEvent, IAction> getHandlerWithNextAction() {
        return handlerWithNextAction;
    }

    public ActionBuilder setHandlerWithNextAction(BiConsumer<ActionEvent, IAction> handlerWithNextAction) {
        this.handlerWithNextAction = handlerWithNextAction;
        return this;
    }

    public StringProperty getMessageProperty() {
        return messageProperty;
    }

    public ActionBuilder setMessageProperty(StringProperty messageProperty) {
        this.messageProperty = messageProperty;
        return this;
    }

    public StringProperty getStateMessageProperty() {
        return stateMessageProperty;
    }

    public ActionBuilder setStateMessageProperty(StringProperty stateMessageProperty) {
        this.stateMessageProperty = stateMessageProperty;
        return this;
    }

    public IFormStateListener getFormStateListener() {
        return formStateListener;
    }

    public ActionBuilder setFormStateListener(IFormStateListener formStateListener) {
        this.formStateListener = formStateListener;
        return this;
    }

    public boolean isParallel() {
        return parallel;
    }

    public ActionBuilder setParallel(boolean parallel) {
        this.parallel = parallel;
        return this;
    }

    public ActionBuilder icon(IBaseIconDescriptor icon) {
        this.icon = icon;
        return this;
    }

    private JInvAction result = new JInvAction();

    /**
     *
     */
    public ActionBuilder() {

    }

    /**
     *
     */
    public ActionBuilder(ActionTypeEnum actionType) {

    }

    /**
     *
     */
    public ActionBuilder(IAction action) {

    }

    public ActionBuilder(JInvAction action) {
        setActionType(action.getActionType());
        setDescription(action.getDescription());
        setEnable(action.isEnabled());
        setHandler(action.getHandler());
        setListKeyCombination(action.getHotKey());
        setId(action.getId());
        setSecurityStrategy(action.getSecurityStrategy());
        setTitle(action.getTitle());
        setMessageProperty(action.getMessageProperty());
        setToolTip(action.getToolTip());
        setParallel(action.isParallel());
        exceptionHandler(action.getExceptionHandler());
        setHandlerWithNextAction(action.getHandlerWithNextAction());
        setNextAction(action.getNextAction());
        setKeyEventType(action.getKeyEventType());
    }

    public ActionBuilder setListKeyCombination(List<KeyCodeCombination> listKeyCombination) {
        if (listKeyCombination == null) {
            return this;
        }
        this.hotKey = listKeyCombination;
        return setToolTipKeys( prepareKeyComboString( listKeyCombination ) );
    }

    public ActionBuilder setKeyCombination(KeyCodeCombination keyCombination) {
        this.hotKey.add( keyCombination );
        return this; /*setToolTipKeys( prepareKeyComboString( new ArrayList<>( Collections.singletonList(
        keyCombination ) ) ) );*/
    }

    private static String prepareKeyComboString(List<KeyCodeCombination> keys){
        String result = "";
        if( keys == null ) return result;
        for ( KeyCodeCombination key : keys ){
            if( key != null){
                result = " (" + key.getDisplayText() + ")";
            }
        }
        return result;
    }

    public ActionBuilder setId(Integer id) {
        this.id = id;
        return this;
    }

    public ActionBuilder id(Integer id) {
        this.id = id;
        return this;
    }

    public ActionBuilder setKeyEventType(EventType<KeyEvent> keyEventType) {
        this.keyEventType = keyEventType;
        return this;
    }

    public ActionBuilder keyEventType(EventType<KeyEvent> keyEventType) {
        this.keyEventType = keyEventType;
        return this;
    }

    public ActionBuilder setSecurityStrategy(SecurityStrategyEnum st) {
        this.secStrategy = st;
        return this;
    }


    public ActionBuilder securityStrategy(SecurityStrategyEnum st) {
        this.secStrategy = st;
        return this;
    }

    public ActionBuilder setActionType(ActionTypeEnum actionType) {
        this.actionType = actionType;
        return this;
    }


    public ActionBuilder setToolTip(String toolTip) {
        this.toolTip = toolTip;
        return this;
    }

    private ActionBuilder setToolTipKeys( String toolTipKeys ) {
        this.toolTipKeys = toolTipKeys;
        return this;
    }

    public ActionBuilder toolTipText(String toolTip) {
        this.toolTip = toolTip;
        return this;
    }

    public ActionBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public ActionBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public ActionBuilder title(String title) {
        this.title = title;
        return this;
    }

    public ActionBuilder setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public EventHandler<ActionEvent> getHandler() {
        return handler;
    }

    public ActionBuilder setHandler(EventHandler<ActionEvent> handler) {
        this.handler = handler;
        return this;
    }

    public ActionBuilder handler(EventHandler<ActionEvent> handler) {
        this.handler = handler;
        return this;
    }

    public TaskHandler getTaskHandler() {
        if (handler != null && handler instanceof TaskHandler) {
            return (TaskHandler) handler;
        } else {
            return null;
        }
    }

    public ActionBuilder setTaskHandler(TaskHandler handler) {
        return setHandler(handler);
    }

    public ActionBuilder exceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public IAction build() {
        result.setActionType(actionType);
        result.setDescription(description);
        result.setEnabled(enable);
        result.setHandler(handler);
        result.setHotKey(hotKey);
        result.setIcon(icon);
        result.setId(id, secStrategy);
        result.setTitle(title);
        result.setMessageProperty(messageProperty);
        result.setStateMessageProperty(stateMessageProperty);
        if( isOkToAddToolTipKeys() ){
            result.setToolTip( toolTip + toolTipKeys );
        } else {
            result.setToolTip( toolTip );
        }
        result.setParallel(parallel);
        result.setExceptionHandler(exceptionHandler);
        result.setHandlerWithNextAction(handlerWithNextAction);
        result.setNextAction(nextAction, forceFxThreadForNextAction);
        result.setKeyEventType(keyEventType);
        return result;
    }

    private boolean isOkToAddToolTipKeys() {
        return ru.inversion.utils.S.isNotNullOrEmpty( toolTipKeys )
                && ru.inversion.utils.S.isNotNullOrEmpty( toolTip )
                && !toolTip.endsWith( toolTipKeys );
    }
}
