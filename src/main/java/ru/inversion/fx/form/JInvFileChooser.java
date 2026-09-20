package ru.inversion.fx.form;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.inversion.utils.S;

import java.io.File;
import java.util.List;


/**
 * Вспомогательный класс реализующий статические методы выбора файлов.
 * @author perov
 * @version 1.0.0
 */
public class JInvFileChooser {

    /**
     * Диалог выбора файла для открытия.
     * <p>
     * @param ownerWindow
     *          окно родителя
     * @param initialDirectory
     *          Исходный каталог для диалогового окна, отображаемого файла.
     * @param initialFileName
     *          Исходное имя файла для отображаемого диалога.
     * @param title
     *          Заголовок диалогового окна.
     * @param selectedExtensionFilter
     *          Массив фильтров для отбора файлов
     *
     * @return File
     *          выбранный файл или {@code null} если была отказ от выбора
     */
    public static File showOpenDialog (
        Window ownerWindow,
        File   initialDirectory,
        String initialFileName,
        String title,
        FileChooser.ExtensionFilter... selectedExtensionFilter
    )
    {
        FileChooser fileChooser = new FileChooser();

        if( initialDirectory != null ) {

            if( initialDirectory.exists() && initialDirectory.isDirectory() )
                fileChooser.setInitialDirectory( initialDirectory );
        }

        fileChooser.setInitialFileName ( initialFileName  );

        if( selectedExtensionFilter != null && selectedExtensionFilter.length > 0 )
            fileChooser.getExtensionFilters().addAll( selectedExtensionFilter );

        if( S.isNotNullOrEmpty( title ) )
            fileChooser.setTitle( title );

        return fileChooser.showOpenDialog( ownerWindow );
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow){
        return showOpenDialog(ownerWindow,null,null,null,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @param title - Заголовок диалогового окна.
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow, String title){
        return showOpenDialog(ownerWindow,null,null,title,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна, отображаемого файла.
     * @param title - Заголовок диалогового окна.
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow, File initialDirectory, String title){
        return showOpenDialog(ownerWindow,initialDirectory,null,title, new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна, отображаемого файла.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param title - Заголовок диалогового окна.
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow, File initialDirectory, String initialFileName, String title){
        return showOpenDialog(ownerWindow,initialDirectory,initialFileName,title,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showOpenDialog(ownerWindow,null,null,null,selectedExtensionFilter);
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна, отображаемого файла.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow, File initialDirectory, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showOpenDialog(ownerWindow,initialDirectory,null,null,selectedExtensionFilter);
    }

    /**
     * Диалог открытия фaйла
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна, отображаемого файла.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showOpenDialog(Window ownerWindow, File initialDirectory, String initialFileName, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showOpenDialog(ownerWindow,initialDirectory, initialFileName, null, selectedExtensionFilter);
    }

    /**
     * Диалог для выбора нескольких файлов
     * <p>
     * @param ownerWindow
     *      Окно родителя
     * @param initialDirectory
     *      Исходный каталог для диалогового окна.
     * @param initialFileName
     *      Исходное имя файла для отображаемого диалога.
     * @param title
     *      Заголовок диалогового окна.
     * @param selectedExtensionFilter
     *      Массив фильтров расширения файла
     *
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog (
            Window  ownerWindow,
            File    initialDirectory,
            String  initialFileName,
            String  title,
            FileChooser.ExtensionFilter... selectedExtensionFilter
    )
    {
        FileChooser fileChooser = new FileChooser();

        if( initialDirectory != null ) {

            if( initialDirectory.exists() && initialDirectory.isDirectory() )
                fileChooser.setInitialDirectory( initialDirectory );
        }

        fileChooser.setInitialFileName ( initialFileName  );

        if( selectedExtensionFilter != null && selectedExtensionFilter.length > 0 )
            fileChooser.getExtensionFilters().addAll( selectedExtensionFilter );

        if( S.isNotNullOrEmpty( title ) )
            fileChooser.setTitle( title );

        return fileChooser.showOpenMultipleDialog(ownerWindow);
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow){
        return showOpenMultipleDialog(ownerWindow,null,null,null,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @param title - Заголовок диалогового окна.
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, String title){
        return showOpenMultipleDialog(ownerWindow,null,null,title,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param title - Заголовок диалогового окна.
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, File initialDirectory, String title){
        return showOpenMultipleDialog(ownerWindow,initialDirectory,null,title, new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param title - Заголовок диалогового окна.
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, File initialDirectory, String initialFileName, String title){
        return showOpenMultipleDialog(ownerWindow,initialDirectory,initialFileName,title,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showOpenMultipleDialog(ownerWindow,null,null,null,selectedExtensionFilter);
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, File initialDirectory, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showOpenMultipleDialog(ownerWindow,initialDirectory,null,null,selectedExtensionFilter);
    }

    /**
     * Диалог открытия нескольких файлов
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return Список выбранных файлов
     */
    public static List<File> showOpenMultipleDialog(Window ownerWindow, File initialDirectory, String initialFileName, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showOpenMultipleDialog(ownerWindow,initialDirectory, initialFileName, null, selectedExtensionFilter);
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param title - Заголовок диалогового окна.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, File initialDirectory, String initialFileName, String title, FileChooser.ExtensionFilter... selectedExtensionFilter){

        FileChooser fileChooser = new FileChooser();

        if( initialDirectory != null ) {

            if( initialDirectory.exists() && initialDirectory.isDirectory() )
                fileChooser.setInitialDirectory( initialDirectory );
        }

        fileChooser.setInitialFileName ( initialFileName  );

        if( selectedExtensionFilter != null && selectedExtensionFilter.length > 0 )
            fileChooser.getExtensionFilters().addAll( selectedExtensionFilter );

        if( S.isNotNullOrEmpty( title ) )
            fileChooser.setTitle( title );

        return fileChooser.showSaveDialog(ownerWindow);
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow){
        return showSaveDialog(ownerWindow,null,null,null,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param title - Заголовок диалогового окна.
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, String title){
        return showSaveDialog(ownerWindow,null,null,title,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param title - Заголовок диалогового окна.
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, File initialDirectory, String title){
        return showSaveDialog(ownerWindow,initialDirectory,null,title, new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param title - Заголовок диалогового окна.
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, File initialDirectory, String initialFileName, String title){
        return showSaveDialog(ownerWindow,initialDirectory,initialFileName,title,new FileChooser.ExtensionFilter[]{});
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showSaveDialog(ownerWindow,null,null,null,selectedExtensionFilter);
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, File initialDirectory, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showSaveDialog(ownerWindow,initialDirectory,null,null,selectedExtensionFilter);
    }

    /**
     * Диалог сохранения
     * @param ownerWindow - окно родителя
     * @param initialDirectory - Исходный каталог для диалогового окна.
     * @param initialFileName - Исходное имя файла для отображаемого диалога.
     * @param selectedExtensionFilter - Массив фильтров расширения файла
     * @return File
     */
    public static File showSaveDialog(Window ownerWindow, File initialDirectory, String initialFileName, FileChooser.ExtensionFilter... selectedExtensionFilter){
        return showSaveDialog(ownerWindow,initialDirectory, initialFileName, null, selectedExtensionFilter);
    }
}
