package ru.inversion.filesearch;

import ru.inversion.fx.app.AppConstants;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.IAppProperties;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.utils.S;

import java.util.Map;

/**
 * Класс реализует функционал поиска полного пути к файлу.
 * Поиск файлов осуществляется по адресу указанному переменной среды XXI_HOME.
 * Для 2х звенной архитектуры если файл не найден, значение полного пути будет null.
 * Для 3х звенной архитектуры если файл не найден в директории XXI_HOME, файл загружается с сервера.
 *
 * @author perov
 * @version 1.0.0
 */
public class FileSearchService {
    /**
     * Текущий статус
     */
    private static IFileSearchState currentState;

    /**
     * Получаем на вход Map с именами файлов, и заполняем полный путь к файлу
     *
     * @param map ключ имя файла
     * @throws Exception
     */
    public static void getFiles(Map<String, String> map) throws Exception {

        if (currentState == null) {
            IAppProperties ap = BaseApp.APP().getProperties(PropertiesTypeEnum.PRP);
            String middleServer = ap.getStringProperty(AppConstants.MIDDLE_DRV_SERVER);
            if (S.isNullOrEmpty(middleServer))
                setCurrentState(new TwoLayerStateImpl());
            else
                setCurrentState(new ThreeLayerStateImpl(middleServer));
        }

        getCurrentState().fillMap(map);
    }

    /**
     * Получить текущий статус
     *
     * @return
     */
    private static IFileSearchState getCurrentState() {
        return currentState;
    }

    /**
     * Установить текущий статус
     *
     * @param currentState
     */
    private static void setCurrentState(IFileSearchState currentState) {
        FileSearchService.currentState = currentState;
    }

}
