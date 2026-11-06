package ru.inversion.fx.app.service;

import ru.inversion.fx.app.AppException;

import java.util.Collection;
import java.util.Iterator;

/**
 * Интерфейс для накопления сведений об изменяемых свойствах формы (размеров) и сохранения их в момент закрытия
 *
 * @author antonovdi
 */
public interface IViewPrefSaver {

    /**
     * Добавить обьект, содержащий описание визуального свойства, либо заменить при наличие такового
     *
     * @param entry
     */
    void addViewPref(PPrefComponent entry);

    public void addAll(Collection<PPrefComponent> list);

    /**
     * Сохранить коллекцию визуальных свойств в базу
     */
    void save() throws AppException;

    /**
     * Возвращает коллекцию визуальных свойств
     * @return
     */
    Collection<PPrefComponent> getInitialPrefs();

    /**
     * Возвращает коллекцию визуальных свойств
     */
    Iterator<PPrefComponent> getInitialPrefs( String componentFor );

    /**
     * Возвращает коллекцию визуальных свойств для элемента
     */
    Iterator<PPrefComponent> getInitialPrefs( String componentFor, String elementFor );

}
