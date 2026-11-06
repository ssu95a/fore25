package ru.inversion.fx.form.controls.filter;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import ru.inversion.dataset.*;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.property.PropertiesTypeEnum;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.ActionFactory.ActionTypeEnum;
import ru.inversion.fx.form.Alerts;
import ru.inversion.fx.form.ViewContext;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.action.JInvKeyboardManager;
import ru.inversion.fx.form.controls.JInvCheckBox;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.filter.impl.FilterManager;
import ru.inversion.icons.IconDescriptor;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.tc.TaskContext;

import java.util.ResourceBundle;
import java.util.function.Consumer;

import static ru.inversion.fx.app.BaseApp.APP;

/**
 *
 * @author Sulimoff, perov
 */
public class JInvFilterToolBar<P> extends JInvToolBar implements IFilterAction {

    final private static ResourceBundle BUNDLE_FILTER = ResourceBundle.getBundle("filter");

    private final JInvCheckBox chSavePrevMarkedRows = new JInvCheckBox();

    /** */
    private JInvTable<P> table;

    /** */
    private FilterManager filterManager;

    /**
     * Метод инициализации Toolbar
     *
     */
    public void init( JInvTable<P> table) {

        if( this.table != null )
            throw new IllegalStateException(Tags.PRODUCT_LABEL + "Table already init");

        this.table = table;

        initAction( );
    }

    private JInvTable<P> getTable() {
        return table;
    }

    /**
     * Инициализация кнопок
     */
    private void initAction() {

        chSavePrevMarkedRows.setToolTipText(BUNDLE_FILTER.getString("SavePrevMarkedRows"));
        chSavePrevMarkedRows
                .setStyle(
                        "-fx-border-width:  1;"
                        + "-fx-border-color:  lightgray;"
                        + "-fx-border-radius: 5;"
                        + "-fx-padding: 4;"
                        + "-fx-label-padding: 0.0em;");

        chSavePrevMarkedRows.setFocusTraversable(false);

        HBox box = new HBox(chSavePrevMarkedRows);
        box.setAlignment(Pos.CENTER);
        this.getItems().addAll(box, new Separator(Orientation.VERTICAL));

        chSavePrevMarkedRows.selectedProperty().bindBidirectional(table.savePrevMarkedRowsProperty());

        Button btnRefresh = ActionFactory.createButton(ActionTypeEnum.REFRESH, a -> {
            IAction action = JInvKeyboardManager.getAction(table, ActionTypeEnum.REFRESH);
            if (action != null) {
                JInvEvent<javafx.event.ActionEvent> wrapEvent = new JInvEvent<>(table, JInvEvent.PlaceType.FILTER, a);
                wrapEvent.setAction(action);
                action.handle(wrapEvent);
            }
        });

        Button btnFilter = (Button) ActionFactory.createButton(
                ActionTypeEnum.FILTER,
                null,
                BUNDLE_FILTER.getString("FILTER_BTN.SHOW_FILTER_PANE"),
                BUNDLE_FILTER.getString("FILTER_BTN.SHOW_FILTER_PANE_TOOLTIP"),
                a -> showFilterAction(),
                false,
                null,
                null);

        Button btnSaveFilter = (Button) ActionFactory.createButton(
                ActionTypeEnum.SAVE_FILE,
                null,
                BUNDLE_FILTER.getString("SAVE_FILTER"),
                BUNDLE_FILTER.getString("SAVE_FILTER_TOOLTIP"),
                a -> saveFilterAction(),
                false,
                null,
                null);

        Button btnExecuteFilter = (Button) ActionFactory.createButton(
                ActionTypeEnum.FILTER_EXEC,
                null,
                BUNDLE_FILTER.getString("FILTER_BTN.LOAD_FILTER"),
                BUNDLE_FILTER.getString("FILTER_BTN.LOAD_FILTER_TOOLTIP"),
                a -> executeFilterAction(),
                false,
                null,
                null);

        Button btnMarkFilter = (Button) ActionFactory.createButton(
                ActionTypeEnum.MARK,
                null,
                BUNDLE_FILTER.getString("FILTER_BTN.MARK_FILTER"),
                BUNDLE_FILTER.getString("FILTER_BTN.MARK_FILTER_TOOLTIP"),
                a -> markFilterAction(),
                false,
                null,
                null);

        Button btnClearFilter = (Button) ActionFactory.createButton(
                null,
                IconDescriptor.of(FontAwesome.fa_close),
                BUNDLE_FILTER.getString("FILTER_BTN.CLEAR_FILTER"),
                BUNDLE_FILTER.getString("FILTER_BTN.CLEAR_FILTER_TOOLTIP"),
                e -> clearFilterAction(),
                false,
                null,
                null
        );


        Button btnSettingsFilter = null;

        if (APP() != null && APP().getProperties(PropertiesTypeEnum.PRP)
                .getBooleanProperty("ru.inversion.form.table.allow_change_filter", false))
        {

            btnSettingsFilter = (Button) ActionFactory.createButton(
                    ActionTypeEnum.SETTINGS,
                    null,
                    BUNDLE_FILTER.getString("FILTER_BTN.SETTING_FILTER"),
                    BUNDLE_FILTER.getString("FILTER_BTN.SETTING_FILTER_TOOLTIP"),
                    e -> settingsFilterAction(),
                    false,
                    null,
                    null);
        }

        getItems().addAll(btnRefresh, btnFilter, new Separator(Orientation.VERTICAL),
                btnSaveFilter, btnExecuteFilter, btnMarkFilter,
                btnClearFilter);

        if (btnSettingsFilter != null) {
            getItems().addAll(
                    new Separator(Orientation.VERTICAL),
                    btnSettingsFilter
            );
        }

        // Устанавливаем размер шрифта и кнопок
        getItems().stream()
                .filter(n -> n instanceof Button)
                .forEach(this::setButtonStyle);

        this.getItems().addListener((ListChangeListener.Change<? extends Node> change) -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().stream()
                            .filter(n -> n instanceof Button)
                            .forEach((Consumer<Node>) this::setButtonStyle);
                }
            }
        });
    }

    private void setButtonStyle(Node b) {
        if (b instanceof Button) {
            b.pseudoClassStateChanged(PseudoClass.getPseudoClass("toolbar-vertical"), true);
            ((Button) b).setMaxWidth(Double.MAX_VALUE);
        }
    }

    /**
     * Сохранение пометок
     */
    public BooleanProperty savePrevMarkedRowsProperty() {
        return chSavePrevMarkedRows.selectedProperty();
    }

    @Override
    public void showFilterAction () {

        // KeyEvent.KEY_RELEASED - не запускает событие нажатия на F7
        KeyEvent newEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                null,
                "applyFilter",
                KeyCode.F7, false, false, false, false
        );
        //так как форма F7 фильтров открывается для таблицы, которая в фокусе,
        //принудительно ставим фокус на таблицу к которой привязан JInvFilterToolBar
        getTable().requestFocus();

        getTable().fireEvent( newEvent );
    }

