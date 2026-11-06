package ru.inversion.fx.form.controls.sortbutton;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.slf4j.Logger;
import ru.inversion.dataset.ISQLDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.dataset.parser.OrderByParser;
import ru.inversion.fx.app.es.JInvErrorService;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.utils.S;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;
import static ru.inversion.fx.form.controls.JInvTableColumn.COLUMN_ORDERBY;
import static ru.inversion.fx.form.controls.sortbutton.JInvSortButton.SORT_BUTTON;
import static ru.inversion.fx.form.controls.sortbutton.JInvSortButton.SortStateEnum.NONE;

/**
 *
 * @author antonovdi
 *         Sulimoff
 */
public class OrderByManager<T> implements EventHandler<SortEvent<TableView<T>>> {

    final private DSFXAdapter<T> dsAdapter;

    private boolean active = false;

    public OrderByManager( DSFXAdapter<T> dsAdapter ) {

        this.dsAdapter = Objects.requireNonNull( dsAdapter, "'dsAdapter' is null");

        if( getTable() != null )
            getTable().setOnSort( this );
    }

    /** */
    public void buttonHandle( JInvSortButton bt, boolean shiftDown )
    {
        final TableColumn tc = bt.toTableColumn();
        final ObservableList< TableColumn< T, ? > > sortOrder = getTable().getSortOrder();

        boolean doSort = false;

        if( shiftDown )
        {
            if( bt.getSortMode() == NONE )
            {
                if(!sortOrder.isEmpty() )
                    sortOrder.remove(tc);
            }
            else
            {
                if( !sortOrder.contains(tc) )
                     sortOrder.add(tc);
                else
                    doSort = true;
            }
        }
        else
        {
            sortOrder.stream()
                .filter( (t)->t.getProperties().get(SORT_BUTTON) != null && t != tc )
                     .forEach((t)->((JInvSortButton)t.getProperties().get(SORT_BUTTON)).setSortMode(NONE) );

            active = false;
            sortOrder.clear();

            sortOrder.add(tc);
        }

        if( doSort )
            sort();
    }

    /** */
    private String prepareOrderBy( String expr, boolean desc ) {
        
        List<String> l = OrderByParser.parseAndGetColumnsList( expr );
        if( !l.isEmpty() ) {
             return l.stream().map( (s)-> s += ( desc ? " DESC" : " ASC" ) ).collect( Collectors.joining(",") );
        }
        return expr;
    }

    /** */
    public void sort( ) {
        internalSort( );
    }

    /** */
    private boolean internalSort( ) {

        try {

            if ( dsAdapter != null )
            {
                String orderBy = getOrderByFromTable();

                if( orderBy.length() > 0 )
                {
                    getDataSet().setOrderBy( orderBy );
//                    logger.trace("order by " + orderBy);
                }
                else
                {
                    getDataSet().setOrderBy( null );
//                    logger.trace("order by " + null);
                }

                dsAdapter.executeQuery();

                Platform.runLater(() -> {
                    getTable().getSortOrder().stream().findFirst().ifPresent(c -> {
                        getTable().scrollToColumn(c);
                    });
                });

                return true;
            }
        } catch ( Throwable ex ) {
            JInvErrorService.handleException( getTable().getScene().getWindow(), ex );
        }
        return false;
    }

    /** */
    private ISQLDataSet getDataSet() {
        return (ISQLDataSet) dsAdapter.getDataSet();
    }

    /** */
    private TableView<T> getTable() {
        return dsAdapter.getTable();
    }

    @Override
    public void handle( SortEvent<TableView<T>> event ) {

        if( active ) {
            active = false;
            return;
        }

//        if( !shiftDown )
//            clearButtons( );

        sort();

        event.consume();
    }

    /** */
    private String getColumnSortExpr( TableColumn tableColumn ) {

        TableColumn.SortType st = null;
        String s = null;

        if( tableColumn.hasProperties()  )
        {
            final JInvSortButton b = (JInvSortButton)tableColumn.getProperties().get(SORT_BUTTON);

            if( b != null )
            {
                st= b.getSortMode() == NONE ? null : b.getSortMode().toSortType();

                if( st != null )
                {
                    if( st == TableColumn.SortType.DESCENDING )
                        s = prepareOrderBy( b.getOrderBy(), true );
                    else
                        s = b.getOrderBy();
                }
            }
            else
            {
                s = (String)tableColumn.getProperties().get( COLUMN_ORDERBY );

                if( S.isNullOrEmpty(s) )
                    s = Controls.getFieldNameFromTableColumn( tableColumn );;

                st = tableColumn.getSortType();

                if( !S.isNullOrEmpty(s) && st == TableColumn.SortType.DESCENDING )
                     s +=  " DESC ";
            }
        }

        if( S.isNullOrEmpty(s) || st == null )
            return null;//S.EMPTY_STRING;

        return s;
    }


    /** */
    private String getOrderByFromTable() {

        return
            getTable()
                .getSortOrder()
                .stream()
                .map( (t)->getColumnSortExpr(t) )
                .filter(S::isNotNullOrEmpty)
                .collect(Collectors.joining(","));
    }
}
