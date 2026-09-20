package ru.inversion.filesearch;

import ru.inversion.filesearch.model.FileAttributes;
import ru.inversion.fx.app.Tags;
import ru.inversion.utils.S;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * Трехзвенная архитектура.
 * <p>
 * Системный класс.
 * @author perov
 * @version 1.0.0
 */
class ThreeLayerStateImpl extends AbstractLayerState {

    private static final String XXI_DIR = "xxi_home";
    private static final String USER_HOME = System.getProperty("user.home");
    private final Map<String, Map<String, Long>> mapCheckToserver = new HashMap<>();
    private final URL url;
    private static final String IFS_URL = "/ifs/webapi";
    private static final String RESOURCE = "/getfile";
 
    ThreeLayerStateImpl(String middleServerUrl) {
        try {
            this.url = new URL(middleServerUrl.substring(0, middleServerUrl.indexOf("/", 8)));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(Tags.PRODUCT_LABEL + "Bad Format for URL", ex);
        }
    }

    @Override
    public Map fillMap(Map<String, String> map) throws Exception {
        logger.trace("[fillMap] user.home = '{}'", USER_HOME);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url + IFS_URL).path(RESOURCE);

        /* Заполняем map добавляя информацию о аттрибутах каждого файла */
        Map<String, FileAttributes> mapToServer = new HashMap<>();
        map.forEach((fileName, value) -> {
            Path file = getFilePath(fileName);
            if (Files.exists(file) && Files.isRegularFile(file)) {
                try {
                    BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);

                    // Создаем объект в котором будем хранить атрибуты файла
                    FileAttributes fileAttributes = new FileAttributes();
                    fileAttributes.setCreationTime(attr.creationTime().toMillis());
                    fileAttributes.setLastAccessTime(attr.lastAccessTime().toMillis());
                    fileAttributes.setLastModifiedTime(attr.lastModifiedTime().toMillis());
                    fileAttributes.setSize(attr.size());

                    mapToServer.put(fileName, fileAttributes);
                } catch (IOException e) {
                    logger.error("Can't read attributes from file {}", file.toAbsolutePath());
                }
            } else if (S.isNotNullOrEmpty(fileName)) {
                mapToServer.put(fileName, new FileAttributes()); // Если известно только имя файла
            }
        });

        if (mapToServer.isEmpty()) return null;

        /* Преобразуем в JSON и отправляем на сервер */
        try {
            Response resp = target.request().post(Entity.json(mapToServer));
            
            if(resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode() || resp.getStatus() == Response.Status.SERVICE_UNAVAILABLE.getStatusCode()){
                throw new RuntimeException(resp.getEntity().toString());
            }
            
            if (resp.getStatus() == Response.Status.OK.getStatusCode()) {
                File file = resp.readEntity(File.class);;
                unZip(file, map);
                file.delete();
            }

            map.forEach((fileName, value) -> {
                Path file = getFilePath(fileName);
                if (Files.exists(file) && Files.isRegularFile(file)) {
                    map.put(fileName, file.toString());
                }
            });
            
            return map;
        } catch (Exception ex) {
            logger.trace(ex.getMessage());
            throw ex;
        }        
    }
}
