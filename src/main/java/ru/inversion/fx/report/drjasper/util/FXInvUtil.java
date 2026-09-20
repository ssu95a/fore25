package ru.inversion.fx.report.drjasper.util;

import com.sun.javafx.tk.*;
import javafx.scene.text.*;
import org.slf4j.*;
import ru.inversion.fx.app.*;
import ru.inversion.fx.app.service.*;
import ru.inversion.fx.form.controls.*;

import static ru.inversion.util.TU.format;

/**
 * <p>Description: </p>
 * Date: 05.03.2021
 * Time: 18:47
 * User:  opl
 */
public class FXInvUtil {

  protected static Logger log = LoggerFactory.getLogger(FXInvUtil.class);
  static ViewPrefAppService service;

  static {
    try {
      service = BaseApp.APP().getViewPrefService();
    } catch (AppException e) {
      throw new RuntimeException(e);
    }
  }

  /* ********************* Class Body *****************************/
  public static double getFontCharWidth(int prefColCount, Font font) {
    log.debug( format("getFontCharWidth..font= {0}", font) );
    FontMetrics metrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(font);
//    float charWidth = metrics.computeStringWidth("7");
    float charWidth = metrics.computeStringWidth("w");
    double reslt = (prefColCount) * charWidth + 5;
//    double reslt = (prefColCount + 2 ) * charWidth + 10;
    log.debug(format("PrefColCount= {0}; CharWidth= {1}; FontInfo= [{2}; {3}]; PrefColCountWidth= {4}", prefColCount, charWidth, font.getName(), font.getSize(), reslt));
    return reslt;
  }

//  public static double getFontNumericWidth(int prefColCount, Font font) {
//    FontMetrics metrics = Toolkit.getToolkit().getFontLoader().getFontMetrics(font);
////    float charWidth = metrics.computeStringWidth("7");
//    float charWidth = metrics.computeStringWidth("w");
//    double reslt = prefColCount * charWidth + 5;
//    log.debug(format("PrefColCount= {0}; CharWidth= {1}; FontInfo= [{2}; {3}]; PrefColCountWidth= {4}", prefColCount, charWidth, font.getName(), font.getSize(), reslt));
//    return reslt;
//  }


  public static double getDefFontCharWidth(int prefColCount) {
    return getFontCharWidth(prefColCount, service.getFont());
  }

  public static String getTColumnFiedldName(JInvTableColumn tableColumn) {
    String fxFieldName = tableColumn.getFieldName(); // tableColumn.getDataSetColumn();
    if (fxFieldName == null || fxFieldName.length() == 0) fxFieldName = tableColumn.getId();
    return fxFieldName;
  }



  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/


}
