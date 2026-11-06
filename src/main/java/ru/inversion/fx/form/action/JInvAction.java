/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.action;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.ThreadPoolManager;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.IFormStateListener;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.U;

import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author antonovdi
 */
public class JInvAction implements IAction {

    protected static Logger logger = LoggerFactory.getLogger(JInvAction.class);
    public static IconDescriptor<?> emptyIcon= IconDescriptor.of(FontAwesome.empty);


    protected ResourceBundle bundle = ResourceBundle.getBundle("fore");
    private List<KeyCodeCombination> hotKey;
    private String iconCode;
    private IBaseIconDescriptor icon; // = IconDescriptor.of(FontAwesome.empty);
    private Integer id;
    private SecurityStrategyEnum secStrategy;
    private ActionFactory.ActionTypeEnum actionType;

    private String toolTip;
    private String description;
    private String title;
    private SimpleBooleanProperty enabledProperty = new SimpleBooleanProperty(true);
    private StringProperty messageProperty = new SimpleStringProperty();
    private StringProperty stateMessageProperty = new SimpleStringProperty();

    private boolean enabledBySecurity = true;
    private EventHandler<ActionEvent> handler;
    private BiConsumer<ActionEvent, IAction> handlerWithNextAction;

    private boolean parallel;
    private IFormStateListener formStateListener;
    private Consumer<Throwable> exceptionHandler;
    private IAction nextAction;
    private boolean forceFxThreadForNextAction;
    private EventType<KeyEvent> keyEventType = KeyEvent.KEY_RELEASED;

    public StringProperty getMessageProperty() {
        return messageProperty;
    }


    /** Устанавливает свойство, на которое будет привязано свойство сообщения в классе Task. */
    public void setMessageProperty(StringProperty messageProperty) {
        this.messageProperty = messageProperty;
    }

    public StringProperty getStateMessageProperty() {
        return stateMessageProperty;
    }

    /** Устанавливает свойство c текстом для отображения на колесе ожидания при параллельных вызовах */
    void setStateMessageProperty( final StringProperty stateMessageProperty ) {
        this.stateMessageProperty = stateMessageProperty;
    }

    public Consumer<Throwable> getExceptionHandler() {
        return exceptionHandler;
    }

    public void setExceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public IFormStateListener getFormStateListener() {
        return formStateListener;
    }

