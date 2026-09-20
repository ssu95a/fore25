package ru.inversion.fx.report.drjasper;

import javafx.collections.*;
import javafx.scene.control.*;
import javafx.scene.text.*;
import net.sf.dynamicreports.jasper.builder.*;
import net.sf.dynamicreports.jasper.builder.export.*;
import net.sf.dynamicreports.report.builder.column.*;
import net.sf.dynamicreports.report.builder.grid.*;
import net.sf.dynamicreports.report.builder.style.FontBuilder;
import net.sf.dynamicreports.report.constant.*;

import net.sf.dynamicreports.report.exception.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.*;
import net.sf.jasperreports.engine.util.*;
import org.apache.commons.io.*;
import org.slf4j.*;
import ru.inversion.dataset.*;
import ru.inversion.dataset.fx.*;
import ru.inversion.db.expr.*;
import ru.inversion.fx.form.*;
import ru.inversion.fx.form.controls.*;
import ru.inversion.fx.jinvtblexport.property.*;
import ru.inversion.fx.jinvtblexport.property.PageOrientEnum;
import ru.inversion.fx.report.drjasper.extracter.*;
import ru.inversion.fx.report.drjasper.util.*;
import ru.inversion.utils.*;

import javax.persistence.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

import static javax.persistence.ParameterMode.IN;
import static net.sf.dynamicreports.report.builder.DynamicReports.*;
import static net.sf.dynamicreports.report.builder.DynamicReports.report;
import static ru.inversion.fx.jinvtblexport.property.SizeExportedDataEnum.FULL_DATA;
import static ru.inversion.fx.jinvtblexport.property.SizeExportedDataEnum.FULL_TABLE;
import static ru.inversion.fx.report.drjasper.util.FXInvUtil.getTColumnFiedldName;
import static ru.inversion.util.TU.format;
//import static ru.inversion.fx.report.djasper.TU.format;

@NamedStoredProcedureQueries(
  value = {
//    procedure Form_Audit.audit_action
//    ( p_form_name in varchar2
//    , p_item_type in varchar2
//    , p_item_name in varchar2
//    , p_ext_info  in clob default null
//    )
    @NamedStoredProcedureQuery(
      name = "Form_Audit.audit_action",
      procedureName = "Form_Audit.audit_action",
      parameters = {
        @StoredProcedureParameter(mode = IN, name = "p_form_name", type = String.class),
        @StoredProcedureParameter(mode = IN, name = "p_item_type", type = String.class),
        @StoredProcedureParameter(mode = IN, name = "p_item_name", type = String.class),
        @StoredProcedureParameter(mode = IN, name = "p_ext_info", type = String.class) } ) //,
  })

/**
 * <p>Description: </p>
 * Date: 17.04.2020
 * Time: 16:58
 * User:  opl
 */
public class RepJInvTableUtil<T> implements IDRJasperConst, IReportExporterExtention {

  private static FontBuilder defFontBuilder;
  private static Font drJasperDefFont;

  static {
    defFontBuilder = DRJasperUtil.buildDefFontBuilder();

    drJasperDefFont = DRJasperUtil.getDefaultDRJasperFont(defFontBuilder);
    ExtracterContext.setDrJasperDefFont(drJasperDefFont);
  }

  protected static Logger log = LoggerFactory.getLogger(RepJInvTableUtil.class);

  /* ********************* Class Body *****************************/

  public RepJInvTableUtil(JInvTable<T> jinvTable,
                          PageSizeEnum pageSizeEnum,
                          PageOrientEnum pageOrientEnum,
                          SizeExportedDataEnum sizeExportedData,
                          RecordSourceEnum recordSource,
                          File outFile) {
    this.jinvTable = jinvTable;
    this.outFile = outFile;
    this.fileExt = FilenameUtils.getExtension(outFile.getName()).toLowerCase();
    transformPageSize(pageSizeEnum);
    transformPageOrient(pageOrientEnum);
    this.sizeExportedData = sizeExportedData;
    this.recordSource = recordSource;
  }


