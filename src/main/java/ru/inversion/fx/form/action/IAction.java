package ru.inversion.fx.form.action;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.ActionFactory.ActionTypeEnum;
import ru.inversion.icons.IBaseIconDescriptor;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;

/**
 * @author antonovdi
 */
public interface IAction extends EventHandler<ActionEvent> {

    /**
     * Горячая клавиша
     *
     * @return
     */
    default List<KeyCodeCombination> getHotKey() {
        return Collections.<KeyCodeCombination>emptyList();
    }

    /**
     * Стратегия применения прав
     *
     * @return
     */
    default SecurityStrategyEnum getSecurityStrategy() {
        return JInvSecurityService.DEFAULT_SECURITY_STRATEGY;
    }

    /**
     * Код в шрифте иконок d
     *
     * @return
     */
    @Deprecated
    default String getIconCode() {
        return null;
    }

    default IBaseIconDescriptor getIcon() {
        return IconDescriptor.of(FontAwesome.empty);
    }

    /**
     * @return
     */
    default Integer getId() {
        return null;
    }

    /**
     * @return
     */
    default void setId(Integer id) {
    }

    /**
     * Стандартный обработчик
     *
     * @return
     */
    default ActionTypeEnum getActionType() {
        return null;
    }


    /**
     * Всплывающая подсказка
     *
     * @return
     */
    default String getToolTip() {
        return null;
    }

    /**
     * Справочное описание
     *
     * @return
     */
    default String getDescription() {
        return null;
    }

    /**
     * Заголовок
     *
     * @return
     */
    default String getTitle() {
        return null;
    }

    /**
     * @param val
     */
    void setEnabled(boolean val);

    default Optional<BooleanProperty> enabledProperty() {
        return Optional.empty();
    }
    /**
     * @return
     */
    boolean isEnabled();

    /**
     * Разрешен ли запуск системой прав
     *
     * @return
     */
    default boolean isEnabledBySecurity() {
        return true;
    }

    /**
     *
     */
    void handle();

    /**
     * @return
     */
    default IAction getNextAction() {return null;}

    /**
     * @param actionAfter
     */
    default void setNextAction(IAction actionAfter) {;}

    /**
     * Перепроверять ли право при запуске экшна
     */
    default boolean isRecheckSecurity(){
        return getSecurityStrategy() != null && getSecurityStrategy() == SecurityStrategyEnum.ERROR_ON_ACTION;
    }

    default EventType<KeyEvent> getKeyEventType(){
        return KeyEvent.KEY_RELEASED;
    }
}
