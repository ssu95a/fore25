package ru.inversion.fx.report.drjasper.util;

import org.slf4j.*;
import ru.inversion.meta.*;
import ru.inversion.util.*;

import java.util.*;

/**
 * <p>Description: </p>
 * Date: 20.02.2021
 * Time: 17:38
 * User:  opl
 */
public class DataRepUtl {

   protected static Logger log = LoggerFactory.getLogger( DataRepUtl.class );

  /* ********************* Class Body *****************************/
  public static Optional<Class> getFieldType(Class dsRowClass, String fxFieldName) {

    Optional<Class> fieldClass = Optional.empty();
    if (fxFieldName != null && !fxFieldName.isEmpty() ) {
      log.debug( TU.format("DataRepUtl.getFieldType.fieldName= {0}; dsRowClass= {1}", fxFieldName, dsRowClass.getName() ) );
      IEntityProperty ep = EntityMetadataFactory.getEntityMetaData(dsRowClass).getProperty(fxFieldName);
      if (ep != null) {
        fieldClass = Optional.ofNullable(ep.getType());
      }
    }
    return fieldClass;

  }
  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/


}
