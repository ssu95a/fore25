package ru.inversion.fx.form.lov;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;
import javafx.stage.DirectoryChooser;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvChoiceButton;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;

/**
 * LOV для выбора директории
 * @author ssu
 */
public class JInvDirectoryChooserLov extends AbstractLovBase<File> {

    public JInvDirectoryChooserLov() {
        super();
        setProperty( JInvChoiceButton.PROPERTY_CUSTOM_ICON, FontAwesome.fa_folder_open_o );
    }

    private final DirectoryChooser dirChooser = new DirectoryChooser();

    // Optional
    private String fileName;

    @Override
    public void showChoiceList(ViewContext vc, String filterString, BiConsumer<Boolean, ILov<File>> clb) {

        dirChooser.setTitle( getTitle() );

        if( dirChooser.getInitialDirectory() == null )
        {
            if (S.isNotNullOrEmpty(filterString)) {
                final Path path = Paths.get(filterString);
                // Если путь является файлом, а не директорией - берем папку файла
                if (Files.isDirectory(path)) {
                    dirChooser.setInitialDirectory(path.toFile());
                } else {
                    dirChooser.setInitialDirectory(path.getParent().toFile());
                }
            } else {
                dirChooser.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
            }
        }

        if (!dirChooser.getInitialDirectory().isDirectory()){
            dirChooser.setInitialDirectory(Paths.get(System.getProperty("user.home")).toFile());
        }

        final File selectedDirectory = dirChooser.showDialog(vc.getStageOrPrimaryStage());

        if( selectedDirectory != null)
        {
            if( S.isNotNullOrEmpty(fileName) )
                setValue(selectedDirectory.toPath().resolve(fileName).toFile() );
             else
                setValue(selectedDirectory);

            if (clb != null)
                clb.accept(true, this);
        }
    }

    public void setInitialDirectory(Path filePath) {
        setInitialDirectory(filePath.toFile());
    }

    public void setInitialDirectory(File file) {
        dirChooser.setInitialDirectory(file);
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Добавляет наименование файла к выбранной директории
     * @param fileName наименование файла
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public boolean isSmallLov( ) {
        return false;
    }

    /* OLD METHOD
    @Override
    public void showChoiceList( ViewContext vc, String filterString, BiConsumer<Boolean, ILov<File>> clb) {

        if( S.isNotNullOrEmpty(filterString) ) {

            File dir = new File(filterString);

            if( dir.exists() && dir.isDirectory() )
                g_directoryChooser.setInitialDirectory(dir);
        }

        g_directoryChooser.setTitle( getTitle() );

        File dir = g_directoryChooser.showDialog( vc.getStageOrPrimaryStage() );

        if( dir != null )
            setValue(dir);

        if( clb != null )
            clb.accept( dir != null, this );
    }
    */
}
