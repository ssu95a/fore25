package ru.inversion.util;

import org.apache.commons.lang3.*;

import java.nio.*;
import java.util.*;

/**
 * <p>Description: </p>
 * Date: 12.05.2021
 * Time: 23:18
 * User:  opl
 */
public class RandomGUID {

  /* ********************* Class Body *****************************/
  public static String getBase64guid(){
    UUID uuid = UUID.randomUUID();
    ByteBuffer uuidBytes = ByteBuffer.wrap(new byte[16]);
    uuidBytes.putLong(uuid.getMostSignificantBits());
    uuidBytes.putLong(uuid.getLeastSignificantBits());

    String simpleBase64 = Base64.getEncoder().encodeToString( uuidBytes.array() );

    return StringUtils.stripEnd( simpleBase64.replace('/', '_').replace( '+', '-' ),  "=" );

  }
  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/


}
