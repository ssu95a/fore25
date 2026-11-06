/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import ru.inversion.fx.form.lov.AbstractLovBase;
import ru.inversion.fx.form.lov.ILov;

import java.util.function.BiConsumer;

/**
 * Интерфейс для контролов, которые могут в себе содержать LOV
 *
 * @author antonovdi
 */
public interface ILovValueControl {

    /**
     * @return список значений LOV (list of values)
     */
    public AbstractLovBase getLOV();

    /**
     * Устанавливает диалог выбора значений LOV (list of values) на текущее поле
     *
     * @param lovObj список LOV (list of values)
     * @param validateFromLOV признак, регулирующий валидацию текстового поля относительно наличия значения в LOV. Если значение true, то происходит блокировка фокуса, пока значение не будет введено
     * верно или стерто.
     */
    public void setLOV(AbstractLovBase lovObj, boolean validateFromLOV);

    /**
     * {@link #setLOV(ru.inversion.fx.form.lov.AbstractLovBase, boolean) }
     */
    public void setLOV(AbstractLovBase lov);

    public SimpleObjectProperty<AbstractLovBase> lovProperty();

    /**
     * @see#setLovClassName(java.lang.String)
     * @return имя класса для LOV
     */
    public String getLovClassName();

    /**
     * Установка имени класса для LOV. Если имя задано происходит автоматическое создание LOV по имени класса
     *
     * @param lovClassName
     */
    public void setLovClassName(String lovClassName);

    /**
     * Метод задающий признак, регулирующий валидацию текстового поля относительно наличия значения в LOV. Если значение true, то происходит блокировка фокуса, пока значение не будет введено верно или
     * стерто.
     *
     * @param valFromLOV
     */
    public void setValidateFromLOV(boolean valFromLOV);

    /**
     *
     * @return признак валидируемости текстового поля по значениеям в LOV
     */
    public boolean isValidateFromLOV();

    public BooleanProperty validateFromLOVProperty();
    /**
     Активно ли дописывание выбранного содержимого из лова в конец текстового поля
     (вместо перезаписи содержимого этого текстового поля)
     */
    boolean isAppendFromLOV();
    /**
     Вкл/выкл дописывание выбранного содержимого из лова в конец текстового поля
     (вместо перезаписи содержимого этого текстового поля)
     */
    void setAppendFromLOV( boolean appendFromLOV );
    BooleanProperty appendFromLOVProperty();
    /**
     * Метод показывающий диалог Lov, если экземлляр {@link AbstractLovBase} привязан к текущему полю.
     */
    public void showLOV(BiConsumer<Boolean,ILov> clb);
}
