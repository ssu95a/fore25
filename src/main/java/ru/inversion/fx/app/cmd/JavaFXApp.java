package ru.inversion.fx.app.cmd;

/**
 * Класс для запуска java FX приложений, через внешнюю JVM
 * (пока только под Windows)
 */
public class JavaFXApp extends JavaApp {
    /** */
    public JavaFXApp() {
        this.namedPrefix = "--";
    }

}
