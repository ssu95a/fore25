package ru.inversion.fx.report.drjasper;

import net.sf.dynamicreports.report.builder.column.*;
import net.sf.dynamicreports.report.constant.*;
import net.sf.dynamicreports.report.definition.datatype.*;
import org.slf4j.*;
import ru.inversion.dataset.fx.*;
import ru.inversion.meta.*;
import ru.inversion.util.*;

import java.math.*;
import java.text.*;
import java.time.*;
import java.util.*;

import static net.sf.dynamicreports.report.builder.DynamicReports.col;
import static net.sf.dynamicreports.report.builder.DynamicReports.type;
import static net.sf.dynamicreports.report.constant.TextAdjust.STRETCH_HEIGHT;
import static ru.inversion.util.TU.format;


/**
 * <p>Description: </p>
 * Date: 21.02.2021
 * Time: 17:28
 * User:  opl
 */
public class DataRepColumnInfo<T> extends AbstractRepColumnInfo implements IDRJasperConst {

  protected static Logger log = LoggerFactory.getLogger(DataRepColumnInfo.class);

  public DataRepColumnInfo(Class rowClass, String fxFieldName, Class fxDataClass, String fxColumnLabel, double fxColumnWidth) {
    super(fxColumnLabel, fxColumnWidth);
    this.fxFieldName = fxFieldName;
    this.fxDataClass = fxDataClass;
    this.rowClass = rowClass;
    log.debug(format("DataRepColumnInfo.new..fxFieldName= {0}; fxColumnLabel= {1}; fxColumnWidth= {2, number, #.##}; fxDataClass= {3}; RowClass= {4}",
      fxFieldName, fxColumnLabel, fxColumnWidth, fxDataClass, rowClass.getSimpleName()));
  }


  public DataRepColumnInfo(Class rowClass,
                           String fxFieldName,
                           Class fxDataClass,
                           String fxColumnLabel,
                           double fxColumnWidth,
                           Optional<String> fxFormatMask) {
    super(fxColumnLabel, fxColumnWidth);
    this.fxFieldName = fxFieldName;
    this.fxDataClass = fxDataClass;
    this.fxFormatMask = fxFormatMask;
    this.rowClass = rowClass;

    log.debug(format("DataRepColumnInfo.new..fxFieldName= {0}; fxColumnLabel= {1}; fxColumnWidth= {2, number, #.##}; fxFormatMask= {3}; fxDataClass= {4}; rowClass= {5}",
      fxFieldName, fxColumnLabel, fxColumnWidth, fxFormatMask, fxDataClass, rowClass.getSimpleName()));
  }

  @Override
  public DataRepColumnInfo init() {
    log.debug( format("DataRepColumnInfo.INIT of ( {0} )", fxFieldName ) );
    djFormatMask = fxFormatMask.orElseGet(() -> detectDRJasperFormatMask());
    propertyDefenition = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(fxFieldName);
    detectFormatter();
    detectDynamicJasperColumn();
    return this;
  }
  /* ********************* Class Body *****************************/

  public Object getDataValue(T row) {
//    Class rowClass = row.getClass();
//    IEntityProperty pd = EntityMetadataFactory.getEntityMetaData(rowClass).getProperty(fxFieldName);
    IEntityProperty pd = propertyDefenition;
    final StubObservableValue objVal = new StubObservableValue(pd, fxFieldName, null);
    objVal.setPojoInstance(row);
    Object fieldValue = objVal.getValue();
    Object reslt = null;

    if (fieldValue != null) {
      if (pd.getType().isInstance(booleanExample)) reslt = (Boolean) fieldValue ? "*" : "";
      else if (pd.getType().isInstance(localDateExample)) reslt = java.sql.Date.valueOf((LocalDate) fieldValue);
      else if (pd.getType().isInstance(localDateTimeExample)) reslt = java.sql.Timestamp.valueOf((LocalDateTime) fieldValue);
      else reslt = fieldValue;
    }
    // Фиксируем длину колонки для неопознанных колонок
//    if (getFxColumnWidth() == 0 && pd.getType().isInstance(stringExample)) {
//      String sres = (String) reslt;
//      if (sres != null && sres.length() > maxDataLength) maxDataLength = sres.length();
//    }
    detectValueLength(fieldValue);
    
    return reslt;
  }

