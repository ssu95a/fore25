package ru.inversion.fx.form.controls.skin;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.collections.MapChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.control.Cell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import ru.inversion.dataset.DataSetEvent;
import ru.inversion.dataset.IDataSet;
import ru.inversion.dataset.IDataSetListener;
import ru.inversion.dataset.XXIDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.filter.JInvFilterToolBar;
import ru.inversion.utils.ResourceBundleFactory;
import ru.inversion.utils.S;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_DATA_SET_ADAPTER;

public class JInvTableViewSkin<T> extends TableViewSkin<T> {

    public static ResourceBundle fore = ResourceBundleFactory.INSTANCE().getBundle("fore");

    private final TableView<T>         tableView;
    private final JInvFilterToolBar<T> toolBar;
    private final DSInfoBar            dsInfoBar ;

    private IDataSet<T> dataSet;
    private IDataSetListener dataSetListener;

    // Чтобы не плодить слушатели/не ставить title много раз
    private boolean titleDone;
    private boolean listenerAttached;

    final MapChangeListener<Object,Object> mapChangeListener = new MapChangeListener<Object, Object>() {
        @Override
        public void onChanged(Change<?, ?> change) {
            if( !PROPERTY_DATA_SET_ADAPTER.equals(change.getKey()) )
                return;
            refreshFromAdapterSafe();
        }
    };

    @Override
    protected TableHeaderRow createTableHeaderRow( ) {
        return new JInvTableHeaderRow(this);
    }

    /** */
    public JInvTableViewSkin( TableView<T> tableView )
    {
        super(tableView);
        this.tableView = tableView;

        toolBar = new JInvFilterToolBar<>();
        toolBar.setOrientation(Orientation.VERTICAL);
        toolBar.setVisible(false);
        toolBar.setManaged(false);

        dsInfoBar = new DSInfoBar();
        dsInfoBar.setManaged(false);

        if( tableView instanceof JInvTable )
        {
            @SuppressWarnings("unchecked")
            final JInvTable<T> jit = (JInvTable<T>)tableView;

            dsInfoBar.init(jit);
            dsInfoBar.visibleProperty().bind(jit.visibleStatusBarProperty());

            toolBar.init(jit);
        }

        getChildren().addAll( toolBar, dsInfoBar );

        // Ловим появление адаптера в properties
        tableView.getProperties().addListener( mapChangeListener );

        // На случай, если адаптер уже был установлен ДО создания skin
        refreshFromAdapterSafe();

        // Любая смена visible -> перелэйаут
        tableView.visibleProperty().addListener(o -> tableView.requestLayout());
        dsInfoBar.visibleProperty().addListener(o -> tableView.requestLayout());
          toolBar.visibleProperty().addListener(o -> tableView.requestLayout());
    }

    private void refreshFromAdapterSafe() {
        if (Platform.isFxApplicationThread()) {
            refreshFromAdapter();
        } else {
            Platform.runLater(this::refreshFromAdapter);
        }
    }

    /** */
    private void refreshFromAdapter() {

        final DSFXAdapter<T> dsAdapter = Controls.getDsAdapterFromControl(tableView);

        if( dsAdapter == null )
            return;

        boolean showFilter = dsAdapter.isEnableFilter();
        toolBar.setVisible(showFilter);

        initFilterTitleListener(dsAdapter);

        // если адаптер уже точно есть — можно снять listener
        tableView.getProperties().removeListener(mapChangeListener);

        tableView.requestLayout();
    }


    /** */
    private void initFilterTitleListener( DSFXAdapter<T> dsAdapter )
    {
        if( titleDone )
            return;

        if (!(tableView instanceof JInvTable<?>)) {
            titleDone = true;
            return;
        }

        final IDataSet<T> ds = dsAdapter.getDataSet();

        if (!(ds instanceof XXIDataSet) || !((XXIDataSet<T>) ds).isEnableAutoFilter()) {
            titleDone = true;
            return;
        }

        if( trySetFilterTitle(ds) ) {
            titleDone = true;
            detachDataSetListener();
            return;
        }

        // Если не получилось — подписываемся один раз и ждём FIRST_TIME_EXECUTE
        if( listenerAttached )
            return;

        dataSet = ds;
        dataSetListener = e -> {
            if (e.isAfter() && e.getEventType() == DataSetEvent.DataSetEventType.FIRST_TIME_EXECUTE) {
                Platform.runLater(() -> {
                    if (titleDone) return;
                    boolean ok = trySetFilterTitle(dataSet);
                    //if (ok)
                    {
                        titleDone = true;
                        detachDataSetListener();
                    }
                });
            }
        };

        dataSet.addDataSetListener(dataSetListener);

        listenerAttached = true;
    }


