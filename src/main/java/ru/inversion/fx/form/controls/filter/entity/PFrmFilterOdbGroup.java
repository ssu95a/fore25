package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.io.Serializable;

/**
@author  perov
@since   2016/10/07 10:28:28
*/
@Entity (name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterOdbGroup")
@NamedNativeQuery (name="ru.inversion.fx.form.controls.filter.entity.PFrmFilterOdbGroup", query="select IDFILTER, IDGROUP, CNAME"
	+"  from (select a.idfilter, a.idgroup, b.grp_name cname"
	+"          from V_JF_FRM_FILTER_ODB_GROUP a, ODB_GROUP_USR b"
	+"         where a.idgroup = b.grp_id)")
public class PFrmFilterOdbGroup implements Serializable  {

    private Long IDFILTER;
    private Long IDGROUP;
    private String CNAME;

    public PFrmFilterOdbGroup(){}

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
    @Column(name="CNAME",length = 64)
    public String getCNAME() {
        return CNAME;
    }
    public void setCNAME(String val) {
        CNAME = val; 
    }
}