  //  Ммм, т.е. я вызываю public static void Audit_Action(AbstractBaseController fxCtrl,
//            String formName,
//            String itemType,
//            String itemName,
//            String extInfo) и на этом все ?
//  Form_Audit.Audit_Action(this,
//                        <название контролера>,
//                        <тип объекта(кнопка, закладка и т.п.)>,
//                        <название объекта>,
//                        <ext info>);
  public RepJInvTableUtil(AbstractBaseController fxFormCtrl,
                          JInvTable<T> jinvTable,
                          PageSizeEnum pageSizeEnum,
                          PageOrientEnum pageOrientEnum,
                          SizeExportedDataEnum sizeExportedData,
                          RecordSourceEnum recordSource,
                          File outFile) {
    this.fxFormCtrl = fxFormCtrl;
    this.jinvTable = jinvTable;
    this.outFile = outFile;
    this.fileExt = FilenameUtils.getExtension(outFile.getName()).toLowerCase();
    transformPageSize(pageSizeEnum);
    transformPageOrient(pageOrientEnum);
    this.sizeExportedData = sizeExportedData;
    this.recordSource = recordSource;
  }


  public void transformPageSize(PageSizeEnum pageSizeEnum) {
    if (pageSizeEnum != PageSizeEnum.AUTO) {
      pageSize = Optional.of(PageType.valueOf(pageSizeEnum.name()));
    }
    log.debug(format("pageSize= {0}", pageSize.orElse(null)));
  }

  public void transformPageOrient(PageOrientEnum pageOrientEnum) {
    pageSize.ifPresent(p -> pageOrientation = Optional.of(PageOrientation.valueOf(pageOrientEnum.name())));
    log.debug(format("pageOrientation= {0}", pageOrientation.orElse(null)));
  }

  /**
   * $Method for External Run$
   */
  public void init() {
    dsfxAdapter = getDataSetAdapter();
    dsRowClass = dsfxAdapter.getDataSet().getRowClass();
    dataSet = dsfxAdapter.getDataSet();
  }

  /**
   * $Method for External Run$
   *
   * @throws DRException
   * @throws DataSetException
   * @throws IOException
   */
  public void exportJInvTableData() throws DRException, DataSetException, IOException {
    // Проверить что в dataSet есть строчки
    // Формирование списка колонок отчета и иерархического списка групп колонок
    // Если у колонки несть тип, то она истинная колонка, если типа нет то это только заколовок
    // Формирование списка строк с данными, которые будут использоваться в качестве источника
    prepareReportColumnInfo();
    buildReport();
    log.debug("EXPORT to *" + outFile.getAbsolutePath() + "* is COMPLETED!");
  }

  public AbstractRepColumnInfo[] getAllColumns() {
    return null;
  }

  public void prepareReportColumnInfo() {
    // Не забыть (ниже) выражение для фильтра
// Колонка не должна содержать подколонок
//    if ((ifExportInvisibeColumns || column.isVisible()) && column instanceof JInvTableColumn
//          && column.getColumns().size() == 0)
    log.debug(format("jinvTable.getWidth()= {0}", jinvTable.getWidth()));

    ObservableList<TableColumn<T, ?>> tColumnList = jinvTable.getColumns();
    List<AbstractRepColumnInfo> tableColList = new ArrayList<>();

    repColumnInfoGroupList = collectTableColumnTree(jinvTable.getColumns());

    if (this.sizeExportedData == FULL_DATA) {
      repColumnInfoGroupList.addAll(buildBindedFieldCoumnList());
    }
    // Необходимо добрать список колонок
    buildDataRepColumnInfoMap(repColumnInfoGroupList);
    buildRepColumn_ColumnGroup();
//    dataSet.forEach();
  }

