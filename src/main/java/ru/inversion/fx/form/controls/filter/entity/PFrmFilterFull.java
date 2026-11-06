package ru.inversion.fx.form.controls.filter.entity;

import ru.inversion.dataset.mark.IDMarkable;
import ru.inversion.db.entity.DBReturningValue;

import javax.persistence.*;
import java.io.Serializable;

/**
    @author  perov, Sulimoff
    @since   16.09.23
*/
@Entity( name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterFull")
@Table ( name ="v_jf_frm_filter" )
/*
@NamedNativeQuery( name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterFull", 
                   query="select CFORMNAME,CUSER,CWHEREBLK,CWHERENAME,CAUTOSETUP,ID,CBLOCKNAME,ISAUTO"
	+"  from (SELECT cformname,"
	+"               cuser,"
	+"               cwhereblk,"
	+"               cwherename,"
	+"               cautosetup,"
	+"               id,"
	+"               cblockname,"
	+"               NVL2(cautosetup, 1, 0) isAuto"
	+"          FROM v_jf_frm_filter " +
     "where cformname = ? and cblockname = ? and ( ? IS NULL OR CUSER = USER ))")
*/
//	+"          FROM v_jf_frm_filter where ( cformname = ? and cblockname = ?) and ( ( ? IS NOT NULL AND CUSER=USER) OR (? IS NULL) ) )")

@NamedNativeQueries(
    value = {
        @NamedNativeQuery(
                name="getById",
                query = "SELECT cformname,\n"
                    + "cuser,\n"
                    + "cwhereblk,\n"
                    + "cwherename,\n"
                    + "( case when cautosetup is null then 0 else 1 end ) isAuto,\n"
                    + "id,\n"
                    + "cblockname\n"
                    + "FROM v_jf_frm_filter WHERE id = ? --AND cuser = upper(USER)"
        ),
        @NamedNativeQuery(
                name = "getBufferTab",
                query = "SELECT COLUMN_VALUE FROM V_IE_BUFER_TAB"
        )
    }
)
@NamedStoredProcedureQueries(
        value = {
                @NamedStoredProcedureQuery (
                        name = "deleteByMark",
                        procedureName = "Frm_Filter_IE_Pkg.delete_Filters_ByMark",
                        resultClasses = {Long.class},
                        parameters = {
                                @StoredProcedureParameter (
                                        name = "markerId",
                                        type = Long.class
                                ),
                                @StoredProcedureParameter(
                                        name = "errorInfo",
                                        type = String.class,
                                        mode = ParameterMode.OUT
                                )

                        }
                ),
                @NamedStoredProcedureQuery(
                        name = "exportByMark",
                        procedureName = "Frm_Filter_IE_Pkg.Export_By_Mrk",
                        parameters = {
                                @StoredProcedureParameter(
                                        name = "markerId",
                                        type = Long.class
                                ),
                                @StoredProcedureParameter(
                                        name = "clearMrk",
                                        type = String.class
                                )
                        }
                ),
                @NamedStoredProcedureQuery(
                        name = "exportByCurrentRow",
                        procedureName = "Frm_Filter_IE_Pkg.Export_Current",
                        parameters = {
                                @StoredProcedureParameter(
                                        name = "filterId",
                                        type = Long.class
                                )
                        }
                )
        }
)
public class PFrmFilterFull extends IDMarkable implements Serializable  {

    private String CFORMNAME;
    private String CUSER;
    private String CWHEREBLK;
    private String CWHERENAME;
    private String CAUTOSETUP;
    private Long ID;
    private String CBLOCKNAME;
    private Boolean ISAUTO;

    public PFrmFilterFull(){}

    @Column(name="CFORMNAME",nullable = false,length = 30)
    public String getCFORMNAME() {
        return CFORMNAME;
    }
    public void setCFORMNAME(String val) {
        CFORMNAME = val; 
    }
    @Column(name="CUSER",nullable = false,length = 30)
    public String getCUSER() {
        return CUSER;
    }
    public void setCUSER(String val) {
        CUSER = val; 
    }
    @Column(name="CWHEREBLK")
    public String getCWHEREBLK() {
        return CWHEREBLK;
    }
    public void setCWHEREBLK(String val) {
        CWHEREBLK = val; 
    }
    @Column(name="CWHERENAME",nullable = false,length = 60)
    public String getCWHERENAME() {
        return CWHERENAME;
    }
    public void setCWHERENAME(String val) {
        CWHERENAME = val; 
    }
    @Column(name="CAUTOSETUP",length = 250)
    public String getCAUTOSETUP() {
        return CAUTOSETUP;
    }
    public void setCAUTOSETUP(String val) {
        CAUTOSETUP = val; 
    }
    @Id
    @DBReturningValue()
    @Column(name="ID",nullable = false,insertable = false, updatable = false)
    public Long getID() {
        return ID;
    }
    public void setID(Long val) {
        ID = val; 
    }
    @Column(name="CBLOCKNAME",length = 30)
    public String getCBLOCKNAME() {
        return CBLOCKNAME;
    }
    public void setCBLOCKNAME(String val) {
        CBLOCKNAME = val; 
    }
    @Column(name="ISAUTO",columnDefinition = "case when cautosetup is null then 0 else 1 end", insertable = false, updatable = false)
    public Boolean getISAUTO() {
        return ISAUTO == null ?  Boolean.FALSE : ISAUTO;
    }
    public void setISAUTO(Boolean val) {
        ISAUTO = val;
        if (val.equals(Boolean.TRUE)) {
            StringBuilder sb = new StringBuilder();
            if (getCFORMNAME() != null) {
                sb.append(getCFORMNAME());
            }
            sb.append(".");
            if (getCUSER() != null) {
                sb.append(getCUSER());
            }
            sb.append(".");
            if (getCBLOCKNAME() != null) {
                sb.append(getCBLOCKNAME());
            }
            setCAUTOSETUP(sb.toString());
        }else{
            setCAUTOSETUP(null);
        }
    }
    @Override
    @Transient
    public Long getMarkLongID() {
        return getID();
    }
}