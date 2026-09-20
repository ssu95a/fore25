/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.action.IAction;
import static ru.inversion.fx.form.controls.Controls.CONTROL_FIELD_NAME;

import ru.inversion.fx.form.valid.ValidMan;
import ru.inversion.fx.form.valid.Validator;
import ru.inversion.fx.form.valid.validators.PatternValidator;
import ru.inversion.fx.help.entity.IHelped;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

/**
 * Базовый интерфейс для контролов, который должны реализовывать контролы в рамках фреймворка
 *
 * @author antonovdi
 */
public interface IJInvControl extends IHelped, IReadOnlyControl {

    public static final String CONTROL_VALUE_CHANGED = "ru.inversion.value_changed";

    /**
     * Привязка экземпляра класса {@link Label} к контролу.
     * <p>
     * Используется во многих областях применения фреймворка.
     * Например, именно из привязанного label берется название к полю поиска в фильтре F7,
     * если речь идет о контроле привязанном к {@link IDataSet}.
     * Также label меняет свой внешний вид в случае, если контрол обязателен к заполнению.
     *
     * @param label
     */
    default Control setLabel( Label label ) {

        if( label != null ) {
            label.setLabelFor((Node)this);
        }
        return (Control)this;
    }

    /**
     * Метод получения label, привязанного к контролу
     *
     * @return экземпляр класса {@link Label} привязанный к контролу или null
     */
    default Label getLabel( ) { return (Label) ((Node)this).queryAccessibleAttribute(AccessibleAttribute.LABELED_BY); }

    /**
     * Метод получения свойства связанной текстовой метки
     * @return
     */
    default StringProperty labelTextProperty() {
        return U.callIfNotNull( getLabel(), Label::textProperty );
    }

    /**
     *
     * @param action
     */
    default void setAction(IAction action) {;}

    /**
     * Метод возвращает экземлпяр {@link DSFXAdapter}, если существует связь между контролом и адаптером
     *
     * @param <T>
     * @return экземлпяр {@link DSFXAdapter} или null
     */
    default <T> DSFXAdapter<T> getDataSetAdapter( ) {
        return Controls.getDsAdapterFromControl(this);
    }

    /**
     * Возвращает имя поля, обычно из sql запроса, привязанного к контролу
     *
     * @return имя поля или null
     */
    default String getFieldName() {
        return getProperty( CONTROL_FIELD_NAME );
    }

    /**
     * Устанавливает имя поля, обычно из sql запроса
     *
     * @param fieldName имя поля
     */
    default void setFieldName( String fieldName ) {
        setProperty( CONTROL_FIELD_NAME, fieldName );
    }

    /** */
    default Tooltip getTooltip() { return ((Control)this).getTooltip(); }
    default void setTooltip(Tooltip value) { ((Control)this).setTooltip(value); }

    /**
     * Устанавливает текст для всплывающей подсказки
     *
     * @param toolTipText текст всплывающей подсказки
     */
    default void setToolTipText( String toolTipText ) {

        if( S.isNullOrEmpty( toolTipText ) && getTooltip() == null )
            return;

        if( getTooltip() != null )
            getTooltip().setText( toolTipText );
        else
            setTooltip( new Tooltip(toolTipText) );
    }

    /**
     *
     * @return текст всплывающей подсказки
     */
    default String getToolTipText()
    {
        if( getTooltip() != null && !getTooltip().getText().isEmpty() )
            return getTooltip().getText();
        else
            return null;
    }

    /**
     *
     * @return контроллер, если таковой был привязан к контролу или к его родителям
     */
    default JInvFXFormController<?> getController() {

        if (this instanceof Node) {
            return Controls.getControllerFromControl((Node) this);
        } else {
            return null;
        }
    }

    /**
     *
     * @param <T>
     * @return
     */
    default <T> ReadOnlyObjectProperty<JInvFXFormController<T>> controllerProperty() {
        return this instanceof Node ? Controls.getControllerProperty((Node) this) : null;
    }

    /*
    * @author Shchapov
    *
    * Установить текущий шаблон проверки(см. класс CheckPatterns)
    *
    * @param descr_key идентификатор шаблона проверки
    * */
    default void setCheckPattern(String descr_key) {

        //Установка текущего паттерна в пропертях контрола
        setProperty(ValidMan.CONTROL_CHECK_PATTERN, descr_key);

        //Получение паттерна по ключу
        ValidMan man = getController().getValidMan();
        ObservableList<Validator> list = man.getValidators((Control) this);
        PatternValidator patternValidator = list.stream()
                .filter(e -> e instanceof PatternValidator && ((PatternValidator) e).getDescr_key().equals(descr_key))
                .map(e -> (PatternValidator) e)
                .findFirst().orElse(null);

        if( patternValidator != null ){
            //Обновление текста тултипа
            setToolTipText(patternValidator.getDescription());
        }
    }
}