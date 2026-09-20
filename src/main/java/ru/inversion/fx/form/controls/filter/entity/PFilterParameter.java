package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.ResultSet;

/**
@author  perov
@since   2016/10/07 14:38:34
*/
@Entity(name="ru.inversion.fx.form.controls.filter.entity.PFilterParameter")
@Table (name="V_JF_Frm_Filter_Parameter")
@NamedNativeQueries(
    value = {
        @NamedNativeQuery(
            name  = "list",
            query = "select * from ( select IDFILTER,\n"
                    + "       IPARAMNUM,\n"
                    + "       CPARAMDESCR,      \n"
                    + "       cparamsave,\n"
                    + "       IDCURSOR,\n"
                    + "       CPARAMDEFAULT, \n"
                    + "       DECODE(CPARAMDEFAULT, NULL,\n"
                    + "                           DECODE(cparamsave,'Y', \n"
                    + "                                       DECODE(SUBSTR(JF_PKG_UTIL.Get_PrefUpper('FRM_FLT.BLK_LOAD_PARAM.'||IDFILTER||'.'||IPARAMNUM, null), 1, 1),\n"
                    + "                                              '&',\n"
                    + "                                              JF_PKG_UTIL.CallMacro(SUBSTR(PREF.Get_Preference('FRM_FLT.BLK_LOAD_PARAM.'||IDFILTER||'.'||IPARAMNUM), 2), 0),\n"
                    + "                                              JF_PKG_UTIL.Get_PrefUpper('FRM_FLT.BLK_LOAD_PARAM.'||IDFILTER||'.'||IPARAMNUM,null)) \n"
                    + "                                       ,\n"
                    + "                                       CPARAMDEFAULT\n"
                    + "                                 )\n"
                    + "                           ,\n"
                    + "                           DECODE(SUBSTR(CPARAMDEFAULT, 1, 1),'&',JF_PKG_UTIL.CallMacro(SUBSTR(CPARAMDEFAULT, 2), 0), CPARAMDEFAULT)  \n"
                    + "       )\n"
                    + "       CPARAMDEFAULT_CALC\n"
                    + "from FRM_FILTER_PARAMETER ) qrslt"
        )
    }
)
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