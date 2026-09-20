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


import net.sf.dynamicreports.report.constant.Constants;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import org.slf4j.*;
import ru.inversion.util.*;

import java.io.Serializable;
import java.util.*;
import java.util.stream.*;

import static java.util.stream.Collectors.toMap;

/**
 * <p>DRDataSource class.</p>
 *
 * @author Ricardo Mariaca
 */
public class DRCompactDataSource implements JRRewindableDataSource, Serializable {

  private static final long serialVersionUID = Constants.SERIAL_VERSION_UID;

  protected static Logger log = LoggerFactory.getLogger( DRCompactDataSource.class );

  private String[] columns;
  private List<Object[]> values;
  private Iterator<Object[]> iterator;
  private Map<String, Object> currentRecord = new HashMap<>();
  private Map<String, Integer> invertedColIndexMap = new HashMap<>();
  private long curRecIx = -1;

  /**
   * <p>Constructor for DRDataSource.</p>
   *
   * @param columns a {@link java.lang.String} object.
   */
  public DRCompactDataSource(String... columns) {
    this.columns = columns;
    this.values = new ArrayList<>();
//    invertColIndex();
  }

  protected void invertColIndex() {
      invertedColIndexMap = IntStream.range(0, columns.length)
               .boxed()
               .collect(toMap(i -> columns[i], i -> i ));
      invertedColIndexMap.forEach((k,v) -> log.debug( TU.format("key= {0}; value= {1}", k, v  ) ));
  }

  /**
   * <p>add.</p>
   *
   * @param values a {@link java.lang.Object} object.
   */
  public void add(Object... values) {
//        Map<String, Object> row = new HashMap<String, Object>();
//        for (int i = 0; i < values.length; i++) {
//            row.put(columns[i], values[i]);
//        }
    this.values.add(values);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getFieldValue(JRField field) throws JRException {
    return currentRecord.get(field.getName());
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
      convertArr2Map(iterator.next());
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
