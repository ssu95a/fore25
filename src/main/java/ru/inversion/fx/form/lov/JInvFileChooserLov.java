package ru.inversion.fx.form.lov;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import ru.inversion.fx.form.FileChooserDialog;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvChoiceButton;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.utils.S;

import java.io.File;
import java.util.function.BiConsumer;

/**
 *
 * @author ssu
 */
public class JInvFileChooserLov extends AbstractLovBase<File> {

    static private final FileChooserDialog g_selectAllFileChooser = new FileChooserDialog();

    private FileChooserDialog fileChooser;

    final private BooleanProperty showSaveProperty = new SimpleBooleanProperty( this, "showSave", false );

    public JInvFileChooserLov() {
        super();
        setProperty( JInvChoiceButton.PROPERTY_CUSTOM_ICON, FontAwesome.fa_folder_open_o );
    }

    public JInvFileChooserLov( boolean showSave ) {
        super();
        if( !showSave )
            setProperty( JInvChoiceButton.PROPERTY_CUSTOM_ICON, FontAwesome.fa_folder_open_o );
        else
            setProperty( JInvChoiceButton.PROPERTY_CUSTOM_ICON, FontAwesome.fa_save );

        showSaveProperty.set( showSave );
    }

    /** */
    @Override
    public boolean isSmallLov( ) {
        return false;
    }

    /**
     * Признак, что диалог выбора показывается в режиме "Открытия" или "Сохранения" файла.
     * <p>
     * true - сохранение
     * false - открытие
     */
    public BooleanProperty showSaveProperty( ) {
        return showSaveProperty;
    }


    private void checkChooser()
    {
        if( fileChooser == null )
            fileChooser = new FileChooserDialog((String)getProperty("chooser_identifier"));
    }

    /**
     * Список фильтров для типов файлов.
     * @see FileChooser#getExtensionFilters()
     */
    public ObservableList<FileChooser.ExtensionFilter> getExtensionFilters() {
        checkChooser();
        return fileChooser.getExtensionFilters();
    }

    /**
     * Папка на которой открывается диалог выбора.
     * @see FileChooser#setInitialDirectory
     */
    public void setInitialDirectory( File initialDirectory ) {

        checkChooser();

        if( initialDirectory != null && initialDirectory.exists() && initialDirectory.isDirectory() &&  initialDirectory.canRead() ) {
            fileChooser.initialDirectory(initialDirectory);
        }
    }

    @Override
    public boolean checkValue( File value ) {

        if( value != null )
        {
            if( !showSaveProperty.get() )
                return value.exists() && value.isDirectory();
            else
                return value.isFile() && value.exists();
        }
        return true;
    }

    /** */
    @Override
    public void showChoiceList( ViewContext vc, String filterString, BiConsumer<Boolean, ILov<File>> clb ) {

        FileChooserDialog fc = fileChooser == null ? g_selectAllFileChooser : fileChooser;

        fc.titleProperty().bind( titleProperty() );
        fc.setInitialFileName( filterString );

        if( S.isNotNullOrEmpty(filterString) ) {

            File dir = new File(filterString);

            if( !dir.exists() )
                 dir = dir.getParentFile();

            if( dir.exists() )
            {
                if( dir.isDirectory() )
                    fc.initialDirectory(dir);

                else {

                    dir = dir.getParentFile();

                    if( dir.exists() && dir.isDirectory() )
                        fc.initialDirectory(dir);
                }
            }
        }

        File file = null;

        if( showSaveProperty.get() )
            file = fc.showSaveDialog( vc.getStageOrPrimaryStage() );
        else
            file = fc.showOpenDialog( vc.getStageOrPrimaryStage() );

        if( file != null )
            setValue(file);

        if( clb != null )
            clb.accept( file != null, this);
    }
}
