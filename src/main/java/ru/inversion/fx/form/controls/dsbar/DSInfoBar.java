package ru.inversion.fx.form.controls.dsbar;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.TableView;
import ru.inversion.dataset.IXXIDataSet;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.fx.form.controls.JInvTable;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.table.toolbar.AggregatorType;
import ru.inversion.utils.U;

import java.util.*;
import java.util.stream.Collectors;

import static ru.inversion.fx.form.controls.dsbar.AbstractPartBase.PART;
import static ru.inversion.fx.form.controls.dsbar.DSInfoBar.PartEnum.*;
import static ru.inversion.fx.form.controls.table.toolbar.AggregatorType.*;

/**
 *
 * @author sulimoff
 */
public class DSInfoBar extends JInvToolBar {

    final private static ResourceBundle g_bundle = ResourceBundle.getBundle("fore");

    private ResourceBundle bundle;

    /** */
    public enum PartEnum {

        RowNum,
        MarkButtons,
        Mark,
        Filter,
        Total,
        CellSum,
        FindById;

        static public int compare( PartEnum p1, PartEnum p2 ) {

            if( p1 != null && p2 != null )
                return p1.ordinal() - p2.ordinal();

            if( p1 == null && p2 == null )
                return 0;

            if( p1 != null )
                return 1;

            return -1;
        }
    };

    /** */
    private TableView table;

    /** */
    private DSFXAdapter adapter;

    /** */
    private boolean constructed = false;

    /** */
    final private List<IDSBarPart> partList = new ArrayList<>();

    /** */
    final private Set<PartEnum> disablePartSet = new TreeSet<>();

    /** */
    public DSInfoBar( ) {
        this.visibleProperty().addListener( ( observable, oldValue, newValue ) -> {
            if( newValue ) {
                doConstruct( );
                partList.forEach( IDSBarPart::onVisible );
            }
            else {
                partList.forEach( IDSBarPart::onHide );
            }
        } );
    }

    /** */
    public ResourceBundle getBundle() {
        return bundle;
    }

    public void setBundle( ResourceBundle bundle ) {
        this.bundle = bundle;
    }

    /** */
    private String getBundleString( String key ) {

        if( bundle != null && bundle.containsKey(key) )
            return bundle.getString(key);

        return g_bundle.getString(key);
    }

    /** */
    private boolean isEnableMark( ) {
        return adapter != null && adapter.isEnableMark();
    }

    /**
     *
     */
    public void init( TableView table ) {

        if( table == null )
            throw new IllegalArgumentException("Table is null");

        this.setStyle("-fx-background-color: -fx-base;");

        this.table = table;

        getItems().add( new Part_RowNum(table, bundle).createControlPane() );
    }

    /** */
    public void init( DSFXAdapter adapter ) {

        if( adapter == null )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'adapter' is null");

        if( !(adapter.getDataSet() instanceof IXXIDataSet) )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'adapter' DataSet must be instanceof 'IXXIDataSet'");

        if( this.adapter != null )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'adapter' DataSet is already initialized");

//        if( !adapter.isEnableMark() )
//            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "Adapter must be 'isEnableMark is true'");

        this.setStyle("-fx-background-color: -fx-background; -fx-padding: 0 0 0 0; ");

        this.adapter = adapter;

        if( adapter.isEnableMark() )
            getItems().add( new Part_MarkButtons(adapter, bundle).createControlPane( ) );
    }

    /** */
    private DSFXAdapter getAdapter( ) {

        if( adapter == null )
            adapter = ( table instanceof JInvTable ) ? ((JInvTable)table).getDataSetAdapter() : null;

        return adapter;
    }

    /** Получить Part по её типу */
    public IDSBarPart getPart( PartEnum part ) {
        for( IDSBarPart p : partList ) {
            if( p.getType() == part )
                return p;
        }
        return null;
    }

    /** */
    private Node getPartNode( PartEnum part ) {
        for( IDSBarPart p : partList ) {
            if( p.getType() == part )
                return p.getControlPane();
        }
        return null;
    }

    /** */
    public void enablePart( boolean enable, PartEnum ... parts ) {

        if( parts == null || parts.length == 0 )
            return;

        if( !enable ) {

            for( PartEnum p : parts ) {

                if( disablePartSet.contains(p) )
                    continue;

                if( this.constructed )
                    this.getItems().remove( getPartNode(p) );

                disablePartSet.add(p);
            }//end if
        }//end if
        else {

            for( PartEnum p : parts ) {

                if( !disablePartSet.contains(p) )
                    continue;

                if( this.constructed ) {

                    this.getItems().add ( getPartNode(p) );

                    this.getItems().sort( (Node o1, Node o2)->PartEnum.compare( (PartEnum)o1.getProperties().get(PART), (PartEnum)o2.getProperties().get(PART) ));

                    disablePartSet.remove(p);
                }//end if
            }
        }
    }

