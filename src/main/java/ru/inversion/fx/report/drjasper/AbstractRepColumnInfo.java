package ru.inversion.fx.report.drjasper;

import org.slf4j.*;

import java.util.*;

/**
 * <p>Description: Необходимая для постоения колоники информация</p>
 * Date: 17.04.2020
 * Time: 17:39
 * User:  opl
 */
public abstract class AbstractRepColumnInfo {

  protected static Logger log = LoggerFactory.getLogger( AbstractRepColumnInfo.class );

  /* ********************* Class Body *****************************/

  public AbstractRepColumnInfo(
            String fxColumnLabel
          , double fxColumnWidth ) {

    this.fxColumnLabel = (fxColumnLabel != null && fxColumnLabel.length() > 0 ? fxColumnLabel : " " );
    this.fxColumnWidth = fxColumnWidth;
//    log.debug( TU.format("fxFieldName is *EMPTY*; fxColumnLabel= {0}; fcColumnWidth= {1, number, #.##}; subColumns.count()= {2}",
//      fxColumnLabel, fxColumnWidth ) );
  }

  public abstract AbstractRepColumnInfo init();

  public Class obtainDataClass() {
    return null;
  }


  /* ********************* #Properties Getter/Setter ************************/

  public String getFxColumnLabel() { return fxColumnLabel; }
  public double getFxColumnWidth() { return fxColumnWidth; }

  public void setFxColumnWidth(double fxColumnWidth) { this.fxColumnWidth = fxColumnWidth; }

  /* **************************** #End Properties **********************************/

  private String fxColumnLabel = " ";
  private double fxColumnWidth = 0;

//  private String fxColumnName;
  
}
