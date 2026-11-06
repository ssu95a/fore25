package ru.inversion.fx.report.drjasper;

import ru.inversion.util.*;

import java.util.*;

/**
 * <p>Description: </p>
 * Date: 21.02.2021
 * Time: 17:32
 * User:  opl
 */
public class GroupRepColumnInfo extends AbstractRepColumnInfo {

  /* ********************* Class Body *****************************/

  public GroupRepColumnInfo(
    String fxColumnLabel
    , double fxColumnWidth
    , List<AbstractRepColumnInfo> subColumns) {

    super(fxColumnLabel, fxColumnWidth);
    this.subColumns = subColumns;

    log.debug(TU.format("fxFieldName is *EMPTY*; fxColumnLabel= {0}; fxColumnWidth= {1, number, #.##}; subColumns.count()= {2}",
      fxColumnLabel, fxColumnWidth, subColumns.size()));
  }


  @Override
  public GroupRepColumnInfo init() {
    return this;
  }

  /* ********************* #Properties Getter/Setter ************************/

  public List<AbstractRepColumnInfo> getSubColumns() { return subColumns; }
  /* **************************** #End Properties **********************************/

  private List<AbstractRepColumnInfo> subColumns = new ArrayList<AbstractRepColumnInfo>();


}