    /** */
    private void detachDataSetListener()
    {
        if( dataSet != null && dataSetListener != null )
            dataSet.removeDataSetListener(dataSetListener);

        dataSet          = null;
        dataSetListener  = null;
        listenerAttached = false;
    }

    /** */
    @Override
    public void dispose() {
        tableView.getProperties().removeListener( mapChangeListener );
        detachDataSetListener();
        super.dispose();
    }

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {

        double tbW = 0.0;

        if( toolBar.isVisible() )
        {
            double pw = toolBar.prefWidth(-1);
            if( pw <= 0 )
                pw = toolBar.getMinWidth();

            tbW = Math.ceil(pw);
        }

        double sbH = 0.0;
        if( dsInfoBar.isVisible() )
        {
            double ph = dsInfoBar.prefHeight(-1);
            if( ph <= 0 )
                ph = dsInfoBar.getMinHeight();

            sbH = Math.ceil(ph);
        }

        if( toolBar.isVisible() )
            layoutInArea(toolBar, x, y, tbW, h, -1, HPos.LEFT, VPos.TOP);

        final double contentX = x + tbW;
        final double contentW = w - tbW;
        final double contentH = h - sbH;

        if( dsInfoBar.isVisible())
            layoutInArea( dsInfoBar, contentX, y + contentH, contentW, sbH, -1, HPos.CENTER, VPos.BOTTOM );

        super.layoutChildren( contentX, y, contentW, contentH);
    }


    /*
    @Override
    protected void layoutChildren(double x, double y, double w, double h)
    {

        Platform.runLater( () -> {
            DSFXAdapter dsAdapter = Controls.getDsAdapterFromControl(tableView);

            if( dsAdapter != null ) {
                if (dsAdapter.isEnableFilter()) {
                    toolBar.setVisible(true);

                    //Установка названия автофильтра в заголовок
                    if (firstInit && tableView instanceof JInvTable) {
                        final IDataSet dataSet = dsAdapter.getDataSet();
                        if (dataSet instanceof XXIDataSet && ((XXIDataSet) dataSet).isEnableAutoFilter()) {

                            boolean didSet = trySetFilterTitle( dataSet );

                            if ( !didSet ){
                                dataSet.addDataSetListener( e -> Platform.runLater( () -> {
                                    if ( e.isAfter() && e.getEventType() == DataSetEvent.DataSetEventType.FIRST_TIME_EXECUTE ){
                                        trySetFilterTitle( dataSet );
                                    }
                                } ) );
                            }

                        }
                        firstInit = false;
                    }
                }
            }
        } );

        StackPane placeholder = (StackPane) tableView.lookup(".placeholder");

        if( getSkinnable().isVisible() )
        {
            toolBar.setPrefHeight( h );
            super.layoutInArea   ( toolBar, x, y, toolBar.getWidth(), h, -1, HPos.CENTER, VPos.TOP );

            super.layoutInArea(placeholder, getX(x), y, getW(w), getH(h), -1, HPos.CENTER, VPos.TOP);
            super.layoutInArea(dsInfoBar, getX(x), y, getW(w), h, -1, HPos.CENTER, VPos.BOTTOM);
            super.layoutChildren(getX(x), y, getW(w), getH(h));
        } else {
            super.layoutChildren(x, y, w, h);
        }

        if (getTableHeaderRow() instanceof JInvTableHeaderRow )
        {
            ((JInvTableHeaderRow) getTableHeaderRow()).updateHeader();
        }

    }
    */

    /**
     * Волшебный трюк для того, чтобы крайний правый столбец не зажёвывался в больших таблицах
     * (баг JavaFX)
     */
    @Override
    protected VirtualFlow<TableRow<T>> createVirtualFlow() {
        return new VirtualFlow<TableRow<T>>(){
            @Override
            protected double getCellBreadth(Cell cell)
            {
                double base = super.getCellBreadth(cell);
                if( tableView.getColumnResizePolicy() != TableView.CONSTRAINED_RESIZE_POLICY )
                    return base + 1;

                return base;
            }
        };
    }
    private boolean trySetFilterTitle( final IDataSet<T> dataSet ) {

        final String filterName = dataSet.getProperty("auto_filter_name");

        if( !S.isNotNullOrEmpty( filterName ) )
            return false;

        final JInvFXFormController<?> controller = ((JInvTable<T>) tableView).getController();

        final String autoFilterName = MessageFormat.format( fore.getString("AUTO_FILTER_NAME"), filterName );

        final String suffix = " (" + autoFilterName + ")";
        final String title = controller.getTitle();

        if (title == null || !title.contains(suffix)) {
            controller.setTitle((title == null ? "" : title) + suffix);
        }

        return true;
    }

    /**
     * Получить статус бар
     * @return JInvToolBar
     */
    public DSInfoBar getStatusBar() {
        return dsInfoBar;
    }

    public JInvFilterToolBar<T> getFilterToolBar() {
        return toolBar;
    }
}
