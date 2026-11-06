package ru.inversion.filesearch;

import java.util.Map;

/**
 * 
 * @author perov
 */
public interface IFileSearchState {
    /**
     * Получаем на вход мапу с именами файлов, и заполняем полный путь к файлу
     * @param map
     * @return 
     * @throws java.lang.Exception 
     */
    Map fillMap(Map<String,String> map) throws Exception;
}
