package ru.inversion.fx.report.drjasper.extracter;

import javafx.scene.text.*;

/**
 * <p>Description: </p>
 * Date: 25.03.2021
 * Time: 16:19
 * User:  opl
 */
public class ExtracterContext {

  private static Font drJasperDefFont;

  /* ********************* Class Body *****************************/

  /* ********************* #Properties Getter/Setter ************************/

  public static void setDrJasperDefFont(Font drJasperDefFont) { ExtracterContext.drJasperDefFont = drJasperDefFont; }

  public static Font getDrJasperDefFont() { return drJasperDefFont; }
  /* **************************** #End Properties **********************************/


}
