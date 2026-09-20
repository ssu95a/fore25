package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.io.Serializable;

/**
@author  perov
@since   2016/10/12 11:22:51
*/
@Entity (name="ru.inversion.fx.form.controls.filter.entity.PFilterOdbGroupTo")
@NamedNativeQuery (name="ru.inversion.fx.form.controls.filter.entity.PFilterOdbGroupTo", query="select V_JF_Frm_Filter_ODB_Group.Idgroup Idgroup, V_JF_Frm_Filter_ODB_Group.Idfilter Idfilter,"
	+"       ODB_Group_USR.grp_name grp_name"
	+"  from V_JF_Frm_Filter_ODB_Group,"
	+"       ODB_Group_USR"
	+"  where ODB_Group_USR.grp_id=V_JF_Frm_Filter_ODB_Group.idgroup")
public class PFilterOdbGroupTo implements Serializable  {

    private Long IDGROUP;
    private Long IDFILTER;
    private String GRP_NAME;

    public PFilterOdbGroupTo(){}

    @Id 
    @Column(name="IDGROUP",nullable = false,length = 38)
    public Long getIDGROUP() {
        return IDGROUP;
    }
    public void setIDGROUP(Long val) {
        IDGROUP = val; 
    }
    @Id 
    @Column(name="IDFILTER",nullable = false,length = 38)
    public Long getIDFILTER() {
        return IDFILTER;
    }
    public void setIDFILTER(Long val) {
        IDFILTER = val; 
    }
    @Column(name="GRP_NAME",length = 64)
    public String getGRP_NAME() {
        return GRP_NAME;
    }
    public void setGRP_NAME(String val) {
        GRP_NAME = val; 
    }
}