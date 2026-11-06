package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.io.Serializable;

/**
@author  perov
@since   2016/10/07 17:05:38
*/
@Entity (name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterLinkTo")
@NamedNativeQuery (name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterLinkTo", query="select V_JF_Frm_Filter_Link.IDGROUP,V_JF_Frm_Filter_Link.IDFILTER, V_JF_Frm_Filter_Group.cname grp_name"
	+"    from V_JF_Frm_Filter_Link, V_JF_Frm_Filter_Group"
	+"   where V_JF_Frm_Filter_Group.id = V_JF_Frm_Filter_Link.idgroup")
public class PFrmFilterLinkTo implements Serializable  {

    private Long IDGROUP;
    private Long IDFILTER;
    private String GRP_NAME;

    public PFrmFilterLinkTo(){}

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
    @Column(name="GRP_NAME",length = 250)
    public String getGRP_NAME() {
        return GRP_NAME;
    }
    public void setGRP_NAME(String val) {
        GRP_NAME = val; 
    }
}