package ru.inversion.fx.report.drjasper.util;

import javafx.scene.text.*;

/**
 * <p>Description: </p>
 * Date: 26.03.2021
 * Time: 17:23
 * User:  opl
 */
public class FXRepUtil {

  static {
    loadReportDefaultFont();
  }

  static void loadReportDefaultFont() {
    Font.loadFont(FXRepUtil.class.getResource("/net/sf/jasperreports/fonts/dejavu/DejaVuSansMono.ttf").toExternalForm(), 10);
    Font.loadFont(FXRepUtil.class.getResource("/net/sf/jasperreports/fonts/dejavu/DejaVuSansMono-Bold.ttf").toExternalForm(), 10);
    Font.loadFont(FXRepUtil.class.getResource("/net/sf/jasperreports/fonts/dejavu/DejaVuSansMono-BoldOblique.ttf").toExternalForm(), 10);
    Font.loadFont(FXRepUtil.class.getResource("/net/sf/jasperreports/fonts/dejavu/DejaVuSansMono-Oblique.ttf").toExternalForm(), 10);
  }

  public static void dummy() {};


  /* ********************* Class Body *****************************/

  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/


}
