package ru.inversion.filesearch;

import ru.inversion.fx.app.Tags;
import ru.inversion.utils.S;
import ru.inversion.utils.U;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


/**
 * Двухзвенная архитектура.
 * <p>
 * Системный класс
 *
 * @author perov
 * @version 1.0.0
 */
class TwoLayerStateImpl extends AbstractLayerState {

    private static final char UNIX_SEPARATOR = '/';

    private static final char WINDOWS_SEPARATOR = '\\';

    public static final String XXI_HOME = "XXI_HOME";
    public static final String UFS_PATH = "UFS_PATH";
    private static Path WORK_DIR;

    private final Map<String, String> env;

    TwoLayerStateImpl() {
        env = System.getenv();
        // Инициализируем директорию
        if (env.containsKey(XXI_HOME)) {
            WORK_DIR = Paths.get(env.get(XXI_HOME));
        }
    }

    @Override
    public Map fillMap(final Map<String, String> map) throws IOException {
        if (WORK_DIR == null)
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Not set environment variable '" + XXI_HOME + "'");
        //xxiHome = System.getenv( XXI_HOME.toLowerCase() );

        logger.trace("[findFile] " + WORK_DIR);

        if (WORK_DIR == null)
            throw new RuntimeException(Tags.PRODUCT_LABEL + "it is impossible to get from '" + WORK_DIR + "' to the file object");

        if (!Files.exists(WORK_DIR))
            throw new FileNotFoundException(Tags.PRODUCT_LABEL + "directory '" + WORK_DIR + "' is not exists.");

        if (!Files.isDirectory(WORK_DIR))
            throw new NotDirectoryException(Tags.PRODUCT_LABEL + "'" + WORK_DIR + "' is not directory.");

        map.keySet().stream()
                .filter(S::isNotNullOrEmpty)
                .forEach(name -> {
                    U.<String>callIfNotNull((s) -> map.put(name, s), findFile(WORK_DIR, name));
                });

        return map;
    }

    private String findFile(final Path homePath, final String fileName) {
        Path filePath = null;
        try {
            logger.debug("[findFile] Search file = {}", fileName);
            if (isUfsFile(fileName) && env.containsKey(UFS_PATH)) {
                // Для UFS файлов ищем в другой директории
                String pathToFile = getPath(fileName.substring(fileName.indexOf("/") + 1)); // Путь до файла
                String ufsFileName = ufsNameToFileName(fileName); // Имя файла из UFS пути
                filePath = Paths.get(env.get(UFS_PATH)).resolve(pathToFile).resolve(ufsFileName);
            } else {
                filePath = homePath.resolve(fileName);
            }

            File file = filePath.toFile();

            if (file.exists() && file.isFile()) {
                String fn = file.getAbsolutePath();

                logger.trace("[findFile] file.getAbsolutePath = '{}'", fn);

                return fn;
            } else {
                if (file.isDirectory()) {
                    logger.debug("[findFile] It's not a file path - '{}'", filePath);
                } else {
                    logger.debug("[findFile] file not found, file path = '{}'", filePath);
                }
            }
        } catch (Exception ex) {
            logger.error("[findFile] error on findFile, file path = '{}'", filePath);
            logger.error("[findFile] Exception", ex);
        }

        return null;
    }

    private String ufsNameToFileName(final String ufsFileName) {
        final int index = indexOfLastSeparator(ufsFileName);
        return ufsFileName.substring(index + 1);
    }

    private int indexOfLastSeparator(final String filename) {
        final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
        final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    private String getPath(final String filename) {
        final int index = indexOfLastSeparator(filename);
        final int endIndex = index + 1;
        return filename.substring(0, endIndex);
    }

    private boolean isUfsFile(final String filename) {
        String filenameLow = filename.toLowerCase();
        if (filenameLow.startsWith("ufs" + File.separator)) {
            return true;
        } else if (filenameLow.startsWith("ufs/")) {
            return true;
        }
        return false;
    }
}