    public void setFormStateListener(IFormStateListener formStateListener) {
        this.formStateListener = formStateListener;
    }

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean paralelel) {
        this.parallel = paralelel;
    }

    public EventHandler<ActionEvent> getHandler() {
        return handler;
    }

    public void setHandler(EventHandler<ActionEvent> handler) {
        this.handler = handler;
    }

    /**
     * Возвращение обработчика. Отличие от стандартного EventHandler<ActionEvent> в том, что в колбека будет приходить экземляр класса Task
     *
     * @return
     */
    public TaskHandler getTaskHandler() {

        if (handler != null && handler instanceof TaskHandler) {
            return (TaskHandler) handler;
        } else {
            return null;
        }
    }

    /**
     * Установка обработчика. Отличие от стандартного EventHandler<ActionEvent> в том, что в колбека будет приходить экземляр класса Task
     *
     * @return
     */
    public void setTaskHandler(TaskHandler handler) {
        this.handler = handler;
    }

    public List<KeyCodeCombination> getHotKey() {
        return hotKey;
    }

    public void setHotKey(List<KeyCodeCombination> hotKey) {
        this.hotKey = hotKey;
    }

    @Deprecated
    @Override
    public String getIconCode() {
        return iconCode;
    }

    @Deprecated
    public void setIconCode(String iconCode) {
        this.iconCode = iconCode;
    }

    @Override
    public boolean isEnabledBySecurity() {
        if (isRecheckSecurity() && getId() != null){
            enabledBySecurity = JInvSecurityService.isCanAccessIsAction(BaseApp.APP().getCommonTaskContext(), getId());
        }
        return enabledBySecurity;
    }

    public void setSecurityStrategy(SecurityStrategyEnum st) {
        this.secStrategy = st;
    }

    @Override
    public SecurityStrategyEnum getSecurityStrategy() {
        if (secStrategy == null) {
            return IAction.super.getSecurityStrategy(); //To change body of generated methods, choose Tools | Templates.
        } else {
            return secStrategy;
        }
    }

    public Integer getId() {
        return id;
    }

    /**
     *
     */
    public void setId(Integer id) {
        setId(id, null);
    }

    /**
     *
     */
    public void setId(Integer id, SecurityStrategyEnum st) {
        this.id = id;
        if (st != null) {
            setSecurityStrategy(st);
        }
        if (id != null && !JInvSecurityService.isCanAccessIsAction(BaseApp.APP().getCommonTaskContext(), id)) {
            enabledBySecurity = false;
        }
    }

    public ActionFactory.ActionTypeEnum getActionType() {
        return actionType;
    }

    public void setActionType(ActionFactory.ActionTypeEnum actionType) {
        this.actionType = actionType;
    }


    public String getToolTip() {
        return toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isEnabled() {
        return enabledProperty.get();
    }

    @Override
    public Optional<BooleanProperty> enabledProperty() {
        return Optional.of(enabledProperty);
    }

    public void setEnabled(boolean enable) {
        enabledProperty.set(enable);
    }

    @Override
    public void handle(ActionEvent event) {
        try {

            //validate
            if (!isEnabled()){
                logger.warn("action {} isn't enabled, aborting handle", this);
                return;
            }

            if ( !isEnabledBySecurity() ){
                throw new SecurityException(String.format(bundle.getString("ERROR_SECURITY_ACTION"), getId()));
            }

            if (isParallel()) {
                if (!handleBeforeParallel(event)) {
                    return;
                }
                Task<Void> task = new JInvTask(event);
                if (messageProperty != null) {
                    messageProperty.bind(task.messageProperty());
                }
                ThreadPoolManager.getInstance().executeTask(task);
            } else if (handlerWithNextAction != null) {
                if (forceFxThreadForNextAction){
                    Platform.runLater(() -> {
                        handlerWithNextAction.accept(event, nextAction);
                    });
                } else {
                    handlerWithNextAction.accept(event, nextAction);
                }
            } else if (handler != null) {
                handler.handle(event);
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void handle() {

        handle(null);
    }

    /**
     * До вызова операции меняем стейт у контроллера
     *
     * @param event
     */
    private boolean handleBeforeParallel(ActionEvent event) {
        logger.trace("handleBefore");

        if (formStateListener == null && event != null) {

            Node node = null;

            if (event instanceof JInvEvent) {
                node = ((JInvEvent) event).getControl();
            }

            if (node == null
                    && event.getSource() instanceof Node) {
                node = (Node) event.getSource();
            }

            if (node != null) {
                JInvFXFormController abc = Controls.getControllerFromControl(node);
                formStateListener = abc;
            }

        }

        if (formStateListener != null) {
            formStateListener.addWaitAction(this);
        }

        return true;
    }

    /**
     * после меняем стейт на дефолтовый
     *
     * @param event
     */
    private void handleAfter(ActionEvent event) {
        logger.trace("handleAfter");

        if (formStateListener != null) {
            formStateListener.removeWaitAction(this);
        }
    }

    private void handleException(Throwable e) {

        if (exceptionHandler != null) {
            exceptionHandler.accept(e);
        } else {
            JInvErrorService.handleException(null, e);
        }
    }

    public EventType<KeyEvent> getKeyEventType() {
        return keyEventType;
    }

    public void setKeyEventType(EventType<KeyEvent> keyEventType) {
        this.keyEventType = keyEventType;
    }

    public class JInvTask extends Task<Void> {

        private final ActionEvent event;

        public JInvTask(ActionEvent event) {
            this.event = event;
        }

        @Override
        protected Void call() throws Exception {

            if (handler instanceof TaskHandler) {
                ((TaskHandler) handler).accept(event, this);

            } else if (handlerWithNextAction != null) {
                if (forceFxThreadForNextAction){
                    Platform.runLater(() -> {
                        handlerWithNextAction.accept(event, nextAction);
                    });
                } else {
                    handlerWithNextAction.accept(event, nextAction);
                }
            } else if (handler != null) {
                handler.handle(event);
            }
            return null;
        }

        @Override
        protected void succeeded() {
            super.succeeded(); //To change body of generated methods, choose Tools | Templates.
            handleAfter(event);
            unbindMessageProperty();
        }

        @Override
        protected void failed() {
            super.failed(); //To change body of generated methods, choose Tools | Templates.
            logger.trace("failed");

            if (this.getException() != null) {
                Platform.runLater(() -> {
                    if (formStateListener != null) {
                        formStateListener.removeWaitAction(JInvAction.this);
                    }
                    handleException(this.getException());
                });
            } else {
                handleAfter(event);
            }
            unbindMessageProperty();
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            handleAfter(event);
            unbindMessageProperty();
        }

        private void unbindMessageProperty() {
            if (messageProperty != null && messageProperty.isBound()) {
                messageProperty.unbind();
            }
        }

        public void setMessage(String message) {
            updateMessage(message);
        }
    }

    public IAction getNextAction() {
        return nextAction;
    }

    public void setNextAction(IAction actionAfter) {
        this.nextAction = actionAfter;
    }

    /**
     * Задать следующее действие
     * @param nextAction следующее действие
     * @param forceFxThread форсировать выполнение следующего действия в fx-потоке или нет
     */
    public void setNextAction(IAction nextAction, boolean forceFxThread) {
        this.nextAction = nextAction;
        this.forceFxThreadForNextAction = forceFxThread;
    }

    public BiConsumer<ActionEvent, IAction> getHandlerWithNextAction() {
        return handlerWithNextAction;
    }

    public void setHandlerWithNextAction(BiConsumer<ActionEvent, IAction> handlerWithNextAction) {
        this.handlerWithNextAction = handlerWithNextAction;
    }

    public IBaseIconDescriptor getIcon() {
        if( icon == null )
        {
            if( getActionType() != null )
                icon = getActionType().getAction().getIcon();

            if( icon == null )
                icon = emptyIcon;
        }
        return icon;
    }
    /** */
    public void setIcon( IBaseIconDescriptor icon ) {
        this.icon = icon;
    }
}