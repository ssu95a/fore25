package ru.inversion.fx.app.cmd;

/** */
public enum CmdTypeEnum {
    // Запуск Java приложения в отдельной JVM
    JAVA_APP,

    // Запуск Java модуля в той же JVM
    JAVA_MODULE,

    // Запуск XXI fore style модуля
    FORE_FX_MODULE,

    // Запуск ORACLE FORMS модуля
    FMX,

    // Команда операционной системы
    OS_COMMAND,

    // Переход по ссылке (чтобы это не значило)
    URL,

    // Отчет альтернативной печати
    ALT_PRINT_REPORT,

    // Команда на выход из приложения
    EXIT,

    // Вызов action из бикомпа
    BICOMP_ACTION
}
