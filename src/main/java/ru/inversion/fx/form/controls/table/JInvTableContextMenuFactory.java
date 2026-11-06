package ru.inversion.fx.form.controls.table;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Window;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.JInvFEDialog;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.controls.JInvMenuItem;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.tc.TaskContext;
import ru.inversion.utils.converter.TypeConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

import static ru.inversion.fx.form.ActionFactory.ActionTypeEnum.SETTINGS;

/**
 *
 * @author perov
 */
public class JInvTableContextMenuFactory {

    final private static ResourceBundle G_BUNDLE = ResourceBundle.getBundle("fore");

//    /** */
//    public enum MenuItemEnum {
//
//        COPY       ( "TABLE_CONTEXT_MENU.COPY",     IconEnum.fa_ellipsis_h ),
//        COPY_ROW   ( "TABLE_CONTEXT_MENU.COPY_ROW", IconEnum.fa_minus      ),
//        EXPORT_DATA( "TABLE_CONTEXT_MENU.COPY_ROW", ActionFactory.ActionTypeEnum.EXPORT )
//
//        private final String   title;
//        private final IconEnum icon;
//
//        private MenuItemEnum( String nameKey, IconEnum icon ) {
//            this.icon  = icon;
//            this.title = G_BUNDLE.getString(nameKey);
//        }
//
//        public String getTitle() {
//            return title;
//        }
//
//        public IconEnum getIcon() {
//            return icon;
//        }
//    }

    /** */
    private static String getTitle( ActionFactory.ActionTypeEnum actionType ) {
        switch ( actionType ) {
            case FE:
                return G_BUNDLE.getString( "OTKRYT_V_DIALOGOVOM_OKNE" );
            case CLONE:
                return G_BUNDLE.getString( "TABLE_CONTEXT_MENU.COPY" );
            case COPY:
                return G_BUNDLE.getString( "TABLE_CONTEXT_MENU.COPY_ROW" );
            case EXPORT:
                return G_BUNDLE.getString( "TABLE_CONTEXT_MENU.EXPORT" );
            case FILTER:
                return G_BUNDLE.getString( "PANEL_FILTER" );
            default:
                return actionType.getName();
        }
    }


    /** */
    private static MenuItem createMenuItem( ActionFactory.ActionTypeEnum actionType, EventHandler<ActionEvent> handler ) {

        MenuItem menuItem = new JInvMenuItem( );
        menuItem.setText    ( getTitle( actionType ) );
        menuItem.setGraphic ( IconFactory.getLabel( actionType.getAction().getIcon() ) );
        menuItem.setOnAction( handler );

        return menuItem;
    }

    private static MenuItem createShowCellMenuItem( final JInvTable table ) {
        return createMenuItem (
            ActionFactory.ActionTypeEnum.FE,
            (ActionEvent event) ->
                    getSelectedCellValue( table ).ifPresent( cv -> {
                        String data = TypeConverter.convert( cv, String.class );
                        Window window = tryGetWindow( table );
                        new JInvFEDialog( window, data ).showAndWait();
            }) );
    }

    private static Window tryGetWindow( final JInvTable table ) {
        final Scene scene = table.getScene();
        Window window = null;
        if ( scene != null ){
            window = scene.getWindow();
        }
        return window;
    }

    /** */
    private static MenuItem createCopyCellMenuItem( JInvTable table ) {

        return createMenuItem (
                ActionFactory.ActionTypeEnum.CLONE,
                (ActionEvent event) ->
                        getSelectedCellValue( table ).ifPresent( cv -> {
                        String data = TypeConverter.convert( cv, String.class );
                        saveToClipboard(data);
                } ) );
    }

