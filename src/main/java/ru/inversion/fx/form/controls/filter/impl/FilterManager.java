package ru.inversion.fx.form.controls.filter.impl;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;
import ru.inversion.dataset.DataSetException;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.SQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.impl.XXIDsDao;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.FXFormLauncher;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.controls.filter.SQLFilter;
import ru.inversion.fx.form.controls.filter.entity.PFrmFilter;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.ResourceBundleFactory;
import ru.inversion.utils.S;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.LinkedBlockingQueue;

import static ru.inversion.fx.form.AbstractBaseController.FormReturnEnum.RET_OK;

/**
 * Запуск формы настройки фильтров
 * @author Sulimoff, fomishkin, perov
 */
public class FilterManager {

    public static final int C_MAX_FIELD_BD = 30; //длина поля в БД
    private static final ResourceBundle filterBundle = ResourceBundleFactory.INSTANCE().getBundle("filter");

    private final TaskContext tc;
    private final ViewContext vc;

    public FilterManager( TaskContext tc, ViewContext vc ) {

        if( tc == null || vc == null )
            throw new IllegalArgumentException("TaskContext or ViewContext is null");

        this.tc = tc;
        this.vc = vc;
    }

    /**
     * Запустить форму настройки фильтров
     *
     * Реагирует на следующие параметры:
     * "FORM_NAME","BLOCK_NAME","DATA_SET"
     */
    public static void openFilterList( ViewContext vc, TaskContext tc, Map<String, Object> params )
    {
        try {
            new FXFormLauncher(tc, vc, FilterListController.class, filterBundle).modal(true).initProperties(params).show();
        } catch( Throwable ex) {
            JInvErrorService.handleException(vc, ex);
        }
    }

    /** */
    public <P> void getAndExecuteFilter( DSFXAdapter<P> dsAdapt) {

        try {

            if( ! (dsAdapt.getDataSet() instanceof SQLDataSet ) )
                return;

            final SQLDataSet<P> ds = (SQLDataSet<P>)dsAdapt.getDataSet();

            String formName  = vc.getFormNameForFilter();
            String blockName = ds.getName();

            if( formName == null || blockName == null ) {
                throw new IllegalArgumentException("formName or blockName is empty");
            }

            formName = S.trimLongString (formName, C_MAX_FIELD_BD );
            blockName = S.trimLongString (blockName, C_MAX_FIELD_BD);
            
            final Map<String, Object> param = new HashMap<>();
            param.put ("FORM_NAME", formName);
            param.put ("BLOCK_NAME", blockName);
            param.put ("FILTER_NAME", FilterWork.getPreferenceKey (formName, blockName));
            param.put ("HASPRM", null );

            new FXFormLauncher( tc, vc, ChoiceFilterController.class, filterBundle )
                .modal(true)
                .initProperties(param)
                .callback((t, u) -> {
                    if( t.equals(RET_OK))
                    {
                        SQLFilter filter = (SQLFilter) param.get("FILTER");
                        if (param.get("HASPRM") != null && (Integer) param.get("HASPRM") > 0) {
                            try {
                                ds.setFilter(filter, (boolean) param.get("FIXED"), (boolean) param.get("APPEND"));
                            } catch (DataSetException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            ds.setFilter(filter.getSQL(), (boolean) param.get("FIXED"), (boolean) param.get("APPEND"));
                        }
                        dsAdapt.executeQuery();
                    }
                })
                .show();
        } catch (Throwable ex) {
            JInvErrorService.handleException( vc, ex );
        }
    }

    /** */
    public void saveCurrentFilter( String filter, String formName, String blockName ) {

        if( S.isNullOrEmpty(filter) )
            throw new IllegalArgumentException("Filter is empty");

        final TextInputDialog dialog = new TextInputDialog( );
        dialog.setTitle      ( filterBundle.getString("SAVE_FILTER.TITLE_DIALOG") );
        dialog.setHeaderText ( filterBundle.getString("SAVE_FILTER.HEADER_TEXT" ) );
        dialog.setContentText( filterBundle.getString("SAVE_FILTER.CONTENT_TEXT") );

        try {

            BaseApp.APP().getViewPrefService().refreshViewSettingsRoot( dialog.getDialogPane() );
            Optional<String> result = dialog.showAndWait();

            if( result.isPresent() && !S.isNullOrEmpty(result.get())) {
                FilterWork.saveFilter(tc.getConnection(), S.trimLongString(formName, C_MAX_FIELD_BD), filter, S.trimLongString(blockName, C_MAX_FIELD_BD), result.get());
            }

        } catch( Exception ex) {
            JInvErrorService.handleException( null, ex);
        }
    }

    /**
     * Запустить форму настройки фильтров для конкретной таблицы
     */
    public void settingsFilter(String formName, IDataSet dataSet) {

        try {
            if (formName == null || dataSet == null) {
                throw new IllegalArgumentException("formName or blockName is empty");
            }
            final Map<String, Object> param = new HashMap<>();
            param.put("FORM_NAME", S.trimLongString(formName, C_MAX_FIELD_BD));
            param.put("BLOCK_NAME", S.trimLongString(dataSet.getName(), C_MAX_FIELD_BD));
            param.put("DATA_SET", dataSet);
            openFilterList(vc, tc, param);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            JInvErrorService.handleException(vc, ex);
        }
    }


    /**
     * Диалог выбора авто фильтра если их больше одного на таблице
     * formName и blockName приведены к общему виду
     */
    public static XXIDsDao.FilterData getAutoFilter( ViewContext vc, TaskContext tc, String formName, String blockName ) {

        try {

            XXIDsDao.FilterData retFilter = null;

            final SQLDataSet<PFrmFilter> dsFilter = new SQLDataSet<>( tc, PFrmFilter.class );
            dsFilter.taskContext(tc)
                    .nativeQueryName("autoList")
                    .queryAllRows()
                    .set ("FORM_NAME", S.trimLongString (formName, C_MAX_FIELD_BD))
                    .set ("BLOCK_NAME", S.trimLongString (blockName, C_MAX_FIELD_BD))
                    .execute( );

            if( !dsFilter.isEmpty() )
            {
                if( dsFilter.getLoadedRowCount() == 1 ) {
                    retFilter = XXIDsDao.loadAutoFilter( tc, formName, blockName );
                }
                else
                {
                    if( Platform.isFxApplicationThread() )
                    {
                        retFilter = new DialogAutoFilter( vc, tc, dsFilter ).showAndWait().get();
                    }
                    else
                    {
                        final LinkedBlockingQueue< XXIDsDao.FilterData > queue = new LinkedBlockingQueue<>();

                        Platform.runLater( ()->
                                queue.add( new DialogAutoFilter( vc, tc, dsFilter ).showAndWait().orElseThrow(()->new RuntimeException("Необходимо выбрать автофильтр")) )
                        );

                        retFilter = queue.take();
                    }
                }
            }
            return retFilter;
        }
        catch( Throwable th ) {
            throw new RuntimeException( Tags.PRODUCT_LABEL + "Error on load auto filter", th );
        }
    }
}

