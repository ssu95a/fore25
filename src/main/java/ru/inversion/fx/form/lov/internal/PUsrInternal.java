package ru.inversion.fx.form.lov.internal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * Для ядерных нужд
 * Автор @psh, fxbicomp, PUsr
 */
@Entity(name="PUsrInternal")
@Table(name="USR")
public class PUsrInternal implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String CUSRLOGNAME;
    private String CUSRNAME;
//    private String CUSRDIVISION;
//    private String SHORT_POSITION;
//    private String CUSRPOSITION;
    private Long IUSRID;
//    private String CUSROFFPHONE;

    public PUsrInternal(){}

    @Id
    @Column(name="CUSRLOGNAME",nullable = false,length = 30)
    public String getCUSRLOGNAME() {
        return CUSRLOGNAME;
    }
    public void setCUSRLOGNAME(String val) {
        CUSRLOGNAME = val;
    }
    @Column(name="CUSRNAME",nullable = false,length = 64)
    public String getCUSRNAME() {
        return CUSRNAME;
    }
    public void setCUSRNAME(String val) {
        CUSRNAME = val;
    }
//    @Column(name="CUSRDIVISION",length = 4)
//    public String getCUSRDIVISION() {
//        return CUSRDIVISION;
//    }
//    public void setCUSRDIVISION(String val) {
//        CUSRDIVISION = val;
//    }
//    @Column(name="SHORT_POSITION",length = 150)
//    public String getSHORT_POSITION() {
//        return SHORT_POSITION;
//    }
//    public void setSHORT_POSITION(String val) {
//        SHORT_POSITION = val;
//    }
//    @Column(name="CUSRPOSITION",length = 150)
//    public String getCUSRPOSITION() {
//        return CUSRPOSITION;
//    }
//    public void setCUSRPOSITION(String val) {
//        CUSRPOSITION = val;
//    }
    @Column(name="IUSRID",nullable = false,length = 6)
    public Long getIUSRID() {
        return IUSRID;
    }
    public void setIUSRID(Long val) {
        IUSRID = val;
    }
//    @Column(name="CUSROFFPHONE",length = 36)
//    public String getCUSROFFPHONE() {
//        return CUSROFFPHONE;
//    }
//    public void setCUSROFFPHONE(String val) {
//        CUSROFFPHONE = val;
//    }

    private String IDSMR;

    @Column(name="IDSMR",length = 3)
    public String getIDSMR() {
        return IDSMR;
    }

    public void setIDSMR(String IDSMR) {
        this.IDSMR = IDSMR;
    }

    private Long IUSRBRANCH;

    @Column(name="IUSRBRANCH",length = 3)
    public Long getIUSRBRANCH() {
        return IUSRBRANCH;
    }

    public void setIUSRBRANCH(Long IUSRBRANCH) {
        this.IUSRBRANCH = IUSRBRANCH;
    }

}