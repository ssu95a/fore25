package ru.inversion.fx.log;

import org.slf4j.Logger;

import javax.persistence.*;
import java.io.Serializable;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;

/**
@author  Fomishkin
@since   2021/08/23 13:11:48
*/
@Entity (name="ru.inversion.fx.log.PSyslogEntry")
@Table (name="JF_SLOG")
public class PSyslogEntry implements Serializable
{
    private static final long serialVersionUID = 23_08_2021_13_11_48l;
    private final static Logger logger = getLogger(lookup().lookupClass());


/*
* Название логгера
*/
    private String CSLOG_LRNAME;

/*
* Уровень логгера
*/
    private String CSLOG_LRLEVEL;

    public PSyslogEntry(){}

    @Id 
    @Column(name="CSLOG_LRNAME",nullable = false,length = 512)
    public String getCSLOG_LRNAME() {
        return CSLOG_LRNAME;
    }
    public void setCSLOG_LRNAME(String val) {
        CSLOG_LRNAME = val; 
    }
    @Column(name="CSLOG_LRLEVEL",nullable = false,length = 16)
    public String getCSLOG_LRLEVEL() {
        return CSLOG_LRLEVEL;
    }
    public void setCSLOG_LRLEVEL(String val) {
        CSLOG_LRLEVEL = val; 
    }

    @Transient
    public LoggerLevelEnum getLogLevel() {
        LoggerLevelEnum loggerLevelEnum = LoggerLevelEnum.OFF;
        try {
            loggerLevelEnum = LoggerLevelEnum.valueOf(getCSLOG_LRLEVEL());
        } catch (Throwable th){
            logger.error("Error converting log level from CSLOG_LRLEVEL:",th);
        }
        return loggerLevelEnum;
    }
}