    /**
     Возвращает поля агрегаторов
     */
    public List<? extends Control> getControls(){
        return aggPartList == null ? new ArrayList<>() :
                aggPartList.stream()
                        .filter( Objects::nonNull )
                        .flatMap( x -> x.getControls().stream() )
                        .collect( Collectors.toList());
    }
    /**
     Возвращает первый контрол агрегатора по колонке, типу и функции
     */
    public JInvNumberField getControl(String column, AggrFuncEnum func, AggregatorType type){

        if ( aggPartList == null || aggPartList.size() < type.ordinal()+1 || aggPartList.get(type.ordinal()) == null ) {
            return null;
        }
        return aggPartList.get( type.ordinal() ).getControl(func, column);
    }

    /** */
    public Object getAggregatorValue( AggregatorType type, String column ) {

        if( aggPartList == null )
            return null;

        AbstractPartBase pa = aggPartList.get( type.ordinal() );

        if( pa == null )
            return null;

        return pa.getValue( column );
    }

    /** */
    public boolean isEnablePart( PartEnum part ) {
        return !disablePartSet.contains(part);
    }

    private List<AbstractAggregatorPart> aggPartList;

    /**
     Добавить новый агрегатор, вернуть его контрол.
     Идентичен addAggregator
     */
    public JInvNumberField addNewAggregator( String column, AggrFuncEnum func, String alias, AggregatorType type, String table, String toolTip ) {

        if( aggPartList == null ) {
            aggPartList = new ArrayList<>();
            aggPartList.add(null); aggPartList.add(null); aggPartList.add(null);
        }

        AbstractAggregatorPart pa = aggPartList.get( type.ordinal() );

        if( pa == null ) {

            if( type == MARK ) {

                Part_AggregatorMark p = new Part_AggregatorMark( getAdapter(), bundle );
                pa = p;
            }
            else {
                Part_Aggregator p = new Part_Aggregator( U.decode( type, MARK, Mark, FILTER, Filter, TOTAL, Total ), getAdapter(), bundle );
                pa = p;
            }

            aggPartList.set( type.ordinal(), pa );
        }

        pa = aggPartList.get( type.ordinal() );

        if( pa instanceof Part_AggregatorMark ) {
            Part_AggregatorMark p = (Part_AggregatorMark)pa;
            return p.addAggregator( column, func, alias, toolTip, table );
        }
        else {
            Part_Aggregator p = (Part_Aggregator)pa;
            return p.addAggregator( column, func, alias, toolTip );
        }
    }

    /**
     Добавить новый агрегатор.
     Идентичен addNewAggregator, но не возвращает контрол
     */
    public void addAggregator( String column, AggrFuncEnum func, AggregatorType type, String table, String toolTip ) {
        addAggregator( column, func, null, type, table, toolTip );
    }

    /**
     Добавить новый агрегатор.
     Идентичен addNewAggregator, но не возвращает контрол
     */
    public void addAggregator( String column, AggrFuncEnum func, String alias, AggregatorType type, String table, String toolTip ) {
        addNewAggregator( column, func, alias, type, table, toolTip );
    }

    /** */
    public boolean setAggregatorPredicate( AggregatorType type, String predicate )
    {
        if( aggPartList == null || aggPartList.get( type.ordinal() ) == null || constructed )
            return false;

        AbstractAggregatorPart pa = aggPartList.get( type.ordinal() );
        pa.setWherePredicate(predicate);

        return true;
    }

    /** */
    private void doConstruct( ) {

        if( constructed )
            return;

        final ObservableList<Node> items = this.getItems();

        boolean forTable = table != null;

        if( true ) {

            if( aggPartList == null ) {

                DSFXAdapter ad = getAdapter();

                if( ad != null && ad.isEnableMark() )
                    addAggregator( "1", AggrFuncEnum.COUNT, MARK, null, g_bundle.getString("COUNT_MARKED") );

            }//end if

        }//end if

        if( aggPartList != null ) {

            for( AbstractPartBase pa : aggPartList ) {

                if( pa != null )
                    partList.add( pa );

            }//end for

        }//end if

        if( forTable && !disablePartSet.contains( CellSum ) )
            partList.add( new Part_TableCellsSum( table, bundle ) );

        for( IDSBarPart p : partList ) {

            p.createControlPane( );

            if( !disablePartSet.contains( p.getType() ) )
                 items.add( p.getControlPane() );
        }

        constructed = true;

        final DSFXAdapter a = getAdapter();
        if( a != null )
            this.disableProperty().bind( a.emptyProperty() );
    }

    /** */
    public void build( ) {
        if( table == null ) {
            this.visibleProperty().set(false);
            this.visibleProperty().set(true );
        }
    }

    /** */
    public void recalculate() {

        if( !constructed )
            return;
        partList.forEach( IDSBarPart::recalculate );
    }
}
