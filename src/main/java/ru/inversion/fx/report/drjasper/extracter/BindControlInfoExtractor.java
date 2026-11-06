package ru.inversion.fx.report.drjasper.extracter;

import javafx.scene.control.*;
import javafx.scene.text.*;
import org.slf4j.*;
import ru.inversion.dataset.fx.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.report.drjasper.*;
import ru.inversion.fx.report.drjasper.util.*;
import ru.inversion.util.*;

import java.util.*;

/**
 * <p>Description: </p>
 * Date: 22.04.2020
 * Time: 14:54
 * User:  opl
 */
@Deprecated
public class BindControlInfoExtractor implements IColumnInfoExtractor {

  protected static Logger log = LoggerFactory.getLogger(BindControlInfoExtractor.class);

  /* ********************* Class Body *****************************/
  public BindControlInfoExtractor(BindControlInfo bindControlInfo, Class dataClass) {
    this.bindControlInfo = bindControlInfo;
    this.dataClass = dataClass;
  }

//  @Override
//  public ReportColumnInfo extactAllInfo() {
//    return null;
//  }

  @Override
  public String extractColumnLabel() {

    String labelText = bindControlInfo.getDataSetColumn();
    Object component = bindControlInfo.getComponent();

    if (component instanceof IJInvControl) {
      IJInvControl control = (IJInvControl) component;
      Label labelComponent = control.getLabel();
      labelText = (labelComponent != null ? (labelComponent.getText() != null ? labelComponent.getText() : labelText) : labelText);
    }
    return labelText;
  }


  @Override
  public double extractFieldWidth() {

    Object component = bindControlInfo.getComponent();
    Control control = (Control) component;
    int prefColumnCount = 0;
    double resultWidth = 0;
    Font controlFont =  Font.getDefault();

    // Field Width by Control Class
    if (control instanceof TextField || control instanceof TextArea || control instanceof ComboBox) {
      if (control instanceof TextField) {
        prefColumnCount = ((TextField) control).getPrefColumnCount();
        controlFont = ((TextField) control).getFont();
      } else if (control instanceof TextArea) {
        prefColumnCount = ((TextField) control).getPrefColumnCount();
        controlFont = ((TextField) control).getFont();
      } else if (control instanceof ComboBox) {
        prefColumnCount = ((ComboBox) control).getEditor().getPrefColumnCount();
        controlFont = ((ComboBox) control).getEditor().getFont();
      }
      if (prefColumnCount > 0 && controlFont != null) {
        //Используем для расчета ширины
        resultWidth = FXInvUtil.getFontCharWidth(prefColumnCount, controlFont);
        log.debug( TU.format("FieldControlType( {0} ).PrefColumnCount {1}; ResultWidth= {2}", bindControlInfo.getDataSetColumn(),  prefColumnCount, resultWidth ) );
      }
    }
    // Field Width by Column
    if (resultWidth == 0){
      // Пытаемся назначить длину по типу данных колонки; В качестве фонта берем default шрифт
      Optional<Integer> prefColCountOpt =  DataRepColumnInfo.getPrefColumnCount(dataClass);
      if (prefColCountOpt.isPresent()){
        resultWidth = FXInvUtil.getDefFontCharWidth(prefColCountOpt.get());
        log.debug( TU.format("FieldDataType( {0} ).PrefColumnCount= {1}; ResultWidth= {2}", bindControlInfo.getDataSetColumn(), prefColCountOpt.get(), resultWidth ) );
      }
    }

    return resultWidth;
  }

  public Optional<String> extractFieldMask() {

    Optional<String> res_lt = Optional.empty();

    String labelText = bindControlInfo.getDataSetColumn();
    Object component = bindControlInfo.getComponent();

    if (component instanceof JInvCalendar) {
      res_lt = Optional.ofNullable(((JInvCalendar) component).getMask());
    }
    return res_lt;
  }

//  protected RepColumnInfo gatherColumnOnFieldInfo(BindControlInfo columnInfo) {
//    IEntityProperty ep = EntityMetadataFactory.getEntityMetaData(dsRowClass).getProperty(columnName);
//    String labelText = extractColumnLabel(columnInfo);
//    Class
//  }


  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/

  private BindControlInfo bindControlInfo;
  private Class dataClass;
}