  protected void buildRepColumn_ColumnGroup() {
    colGridComponentBuilderArr = collectDJRepColumn_ColumnGroup(repColumnInfoGroupList);
    log.debug(format("colGridComponentBuilderArr= {0}", colGridComponentBuilderArr));
    colBuilderArr = collectDJRepColumn();
  }

  protected ColumnGridComponentBuilder[] collectDJRepColumn_ColumnGroup(List<AbstractRepColumnInfo> rcInfoGroupList) {
    log.debug(format("rcInfoGroupList= {0}", repColumnInfoGroupList));
    ColumnGridComponentBuilder[] columnGridComponentBuilder =
      rcInfoGroupList.stream()
        .map(rci -> {
          if (rci instanceof DataRepColumnInfo) {
            log.debug(format("CollectGroup.RepDataColumnInfo.name= {0}", ((DataRepColumnInfo) rci).getFxFieldName()));
            return ((DataRepColumnInfo) rci).getTextColumnBuilder();
          } else /*if (rci instanceof RepGroupColumnInfo)*/ {
            log.debug(format("CollectGroup.RepGroupColumnInfo.title= {0}", ((GroupRepColumnInfo) rci).getFxColumnLabel()));
            return grid.titleGroup(((GroupRepColumnInfo) rci).getFxColumnLabel(), collectDJRepColumn_ColumnGroup(((GroupRepColumnInfo) rci).getSubColumns()));
          }
//        return null;
        }).toArray(n -> new ColumnGridComponentBuilder[n]);
    log.debug(format("columnGridComponenBuilder= {0}", columnGridComponentBuilder));
    return columnGridComponentBuilder;
  }

  protected ColumnBuilder[] collectDJRepColumn() {
    return dataColInfoMap.keySet().stream()
      .map(key -> dataColInfoMap.get(key).getTextColumnBuilder())
      .toArray(n -> new ColumnBuilder[n]);
  }

  protected String[] getRowColumnNameAsArr() {
    return dataColInfoMap.keySet().stream()
//        .map( key -> dataColInfoMap.get(key) )
      .toArray(n -> new String[n]);
  }

  protected DataRepColumnInfo<T>[] getColInfoAsArr() {
    return dataColInfoMap.values().stream()
//        .map( key -> dataColInfoMap.get(key) )
      .toArray(n -> new DataRepColumnInfo[n]);
  }

  protected Object[] getRowDataAsArr(T row) {
//    return dataColInfoMap.keySet().stream()
//      .map(idColumn -> {
//        IEntityProperty pd = EntityMetadataFactory.getEntityMetaData(dsRowClass).getProperty(idColumn);
//        final StubObservableValue obVal = new StubObservableValue(pd, idColumn, null);
//        obVal.setPojoInstance(row);
//        return pd.getType().isInstance(new Boolean(true)) ? ( (Boolean) obVal.getValue() ? "*" : "" ) : obVal.getValue() ;
    return dataColInfoMap.entrySet().stream()
      .map(entry -> {
        return entry.getValue().getDataValue(row);
      }).toArray(n -> new Object[n]);
  }

  //  private boolean isTableColumnBeingAccepted(JInvTableColumn tableColumn) {
  private boolean isTableColumnAcceptable(TableColumn tableColumn) {
    return tableColumn.isVisible()
      || (!tableColumn.isVisible() && sizeExportedData == FULL_TABLE)
      || (!tableColumn.isVisible() && sizeExportedData == FULL_DATA);
  }

  protected List<AbstractRepColumnInfo> collectTableColumnTree(ObservableList<TableColumn<T, ?>> tColumnList) {
    log.debug(format("COLUMNS.columns= {0}", tColumnList));
    return tColumnList.stream()
//      .filter(col -> col instanceof JInvTableColumn && isTableColumnBeingAccepted((JInvTableColumn) col))
      .filter(col -> col instanceof TableColumn && isTableColumnAcceptable(col)
        && isColumnTypeAcceptable(dsRowClass, col))
      .map(RepJInvTableUtil.this::gatherRepColumnInfoByTableColumn)
      .filter(arci -> {
        boolean reslt = true;
        if (arci instanceof GroupRepColumnInfo) {
          reslt = ((GroupRepColumnInfo) arci).getSubColumns().size() > 0;
        }
        return reslt;
      }).collect(Collectors.toList());
  }

