package ru.inversion.fx.form.controls;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author ssu
 */
@Entity
@Table(name = "cur")
@NamedNativeQuery( name = "cur", query = "select * from cur where ~(NVL(cCurTag,'N') != 'D')~" )
public class PCurLov implements Serializable 
{
    private static final long serialVersionUID = 1L;
    
    private String  curISO;
    private String  curRName;
    private Integer iCurISO;

    @Id
    @Column(name = "CCURISO", length = 8)
    public String getCurISO() {
        return curISO;
    }

    public void setCurISO(String curISO) {
        this.curISO = curISO;
    }

    @Column(name = "CCURRNAME", length = 30 )
    public String getCurRName() {
        return curRName;
    }

    public void setCurRName(String curRName) {
        this.curRName = curRName;
    }

    @Column(name = "ICURISO")
    public Integer getICurISO() {
        return iCurISO;
    }

    public void setICurISO(Integer iCurISO) {
        this.iCurISO = iCurISO;
    }
}
