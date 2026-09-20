package ru.inversion.fx.report.drjasper.extracter;

import org.slf4j.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.report.drjasper.*;
import ru.inversion.fx.report.drjasper.util.*;

import java.util.*;

import static ru.inversion.util.TU.format;

/**
 * <p>Description: </p>
 * Date: 22.04.2020
 * Time: 14:54
 * User:  opl
 */
public class DataTypeTableColInfoExtractor implements IColumnInfoExtractor {

  protected static Logger log = LoggerFactory.getLogger(DataTypeTableColInfoExtractor.class);

  /* ********************* Class Body *****************************/
  public DataTypeTableColInfoExtractor(JInvTableColumn tableColumn, Class dataClass) {
    this.tableColumn = tableColumn;
    this.dataClass = dataClass;
    log.debug( format("DataTypeTableColInfoExtractor.tableColumn.name= {0}; dataClass= {1}", tableColumn.getFieldName(), dataClass ) );
  }

//  @Override
//  public ReportColumnInfo extactAllInfo() {
//    return null;
//  }

  @Override
  public String extractColumnLabel() {
    return tableColumn.textProperty().get();
  }

  @Override
  public Optional<String> extractFormatMask() {
    formatMask = Optional.ofNullable(tableColumn.getMask());
    log.debug( format("formatMask= {0}", formatMask.orElse(null) ) );
    return formatMask;
  }

  @Override
  public double extractFieldWidth() {

    double resultWidth = 0;

    // Field Width by Column
    if (resultWidth == 0){
      // Пытаемся назначить длину по типу данных колонки; В качестве фонта берем default шрифт
      Optional<Integer> prefColCountOpt =  DataRepColumnInfo.getPrefColumnCount(dataClass, formatMask);
      log.debug( format("dttcie.extractFieldWidth..prefColCountOpt= {0}" , prefColCountOpt.orElse(null)));
      if (prefColCountOpt.isPresent()){
        resultWidth = FXInvUtil.getFontCharWidth(prefColCountOpt.get(), ExtracterContext.getDrJasperDefFont());
        log.debug( format("dttcie.extractFieldWidth..FieldDataName= {0}; PrefColumnCount= {1}; ResultWidth= {2}", tableColumn.getFieldName(), prefColCountOpt.get(), resultWidth ) );
      }
    }
    return resultWidth;
  }

//  public Optional<String> extractFieldMask() {
//    Optional<String> res_lt = Optional.empty();
//    String labelText = bindControlInfo.getDataSetColumn();
//    Object component = bindControlInfo.getComponent();
//
//    if (component instanceof JInvCalendar) {
//      res_lt = Optional.ofNullable(((JInvCalendar) component).getMask());
//    }
//    return res_lt;
//  }

//  protected RepColumnInfo gatherColumnOnFieldInfo(BindControlInfo columnInfo) {
//    IEntityProperty ep = EntityMetadataFactory.getEntityMetaData(dsRowClass).getProperty(columnName);
//    String labelText = extractColumnLabel(columnInfo);
//    Class
//  }


  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/

  private JInvTableColumn tableColumn;
  private Class dataClass;
  private Optional<String> formatMask = Optional.empty();
}