  protected void buildDataRepColumnInfoMap(List<AbstractRepColumnInfo> repColumnInfoList) {
    log.debug(format("repColumInfoList= {0}", repColumnInfoList));
    repColumnInfoList.forEach(colInfo -> {
      if (colInfo instanceof DataRepColumnInfo) {
        dataColInfoMap.put(((DataRepColumnInfo) colInfo).getFxFieldName(), (DataRepColumnInfo) colInfo);
        log.debug(format("DataRepColummInfo.name= {0}", ((DataRepColumnInfo) colInfo).getFxFieldName()));
      } else {
        buildDataRepColumnInfoMap(((GroupRepColumnInfo) colInfo).getSubColumns());
      }
    });
  }

  //  protected AbstractRepColumnInfo[] buildBindedFieldCoumnList() {
  protected List<DataRepColumnInfo> buildBindedFieldCoumnList() {
    List<BindControlInfo> inputList = dsfxAdapter.getBindedControls();
    return inputList.stream()
//      .map(RepJInvTableUtil.this::gatherColumnByFieldInfo)
      .filter(col -> isFieldTypeAcceptable(dsRowClass, col))
      .map(RepJInvTableUtil.this::gatherRepColumnInfoByFieldInfo)
      .collect(Collectors.toList());
//    return colList.stream().toArray(size -> new AbstractRepColumnInfo[size]);
  }

  public static Optional<Class> extractFieldClass(Class dsRowClass, JInvTableColumn tColumn) {
//    String fxFieldName = tColumn.getFieldName(); // tableColumn.getDataSetColumn();
//    if (fxFieldName == null || fxFieldName.length() == 0) fxFieldName = tColumn.getId();
    String fxFieldName = getTColumnFiedldName(tColumn);
    Optional<Class> fieldClassOpt = DataRepUtl.getFieldType(dsRowClass, fxFieldName);
    return fieldClassOpt;
  }

  protected AbstractRepColumnInfo gatherRepColumnInfoByTableColumn(TableColumn tColumn) {

//    JInvTableColumn tableColumn = (JInvTableColumn) tColumn;
//    String fxFieldName = Controls.getFieldNameFromTableColumn(tableColumn); // tableColumn.getDataSetColumn();
    AbstractRepColumnInfo reslt = null;

    if (tColumn instanceof JInvTableColumn) {
      JInvTableColumn jinvTableColumn = (JInvTableColumn) tColumn;
//      log.debug( format("{0}", ) );
//      String fxFieldName = jinvTableColumn.getFieldName(); // tableColumn.getDataSetColumn();
//      if (fxFieldName == null || fxFieldName.length() == 0) fxFieldName = tColumn.getId();
      String fxFieldName = getTColumnFiedldName(jinvTableColumn);
      Optional<Class> fieldClassOpt = DataRepUtl.getFieldType(dsRowClass, fxFieldName);
//    BindControlInfoExtracter extracter = new BindControlInfoExtracter(tableColumn);

      IColumnInfoExtractor extractor = tableExtractorFactory.getExtracterInstance(jinvTableColumn, fieldClassOpt);

      // Порядок вызова строго обязателен
      String labelText = extractor.extractColumnLabel();
      Optional<String> formatMask = extractor.extractFormatMask();
      double width = extractor.extractFieldWidth();

      reslt = tColumn.getColumns().size() == 0 && fieldClassOpt.isPresent()
        ? new DataRepColumnInfo(dsRowClass, fxFieldName, fieldClassOpt.get(), labelText, width, formatMask).init()
        : new GroupRepColumnInfo(labelText, width, collectTableColumnTree(jinvTableColumn.getColumns())).init();
    } else {
      reslt = new GroupRepColumnInfo(tColumn.textProperty().get(), tColumn.getWidth(), collectTableColumnTree(tColumn.getColumns())).init();
    }
    return reslt;
  }

