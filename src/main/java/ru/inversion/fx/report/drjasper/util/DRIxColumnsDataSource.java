package ru.inversion.fx.report.drjasper.util;

/**
 * <p>Description: </p>
 * Date: 17.05.2021
 * Time: 21:11
 * User:  opl
 */
/*
 * DynamicReports - Free Java reporting library for creating reports dynamically
 *
 * Copyright (C) 2010 - 2018 Ricardo Mariaca and the Dynamic Reports Contributors
 *
 * This file is part of DynamicReports.
 *
 * DynamicReports is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DynamicReports is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with DynamicReports. If not, see <http://www.gnu.org/licenses/>.
 */


import net.sf.dynamicreports.report.constant.*;
import net.sf.jasperreports.engine.*;
import org.slf4j.*;
import ru.inversion.util.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.*;
import static ru.inversion.util.TU.format;

/**
 * <p>DRDataSource class.</p>
 *
 * @author Ricardo Mariaca
 */
public class DRIxColumnsDataSource implements JRRewindableDataSource, Serializable {

  private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

  protected static Logger log = LoggerFactory.getLogger(DRIxColumnsDataSource.class);

  private String[] columns;
  private List<Object[]> values;
  private Iterator<Object[]> iterator;
  private Map<String, Object> currentRecord = new HashMap<>();
  private Map<String, Integer> invertedColIndexMap = new HashMap<>();
  private long curRecIx = -1;
  private Object[] curRecordArr;

  /**
   * <p>Constructor for DRDataSource.</p>
   *
   * @param columns a {@link String} object.
   */
  public DRIxColumnsDataSource(String... columns) {
    log.debug("DRIxColumnsDataSource being creating ...");
    this.columns = columns;
    this.values = new ArrayList<>();
    invertColIndex();
  }

  protected void invertColIndex() {

    for (int i = 0; i < columns.length; i++) {
      log.debug( format("DRIxColumnsDataSource.invertColIndex.colName={0}; index= {1}", columns[i], i) );
    }

    invertedColIndexMap = IntStream.range(0, columns.length)
      .boxed()
      .collect(toMap(i -> columns[i], i -> i));
    invertedColIndexMap.forEach((k, v) -> log.debug(format("invertColIndex.key= {0}; value= {1}", k, v)));
  }

  /**
   * <p>add.</p>
   *
   * @param vals a {@link Object} object.
   */
  public void add(Object... vals) {
//        Map<String, Object> row = new HashMap<String, Object>();
//        for (int i = 0; i < values.length; i++) {
//            row.put(columns[i], values[i]);
//        }
    if (this.values.size() < 50) {
      log.debug( "DRIxColumnsDataSource ADD(); < 50" );
      IntStream.range(0, columns.length)
        .forEach(i -> {
          Object value = vals[i];
          String value4logStr = null;
          if (value instanceof String) {
            value4logStr = (String) value;
            value4logStr = value4logStr.substring(0, value4logStr.length() > 500 ? 500 : value4logStr.length());
          }
          log.debug(format("DRIxColumnsDataSource.add.FN= {0}; value= {1}", columns[i], value4logStr != null ? value4logStr : value));
        });
    }
    this.values.add(vals);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getFieldValue(JRField field) throws JRException {
    int i = invertedColIndexMap.get(field.getName());
    return curRecordArr[i];
  }

  protected Map<String, Object> convertArr2Map(Object[] arr) {
    HashMap<String, Object> reslt = new HashMap<>();
//      IntStream.range(0, alphabet.size())
//               .boxed()
//               .collect(toMap(alphabet::get, i -> i));
//      return IntStream.range(0, arr.length)
//               .boxed()
//               .collect(toMap(i -> columns[i], i -> arr[i]));
//        Map<String, Object> row = new HashMap<String, Object>();

    for (int i = 0; i < columns.length; i++) {
      currentRecord.put(columns[i], arr[i]);
    }
    return currentRecord;
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
//      convertArr2Map(iterator.next());
      curRecordArr = iterator.next();
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
