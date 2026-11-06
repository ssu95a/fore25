/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvstimul.property;

import java.lang.reflect.Field;
import java.util.ArrayList;
import javafx.scene.control.TableColumn;
import ru.inversion.meta.IEntityProperty;

/**
 *
 * @author polyatykina
 */
public class ReportHeaderBandInfo {
    /*
    * left -
    */
    public Ref<Double> left = new Ref<>( 0.0 );  
    /*
    * width -
    */
    public Ref<Double> width = new Ref<>( 0.0 );
    /*
    * minimumWidth -
    */
    public Ref<Double> minimumWidth = new Ref<>( 0.0 );
    /*
    * optimumWidth -
    */
    public Ref<Double> optimumWidth = new Ref<>( 0.0 );
    /*
    * caption -
    */
    public Ref<String> caption = new Ref<>( "" );
    /*
    * columnHeader -
    */
    public TableColumn bandHeader;
    public void setBandHeader( TableColumn value ){ bandHeader = value; }
    public TableColumn getBandHeader(){ return bandHeader; }
    
    /*
    * columnHeader -
    */
    public IEntityProperty bandHeader4Field;
    public void setBandHeader4Field( IEntityProperty value ){ bandHeader4Field = value; }
    public IEntityProperty getBandHeader4Field(){ return bandHeader4Field; }
    
    /*
    * columnsHeader -
    */
    public Ref<ArrayList> bandsHeader;
    /*
    * column -
    */
    public TableColumn bandData;
    public void setBandData( TableColumn value ){ bandData = value; }
    public TableColumn getBandData(){ return bandData; }
    /*
    * columns -
    */
    public Ref<ArrayList> bandsData;
    /*
    * parent -
    */
    public ReportHeaderBandInfo parent;
    
    public ReportHeaderBandInfo( double left, String caption, TableColumn column )
    {
        this.left.set(left);
	this.width.set(0.0);
	this.caption.set(caption);
        this.bandHeader = column;
        this.bandsHeader = new Ref<>( new ArrayList() );
        this.bandsData = new Ref<>( new ArrayList() );
        this.parent = null;     
        
    }
    public ReportHeaderBandInfo( double left, String caption, IEntityProperty field )
    {
        this.left.set(left);
	this.width.set(0.0);
	this.caption.set(caption);
        this.bandHeader = null;
        this.bandsHeader = new Ref<>( new ArrayList() );
        this.bandsData = new Ref<>( new ArrayList() );
        this.parent = null;   
        this.bandHeader4Field = field;
        
    }
}
