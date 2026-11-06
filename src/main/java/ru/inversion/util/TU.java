package ru.inversion.util;



import org.apache.commons.lang3.*;

import java.nio.*;
import java.text.*;
import java.util.*;

/**
 * <p>Description: TextUtil</p>
 * Date: 09.08.2017
 * Time: 20:27
 * User:  opl
 */
public class TU {
/* ********************* Class Body *****************************/
  public static String format(String template,  Object ... paramArr) {
    return new MessageFormat(template).format(paramArr);
  }
  

/* ********************* #Properties Getter/Setter ************************/
/* **************************** #End Properties **********************************/


}