  /**
   * Ключевой момент это обнуленная ширина для поля
   * Если данных мало но лейбл существенно больше ширина должна устанавливаться по лейблу - 15
   * лейбл должен добавляться
   *
   * @param value
   */
  private void detectValueLength(Object value) {
    if (value != null && getFxColumnWidth() == 0 && isColumnWidthCorrectable()) {
      String sres = null;
      if (fxDataClass.isInstance(stringExample)) {
        sres = (String) value;
      } else { // Числовые типы
        DecimalFormat decFormat = decimalFormat.orElseGet(() -> new DecimalFormat("#0.00##"));
        sres = decFormat.format(value);
      }
      if (sres != null && sres.length() > maxDataLength) maxDataLength = sres.length();
    } else if ( value != null ) {
      maxDataLength = 0;
    }
  }

  public static Optional<Integer> getPrefColumnCount(Class fxDataClass) {
    Optional<Integer> reslt = Optional.empty();
    if (fxDataClass.isInstance(localDateExample) || fxDataClass.isInstance(sqlDateExample)) reslt = Optional.of(10); //"dd.MM.yyyy";
    else if (fxDataClass.isInstance(localDateTimeExample) || fxDataClass.isInstance(timeStampExample) || fxDataClass.isInstance(dateExample) ) reslt = Optional.of(19); //"dd.MM.yyyy HH:mm:ss";
    else if (fxDataClass.isInstance(longExample)) reslt = Optional.of(18); // = "#0";
    else if (fxDataClass.isInstance(integerExample)) reslt = Optional.of(14); //"#0";
    else if (fxDataClass.isInstance(bigDecimalExample)) reslt = Optional.of(22); //"#0.00##";
    else if (fxDataClass.isInstance(booleanExample)) reslt = Optional.of(12);

    return reslt;
  }

  public static Optional<Integer> getPrefColumnCount(Class fxDataClass, Optional<String> colFormatMask) {
    Optional<Integer> reslt = Optional.empty();
    if ( fxDataClass.isInstance(localDateExample) || fxDataClass.isInstance(sqlDateExample) )
      reslt = (colFormatMask.isPresent() ? Optional.of(colFormatMask.get().length()) : Optional.of(11)); //"dd.MM.yyyy";
    else if ( fxDataClass.isInstance(localDateTimeExample) || fxDataClass.isInstance(timeStampExample) || fxDataClass.isInstance(dateExample) )
      reslt = (colFormatMask.isPresent() ? Optional.of(colFormatMask.get().length()) : Optional.of(20)); //"dd.MM.yyyy HH:mm:ss";

//    else if (fxDataClass.isInstance(new Long(1))) reslt = (colFormatMask.isPresent() ? Optional.of(colFormatMask.get().length()) : Optional.of(16)); // = "#0";
//    else if (fxDataClass.isInstance(new Integer(1))) reslt = (colFormatMask.isPresent() ? Optional.of(colFormatMask.get().length()) : Optional.of(12)); //"#0";
//    else if (fxDataClass.isInstance(new BigDecimal(1))) reslt = (colFormatMask.isPresent() ? Optional.of(colFormatMask.get().length()) : Optional.of(20)); //"#0.00##";
//    else if (fxDataClass.isInstance(new Boolean(true))) reslt = Optional.of(8);
    
//    if (fxDataClass.isInstance(LocalDateTime.now())) reslt = Optional.of(16); //"dd.MM.yyyy HH:mm:ss";
//    else if (fxDataClass.isInstance(LocalDate.now())) reslt = Optional.of(20); //"dd.MM.yyyy";
//    else if (fxDataClass.isInstance(new Long(1))) reslt = Optional.of(18); // = "#0";
//    else if (fxDataClass.isInstance(new Integer(1))) reslt = Optional.of(14); //"#0";
//    else if (fxDataClass.isInstance(new BigDecimal(1))) reslt = Optional.of(22); //"#0.00##";
//    else if (fxDataClass.isInstance(new Boolean(true))) reslt = Optional.of(12);
    return reslt;
  }

  private void detectFormatter() {
    if ( isColumnNumeric() ) {
      decimalFormat = Optional.of(new DecimalFormat(djFormatMask));
      log.debug( format("decimalFormat= {0}", djFormatMask ) );
    }
  }

