package ru.inversion.fx.report.drjasper.util;

import net.sf.dynamicreports.report.constant.*;
import net.sf.jasperreports.engine.*;
import org.slf4j.*;
import ru.inversion.fx.report.drjasper.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.toMap;
import static ru.inversion.util.TU.format;

/**
 * <p>Description: </p>
 * Date: 21.05.2021
 * Time: 20:00
 * User:  opl
 */
public class DREntityDataSource<T> implements JRRewindableDataSource, Serializable {

  private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

  protected static Logger log = LoggerFactory.getLogger(DRIxColumnsDataSource.class);

  //  private String[] columns;
  private DataRepColumnInfo<T>[] columns;
  private List<T> values;
  private Iterator<T> iterator;
  private Map<String, Object> currentRecord = new HashMap<>();
  private Map<String, DataRepColumnInfo> invertedColIndexMap = new HashMap<>();
  private long curRecIx = -1;
  private T curRecord;

  /**
   * <p>Constructor for DRDataSource.</p>
   *
   * @param columns a {@link String} object.
   */
  public DREntityDataSource(DataRepColumnInfo<T>... columns) {
    log.debug("DREntityDataSource being creating ...");
    this.columns = columns;
    this.values = new ArrayList<>();
//    this.values = new ArrayList<>();
    invertColIndex();
  }


  /**
   * <p>add.</p>
   *
   * @param pojo a {@link Object} object.
   */
  public void add(T pojo) {
//        Map<String, Object> row = new HashMap<String, Object>();
//        for (int i = 0; i < values.length; i++) {
//            row.put(columns[i], values[i]);
//        }
    this.values.add(pojo);
  }

  protected void invertColIndex() {
    invertedColIndexMap = IntStream.range(0, columns.length)
      .boxed()
      .collect(toMap(i -> columns[i].getFxFieldName(), i -> columns[i]));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getFieldValue(JRField field) throws JRException {
    DataRepColumnInfo<T> colInfo = invertedColIndexMap.get(field.getName());
    Object reslt = colInfo.getDataValue(curRecord);
    if (values.size() <= 10) {
      String value4logStr = null;
      if (reslt instanceof String) {
        value4logStr = (String) reslt;
        value4logStr = value4logStr.substring(0, value4logStr.length() > 500 ? 500 : value4logStr.length());
      }
      log.debug(format("DREntityDataSource.FN= {0}; value= {1}", field.getName(), value4logStr != null ? value4logStr : reslt));
    }
    return reslt;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean next() throws JRException {
    if (iterator == null) {
      this.iterator = values.iterator();
    }
    boolean hasNext = iterator.hasNext();
    if (hasNext) {
      curRecord = iterator.next();
    }
    return hasNext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moveFirst() throws JRException {
    iterator = null;
  }
}
