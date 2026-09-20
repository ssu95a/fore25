package ru.inversion.fx.form.controls.filter.entity;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author banin
 * @since 2018/06/26 14:58:43
 */
@Entity(name = "ru.inversion.fx.form.controls.filter.entity.PFrmFilterGroup")
@Table(name = "FRM_FILTER_GROUP")
@NamedNativeQuery(
    name="",
    query = "select ID || '. ' || CNAME CNAME, ID from FRM_FILTER_GROUP where ID=?"
)
@NamedNativeQueries(
    value =
    {
        @NamedNativeQuery(
                name="list",
                query = "select CNAME, ID from FRM_FILTER_GROUP"
        )
    }
)
public class PFrmFilterGroup implements Serializable {

    private static final long serialVersionUID = 4489607313289859580L;

    private Long ID;
    private String CNAME;

    public PFrmFilterGroup() {
    }

    @Id
    @Column(name = "ID", nullable = false)
    public Long getID() {
        return ID;
    }
    public void setID(Long val) {
        ID = val;
    }

    @Column(name = "CNAME", length = 250)
    public String getCNAME() {
        return CNAME;
    }
    public void setCNAME(String val) {
        CNAME = val;
    }
}
