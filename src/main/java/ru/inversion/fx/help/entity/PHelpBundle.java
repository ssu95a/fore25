package ru.inversion.fx.help.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;

/**
@author  perov
@since   2016/04/05 12:48:38
*/
@Entity(name = "ru.inversion.fx.help.entity.PHelpBundle")
@Table(name = "JF_HELP_BUNDLE")
@NamedNativeQuery( name="ru.inversion.fx.help.entity.PHelpBundle",query =
        "SELECT form,\n"
      + "       cntr_name,\n"
      + "       html_text\n"
      + "FROM JF_HELP_BUNDLE\n"
//      + "where form=:FORM \n"
//      + "and cntr_name=:CONTROL"
        )
public class PHelpBundle  implements Serializable  {

    private String FORM;
    private String CNTR_NAME;
    private String HTML_TEXT;
   // private String HTML_TEXT_STR;

    public PHelpBundle(){}

    @Id
    @Column(name="FORM",nullable = false,length = 250)
    public String getFORM() {
        return FORM;
    }
    public void setFORM(String val) {
        FORM = val;
    }
    @Id
    @Column(name="CNTR_NAME",nullable = false,length = 250)
    public String getCNTR_NAME() {
        return CNTR_NAME;
    }
    public void setCNTR_NAME(String val) {
        CNTR_NAME = val;
    }
    @Column(name="HTML_TEXT",nullable = true)
    public String getHTML_TEXT() {
        return HTML_TEXT;
    }
    public void setHTML_TEXT(String val) {
        HTML_TEXT = val;
    }

}
