package ru.inversion.fx.jinvtblexport.property;

import java.util.*;

/**
 * <p>Description: </p>
 * Date: 19.03.2021
 * Time: 12:34
 * User:  opl
 */
public enum RecordSourceEnum {

  LOADED, ALL, MARKED;

  static ResourceBundle bundle;
  static {
    bundle = ResourceBundle.getBundle(RecordSourceEnum.class.getName());
  }

  public String getLabel() {
    return bundle.getString(this.name());
  }

  @Override
  public String toString() {
    return getLabel();

  }


}
