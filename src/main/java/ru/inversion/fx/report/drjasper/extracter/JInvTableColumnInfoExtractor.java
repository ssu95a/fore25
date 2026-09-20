package ru.inversion.fx.report.drjasper.extracter;

import org.slf4j.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.util.*;

import java.util.*;

/**
 * <p>Description: </p>
 * Date: 07.05.2020
 * Time: 14:39
 * User:  opl
 */
public class JInvTableColumnInfoExtractor implements IColumnInfoExtractor {

  protected static Logger log = LoggerFactory.getLogger( JInvTableColumnInfoExtractor.class );

  /* ********************* Class Body *****************************/

  public JInvTableColumnInfoExtractor(JInvTableColumn tableColumn) {
    this.tableColumn = tableColumn;
    log.debug( TU.format("JInvTableColumnInfoExtractor.tableColumn.name= {0}", tableColumn.getFieldName() ) );
//    tableColumn.getId()
  }

  @Override
  public String extractColumnLabel() {
    return tableColumn.textProperty().get();
  }

  @Override
  public double extractFieldWidth() {
    return tableColumn.widthProperty().get() + 10;
  }

  public Optional<String> extractFormatMask() { return Optional.ofNullable(tableColumn.getMask()); }

//  public Class extractColumnClass() {
//    tableColumn.get
//  }


  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/

  private JInvTableColumn tableColumn;

  private String columnName;
}