  public boolean isColumnWidthCorrectable() {
    return fxDataClass.isInstance(stringExample) || isColumnNumeric();
  }

  public boolean isColumnNumeric() {
    return fxDataClass.isInstance(longExample)
      || fxDataClass.isInstance(integerExample) || fxDataClass.isInstance(bigDecimalExample);
  }

  //  public Optional <String> detectFormatMaskFromType() {
  public String detectDRJasperFormatMask() {
//    return Optional.ofNullable(this.fxFormatMask.orElseGet(() -> {
    String res_lt = "";
    log.debug(format("fxDataClass= {0}", fxDataClass));

    if (this.fxDataClass.isInstance(localDateTimeExample)
      || this.fxDataClass.isInstance( timeStampExample ) || fxDataClass.isInstance(dateExample)) res_lt = "dd.MM.yyyy HH:mm:ss";
    else if (this.fxDataClass.isInstance(localDateExample) || fxDataClass.isInstance(sqlDateExample)) res_lt = "dd.MM.yyyy";
//    else if (this.fxDataClass.isInstance(new Long(1))) res_lt = "##################0";
//    else if (this.fxDataClass.isInstance(new Integer(1))) res_lt = "##################0";
//    else if (this.fxDataClass.isInstance(new BigDecimal(1))) res_lt = "##################0.00##";
    else if (this.fxDataClass.isInstance(longExample)) res_lt = "#0";
    else if (this.fxDataClass.isInstance(integerExample)) res_lt = "#0";
    else if (this.fxDataClass.isInstance(bigDecimalExample)) res_lt = "#0.00##";

    log.debug(format("Column *{0}* Format Mask from Type *{1}*= {2}", fxFieldName, fxDataClass, res_lt));

    return res_lt;
//    }));
  }

  int getColRelWidth(/*double tableWidth,*/) {
    log.debug(format("Column Width= {0}", getFxColumnWidth()));
    return (int) getFxColumnWidth();
  }

  public void detectDynamicJasperColumn(/*int tableWidth*/) {
    DRIDataType res_lt = null;
    if (this.fxDataClass.isInstance(stringExample))
      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.stringType()).setWidth((int) getFxColumnWidth());
    else if (this.fxDataClass.isInstance(localDateTimeExample)
              || this.fxDataClass.isInstance(localDateExample.now())
              || this.fxDataClass.isInstance(dateExample)
              || this.fxDataClass.isInstance(sqlDateExample)
              || this.fxDataClass.isInstance(timeStampExample) )
      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.dateType()).setPattern(djFormatMask).setWidth((int) getFxColumnWidth());
//    else if (this.fxDataClass.isInstance(LocalDate.now())) dynJasperDataType = type.dateType();
    else if (this.fxDataClass.isInstance(longExample)) // dynJasperDataType = type.longType();
      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.longType()).setPattern(djFormatMask).setWidth((int) getFxColumnWidth());
    else if (this.fxDataClass.isInstance(integerExample)) //dynJasperDataType = type.integerType();
      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.integerType()).setPattern(djFormatMask).setWidth((int) getFxColumnWidth());
    else if (this.fxDataClass.isInstance(bigDecimalExample)) //dynJasperDataType = type.bigDecimalType();
      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.bigDecimalType()).setPattern(djFormatMask).setWidth((int) getFxColumnWidth());
    else if (this.fxDataClass.isInstance(booleanExample)) // dynJasperDataType = type.booleanType();
      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.stringType()).setWidth((int) getFxColumnWidth()).setHorizontalTextAlignment(HorizontalTextAlignment.CENTER);
//      textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.booleanType()).setWidth(new Double(getFxColumnWidth()).intValue());
    log.debug(format("Column *{0}* Dynamic Jasper Column from Class *{1}*= {2}", fxFieldName, fxDataClass, textColumn));
