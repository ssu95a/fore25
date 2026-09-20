/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.control.Control;
import ru.inversion.db.entity.Required;
import ru.inversion.fx.form.valid.ReqValidator;

import java.util.function.Function;

/**
 * Интерфейс для компонентов, которые поддерживают валидацию.
 *
 * @author antonovdi
 */
public interface IValidatableControl extends IBaseControl {

    /**
     * Признак обязательности заполнения компонента.
     * Применяется в связке с компонента реализующими интерфейс {@link IValidatableControl}
     */
    public static enum RequiredStateEnum {

        /**
         * Признак обязательного заполнения берется с модели предметной области.
         * В частности из аннотации {@link Required} на get методе связанного с текущим компонентом.
         */
        MODEL,

        /**
         * Компонент обязателен к заполнению
         */
        REQUIRED,

        /**
         * Компонент необязателен к заполнению
         */
        NOT_REQUIRED
    }

    /**
     * Метод устанавливает признак обязательности заполнения компонента.
     *
     * @param state
     */
    default void setRequiredState(RequiredStateEnum state) {
        requiredStateProperty().set(state);
    }

    /**
     * Метод возвращает признак обязательности заполнения компонента.
     *
     * @return {@link RequiredStateEnum} признак обязательности заполения компонента
     */
    default RequiredStateEnum getRequiredState() {
        return requiredStateProperty().get();
    }

    /**
     * Возвращает свойство обязательности заполнения компонента
     *
     * @return Свойство обязательности заполнения компонента
     */
    ObjectProperty<RequiredStateEnum> requiredStateProperty();

    /**
     * Метод устанавливает признак обязательности заполнения компонента.
     *
     * @param val
     */
    default void setRequired(boolean val) {
        setRequiredState( val ? RequiredStateEnum.REQUIRED : RequiredStateEnum.NOT_REQUIRED );
    }

    /**
     * Метод возвращает признак обязательности заполнения компонента.
     *
     * @return Если значение true - компонент обязателен к заполения, если false, то необязателен
     */
    boolean isRequired( );

    /** */
    default ObjectProperty<Function<Control,Boolean>> customReqValidatorProperty( ) {

        ObjectProperty<Function<Control,Boolean>> p = getProperty("r.i.v.r.p");

        if( p == null )
        {
            p = new ObjectPropertyBase< Function< Control, Boolean > >() {
                @Override
                public Object getBean() {
                    return null;
                }
                @Override
                public String getName() {
                    return "customReqValidator";
                }
                @Override
                public void set( Function< Control, Boolean > v )
                {
                    setCustomReqValidator(v);
                }
                public Function< Control, Boolean > get() {
                    return getCustomReqValidator();
                }
            };

            setProperty( "r.i.v.r.p", p );
        }

        return p;
    }

    /** */
    default void setCustomReqValidator( Function <Control,Boolean> vldtr ) {
        getProperties().put( ReqValidator.CUSTOM_CONTROL_VALIDATOR, vldtr );
    }
    default Function <Control,Boolean> getCustomReqValidator( ) {
        return (Function <Control,Boolean>)getProperties().get( ReqValidator.CUSTOM_CONTROL_VALIDATOR );
    }
}