    private static Optional<Object> getSelectedCellValue( final JInvTable table ) {
        Optional<Object> cellValue = Optional.empty();
        if( table.getSelectionModel().getSelectedCells().size() > 0) {
            TablePosition pos = table.getFocusModel().getFocusedCell();

            int row = pos.getRow();

            // Если у колонки есть родители, то значение нужно брать из этой колонки
            if (pos.getTableColumn() != null && pos.getTableColumn().getParentColumn() != null) {
                return Optional.ofNullable(pos.getTableColumn().getCellData(table.getItems().get(row)));
            }

            TableColumn col = (TableColumn) table.getColumns().get( table.indexClickedColumnProperty().get() );

            if (col != null)
            {
                cellValue = Optional.ofNullable( col.getCellObservableValue( table.getItems().get( row ) ).getValue() );
            }
        }
        return cellValue;
    }

    /** */
    private static MenuItem createCopyRowMenuItem( JInvTable table ) {

        return createMenuItem (
            ActionFactory.ActionTypeEnum.COPY,
            (ActionEvent event) ->
            {
                if( table.getSelectionModel().getSelectedItem() != null ) {
                    final StringBuilder sb = new StringBuilder();

                    ObservableList columns = table.getColumnsFiltered();

                    for ( int i = 0; i < columns.size(); i++) {
                        final Object column = columns.get(i);
                        if ( columns.get(i) instanceof JInvTableColumn) {
                            final TableView.TableViewSelectionModel selectionModel = table.getSelectionModel();
                            final JInvTableColumn tableColumn = (JInvTableColumn) column;

                            // Проверяем есть ли наследники у колонки и добавляем их
                            ObservableList childColumns = tableColumn.getColumns().filtered( table.getColumnFilter() );
                            if (!childColumns.isEmpty()) {
                                sb.append(tableColumn.getText()).append(System.getProperty("line.separator"));
                                for (Object o : childColumns ) {
                                    final JInvTableColumn childColumn = (JInvTableColumn) o;
                                    sb.append('\t').append(childColumn.getText()).append(": ");
                                    final Object cellData = childColumn.getCellData(selectionModel.getFocusedIndex());
                                    appendCellData(sb, cellData);
                                    if (i != columns.size() - 1) {
                                        sb.append(System.getProperty("line.separator"));
                                    }
                                }
                                continue;
                            }

                            sb.append(tableColumn.getText()).append(": ");
                            final Object cellData = tableColumn.getCellData(selectionModel.getFocusedIndex());
                            appendCellData(sb, cellData);
                            if (i != columns.size() - 1) {
                                sb.append(System.getProperty("line.separator"));
                            }
                        }
                    }
                    saveToClipboard(sb.toString());
                    //saveToClipboard( table.getSelectionModel().getSelectedItem().toString() );
                }
        });
    }

    private static IAction g_exportAction = new IAction(){
        @Override
        public void handle( ActionEvent event ) {

            JInvTable table = (JInvTable)event.getSource();

            if( JInvSecurityService.isCanAccessIsAction( table.getDataSetAdapter().getTaskContext(), 3691 ) )
                ((JInvTable)event.getSource()).showExportDialog();
        }

        @Override
        public void setEnabled( boolean val ) {
        }
        @Override
        public boolean isEnabled() {
            return true;
        }
        @Override
        public void handle() {
            handle(null);
        }
        @Override
        public IAction getNextAction() { return null;}
        @Override
        public void setNextAction( IAction actionAfter ) { }

        @Override
        public boolean isEnabledBySecurity() {
            return true;
        }
    };

    /** */
    private static MenuItem createExportDataMenuItem( JInvTable table ) {

        if( table.getDataSetAdapter() == null )
            return null;

        TaskContext taskContext = table.getDataSetAdapter().getTaskContext();

        if( taskContext == null )
            return null;

        ActionFactory.ActionTypeEnum exportAction = ActionFactory.ActionTypeEnum.EXPORT;

        MenuItem menuItem = new JInvMenuItem( );
        menuItem.setText    ( getTitle( exportAction ) );
        menuItem.setGraphic ( IconFactory.getLabel( exportAction.getAction().getIcon() ) );
        menuItem.setOnAction(
            JInvSecurityService.<Throwable>wrapSecAction( table.getDataSetAdapter().getTaskContext(),
            3691,
            new IAction() {
                @Override
                public void handle( ActionEvent event ) {
                    table.showExportDialog();
                }
                @Override
                public void setEnabled( boolean val ) {
                }
                @Override
                public boolean isEnabled() {
                    return true;
                }
                @Override
                public void handle() {
                    handle(null);
                }
            },
            throwable -> JInvErrorService.handleException( table.getScene().getWindow(), throwable ) )
        );

        return menuItem;
    }

