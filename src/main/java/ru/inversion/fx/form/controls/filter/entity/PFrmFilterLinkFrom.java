package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.io.Serializable;

/**
@author  perov
@since   2016/10/07 16:51:27
*/
@Entity (name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterLinkFrom")
@NamedNativeQuery (name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterLinkFrom", query="select V_JF_Frm_Filter.id idfilter,"
	+"       V_JF_Frm_Filter_Group.id idgroup,"
	+"       V_JF_Frm_Filter_Group.cname grp_name"
	+"  from V_JF_Frm_Filter,"
	+"       V_JF_Frm_Filter_Group"
	+"  where not exists (select null"
	+"                      from V_JF_Frm_Filter_Link"
	+"                      where V_JF_Frm_Filter_Link.idfilter=V_JF_Frm_Filter.id and"
	+"                            V_JF_Frm_Filter_Link.idgroup=V_JF_Frm_Filter_Group.id)")
public class PFrmFilterLinkFrom implements Serializable  {

    private Long IDFILTER;
    private Long IDGROUP;
    private String GRP_NAME;

    public PFrmFilterLinkFrom(){}

    @Id 
    @Column(name="IDFILTER",nullable = false,length = 38)
    public Long getIDFILTER() {
        return IDFILTER;
    }
    public void setIDFILTER(Long val) {
        IDFILTER = val; 
    }
    @Id 
    @Column(name="IDGROUP",nullable = false,length = 38)
    public Long getIDGROUP() {
        return IDGROUP;
    }
    public void setIDGROUP(Long val) {
        IDGROUP = val; 
    }
    @Column(name="GRP_NAME",length = 250)
    public String getGRP_NAME() {
        return GRP_NAME;
    }
    public void setGRP_NAME(String val) {
        GRP_NAME = val; 
    }
}