  boolean isColumnClassAcceptable(Class fxDataClass) {
    return fxDataClass.isInstance(stringExample)
      || (fxDataClass.isInstance(localDateExample) || fxDataClass.isInstance(sqlDateExample))
      || (fxDataClass.isInstance(localDateTimeExample) || fxDataClass.isInstance(timeStampExample) || fxDataClass.isInstance(dateExample))
      || fxDataClass.isInstance(longExample)
      || fxDataClass.isInstance(integerExample)
      || fxDataClass.isInstance(bigDecimalExample)
      || fxDataClass.isInstance(booleanExample);

  }

  boolean isColumnTypeAcceptable(Class dsRowClass, TableColumn tColumn) {
    boolean res_lt = true;
    log.debug(format("isColumnTypeAcceptable.tColumn= {0}", tColumn));
    if (tColumn instanceof JInvTableColumn) {
      Optional<Class> fxDataClassOpt = RepJInvTableUtil.extractFieldClass(dsRowClass, (JInvTableColumn) tColumn);
      log.debug(format("isColumnTypeAcceptable.fxDataClassOpt= {0}", fxDataClassOpt));
      if (fxDataClassOpt.isPresent()) {
        Class fxDataClass = fxDataClassOpt.get();
        res_lt = isColumnClassAcceptable(fxDataClass);
      }
    }
    return res_lt;
  }

  boolean isFieldTypeAcceptable(Class dsRowClass, BindControlInfo fieldInfo) {
    boolean res_lt = true;

    String fxFieldName = fieldInfo.getDataSetColumn();
    Class fieldClass = DataRepUtl.getFieldType(dsRowClass, fxFieldName).get();

    log.debug(format("isFieldTypeAcceptable.ColumnName= {0}; fxDataClassOpt= {1}", fieldInfo.getDataSetColumn(), fieldClass));

    return isColumnClassAcceptable(fieldClass);

  }

  protected String extractColumnLabel(BindControlInfo columnInfo) {

    String labelText = columnInfo.getDataSetColumn();
    Object component = columnInfo.getComponent();

    if (component instanceof IJInvControl) {
      IJInvControl control = (IJInvControl) component;
      Label labelComponent = control.getLabel();
      labelText = (labelComponent != null ? (labelComponent.getText() != null ? labelComponent.getText() : labelText) : labelText);
    }
    return labelText;
  }

//  protected int extractFieldWidth(BindControlInfo)

  protected DataRepColumnInfo gatherRepColumnInfoByFieldInfo(BindControlInfo fieldInfo) {

    log.debug(format("BindedControlInfo.proc()= {0}", fieldInfo));

    String fxFieldName = fieldInfo.getDataSetColumn();
    Class fieldClass = DataRepUtl.getFieldType(dsRowClass, fxFieldName).get();

    DataTypeControlColInfoExtractor extracter = new DataTypeControlColInfoExtractor(fieldInfo, fieldClass);
    String labelText = extracter.extractColumnLabel();
    Optional<String> formatMask = extracter.extractFormatMask();
    double width = extracter.extractFieldWidth();
//    int prefCol = extracter.extractPrefFieldWidth();
    log.debug(format("Field.Width.Info[]; fxFieldName= {0}; width= {1}", fxFieldName, width));

    return new DataRepColumnInfo(dsRowClass, fxFieldName, fieldClass, labelText, width, formatMask).init();
  }


  public DSFXAdapter<T> getDataSetAdapter() {
//    jinvTable.getDataSetAdapter().getBindedControls()
    return jinvTable.getDataSetAdapter();
  }

