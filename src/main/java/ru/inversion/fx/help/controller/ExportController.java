package ru.inversion.fx.help.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.dataset.aggr.IAggregator;
import ru.inversion.fx.app.AlertException;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.JInvChoiceBox;
import ru.inversion.fx.form.controls.JInvRadioButton;
import ru.inversion.fx.form.controls.JInvTextField;
import ru.inversion.fx.form.controls.progress.ProgressCallback;
import ru.inversion.fx.form.controls.progress.ProgressTaskExecutor;
import ru.inversion.fx.form.lov.*;
import ru.inversion.fx.help.entity.PHelp;
import ru.inversion.fx.help.store.HelpSaver;
import ru.inversion.utils.Pair;
import ru.inversion.utils.U;
import ru.inversion.utils.converter.TypeConverter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static ru.inversion.fx.help.store.HelpSaver.Format.*;


/**
 * Внутренний класс.
 * Класс реализует контроллер для добавления ссылок.
 *
 * @author perov
 * @version 1.0.0
 */
public class ExportController extends JInvFXFormController<PHelp> {

    final private static ResourceBundle bundle = ResourceBundle.getBundle("ru.inversion.fx.help.fxml.help");

    @FXML
    JInvTextField edFile;

    @FXML
    JInvRadioButton rbMarked;

    @FXML
    JInvRadioButton rbAll;

    @FXML
    JInvChoiceBox<HelpSaver.Format,HelpSaver.Format> cbFormat;

    @Override
    protected void init() throws Exception {

        setTitle( bundle.getString("EXPORT_TITLE") );

        cbFormat.getItems().addAll( HelpSaver.Format.values() );
        cbFormat.setValue(INV_UTF8_BOM);

        rbAll.setSelected(true);

        final AbstractLovBase<File> fileLov = saveFileLov( () -> edFile.getText(), ()->cbFormat.getValue() );
        edFile.setLOV( fileLov );

        final XXIDataSet<PHelp> ds = getInitParameter("ds");
        if( !ds.hasMarkedRows() )
            rbMarked.setDisable(true);
    }

    static private File pathToExport;

    /** Создание лов для выбора имени сохраняемого файла */
    public static AbstractLovBase<File> saveFileLov( Supplier<String> nameInitiator, Supplier<HelpSaver.Format> fmtSupplier )
    {

        final JInvFileChooserLov fileLov = new JInvFileChooserLov() {
            @Override
            public void showChoiceList(ViewContext vc, String filterString, BiConsumer< Boolean, ILov<File>> clb) {

            getExtensionFilters().clear();

            getExtensionFilters().add (
                INV_UTF8_BOM == fmtSupplier.get()
                    ? new FileChooser.ExtensionFilter( "INVO format files (utf-8, BOM)", "*.inv" )
                    :
                    new FileChooser.ExtensionFilter( "XML Files", "*.xml" )
            );
            super.showChoiceList(vc, nameInitiator.get(), clb);
            }
        };

        fileLov.setTitle(bundle.getString("EXPORT_TO_FILE"));
        fileLov.showSaveProperty().set(true);


        if( pathToExport != null )
            fileLov.setInitialDirectory(pathToExport);

        fileLov.valueProperty().addListener(new ChangeListener< File>() {
            @Override
            public void changed(ObservableValue< ? extends File> observable, File oldValue, File newValue) {
                pathToExport = newValue.getParentFile();
            }
        });

        return fileLov;
    }

    /** */
    private Pair<Iterable<PHelp>, Long > prepareIterable( ) throws Exception
    {
        Iterator<PHelp> iter;

        final XXIDataSet<PHelp> ds = getInitParameter("ds");

        if( rbAll.isSelected() )
        {
            iter = ds.createRSIterator(false);

            try ( IAggregator<PHelp> aggr = ds.createAggregatorBuilder(false, false).add("1", AggrFuncEnum.COUNT).build() ) {

                  aggr.execute();

                  return Pair.makePair( U.iterable(iter), TypeConverter.convert( aggr.getValues().get("A$1"), Long.class ) );

            }
        }
        else if( rbMarked.isSelected() )
        {
            iter = ds.createMarkedRSIterator(false);
            return Pair.makePair( U.iterable(iter), ds.computeNumberMarkedRows() );
        }
        else {

            PHelp current = ds.getCurrentRow();
            if( current == null )
                return Pair.makePair( U.iterable(Collections.emptyIterator()), 0L );
            else
                return Pair.makePair( U.iterable(Collections.singletonList(current).iterator()), 1L );
        }
    }

    /** */
    private void save( ) throws Exception {

        Pair<Iterable<PHelp>, Long > p = prepareIterable();

        final HelpSaver.Format fmt = cbFormat.getValue();

        String fileName = edFile.getText();

        final Path fileTo;
        try {
            fileTo = Paths.get(fileName);
        } catch ( Exception e) {
            throw new AlertException(e);
        }

        final ProgressTaskExecutor<Void, Void> progressTaskExecutor = new ProgressTaskExecutor<>();

        progressTaskExecutor
            .stage( getViewContext().getStageOrPrimaryStage(), "Выгрузка в файл", true )
            .callback( saveExport(fileTo, fmt, p.first, p.second ) )
        .execute();
    }

    /** */
    private static ProgressCallback<Void, Void> saveExport( Path fileTo, HelpSaver.Format format, Iterable<PHelp> iter, long totalCount )
    {
        return ( progress, param ) -> {
            try {

                final HelpSaver hs = new HelpSaver();

                hs.save(
                        fileTo,
                        format,
                        iter,
                        new Consumer<String>()
                        {
                            long counter = 0L;
                            @Override
                            public void accept( String s) {
                                progress.process( ++counter, totalCount, s );
                            }
                        }
                );

                progress.end( "ok" );

                return null;

            } catch ( Throwable th ) {
                throw new RuntimeException( th );
            }
        };
    }

    /** */
    @Override
    protected boolean onOK() {
        try {
            save();
        } catch (Exception e) {
            handleException(e);
        }
        return false;
    }
}
