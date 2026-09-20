package ru.inversion.filesearch;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Системный класс
 * @author perov
 * @version 1.0.0
 */
abstract class AbstractLayerState implements IFileSearchState {

    protected Logger logger = LoggerFactory.getLogger(getClass().getCanonicalName());
    public static final int BUFFER = 2048;
    public static final String USER_HOME = "user.home";

//    String getEnvVarable(String varableName) {
//        return System.getenv().get(varableName);
//    }

    /**
     * Формируем путь в зависимости от OS
     *
     * @param path
     * @return
     */
    String getPathToOs(String path) {
        if (!SystemUtils.IS_OS_WINDOWS) {
            return path.toLowerCase();
        }
        return path;
    }

    static boolean unZip(File zipFile, Map<String, String> map) throws IOException {
        try (ZipFile zipfile = new ZipFile(zipFile)) {
            ZipEntry entry;
            Enumeration e = zipfile.entries();
            if (!e.hasMoreElements()){
                return false;
            }
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
//                System.out.println("[unZip] Extracting: " + entry);
                File file = new File(System.getProperty(USER_HOME) + File.separator + entry.getName());
                file.getParentFile().mkdirs(); // Will create parent directories if not exists
                file.createNewFile();
                try (BufferedInputStream is = new BufferedInputStream(zipfile.getInputStream(entry));
                        FileOutputStream fos = new FileOutputStream(file);
                        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);) {
                    int count;
                    byte data[] = new byte[BUFFER];
                    while ((count = is.read(data, 0, BUFFER))
                            != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    map.put(entry.getName(), file.getAbsolutePath());
                }
                file.setLastModified(trunkMillis(entry.getLastModifiedTime().toMillis()));
            }
            return true; 
        }
    }

    Path getFilePath(String fileName) {
        logger.trace(String.format("[getFilePath] fileName = %s", fileName));
        Path userDir = Paths.get(System.getProperty(USER_HOME));
        logger.trace(String.format("[getFilePath] userDir = %s", userDir));
        return userDir.resolve(fileName);
    }
    
    /**
     * Обрезаем милисекунды
     * @param dateMillis
     * @return 
     */
     
    static long trunkMillis(long dateMillis) {
        return dateMillis/1000*1000;
    }

}