//    textColumn.setStretchWithOverflow(true);
//    !Production
    textColumn.setTextAdjust(STRETCH_HEIGHT);
    
    log.debug(format("Column *{0}* Dynamic Jasper Column from Class *{1}*= {2}", fxFieldName, fxDataClass, textColumn.getName()));
  }

  public void buildRepTextColumn() {
    textColumn = col.column(getFxColumnLabel(), getFxFieldName(), type.longType()).setPattern("##################0").setWidth(3000);
  }

  public int getCorrectedPrefColumnLength() {
    
    int prefColLength = 0;
    int dataColLength = 0;

    int maxDataLength = getMaxDataLength();
    int labelLength = getFxColumnLabel().length();

    log.debug( format("CorrectedPrefColumnLength.fxFieldName= {0}; maxDataLength= {1}; labelLength= {2} "
        , fxFieldName, maxDataLength, labelLength ) );

//    if (fxDataClass.isInstance(stringExample)) {
      if (maxDataLength > c_longStringPrefLengh) {
        prefColLength = - c_longStringPrefLengh;
        log.debug( "V1..prefColLength = c_longStringPrefLengh..( maxDataLength >= c_longStringPrefLengh )" );
        return prefColLength + 1;
      } else if (maxDataLength > c_minStringPrefLength /*&& labelLength > c_minStringPrefLength&& maxDataLength > labelLength*/ ) {
        // Разбить на два варианта: по заголовку и по данным
//        prefColLength = labelLength;
        log.debug( "V2..prefColLength = maxDataLength..( maxDataLength > c_minStringPrefLength )" );
        prefColLength = maxDataLength + 1;
        return prefColLength;
//      } else if (maxDataLength > c_minStringPrefLength && labelLength > c_minStringPrefLength
//            && maxDataLength < labelLength) {
//        // Разбить на два варианта: по заголовку и по данным
//        prefColLength = maxDataLength;
//        return prefColLength;
      } else if (maxDataLength < c_minStringPrefLength && labelLength < c_minStringPrefLength
        && maxDataLength >= labelLength) {
        prefColLength = maxDataLength  + 1;
        log.debug( "V3..prefColLength = maxDataLength..( maxDataLength < c_minStringPrefLength && labelLength < c_minStringPrefLength && maxDataLength > labelLength )" );
        return prefColLength;
      } else if (maxDataLength < c_minStringPrefLength && labelLength < c_minStringPrefLength
        && maxDataLength < labelLength) {
        log.debug( "V4..prefColLength = labelLength..( maxDataLength < c_minStringPrefLength && labelLength < c_minStringPrefLength && maxDataLength < labelLength )" );
        prefColLength = labelLength +1;
        return prefColLength;
      } else {
        log.debug( "V5.else.prefColLength = c_minStringPrefLength" );
        return c_minStringPrefLength + 1;
      }
//    }
  }

  public void setProperty(String key, String value) {
    log.debug( TU.format("textColumn.name= {0}; property.key=*{1}*; value=*{2}*", textColumn.getName(), key, value) );
    textColumn.addProperty(key, value);
//    colProperties.setProperty(key, value);
  }

  /* ********************* #Properties Getter/Setter ************************/

  public String getFxFieldName() { return fxFieldName; }

  public Class getFxDataClass() { return fxDataClass; }

  public String getDjFormatMask() { return djFormatMask; }

  public TextColumnBuilder getTextColumnBuilder() {
    log.debug(format("RepDataColumnInfo.getTextColumnBuilder()= {0}", this));
    return textColumn;
  }

  public void setMaxDataLength(int maxDataLength) { this.maxDataLength = maxDataLength; }

  public int getMaxDataLength() { return maxDataLength; }

  @Override
  public void setFxColumnWidth(double fxColumnWidth) {
    super.setFxColumnWidth(fxColumnWidth);
    if (textColumn != null) textColumn.setWidth((int) fxColumnWidth);
  }

  public Optional<DecimalFormat> getDecimalFormat() { return decimalFormat; }

  public Properties getColProperties() { return colProperties; }

  /* **************************** #End Properties **********************************/
  private String fxFieldName;
  private Class fxDataClass;
  private Optional<String> fxFormatMask = Optional.empty();
  private Class rowClass;


  private String djFormatMask;
  private double prefColDataCount;
  private double prefColHeaderCount;
  private IEntityProperty propertyDefenition;

  private int maxDataLength = -1; // Соответствует пустым значениям в колонке

  private DRIDataType dynJasperDataType = null;


  private TextColumnBuilder textColumn;

  private Optional<DecimalFormat> decimalFormat = Optional.empty();

  private Properties colProperties = new Properties();


}
