package ru.inversion.fx.form.lov;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import java.io.Serializable;

/**
 * @author perov
 * @since 2016/03/28 10:51:25
 */
@Entity
@NamedNativeQuery(name = "ru.inversion.fx.help.entity.PHelp", query = "select form, "
    + "descr, "
    + "ver, "
    + "html_text"
    + " from JF_HELP")
public class PHelp implements Serializable {

    private String FORM;
    private String DESCR;
    private String VER;
    private String HTML_TEXT;

    @Column(name = "HTML_TEXT")
    public String getHTML_TEXT() {
        return HTML_TEXT;
    }

    public void setHTML_TEXT(String HTML_TEXT) {
        this.HTML_TEXT = HTML_TEXT;
    }

    public PHelp() {
    }

    @Id
    @Column(name = "FORM", nullable = false, length = 250)
    public String getFORM() {
        return FORM;
    }

    public void setFORM(String val) {
        FORM = val;
    }

    @Column(name = "DESCR", nullable = false, length = 1000)
    public String getDESCR() {
        return DESCR;
    }

    public void setDESCR(String val) {
        DESCR = val;
    }

    @Column(name = "VER", length = 20)
    public String getVER() {
        return VER;
    }

    public void setVER(String val) {
        VER = val;
    }

    @Override
    public String toString() {

        return HTML_TEXT != null ? "PHelp{" + "FORM=" + FORM + ", DESCR=" + DESCR + ", VER=" + VER + ", HTML_TEXT=" + HTML_TEXT.substring(0, Math.min(100, HTML_TEXT.length())) + '}'
            : "PHelp{" + "FORM=" + FORM + ", DESCR=" + DESCR + ", VER=" + VER + ", HTML_TEXT=" + HTML_TEXT + '}';
    }
}
