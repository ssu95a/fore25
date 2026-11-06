package ru.inversion.fx.app.property;

/**
 *
 * @author ssu
 */
public enum PropertiesTypeEnum {
	PRP,			// из файла Properties или JVM
    SMR,			// из настроек таблицы smr
//    USR,            // из таблицы usr
    DB_USER,		// настройки из БД для пользователя
    DB_GLOBAL,		// настройки из БД для всех
    @Deprecated
    DB_APP_USER,	// настройки для приложения для пользователя
    @Deprecated
    DB_APP_GLOBAL,	// настройки для приложения для всех
    LOCAL_USER,		// локальные настройки для пользователя из реестра
	LOCAL_SYSTEM,	// системные настройки из реестра
    @Deprecated
    LOCAL_APP_USER,	// локальные настройки для приложения, для пользователя из реестра
    @Deprecated
	LOCAL_APP_SYSTEM,// системные настройки для приложения из реестра
    DB_UNIVERSAL,
    @Deprecated
    DB_APP_UNIVERSAL,

    APP_CACHE        // Кэш приложения, с возможностью чтения и записи
}
