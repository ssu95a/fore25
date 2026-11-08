# D4-001: listener lifecycle

Рабочая ветка: `migration/java25-correctness`; исходный HEAD: `f08248e`.
Ожидаемый production commit `475b24f` является предком HEAD; следующий commit менял только отчёт D3.

CONFIRMED: `bindTable` добавляет Row, Navigation и DataSet listeners. Унаследованный
`DSControlAdapter.close()` удалял только первые два и обнулял dataSet. DataSet listener
оставался в Dataset и получал callbacks после close.

CONFIRMED: `initMarkColumn` при поддержке mark добавляет mark listener. Через javap
проверены публичный `XXIDataSet.removeMarkDataSetListener(IDataSetMarkListener<T>)`
и его вызов listener manager removal. Этот listener также оставался после close.
REJECTED: предположение об отсутствии публичного mark removal API.

Исправление: DSFXAdapter.close удаляет DataSet listener и для XXIDataSet mark listener
до super.close, сохраняющего Row/Navigation cleanup и обнуление Dataset. Removal
без предварительной mark-подписки безопасен по реализации listener manager;
проверка текущего enableMark не нужна: он мог измениться после подписки.
setDataSet сохраняет public void signature и generic parameter IDataSet<T>, получает
Override/Deprecated и безусловно бросает UnsupportedOperationException. Superclass
и JInvCommon не изменены. Override close сохраняет прежний public void close() API.

## Проверки

`powershell -NoProfile -ExecutionPolicy Bypass -File src/test/probes/Test-D4Lifecycle.ps1 -ExpectLeak`
воспроизводит прежнее поведение: извлечённые из production bind listener registrations
и superclass close исполняются с настоящими ArrayDataSet и XXIDataSet из локального
JInvCommon. CONFIRMED: DataSet и mark callbacks после close; Row/Navigation сняты.

Тот же runner без ExpectLeak исполняет извлечённые новые close/setDataSet:
все проверки PASS — callbacks приходят до close, после close отсутствуют все четыре,
dataSet обнулён, повторный close безопасен; setter отклоняет DS1/DS2/null при binding,
вызовы до binding и после close, не меняя dataSet; точный exception message проверен.
Сигнатура и annotations проверяются в source. MarkData переопределяет только
isSupportMark для разрешения настоящего fireMarkDataSetEvent(REFRESH), без SQL/БД.

Ограничение probe: это extraction test lifecycle-блоков, а не compilation/runtime
полного DSFXAdapter или настоящий вызов bindTable с TableView. Callback bodies
заменены счётчиками. Classpath берётся из доступного локального Maven cache;
это не корпоративный Maven classpath. Параметр MavenRepository позволяет задать cache.

Локально выполнены на JDK 25.0.1:

- D4 reproduction и regression: PASS.
- Test-EmptyProperty: 90 checks, 0 failures (JavaFX base 11, JInvCommon 1.0.0,
  slf4j-api 1.7.25). Удалены прежние проверки синхронизации через setter:
  теперь этот runtime use намеренно запрещён; rejection покрыт D4.
- Test-ColumnFieldsSet: 31 PASS (JavaFX base/graphics/controls 11).
- Test-D3SelectionSync -ValidateOnly: Java syntax PASS; runtime не выполнялся.
- git diff --check: PASS.

UNKNOWN: полный corporate JavaFX 25 build и integration runtime. JavaFX 25 отсутствует
в локальном cache, mvn отсутствует в PATH, mvnw.cmd не стартует с ошибкой
`Cannot index into a null array / Cannot start maven from wrapper` до Maven build.
Корпоративные dependencies не заменялись. Полный bindTable/close сценарий следует
проверить на corporate runtime; результат extraction probe не выдаётся за него.

D3 selection, TableView event handlers, initial binding, clearSelection, D5-D7,
logback, JInvXMLViewer и ru.inversion.diff не менялись. Расширения scope нет.
