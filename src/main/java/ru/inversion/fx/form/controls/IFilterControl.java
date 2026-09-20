/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.form.controls;
import javafx.util.Pair;

import java.util.List;
import java.util.function.Supplier;

/**
 * Компонент используемы в диалоге фильтра
 * <p>
 * @author antonovdi
 *         Sulimoff
 */
public interface IFilterControl extends IBaseControl {

    public static final String INDEX_SEARCH_ALLOWED = "INDEX_SEARCH_ALLOWED";

    /**
     * Возвращает Идентификатор группы в F7 фильтре
     */
    public String getIdF7FilterGroup();

    /**
     * Устанавливает Идентификатор группы в F7 фильтре
     */
    public void setIdF7FilterGroup(String idF7FilterGroup);

    /**
     * Возвращает порядок следования в группе F7 фильтра
     *
     * @return
     */
    public Integer getOrderInF7FilterGroup();

    /**
     * Устанавливает порядок следования в группе F7 фильтра
     *
     * @param orderInF7FilterGroup
     */
    public void setOrderInF7FilterGroup(Integer orderInF7FilterGroup);

    /**
     * Устанавливает признак того, что поиск по индексу допустим
     */
    public void setIndexSearchAllowed(boolean value);

    /**
     * Возвращает признак того, что поиск по индексу допустим
     */
    public boolean isIndexSearchAllowed();

    /** */
    public default Supplier<List<Pair<?, String>>> getFactoryList( ) {
        return (Supplier<List<Pair<?, String>>>) getProperty("ru.inversion.factory_list");
    }

    /** */
    public default void setFactoryList( Supplier<List<Pair<?, String>>> f ) {
        setProperty( "ru.inversion.factory_list", f );
    }

    /** Возвращает заголовок для компонента в фильтре */
    public default String getF7Title() {
        return getProperty("ru.inversion.f7_title");
    }

    /** Устанавливает заголовок для компонента в фильтре */
    public default void setF7Title( String title ) {
        setProperty("ru.inversion.f7_title", title);
    }

}