//    @Override
//    public void showFilterAction() {
//
//        KeyEvent newEvent = new KeyEvent(
//                KeyEvent.KEY_PRESSED,
//                null,
//                "applyFilter",
//                KeyCode.F7, false, false, false, false
//        );
//
//        getTable().fireEvent(newEvent);
//    }

    @Override
    public void executeFilterAction() {

        try {

            if( filterManager == null )
                filterManager = new FilterManager( getTaskContext(), getViewContext() );

            filterManager.getAndExecuteFilter( table.getDataSetAdapter() );

        } catch (Exception ex) {
            JInvErrorService.handleException(null, ex);
        }

    }

    private TaskContext getTaskContext() {
        return getTable().getDataSetAdapter().getTaskContext();
    }

    private ViewContext getViewContext() {
        return getTable().getController().getViewContext();
    }

    @Override
    public void saveFilterAction() {

        try {

            if (filterManager == null) {
                filterManager = new FilterManager(
                        getTaskContext(),
                        getViewContext()
                );
            }

            if (table.getDataSetAdapter().getDataSet() instanceof ISQLDataSet) {
                filterManager.saveCurrentFilter(
                        ((ISQLDataSet) table.getDataSetAdapter().getDataSet()).getFilter(ISQLDataSet.FilterTypeEnum.TEMPORARY, ISQLDataSet.FilterTypeEnum.FIXED),
                        getViewContext().getFormNameForFilter(),
                        getTable().getDataSetAdapter().getDataSet().getName()
                );
            }
        } catch (Exception ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    @Override
    public void markFilterAction() {

        try {

            IDataSet dataSet = table.getDataSetAdapter().getDataSet();

            if (dataSet != null && dataSet instanceof XXIDataSet) {
                XXIDataSet xds = (XXIDataSet) dataSet;

                if (!xds.hasMarkedRows()) {
                    Alerts.info(getScene().getWindow(), null, BUNDLE_FILTER.getString("NET_POMECHENNYH_ZAPISEJ_FILTR_NE_USTANOVLEN"));
                } else {
                    xds.saveMark2Filter();
                    xds.executeQuery();
                }
            }

        } catch (Exception ex) {
            JInvErrorService.handleException(getScene().getWindow(), ex);
        }

    }

    @Override
    public void clearFilterAction() {

        IDataSet dataSet = table.getDataSetAdapter().getDataSet();

        if (dataSet != null && dataSet instanceof ISQLDataSet) {
            ISQLDataSet sds = (ISQLDataSet) dataSet;
            sds.clearFilter(true);
            //Решили всегда очищать
            sds.clear();
        }
    }

    @Override
    public void settingsFilterAction() {
        try {
            if (filterManager == null) {
                filterManager = new FilterManager(getTaskContext(), getViewContext());
            }
            filterManager.settingsFilter(getViewContext().getFormNameForFilter(), getTable().getDataSetAdapter().getDataSet());
        } catch (Exception ex) {
            JInvErrorService.handleException(null, ex);
        }
    }

    /**
     *
     */
    public void enablePrevMarkedRows(Boolean show) {
        chSavePrevMarkedRows.setVisible(show);
    }
}
