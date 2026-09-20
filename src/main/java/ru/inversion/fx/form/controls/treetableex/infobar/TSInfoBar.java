package ru.inversion.fx.form.controls.treetableex.infobar;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Control;
import ru.inversion.dataset.aggr.AggrFuncEnum;
import ru.inversion.fx.app.Tags;
import ru.inversion.fx.form.controls.JInvNumberField;
import ru.inversion.fx.form.controls.JInvToolBar;
import ru.inversion.fx.form.controls.dsbar.AbstractPartBase;
import ru.inversion.fx.form.controls.dsbar.DSInfoBar;
import ru.inversion.fx.form.controls.dsbar.IDSBarPart;
import ru.inversion.fx.form.controls.table.toolbar.AggregatorType;
import ru.inversion.fx.form.controls.treetableex.JInvTreeTableEx;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;

import java.util.*;
import java.util.stream.Collectors;

import static ru.inversion.dataset.fx.DSFXAdapter.PROPERTY_DATA_SET_ADAPTER;
import static ru.inversion.fx.form.controls.dsbar.AbstractPartBase.PART;

/** */
public class TSInfoBar extends JInvToolBar {

    final private static ResourceBundle g_bundle = ResourceBundle.getBundle("fore");

    private ResourceBundle bundle;

    /** */
    private JInvTreeTableEx treeTable;

    /** */
    private TSFXAdapter adapter;

    /** */
    private boolean constructed = false;

    /** */
    //final private List< IDSBarPart > partList = new ArrayList<>();

    /** */
    final private Set< DSInfoBar.PartEnum > disablePartSet = new TreeSet<>();

    /** */
    public TSInfoBar( ) {
        /*
        this.visibleProperty().addListener( ( observable, oldValue, newValue ) -> {
            if( newValue ) {
                doConstruct( );
                partList.forEach( IDSBarPart::onVisible );
            }
            else {
                partList.forEach( IDSBarPart::onHide );
            }
        } );
        */
    }

    /** */
    public ResourceBundle getBundle() {
        return bundle;
    }

    /** */
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

    /** */
    public void init( JInvTreeTableEx table ) {

        if( table == null )
            throw new IllegalArgumentException("Table is null");

        if( this.treeTable != null )
            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'treeTable' is already initialized");

        this.treeTable = table;

        Platform.runLater( ()->init( getAdapter() ) );
    }

    /** */
    private void init( TSFXAdapter adapter ) {

        if( adapter == null )
            return;//throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'adapter' is null");

//        if( !(adapter.getDataSet() instanceof IXXIDataSet) )
//            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'adapter' DataSet must be instanceof 'IXXIDataSet'");

//        if( this.adapter != null )
//            throw new IllegalArgumentException( Tags.PRODUCT_LABEL + "'adapter' DataSet is already initialized");

        this.setStyle("-fx-background-color: -fx-base;");

        if( !disablePartSet.contains(DSInfoBar.PartEnum.Total) )
            getItems().add( new Part_TotalCount(adapter, bundle).createControlPane() );

        if( adapter.isEnableMark() && !disablePartSet.contains(DSInfoBar.PartEnum.Mark) )
            getItems().add( new Part_MarkCount(adapter, bundle).createControlPane( ) );

        if( !disablePartSet.contains(DSInfoBar.PartEnum.FindById) )
            getItems().add( new Part_FindById(adapter, bundle).createControlPane() );

        constructed = true;
    }

    /** */
    private TSFXAdapter getAdapter( ) {

        if( adapter == null )
            adapter = treeTable.getProperty(PROPERTY_DATA_SET_ADAPTER);

        return adapter;
    }

    /** */
    private DSInfoBar.PartEnum getPartType( Node n )
    {
        IDSBarPart p = (IDSBarPart)n.getProperties().get(PART);
        return p != null ? p.getType() : null;
    }

    /** Получить Part по её типу */
    public IDSBarPart getPart( DSInfoBar.PartEnum part ) {
        return getItems().stream().filter( n->getPartType(n) == part ).map(n->(IDSBarPart)n.getProperties().get(PART)).findFirst().orElse(null);
    }

    private Node getPartNode( DSInfoBar.PartEnum part ) {
        return getItems().stream().filter( n->getPartType(n) == part ).findFirst().orElse(null);
    }

    /** */
    public void enablePart( boolean enable, DSInfoBar.PartEnum... parts ) {

        if( parts == null || parts.length == 0 )
            return;

        if( !enable ) {

            for( DSInfoBar.PartEnum p : parts ) {

                if( disablePartSet.contains(p) )
                    continue;

                if( this.constructed )
                    this.getItems().remove( getPartNode(p) );

                disablePartSet.add(p);
            }//end if
        }//end if
        else {

            for( DSInfoBar.PartEnum p : parts ) {

                if( !disablePartSet.contains(p) )
                    continue;

                if( this.constructed ) {

                    this.getItems().add ( getPartNode(p) );

                    this.getItems().sort( (Node o1, Node o2)-> DSInfoBar.PartEnum.compare( (DSInfoBar.PartEnum)o1.getProperties().get(PART), (DSInfoBar.PartEnum)o2.getProperties().get(PART) ));

                    disablePartSet.remove(p);
                }//end if
            }
        }
    }

    /**
     Возвращает поля агрегаторов
     */
    public List<? extends Control > getControls(){
        return aggPartList == null ? new ArrayList<>() :
                aggPartList.stream()
                        .filter( Objects::nonNull )
                        .flatMap( x -> x.getControls().stream() )
                        .collect( Collectors.toList());
    }
    /**
     Возвращает первый контрол агрегатора по колонке, типу и функции
     */
    public JInvNumberField getControl( String column, AggrFuncEnum func, AggregatorType type){

        if ( aggPartList == null || aggPartList.size() < type.ordinal()+1 || aggPartList.get(type.ordinal()) == null ) {
            return null;
        }
        return null;
        //return aggPartList.get( type.ordinal() ).getControl(func, column);
    }

    /*
    public Object getAggregatorValue( AggregatorType type, String column ) {

        if( aggPartList == null )
            return null;

        AbstractPartBase pa = aggPartList.get( type.ordinal() );

        if( pa == null )
            return null;

        return pa.getValue( column );
    }
    */
    /** */
    public boolean isEnablePart( DSInfoBar.PartEnum part ) {
        return !disablePartSet.contains(part);
    }

    private List<AbstractPartBase> aggPartList;


    /**
    private void doConstruct( ) {

        if( constructed )
            return;

        final ObservableList<Node> items = this.getItems();

        boolean forTable = treeTable != null;

        if( true ) {

            if( aggPartList == null ) {

                TSFXAdapter ad = getAdapter();

                if( ad != null && ad.isEnableMark() )
                    ;//addAggregator( "1", AggrFuncEnum.COUNT, MARK, null, g_bundle.getString("COUNT_MARKED") );

            }//end if

        }//end if

        if( aggPartList != null ) {

            for( AbstractPartBase pa : aggPartList ) {

                if( pa != null )
                    partList.add( pa );

            }//end for

        }//end if

        for( IDSBarPart p : partList ) {

            p.createControlPane( );

            if( !disablePartSet.contains( p.getType() ) )
                items.add( p.getControlPane() );
        }

        constructed = true;

        //this.disableProperty().bind( getAdapter().emptyProperty() );
    }
    */

    /** */
    public void build( ) {
        if( treeTable == null ) {
            this.visibleProperty().set(false);
            this.visibleProperty().set(true );
        }
    }

    /** */
    public void recalculate() {

        if( !constructed )
            return;
        //partList.forEach( IDSBarPart::recalculate );
    }

}