  private JRDataSource createDataSource() throws DataSetException {
//    final DRCompactDataSource dataSource = new DRCompactDataSource(getRowColumnNameAsArr());
    final DRIxColumnsDataSource dataSource = new DRIxColumnsDataSource(getRowColumnNameAsArr());
    if (recordSource == RecordSourceEnum.LOADED) {
      dataSet.forEach(r -> dataSource.add(getRowDataAsArr(r)), null);
    } else if (recordSource == RecordSourceEnum.MARKED) {
      StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
//          ((IXXIDataSet<T>) dataSet).createMarkedRSIterator(false),
          ((IXXIDataSet<T>) dataSet).createMarkedRSIterator(false),
          Spliterator.ORDERED)
        , false).forEach(r -> dataSource.add(getRowDataAsArr(r)));
    } else if (recordSource == RecordSourceEnum.ALL) {
      // Etaton
//      StreamSupport.stream(
//        Spliterators.spliteratorUnknownSize(
//          ((IXXIDataSet<T>) dataSet).createRSIterator(false),
//          Spliterator.ORDERED)
//        , false).forEach(r -> dataSource.add(getRowDataAsArr(r)));
      // SQLDataSet
      StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
          ((SQLDataSet<T>) dataSet).createRSIterator(false),
          Spliterator.ORDERED)
        , false).forEach(r -> dataSource.add(getRowDataAsArr(r)));

//        Test
//        , false).forEach( r -> {;} );
//      Iterator<T> iterator = ((IXXIDataSet<T>) dataSet).createRSIterator(false);
//      while (iterator.hasNext()){
//        dataSource.add( getRowDataAsArr(iterator.next()) );
//      }
    }
    return dataSource;
  }

  private JRDataSource createDataSource_new() throws DataSetException {
//    final DRCompactDataSource dataSource = new DRCompactDataSource(getRowColumnNameAsArr());
    final DREntityDataSource<T> dataSource = new DREntityDataSource<>(getColInfoAsArr());
    if (recordSource == RecordSourceEnum.LOADED) {
      dataSet.forEach(r -> dataSource.add(r), null);
    } else if (recordSource == RecordSourceEnum.MARKED) {
      StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
          ((IXXIDataSet<T>) dataSet).createMarkedRSIterator(false),
          Spliterator.ORDERED)
        , false).forEach(r -> dataSource.add(r));
    } else if (recordSource == RecordSourceEnum.ALL) {
      StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(
          ((IXXIDataSet<T>) dataSet).createRSIterator(false),
          Spliterator.ORDERED)
        , false).forEach(r -> dataSource.add(r));
