package ru.inversion.fx.form;

import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.utils.S;

import java.io.File;
import java.util.List;

/**
 * Обертка на штатным механизмом выбора/сохранения файла!
 * Добавлена возможность работы с классом в стиле fluent-design
 * */
public class FileChooserDialog {

    final private FileChooser chooserImpl;
    final private String identifier;

    /** */
    public FileChooserDialog()
    {
        chooserImpl = new FileChooser();
        identifier  = null;
    }

    /** */
    public FileChooserDialog( String identifier)
    {
        this.identifier = identifier;
        chooserImpl = new FileChooser();

        if( !S.isNullOrEmpty(identifier) )
        {
            final Object o = BaseApp.APP().getProperties(PropertiesTypeEnum.LOCAL_USER).getProperty(identifier);
            if( o != null )
            {
                File f   = new File( o.toString() );
                File d   = null;
                String s = null;

                if( f.exists() )
                {
                    if( f.isDirectory() ) {
                        d = f;
                    }
                    else
                    {
                        d = f.getParentFile();
                        s = f.getName();
                    }
                }
                else
                {
                    d = f.getParentFile();
                    if( d.exists() && d.isDirectory() )
                        ;
                    else
                        d = null;
                }

                if( d != null )
                    initialDirectory(d);
                if( s != null )
                    fileName(s);
            }
        }
    }

    /** */
    public FileChooserDialog initialDirectory( File initialDirectory )
    {
        chooserImpl.setInitialDirectory(initialDirectory);
        return this;
    }
    public ObjectProperty<File> initialDirectoryProperty()
    { return chooserImpl.initialDirectoryProperty(); }
    public File getInitialDirectory()
    { return chooserImpl.getInitialDirectory(); }
    public void setInitialDirectory(File initialDirectory)
    {  chooserImpl.setInitialDirectory(initialDirectory); }

    /** */
    public FileChooserDialog title( String title )
    {
        chooserImpl.setTitle(title);
        return this;
    }
    public StringProperty titleProperty( )
    { return chooserImpl.titleProperty(); }
    public String getTitle( )
    { return chooserImpl.getTitle(); }
    public void setTitle( String title )
    { chooserImpl.setTitle(title); }

    /** */
    public FileChooserDialog initialFileName( String fileName )
    {
        chooserImpl.setInitialFileName(fileName);
        return this;
    }
    public ObjectProperty<String> initialFileNameProperty( )
    { return chooserImpl.initialFileNameProperty(); }
    public String getInitialFileName( )
    { return chooserImpl.getInitialFileName(); }
    public void setInitialFileName( String initialFileName )
    { chooserImpl.setInitialFileName(initialFileName); }

    private StringProperty fileNameProperty;

    public StringProperty fileNameProperty( )
    {
        if( fileNameProperty == null ) {
            fileNameProperty = new StringProperty( ) {
                final ObjectProperty<String> base = chooserImpl.initialFileNameProperty();
                @Override
                public void addListener( InvalidationListener listener ) { base.addListener(listener);}
                @Override
                public void removeListener( InvalidationListener listener ) { base.removeListener(listener);}
                @Override
                public void addListener( ChangeListener< ? super String > listener ) { base.addListener(listener);}
                @Override
                public void removeListener( ChangeListener< ? super String > listener ) { base.removeListener(listener); }
                @Override
                public String get() {
                    return base.get();
                }
                @Override
                public void set( String value ) {
                    base.set(value);
                }
                @Override
                public Object getBean() { return FileChooserDialog.this;}
                @Override
                public String getName() { return "fileName"; }

                @Override
                public void bind( ObservableValue< ? extends String > observable ) {
                    base.bind(observable);
                }
                @Override
                public void unbind() {
                    base.unbind();
                }
                @Override
                public boolean isBound() {
                    return base.isBound();
                }
            };
        }
        return fileNameProperty;
    }
    public FileChooserDialog fileName( String fileName )
    {
        setFileName(fileName);
        return this;
    }
    public String getFileName( )
    { return chooserImpl.getInitialFileName(); }
    public void setFileName( String initialFileName )
    { chooserImpl.setInitialFileName(initialFileName); }

    /** */
    public FileChooserDialog addFilter( String name, String ... ext )
    {
        final ObservableList<FileChooser.ExtensionFilter> filters = chooserImpl.getExtensionFilters();
        filters.add( new FileChooser.ExtensionFilter(name, ext) );
        return this;
    }
    /** */
    public ObservableList< FileChooser.ExtensionFilter > getExtensionFilters() {
        return chooserImpl.getExtensionFilters();
    }
    /** */
    public ObjectProperty< FileChooser.ExtensionFilter > selectedExtensionFilterProperty() {
        return chooserImpl.selectedExtensionFilterProperty();
    }
    public void setSelectedExtensionFilter( FileChooser.ExtensionFilter filter) {
        chooserImpl.setSelectedExtensionFilter(filter);
    }
    public FileChooser.ExtensionFilter getSelectedExtensionFilter() {
        return chooserImpl.getSelectedExtensionFilter();
    }
    /** */
    public File showOpenDialog( Window ownerWindow ) {
        final File f = chooserImpl.showOpenDialog(ownerWindow);
        if( f != null && !S.isNullOrEmpty(identifier) )
        {
            BaseApp.APP().getProperties(PropertiesTypeEnum.LOCAL_USER).setProperty(identifier, f.getAbsolutePath() );
        }
        return f;
    }
    /** */
    public List<File> showOpenMultipleDialog( Window ownerWindow ) {
        return chooserImpl.showOpenMultipleDialog(ownerWindow );
    }
    /** */
    public File showSaveDialog( Window ownerWindow ) {
        final File f = chooserImpl.showSaveDialog(ownerWindow);
        if( f != null && !S.isNullOrEmpty(identifier) )
        {
            BaseApp.APP().getProperties(PropertiesTypeEnum.LOCAL_USER).setProperty(identifier, f.getAbsolutePath() );
        }
        return f;
    }

}
