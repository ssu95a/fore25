/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.inversion.fx.jinvtblexport.property;

import javafx.scene.control.TableColumn;

/**
 *
 * @author polyatykina
 */
public class ReportColumnInfo {
    /*
    * left -
    */
    public Ref<Double> left = new Ref<>( 0.0 );   
    /*
    * width -
    */
    public Ref<Double> width = new Ref<>( 0.0 );
    /*
    * optimumWidth -
    */
    public Ref<Double> optimumWidth = new Ref<>( 0.0 );
    /*
    * fieldName - 
    */
    public Ref<String> fieldName = new Ref<>( "" );;
    /*
    * textMask - маска на данные
    */
    public String textMask;
    
    /*
    * DataClass - класс данных
    */
    private Class dataClass;
    public void setDataClass( Class value ){ dataClass = value; }
    public Class getDataClass(){ return dataClass; }  
    /*
    * column-
    */
    private TableColumn column;
    public void setСolumn( TableColumn value ){ column = value; }
    public TableColumn getСolumn(){ return column; }
    /*
    * column-
    */
    public ReportHeaderBandInfo band;
}