//        Test
//        , false).forEach( r -> {;} );
//      Iterator<T> iterator = ((IXXIDataSet<T>) dataSet).createRSIterator(false);
//      while (iterator.hasNext()){
//        dataSource.add( getRowDataAsArr(iterator.next()) );
//      }
    }
    return dataSource;
  }

  // Корректируем ширину колонок если необходимо
  private void correctDataRepColInfo() {
    dataColInfoMap.values().stream()
      .forEach(drci -> {
        int prefColCount = 0;
        log.debug(format("Check Column for Correction *{0}*; Column.width= {1}", drci.getFxFieldName(), drci.getFxColumnWidth()));
        if (drci.getFxColumnWidth() == 0) {
//          if (drci.getMaxDataLength() > c_longStringPrefLengh) prefColCount = c_longStringPrefLengh;
//          else prefColCount = ( drci.getMaxDataLength() > c_minStringPrefLength ? drci.getMaxDataLength() : c_minStringPrefLength);
          prefColCount = drci.getCorrectedPrefColumnLength();
          if (prefColCount < 0 && false) {
            // !Test 2 recs
            drci.setProperty("net.sf.jasperreports.export.xls.auto.fit.row", "true"); // На некторорых машинах приводит
            drci.setProperty("net.sf.jasperreports.export.xls.auto.fit.column", "true"); // На некторорых машинах приводит
//            drci.setProperty( "net.sf.jasperreports.export.xls.cell.shrinktofit.enabled", "false" );
          }
          log.debug(format("Corrected Column {0}; Detected PrefColumnCount by MaxLength= {1}", drci.getFxFieldName(), prefColCount));
          drci.setFxColumnWidth(FXInvUtil.getFontCharWidth(Math.abs(prefColCount), drJasperDefFont));
        } else if (drci.getMaxDataLength() == -1) {
          drci.setMaxDataLength(1);
          prefColCount = drci.getCorrectedPrefColumnLength();
          if (prefColCount < 0 && false) {
            // !Test 2 recs
            drci.setProperty("net.sf.jasperreports.export.xls.auto.fit.row", "true"); // На некторорых машинах приводит
            drci.setProperty("net.sf.jasperreports.export.xls.auto.fit.column", "true"); // На некторорых машинах приводит
//            drci.setProperty( "net.sf.jasperreports.export.xls.cell.shrinktofit.enabled", "false" );
          }
          log.debug(format("Corrected Null Value Column {0}; Detected PrefColumnCount by MaxLength= {1}", drci.getFxFieldName(), prefColCount));
          drci.setFxColumnWidth(FXInvUtil.getFontCharWidth(Math.abs(prefColCount), drJasperDefFont));
        }
      });
  }

  private void checkExpFile() throws IOException {
    File parentDir = outFile.getParentFile();
    if (!parentDir.exists())
      FileUtils.forceMkdir(parentDir);
  }

  /**
   * Устанавливает размер и ориентацию для Билдера Отчета
   *
   * @param jrb
   */
  private void procPageSize(JasperReportBuilder jrb) {
    if (pageSize.isPresent()) {
      // Устанавливем размер и ориентацию
      jrb.setPageFormat(pageSize.get(), pageOrientation.get());
    } else {
      jrb.ignorePageWidth();
    }
  }

//  В "Аудите действий в формах" реализовать возможность ведения аудита по экспорту данных из формы.
// В поле «Название кнопки/Закладки» пишем «Экспорт»
// В самом аудите выводим следующую информацию:
// 1.Формат выгрузки из настроек экспорта(excel,rtf,html,pdf,docx,txt,jpeg);
// 2.Директория выгрузки из настроек экспорта.
// 3.Запрос из блока формы, откуда производится экспорт.

