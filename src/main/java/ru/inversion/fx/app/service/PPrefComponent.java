package ru.inversion.fx.app.service;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Класс для сохранения и установки параметров визуальных компонентов форм.
 * <p>
 * @author antonovdi,
 *         sulimoff
 * @since 2016/06/23 19:06:51
 */
@Entity
@Table(name = "V_JF_PREF_COMPONENT")
/*
@NamedStoredProcedureQueries({
    @NamedStoredProcedureQuery(
        name = "save",
        procedureName = "JF_PKG_UTIL.SAVE_PREF_UI_SETTINGS",
        parameters = {
            @StoredProcedureParameter( type = String.class, name = "p_formname"),
            @StoredProcedureParameter( type = List.class,   name = "p_settings")
        }
    )
})
*/
//@XmlRootElement(name = "pref")
public class PPrefComponent implements Serializable {

    /** Имя формы, для которой сохраняются параметры */
    private String FORM_NAME;

    /** Имя компонента формы, для которой сохраняются параметры */
    private String COMPONENT;

    /**
     * Имя элемента компонента формы, для которой сохраняются параметры
     * (например столбец таблицы)
     */
    private String ELEMENT;

    /** Признак видимый/скрытй */
    private Long VISIBLE = 1L;

    /** Высота */
    private Long HEIGHT;

    /** Длина */
    private Long WIDTH;

    /** Порядок вывода на экран */
    private Integer ORDBY;

    /** */
    private Long SERIAL_UID;

    public PPrefComponent( ) {
    }

//    @Id
    @Column(name = "FORM_NAME")
    public String getFORM_NAME() {
        return FORM_NAME;
    }
    public void setFORM_NAME(String val) {
        FORM_NAME = val;
    }

    @Column(name = "COMPONENT")
    public String getCOMPONENT() {
        return COMPONENT;
    }

    public void setCOMPONENT(String val) {
        COMPONENT = val;
    }

    @Column(name = "ELEMENT")
    public String getELEMENT() {
        return ELEMENT;
    }

    public void setELEMENT(String val) {
        ELEMENT = val;
    }

    @Column(name = "VISIBLE", nullable = false)
    public Long getVISIBLE() {
        return VISIBLE;
    }
    public void setVISIBLE(Long val) {
        VISIBLE = val;
    }

    @Column(name = "HEIGHT", length = 38)
    public Long getHEIGHT() {
        return HEIGHT;
    }

    public void setHEIGHT(Long val) {
        HEIGHT = val;
    }

    @Column(name = "SERIAL_UID")
    public Long getSERIAL_UID() {
        return SERIAL_UID;
    }

    public void setSERIAL_UID(Long val) {
        SERIAL_UID = val;
    }

    @Column(name = "WIDTH", length = 38)
    public Long getWIDTH() {
        return WIDTH;
    }
    public void setWIDTH(Long val) {
        WIDTH = val;
    }

    @Column(name = "ORDBY", length = 38)
    public Integer getORDBY() {
        return ORDBY;
    }

    public void setORDBY(Integer val) {
        ORDBY = val;
    }

    @Override
    public boolean equals( final Object o ) {

        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        final PPrefComponent that = (PPrefComponent) o;
        return  Objects.equals( FORM_NAME, that.FORM_NAME ) &&
                Objects.equals( COMPONENT, that.COMPONENT ) &&
                Objects.equals( ELEMENT,   that.ELEMENT );
    }

    @Override
    public int hashCode() {
        return Objects.hash( FORM_NAME, COMPONENT, ELEMENT );
    }

    @Override
    public String toString() {
        return "PPrefComponent { FORM_NAME=" + FORM_NAME + ", COMPONENT=" + COMPONENT + ", ELEMENT=" + ELEMENT + ", " +
                "VISIBLE=" + VISIBLE + ", HEIGHT=" + HEIGHT + ", WIDTH=" + WIDTH + ", ORDBY=" + ORDBY + ", SERIAL_UID=" + SERIAL_UID + '}';
    }

}
