/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.service.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inversion.fx.app.AppException;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.BaseAppHelper;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.service.IAppService;
import ru.inversion.utils.S;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Сервис осуществляющий модульные операции
 * <>p</>
 * Модуль как правило - один jar файл
 *
 * @author antonovdi
 */
public class ModuleService implements IAppService {

    public final static String SERVICE_ID = "APPSERVICE_MODULE";
    private static ModuleService g_instance;

    private Map<String, File> mapClassNameToJarFile = new HashMap<>();
    private Map<File, ModuleContext> mapJarFileToModuleContext = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(ModuleService.class);

    @Override
    public String getServiceInfo() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param clazz
     * @throws AppException
     */
    public void invokeModuleService(Class clazz) throws AppException {
        ModuleContext context = getModuleContextOrCreate(clazz);
        if (context != null) {
            checkVersion(context);
        }
    }


    public ModuleContext getModuleContextOrCreate( Class clazz ) {

        ModuleContext result = null;

        if (clazz != null) {
            String className = clazz.getCanonicalName();

            if (!mapClassNameToJarFile.containsKey(className)) {
                result = initModuleContext(clazz);
            } else {
                result = mapJarFileToModuleContext.get(mapClassNameToJarFile.get(className));
            }
        }

        return result;
    }

    private ModuleContext initModuleContext(Class clazz) {

        File jarFile = getJarFile(clazz);

        if (jarFile != null) {
            mapClassNameToJarFile.put(clazz.getCanonicalName(), jarFile);

            // Если джар файл уже добавлен в мапу с контекстами, то новый не создаем
            if (!mapJarFileToModuleContext.containsKey(jarFile)) {

//                logger.info("add jar to map " + "class " + clazz.getCanonicalName() + " jarFile " + jarFile.getAbsolutePath());
                ModuleContext context = new ModuleContext();
                try {
//                    Enumeration<URL> resources = clazz.getClassLoader().getResources(JarFile.MANIFEST_NAME);
//                    if (resources.hasMoreElements()) {

//                        Manifest manifest = new Manifest(resources.nextElement().openStream());
                    Manifest manifest = new JarFile(jarFile).getManifest();
                    Attributes attributes = manifest.getMainAttributes();

                    String value = attributes.getValue("Implementation-Version");

//                          String value = clazz.getPackage().getImplementationVersion();
//                        logger.info("impl version "+ value);
                    if (value != null) {
                        context.setFullVersion(value);

                        String appShortVersion = "";
                        if (S.isNotNullOrEmpty(value)) {
                            String[] splitVersion = value.split("\\s");
                            appShortVersion = splitVersion[splitVersion.length - 1];

                            if (appShortVersion.contains("_")) {
                                context.setManagerVersion(appShortVersion);
                            }
                        }
                    }

                    value = attributes.getValue("Core-Version");
                    if (value != null) {
                        context.setCoreVersion(value);
                    }

//                    }
                } catch (IOException ex) {
                    logger.error("Error read module proprties. Path to jar file  " + jarFile.getAbsolutePath(), ex);
                }
                context.setJarName(jarFile.getName());
                mapJarFileToModuleContext.put(jarFile, context);
                return context;
            } else {
                return mapJarFileToModuleContext.get(jarFile);
            }
        } else {
            // Запущено из среды не проводим проверки
        }

        return null;
    }

    private void checkVersion(ModuleContext context) throws AppException {
        if (!context.isCheckedDb()) {

            String moduleVersion = context.getManagerVersion();

            if (S.isNotNullOrEmpty(moduleVersion)) {
                if (context.getJarName() != null) {
                    BaseAppHelper.checkModuleVersion(BaseApp.APP().getCommonTaskContext(), moduleVersion, context.getJarName().toUpperCase());
                    context.setCheckedDb(true);
                }
            }
        }
    }

    /** */
    public static File getJarFile( Class<?> clazz ) {
        
        if( clazz == null )
            return null;
        
        try {
        
            //File file = new File( clazz.getProtectionDomain().getCodeSource().getLocation().toURI() );

            final URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
            //logger.info("URL jar file " + url);
            final URI uri = url.toURI();
            //logger.info("URI jar file " + uri);
            String pathToJar = uri.getPath();
            //logger.info("Path jar file " + pathToJar);


            final Path path = Paths.get(uri);
            final File file = path.toFile();

            //File file = new File( pathToJar );

            if( !file.isFile() )
                //throw new IllegalStateException(" The class '" + clazz.getName() + "' is not in jar module." + file.getAbsolutePath() + " is not file." );
                return null;
        
            return file;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on get jar file name by class '" + clazz.getName() + "'", th );
        }
    }
}
