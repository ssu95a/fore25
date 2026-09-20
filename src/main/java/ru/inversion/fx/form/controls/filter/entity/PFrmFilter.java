package ru.inversion.fx.form.controls.filter.entity;

import ru.inversion.db.entity.ProxyFor;
import ru.inversion.utils.U;

import javax.persistence.*;
import java.io.Serializable;

/**
    @author  Sulimoff,
             perov
    @since   2016/09/29 15:40:53
*/
//F.CWHEREBLK
@Entity (name="fore.PFrmFilter")
@NamedNativeQueries(
    value = {
        @NamedNativeQuery(
            name = "autoList",
            query = "SELECT ID, CWHERENAME, CUSER from FRM_FILTER "
                    +" where cAutoSetup = :FORM_NAME ~|| '.' || user || '.' ||~ :BLOCK_NAME \n"
                    + " union \n"
                    + "SELECT F.ID, F.CWHERENAME, F.cUser \n"
                    + "  FROM FRM_FILTER F\n"
                    + " WHERE F.cFormName = :FORM_NAME\n"
                    + "   AND ( F.cBlockName IS NULL OR F.cBlockName = :BLOCK_NAME ) \n"
                    + "   AND F.ID IN ( SELECT G.IDFilter\n"
                    + "					  FROM FRM_FILTER_ODB_GROUP G,\n"
                    + "                  	   ODB_GRP_MEMBER M\n"
                    + "                  WHERE G.IDGroup = M.Grp_ID \n"
                    + "                    AND M.iUsrID = ~to_number( SYS_CONTEXT('B21', 'IDUsr'))~ )"
        ),
        @NamedNativeQuery (
                name="",
                query="SELECT ID, CWHERENAME,CWHEREBLK,CUSER,HASPRM "
                        +"  FROM (select ID, cwherename, cwhereblk, cuser,"
                        +"         ( select count(*) from V_JF_frm_filter_parameter a where a.IDFilter = V_JF_FRM_FILTER.ID ) as hasPrm"
                        +"         from V_JF_FRM_FILTER"
                        +"         WHERE ( to_number(:IDGROUP) is null or EXISTS" // 0
                        +"                (SELECT NULL FROM V_JF_FRM_FILTER_LINK l WHERE l.IDFilter = V_JF_FRM_FILTER.ID AND l.IDGROUP = :IDGROUP ))"
                        +"                    AND cFormName = :FORM AND cBlockName = :BLOCK AND ( :ALL_USER = 0 OR CUSER=USER )) q"
        )
    }
)
@Table(name = "V_JF_FRM_FILTER")
public class PFrmFilter implements Serializable  {

    private Long    ID;
    private String  CWHERENAME;
    private String  CUSER;
    private Integer HASPRM;

    public PFrmFilter(){}

    @Id 
    @Column(name="ID",nullable = false)
    public Long getID() {
        return ID;
    }
    public void setID(Long val) {
        ID = val; 
    }
    @Column(name="CWHERENAME",nullable = false,length = 60)
    public String getCWHERENAME() {
        return CWHERENAME;
    }
    public void setCWHERENAME(String val) {
        CWHERENAME = val; 
    }
    
//    @Column(name="CWHEREBLK",nullable = false,length = 60)
//    public String getCWHEREBLK() {
//        return CWHEREBLK;
//    }
//    public void setCWHEREBLK(String val) {
//        CWHEREBLK = val;
//    }
    
    @Column(name="CUSER",nullable = false,length = 30)
    public String getCUSER() {
        return CUSER;
    }
    public void setCUSER(String val) {
        CUSER = val; 
    }
    @Column(name="HASPRM",length = 1, columnDefinition = "( select count(*) from V_JF_frm_filter_parameter a where a.IDFilter = ID )")
    public Integer getHASPRM() {
        return HASPRM;
    }
    public void setHASPRM(Integer val) {
        HASPRM = val; 
    }
    /** */
    @ProxyFor(columnName = "HASPRM")
    public boolean hasParam( ) {
        return U.nvl( getHASPRM(), 0 ) > 0;
    }
}