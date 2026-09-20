package ru.inversion.fx.app;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import ru.inversion.utils.Pair;
import ru.inversion.fx.app.frame.menu.IMenuLoader;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.tc.TaskContext;

/**
 * Интерфейс для подключения ф-ционал из БД
 *
 * @author ssu @
 */
public interface IDBTools {

    /**
     * Получить свойство для пользователя
     *
     * @param userName - логин пользователя, если null то подставляется текущий
     */
//	String getPreference( TaskContext tc, String preference, String defValue, String userName );
    /**
     *
     */
//	String getGlobalPreference( TaskContext tc, String preference, String defValue );
    /**
     * Установить свойство для пользователя
     *
     * @param userName - логин пользователя, если null то подставляется текущий
     */
//	void setPreference( TaskContext tc, String preference, String value, String userName );
    /**
     *
     */
//	void setGlobalPreference( TaskContext tc, String preference, String value );
    /**
     *
     */
    IMenuLoader getMenuLoader(TaskContext tc, String menuID);

    /**
     *
     * @param tc
     * @param properties
     */
    void getAppProperties(TaskContext tc, Properties properties);

    /** */
    String getPreference(TaskContext tc, PropertiesTypeEnum type, String preference, String defValue);

    /**
     *
     * @param tc
     * @param type
     * @param property
     * @param value
     */
    void setPreference(TaskContext tc, PropertiesTypeEnum type, String property, String value);

    /**
     * Групповое сохранение
     * @param tc
     * @param map
     */
    void setPreference(TaskContext tc, Map<PropertiesTypeEnum, List<Pair<String,String>>> map);

    /**
     * Инициализируем свойства приложения при инициализации и кладет их в кеш. ВАЖНО: Не для прикладных программистов.
     * @param prefNames
     */
    Map<PropertiesTypeEnum, Properties> initPreferences(
            TaskContext tc,
            Map<PropertiesTypeEnum, List<String>> prefNames);
    /**
     * Обновляет кеш свойств приложения при инициализации . ВАЖНО: Не для прикладных программистов.
     * @param tc
     */
    void refreshAppPreferenceCache(TaskContext tc);

}
