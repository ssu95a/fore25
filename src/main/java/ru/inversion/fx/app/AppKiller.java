package ru.inversion.fx.app;

import javafx.application.Platform;
import ru.inversion.fx.service.module.ModuleService;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.*;
import java.time.*;
import java.util.concurrent.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static ru.inversion.fx.app.AppConstants.PRP_CHECK_XXI_UPGRADE_DISABLE;
import static ru.inversion.fx.app.property.PropertiesTypeEnum.PRP;

/**
 * Объект предназначенный для слежения за стоп-файлом
 * <p>
 * Проверяет наличие файла в момент инициализации.
 * Запускает {@link WatchService} и следит за изменением или созданием файла.
 * <p>
 * Стоп-файл должен содержать в себе дату (с какого момента он создан) и время ожидания (в секундах)
 * Ex: 2017-05-29T17:46:00_120
 * Если стоп-файл ничего в себе не содержит - значит файл без окончания срока действия и приложение закроется
 *
 * @author banin,
 *         Sulimoff
 */
public final class AppKiller {

    //флаг, для помощи именования потока
    private static boolean name_thread_mode = true;

    public final static String STOP_FILE = "ru.inversion.stop";
    
    /** Папка откуда запускается приложение */
    private static final Path WORKING_DIR;

    static {
        
        Path path;

        String userDir = System.getProperty("user.dir");

        try {

            final File jarFile = ModuleService.getJarFile(AppKiller.class);
            if( jarFile != null )
                userDir = jarFile.getParentFile().getAbsolutePath();

            path = Paths.get( System.getProperty("ru.inversion.stop_file_path", userDir ) );
        }
        catch( Throwable th ) {
            path = Paths.get( System.getProperty("user.dir") );
        }
        
        WORKING_DIR = path;
    }
    
    /** Стоп-файл */
    private final Path stopFile = WORKING_DIR.resolve(STOP_FILE);

    /** Фабрика для потоков, создаем демонов!*/
    static private final ThreadFactory threadFactory = r -> newThread(
        name_thread_mode ? "XXI Application killer thread" : "check_Xxi_Upgrade thread",
        r
    );

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor( threadFactory );

    private WatchService watchService;

    public AppKiller() {

        BaseApp.appLog.info( Tags.PRODUCT_LABEL + "AppKiller run.\n stop_file_path = " + WORKING_DIR.toString() + "\n waiting for '" + STOP_FILE  + "' ..." );
        
        // Перед инициализацией watch service проверяем наличие стоп-файла
        checkStopFile();

        name_thread_mode = true;

        // Инициализируем watch service
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            WORKING_DIR.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException ignored) {
        }

