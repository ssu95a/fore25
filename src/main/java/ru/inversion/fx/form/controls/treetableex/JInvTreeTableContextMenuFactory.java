package ru.inversion.fx.form.controls.treetableex;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Window;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.app.sec.JInvSecurityService;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.JInvFEDialog;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.controls.JInvMenuItem;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvTableColumn;
import ru.inversion.icons.IconDescriptorBuilder;
import ru.inversion.icons.IconFactory;
import ru.inversion.icons.RowIconDescriptorBuilder;
import ru.inversion.icons.enums.FontAwesome;
import ru.inversion.tc.TaskContext;
import ru.inversion.tds.ITreeDataSetItem;
import ru.inversion.utils.S;
import ru.inversion.utils.converter.TypeConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static ru.inversion.fx.form.controls.treetableex.TSFXAdapter.PROPERTY_TITLE_EXTRACTOR;

/**
 *
 * @author perov
 */
public class JInvTreeTableContextMenuFactory {

    final private static ResourceBundle g_foreBundle = ResourceBundle.getBundle("fore");

    /** */
    private static String getTitle( ActionFactory.ActionTypeEnum actionType ) {
        switch ( actionType ) {
            case FE:
                return g_foreBundle.getString( "OTKRYT_V_DIALOGOVOM_OKNE" );
            case CLONE:
                return g_foreBundle.getString( "TABLE_CONTEXT_MENU.COPY" );
            case COPY:
                return g_foreBundle.getString( "TABLE_CONTEXT_MENU.COPY_ROW" );
            case EXPORT:
                return g_foreBundle.getString( "TABLE_CONTEXT_MENU.EXPORT" );
            case FILTER:
                return g_foreBundle.getString( "PANEL_FILTER" );
            case EXPAND:
                return g_foreBundle.getString( "TREE_TABLE_EXPAND" );
            case COLLAPSE:
                return g_foreBundle.getString( "TREE_TABLE_COLLAPSE" );
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

    /** */
    private static MenuItem createShowCellMenuItem( final TreeTableView table ) {
        return createMenuItem (
            ActionFactory.ActionTypeEnum.FE,
            (ActionEvent event) ->
                    getSelectedCellValue( table ).ifPresent( cv -> {
                        String data = TypeConverter.convert( cv, String.class );
                        Window window = tryGetWindow( table );
                        new JInvFEDialog( window, data ).showAndWait();
            }) );
    }

    private static Window tryGetWindow( final Node table ) {
        final Scene scene = table.getScene();
        Window window = null;
        if ( scene != null ){
            window = scene.getWindow();
        }
        return window;
    }

    /** */
    private static MenuItem createCopyCellMenuItem( TreeTableView tree ) {

        return createMenuItem (
                ActionFactory.ActionTypeEnum.CLONE,
                (ActionEvent event) ->
                        getSelectedCellValue( tree ).ifPresent( cv -> {
                        String data = TypeConverter.convert( cv, String.class );
                        saveToClipboard(data);
                } ) );
    }

    private static Optional<Object> getSelectedCellValue( final TreeTableView tree ) {

        Optional<Object> cellValue = Optional.empty();

        if( tree.getSelectionModel().getSelectedCells().size() > 0)
        {
            final TreeTablePosition pos = tree.getFocusModel().getFocusedCell();
            return Optional.ofNullable( pos.getTableColumn().getCellData( pos.getTreeItem() ) );

            /*
            int row = pos.getRow();

            // Если у колонки есть родители, то значение нужно брать из этой колонки
            if( pos.getTableColumn() != null && pos.getTableColumn().getParentColumn() != null )
            {
                return Optional.ofNullable( pos.getTreeItem() );
                //return Optional.ofNullable(pos.getTableColumn().getCellData( pos.getTreeItem() ) );
            }

            TreeTableColumn col = (TreeTableColumn) tree.getColumns().get( tree.indexClikedColumnProperty().get() );

            if (col != null)
            {
                cellValue = Optional.ofNullable( col.getCellObservableValue( tree.getItems().get( row ) ).getValue() );
            }
             */
        }
        return cellValue;
    }

    /** */
    private static MenuItem createCopyRowMenuItem( JInvTreeTableEx tree ) {

        return createMenuItem (
            ActionFactory.ActionTypeEnum.COPY,
            (ActionEvent event) ->
            {
                final TreeTableView.TreeTableViewSelectionModel selectionModel = tree.getSelectionModel();
                final String separ = System.getProperty("line.separator");

                if( selectionModel.getSelectedItem() != null ) {

                    final StringBuilder sb = new StringBuilder();

                    for ( Object column : tree.getLeafColumns() )
                    {
                        if( column instanceof TreeTableColumn )
                        {
                            TreeTableColumn tableColumn = (TreeTableColumn)column;
//                            final JInvTableColumn tableColumn = (JInvTableColumn) column;

//                            final javafx.scene.control.TreeTableView.TreeTableViewSelectionModel selectionModel = tree.getSelectionModel();
//                            final JInvTableColumn tableColumn = (JInvTableColumn) column;

                            // Проверяем есть ли наследники у колонки и добавляем их
                            /*
                            ObservableList childColumns = tableColumn.getColumns().filtered( tree.getColumnFilter() );
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
                            */

                            if( !S.isNullOrEmpty( tableColumn.getText() ) )
                            {
                                sb.append( tableColumn.getText() ).append(": ");
                                final Object cellData = tableColumn.getCellData( selectionModel.getSelectedItem() );
                                appendCellData(sb, cellData);
                                sb.append( separ );
                            }
                        }
                    }//end if
                    saveToClipboard( sb.toString() );
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

    /*
    private static MenuItem createFilterMenuItem( JInvTreeTableEx tree ) {

        if( tree.getDataSetAdapter() != null && !( tree.getDataSetAdapter().getDataSet() instanceof ISQLDataSet ) )
            return null;

        CheckMenuItem menuItem = new CheckMenuItem();
        menuItem.setText    ( getTitle( ActionFactory.ActionTypeEnum.FILTER ) );
        menuItem.setGraphic ( IconFactory.getLabel( ActionFactory.ActionTypeEnum.FILTER.getAction().getIcon() ) );

        if( tree.getDataSetAdapter() != null && tree.getDataSetAdapter().isEnableFilter()) {
            menuItem.selectedProperty().bindBidirectional( tree.getFilterToolbar().visibleProperty() );
            menuItem.selectedProperty().bindBidirectional( tree.getDataSetAdapter().enableToolbarFilterProperty() );
        }
        else
            menuItem.setDisable(false);

        return menuItem;
    }
    */

    /**
     * Скрываем/показываем статус бар
     */
    private static MenuItem createStatusBarMenuItem( JInvTreeTableEx table ) {
        CheckMenuItem menuItem = new CheckMenuItem();
        menuItem.setText    ( g_foreBundle.getString("STATUS_BAR") );
        menuItem.setGraphic ( IconFactory.getLabel(FontAwesome.fa_window_minimize) );
        menuItem.selectedProperty().bindBidirectional( table.visibleStatusBarProperty() );
        return menuItem;
    }

    /**
     * Развернуть узел
     */
    private static MenuItem createExpandMenuItem( JInvTreeTableEx tree ) {
        return createMenuItem (
            ActionFactory.ActionTypeEnum.EXPAND,
            (ActionEvent event) -> tree.expandCurrentItem()
        );
    }

    private static MenuItem createExpandAllMenuItem( JInvTreeTableEx tree ) {

        MenuItem menuItem = new JInvMenuItem( );
        menuItem.setText    ( "Развернуть все" );
        menuItem.setGraphic (
            IconFactory.getLabel(
                new RowIconDescriptorBuilder().add(FontAwesome.fa_expand).add(FontAwesome.fa_asterisk).padding(2).build()
            )
        );
        menuItem.setOnAction( (e)->tree.expandAll() );

        return menuItem;
    }

    private static MenuItem createCollapseAllMenuItem( JInvTreeTableEx tree ) {

        MenuItem menuItem = new JInvMenuItem( );
        menuItem.setText    ( "Свернуть все" );
        menuItem.setGraphic (
            IconFactory.getLabel(
                    new RowIconDescriptorBuilder().add(FontAwesome.fa_compress).add(FontAwesome.fa_asterisk).padding(2).build()
            )
        );
        menuItem.setOnAction( (e)->tree.collapseAll() );

        return menuItem;
    }

    private static MenuItem createCollapseMenuItem( JInvTreeTableEx tree ) {

        return createMenuItem (
            ActionFactory.ActionTypeEnum.COLLAPSE,
            (ActionEvent event) -> tree.collapseCurrentItem()
        );
    }

    /** */
    private static MenuItem createRefreshMenuItem( JInvTreeTableEx tree ) {

        return createMenuItem (
            ActionFactory.ActionTypeEnum.REFRESH,
            (ActionEvent event) -> tree.getTSFXAdapter().executeQuery()
        );
    }

    /** */
    private static <P> MenuItem createCopyPathMenuItem( JInvTreeTableEx tree ) {

        MenuItem menuItem = new JInvMenuItem( );
        menuItem.setText    ( "Путь до узла" );
        menuItem.setGraphic (
                IconFactory.getLabel(
                        new IconDescriptorBuilder().iconId(FontAwesome.fa_terminal).build()
                )
        );
        menuItem.setOnAction(
            (e)-> {

                final ITreeDataSetItem<P> selectedItem = (ITreeDataSetItem<P>)tree.getSelectionModel().getSelectedItem();

                if( selectedItem != null )
                {
                    Function<P,String> titleExtractor = tree.getProperty( PROPERTY_TITLE_EXTRACTOR );
                    final String s;
                    if( titleExtractor == null )
                        s = selectedItem.getPath().toString();
                    else
                        s = selectedItem.getPath().toString(titleExtractor,null);

                    saveToClipboard(s);
                }
            }
        );

        return menuItem;
    }

    /**
    private static MenuItem createTableSettingsMenuItem( JInvTreeTable table ) {

        return createMenuItem( SETTINGS,
                (ActionEvent event) ->
                {
                    JInvTableSettingDialog d = new JInvTableSettingDialog(table);
                    d.initOwner( table.getScene().getWindow() );
                    d.showAndWait();
                }
                );
    }
    */

    /** */
    public static <P> Menu createTreeTableMenu( JInvTreeTableEx tree ) {

        Menu tableMenu = new Menu( g_foreBundle.getString("TREETABLE_CONTEXT_MENU"), IconFactory.getLabel( FontAwesome.fa_table ) );

        tableMenu.getItems().addAll (
            createExpandMenuItem     ( tree ),
            createCollapseMenuItem   ( tree ),
            createExpandAllMenuItem  ( tree ),
            createCollapseAllMenuItem( tree ),
            new SeparatorMenuItem( ),
            createRefreshMenuItem    ( tree ),
            new SeparatorMenuItem( ),
            createShowCellMenuItem   ( tree ),
            createCopyCellMenuItem   ( tree ),
            createCopyRowMenuItem    ( tree ),
            createCopyPathMenuItem   ( tree )
        );
/*
        MenuItem exportDataMenuItem = createExportDataMenuItem(table);

        if( exportDataMenuItem != null )
            tableMenu.getItems().addAll( new SeparatorMenuItem(), exportDataMenuItem );
        MenuItem filterMenuItem = createFilterMenuItem(tree);

        if( filterMenuItem != null )
            tableMenu.getItems().addAll( new SeparatorMenuItem(), filterMenuItem );
*/

        tableMenu.getItems().addAll( new SeparatorMenuItem(), createStatusBarMenuItem( tree ) );

        /*
        if( tree.getController() != null )
        {
            final MenuItem tableSettingsMenuItem = createTableSettingsMenuItem(tree);
            tableMenu.getItems().addAll( new SeparatorMenuItem(), tableSettingsMenuItem );
        }
        */
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
