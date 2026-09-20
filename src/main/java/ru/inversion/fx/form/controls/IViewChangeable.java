/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;

import ru.inversion.fx.app.service.IViewPrefSaver;

/**
 * Интерфейс для компонента способного сообщать об измененях визуальных свойств (размеров) и применять сохраненные свойства к себе
 *
 * @author antonovdi
 */
public interface IViewChangeable {

    /**
     * Передает обьект, отвечающий за хранение размеров. См {@link IViewPrefSaver}
     * @param saver 
     */
    void setViewPrefSaver(IViewPrefSaver saver);

    /**
     * Заставляет компонент применять к себе визуальные настройки
     * @param prefs 
     */
    void applyViewPrefs();
}