        // Запускаем watch service
        threadPool.execute( () -> {
            try {
                startWatchService();
            } catch (InterruptedException ignored) { }
        });
    }

    /** */
    private void startWatchService() throws InterruptedException {
        WatchKey key;
        while ((key = watchService.take()) != null) {
            Thread.sleep(50); // Чтобы не получать два одинаковых события
            for (WatchEvent<?> event : key.pollEvents()) {
                final Path context = (Path) event.context();

                if (!context.endsWith(stopFile.getFileName())) continue;

                if (event.kind().equals(ENTRY_CREATE) || event.kind().equals(ENTRY_MODIFY)) {
                    checkStopFile();
                }
            }
            key.reset();
        }
    }

    public void stopWatchService( ) {
        threadPool.shutdownNow();
        try {
            watchService.close();
        } catch (IOException ignored) {
        }
    }

    private void checkStopFile( ) {

        // останавливаем приложение, при наличии stop файла
        if( Files.exists(stopFile) )
            killApplication( true );
        /*
        // Если файл существует и он пустой, то убиваем приложение
        if (checkFileOnEmpty()) {
            BaseApp.appLog.warn( Tags.PRODUCT_LABEL + "You can't use this application yet," +
                        " please, wait");
            killApplication(true);
            return;
        }

        String[] time = new String(read(stopFile)).split(Pattern.quote("_"));

        if (time.length < 2 || time.length > 2) return;

        // Получим дату и период
        if (!time[0].isEmpty() && !time[1].isEmpty()) {
            String date = time[0] + "Z"; // Добавляем Z для корректного парсинга
            long duration = Long.parseLong(time[1]);

            LocalDateTime localDateTime = convertToLocalTime(date).plusSeconds(duration);

            // Сравним текущее время с ожидаемым
            if (localDateTime.compareTo(LocalDateTime.now()) > 0) {
                BaseApp.appLog.warn( Tags.PRODUCT_LABEL + "You can't use this application yet," +
                        " please, wait until " + localDateTime);
                killApplication(true);
            }
        }
         */
    }

    private boolean checkFileOnEmpty() {
        return new String(read(stopFile)).isEmpty();
    }

    private byte[] read(Path file) {
        long size = 0;
        try {
            size = readAttributes(file).size();
        } catch (IOException ignored) {
        }

        byte[] bytes = new byte[(int) size];
        try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ)) {
            read(input, bytes);
        } catch (IOException ignored) {
        }

        return bytes;
    }

    private void read(InputStream input, byte[] bytes) throws IOException {
        int offset = 0;
        while (offset < bytes.length) {
            int length = input.read(bytes, offset, bytes.length - offset);
            if (length < 0) {
                throw new EOFException(String.format("%d bytes remaining", bytes.length - offset));
            }
            offset += length;
        }
    }

    private BasicFileAttributes readAttributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class);
    }

    /*
    private String getCurrentTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now).replace(" ", "T");
    }
    */

    private LocalDateTime convertToLocalTime(String dateString) {
        Instant instant = Instant.parse(dateString);
        return LocalDateTime.ofInstant(instant, ZoneId.of(ZoneOffset.UTC.getId()));
    }

    /** */
    private static Thread newThread( String name, Runnable runnable )
    {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        if( name != null )
        {
            thread.setName( name );
        }
        return thread;
    }

    /** */
    static private void killApplication( boolean reason ) {

        BaseApp.appLog.info( Tags.PRODUCT_LABEL + "AppKiller stop the APP! " + ( reason ? "by the stop file." : " by the CHECK_XXI_UPDATE." ) );

        Platform.exit( );
        System.exit  (0);
    }

    public Path getStopFile() {
        return stopFile;
    }
    
    /** */
    public static void run()  {
        new AppKiller();
    }

    /** */
    public static void runCheckXxiUpgrade( )
    {

        if( BaseApp.APP( ).getProperties( PRP ).getBooleanProperty( PRP_CHECK_XXI_UPGRADE_DISABLE, false ) ) {
            BaseApp.appLog.info( Tags.PRODUCT_LABEL + "CHECK_XXI_UPGRADE is disabled, '" + PRP_CHECK_XXI_UPGRADE_DISABLE + "' is set!" );
            return;
        }

        BaseApp.appLog.info( Tags.PRODUCT_LABEL + "CHECK_XXI_UPGRADE run. check every 10 min!" );

        name_thread_mode = false;

        // Получаем текущее время из БД
        final Connection c1 = BaseApp.APP().getCommonTaskContext().getConnection();
        LocalTime time = LocalTime.now();
        try( final PreparedStatement ps = c1.prepareStatement ("select LOCALTIMESTAMP d from dual") ) {
            try( final ResultSet rs = ps.executeQuery() ) {
                 rs.next();
                 time = TypeConverter.convert( rs.getObject(1), LocalTime.class );
            }
        }
        catch(Exception ignored) {
        }

        final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor( threadFactory );

        final Runnable check4XxiUpgrade = new Runnable() {
            @Override
            public void run() {

                final Runnable stop = new Runnable() {
                    @Override
                    public void run() {
                        ThreadPoolManager.getInstance().executeTask( ()->ses.shutdownNow( ) );
                    }
                };

                try {

                    final Connection c = BaseApp.APP().getCommonTaskContext().getConnection();

                    if( c == null || c.isClosed() )
                        throw new IllegalStateException( Tags.PRODUCT_LABEL + "Common task context connection is closed." );

                    boolean enableUpgrade = false;

                    try( final PreparedStatement ps = c.prepareStatement (
                         "select OBJECT_ID from all_objects where owner = 'PUBLIC' and object_name = 'XXI_UPGRADE' and OBJECT_TYPE = 'SYNONYM'\n--lti"
                            //QueryDBTraceIgnoreList.exprList[1]
                         )
                    ) {
                        try( final ResultSet rs = ps.executeQuery() ) {
                            enableUpgrade = rs.next();
                        }
                    }

                    if( enableUpgrade ) {
                        BaseApp.appLog.info( Tags.PRODUCT_LABEL + "(ru) Технический перерыв! В ЦАБС ведутся системные работы! " );
                        BaseApp.appLog.info( Tags.PRODUCT_LABEL + "(en) Technical break! System works are carried out in the XXI DB!" );
                        //ses.shutdownNow( );

                        stop.run();

                        killApplication( false );
                    }
                }
                catch( Throwable th ) {
                    System.err.println("Error on check Xxi Upgrade. Service CHECK_XXI_UPGRADE is shutdown.");

                    stop.run();
                    //ses.shutdownNow( );
                }
            }
        };

        // Запускаем раз в 10 мин ОТ НАЧАЛА часа
        int m = time.getMinute();
        int n = 0;
        if( m != 0 )
        {
            n = m % 10;
            n = 10 - n;
            m += n;

            if( m > 59 )
                m = 59;

            time = time.withMinute(m);
        }

        BaseApp.appLog.info( Tags.PRODUCT_LABEL + "start with " + time + ", with initDelay " + n );

        ses.scheduleWithFixedDelay( check4XxiUpgrade, n, 10, TimeUnit.MINUTES );
    }

    // Убить сессию в БД
    public static int killDBSession( Long sessionId ) //throws Exception
    {
        if( U.nvl(sessionId,0l) == 0l )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "Bad 'sessionId' value. 0 or null" );

        if( sessionId.equals( BaseApp.APP().getCommonTaskContext().getSessionID() ) )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'sessionId' is equal commonTaskContext sessionId. Unacceptable." );

        final Connection connection = BaseApp.APP().getCommonTaskContext().getConnection();

        try( CallableStatement cs = connection.prepareCall("{?= call JF_pkg_Util.kill_Session(?,?)}") ){

             cs.registerOutParameter(1, Types.INTEGER);
             cs.registerOutParameter(3, Types.INTEGER);
             cs.setLong(2,sessionId);

             cs.execute();

             if( cs.getInt(1) == -1 )
                 throw new RuntimeException( cs.getString(3) );

             return 0;
        }
        catch( SQLException ex ) {

            if( ex.getErrorCode() == 31 )
                return 31;

            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on kil # " + sessionId +" session ", ex  );
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on kil # " + sessionId +" session ", th  );
        }
    }
}
