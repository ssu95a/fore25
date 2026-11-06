package ru.inversion.fx.app.cmd;

import ru.inversion.utils.S;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Класс для запуска java приложений, упакованных в jar файл
 * (пока только под Windows)
 */
public class JavaApp extends OSCmd {

    /** Параметры, которые передаются в JVM*/
    protected String jvmOptions;

    /** Класс который необходимо запустить */
    protected String mainClass;

    /** Имя */
    protected String jarName;

    /** */
    protected String classPath;

    /** */
    protected boolean guiMode = true;

    /** */
    public JavaApp() { }

    /**
     * Устанавливает конкретную JVM для запуска,
     * если не будет установлено - то берется текущая
     */
    public JavaApp useJvm( String javaExe ) {
        super.command(javaExe);
        return this;
    }

    /** */
    public JavaApp guiMode( boolean guiMode ) {
        this.guiMode = guiMode;
        return this;
    }

    /** */
    private void useCurrentJvm( ) {
        String javaHome = System.getProperty("java.home");
        String javaExe  = javaHome + File.separator + "bin" + File.separator + ( guiMode ? "javaw.exe" : "java.exe" );
        this.waitFor(!guiMode);
        super.command(javaExe);
    }

    /** */
    public JavaApp classPath( String classPath ) {
        this.classPath = classPath;
        return this;
    }

    /** */
    @Override
    public CmdTypeEnum getType() {
        return CmdTypeEnum.JAVA_APP;
    }

    /** Имя JAR */
    public JavaApp jar( String jarName )
    {
        this.jarName = jarName;
        return this;
    }

    /** Главный класс, необязательно */
    public JavaApp mainClass( String mainClass )
    {
        this.mainClass = mainClass;
        return this;
    }

    /** Параметры JVM, необязательны */
    public JavaApp jvmOptions( String jvmOptions )
    {
        this.jvmOptions = jvmOptions;
        return this;
    }

    /** Режим запуска */
    private enum RunMode {
        JAR,       // только jar, стартует класс из манифеста, - java -jar app.jar
        JAR_CLASS, // из jar стартует указанный класс, java -cp app.jar MainClass
        CLASS      // только класс, а откуда он пускается, ищется в classpath, java -cp "..." MainClass
    }


    /** */
    private RunMode runMode( )
    {
        if( !S.isNullOrEmpty(jarName) )
        {
            if (S.isNullOrEmpty(mainClass))
            {
                // Только JAR - используем Main-Class из манифеста
                return RunMode.JAR;
            }
            else
            {
                // JAR + класс - запускаем конкретный класс из JAR
                return RunMode.JAR_CLASS;
            }
        }
        else if (!S.isNullOrEmpty(mainClass))
        {
            // Только класс - нужен classpath
            return RunMode.CLASS;
        }
        throw new IllegalStateException("Cannot determine run mode. Incorrect set ");
    }


    /** Строит и нормализует classpath для JAR_CLASS режима */
    private String buildAndNormalizeClassPath()
    {
        if (S.isNullOrEmpty(classPath))
            return jarName;


        // Простая нормализация: убираем мусор и заключаем в кавычки
        String combined = jarName + File.pathSeparator + classPath;
        return normalize(combined);
    }

    /**  Основной метод нормализации - вызывается при добавлении в cmdItems */
    private String normalize(String classPath) {
        if (S.isNullOrEmpty(classPath)) {
            return classPath;
        }

        String sep = File.pathSeparator;
        String escapedSep = sep.equals(";") ? ";" : Pattern.quote(sep);

        // Разбиваем, чистим, собираем
        return Arrays.stream(classPath.split(escapedSep))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::autoQuote)
                .collect(Collectors.joining(sep));
    }

    /**  Автоматически добавляет кавычки если нужно */
    private String autoQuote(String path) {
        if (path.contains(" ") && !(path.startsWith("\"") && path.endsWith("\""))) {
            return "\"" + path + "\"";
        }
        return path;
    }
    /** */
    @Override
    protected void onPrepare( PrepEnum prep, boolean after, List<String> cmdItems ) {

        if( prep == PrepEnum.CMD && !after )
        {
            if( S.isNullOrEmpty(this.command) )
                useCurrentJvm( );
        }

        if( prep == PrepEnum.CMD && after )
        {
            if( !S.isNullOrEmpty(jvmOptions) )
                // Разбиваем строку опций на отдельные аргументы
                Arrays.stream(jvmOptions.split("\\s+")).map(String::trim).filter(S::isNotNullOrEmpty).forEach( cmdItems::add );

            switch( runMode() ) {
                case JAR:
                    // java -jar app.jar
                    cmdItems.add("-jar" );
                    cmdItems.add(jarName);
                break;
                case JAR_CLASS:
                    cmdItems.add("-cp");
                    String cp1 = buildAndNormalizeClassPath();
                    cmdItems.add(cp1);
                    cmdItems.add(mainClass);
                    break;
                case CLASS:
                    if(!S.isNullOrEmpty(classPath) )
                    {
                        cmdItems.add("-cp");
                        String cp2 = normalize(classPath);
                        cmdItems.add(cp2);
                    }
                    cmdItems.add(mainClass);
                break;
            }

        }
    }
}