    /** */
    private static MenuItem createFilterMenuItem( JInvTable table ) {

        if(
            table.getDataSetAdapter() == null
            ||
            (
               !( table.getDataSetAdapter().getDataSet() instanceof ISQLDataSet )
               ||
               !table.getDataSetAdapter().isEnableFilter()
            )
        )
            return null;

        CheckMenuItem menuItem = new CheckMenuItem();
        menuItem.setText    ( getTitle( ActionFactory.ActionTypeEnum.FILTER ) );
        menuItem.setGraphic ( IconFactory.getLabel( ActionFactory.ActionTypeEnum.FILTER.getAction().getIcon() ) );

        if( table.getDataSetAdapter() != null && table.getDataSetAdapter().isEnableFilter()) {
            menuItem.selectedProperty().bindBidirectional( table.getFilterToolbar().visibleProperty() );
            menuItem.selectedProperty().bindBidirectional( table.getDataSetAdapter().enableToolbarFilterProperty() );
        }
        else
            menuItem.setDisable(false);

        return menuItem;
    }
    /**
     * Скрываем/показываем статус бар
     */
    private static MenuItem createStatusBarMenuItem( JInvTable table ) {

        CheckMenuItem menuItem = new CheckMenuItem();
        menuItem.setText    ( G_BUNDLE.getString("STATUS_BAR") );
        menuItem.setGraphic ( IconFactory.getLabel(FontAwesome.fa_window_minimize) );
        menuItem.selectedProperty().bindBidirectional( table.visibleStatusBarProperty() );

        return menuItem;
    }

    /** */
    private static MenuItem createTableSettingsMenuItem( JInvTable table ) {

        return createMenuItem( SETTINGS,
                (ActionEvent event) ->
                {
                    JInvTableSettingDialog d = new JInvTableSettingDialog(table);
                    d.initOwner( table.getScene().getWindow() );
                    d.showAndWait();
                }
                );
    }

    /** */
    public static Menu createTableMenu( JInvTable table ) {

        Menu tableMenu = new Menu( G_BUNDLE.getString("TABLE_CONTEXT_MENU"), IconFactory.getLabel( FontAwesome.fa_table ) );

        tableMenu.getItems().addAll (
            createShowCellMenuItem  ( table ),
            createCopyCellMenuItem  ( table ),
            createCopyRowMenuItem   ( table )
        );

        MenuItem exportDataMenuItem = createExportDataMenuItem(table);

        if( exportDataMenuItem != null )
            tableMenu.getItems().addAll( new SeparatorMenuItem(), exportDataMenuItem );

        MenuItem filterMenuItem = createFilterMenuItem(table);

        if( filterMenuItem != null )
            tableMenu.getItems().addAll( new SeparatorMenuItem(), filterMenuItem );

        tableMenu.getItems().addAll( new SeparatorMenuItem(),
                                     createStatusBarMenuItem( table ) );

        if( table.getController() != null )
        {
            final MenuItem tableSettingsMenuItem = createTableSettingsMenuItem(table);
            tableMenu.getItems().addAll( new SeparatorMenuItem(), tableSettingsMenuItem );
        }

        return tableMenu;
    }

    /** */
    static private void saveToClipboard( String s ) {

        final Clipboard        clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content   = new ClipboardContent();

        content.putString(s.replaceAll("\r\n", "\n"));

        clipboard.setContent(content);
    }

    private static void appendCellData(final StringBuilder sb, final Object cellData) {
        if (cellData instanceof LocalDate) {
            final String cellDate = ((LocalDate) cellData).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            sb.append(cellDate);
        } else {
            sb.append(cellData != null ? cellData : "");
        }
    }
}