//  Form_Audit.Audit_Action(this,
//                        <название контролера>,
//                        <тип объекта(кнопка, закладка и т.п.)>, *pdf*
//                        <название объекта>, *Экспорт*  + *Директория Выгрузки*
//                        <ext info>);

  private void registerAudit() {
    try {
      String completedSqlPath = format("Директория выгрузки: {0};", outFile.toPath().normalize().toString() );
      if (dataSet instanceof SQLDataSet) {
        completedSqlPath = format("Директория выгрузки: {0};\nSQL= {1}", outFile.toPath().normalize().toString(), ((SQLDataSet) dataSet).getCompletedSQL() );
      }
      Object[] params = new Object[]{fxFormCtrl.getClass().getSimpleName()
        , fileExt
//        , format("Экспорт; директория выгрузки {0}" , outFile.toPath().toString())
        , "ЭКСПОРТ"
        , completedSqlPath};
      log.debug(format("registerAudit().Контроллер= {0}; Расширение= {1}; Путь {2}", params));
      SQLExpressionFactory.INSTANCE().execute(this.getClass(), "Form_Audit.audit_action", dsfxAdapter.getTaskContext().getConnection(), params);
    } catch (SQLExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  private void buildReport() throws DataSetException, DRException, IOException {
//    try {
    checkExpFile();
    JasperReportBuilder jrb = report();
    AbstractJasperExporterBuilder exporterBuilder = ReportExporterFactory.getBuilderInstance(
      fileExt, outFile, pageSize, jrb);
//    					, new File("reports")
//    					, "DR_" + RandomGUID.getBase64guid() );
    JRDataSource jrds = createDataSource();
    // Test
//    if (true) return;
    correctDataRepColInfo();
    jrb.setDefaultFont(defFontBuilder)
      .setTemplate(Templates.reportTemplate.setHighlightDetailEvenRows(false))
      .columnGrid(colGridComponentBuilderArr)
      .columns(colBuilderArr)
      .setDataSource(jrds);

    procPageSize(jrb);
    String tempDirectory = FileUtils.getTempDirectoryPath();

    // $ Добавляем фиксацию выгрузки в Аудит

//    Form_Audit.Audit_Action(AbstractBaseController fxCtrl,
//            String formName,
//            String itemType,
//            String itemName,
//            String extInfo) и на этом все ?
    registerAudit();
    // Swap - файл
    JRSwapFile swapFile = new JRSwapFile(
      tempDirectory
      , 8192 // размер блока в байтах
      , 1000 // Минимальное количество блоков, прирастаемое за раз при увеличении файла Свопа
    );
    log.debug(format("\nJR.RepJInvTableUtil.Temp.Directory= {0}\nJR.SwapFile= {1}", tempDirectory, swapFile));
    JRSwapFileVirtualizer jrSwapVirt = new JRSwapFileVirtualizer(
      20 // Количество страниц (возможно, вирутальных страниц), которые могут проходить до Свопа
      , swapFile
      , true);
    jrb.setVirtualizer(jrSwapVirt);

    try {
      if (fileExt.equals(C_EXT_JPEG)) {
        jrb.toImage((JasperImageExporterBuilder) exporterBuilder);
      } else {
        jrb.export(exporterBuilder);
      }
    } finally {
      log.debug(format("Clear JR Virtual Swap File= {0}", jrSwapVirt));
      if (jrSwapVirt != null) jrSwapVirt.cleanup();
    }

    //				.show();
    //			trnNumColumn
    //			log.debug( format("numColGroup.getColumnGridTitleGroup()...= {0}", numColGroup.getColumnGridTitleGroup().getList().getListCells() ) );
    //			numColGroup.getColumnGridTitleGroup().getList().getListCells().forEach(cell -> cell.getComponent().);
//    } catch (DRException | DataSetException e) {
//      e.printStackTrace();
//    }
  }

  /* ********************* #Properties Getter/Setter ************************/
  /* **************************** #End Properties **********************************/

  private AbstractBaseController fxFormCtrl;
  private JInvTable<T> jinvTable;
  private File outFile;
  private String fileExt;
  private Optional<PageType> pageSize = Optional.empty();
  private Optional<PageOrientation> pageOrientation = Optional.empty();
  private SizeExportedDataEnum sizeExportedData;
  private RecordSourceEnum recordSource;

  private DSFXAdapter dsfxAdapter;
  private Class dsRowClass;
  private IDataSet<T> dataSet;

  private TableColInfoExtracterFactory tableExtractorFactory = new TableColInfoExtracterFactory();


  // Список ColumnGroup, добавляемых (после трансформации) в Report
  private List<AbstractRepColumnInfo> repColumnInfoGroupList;
  private List<ColumnGridComponentBuilder> cgcbList = new ArrayList<ColumnGridComponentBuilder>();

  private LinkedHashMap<String, DataRepColumnInfo> dataColInfoMap = new LinkedHashMap<>();
  // Аттрибуты отчета
  private ColumnGridComponentBuilder[] colGridComponentBuilderArr;
  private ColumnBuilder[] colBuilderArr;

  private class TableColInfoExtracterFactory {
    IColumnInfoExtractor getExtracterInstance(JInvTableColumn tableColumn, Optional<Class> dataClassOpt) {
      IColumnInfoExtractor reslt = new JInvTableColumnInfoExtractor(tableColumn);
      if (U.in(sizeExportedData, FULL_DATA, FULL_TABLE) && dataClassOpt.isPresent()) {
        reslt = new DataTypeTableColInfoExtractor(tableColumn, dataClassOpt.get());
      }
      return reslt;
    }
  }
}
