package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.ResultSet;

/**
 * Данные одной записи параметра фильтра
 * <p>
 * @author  perov, Sulimoff
*/
@Entity(name="ru.inversion.fx.form.controls.filter.entity.PFilterParameter")
@NamedStoredProcedureQueries(
    value = {
        @NamedStoredProcedureQuery (
            name = "getFltParametersValues",
            procedureName = "JF_PKG_UTIL.get_flt_parameters_values",
            parameters = {
                @StoredProcedureParameter( name = "filterId",    type = Long.class, mode = ParameterMode.IN  ),
                @StoredProcedureParameter( name = "cParameters", type = ResultSet.class, mode = ParameterMode.OUT )
            }
        )
    }
)
@NamedNativeQueries(
    value = {
        @NamedNativeQuery(name = "getFltParametersValues.pg", query = "SELECT * FROM jf_pkg_util.get_flt_parameters_values(:IDFILTER)")
    }
)
@Table(name = "FRM_FILTER_PARAMETER")
public class PFilterParameter implements Serializable  {

    private Long IDFILTER;
    private Long IPARAMNUM;
    private String CPARAMDESCR;
    private String CPARAMDEFAULT;
    private Long IDCURSOR;
    private String CPARAMSAVE;
    private Boolean BPARAMSAVE;
    private String CPARAMDEFAULT_CALC;

    public PFilterParameter(){}

    @Id 
    @Column(name="IDFILTER",nullable = false)
    public Long getIDFILTER() {
        return IDFILTER;
    }
    public void setIDFILTER(Long val) {
        IDFILTER = val; 
    }
    @Id 
    @Column(name="IPARAMNUM",nullable = false)
    public Long getIPARAMNUM() {
        return IPARAMNUM;
    }
    public void setIPARAMNUM(Long val) {
        IPARAMNUM = val; 
    }
    @Column(name="CPARAMDESCR")
    public String getCPARAMDESCR() {
        return CPARAMDESCR;
    }
    public void setCPARAMDESCR(String val) {
        CPARAMDESCR = val; 
    }
    @Column(name="CPARAMDEFAULT")
    public String getCPARAMDEFAULT() {
        return CPARAMDEFAULT;
    }
    public void setCPARAMDEFAULT(String val) {
        CPARAMDEFAULT = val; 
        setCPARAMDEFAULT_CALC(val);
    }
    @Column(name="IDCURSOR")
    public Long getIDCURSOR() {
        return IDCURSOR;
    }
    public void setIDCURSOR(Long val) {
        IDCURSOR = val; 
    }
    @Column(name="CPARAMSAVE",length = 1)
    public String getCPARAMSAVE() {
        return CPARAMSAVE;
    }
    public void setCPARAMSAVE(String val) {
        
        CPARAMSAVE = val;       
        setBPARAMSAVE(CPARAMSAVE.equals("Y"));
       
    }
    
    @Transient
    public Boolean getBPARAMSAVE() {
        return BPARAMSAVE;
    }
    public void setBPARAMSAVE(Boolean val) {
        BPARAMSAVE = val;       
    }
    @Column(name="CPARAMDEFAULT_CALC", insertable = false, updatable = false, columnDefinition = "null")
    public String getCPARAMDEFAULT_CALC() {
        return CPARAMDEFAULT_CALC;
    }
    public void setCPARAMDEFAULT_CALC(String val) {
        CPARAMDEFAULT_CALC = val; 
    }
   
}