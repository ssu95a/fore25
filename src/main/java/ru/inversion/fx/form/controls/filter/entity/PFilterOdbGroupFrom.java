package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.io.Serializable;

/**
@author  perov
@since   2016/10/12 11:04:12
*/
@Entity (name="ru.inversion.fx.form.controls.filter.entity.PFilterOdbGroupFrom")
@NamedNativeQuery (name="ru.inversion.fx.form.controls.filter.entity.PFilterOdbGroupFrom",
        query="select V_JF_Frm_Filter.id idfilter,"
	+"       ODB_Group_USR.grp_id idgroup,"
	+"       ODB_Group_USR.grp_name grp_name"
	+"  from V_JF_Frm_Filter,"
	+"       ODB_Group_USR"
	+"  where not exists (select null"
	+"                      from V_JF_Frm_Filter_ODB_Group"
	+"                      where V_JF_Frm_Filter_ODB_Group.idfilter=V_JF_Frm_Filter.id and"
	+"                            V_JF_Frm_Filter_ODB_Group.idgroup=ODB_Group_USR.grp_id)")
public class PFilterOdbGroupFrom implements Serializable  {

    private Long IDFILTER;
    private Long IDGROUP;
    private String GRP_NAME;

    public PFilterOdbGroupFrom(){}

    @Id 
    @Column(name="IDFILTER",nullable = false,length = 38)
    public Long getIDFILTER() {
        return IDFILTER;
    }
    public void setIDFILTER(Long val) {
        IDFILTER = val; 
    }
    @Id 
    @Column(name="IDGROUP",nullable = false,length = 4)
    public Long getIDGROUP() {
        return IDGROUP;
    }
    public void setIDGROUP(Long val) {
        IDGROUP = val; 
    }
    @Column(name="GRP_NAME",length = 64)
    public String getGRP_NAME() {
        return GRP_NAME;
    }
    public void setGRP_NAME(String val) {
        GRP_NAME = val; 
    }
}