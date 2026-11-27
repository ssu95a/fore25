# Codex mailbox outbox

## Protocol

Codex writes replies here on branch `agent/codex-mailbox`.

Each reply should be appended or replace the current pending reply using this structure:

```text
MESSAGE-ID: <id>
STATUS: DONE | BLOCKED
WORKING-BRANCH: <branch>
WORKING-HEAD: <sha>
PRODUCTION-COMMIT: <sha or NONE>
PUSH: YES | NO

SUMMARY:
...

CONFIRMED:
...

UNKNOWN:
...

FILES:
...
```

Do not place production source changes on the mailbox branch.

---

No replies yet.

## Reply D4-001

MESSAGE-ID: D4-001
STATUS: DONE
WORKING-BRANCH: migration/java25-correctness
WORKING-HEAD: 0dd94764ac4d75eccc75a57c85ad005676875d19
PRODUCTION-COMMIT: 0dd94764ac4d75eccc75a57c85ad005676875d19
PUSH: NO (production); YES (mailbox reply)

SUMMARY:
D4.1: setDataSet(IDataSet<T>) сохранён, добавлены @Override/@Deprecated;
каждый вызов немедленно бросает UnsupportedOperationException без изменения состояния.
Message: Changing Dataset for a bound TableView adapter is unsupported
D4.2: новый close() снимает DataSet и XXIDataSet mark listeners перед super.close().
Superclass продолжает снимать Row/Navigation listeners и обнулять dataSet.

CONFIRMED:
- bindTable добавляет Row/Navigation/DataSet listeners; прежний inherited close
  оставлял DataSet listener. Targeted extraction probe с настоящим ArrayDataSet
  воспроизвёл callback после close до исправления и его отсутствие после.
- initMarkColumn добавляет mark listener. javap подтвердил public
  XXIDataSet.removeMarkDataSetListener(IDataSetMarkListener<T>) и removal implementation.
  Реальный XXIDataSet listener manager: mark callback доставляется до close,
  остаётся при прежнем close, отсутствует при новом close.
- REJECTED: отсутствие публичного mark removal API.
- Row/Navigation cleanup сохранён; повторный close безопасен.
- Setter отклоняет DS1/DS2/null при binding, вызовы до binding и после close;
  exception type/message и отсутствие изменения dataSet проверены.
- D4 reproduction/regression PASS. D1: 31 PASS; D2: 90 checks, 0 failures;
  D3 -ValidateOnly: Java syntax PASS. git diff --check PASS.
- Public void setDataSet(IDataSet<T>) signature сохранена; поведение намеренно
  несовместимо с runtime rebinding. Нового replacement API нет; close override
  сохраняет прежнюю доступную public void close() signature. JInvCommon не изменён.
- Expected production commit 475b24f присутствовал; исходный HEAD f08248e
  добавлял поверх него только D3 verification report.

UNKNOWN:
- Corporate JavaFX 25 build/runtime и полный bindTable(TableView)/close integration.
  Локально JDK 25.0.1; JavaFX 25 нет в cache, mvn нет в PATH, wrapper не стартует:
  Cannot index into a null array / Cannot start maven from wrapper.
- D4 probe исполняет извлечённые production lifecycle-блоки с настоящим JInvCommon,
  заменяя callback bodies счётчиками; это НЕ запуск полного DSFXAdapter/TableView.
  MarkData override isSupportMark разрешает настоящий mark event без БД.
  D1/D2 использовали локальный JavaFX 11, не corporate JavaFX 25.
- Follow-up: проверить полный binding/close на corporate runtime. D3 selection,
  TableView handlers, initial binding, clearSelection, D5-D7 и deferred packages
  не менялись; расширения scope нет.

FILES:
- src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
- src/test/probes/Test-D4Lifecycle.ps1
- src/test/probes/Test-EmptyProperty.ps1 (убраны прежние setter-sync проверки;
  намеренный rejection теперь покрыт D4)
- docs/codex/d4-listener-lifecycle.md (подробности проверок и ограничений)

LOCAL-COMMIT: 0dd94764ac4d75eccc75a57c85ad005676875d19
PRODUCTION-PUSH: NO

## Reply D4-002

MESSAGE-ID: D4-002
STATUS: DONE
WORKING-BRANCH: migration/java25-correctness
WORKING-HEAD: 0dd94764ac4d75eccc75a57c85ad005676875d19
PRODUCTION-COMMIT: 0dd94764ac4d75eccc75a57c85ad005676875d19 (existing, unchanged)
PARENT: f08248e4a4d90ae9423316e29f2197fee83fa9ba
PUSH: NO (production); YES (mailbox reply only)

SUMMARY:
Review/report only. Код не менялся, amend не выполнялся, новых production commits нет.
Ниже полные git diffs parent..commit без сокращений; методы взяты из того же commit.

CONFIRMED:
- D2 probe ранее извлекал super.setDataSet(ds)/updateEmptyProperty и проверял
  успешную синхронизацию через setter, включая null. После решения запретить
  каждый вызов setter этот source marker исчез, а прежние ожидания стали неверны.
  Поэтому удалены extraction/template/assertions именно setter-сценария.
  Initial bind и Row/DataSet event emptyProperty проверки сохранены (90 checks).
  Новый контракт setter проверяется D4 probe, включая отказ без изменения dataSet.
- close снимает DataSet listener при dataSet != null.
- mark removal вызывается только для dataSet instanceof XXIDataSet. Наличие
  фактической mark-подписки не отслеживается: removal вызывается и когда она не
  устанавливалась. В доступном JInvCommon removal отсутствующего listener — no-op.
  Поэтому cleanup работает и если enableMark/supportMark изменились после подписки.
  Это условие по типу Dataset, а не условие по текущему enableMark.
- super.close() снимает Row/Navigation listeners и затем обнуляет dataSet.
- При штатном завершении первого close повторный close безопасен: dataSet уже null,
  и оба null guards пропускают removal. close до binding также безопасен по коду.
  Повторный close после обычного и mark binding покрыт D4 extraction probe.
- Null safety относится к dataSet, а не к исключениям из сторонних remove методов:
  try/finally здесь нет; если removal бросит исключение, дальнейший cleanup не
  гарантирован. Потокобезопасность/concurrent close этим commit не добавлены.
- Helpers/state специально для D4 не добавлялись. Только setter replacement и
  close override; superclass и JInvCommon не менялись.

UNKNOWN:
Полный corporate JavaFX 25 build/runtime остаётся UNKNOWN из D4-001.
Новых runtime-проверок в этом report-only follow-up не запускалось.
Extraction probe не является запуском полного DSFXAdapter.bindTable/TableView;
classpath локального Maven cache не подтверждает corporate build compatibility.

FILES:
Review material below: DSFXAdapter.java production diff and complete D4 methods;
Test-D4Lifecycle.ps1 and Test-EmptyProperty.ps1 exact probe diffs.

### Full production diff

```diff
diff --git a/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java b/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
index 5c4d7b3..ce8c42c 100644
--- a/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
+++ b/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
@@ -465,26 +465,21 @@ public class DSFXAdapter<T> extends DSControlAdapter<T> implements IDataSetListe
     }
 
     /** */
+    @Override
+    @Deprecated
     public void setDataSet( IDataSet<T> ds ) {
+        throw new UnsupportedOperationException("Changing Dataset for a bound TableView adapter is unsupported");
+    }
 
-        super.setDataSet(ds);
-        updateEmptyProperty();
-
-        if( dataSet != null )
-        {
-            // Связывание данных
-            final List<T> rows = dataSet.getRows();
-
-            if( !(rows instanceof ObservableList) )
-                throw new IllegalStateException( Tags.PRODUCT_LABEL + "Internal collection in DataSet is not ObservableList!" );
-
-            final ObservableList<T> ol = (ObservableList<T>)rows;
-            this.tableView.setItems( ol );
-        }
-        else
-        {
-            this.tableView.setItems( FXCollections.emptyObservableList() );
+    /** */
+    @Override
+    public void close() {
+        if( dataSet != null ) {
+            dataSet.removeDataSetListener(this);
+            if( dataSet instanceof XXIDataSet )
+                ((XXIDataSet<T>) dataSet).removeMarkDataSetListener(this);
         }
+        super.close();
     }
 
     /** */
```

### Full current methods (no D4 helper/state added)

```java
    @Override
    @Deprecated
    public void setDataSet( IDataSet<T> ds ) {
        throw new UnsupportedOperationException("Changing Dataset for a bound TableView adapter is unsupported");
    }

    /** */
    @Override
    public void close() {
        if( dataSet != null ) {
            dataSet.removeDataSetListener(this);
            if( dataSet instanceof XXIDataSet )
                ((XXIDataSet<T>) dataSet).removeMarkDataSetListener(this);
        }
        super.close();
    }
```

### Exact probe diff

```diff
diff --git a/src/test/probes/Test-D4Lifecycle.ps1 b/src/test/probes/Test-D4Lifecycle.ps1
new file mode 100644
index 0000000..c7c9086
--- /dev/null
+++ b/src/test/probes/Test-D4Lifecycle.ps1
@@ -0,0 +1,112 @@
+param([switch]$ExpectLeak, [string]$MavenRepository = 'D:/Java/.m2/repository')
+$ErrorActionPreference = 'Stop'
+$root = [IO.Path]::GetFullPath("$PSScriptRoot/../../..")
+$fx = Get-Content "$root/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java" -Raw -Encoding UTF8
+$base = Get-Content "$root/src/main/java/ru/inversion/dataset/fx/DSControlAdapter.java" -Raw -Encoding UTF8
+if ($fx -notmatch '@Override\s+@Deprecated\s+public void setDataSet\( IDataSet<T> ds \)') {
+    throw 'Public setter signature/annotations changed'
+}
+Write-Output 'PASS public void setDataSet(IDataSet<T>) with Override/Deprecated'
+function Method([string]$source, [string]$signature) {
+    $start = $source.IndexOf($signature)
+    if ($start -lt 0) { throw "Missing method $signature" }
+    $brace = $source.IndexOf('{', $start)
+    $depth = 1; $end = $brace + 1
+    while ($depth -gt 0) {
+        if ($source[$end] -eq '{') { $depth++ }
+        if ($source[$end] -eq '}') { $depth-- }
+        $end++
+    }
+    return $source.Substring($start, $end - $start)
+}
+$close = if ($ExpectLeak) { '' } else { Method $fx 'public void close(' }
+$setter = if ($ExpectLeak) { '' } else { Method $fx 'public void setDataSet(' }
+$start = $fx.IndexOf('        this.dataSet = dataSet;')
+$end = $fx.IndexOf('        this.tableView = tableView;', $start)
+$bind = $fx.Substring($start, $end - $start).Replace('        updateEmptyProperty();', '')
+$mark = [regex]::Match($fx, 'xxiDs\.addMarkDataSetListener\(this\);').Value
+if (!$mark) { throw 'Missing mark registration' }
+# Исполняются извлечённые lifecycle-блоки без UI; события доставляет настоящий JInvCommon.
+$template = @'
+import java.util.*;
+import ru.inversion.dataset.*;
+class Parent<T> implements IDataSetRowListener<T>, IDataSetNavigationListener<T> {
+    protected IDataSet<T> dataSet;
+    public void rowOperation(DataSetRowEvent<T> e) {}
+    public void navigated(DataSetNavigationEvent<T> e) {}
+    public void setDataSet(IDataSet<T> ds) {}
+__BASE_CLOSE__
+}
+public class D4LifecycleProbe<T> extends Parent<T> implements IDataSetListener, IDataSetMarkListener<T> {
+    int data, row, nav, mark;
+    public void dataSetChanged(DataSetEvent e) { data++; }
+    public void rowOperation(DataSetRowEvent<T> e) { row++; }
+    public void navigated(DataSetNavigationEvent<T> e) { nav++; }
+    public void markAction(DataSetMarkEvent<T> e) { mark++; }
+    void bind(IDataSet<T> dataSet) {
+__BIND__
+    }
+    void bindMark(XXIDataSet<T> xxiDs) { __MARK__ }
+__CLOSE__
+__SETTER__
+    static class Data extends ArrayDataSet<String> {
+        Data() { super(String.class, new ArrayList<>(), false); }
+        void fire() {
+            fireDataSetEvent(DataSetEvent.DataSetEventType.EXECUTE, false);
+            fireNavigationEvent(null, null, 0, 1);
+            fireRowEvent(DataSetRowEvent.RowOperationEnum.UPDATE, null, null, 0, 0);
+        }
+    }
+    static class MarkData extends XXIDataSet<String> {
+        @Override public boolean isSupportMark() { return true; }
+        void fire() { fireMarkDataSetEvent(DataSetMarkEvent.MarkActionEnum.REFRESH); }
+    }
+    static void check(String name, boolean ok) {
+        if (!ok) throw new AssertionError(name);
+        System.out.println("PASS " + name);
+    }
+    public static void main(String[] args) throws Exception {
+        boolean leak = Boolean.parseBoolean(args[0]);
+        var a = new D4LifecycleProbe<String>();
+        var ds = new Data();
+        if (!leak) reject(a, ds);
+        a.bind(ds); ds.fire();
+        check("all three callbacks before close", a.data == 1 && a.row == 1 && a.nav == 1);
+        if (!leak) { reject(a, ds); reject(a, new Data()); reject(a, null); }
+        a.close(); ds.fire();
+        check("Row/Navigation detached", a.row == 1 && a.nav == 1);
+        check(leak ? "REPRODUCED DataSet callback after close" : "DataSet detached", a.data == (leak ? 2 : 1));
+        check("dataSet cleared", a.dataSet == null);
+        a.close();
+        if (!leak) reject(a, ds);
+        var m = new D4LifecycleProbe<String>();
+        var md = new MarkData();
+        m.bind(md); m.bindMark(md); md.fire();
+        check("mark delivered before close", m.mark == 1);
+        m.close(); md.fire();
+        check(leak ? "REPRODUCED mark callback after close" : "mark detached", m.mark == (leak ? 2 : 1));
+        m.close();
+    }
+    static void reject(D4LifecycleProbe<String> a, IDataSet<String> ds) {
+        var before = a.dataSet;
+        try { a.setDataSet(ds); throw new AssertionError("setter returned"); }
+        catch (UnsupportedOperationException e) {
+            check("setter rejects without rebinding", a.dataSet == before && e.getMessage().equals("Changing Dataset for a bound TableView adapter is unsupported"));
+        }
+    }
+}
+'@
+$out = "$root/target/d4-lifecycle-probe".Replace('\','/')
+New-Item -ItemType Directory -Force $out | Out-Null
+$generated = $template.Replace('__BASE_CLOSE__', (Method $base 'public void close(')).Replace('__CLOSE__', $close).Replace('__SETTER__', $setter).Replace('__BIND__', $bind).Replace('__MARK__', $mark)
+[IO.File]::WriteAllText("$out/D4LifecycleProbe.java", $generated, [Text.UTF8Encoding]::new($false))
+$jars = Get-ChildItem $MavenRepository -Recurse -Filter *.jar | Select-Object -ExpandProperty FullName
+$cp = $jars -join ';'
+$compile = '-encoding UTF-8 -proc:none -cp "' + $cp.Replace('\','/') + '" -d "' + $out + '" "' + $out + '/D4LifecycleProbe.java"'
+[IO.File]::WriteAllText("$out/compile.args", $compile)
+& javac "@$out/compile.args"
+if ($LASTEXITCODE) { throw 'Compile failed' }
+$run = '-cp "' + $out + ';' + $cp.Replace('\','/') + '" D4LifecycleProbe ' + $ExpectLeak.IsPresent.ToString().ToLowerInvariant()
+[IO.File]::WriteAllText("$out/run.args", $run)
+& java "@$out/run.args"
+if ($LASTEXITCODE) { throw 'Probe failed' }
diff --git a/src/test/probes/Test-EmptyProperty.ps1 b/src/test/probes/Test-EmptyProperty.ps1
index ec1566f..0d00de8 100644
--- a/src/test/probes/Test-EmptyProperty.ps1
+++ b/src/test/probes/Test-EmptyProperty.ps1
@@ -21,9 +21,6 @@ $property = Get-SourceBetween '    private ReadOnlyBooleanWrapper emptyProperty;
 $binding = Get-SourceBetween '        this.dataSet = dataSet;' '        this.tableView = tableView;'
 $row = Get-SourceBetween '    public void rowOperation( DataSetRowEvent<T> event ) {' '        if( event.getRowOperation() == UPDATE )'
 $data = Get-SourceBetween '    public void dataSetChanged( DataSetEvent e ) {' '        if( e.isAfter() && e.getEventType() == DataSetEvent.DataSetEventType.EXECUTE )'
-$setter = Get-SourceBetween '        super.setDataSet(ds);' '        if( dataSet != null )'
-# Здесь проверяется только начальная синхронизация setter, без переподключения listeners из D4.
-$setter = $setter.Replace('super.setDataSet(ds);', 'this.dataSet = ds;')
 $template = @'
 import java.util.*;
 import javafx.beans.property.*;
@@ -35,9 +32,6 @@ public class DSFXAdapter<T> implements IDataSetRowListener<T>, IDataSetListener,
     void bind(IDataSet<T> dataSet) {
 __BINDING__
     }
-    void synchronizeSetter(IDataSet<T> ds) {
-__SETTER__
-    }
 __PROPERTY__
 __ROW__
     }
@@ -120,10 +114,6 @@ __DATA__
             ds.event(type, false);
             check(label + type + " AFTER empty", property.get());
         }
-        adapter.synchronizeSetter(new Data(1));
-        check(label + "setDataSet initial synchronization", !property.get());
-        adapter.synchronizeSetter(null);
-        check(label + "setDataSet null synchronization", property.get());
     }
     public static void main(String[] args) {
         scenario(false, 0);
@@ -135,7 +125,7 @@ __DATA__
     }
 }
 '@
-$generated = $template.Replace('__BINDING__', $binding).Replace('__SETTER__', $setter).Replace('__PROPERTY__', $property).Replace('__ROW__', $row).Replace('__DATA__', $data)
+$generated = $template.Replace('__BINDING__', $binding).Replace('__PROPERTY__', $property).Replace('__ROW__', $row).Replace('__DATA__', $data)
 $probeDir = Join-Path ([IO.Path]::GetTempPath()) ('fore25-empty-property-' + [Guid]::NewGuid().ToString('N'))
 New-Item -ItemType Directory -Path $probeDir | Out-Null
 $probeFile = Join-Path $probeDir 'DSFXAdapter.java'
```

## Reply D4-003

MESSAGE-ID: D4-003
STATUS: DONE
COMMIT: f08248e4a4d90ae9423316e29f2197fee83fa9ba
PARENT: 475b24f935aa0f38e1ed6993809ec18d71f4ac5f
WORKING-BRANCH: migration/java25-correctness
WORKING-HEAD: 0dd94764ac4d75eccc75a57c85ad005676875d19
PUSH: NO (production); YES (mailbox reply only)

DIFF SUMMARY:
Commit message (complete): docs: finalize D3 selection sync verification
Changed files (complete):
- M docs/codex/stage2-d3-selection-sync-investigation.md: +69 / -0.
Total: 1 file changed, 69 insertions, 0 deletions.
Parent relationship confirmed: this documentation commit is the direct parent
of 0dd94764ac4d75eccc75a57c85ad005676875d19.

PRODUCTION CODE CHANGES: NO
PROBE CODE CHANGES: NO
PUBLIC API CHANGES: NO
JInvCommon CHANGES: NO
DEFERRED AREAS CHANGED: NO
ONLY DOCUMENTATION: YES

CONFIRMED:
Добавлен только раздел «D3 final corporate verification» в конец D3 report.
Он фиксирует источник evidence (предоставленный пользователем corporate runtime
Java 25.0.1 / JavaFX 25, настоящий DSFXAdapter/TableView/ArrayDataSet), исходный
дефект до fix, root cause, принятый fix 475b24f, compatibility и итог D3 CLOSED.
Описание selectedIndexProperty listener и directional guard относится к ранее
принятому production fix; этот documentation commit Java-код не изменяет.

Матрица и приведённый output согласуются со всеми результатами, указанными
в D4-003 как уже записанные corporate results:
- P2 PASS.
- P1 REJECTED_GAP: Table и Dataset согласованы на row 2.
- P1 additional methods PASS: select(item), selectFirst, clearAndSelect.
- P3 PASS (selectLast).
- P4 PASS: Dataset остаётся на B (index=1), Table index=-1/item=null.
- P5 PASS: точный NAV=[0->1, 1->2].
- SCENARIOS COMPLETED=P1,P2,P3,P4,P5,INITIAL_BINDING; PROBE STATUS=VALID EXIT=0.
- Initial binding сохранён: Table selectedIndex=-1, Dataset current index=2.
- clearSelection semantics сохранены; новая semantics отсутствующей current row
  не вводилась.
- Feedback loop не наблюдался в проверенном P5: дополнительных обратных
  navigation callbacks не было. Это не обобщается на все interleavings/Datasets.

Отчёт явно ограничивает вывод проверенной P1-P5 matrix, не объявляет устранёнными
все stale-queue/interleaving риски и не включает D4-D7 в документационную задачу.
logback, JInvXMLViewer и весь package ru.inversion.diff не изменены: полный diff
затрагивает только один Markdown-файл, без удаления предыдущего содержимого.

UNKNOWN:
Это review сохранённого отчёта и его соответствия recorded corporate results,
а не новый независимый запуск corporate runtime. Исходный внешний runtime log
в этом follow-up не получен и заново не исполнялся.

VERDICT:
CONFIRMED documentation-only commit. Содержание согласовано с corporate results,
перечисленными в D4-003; production/probe/API/JInvCommon/deferred изменений нет.
Код не менялся, existing commits не amend, production branch не push.
Этот ответ не выполняет и не предполагает разрешения на production push.

FULL DIFF:
Exact git diff 475b24f935aa0f38e1ed6993809ec18d71f4ac5f..f08248e4a4d90ae9423316e29f2197fee83fa9ba
(no path filter, no omissions):

```diff
diff --git a/docs/codex/stage2-d3-selection-sync-investigation.md b/docs/codex/stage2-d3-selection-sync-investigation.md
index 539c1ce..db1acaa 100644
--- a/docs/codex/stage2-d3-selection-sync-investigation.md
+++ b/docs/codex/stage2-d3-selection-sync-investigation.md
@@ -378,3 +378,72 @@ Trace содержит BEFORE/IMMEDIATE/AFTER snapshots с table index/item, Dat
 Подтверждено наличие обоих probe files в рабочем дереве. Запуск runner без `-ValidateOnly` действительно выполняет Maven compile, компиляцию отдельного Java probe и его main, запускающий toolkit и P1–P5. В output явно печатаются `MODE=CORPORATE_EXECUTION`, вход/возврат настоящего `bindTable` и `SCENARIOS COMPLETED=P1,P2,P3,P4,P5,INITIAL_BINDING` при технически успешном завершении. Это executable probe, а не только обновление отчёта.
 
 Повторно проверены PowerShell parsing, Java 25 syntax parsing и whitespace новых probe files. Corporate type-check и runtime результаты по-прежнему UNKNOWN до запуска на корпоративной машине. Конкретная невозможность минимального bindTable не установлена: код использует настоящий binding и при его ошибке выводит stack trace, UNKNOWN и non-zero, без fallback на fake/mock. Production code/API и JInvCommon не изменены; commit/push не выполнялись.
+
+## D3 final corporate verification
+
+### Контекст и источник evidence
+
+- Repository: `ssu95a/fore25`.
+- Branch: `migration/java25-correctness`.
+- Accepted production fix commit: `475b24f935aa0f38e1ed6993809ec18d71f4ac5f` — `fix: synchronize table selection with dataset row`.
+- Пользователь предоставил результаты повторного запуска probe на корпоративной машине: Java 25.0.1, JavaFX 25, production DSFXAdapter, настоящий TableView и ArrayDataSet.
+- Результаты ниже являются corporate runtime evidence пользователя, а не локальной симуляцией. Исторические UNKNOWN предыдущих разделов отражают состояние на момент первоначального investigation; для проверенной P1–P5 matrix они разрешены этим запуском.
+
+### CONFIRMED: defect до fix
+
+Контрольный Dataset → Table путь работал (P2 PASS), но programmatic `table.select(2)` оставлял Dataset на row 0 при Table selection 2 (P1 CONFIRMED_GAP). `selectLast()` воспроизводил то же расхождение: table.index=2, dataset.index=0.
+
+Root cause: DSFXAdapter не имел общего listener на изменение selection. Table → Dataset synchronization зависела от mouse/key events, которые programmatic selection не генерирует.
+
+### Accepted fix и compatibility
+
+В `src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java` добавлен listener `selectedIndexProperty`, обрабатывающий только валидный индекс через существующий `refreshCurrentRow()` и прежний index-based Dataset setter. Private directional guard `insideInSetTableSelection` установлен непосредственно вокруг Dataset-driven `select(index)` в queued `positionRow`, чтобы изменение Table selection не инициировало обратный Dataset setter. Контракт существующего `insideInSetCurrentRow` и legacy mouse/key handlers сохранён.
+
+Public/protected API не изменён; изменений прикладного кода не требуется. Cleanup/rebinding listeners не расширялся до D4.
+
+### CONFIRMED: corporate results после fix
+
+| Сценарий | Результат |
+| --- | --- |
+| P2 Dataset → Table | PASS |
+| P1 programmatic select(2) | REJECTED_GAP: Table и Dataset согласованы на row 2 |
+| P1 additional methods | PASS: select(item), selectFirst, clearAndSelect |
+| P3 selectLast | PASS |
+| P4 clearSelection | PASS: Dataset остаётся на B |
+| P5 rapid Dataset navigation | PASS; точный trace NAV=[0->1, 1->2] |
+
+Итоговый output предоставленного запуска:
+
+```text
+P2 RESULT=PASS
+P1 RESULT=REJECTED_GAP
+P1 ADDITIONAL METHODS=PASS
+P3 RESULT=PASS
+P4 RESULT=PASS (Dataset remains on B)
+P5 RESULT=PASS NAV=[0->1, 1->2]
+SCENARIOS COMPLETED=P1,P2,P3,P4,P5,INITIAL_BINDING
+D3 PROGRAMMATIC SELECTION GAP — REJECTED
+PROBE STATUS=VALID EXIT=0
+```
+
+Точный P5 trace содержит только `0->1` и `1->2`. Дополнительных reverse navigation callbacks `2->1` / `1->2` нет: отсутствие feedback loop в этом сценарии подтверждено runtime probe.
+
+### Сохранённые semantics и границы
+
+Initial binding не изменён: после binding Table selectedIndex=-1, Dataset current index=2. Это отдельно подтверждено corporate запуском и остаётся отдельным вопросом, вне D3 fix.
+
+`clearSelection()` сохраняет прежнее observable behavior: Table index=-1 и item=null; Dataset index=1 и row=B. Новая semantics отсутствующей current row не вводилась.
+
+Закрытие относится к проверенному D3 contract валидного selection при прежнем index mapping. Оно не означает устранения всех возможных stale-queue/interleaving рисков или проверки всех Dataset implementations. D4–D7 не исправлялись; D4 не начат этой документационной задачей.
+
+### Итог
+
+**D3 — CLOSED**
+
+```text
+PUBLIC API CHANGES: NONE
+JInvCommon CHANGES: NONE
+D4-D7 CHANGES: NONE
+```
+
+В рамках final verification update изменён только этот Markdown-отчёт. Production и probe code не изменены. Push не выполнялся.
```

---

## Reply D5-001

- MESSAGE-ID: D5-001
- STATUS: DONE
- BRANCH: `migration/java25-correctness`
- HEAD: `0dd94764ac4d75eccc75a57c85ad005676875d19` (local HEAD and GitHub ref verified)
- PRODUCTION CHANGES: NONE
- PRODUCTION COMMIT / PUSH: NONE

### CONFIRMED

**Call sites and consumers.** `DSFXAdapter.java:1049–1060` defaults the no-arg and callback-only entry points to `parallel=true`; the three-argument overload forwards the selected mode with `readAllRecords=false`. `JInvTable.java:844–891` defaults its ordinary overloads to parallel; the one-boolean overload sets `readAllRecords`. Both `executeQueryBlock` overloads explicitly use `parallel=false` with a null callback. The explicit-mode overloads forward caller arguments; the final table wrapper silently skips missing adapter/Dataset. No in-tree invocation of the Block overloads or explicit sync caller with a non-null callback was found. `ViewTab.java:93` and `EditTab.java:439` call the Dataset, and `TSFXAdapter.java:712` belongs to a different adapter. `AbstractBaseController.java:3111`, `JInvKeyboardManager.java:155–158`, `OrderByManager.java:120` and the adapter's own call at line 1418 use the default parallel entry. `JInvLovSearchPane.java:95–126` passes `postExecQuery` through the default parallel table overload. It throws on false, and on true checks page size, selects a row and clears the filter. It can also throw during success handling. The default adapter callback queues error reporting on FX only for `false` with non-null Throwable.

**Synchronous guard.** `boolean checkForFullScan()` returns true when the guard is inapplicable (non-JInvTable/non-XXIDataSet, big-table mode off or absent ViewContext) or allows execution. Its two false returns follow a non-YES answer from `Alerts.yesNo`: the full scan prompt or a prompt after COUNT exceeds `BIG_T_MAX_RECORD`. Dialog dismissal also fails the YES comparison. False skips the main query; guard work such as COUNT may already have run. Accepted full scan clears ordering; accepted large COUNT clears ordering only above `BIG_T_MAX_SORTED`. Defaults are 100 / 1000. Guard exceptions reach the query branch's error callback.

| Synchronous outcome | Main query | Callback attempts before fix | Positioning and propagation |
| --- | --- | --- | --- |
| Query returns normally | 1 | 0 | Queues row 0 in finally |
| Main query throws | 1 | 1 × `(false, original Throwable)` | Queues row 0; original error is handled by callback unless callback throws |
| Guard throws | 0 | 1 × `(false, original Throwable)` | Same finally |
| Guard returns false | 0 | 0 | Still queues row 0 |
| Dataset absent at entry | 0 | 0 | Returns before listener resolution and positioning |
| Listener resolution throws before try | 0 | 0 | Exception escapes; no positioning. Null listener plus a plain TableView casts to JInvTable and throws ClassCastException. A null table would fail at getter dereference if reachable. A null controller return alone is harmless in sync mode. |
| Error callback throws | As above | 1, no retry | Finally still attempts to queue row 0; callback exception escapes if finally completes |
| `positionRow(0)` scheduling throws | As above | Existing count, no extra callback | Finally exception escapes and can mask the callback exception |
| Queued positioning code throws | As above | Existing count, no extra callback | Failure occurs later on the FX thread, outside the query catch |

Sync checks/query/error callback and the enqueue attempt run on the caller thread under this adapter's `DATA_SET_LOCK`. The queued UI work does not acquire that lock in `positionRow`. Callback execution on FX depends on the caller in sync mode; the callback is not an acknowledgement that row selection or UI rendering has completed.

**Parallel comparison.** Selection is cleared before submitting the first action. `int checkForFullScan(Holder<Integer>)` classifies the request on the worker under `DATA_SET_LOCK`: 0 direct, 1 full scan, 2 large COUNT, 3 large COUNT with sort removal on approval. The 0 path invokes query and callback on that worker under the lock. The positive path resolves ViewContext and queues `alertFullScan` on FX. Approval launches a second worker action whose `executeRun` does not explicitly acquire this lock; refusal launches nothing and sends no callback. `JInvAction.handle` submits a Task to the pool on normal enabled/accepted dispatch; errors/aborts outside the worker try and exceptions inside the later FX prompt body have no guaranteed query callback. Worker errors caught by the adapter attempt positioning first, then `(false,th)`; if positioning enqueue throws, error notification can be skipped. Both worker success callbacks are inside their query try. A throwing success consumer can receive a second attempt as `(false, callbackThrowable)`, subject to successful positioning enqueue. Failure callbacks that throw escape the adapter worker lambda. This is comparison evidence only.

### BEFORE FIX evidence

Local JDK 25.0.1 extraction probe PASS (exit 0). It compiles the method's production prelude and sync block verbatim, with an outer sync-only selector and doubles for Dataset, guard, scheduling and controller. Results: success 1 query / 0 callbacks; query failure 1 / 1 with false and identical Throwable; guard failure 0 / 1; declined 0 / 0 with positioning attempt; null 0 / 0 without positioning; plain-table cast escaped before query/callback/positioning; a throwing failure callback escaped once after finally; simulated positioning failure masked that callback exception. Every observed callback asserted caller-thread and lock ownership. The simulated guard and scheduling outcomes exercise control flow; the extraction does not run JavaFX, Alerts, or the real Dataset.

Local command from repo root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File src/test/probes/Test-D5ExecuteQuery.ps1
```

Corporate command from repo root with Java 25 on PATH, project classes built from the reported HEAD under `target/classes` and `FORE25_CORPORATE_DEPS` set to the semicolon-separated project dependency classpath (including JInvCommon and JavaFX 25):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -Command '. .\src\test\probes\Test-D5ExecuteQuery.ps1 -CorporateClasspath ("target/classes;" + $env:FORE25_CORPORATE_DEPS)'
```

The corporate runner loads the real adapter and JavaFX, uses ArrayDataSet through a forwarding IDataSet proxy to record execute calls and inject an error, and checks success/error/null/cast without an external DB. Full-scan dialogs and FX positioning failure injection are outside that runner.

### HYPOTHESIS

Public callback documentation and the parallel consumer support one result notification when an attempted query returns normally or fails, with `(true,null)` / `(false,th)`. The current synchronous success path omits that notification. No in-tree non-null sync consumer establishes stronger completion, cancellation, or precondition guarantees.

Proposed minimal internal change, pending approval: in the sync block track successful return of the Dataset call. In the existing `finally`, add an inner `try/finally`; its try invokes `(true,null)` only when the query returned normally, and its finally retains `positionRow(0)`. Placing success notification outside the query catch prevents a throwing success callback from being reported as a second query result. Preserve the existing caller thread/lock, query errors, positioning on all existing guarded paths and public/protected signatures. With this ordering a success callback can throw to the caller and positioning still runs; an enqueue failure can mask that exception as on the existing error path. Leave cancellation silent unless explicitly approved otherwise. Agree on a cancellation signal and check consumers before introducing one; `(false,null)` already causes the LOV consumer to throw, and `(true,null)` denotes successful execution.

### UNKNOWN

External sync callers and their cancellation/completion expectations; corporate JavaFX/ArrayDataSet run and real full-scan dialog runtime. No evidence supports adding callbacks for missing Dataset or pre-try failures. No external database was accessed for probes.

### Local changes

`git status --short --branch` showed `migration/java25-correctness...origin/migration/java25-correctness` and only these untracked probe files:

- `src/test/probes/Test-D5ExecuteQuery.ps1`
- `src/test/probes/d5/D5ExecuteQueryProbe.java`

Only this mailbox reply is published.

---

## Reply D5-002

- STATUS: DONE
- BRANCH: `migration/java25-correctness`
- LOCAL COMMIT: `692ed672b6215dd5845491b7bb818f36ae324b10`
- PARENT: `0dd94764ac4d75eccc75a57c85ad005676875d19`
- PRODUCTION PUSH: NO

The initial checkout matched the expected baseline and contained only the two untracked D5 probes. The final production checkout is clean and one commit ahead of its origin tracking ref.

### Changed files

- `src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java`
- `src/test/probes/Test-D5ExecuteQuery.ps1`
- `src/test/probes/d5/D5ExecuteQueryProbe.java`

The inner catch covers the guard and Dataset query. Early returns for refusal and handled failure let successful execution fall through to one success callback without a flag. The outer finally still attempts row positioning on every path that enters the sync block. Success callback exceptions escape without a failure retry. The callback runs on the caller thread under the same lock before the positioning enqueue attempt. Public/protected signatures, runtime defaults, parallel execution, full-scan dialog logic, and JInvCommon are unchanged.

### Complete production diff

```diff
diff --git a/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java b/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
index ce8c42c..d6969e3 100644
--- a/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
+++ b/src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java
@@ -1274,11 +1274,16 @@ public class DSFXAdapter<T> extends DSControlAdapter<T> implements IDataSetListe
             synchronized( DATA_SET_LOCK )
             {
                 try {
-
-                    if( checkForFullScan() )
+                    try {
+                        if (!checkForFullScan()) {
+                            return;
+                        }
                         getDataSet().executeQuery(readAllRecords);
-                } catch (Throwable th) {
-                    callBack.accept( false, th );
+                    } catch (Throwable th) {
+                        callBack.accept(false, th);
+                        return;
+                    }
+                    callBack.accept(true, null);
                 } finally {
                     positionRow(0);
                 }
```

### Probes

Run from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File src/test/probes/Test-D5ExecuteQuery.ps1 -Before
powershell -NoProfile -ExecutionPolicy Bypass -File src/test/probes/Test-D5ExecuteQuery.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File src/test/probes/Test-D5ExecuteQuery.ps1 -Corporate
```

BEFORE reads the parent source from Git without checking it out. AFTER reads the working source. Both extraction runs passed on JDK 25.0.1 (exit 0). They compile the extracted production prelude and sync block with test doubles; they do not exercise the real adapter, Dataset, dialog, or JavaFX queue.

| Case | BEFORE | AFTER |
| --- | --- | --- |
| Successful query | 1 query, 0 callbacks | 1 query, 1 × `(true,null)`, then positioning attempt |
| Query throws | 1 query, 1 × `(false, original)` | Same |
| Guard throws | 0 queries, 1 × `(false, original)` | Same |
| Guard declines | 0 queries, 0 callbacks, positioning attempted | Same |
| Null Dataset | 0 queries, 0 callbacks, no positioning | Same |
| Controller cast fails before try | Original cast exception escapes, no callback or positioning | Same |
| Success callback throws | Callback never reached | 1 success attempt, original callback exception escapes after positioning attempt |
| Failure callback throws | 1 failure attempt, original callback exception escapes after positioning attempt | Same |
| Simulated positioning enqueue throws | Enqueue exception escapes and masks a pending failure callback exception, with no extra notification | Same, including masking of a pending success callback exception; a normal successful query still gets its one success callback before enqueue throws |

Callback checks assert original Throwable identity on errors, caller thread, lock ownership, count and order. The corporate probe passed a JDK parser-only syntax check. Corporate mode uses installed Maven and the project's cached dependencies offline, with buildnumber skipped. Local invocation stopped before compilation because `mvn` is absent from PATH. The real adapter/JavaFX 25 integration result remains UNKNOWN. The corporate probe exercises success, query failure, both throwing callbacks, null Dataset, and pre-try cast. Real full-scan dialogs and positioning enqueue failure are outside that integration probe. No external DB was used.

### D5-002 follow-up — 2026-11-21

- The local extraction probe was rerun on Oracle JDK 25.0.1 and passed all 12 scenarios. It checks the synchronous branch with test doubles.
- IntelliJ IDEA's bundled Apache Maven 3.9.16 starts from PowerShell. With the settings and repository under D:/Java/.m2, the latest offline rerun reached Java compilation.
- The compiler could not resolve corporate APIs used by fore25, including SqlDialect.forLovPredicate(String, List<String>), TaskContext.getJdbcTracer(), XXIConnectorException.isRemote(), and JDBC trace classes. The D5 commit retains the executeQuery signature. The corporate probe did not execute, so its JavaFX 25 integration result remains UNKNOWN.
- The user will handle the build setup separately. Once the matching dependencies are available, rerun Test-D5ExecuteQuery.ps1 -Corporate and record the result. Continue with the next audit item once its scope is available in the inbox.
- Production HEAD is 692ed672b6215dd5845491b7bb818f36ae324b10. No production changes or push in this follow-up.

## Reply D5-003

- Verdict — the extraction probe covers the sync callback regression. Corporate validation is pending. The committed runner requires Maven on PATH and reports all nonzero Java launcher exits through one generic error.
- Working branch is `migration/java25-correctness`. Clean local HEAD `692ed672b6215dd5845491b7bb818f36ae324b10`, parent `0dd94764ac4d75eccc75a57c85ad005676875d19`, one commit ahead of its tracking ref. Production PUSH: NO.
- Complete committed PowerShell and Java sources are in [docs/codex/d5-probe-runner-review.md](d5-probe-runner-review.md).

### Extraction review

`-Before` reads DSFXAdapter.java at the hard-coded parent SHA via `git show SHA:path`, without checkout. The default mode reads DSFXAdapter.java from disk, so it represents the committed D5 version on the verified clean checkout. The runner regex locates the four-argument overload, copies the prelude up to `Runnable executeRun`, inserts an `if (!parallel)` wrapper and appends the original synchronized block from `synchronized( DATA_SET_LOCK )` through the overload's closing branches. It inserts this text into a generated Java class. The changed inner try/catch, success callback and finally are copied verbatim. The async branch is omitted. This is a text extraction tied to the current source layout.

The generated class provides a local lock, fake Dataset and Table, and controlled replacements for full-scan guard, positioning, Platform.runLater and error handling. It calls the sync branch with a supplied callback on the caller thread and checks the event sequence, lock ownership and exception identity. It covers success, query/guard errors, refusal, null Dataset, the pre-try cast, throwing callbacks and a simulated positioning enqueue failure. It does not verify actual JavaFX scheduling, production guard behavior, real Dataset semantics or a database. Prior BEFORE and AFTER extraction runs exited 0, and the later AFTER rerun passed all 12 scenarios. This review inspected the committed sources.

### Corporate review

`-Corporate` resolves `java` and `mvn` from PATH. Maven invokes offline `compile` and `dependency:build-classpath` with profile `build` and buildnumber skipped. The launcher then places `target/classes` and the resolved test-scope dependencies on the classpath and compiles/runs the probe in Java source-file mode. The POM requests JavaFX 25 and JInvCommon 1.0.0. JDK feature 25 is checked by the source. The actual artifacts must be present and API-compatible in the corporate cache.

The probe starts real JavaFX, binds the production DSFXAdapter to a plain TableView in an unshown Stage and supplies a small property-backed listener. Its IDataSet interface proxy forwards calls to a real ArrayDataSet and unwraps reflective exceptions. For executeQuery(true) it records and delegates, and for executeQuery(false) it injects an exception before the Dataset is called. The proxy changes concrete type and identity. In this setup the plain TableView and non-XXIDataSet skip the interactive full-scan branch. The probe can check the success and injected failure callback values/count, FX-thread delivery, escaped callback exceptions, null Dataset and pre-try cast. The FX barrier checks for observed uncaught errors in queued work, with no assertion of row selection, enqueue count, or lock ownership. It does not cover real guard refusal/exception, enqueue failures, DB behavior, or actual execution of the false/read-limited Dataset query.

The latest corporate attempt reached production compilation and failed on unavailable or incompatible corporate APIs including SqlDialect.forLovPredicate(String, List<String>), TaskContext.getJdbcTracer() and JDBC trace types. The Java probe never started. Corporate JavaFX 25 result remains UNKNOWN pending the build setup.

### Windows commands

From the corporate checkout root, with JAVA_HOME already set to the corporate JDK 25 and Maven settings/cache configured:

```powershell
$env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-D5ExecuteQuery.ps1 -Corporate
```

The committed script has no direct wrapper support. The equivalent manual wrapper invocation below adds a distinct probe type-check before the JavaFX run. The wrapper is pinned to Maven 3.8.8 and needs its distribution available. Maven is offline for the project build, while wrapper bootstrap can still need the distribution. Use the configured corporate settings/repository for either invocation.

```powershell
$classpathFile = Join-Path $PWD ("target/d5-wrapper-" + [guid]::NewGuid().ToString('N') + ".txt")
& .\mvnw.cmd '-o' '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$classpathFile" '-DincludeScope=test'
if ($LASTEXITCODE -ne 0) { throw 'Wrapper or project compile/classpath failed' }
$dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
if (!$dependencies) { throw 'Maven returned an empty dependency classpath' }
$classpath = (Join-Path $PWD 'target/classes') + [IO.Path]::PathSeparator + $dependencies
$classes = New-Item -ItemType Directory -Path (Join-Path $PWD ("target/d5-probe-" + [guid]::NewGuid().ToString('N')))
& (Join-Path $env:JAVA_HOME 'bin/javac.exe') '-cp' $classpath '-d' $classes.FullName '.\src\test\probes\d5\D5ExecuteQueryProbe.java'
if ($LASTEXITCODE -ne 0) { throw 'D5 Java probe compilation/type-check failed' }
$runClasspath = $classes.FullName + [IO.Path]::PathSeparator + $classpath
& (Join-Path $env:JAVA_HOME 'bin/java.exe') '--enable-native-access=ALL-UNNAMED' '-cp' $runClasspath 'D5ExecuteQueryProbe'
if ($LASTEXITCODE -ne 0) { throw 'D5 JavaFX execution failed; inspect the runtime diagnostic' }
```

With the committed runner, a Maven nonzero exit gives `D5 corporate Maven compile/classpath failed`. Java source compilation diagnostics and runtime assertions/timeouts both give `D5 corporate probe failed`; inspect the launcher output to distinguish them. For the manual invocation, javac errors stop at the type-check step. JavaFX startup/linkage errors are runtime setup failures, and assertion/timeout diagnostics identify a failed or incomplete probe run. Exit 0 plus the AFTER FIX PASS marker is the expected integration result. The manual wrapper sequence was reviewed statically and has not been run against compatible corporate artifacts.

## Reply D5-004

- Status — DONE, pending corporate execution and review.
- Local test-only commit — `60e70689f7eeed990c64f19669b35b817d3f82b8`.
- Parent — `692ed672b6215dd5845491b7bb818f36ae324b10`.
- Branch — `migration/java25-correctness`, clean and two commits ahead of its tracking ref.
- Production changes — NONE. Production PUSH — NO.

Corporate mode validates Java and javac 25 using JAVA_HOME first and PATH when unset. It prefers Maven on PATH and falls back to the repository wrapper. Maven can use the configured repositories. The runner compiles the probe with javac and runs the compiled class, using quoted argument files and a unique target directory. Maven compile/classpath, Java probe type-check and JavaFX runtime/assertion failures have separate messages and nonzero exits. The integration assertions and extraction test behavior are preserved.

### Verification

- PowerShell parser reported zero errors. Git diff --check passed.
- BEFORE and AFTER extraction each passed all 12 scenarios with exit 0. BEFORE observed no success callback; AFTER observed one (true, null) callback before positioning.
- An invalid JAVA_HOME was rejected with a JDK 25 diagnostic and nonzero exit.
- A local JDK 25 smoke test independently verified javac/java argument files with paths containing spaces.
- Actual corporate JavaFX 25 execution remains UNKNOWN pending the user run with compatible dependencies.

### Corporate invocation

From the repository root with JDK 25 and Maven settings/cache configured:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-D5ExecuteQuery.ps1 -Corporate
```

### Exact PowerShell diff

```diff
diff --git a/src/test/probes/Test-D5ExecuteQuery.ps1 b/src/test/probes/Test-D5ExecuteQuery.ps1
index fb1a2ab..f05659e 100644
--- a/src/test/probes/Test-D5ExecuteQuery.ps1
+++ b/src/test/probes/Test-D5ExecuteQuery.ps1
@@ -6,25 +6,67 @@ param(
 
 $ErrorActionPreference = 'Stop'
 $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
-$java = (Get-Command java -ErrorAction Stop).Source
 if ($Corporate) {
-    $maven = (Get-Command mvn -ErrorAction Stop).Source
-    $dependencyFile = Join-Path $root ('target/d5-dependencies-' + [Guid]::NewGuid().ToString('N') + '.txt')
+    if ($env:JAVA_HOME) {
+        $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
+        $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
+        if (!(Test-Path -LiteralPath $java -PathType Leaf) -or !(Test-Path -LiteralPath $javac -PathType Leaf)) {
+            throw "JAVA_HOME must point to a JDK 25 with java.exe and javac.exe: $env:JAVA_HOME"
+        }
+    } else {
+        $java = (Get-Command java -CommandType Application -ErrorAction SilentlyContinue).Source
+        $javac = (Get-Command javac -CommandType Application -ErrorAction SilentlyContinue).Source
+        if (!$java -or !$javac) { throw 'JDK 25 required: set JAVA_HOME or put java and javac on PATH' }
+    }
+    $javaVersion = & $java '--version'
+    if ($LASTEXITCODE -ne 0 -or ($javaVersion -join "`n") -notmatch '^(?:openjdk|java) 25(?:[. +\-]|$)') {
+        throw "D5 requires Java 25. $java reported: $javaVersion"
+    }
+    $javacVersion = & $javac '--version'
+    if ($LASTEXITCODE -ne 0 -or ($javacVersion -join "`n") -notmatch '^javac 25(?:[. +\-]|$)') {
+        throw "D5 requires javac 25. $javac reported: $javacVersion"
+    }
+
+    $maven = (Get-Command mvn -CommandType Application -ErrorAction SilentlyContinue).Source
+    if (!$maven) {
+        $maven = Join-Path $root 'mvnw.cmd'
+        if (!(Test-Path -LiteralPath $maven -PathType Leaf)) {
+            throw 'D5 requires mvn on PATH or mvnw.cmd in the project root'
+        }
+    }
+    $output = Join-Path $root ('target/d5-corporate-' + [Guid]::NewGuid().ToString('N'))
+    New-Item -ItemType Directory -Path $output | Out-Null
+    $dependencyFile = Join-Path $output 'dependencies.txt'
     Push-Location $root
     try {
-        & $maven '-o' '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
+        & $maven '-Pbuild' '-Dmaven.buildNumber.skip=true' '-DskipTests' 'compile' 'dependency:build-classpath' "-Dmdep.outputFile=$dependencyFile" '-DincludeScope=test'
         if ($LASTEXITCODE -ne 0) { throw 'D5 corporate Maven compile/classpath failed' }
     } finally {
         Pop-Location
     }
+    if (!(Test-Path -LiteralPath $dependencyFile -PathType Leaf)) {
+        throw 'D5 corporate Maven compile/classpath failed: dependency file is missing'
+    }
     $dependencies = [IO.File]::ReadAllText($dependencyFile).Trim()
-    if (!$dependencies) { throw 'Maven returned an empty dependency classpath' }
+    if (!$dependencies) { throw 'D5 corporate Maven compile/classpath failed: dependency classpath is empty' }
     $classpath = (Join-Path $root 'target/classes') + [IO.Path]::PathSeparator + $dependencies
-    & $java '--enable-native-access=ALL-UNNAMED' '-cp' $classpath (Join-Path $PSScriptRoot 'd5/D5ExecuteQueryProbe.java')
-    if ($LASTEXITCODE -ne 0) { throw 'D5 corporate probe failed' }
+    $probeSource = Join-Path $PSScriptRoot 'd5/D5ExecuteQueryProbe.java'
+    $compileArgs = Join-Path $output 'javac.args'
+    $runArgs = Join-Path $output 'java.args'
+    $runClasspath = $output + [IO.Path]::PathSeparator + $classpath
+    # Кавычки в файлах аргументов сохраняют пробелы, а прямые слеши исключают экранирование путей.
+    [IO.File]::WriteAllLines($compileArgs, @('-cp', ('"' + $classpath.Replace('\', '/') + '"'),
+        '-d', ('"' + $output.Replace('\', '/') + '"'), ('"' + $probeSource.Replace('\', '/') + '"')))
+    [IO.File]::WriteAllLines($runArgs, @('--enable-native-access=ALL-UNNAMED',
+        '-cp', ('"' + $runClasspath.Replace('\', '/') + '"'), 'D5ExecuteQueryProbe'))
+    & $javac "@$compileArgs"
+    if ($LASTEXITCODE -ne 0) { throw 'D5 corporate Java probe compilation/type-check failed' }
+    & $java "@$runArgs"
+    if ($LASTEXITCODE -ne 0) { throw 'D5 corporate JavaFX runtime/assertion failed' }
     return
 }
 
+$java = (Get-Command java -ErrorAction Stop).Source
 $relativePath = 'src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java'
 if ($Before) {
     $baseline = '0dd94764ac4d75eccc75a57c85ad005676875d19'
```

## Reply GUI-001

### D5 publication

Pushed exactly `60e70689f7eeed990c64f19669b35b817d3f82b8` to `migration/java25-correctness` by fast-forward after verifying the clean checkout and the two approved commits. Verified the remote SHA with git ls-remote. D5 corporate JavaFX 25 runtime remains UNKNOWN.

### GUI inventory and selected path

- JInvTable installs JInvTableViewSkin. The skin uses the Legacy header, filter toolbar and status bar. JInvTable tracks the selected column. JInvTableColumn has distinct sizing/preferences and cell-editing paths. The small smoke uses an in-memory item list, two real columns and cell selection, without entering saved preference or Dataset paths.
- JInvTextField installs JInvTextFieldButtonSkin. It uses the Legacy text formatter and focus hooks. JInvTextArea installs JInvTextAreaSkin. Both skins have context-menu installation and ownership logic. The run targets focused text replacement in JInvTextField.
- JInvComboBox creates JInvComboBoxListViewSkin, which synchronizes text through the editable ComboBox's editor. Popup opening, list traversal and commit/cancel are future runtime checks.
- TestIconMain shows FXML/CSS but targets icons and sets a fixed icon path. Existing D3/D5 probes use a plain JavaFX TableView and do not show their Stage. The new harness uses a minimal inline FXMLLoader document plus the production css/general.css, also used by JInvMainFrame. SceneBuilder and full application forms are outside this run.
- JInvPropertySheet/JInvPropertySheetSkin wrap ControlsFX. JInvToolBar/JInvToolBarSkin and property editor/toolbar interaction are next-tier checks. Table-skin child initialization is covered only as part of creating and laying out the table.
- The candidate path uses a normal JavaFX Stage/Scene and no Dataset, BaseApp bootstrap, login or business DB. Whether it completes on the corporate JavaFX 25 runtime remains to be verified.

### Harness

- `src/test/probes/gui/LegacyGuiSmoke.java`
- `src/test/probes/Test-LegacyGuiSmoke.ps1`

From the repo root on an interactive Windows desktop with JDK 25 and corporate Maven settings configured:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1
```

Uses mvn on PATH, with fallback to the existing mvnw.cmd. Maven resolves the project classpath and compiles production classes with buildnumber skipped. The launcher separately compiles the smoke and runs it with that classpath. The smoke logs Java/JavaFX versions and Legacy class origin. It requires a shown and focused Stage, the actual Legacy table/editor skins, a laid-out cell in the second column, selection of row B and that column, focus transfer from table to editor, and text replacement. It loads the inline FXML and attaches the production stylesheet before CSS/layout. Success prints GUI-001 PASS with exit 0. The window closes when the assertions finish. If desktop policy withholds focus, activate the smoke window within 30 seconds.

Table edit commit/cancel, persisted sizing, physical mouse/key gestures, menu ownership, TextArea, ComboBox popup, SceneBuilder, full forms and ControlsFX interoperability remain outside the smoke assertions. All real GUI results remain UNKNOWN until a run reaches JavaFX.

### Local result

Java 25 parser and PowerShell parser checks passed. Verified FXMLLoader's InputStream overload against the cached JavaFX 25 jar. Git diff --cached --check passed on the GUI files. The local run used IDEA Maven 3.9.16 and the existing offline cache. It reached project compilation but failed on corporate API mismatches including SqlDialect.forLovPredicate(String, List<String>) and TaskContext.getJdbcTracer(). It exited nonzero at the Maven stage before Java smoke compilation or window creation.

The first local attempt also exposed duplicate applications returned by PowerShell for mvn on this machine (mvn.cmd and extensionless mvn). GUI-001 explicitly takes the first result. The already approved D5 runner uses the same unfiltered lookup pattern and was left as published.

Local test-only commit `74fa142a5af7722809e6ce842be0581d0fe7cd8c`, parent `60e70689f7eeed990c64f19669b35b817d3f82b8`. Working tree clean. Production changes for GUI-001 NONE. GUI-001 push NONE. Stopped for review.

## Reply GUI-002

### Verdict

The commit is suitable for publication as a narrow test-only harness, with the runtime result still **UNKNOWN**. The previous local launch stopped at project compilation because the cached corporate dependencies lack APIs such as SqlDialect.forLovPredicate(String, List<String>) and TaskContext.getJdbcTracer(). This review was static. No new GUI or Maven run was performed.

### Exact review evidence

- Local branch `migration/java25-correctness`, clean, one commit ahead of its tracking ref.
- HEAD `74fa142a5af7722809e6ce842be0581d0fe7cd8c`, parent and remote tracking SHA `60e70689f7eeed990c64f19669b35b817d3f82b8`.
- Added `src/test/probes/gui/LegacyGuiSmoke.java` (134 lines) and `src/test/probes/Test-LegacyGuiSmoke.ps1` (54 lines). Total 188 additions and zero deletions.
- [Complete exact diff and both committed sources](gui-001-review.md). Verified all three blocks against Git. The commit passes git diff --check.
- Production, supported public/protected APIs, JInvCommon and deferred areas are untouched. No changes to the local GUI commit. Production PUSH: NO.

### What this run can establish

The harness calls Stage.show on the FX thread and requires that the Stage is both focused at the callback and showing when it runs the checks. FXMLLoader instantiates the actual no-argument Legacy controls, the scene uses the project's CSS resource, and the harness verifies the production table/editor skin types. Two real Legacy columns have in-memory values. Applying CSS and laying out the scene must produce a TableCell with the second row's second-column value and a positive column width. clearAndSelect must update the selected row and the Legacy selected-column index. Focus must transfer from table to editor, and editing uses selectRange and replaceSelection on the real editor. These are programmatic interactions. Physical key/mouse input, table edit commit, menu ownership, ComboBox, SceneBuilder and the corporate app shell are outside the assertions.

The inline FXML is a valid minimal route through the actual loader and Legacy constructors. It has no relative includes or controller injection. Missing production classes, constructor/resource-bundle failures or a missing CSS resource fail setup. The general.css URL is attached and applyCss is called, but no style-specific value is asserted and JavaFX may log CSS warnings without failing. PASS establishes only the explicit control assertions, not full stylesheet validity or full form/resource resolution.

### Runtime and launcher findings

- Focus denied by Windows should keep the test waiting or fail a focus assertion rather than give PASS. The 30-second wait is on the verdict after Platform.startup returns. There is no outer process watchdog for a stalled toolkit startup or shutdown. Run on an interactive desktop and activate the smoke window if necessary.
- The nested runLater gives already queued FX tasks an opportunity to run before close and success. It is not a guarantee that later pulses or delayed work all finish. The FX-thread exception handler can complete the verdict exceptionally before success. The window closes after the checks, and the main thread calls Platform.exit then System.exit. Failures exit nonzero and do not print GUI-001 PASS. The current launcher does not emit a literal UNKNOWN marker for setup failures, so runtime evidence remains UNKNOWN when Maven, javac, native startup or focus prevents the assertions from completing. An assertion failure after valid setup should be recorded as a failed smoke with its diagnostic.
- The runner uses JAVA_HOME/bin when set and otherwise PATH, validating both java and javac major version 25. Maven chooses the first application on PATH and falls back to the existing root mvnw.cmd only when no mvn is found. The fallback is present in source; it was not run in this review. Maven can use configured repositories. If JAVA_HOME is unset and PATH selects java 25, the Maven installation or toolchains may still select a distinct compiler. Set JAVA_HOME to the corporate JDK before running.
- A unique target/gui-001-* folder contains dependency output, args and smoke classes. Windows paths with spaces are kept in single PowerShell arguments for Maven, while javac/java use quoted argfiles with forward slashes. The project compiles before the smoke, and Maven/classpath, javac and runtime errors each have their own diagnostic and nonzero exit.
- JavaFX is supplied by the project's Maven classpath with target/classes and test-scope dependencies. The smoke verifies javafx.version after startup. The main class does not extend Application, so it explicitly starts the toolkit. Native access is enabled for unnamed modules. A working Windows JavaFX native artifact and desktop session are still required. A module-access or native loading failure is a runtime setup diagnostic. This review did not verify the resolved corporate classpath or an actual displayed window.

### Corporate Windows command

From the repo root in an interactive desktop session, with JAVA_HOME set to JDK 25 and corporate Maven configured. No external business database required.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1
```

## Reply GUI-003

- Push succeeded by fast-forward. Local and remote `migration/java25-correctness` are both at `74fa142a5af7722809e6ce842be0581d0fe7cd8c`, verified remotely with `git ls-remote`. Parent is `60e70689f7eeed990c64f19669b35b817d3f82b8`. The pre-push worktree was clean and the commit adds only the two reviewed smoke test files.
- Production source, public/protected APIs and JInvCommon changes: NONE.
- GUI JavaFX 25 runtime: UNKNOWN. D5 corporate runtime remains pending separately.

From the corporate Windows checkout of `migration/java25-correctness`, in an interactive desktop session with JAVA_HOME set to JDK 25 and corporate Maven settings configured:

```powershell
git pull
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1
```

Record GUI PASS only when the launcher prints `GUI-001 PASS` and exits 0. For failures, report the exact diagnostic and classify the stage as Maven/classpath, javac, toolkit/native startup, application infrastructure, or focus/layout/assertion. No follow-on implementation was started.

## Reply GUI-004

- GUI-001 is CLOSED for its tested matrix based on the user's corporate BUILD SUCCESS, visible Stage and GUI-001 PASS. The user reported nonblocking unnamed-module and icon-path warnings. This is not evidence of complete component or full-form equivalence.
- Added `src/test/probes/gui/LegacyEditorsSmoke.java` and extended `src/test/probes/Test-LegacyGuiSmoke.ps1` with `-Editors`. The default launcher still selects the original GUI-001 class. Maven settings, including MAVEN_ARGS, are inherited.
- Local test-only commit `22d2dffff9d48558193a3c95184fe1ed94b51244` with parent `504f94d6fa4331ce3fe5e561afb8a96e510ebacd`. Clean working tree, one commit ahead of origin. [Exact review diff](gui-004-review.md).
- Production source, API, POM, Maven wrapper and JInvCommon changes: NONE. Working branch push: NO.

### Smoke coverage

The harness uses real Legacy TextArea and editable ComboBox with their production skins, project CSS, a shown/focused Stage and nonzero layout. It exercises programmatic TextArea focus, selection and replacement. It checks the skin-installed menu is populated, yields to a request handler, leaves a caller-supplied menu intact, and restores the original skin menu after removal.

The ComboBox test applies an explicit trim/uppercase converter, checks selection/index/value/editor consistency, and runs Legacy forceCommitValue for both a changed value and normalization back to the same value. It requests actual popup show/hide. After FX pulses it checks the skin popup content belongs to a showing PopupWindow owned by the Stage, then checks the window is hidden, combo focus is retained and focus can move back to the TextArea. The harness does not perform physical input or invoke a context-menu action, SQL extension styling, external editor, LOV, login, or database operations. No BaseApp bootstrap or icon path is configured. A resource or desktop failure should be classified from its diagnostic rather than treated as a confirmed control bug.

### Checks and command

Java 25 parser and PowerShell parser passed. Local Maven compile/classpath with existing settings and offline cache reported BUILD SUCCESS. Both smoke sources passed explicit javac type-check against the project's classes and dependency classpath. Git diff --check passed on the staged test files. No Stage was launched locally. **GUI-004 corporate JavaFX 25 runtime remains UNKNOWN.**

From the checkout containing the local GUI-004 commit, run on an interactive Windows desktop with JDK 25 and Maven settings already configured:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -Editors
```

Expect GUI-004 PASS and exit 0 only after all assertions complete. The launcher distinguishes Maven/classpath, javac and JavaFX errors. The harness identifies focus/layout/menu/commit/popup failures and reports a focus/pulse timeout separately. No GUI-005 or other follow-on started.



---

## Reply GUI-005

**MESSAGE-ID:** GUI-005  
**STATUS:** DONE, published; interactive runtime verification pending

**GUI-004 push:** Fast-forward from `504f94d6fa4331ce3fe5e561afb8a96e510ebacd` to `22d2dffff9d48558193a3c95184fe1ed94b51244`. Before push, the working tree was clean, the local branch and exact single parent matched the authorization, and `git ls-remote` returned the approved parent. After push, `git ls-remote` returned `22d2dffff9d48558193a3c95184fe1ed94b51244`. GUI-004 PASS is the user's reported interactive result for the TextArea and ComboBox matrix.

**GUI-005 push:** `migration/java25-correctness` at `13ceedc419941bb53b701707e2d0a737af9edeba`, parent `22d2dffff9d48558193a3c95184fe1ed94b51244`, clean tree. Fast-forward from the parent to this exact test-only commit under the confirmed standing approval.

- Added `src/test/probes/gui/LegacyTableEditingSmoke.java` and extended `src/test/probes/Test-LegacyGuiSmoke.ps1` with `-TableEditing`. No flag still selects GUI-001 and `-Editors` selects GUI-004. Maven invocation retains the caller's environment and settings.
- `JInvTable` inherits the TableView edit path, and its separate read-only setting controls table actions. `JInvTableColumn` constructors set JavaFX editable to false. The probe explicitly enables editability on table and column, uses the real `JInvTableViewSkin` and the public `JInvTextFieldTableCell.forTableColumn()` factory with an observable property per row. The editor is deprecated and has no in-repository instantiation outside its factory. The filter-specific `ChoiceFilterController.EditingCell` takes a `TaskContext` and couples commit to filter parameters and optional database persistence.
- The probe checks visible Stage, production CSS, Legacy skin, realized editable cells, and entry via `table.edit(0, name)`. It checks editor focus and cell selection, injects Enter key release into the real editor, and checks that exactly the first property changes once. The production Enter handler selects and edits the next row, whose editor must receive focus. The probe changes its draft and clears editing through `table.edit(-1, null)`; row two stays selected, both model values and change counts remain as expected, and the editor is removed. This cancel scenario has no mouse press because the Legacy cell's mouse filter can remember the editing position and trigger a commit on cancel. Coverage is programmatic, including synthetic Enter. Physical input, persisted values and interaction with a business Dataset remain outside this matrix.
- Local checks passed for PowerShell parsing, JDK 25 javac against existing real `target/classes` and the GUI-004 Maven dependency classpath, and staged `git diff --check`. Interactive JavaFX 25 GUI status UNKNOWN pending user execution. The runner distinguishes Maven/classpath, javac and runtime failures. The harness labels setup, layout, focus, selection, editing, model and cancel failures. A focus/FX timeout requires an interactive desktop check before attributing a control defect.
- Production source, public/protected API, POM/wrapper, JInvCommon, D6/D7, logback, JInvXMLViewer and ru.inversion.diff changes NONE.

Run from the project root on an interactive Windows desktop with JDK 25 and corporate Maven settings:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableEditing
```

Successful execution prints `GUI-005 PASS` and exits 0.

### Full exact diff

```diff
diff --git a/src/test/probes/Test-LegacyGuiSmoke.ps1 b/src/test/probes/Test-LegacyGuiSmoke.ps1
index 7de4105ec8a19cccf1958af1acb95c067d183b88..0c4c8603caf150950de72ede504162ad2d1bd68c 100644
--- a/src/test/probes/Test-LegacyGuiSmoke.ps1
+++ b/src/test/probes/Test-LegacyGuiSmoke.ps1
@@ -1,10 +1,19 @@
-param([switch] $Editors)
+[CmdletBinding(DefaultParameterSetName = 'Default')]
+param(
+    [Parameter(ParameterSetName = 'Editors')]
+    [switch] $Editors,
+    [Parameter(ParameterSetName = 'TableEditing')]
+    [switch] $TableEditing
+)
 
 $probeId = 'GUI-001'
 $probeClass = 'LegacyGuiSmoke'
 if ($Editors) {
     $probeId = 'GUI-004'
     $probeClass = 'LegacyEditorsSmoke'
+} elseif ($TableEditing) {
+    $probeId = 'GUI-005'
+    $probeClass = 'LegacyTableEditingSmoke'
 }
 $ErrorActionPreference = 'Stop'
 $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
diff --git a/src/test/probes/gui/LegacyTableEditingSmoke.java b/src/test/probes/gui/LegacyTableEditingSmoke.java
new file mode 100644
index 0000000000000000000000000000000000000000..94ef3631a2f6e523a9882ba2383f0a7e25612435
--- /dev/null
+++ b/src/test/probes/gui/LegacyTableEditingSmoke.java
@@ -0,0 +1,191 @@
+import java.util.Objects;
+import java.util.concurrent.CompletableFuture;
+import java.util.concurrent.TimeUnit;
+import java.util.concurrent.TimeoutException;
+import javafx.application.Platform;
+import javafx.beans.property.SimpleStringProperty;
+import javafx.beans.property.StringProperty;
+import javafx.event.Event;
+import javafx.scene.Scene;
+import javafx.scene.control.TextField;
+import javafx.scene.input.KeyCode;
+import javafx.scene.input.KeyEvent;
+import javafx.scene.layout.StackPane;
+import javafx.stage.Stage;
+import ru.inversion.fx.form.controls.JInvTable;
+import ru.inversion.fx.form.controls.JInvTableColumn;
+import ru.inversion.fx.form.controls.renderer.JInvTextFieldTableCell;
+import ru.inversion.fx.form.controls.skin.JInvTableViewSkin;
+
+@SuppressWarnings("deprecation")
+public final class LegacyTableEditingSmoke {
+    private record Row(StringProperty name) {
+        Row(String name) {
+            this(new SimpleStringProperty(name));
+        }
+    }
+
+    private final CompletableFuture<Void> verdict = new CompletableFuture<>();
+    private final Row first = new Row("First");
+    private final Row second = new Row("Second");
+    private Stage stage;
+    private JInvTable<Row> table;
+    private JInvTableColumn<Row, String> name;
+    private boolean started;
+    private int firstChanges;
+    private int secondChanges;
+
+    public static void main(String[] args) {
+        var smoke = new LegacyTableEditingSmoke();
+        int exit = 1;
+        try {
+            require(Runtime.version().feature() == 25, "setup: Java 25 required");
+            Platform.startup(() -> smoke.runChecked(smoke::show));
+            smoke.verdict.get(30, TimeUnit.SECONDS);
+            System.out.println("GUI-005 PASS");
+            exit = 0;
+        } catch (TimeoutException failure) {
+            System.err.println("GUI-005 UNKNOWN: timed out waiting for Stage focus or FX queue. Use an interactive desktop.");
+        } catch (Throwable failure) {
+            System.err.println("GUI-005 failed; see the setup, focus, or editing diagnostic below.");
+            failure.printStackTrace(System.err);
+        } finally {
+            Platform.exit();
+        }
+        System.exit(exit);
+    }
+
+    private void show() {
+        Platform.setImplicitExit(false);
+        Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> verdict.completeExceptionally(failure));
+        String version = System.getProperty("javafx.version", "UNKNOWN");
+        require(version.equals("25") || version.startsWith("25."), "setup: JavaFX 25 required: " + version);
+        System.out.println("Java=" + Runtime.version() + " JavaFX=" + version);
+        System.out.println("Legacy controls=" + JInvTable.class.getProtectionDomain().getCodeSource().getLocation());
+
+        table = new JInvTable<>();
+        name = new JInvTableColumn<>("Name");
+        require(!name.isEditable(), "setup: Legacy column should start non-editable");
+        table.setEditable(true);
+        name.setEditable(true);
+        name.setCellValueFactory(cell -> cell.getValue().name());
+        name.setCellFactory(JInvTextFieldTableCell.forTableColumn());
+        first.name().addListener((property, before, after) -> firstChanges++);
+        second.name().addListener((property, before, after) -> secondChanges++);
+        table.getColumns().add(name);
+        table.getItems().setAll(first, second);
+        table.getSelectionModel().setCellSelectionEnabled(true);
+
+        var scene = new Scene(new StackPane(table), 420, 240);
+        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/general.css"),
+                "setup: production CSS missing").toExternalForm());
+        stage = new Stage();
+        stage.setTitle("GUI-005 Legacy table editing smoke");
+        stage.setScene(scene);
+        stage.focusedProperty().addListener((property, before, focused) -> {
+            if (focused) later(() -> {
+                if (stage.isFocused() && !started) {
+                    started = true;
+                    startEdit();
+                }
+            });
+        });
+        stage.show();
+        stage.requestFocus();
+    }
+
+    private void startEdit() {
+        stage.getScene().getRoot().applyCss();
+        stage.getScene().getRoot().layout();
+        require(stage.isShowing() && table.getSkin() instanceof JInvTableViewSkin<?>
+                        && table.getWidth() > 0 && name.getWidth() > 0,
+                "layout: visible Legacy table skin and column required");
+        require(table.isEditable() && name.isEditable() && cellAt(0).isEditable() && cellAt(1).isEditable(),
+                "setup: table, column and both realized Legacy cells must be editable");
+        table.requestFocus();
+        require(stage.isFocused() && table.isFocused(), "focus: table did not receive focus");
+        table.getSelectionModel().clearAndSelect(0, name);
+        table.edit(0, name);
+        later(this::commitAndMove);
+    }
+
+    private void commitAndMove() {
+        TextField editor = requireEditor(0);
+        editor.setText("Updated first");
+        Event.fireEvent(editor, new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.ENTER,
+                false, false, false, false));
+        requireModel();
+        later(this::cancelSecond);
+    }
+
+    private void cancelSecond() {
+        TextField editor = requireEditor(1);
+        require(!cellAt(0).isEditing() && cellAt(0).getGraphic() == null,
+                "navigation: first cell still has an editor after Enter");
+        editor.setText("Discard this");
+        table.edit(-1, null);
+        later(() -> {
+            require(table.getEditingCell() == null && !cellAt(1).isEditing() && cellAt(1).getGraphic() == null,
+                    "cancel: second cell or table remains in editing state");
+            requireSelected(1);
+            requireModel();
+            System.out.println("Legacy table edit, single-property commit, Enter navigation/focus and cancel passed");
+            stage.close();
+            verdict.complete(null);
+        });
+    }
+
+    private TextField requireEditor(int row) {
+        requireSelected(row);
+        var position = table.getEditingCell();
+        var cell = cellAt(row);
+        require(position != null && position.getRow() == row && position.getTableColumn() == name
+                        && cell.isEditing() && cell.getGraphic() instanceof TextField,
+                "editing: Legacy editor not active at row " + row);
+        var editor = (TextField) cell.getGraphic();
+        require(stage.isFocused() && stage.getScene().getFocusOwner() == editor && editor.isFocused(),
+                "focus: Legacy cell editor did not receive focus at row " + row);
+        return editor;
+    }
+
+    private void requireSelected(int row) {
+        var selection = table.getSelectionModel();
+        require(selection.isSelected(row, name) && selection.getSelectedCells().size() == 1
+                        && selection.getSelectedItem() == table.getItems().get(row),
+                "selection: expected only row " + row + " in Name");
+    }
+
+    private void requireModel() {
+        require("Updated first".equals(first.name().get()) && firstChanges == 1
+                        && "Second".equals(second.name().get()) && secondChanges == 0,
+                "model: expected one change to first row and zero to second; got "
+                        + first.name().get() + "/" + firstChanges + ", " + second.name().get() + "/" + secondChanges);
+    }
+
+    private JInvTextFieldTableCell<?, ?> cellAt(int row) {
+        return table.lookupAll(".table-cell").stream()
+                .filter(node -> node instanceof JInvTextFieldTableCell<?, ?> cell
+                        && cell.getTableColumn() == name && cell.getIndex() == row && !cell.isEmpty()
+                        && cell.getWidth() > 0 && cell.getHeight() > 0)
+                .map(node -> (JInvTextFieldTableCell<?, ?>) node)
+                .findFirst()
+                .orElseThrow(() -> new AssertionError("layout: no realized Legacy cell for row " + row));
+    }
+
+    private void later(Runnable action) {
+        Platform.runLater(() -> runChecked(action));
+    }
+
+    private void runChecked(Runnable action) {
+        if (verdict.isDone()) return;
+        try {
+            action.run();
+        } catch (Throwable failure) {
+            verdict.completeExceptionally(failure);
+        }
+    }
+
+    private static void require(boolean condition, String message) {
+        if (!condition) throw new AssertionError(message);
+    }
+}
```



---

## Reply GUI-005-PUBLISH

DONE. Fast-forwarded `migration/java25-correctness` from `22d2dffff9d48558193a3c95184fe1ed94b51244` to `13ceedc419941bb53b701707e2d0a737af9edeba` and verified the latter SHA with `git ls-remote`. The starting tree was clean. JDK 25 compilation against existing project classes and Maven dependencies, PowerShell parsing and `git diff --check` passed. See Reply GUI-005 for the exact diff. Interactive JavaFX 25 result remains UNKNOWN.

Review caveat. Programmatic cancellation does not exercise the branch where a table mouse press sets the Legacy cell's `editingCell` field and `cancelEdit()` fires a commit event before calling its superclass. This is a potential data-integrity risk pending a targeted probe after the GUI-005 runtime result. Physical input, actual Escape cancellation and Dataset persistence are outside the current smoke. Enter is synthetic and the model is an in-memory JavaFX property.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableEditing
```



---

## Reply GUI-006

**Status** DONE. Fast-forward publication to `migration/java25-correctness` from `13ceedc419941bb53b701707e2d0a737af9edeba` through the two commits below. The working tree was clean, and the two parents matched the authorized linear range. The branch and this reply are published atomically and the two remote refs are checked after push.

- A, Maven settings. `e2d57812410df5ca800c4ade422286612d0ec803` adds optional `-SettingsFile` to the existing PowerShell runner. It resolves and validates a literal file path before Maven starts and supplies `-s` and the absolute path as separate arguments. Omission supplies no extra arguments and preserves the caller's Maven environment/defaults. [Exact commit diff](https://github.com/ssu95a/fore25/commit/e2d57812410df5ca800c4ade422286612d0ec803)
- B, cancellation smoke. `8ead483ae4eba6a972b474ea969aa783e953b33e` adds `LegacyTableCancelSmoke.java` and `-TableCancel`. It shows a Stage with the real table, skin, column and cell factory. It runs unarmed programmatic cancel, an in-editor synthetic primary press followed by Escape dispatched to the focused editor, and the same press followed by `table.edit(-1, null)`. The probe records drafts, model values, actual property changes, commit event row/column/value, focus and selection across each action. It checks editor teardown, no commits or model changes in the first two cases, and no double commits or cross-row writes. [Exact commit diff](https://github.com/ssu95a/fore25/commit/8ead483ae4eba6a972b474ea969aa783e953b33e)

**Confirmed in source.** The production table mouse filter records the editing position on press, Escape clears it, and `cancelEdit()` explicitly fires an edit-commit event when that position is present. A single event carrying the draft and matching a single model change after only press plus programmatic cancel supplies behavioral evidence of that branch without private-field access. That outcome is recorded and allowed. If no commit occurs, the probe reports UNKNOWN for mouse-capture coverage. Whether an actual outside click or native Escape reproduces the same behavior, and whether any genuine discard action incorrectly saves a value, remain open. Synthetic events exercise JavaFX dispatch. Native pointer targeting, focus-loss/click-away behavior and Dataset persistence are outside this probe.

**Verification.** On JDK 25.0.1, compiled all four GUI harnesses against existing project classes and the resolved GUI-004 dependencies. PowerShell parsing and dispatch for default, `-Editors`, `-TableEditing`, and `-TableCancel` passed. A Windows batch receiver confirmed native argument boundaries for a settings path with spaces and brackets. Omitted and missing-file cases also passed. No Maven credentials were read for these checks. Both staged whitespace checks passed. GUI-005 PASS is the user's reported result for programmatic edit, synthetic Enter and programmatic cancel. GUI-006 interactive JavaFX 25 result remains UNKNOWN pending user execution.

From the repository root on an interactive Windows desktop with JDK 25:

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableCancel -SettingsFile 'C:\Path With Spaces\settings.xml'
```

Omit `-SettingsFile` to use existing Maven configuration. `GUI-006 PASS` requires the expected Escape and control invariants plus observed mouse-armed commit. The harness reports the scenario and before/action/after diagnostics for investigation.

Production code, public/protected API, POM, Maven wrapper, JInvCommon, main, D6/D7, logback, JInvXMLViewer and ru.inversion.diff were outside the change.



---

## Reply GUI-007

**Status** DONE, investigation and test-only probe. Fast-forward to `0b4b2d65b05ec637959ca3a81a81df5d4d650cd3` from `8ead483ae4eba6a972b474ea969aa783e953b33e` on `migration/java25-correctness`. The test commit and this reply are published atomically and checked with `git ls-remote`. [Complete source diff](https://github.com/ssu95a/fore25/commit/0b4b2d65b05ec637959ca3a81a81df5d4d650cd3)

**Observed in the user's GUI-006 run.** The mouse-plus-Escape scenario discarded the model change and emitted zero commits. The same editor reopened with that discarded draft while the model was still Original. In the next GUI-006 scenario, the user-supplied probe wrote a different draft before committing; that run does not establish that an untouched reentry commits the previous draft.

**Lifecycle.** Legacy `startEdit()` calls `super.startEdit()` before using the editor. Local JavaFX 25 bytecode shows that `TableCell.startEdit()` calls its item-update routine before entering editing state. Legacy `updateItem(T, boolean)` assigns text to the editor only for a nonempty item while the cell is editing and the editor already exists. On first allocation Legacy explicitly clears the fresh TextField after creation. On reentry it reuses the field and sets the cell label from the converter, never assigning that label to the reused TextField at start. This establishes first-allocation behavior (blank), while the intended UX for each new session remains unspecified. Legacy cancel removes the graphic and filters without resetting the TextField, and JavaFX superclass cancel emits cancellation without refreshing item text. A dirty item can invoke an item refresh during a later layout and potentially reset text while editing. A layout pulse alone does not guarantee an item refresh. The reuse conditions of interest are the same realized cell, the existing editor and absence of a refreshing update during the new edit.

**Probe.** `-EditorReentry` uses the production Legacy cell and skin in a visible Stage without a DB. It records the first edit, sets a unique draft, routes synthetic Escape, and requires the model and commit counts to stay unchanged. It reopens the same realized cell without editing its text and logs the initial editor identity, draft and model immediately and after a pulse. Then it routes an in-editor synthetic mouse press and calls `table.edit(-1, null)`. It correlates commit event row/column/value with actual property changes and requires the second row to stay untouched. The program prints GUI-007 REPRODUCED and exits nonzero only for the diagnosed stale-draft commit, or reports other invariant/setup failures independently. It reports UNKNOWN if no mouse-armed commit is observable. GUI-007 PASS means that the exercised commit carried a value other than the discarded draft. It imposes no exact initial-text policy. Physical click-away and Dataset persistence remain outside this scenario.

**Local verification.** The four Java harness sources compiled against existing production classes and resolved JavaFX 25 dependencies. PowerShell parsing and dispatch passed for default, Editors, TableEditing, TableCancel and EditorReentry with SettingsFile. Staged whitespace checks passed. Interactive JavaFX 25 GUI-007 runtime remains UNKNOWN pending user execution. No production Java, API, POM, wrapper, JInvCommon or deferred-module change.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -EditorReentry -SettingsFile 'C:\Path With Spaces\settings.xml'
```



---

## Reply GUI-008

**Status** DONE. Fast-forward to `fa4af1b688b4e20662663a110bad2b86fe1fe48c` from `0b4b2d65b05ec637959ca3a81a81df5d4d650cd3` on `migration/java25-correctness`. The fix and this reply are published atomically and both remote refs verified. [Exact production and regression diff](https://github.com/ssu95a/fore25/commit/fa4af1b688b4e20662663a110bad2b86fe1fe48c)

**Root cause and fix.** Legacy `startEdit()` reuses the TextField and previously refreshed only the cell label. The JavaFX 25 superclass updates the item before entering editing state, so the Legacy update hook cannot be relied on for editor initialization. Each successful new session now initializes the editor from the current item before attaching it and requesting focus. An active session ignores another start request. Null starts blank. A configured converter formats non-null items, and string form is a defensive start-time fallback without a converter. Existing conversion on commit keeps its previous contract. Resetting only on Escape would leave the confirmed first-allocation problem. Relying on layout refresh would leave setup timing dependent on item invalidation. Disabling mouse-armed commit would change established Legacy behavior. The change in visible behavior is that first edit of a nonempty String now starts with its value instead of blank. Public/protected declarations match before and after, checked with javap. No changes to JInvCommon or deferred modules.

**Local RED/GREEN on interactive Windows Java 25.0.1 / JavaFX 25.** The existing user-reported GUI-007 RED was reproduced locally against the baseline class. Escape produced zero commits and property changes. Untouched reentry kept the draft through a pulse, and synthetic mouse press plus programmatic cancel committed it exactly once to row 0. A separate cold-only run of the same cold assertion against the baseline showed row 1 change from Other to an empty string with zero typing. Both RED cases exited 1. On the fixed Maven-built class, Escape still produced zero commits. Reentry showed Original both immediately and after a pulse. Untouched armed cancel emitted one event carrying Original and zero property changes. Cold first edit showed Other and produced one event carrying Other and zero property changes. No cross-row writes or duplicate commits. The end-to-end runner printed GUI-007 PASS and exited 0.

**Additional checks.** Local Maven 3.9.11 offline `-Pbuild compile` and the end-to-end dependency classpath build succeeded with the existing custom settings and cache. All smoke sources compiled on JDK 25. GUI-001, GUI-004, GUI-005 and GUI-006 also passed on the local JavaFX 25 desktop with the rebuilt cell and real project classes. Staged whitespace checks passed. The tests use synthetic input and in-memory properties. Physical click-away, production Dataset persistence and arbitrary converter round trips remain outside the matrix. A separate user rerun in the corporate environment is pending.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -EditorReentry -SettingsFile 'C:\Path With Spaces\settings.xml'
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableCancel -SettingsFile 'C:\Path With Spaces\settings.xml'
```



---

## Reply GUI-009

**Status** DONE. Fast-forward from `fa4af1b688b4e20662663a110bad2b86fe1fe48c` to `0a328b9f1ec960837862db2cd785abf2a42309ec` on `migration/java25-correctness`. The test commit and this reply are published atomically and both remote refs are verified with `git ls-remote`. [Exact test and runner diff](https://github.com/ssu95a/fore25/commit/0a328b9f1ec960837862db2cd785abf2a42309ec)

**Design.** A visible Stage with production CSS, real `JInvTable`, `JInvTableColumn`, `JInvTableViewSkin`, and the real `DSFXAdapter.bindTable()` binds three actual JInvCommon ArrayDataSet rows. Two ordinary JPA-annotated String properties bind via `setFieldName()`, EntityMetadataFactory and the production cell factory. The probe installs no custom cell factories or model copies. Source inspection found that a controller is optional in `bindColumns()`, and the fixture has neither SQL sort metadata nor a query expression needing a dialect. The skin's filter title listener branch applies to XXIDataSet. This route worked with no controller or BaseApp bootstrap.

**Verified on local interactive Windows, Java 25.0.1 / JavaFX 25.**

- The Legacy cells rendered A/Alpha, B/Bravo and C/Charlie in two columns. All three TableView entries were the identical objects returned by ArrayDataSet.
- Synthetic Down sent through the focused table moved selection and Dataset 0->1. Then Dataset cursor 1->2 selected row 2. Independent traces for navigation and selection both read [0->1, 1->2], with exactly one adapter navigation callback per action and correct row identities throughout.
- Mutating the existing bean left the visible text at Charlie through a pulse. The production column uses StubObservableValue, whose listener registration methods are no-ops. `adapter.refresh()` rebuilt visible values and rendered Charlie updated, keeping the selected row and the neighboring value.
- The adapter observer overrides callbacks only to count and delegates each to the production implementation. A test subclass of real ArrayDataSet invokes its inherited event publisher to exercise Dataset-level notification, since ArrayDataSet.executeQuery(boolean) only positions the cursor. Independent listeners proved delivery before and after adapter.close(). After close, real cursor navigation, the explicitly emitted Dataset notification, and updateCurrentRow reached the independent listeners, with zero callbacks to the closed adapter and no selection move from post-close cursor navigation. The test does not treat adapter.close() as TableView disposal.

**Checks.** JDK 25 javac for all GUI sources, PowerShell parsing and dispatch for all six modes, and staged whitespace checks passed. Full PowerShell launcher with the existing custom Maven settings and local cache completed Maven `-Pbuild compile` and dependency classpath resolution, then printed `GUI-009 PASS` and exited 0. A separate corporate rerun is pending. Physical input, SQLDataSet, business forms and DB persistence are outside the observed scope. Only the Java probe and PowerShell test runner changed.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -DataBinding -SettingsFile 'C:\Path With Spaces\settings.xml'
```



---

## Reply GUI-010

**Status** DONE for standalone production FXML/controller and SDI close. Test-only fast-forward from `0a328b9f1ec960837862db2cd785abf2a42309ec` to `789c2e59de58a963701f72fa46fe5f02e3de3df8` on `migration/java25-correctness`. The test commit and this reply are published atomically. Both remote refs are checked with `git ls-remote`. [Exact test and runner diff](https://github.com/ssu95a/fore25/commit/789c2e59de58a963701f72fa46fe5f02e3de3df8)

**CONFIRMED in source.** `FXFormLauncher.showInternal()` delegates to `AbstractBaseController.internalShow()`. It calls `BaseApp.APP().reportGuiLaunch()` before FXML and later uses the application's view preference service for frame mode. `initFormController()` attaches TaskContext and ViewContext, initializes keyboard handling, installs Stage close/show handlers and calls `initViewSettings()`. First show invokes `internalInit()`, whose `beforeInit()` needs BaseApp ModuleService and can check a license. `AbstractBaseController.initialize(URL, ResourceBundle)` is the separate FXMLLoader entry point that binds the return properties and registers the lifecycle listener. `ViewContext.of(stage)` can wrap an explicit Stage without application globals. The existing production `JInvTextAreaForm.fxml` declares its own `JInvTextAreaFormController`, injected TextArea, and standard OK button.

**CONFIRMED on local interactive Windows, Java 25.0.1 / JavaFX 25.** FXMLLoader loaded the production resource and exact production controller. The fixture explicitly supplied an existing Stage, ViewContext, String payload, edit mode, empty init properties and a return callback, with saved dimensions disabled. Package access lets it invoke the real controller `init()`, standard button configuration and register the real SDI `internalOnClose` handler without reflection, subclassing or stubbing BaseApp. `init()` populated the injected editor from controller data. A pulse confirmed visible production CSS, Legacy TextArea skin and laid-out controls. Programmatic OK fired the production action, which set the bound return result and dispatched one WINDOW_CLOSE_REQUEST. The real handler reached ON_OK then ON_CLOSE, delivered one RET_OK callback with no exception and hid the form. The production close-resource progress Stage appeared and disappeared, leaving no showing windows. This uses the no-entity, no-owned-TaskContext branch. Saved dimensions are disabled and the form collects no preference records, so the DAO's empty-list guard avoids SQL.

**UNKNOWN outside this run.** Full launcher startup, first-show BEFORE_INIT/INIT/AFTER_INIT/RUNTIME orchestration, login, keyboard setup, ModuleService, licensing, nonempty preferences, owned TaskContext cleanup, SQLDataSet, business forms and physical input. The next launcher check needs an actual initialized BaseApp with the application services and the form's supported TaskContext, on a sanctioned desktop. A standalone Stage cannot exercise that entry point. GUI-009 remains user-confirmed PASS for ArrayDataSet, and Caffeine 2.9.3 Unsafe warnings remain DEFERRED third-party compatibility debt.

**Verification.** Compiled all GUI probe sources on JDK 25. PowerShell parser and dispatch checks passed for all seven modes. Maven `-Pbuild compile` and dependency classpath resolution completed with existing custom settings and local cache, followed by `GUI-010 PASS` and exit 0. Standalone direct GUI invocation also passed. The unnamed-module and missing icon path warnings were nonblocking. Staged whitespace checks passed. Only the probe and test runner changed.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -FormLifecycle -SettingsFile 'C:\Path With Spaces\settings.xml'
```



---

## Reply GUI-011

**Status** Read-only investigation complete. Full-launcher execution BLOCKED under the isolated no-login/no-DB constraints. JavaFX 25 full-launcher runtime **UNKNOWN**. GUI-010 remains the user's confirmed PASS for the standalone production FXML/controller and SDI close path. No executable launcher probe or working-branch commit. The verified working branch remains `789c2e59de58a963701f72fa46fe5f02e3de3df8`.

### CONFIRMED dependency chain

1. `FXFormLauncher.show()` assembles launch options. `showInternal()` delegates to `AbstractBaseController.internalShow()`, which calls `BaseApp.APP().reportGuiLaunch()` synchronously before scheduling FX work. Without BaseApp the path ends here. The FX task loads FXML, reads frame mode from the actual ViewPrefAppService and PRP configuration (SDI is the fallback), creates and attaches the Stage and controller, then calls `initFormController()`. `ViewContext.of(stage)` can wrap a real explicit Stage but supplies neither app services nor login context. [FXFormLauncher](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/form/FXFormLauncher.java#L376) [AbstractBaseController](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/form/AbstractBaseController.java#L474)
2. `BaseApp.init()` publishes the singleton and performs pre-run configuration, but full `start(Stage)` prepares the primary ViewContext, calls `LoginManager.login()`, and on success runs the upgrade check, after-login configuration and main-window startup. Login uses an authenticated datasource or dialog; the pseudo-connection setting still goes through the login flow. `internalAfterLogin()` initializes DB_USER and after-login properties and installs database locale handling. The in-repo BaseApp subclass SearchMain inherits the normal start/login path. No supported no-login BaseApp test bootstrap was found in the repository or probes. Calling init alone is insufficient evidence of an initialized application, and was not attempted. [BaseApp init/start](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/app/BaseApp.java#L408) [LoginManager](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/app/login/LoginManager.java#L340)
3. `initFormController()` receives TaskContext, ViewContext, mode, data and result property, initializes keyboard handling and view settings, installs the close handler, and registers one-shot initialization on Stage showing. `internalInit()` transitions BEFORE_INIT -> INIT, invoking license check when SSCODE is present, the real ModuleService, state decorator, data/FXEntity setup, controller `init()`, control binding/configuration and standard buttons. It then enters AFTER_INIT and RUNTIME. The TextArea controller inherits null SSCODE, so its per-form license check is skipped by production logic. ModuleService lookup still executes. A supplied non-owned TaskContext still enters the session-title path; inspection with `javap -c` confirmed the dependency's `assignSessionTitle()` and `restoreSessionTitle()` obtain a JDBC connection for Oracle `dbms_application_info.set_action`. Ownership determines context closing, not those calls. [Controller initialization](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/form/AbstractBaseController.java#L1292) [Before-init hook](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/form/AbstractBaseController.java#L1475)
4. Passing null TaskContext is not a supported isolation strategy. `internalAfterInit()` calls `getTaskContext()`, which lazily creates an owned context, and asks the real view preference service for a connection indicator. That service reads DB_USER preferences, whose uncached PrefStorage path uses the application's common TaskContext and executes a SQL call. The indicator lookup can catch errors and continue; reaching RUNTIME alone would therefore not establish a healthy bootstrap. The system's existing `JInvTextAreaForm.show()` passes the app's common TaskContext. Its String payload is not an entity and avoids entity-save work, but leaves the application's session and preference dependencies in place. [After-init](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/form/AbstractBaseController.java#L1666) [View preference indicator](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/app/service/ViewPrefAppService.java#L1017) [Production TextArea entry](https://github.com/ssu95a/fore25/blob/789c2e59de58a963701f72fa46fe5f02e3de3df8/src/main/java/ru/inversion/fx/form/JInvTextAreaForm.java#L23)

### Smallest supervised next experiment

Decide whether an existing, normally authenticated corporate JavaFX 25 desktop session may be used for a single TextArea form. Within that app's approved test entry point, invoke the real nonmodal `FXFormLauncher<String>` with its existing parent ViewContext, actual non-owned TaskContext, production JInvTextAreaForm FXML, fore bundle, a String payload and saved dimensions disabled. Register `controllerCallback` before show and use it to attach the lifecycle listener before the Stage is shown. Register `closeCallback` for exactly one RET_OK and error observation. Verify visible payload and BEFORE_INIT -> INIT -> AFTER_INIT -> RUNTIME, then press standard OK and observe ON_OK -> ON_CLOSE, window teardown, close-resource completion and parent context still operational. Treat any initialization exception, including caught indicator/prefs errors, as evidence to investigate. This is a plan only. No runtime proof of corporate services, preferences or cleanup exists yet for this path.

Validation was inspection of repository sources and dependency bytecode, plus clean branch and whitespace checks. No application, login, SQL session or launcher was started. No `-FullFormLauncher` command exists. Production, tests and configuration were left intact. Caffeine remains deferred.



---

## Reply GUI-012

**Status** DONE, test-only. Working branch fast-forward from `789c2e59de58a963701f72fa46fe5f02e3de3df8` to `2077722f0fb27b807dfa9bbac4bf9f43cf1d3dbc`, published atomically with this outbox reply and checked via `git ls-remote`. [Exact source diff](https://github.com/ssu95a/fore25/commit/2077722f0fb27b807dfa9bbac4bf9f43cf1d3dbc)

### Confirmed hierarchy and source-level contracts

```text
TextField -> JInvTextField
  -> JInvRegExpTextField
  -> F7FilterTextField
  -> ParamCellTextField (abstract; anonymous implementations in filter editors)
  -> JInvTextFieldCUR -> JInvCURTextField (deprecated alias)
  -> JInvValueField<T>
       -> JInvNumberField<T>
            -> JInvIntegerField
            -> JInvLongField
            -> AbstractBigDecimalField
                 -> JInvBigDecimalField -> JInvPercentField
                 -> JInvMoneyField
       -> JInvTimeField
       -> JInvTimeFieldNew
       -> JInvTextFieldFormatted
       -> JInvCustomValueField<T>
PasswordField -> JInvPasswordField (adjacent)
TextArea -> JInvTextArea (shared ITextFieldBase boundary)
```

| Class | Text, value, normalization and state | Runtime boundary / priority |
|---|---|---|
| JInvTextField | Text is the value surface. Case formatter, skin context menu, tooltip and selection cache; trim listener exists but is not installed by the constructor. State starts NULL. Validated LOV requires controller and validator manager in AFTER_INIT/RUNTIME; completion can replace or append text. | Base edit, menu and focus tested. LOV, skin replacement and external/inner button lifecycle UNKNOWN. |
| JInvRegExpTextField | Inherits base formatter and skin. Text listener checks nonempty text/configured regex, marks ERROR on failure and restores NULL/VALUE from ERROR. No typed property. | Standalone regex cases feasible; validation transitions on initial entry and clearing UNKNOWN. |
| F7FilterTextField | Text listener switches VALUE/LIST/EXPRESSION modes and validates according to filter metadata, with empty text leading to NULL on the value path. | Static colors dereference BaseApp view preferences; isolated construction is outside this probe. Filter correctness and LOV UNKNOWN. |
| ParamCellTextField | Inherited text/formatter; overridden keyboard handlers send Enter/focus loss to commit and Escape to cancel. F9 can invoke LOV. Constructor changes shared focus-on-error flag. | Needs concrete filter cell integration to exercise commit/cancel; ordering and shared focus effect UNKNOWN. |
| JInvTextFieldCUR | Separate String curValue trims and maps null to empty; writes text, and early form lifecycle changes sync the curValue. CUR validator and LOV button integrate with the form. | Default lazy LOV uses BaseApp common TaskContext; check/choice semantics UNKNOWN in real forms. |
| JInvCURTextField | Deprecated alias inheriting CUR behavior and dependencies. | Same boundary as CUR. |
| JInvValueField | Abstract typed property, inherited text and skin; clipboard copies full typed value or selected text without spaces; accepted LOV value is converted to declared type. Null/state semantics are subclass-owned. | Typed clipboard and LOV paths UNKNOWN. |
| JInvNumberField | Text/value listeners use reentrancy flags. Empty maps to null/NULL after a real text change; converter exceptions mark ERROR. Focus loss reformats non-error values without reparsing through the text listener. | Constructor mismatch confirmed below. Real focus and event count tested in four descendants. |
| JInvIntegerField | Nullable integer property and fixed TypeStringConverter through JInvCommon. Inherits base formatter and numeric focus/state rules. | Min/max and upper overflow tested. |
| JInvLongField | Nullable long property and fixed TypeStringConverter. Same rules with 64-bit range. | Min/max and upper overflow tested. |
| AbstractBigDecimalField | Non-null typed assignments use configured scale and HALF_UP. Null remains null; formatting delegated. | Data integrity on scale and locale round trips is higher priority than cosmetic output. |
| JInvBigDecimalField | Parser maps comma to dot and removes regex whitespace. Display uses configured grouping, space separator and dot decimal. Defaults: scale 2, grouping off. Mutable formatters shared by a weak cache. | Grouped comma input, rounding and focus tested with grouping enabled. Dynamic scale/current rendered value and off-FX access UNKNOWN. |
| JInvPercentField | Inherits decimal parser, display and rounding; configurable static default scale with per-field override. | Standalone feasible. No GUI-012 evidence for percent-specific scale changes. |
| JInvMoneyField | Default scale 2; shared DecimalFormat, MoneyStringConverter and bounded parse cache. Accepts separators, grouping spaces and shorthand suffixes. Typed property enforces scale; converter discards its non-mutating round() return. | Grouped comma parse, HALF_UP and focus tested. Shorthand, malformed suffixes, settings changes, precision extremes and cross-thread formatter/cache behavior UNKNOWN. |
| JInvTimeField | LocalTime, seconds mode, dedicated formatter and JInvTimeSkin. Guarded text/value updates; conversion failure marks ERROR, empty clears value, focus loss normalizes valid values. | Separate standalone time probe. Range, partial edits and seconds toggling UNKNOWN. |
| JInvTimeFieldNew | LocalTime with replacing mask formatter. Text listener treats leading underscore as NULL and otherwise calls LocalTime.parse; no empty guard at charAt(0). Value changes write empty or formatted text. Inherited button skin. | Source-level exception candidate only, since the actual formatter may mask some empty paths. Check real clear/delete, paste, invalid time and seconds toggling in GUI-013. |
| JInvTextFieldFormatted | String value, mask formatter replaces base formatter when configured. Empty clearText leads to empty value/NULL, partial masked text ERROR; complete text yields masked or stripped value, and value changes write back to text. | Standalone feasible. Partial masks, paste/delete, mask changes and feedback UNKNOWN. |
| JInvCustomValueField | Delegates property, type and text-state calculation to ICustom. No default value/null contract. Each setCustomImpl adds another text listener capturing that implementation. | Lifecycle candidate when replacing implementations; no runtime proof. Use a real configured implementation for subsequent tests. |
| JInvPasswordField | Separate JavaFX PasswordField with Legacy required/validation support. | Inventory only. |
| JInvTextArea | Separate TextArea and skin; shares ITextFieldBase value/label/LOV boundary. | GUI-004 already covers its edit/menu behavior. |

### Local interactive evidence and triage

On Windows Java 25.0.1 / JavaFX 25, the visible Stage used production CSS and real JInvTextFieldButtonSkin instances. The plain field passed empty/null/normal text, programmatic selection and replacement, and menu ownership when an application handler or custom menu was present. All four numeric fields passed typed assignment, valid edit, invalid edit preservation with ERROR, same-value recovery, clear-to-null, and a second valid edit. Each produced four value notifications and six text notifications before focus testing, and zero value changes on focus loss. Integer/Long upper overflow retained the old value. Both decimals normalized grouped comma input to value 12345.69 at scale 2, and displayed 12 345.69 after focus loss on the test desktop. Solo constructor samples confirmed that all four numeric String constructors with "42" render "42" while typed value remains null and state NULL. Source explains the divergence: initial text is passed to super before the numeric listener is installed. This is a confirmed observable integrity inconsistency; intended constructor contract, external usages and persistence impact remain UNKNOWN. In-repo creation sites found use no-arg constructors. The diagnostic reports this observation separately from the PASS matrix.

Prioritize that constructor/value divergence and time/mask conversion/clearing before lifecycle risks. Next lifecycle candidates are LOV listener replacement, repeated custom-implementation listeners and direct skin listeners surviving dispose. Shared DecimalFormat/cache concerns require workload/threading evidence; no performance or cosmetic fixes are proposed. GUI-013 should independently test both time fields and formatted masks through real clear/delete, invalid/partial text, paste, programmatic assignment, focus loss and seconds toggling, with uncaught FX exception capture. Keep that separate from numeric contract decisions.

Full PowerShell launcher with existing custom settings completed Maven -Pbuild compile and dependency resolution, then printed GUI-012 PASS with exit 0. JDK 25 javac for all GUI sources, PowerShell parsing and all eight dispatch modes, and staged whitespace checks passed. The probe distinguishes RED invariants from execution/setup failures. Physical input, actual LOV providers, skin replacement and application validation dialogs were outside this run. No production changes.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TextFields -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply GUI-012A

**Status** DONE. Fast-forward from `2077722f0fb27b807dfa9bbac4bf9f43cf1d3dbc` to `0ee740be4ff8a314a0a3b08f280b0bbda3cde770` on `migration/java25-correctness`, published atomically with this reply and verified via `git ls-remote`. [Exact source and regression diff](https://github.com/ssu95a/fore25/commit/0ee740be4ff8a314a0a3b08f280b0bbda3cde770)

**Contract and usage.** Source search of production Java, XML and FXML found no live String-constructor caller or FXML declaration for these numeric classes. Controls creates Integer, Long and BigDecimal via no-arg constructors; Part_FindById uses no-arg Long; the Dataset aggregators instantiate no-arg JInvNumberField. Runtime behavior in downstream callers remains UNKNOWN. String construction uses the initial-text superclass overload, while typed overloads explicitly set the typed property. Neither the constructor declarations nor repository callers establish a display-only numeric mode. The same text entered after construction parses immediately, and merely leaving a nonempty String-constructed field previously erased the text. Treat valid initial numeric text as input to the existing typed conversion, consistent with runtime edits. Preserve the literal entered spelling until normal focus formatting. Null and empty remain null/NULL; malformed text remains visible with ERROR and no typed number.

**Fix.** Reuse the same text-to-value conversion routine from the text listener and constructor initialization. It keeps the existing guards, converter lookup, error handling, value property, and rounding. Generic JInvNumberField uses TypeStringConverter(Number.class); Integer and Long use their existing fixed converters and nullable properties. Decimal and Money continue to use their existing converters and AbstractBigDecimalField's scale/HALF_UP property; Percent continues to supply its own default scale through the decimal hierarchy. The initializer is package-private and each built-in concrete String constructor calls it only for its exact runtime class. Thus an ancestor cannot trigger a derived converter, value setter or scale override before the descendant is initialized, especially BigDecimal -> Percent. Unknown downstream subclasses keep their legacy initialization behavior. The pre-existing valueProperty lookup used for listener registration is unchanged. No text toggling and no new public/protected signatures.

**Local RED/GREEN on interactive Windows JDK 25.0.1 / JavaFX 25.** On baseline, all five leaf String constructors with 42 displayed 42 with null/NULL, then lost all text after untouched focus loss. Invalid text stayed in NULL. The final harness was also run against the six recompiled baseline classes and printed GUI-012 RED (exit 1), including generic Number mismatch. On GREEN the same harness reported 42 or 42.00 with VALUE at construction and after untouched focus, with zero value notifications. Null/empty and invalid cases passed for all five fields; the three decimal leaves rounded 42.555 to 42.56. Typed Integer/Long/BigDecimal/Percent constructors and the existing GUI-012 live numeric and base field matrix passed. The generic Number initial value matched the existing edit path. Full PowerShell launcher with custom settings completed Maven -Pbuild compile and dependency classpath resolution, then printed GUI-012 PASS and exited 0. javap -protected output was identical for all six production classes. Staged whitespace checks passed; the runner and other modes were untouched. This establishes local behavior with programmatic focus and input, while a separate user rerun, downstream subclasses and domain persistence remain unverified.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TextFields -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply VF-001

**Status** DONE. Fast-forward from `0ee740be4ff8a314a0a3b08f280b0bbda3cde770` to `44b7fed91ebe394cc691f553d74847df043c7c54` on `migration/java25-correctness`, published atomically with this reply and verified using `git ls-remote`. [Exact production and regression diff](https://github.com/ssu95a/fore25/commit/44b7fed91ebe394cc691f553d74847df043c7c54)

**Typed data and validation contract.** Binder.bindValueControl binds the field's typed valueProperty bidirectionally to the FXEntity property. Controls.getValue returns that typed value for JInvValueField. The visible VF-001 probe uses the same JavaFX bindBidirectional API with SimpleObjectProperty as an in-memory model surrogate, and does not exercise Binder, FXEntity or an actual form. The actual standard OK source path in VM_INS/VM_EDIT/VM_NONE installs the StateValidator via configControls, calls ValidMan.validate in internalOnClose before onOK, and consumes the close request on failure. ValidMan runs the state gate before the other validators when validation proceeds. StateValidator rejects an ERROR control when it is not read-only. This is source evidence only. Actual custom actions, validators and business persistence remain UNKNOWN.

**Interactive RED/GREEN, Windows Java 25.0.1 / JavaFX 25.** Same visible Stage and production skin with JInvIntegerField and JInvMoneyField before and after. Initial state was null/NULL. Model assignment updated both sides; valid editing, invalid draft, recovery, clear-to-null and second recovery ran in sequence. In ERROR, the visible invalid draft kept the last valid field and model value and made no typed change. Per field, the pre-focus sequence had five exact field/model transitions and six text notifications. Valid focus loss made zero typed/model changes and at most one text reformat. No callback storm observed.

| Returned ClipboardContent | RED | GREEN |
|---|---|---|
| Integer full ERROR, visible bad integer | 42 | bad integer |
| Money full ERROR, visible bad money | 12346.70 | bad money |
| Integer full valid | 42 | 42 |
| Money full valid, visible 12 346,70 | 12346.70 | 12346.70 |
| Money grouped partial, selected 12 346 | 12346 | 12346 |
| Both ERROR partial, selected bad | bad | bad |
| Both empty and cleared | no string | no string |

The RED run exited 1 only on the invalid full-copy checks. The GREEN run printed VF-001 PASS and exited 0. Tests invoked actual getClipboardContent directly. The OS clipboard and physical input were outside the matrix. The correction is confined to JInvValueField.getClipboardContent: on full nonempty ERROR selection it returns the exact selected visible text. Full valid copies still use the typed value when nonnull. Partial copies still remove spaces. The numeric synchronization and standard validation code were untouched. No public/protected API change.

**Source-only lifecycle findings.** JInvTextField.setLOV sets the new reference, invokes onSetLov and adds a new anonymous listener to that LOV's valueProperty. Neither replacement nor unbindLOV detaches the old listener; unbind clears the reference and removes the button. The listener calls setTextValueFromLov with a lookup of the current getLOV, so stale notifications could act on the replacement or a null reference. Repeated same-instance registration can attach multiple listeners. JInvCustomValueField.setCustomImpl updates the current implementation used by valueProperty and adds a listener that captures the passed implementation. Prior listeners can still invoke replaced implementations; reattaching the same implementation can duplicate the callback. Replacement of a bound implementation may also strand the original property binding, pending proof. For VF-002, use a supported real no-DB LOV provider if available, exercise A -> B -> unbind and repeated same-provider install with callback counts, then independently test custom implementation replacement, clearing and property identity. No lifecycle production changes in VF-001.

Maven -Pbuild compile and dependency classpath generation succeeded with the existing custom settings for both runs. All GUI sources compiled with JDK 25. PowerShell parser and nine dispatch modes passed. Staged whitespace checks passed.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ValueFields -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply VF-002

**Status** Custom listener ownership FIXED and verified. Dynamic bound-property replacement needs a contract decision. LOV read-only, standalone runtime BLOCKED. Test/fix fast-forward from `44b7fed91ebe394cc691f553d74847df043c7c54` to `e5e9032d3d455efb92f7cfabda0cdb4fbee86648` on `migration/java25-correctness`, published atomically with this reply and verified via `git ls-remote`. [Exact fix and regression diff](https://github.com/ssu95a/fore25/commit/e5e9032d3d455efb92f7cfabda0cdb4fbee86648)

### Custom implementation

Repository Java and FXML search found no production callers of setCustomImpl and no dynamic replacement contract. The existing setter and delegated property support exercising implementation replacement and text callbacks in a standalone test. External callers and a promise of transparent migration of existing bindings are UNKNOWN. Since the field follows only one implementation for its value and type, letting retired implementations process current text is inconsistent with that ownership. Each setter call formerly added an anonymous listener capturing that implementation and never removed it. The narrow fix stores a single removable listener, detaches it before switching, uses only the active implementation, skips identity-repeat attachment and detaches on null. Installation still does not process existing text. On detach it leaves state alone and does not define typed access without an implementation. Property delegation, public and protected declarations, and JInvValueField clipboard/typed semantics are unchanged.

### Exact RED/GREEN evidence on Windows Java 25.0.1 / JavaFX 25

The visible real JInvCustomValueField used production skin and CSS. Counters A and B implement the real ICustom interface using distinct JavaFX properties. Initial text was `initial`, both callback counts were zero, state NULL and field/model null. A's original property was bound bidirectionally to an in-memory model. A's callback returns VALUE, B's ERROR, making the active state visible in the trace.

| Action | RED callbacks | GREEN callbacks | GREEN field / state / model |
|---|---|---|---|
| A, edit one | A:one | A:one | A:one / VALUE / A:one |
| Replace with B, edit two | A:two, B:two | B:two | B:two / ERROR / A:one |
| Model writes model-only with B active | no additional text callback | no additional text callback | B:two / ERROR / model-only |
| Install same B, edit three | A:three, B:three, B:three | B:three | B:three / ERROR / model-only |
| Reinstall A, edit four | A:four, B:four, B:four, A:four | A:four | A:four / VALUE / A:four |
| Clear implementation, edit detached | A:detached, B:detached, B:detached, A:detached | none | no active typed read / VALUE / A:four |

RED exited 1. On RED, obsolete A wrote A:two and later A:three into the original bound model even while B owned the field; after detach, obsolete A wrote A:detached. GREEN printed VF-002 PASS (custom listener ownership) and exited 0. No FX exception was observed. In both versions, replacement exposed B's property and the model continued to bind to A's original property. Reinstalling A re-exposed that exact original property. On GREEN, model-only stayed on A while B's edits updated B only; A reinstallation recovered the model-only value before the next edit. Transparent replacement under an existing binding remains BLOCKED as a contract decision. Choose whether to require callers to unbind/rebind explicitly or design a stable field-owned property with defined transfer, rebinding and null semantics. There is no silent binding transfer in this fix.

### LOV source audit and blocker

`AbstractLovBase` initialization dereferences `BaseApp.APP().getViewPrefService().getLovSmallCountRows()` and catches AppException only. A standalone no-BaseApp provider subclass would therefore fail superclass initialization with a null dereference. An in-memory value property alone cannot reach the real LOV API. No fake BaseApp or bootstrap mutation was used. Source confirms that every `JInvTextField.setLOV` registration installs a new listener, replacement drops the reference without removing the old listener, and `unbindLOV` clears the reference and button only. The old provider property holds its listener, which captures the field and resolves current `getLOV()`; repeated same-provider calls also register multiple callbacks. Stale update, duplicate invocation and post-unbind exception are source-level candidates. Actual counts, field mutations, exception and reachability were not measured. Binder and form/filter call sites use setLOV for configured providers; none of the reviewed usages establishes same-instance reinstallation as a promised feature. A dedicated test in an authorized real BaseApp context should cover A -> B -> B -> unbind with a real no-query provider and separate callback, binding and provider ownership diagnostics before a fix. No LOV code changed.

### Verification

Maven -Pbuild compile and dependency classpath generation succeeded via the existing settings. Local VF-002 RED/GREEN and post-fix VF-001 PASS used actual interactive JavaFX 25. JDK 25 compiled all GUI sources; PowerShell parser and all ten dispatch modes passed. Staged whitespace checks passed. Full form, SQL and physical input remain outside this matrix.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ValueLifecycle -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply VF-003

**Status** Read-only architecture investigation complete. Design approval pending. Working branch and remote HEAD remain `e5e9032d3d455efb92f7cfabda0cdb4fbee86648`. Only this mailbox reply was published.

### Evidence and current contract

A repository-wide usage search, including Java and FXML, found no production installation of ICustom, no production implementation or FXML declaration of this field, and no advertised replacement contract. The only executable usage found is the VF-002 test. External consumers may call the public raw setter or depend on property-object identity, so their behavior is UNKNOWN. [Field and ICustom](https://github.com/ssu95a/fore25/blob/e5e9032d3d455efb92f7cfabda0cdb4fbee86648/src/main/java/ru/inversion/fx/form/controls/JInvCustomValueField.java#L12) [VF-002 sequence](https://github.com/ssu95a/fore25/blob/e5e9032d3d455efb92f7cfabda0cdb4fbee86648/src/test/probes/gui/LegacyValueLifecycleSmoke.java#L95)

`valueProperty()` and `getClassValue()` delegate to the current implementation. The generic method and raw setter do not enforce a stable concrete runtime type or stable Property object across installations. `JInvValueField.getValue()/setValue()` consult the property at call time. Controls reads that typed value and writes by converting via the current type and setting the current property. The separate editor-string path reads displayed text. `Binder.bindValueControl()` instead calls `bindBidirectional` on the Property instance returned at bind time. JavaFX connects those objects; a later accessor returning another object cannot redirect the link. Binder also configures LOV and validation for the control at bind time. Native bidirectional relationships cannot be reliably detected or enumerated via Property.isBound(), which reports unidirectional binding. [JInvValueField](https://github.com/ssu95a/fore25/blob/e5e9032d3d455efb92f7cfabda0cdb4fbee86648/src/main/java/ru/inversion/fx/form/controls/JInvValueField.java#L40) [Controls set](https://github.com/ssu95a/fore25/blob/e5e9032d3d455efb92f7cfabda0cdb4fbee86648/src/main/java/ru/inversion/fx/form/controls/Controls.java#L112) [Controls get](https://github.com/ssu95a/fore25/blob/e5e9032d3d455efb92f7cfabda0cdb4fbee86648/src/main/java/ru/inversion/fx/form/controls/Controls.java#L159) [Binder](https://github.com/ssu95a/fore25/blob/e5e9032d3d455efb92f7cfabda0cdb4fbee86648/src/main/java/ru/inversion/fx/form/Binder.java#L101)

### User-confirmed runtime, VF-002 on Java 25 / JavaFX 25

| Phase | Display and state | Current typed property | Model bound at first setup |
|---|---|---|---|
| Install A on existing text | initial / NULL, no text callback | A, null | null on A |
| Edit one with A | one / VALUE | A, A:one | A:one |
| Install B, edit two | two / ERROR | B, B:two | A:one on A |
| Model writes model-only; repeat B then edit three | three / ERROR, one B callback on edit | B, B:three | model-only on A |
| Restore A, before editing | three / ERROR | original A, model-only | still model-only on A |
| Edit four then detach with null and edit detached | detached / VALUE, zero callbacks after detach | no implementation | A:four on original A |

A's original Property remains valid and bound, but is not the field property while B is installed. Runtime example: after B:two, a direct Controls.getValue read would see B:two by source, while a business-model read of the original binding sees A:one. VF-002 simulated the bind call with a JavaFX property, not a business form or persistence. After null, source guarantees that typed access via valueProperty/getValue/getClassValue or Controls conversion dereferences a missing implementation. The probe deliberately only observed text, the last state and the old model in that phase. Neither persistence impact nor a downstream broken caller is proven.

### Options and recommendation

| Contract | Correctness and compatibility |
|---|---|
| Configure once before any typed access or Binder call | Keeps the implementation-owned Property and its identity. Recommend treating the same instance as a no-op, null before configuration as still unconfigured, and rejecting a different instance or null after configuration before changing the listener, state or value. Typed access while unconfigured should fail explicitly, rather than reporting a false valid value. Requires ICustom to return the same nonnull Property and fixed compatible type for its configured lifetime. Preserves signatures and direct property ownership, but callers that legitimately replace or detach an already configured implementation would see a new failure. This cannot safely be limited to detected bound fields. |
| Stable field-owned Property | Binder and external listeners keep one object while implementations change. Changes the published property identity and any consumer assumption of getting the implementation's own property or subtype. Before switching, choose authoritative value (preserve field/model value versus adopt the replacement's value), validate the type and writability and define null/detach; do not rely on bindBidirectional's initial synchronization direction. Detach old listeners, install the new delegate in defined order, suppress feedback and reentrant overwrites, and leave the old configuration intact if transfer fails. Optional unconfigured phase, binding before configuration and text/state synchronization need explicit contracts. Useful when live replacement is a real requirement, with the broadest compatibility surface. |
| Caller-managed unbind/rebind | Retains current exposed property identity per implementation. Caller must retain and unbind the old Property, transfer the chosen authoritative value and reconnect the replacement to the model and dependent observers or validators. Existing Binder supplies no general automatic replacement protocol. Third-party bindings may be invisible, which makes missed rebinding a silent integrity risk. Could work inside a single audited owner transaction, but is unsuitable as a general field guarantee. |

**Recommendation.** Approve a configure-once field with fail-fast replacement and detach after configuration. The single design decision is whether applications must support swapping an implementation on an already configured/bound field. No repository usage requires it. If such a requirement is confirmed, use the stable field-owned property design with an explicit transfer policy instead of claiming the existing API can migrate bindings transparently.

**State is a separate axis.** Reinstalling A with B's ERROR and detaching with retained VALUE are confirmed stale states relative to the active implementation. State is only the last result returned by ICustom.setText and is independent of typed property changes; the current API has no side-effect-free validator or formatter from which to infer a new state. Setting NULL/VALUE from the typed value would conflate invalid draft and null model; calling setText during install could mutate an already-bound model. Under configure-once, rejecting the transitions before effects removes both replacement scenarios without assigning invented states. Initial nonempty text still has no defined replay policy. If replacement is required, specify a policy for text, typed value and validation state on replace/detach before implementation. Do not opportunistically reset ERROR or VALUE.

**Smallest follow-up after approval.** For configure-once, add a focused regression for first configuration, idempotent same-instance assignment, stable Property and model on rejected B/null, unconfigured access and unchanged state/text on rejection. Keep the original VF-002 evidence as the pre-contract baseline and make any future probe changes explicit because its current accepted replacement path cannot also assert rejection. Preserve VF-001 and retain active-only listener coverage. No new runtime was executed in VF-003. This report uses the user's JavaFX 25 result plus source, usage and Git checks. No implementation, tests or application configuration changed.



---

## Reply NUM-001

**Status** Test-only investigation complete, **RED reproduced**. Fast-forward from `e5e9032d3d455efb92f7cfabda0cdb4fbee86648` to `4c75d9943927aebe9c91e1ec235afce1c6a63f7d` on `migration/java25-correctness` was published atomically with this reply and verified via `git ls-remote`. [Exact test/runner diff](https://github.com/ssu95a/fore25/commit/4c75d9943927aebe9c91e1ec235afce1c6a63f7d)

### Contracts and usage

- `MoneyStringConverter` parses to raw BigDecimal using one optional leading sign, separators, spaces/NBSP and several shorthand suffixes. Its unused `round(new MathContext(...))` result does not affect that number. MathContext precision refers to significant digits, so assigning that result would create unrelated precision loss. Display formatting uses the public static mutable money formatter with its own rounding mode. Typed setting and accepted edits of both fields pass through AbstractBigDecimalField's property setter, which uses current scale and HALF_UP. This is separate from display formatting. Constructor paths remain consistent with GUI-012A.
- `JInvMoneyField` uses a static shared converter and static formatter. TableColumnMoney also returns that same public formatter, and the decimal TableColumn and tree column use its scale for defaults. The production settings setter has no in-repo invocation outside Money's static initialization found by search. `Controls` constructs JInvBigDecimalField with no args. No FXML declarations of these three field classes were found. Percent inherits decimal parsing and formatting with its own static default scale until its instance fraction property is created. No supported off-FX call path was established. External corporate access to the public formatter and converters is UNKNOWN.
- `JInvBigDecimalField` uses a lazily created inner converter, accepts commas as decimal points and strips regex whitespace. It looks up a mutable DecimalFormat by fraction digits and grouping and retains a strong reference while in use. Changing grouping or scale changes that field's formatter reference. This does not itself assign the existing typed value. The weak-cache key omits FORMAT locale and other symbol configuration; locale reuse across an existing key is a source-level concern, not stress-tested. Percent depends on this same mechanism; no new Percent runtime matrix in NUM-001.

### Local interactive Java 25.0.1 / JavaFX 25

| Scenario | Result |
|---|---|
| Direct parser exact decimal input | CONFIRMED 1.005, -1.005, zero, space/NBSP grouping, comma/dot and long precision preserved. The default formatter displayed 1.00 and -1.00 for the half ties. |
| Shorthand | OBSERVED +1.25K -> 1250.00, -2T -> -2000, 3M -> 3000000 and analogous Cyrillic suffixes. 12K34 -> 12034 and K -> 0 observed; intended shorthand grammar beyond the tested examples remains UNKNOWN. |
| Invalid direct input | CONFIRMED abc and 1.2,3 rejected. RED bare + and - both returned zero without any digits. |
| Typed assignment and edit | CONFIRMED both fields held 1.01 after typed 1.005 and -1.01 after edit -1.005. Long integral digits remained exact with HALF_UP to scale 2. Malformed 1.2,3 produced ERROR with zero typed notifications and kept the prior amount. Typed null and valid recovery passed. |
| Money draft after a valid amount | RED replacing 12.34 with a bare - produced text -, value 0.00, state VALUE, one text and one typed notification. The draft was accepted as a number and overwrote the previous value on the ordinary edit listener path. |
| Focus and settings | CONFIRMED both fields formatted on focus loss with zero typed notifications. Changing decimal grouping/scale left current text and typed value unchanged and the peer retained its formatter. After lowering scale 3 to 2, existing 1.245 stayed typed as 1.245 while the new converter display was 1.24. Whether live settings changes should rescale or refresh immediately is UNKNOWN. Money formatter replacement likewise left existing text/value alone; a new typed assignment used scale 3. |
| Money and decimal locale/grouping | CONFIRMED default spaced roundtrip, comma decimal at scale 3, and localized ar-EG digits under restored FORMAT configuration for both fields. RED in US FORMAT locale with money grouping=false: the formatter still emitted 12,345.67, which its own parser rejected at the second separator. Source sets grouping symbols when enabled but never calls setGroupingUsed with the flag. |

### Retained allocations and limits

JInvTextField skin, case formatter, text/focus/selection listeners and common control properties are baseline costs. Incrementally, JInvNumberField allocates a default TypeStringConverter and installs one text, one focus and one typed-value listener per field. Both decimal leaves override converter selection, so the inherited per-instance converter is still retained but neither leaf uses it. AbstractBigDecimalField allocates a distinct rounding ObjectProperty per field on listener registration; the inherited JInvNumberField value-property slot is bypassed. JInvBigDecimalField adds a formatter reference, one lazy nonstatic converter when exercised, and a lazy fraction-digits property/listener when configured. Money keeps the actual converter and DecimalFormat static. Parsing also has temporary per-miss allocations (builders, token list and arithmetic), outside the retained-field inventory.

Measured after warmup on the FX thread, read-only identity inspection in 100 decimal and 100 money objects held strongly through the count. All 100 freshly constructed decimal fields had null converter and formatter slots. After assigning a value they held 100 distinct converters sharing one formatter for their common configuration. Both batches had 100 distinct typed properties and 100 inherited default converter objects; Money's actual formatter/converter remained 1/1 static. The memory batch measures identity, not byte size or attached-scene layout costs. **Retained bytes UNKNOWN.**

The Money cache is access-order, strong and shared, with a bound of 250 pairs, not a per-field cost. A run of 300 more distinct strings left 250 entries and evicted the older sentinel. The same raw parse object was reused across global formatter replacement. That agrees with current exact parsing and unused rounding; settings-independent raw parsing did not cause incorrect output in the tested cache case. Hit-rate and allocation benefit beyond observed identity reuse UNKNOWN. Decimal weak slots/live were 5/5 in this run. A live field retains a strong reference; a format can be recreated after its final strong owner disappears and GC clears the weak referent. The source loop records a null slot using an index advanced only for live entries, so after dead entries it can replace the wrong slot and does not prune all dead wrappers. Longer growth or a leak was not established. No GC forcing, memory optimization or concurrency stress.

**One next correctness fix.** Reject nonblank sign-only input in MoneyStringConverter before it can yield zero, so the existing field ERROR path retains the previous typed amount. Use the same RED probe for GREEN; retain zero, signed valid amounts and verified shorthand grammar. Keep the independent grouping=false finding separate.

**Verification.** Full PowerShell/Maven launcher with existing custom settings completed BUILD SUCCESS and the visible JavaFX 25 probe exited 1 with four expected RED assertions (two signs, one field overwrite, one grouping roundtrip). JDK 25 compiled all GUI sources, PowerShell parsing and eleven dispatch modes passed, and staged whitespace checks passed. Tests used programmatic input/focus and no DB. Production remained unchanged. GUI-013 was stopped; its local draft and logs were retained under ignored target paths and were excluded from this commit.

```powershell
git pull --ff-only
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply NUM-002

**Status** DONE. Focused sign regression GREEN. Full MoneyDecimal suite has one independently known RED. Fast-forward from `4c75d9943927aebe9c91e1ec235afce1c6a63f7d` to `13b60790a434a9d261dde3392342d6184823ac6b` on `migration/java25-correctness` was published atomically with this reply and verified via `git ls-remote`. [Exact production and test diff](https://github.com/ssu95a/fore25/commit/13b60790a434a9d261dde3392342d6184823ac6b)

**Fix.** Move the existing leading space/sign scan before the parse cache lookup. If only the sign and parser-tolerated spaces remain, throw NumberFormatException without passing through the cache or numeric accumulation. Space classification includes NBSP via Character.isSpaceChar. The existing trim/empty return stays in place. Numeric values, suffix grammar, scale, rounding, cache policy, formatter and public/protected declarations retain their existing behavior. The field's existing conversion-exception path sets ERROR and keeps the last valid typed value.

**Windows Java 25.0.1 / JavaFX 25 RED/GREEN.** Same strengthened visible MoneyDecimal probe against baseline and fixed Maven builds. Before the fix, all eight direct sign cases (bare signs, ordinary spaces, NBSP, mixed surroundings) returned zero. Both sign-only field edits overwrote the previous amount with 0.00/ VALUE and emitted one typed event. Baseline also retained the grouping check and printed NUM-001 RED findings=11 (exit 1).

- Focused GREEN. All eight direct inputs threw NumberFormatException. Editing 12.34 to minus left visible minus, typed 12.34 and ERROR. After recovery to -12.35, editing to plus left visible plus, typed -12.35 and ERROR. Each invalid edit emitted one text event and zero typed events. Recovery via -12.35 and +12.36 gave VALUE and one typed event each. Real signed and unsigned zero, signed numeric amounts, signed K/T/M samples, long precision and prior decimal assertions passed. Observed K -> 0 and 12K34 -> 12034 were unchanged.
- Remaining grouping RED. US FORMAT locale with grouping=false emits 12,345.67 and its own parser rejects it. The full runner printed NUM-001 RED findings=1; PowerShell surfaces the nonzero exit. BUILD SUCCESS and the GUI trace confirm actual execution.
- UNKNOWN. Physical keyboard and real persistence paths were outside this test. User rerun of this commit is pending.

Maven -Pbuild compile, dependency classpath generation and probe javac passed with existing custom settings. PowerShell parser and staged whitespace checks passed. The diff contains only MoneyStringConverter.java and LegacyMoneyDecimalSmoke.java.

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply NUM-003

**Status** DONE / GREEN. Fast-forward from `13b60790a434a9d261dde3392342d6184823ac6b` to `2f88c30e8b46f0eeb51433265b1d9faaaf2d2a54` on `migration/java25-correctness`. Published atomically with this reply and verified via `git ls-remote`. [Exact source and regression diff](https://github.com/ssu95a/fore25/commit/2f88c30e8b46f0eeb51433265b1d9faaaf2d2a54)

**Cause and fix.** The shared DecimalFormat inherited the FORMAT locale's default grouping flag because the factory changed the grouping symbol and size without applying the boolean argument. One call to `setGroupingUsed(grouping)` in `JInvMoneyField.initDefaultMoneyFormatter()` now applies the setting. Enabled grouping retains the configured space separator and group size three. Decimal separators, fraction digits, HALF_UP value assignment, display rounding, public/protected API, and parser grammar/cache are unchanged.

**Local RED/GREEN on interactive Windows Java 25.0.1 / JavaFX 25.** The same strengthened smoke was used for both builds. RED found eight display/roundtrip assertion failures for disabled grouping (dot/comma, plus/minus), e.g. `12,345.67` and `-12,345,67`; the grouped cases and NUM-002 stayed green. After the fix, all eight US FORMAT combinations of sign, separator and grouping produced exact display strings and parsed to the original signed BigDecimal of scale 2:

| Decimal separator | Grouping disabled | Grouping enabled |
|---|---|---|
| Dot | `12345.67`, `-12345.67` | `12 345.67`, `-12 345.67` |
| Comma | `12345,67`, `-12345,67` | `12 345,67`, `-12 345,67` |

At each formatter replacement, the already existing money field retained typed `71.005` and displayed `71,005`, with zero text and value notifications. The direct formatter output reflects the selected scale/display rules while the stored property stays intact. Existing scale/rounding, long precision, shorthand, grouping, ar-EG samples, BigDecimalField and sign-only regressions passed. Locale and original formatter are restored by the existing teardown. Results for other locales, physical input and actual persistence remain UNKNOWN.

Maven -Pbuild compile, dependency lookup and probe javac passed with the existing settings. PowerShell parse and staged whitespace checks passed. The full local run produced `NUM-001 PASS (tested invariants)` and exit 0. Only the money formatter and its focused test changed.

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
```



---

## Reply NUM-004

**Status** DONE / GREEN. Fast-forward from `2f88c30e8b46f0eeb51433265b1d9faaaf2d2a54` to `ca05ae3c991973f48269542309041fd3eaf426ec` on `migration/java25-correctness`, published atomically with this reply and verified via `git ls-remote`. [Exact source and regression diff](https://github.com/ssu95a/fore25/commit/ca05ae3c991973f48269542309041fd3eaf426ec)

**Compatibility.** TypeStringConverter stores only a final target Class and delegates parsing and display conversion to TypeConverter. No repository consumers of distinct default object identity or mutable converter state were found. The two Dataset aggregate controls create ordinary JInvNumberField instances and replace their converters using setConverter with the existing Long or Money converter. Integer, Long, Money and BigDecimal override converter selection; Percent inherits the BigDecimal path. The fix assigns one private static final default to the existing per-field slot. The setter still replaces only that field's slot, with no change to signatures, override dispatch, conversion/rounding, constructor order or listeners. External reliance on protected default-converter identity remains UNKNOWN.

**Local visible Windows Java 25.0.1 / JavaFX 25.** The same strengthened MoneyDecimal test before and after the change showed:

| Retained identity in the live batch | Baseline | Optimized |
|---|---:|---:|
| Inherited defaults in 100 used BigDecimal fields | 100 | 1 |
| Inherited defaults in 100 used Money fields | 100 | 1, same object as BigDecimal batch |
| Defaults in two ordinary Number fields | 2 | 1, same object as both batches |
| Actual BigDecimal converter objects | 100 | 100 |
| BigDecimal/Money typed properties | 100/100 | 100/100 |
| Actual Money converter | 1 | 1 |

Reflection read the retained references and verified actual Money converter dispatch for all Money fields. Constructor conversion on the two ordinary fields still yielded 7; a custom converter on the first parsed custom:21 and rendered custom:22, while the peer parsed 21 and rendered 22 using its original default. Baseline exited 1 only on the two new default-identity assertions. The optimized full run printed NUM-001 PASS (tested invariants) and exited 0. Existing grouping, sign-only, rounding, precision, locale and event assertions passed. Measured object identities, not retained bytes. Total retained bytes remain UNKNOWN.

Maven -Pbuild compile, dependency classpath generation, probe javac and PowerShell parsing passed. Staged whitespace checks passed. Only JInvNumberField and the MoneyDecimal probe changed. No other memory tuning.

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -MoneyDecimal -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```



---

## Reply NUM-005

**Status** Read-only investigation DONE. Recommendation: **retain the lazy per-instance JInvBigDecimalConverter**. No compatible reduction in retained converter objects was established. Working branch and verified remote SHA remain `ca05ae3c991973f48269542309041fd3eaf426ec`. Only this mailbox reply was published.

### Retention and usage

| Layer | Incremental retained objects or references |
|---|---|
| JInvTextField / JavaFX | Control state, skin and its nodes, base formatter and text/focus/selection infrastructure belong to the shared control baseline. |
| JInvNumberField | References the shared immutable NUM-004 default unless a different converter is assigned. The field already supplies text, focus and typed-value listener registrations. Both decimal and Money override converter selection. TypeStringConverter owns only a final Class and delegates conversion. |
| AbstractBigDecimalField | One rounding ObjectProperty per used field, initialized by virtual valueProperty lookup when the base registers its listener. The shadowed parent slot adds no second allocated property. The setter applies current scale with HALF_UP; formatting is separate. |
| JInvBigDecimalField | One lazy, stable nonstatic converter per field after getConverter is invoked. Its enclosing-instance reference provides the current formatter. Each field retains a formatter reference; the actual mutable DecimalFormat is shared by matching settings. Explicit fraction property access allocates an owner-specific IntegerPropertyBase with one change listener. Changing grouping does not need that property. |
| JInvPercentField | Inherits the same converter and formatter; supplies its static default scale until its per-field fraction property is allocated. |
| JInvMoneyField | Actual converter is static because its formatter is global; a different ownership contract. |

Repository search found Controls constructing JInvBigDecimalField with no arguments, Percent as the in-repo subclass, and no FXML declarations or production caller retaining this field's public getConverter found. The public API still exposes a retained converter handle; external users and subclass expectations are UNKNOWN. JInvNumberField uses virtual getConverter for parse, typed display and focus normalization. The converter parses without owner settings, but its public toString calls that owner's formatter on each call. The legacy setConverter slot is separate from the overriding decimal getter. The converter/field back-reference is not by itself a leak; an external holder of the converter could extend the lifetime of its owning field. No such retention was demonstrated. [Decimal field](https://github.com/ssu95a/fore25/blob/ca05ae3c991973f48269542309041fd3eaf426ec/src/main/java/ru/inversion/fx/form/controls/JInvBigDecimalField.java#L27) [Numeric base](https://github.com/ssu95a/fore25/blob/ca05ae3c991973f48269542309041fd3eaf426ec/src/main/java/ru/inversion/fx/form/controls/JInvNumberField.java#L17) [Percent](https://github.com/ssu95a/fore25/blob/ca05ae3c991973f48269542309041fd3eaf426ec/src/main/java/ru/inversion/fx/form/controls/JInvPercentField.java#L9)

### Evidence on visible Java 25 / JavaFX 25

A temporary untracked diagnostic under target used production controls, skin and CSS on one Stage. With US FORMAT and argument 12345.6789, A at scale 2 ungrouped returned 12345.68; B at scale 3 grouped returned 12 345.679. After changing A to scale 4 grouped, the **same retained converter handle** returned 12 345.6789, and B's handle still returned 12 345.679. A kept its original property, typed value and displayed text at 12345.68 through the settings change. Percent with temporary default scale 4 rendered 12345.6789 and the same handle rendered 12345.679 after explicit scale 3. The FORMAT locale and Percent default were restored in the isolated process. Probe PASS, javac PASS. The accepted MoneyDecimal NUM-004 run already counted 100 actual converters and one formatter for 100 used Decimal fields, with one inherited default across both batches. No new heap measurement. Retained bytes UNKNOWN; object counts do not imply a byte saving, and allocation sampling would not establish retained size.

### Weak DecimalFormat cache

**Demonstrated:** while B held the US scale-3 grouped formatter, switching FORMAT to ar-EG and configuring a new field with the same key reused ASCII digits (first U+0031). A fresh scale-7 key used Arabic-Indic digits (first U+0661), and the localized sample parsed to the expected BigDecimal. This demonstrates locale-dependent reuse after an explicit in-process locale switch. Whether applications require live FORMAT switching or that display behavior reaches users is UNKNOWN. Current fields also hold their selected formatters directly. Sharing formatters is distinct from sharing converters.

**Rejected by source inspection:** the proposed incorrect replacement index for the *first* cleared weak reference. The loop advances i for live entries only, but before the first dead slot all preceding entries are live, so i equals the actual list index when nullIndex is initially captured. Later dead entries do not replace nullIndex. For example, [live, dead, live, dead] records index 1 correctly. A matching live entry returns without pruning dead wrappers, and a miss reuses a single dead slot or appends when none is found. Cache size after workload churn and GC is UNKNOWN. No forced GC, leak conclusion or cache change.

### Alternatives and recommendation

- **Keep current lazy per-owner adapter.** Preserves stable handle identity, current settings, overridable scale and Percent behavior. One object per used field; unused fields retain no allocated adapter.
- **Static nested adapter with explicit owner or supplier.** Still needs one adapter and a reference to owner-dependent state per used field; a capturing supplier can add another allocation. No demonstrated saving.
- **Share immutable converters keyed by formatting configuration.** Could reduce adapter counts for common keys but changes previously obtained handles into settings snapshots, or changes identity on settings mutation. Both violate demonstrated live behavior. A mutable shared converter would cause cross-field settings interference. Per-owner delegation preserves behavior but reintroduces the per-owner object or additional tracking.
- **Split a shared parser from display.** Parsing is owner-independent already, but keeping a live public getConverter still requires the owner-aware adapter. Extracting parsing alone offers no retained-adapter reduction. A weak owner reference also changes escaped-handle behavior and adds an object.

Retain the current per-instance converter and existing formatter sharing. No production or tracked test change and no implementation-branch push. The verified dependency on live owner settings is stronger evidence than the speculative memory savings from converter sharing.



---

## Reply CTRL-001

**Status** DONE, read-only. Audited `Controls.java` at `ca05ae3c991973f48269542309041fd3eaf426ec`. Working branch and verified remote SHA remain at that commit. Only this mailbox reply was published. Local scratch diagnostics live under ignored `target/ctrl-001-local`; no tracked code/test change. [Audited source](https://github.com/ssu95a/fore25/blob/ca05ae3c991973f48269542309041fd3eaf426ec/src/main/java/ru/inversion/fx/form/controls/Controls.java)

### Public method map and production paths

| Family | Methods and representative consumers |
|---|---|
| Value and editor state | setValue/getValue/getStringValueFromEditor. DSControlAdapter and TSControlAdapter populate editors on row motion; DCFXAdapter reads IN and writes OUT through this helper; F7FilterDialog reads filter values and separately reads the editor-string surface for expression transfer; ValidMan and ReqValidator read values. Binder instead binds JInvValueField.valueProperty directly to FXEntity and configures validation, avoiding dispatch on every typed bind update. |
| Properties and metadata | setProperty/hasProperty/getProperty, field-name extraction and clear, validation and value-changed flags. Binder and AbstractBaseController discover fields; ValidMan validation focus paths and F7 consume flags. Metadata reads are weakly typed and can initialize JavaFX maps via getProperties; the observables have separate semantics. |
| Interactivity and scrolling | disableControl/enableControl, editableProperty/isEditable, scrollToNode/getRecursiveBoundsOfParent. JInvTextField/Calendar/ComboBox/ChoiceBox/Button and others route readOnly changes here. Validation uses ancestor traversal and scrolling. |
| Graph discovery and ownership | getParentStreamOfControl, getControllerProperty/getControllerFromControl, getDsAdapterFromControl, getControlList/getNodeList/getNodeByFieldName, non-control registration/getter. Controller association is a nearest marked Parent property, including self when it is a Parent; it does not follow an arbitrary detached popup or logical-owner pointer. Binder and AbstractBaseController use tree and non-control discovery; ValidViewDecorator and JInvKeyboardManager use ancestry. |
| Factories, column metadata and UI helper | getControlByClass, createColumnByClass, column field/title/filter and traversal methods, getSerialVersionUID, tooltip bundle and timing. F7FilterDialog and CalendarRangeDialog use typed controls, PropertySheet uses the numeric factory with a fallback, DSFXAdapter uses typed column creation and leaf walks, ViewPrefAppService persists table columns, and context menus/F7 request column titles. |

**Dispatch contract boundaries.** Null control means no-op/null in set/get. ICustomValueControl takes precedence and receives raw value; JInvRadioGroup has a writer-specific string conversion and a custom reader. JInvValueField writes convert with getClassValue and TypeConverter, while reads return the typed property regardless of ERROR. Generic text returns null for empty text; string results are also normalized at the getValue boundary. Plain CheckBox null writes false, whereas the string-specific checkbox has its own write/read path; editor-string view for a checkbox is its boolean text. Legacy ComboBox writes a selected value; the plain ComboBox accesses value; editor-string reads this committed value even on editable combos. A choice box and JInvTable have special read paths without matching generic write paths. Unsupported dispatch generally throws, with the DatePicker exception below. Custom setter side effects and external public consumers remain UNKNOWN. The probe confirmed typed 42 versus visible '-' in ERROR and an editable combo with value/Controls string 'committed' while the editor displayed 'draft'. These are distinct data surfaces. A custom action can read them, but this does not establish a save/validation bypass. The known standard OK state gate remains as documented in VF-001.

### Prioritized findings

| Rank / methods | Concrete evidence and consequence | Status |
|---|---|---|
| 1 / HIGH, setValue + getValue plain DatePicker | Direct set from 2025-01-02 to 2025-03-04 and null clear both returned normally while the picker retained 2025-01-02. Reproduced through actual `DSControlAdapter.bindControl(picker, null, row -> row)` and real ArrayDataSet navigation: mapping callback ran twice, dataset advanced to 2025-03-04, picker stayed 2025-01-02. The enclosing DatePicker branch accepts the type but writes only JInvCalendarTime/Calendar, while getter handles plain DatePicker. A stale date can survive row changes or clear. No in-repo plain DatePicker constructor/FXML use was found; external adapter usage and business persistence are unknown. | CONFIRMED |
| 2 / MED-HIGH, getNodeList | TabPane with empty tab and SplitPane with an empty ScrollPane produced two null entries, despite one real nested control visit. A normal predicate calling getId threw NPE. The sibling getControlList and getNodeByFieldName succeeded; their instanceof-based predicates tolerate null. AbstractBaseController and ViewPrefAppService consume getNodeList; many current predicates are null-safe, so current business failure coverage is UNKNOWN. | CONFIRMED |
| 3 / MED-HIGH, disableControl | TextInputControl with bound editable threw `A bound value cannot be set`. A bound-visible Button was left visible and not actually disabled, though the pseudo-class path still runs. Plain read-only text and hidden button became editable/visible after disable/enable, and ComboBox focusTraversable became true. Existing enable docs describe forcing these states, so automatic restoration of arbitrary user settings is not an established contract. Two disables then one enable blocked and then delivered one synthetic press (0/1), rejecting the duplicate-filter problem for that matrix. Overlapping independent restrictions and app-specific bindings remain UNKNOWN. | CONFIRMED behavior; ownership contract UNKNOWN |
| 4 / MED, getTitleFromTableColumn and clearPropertyNameInControl | Detached Legacy column threw NPE and one in a plain TableView threw ClassCastException before returning a valid header. Plain column with empty text and Label graphic returned the field ID. Filter/table menu calls exist, but tested malformed ownership is outside their normal attached Legacy table fixture. Null prompt clear threw NPE. Prompt field token fails explicitly by design and no active external caller of the private migration helper was found. | CONFIRMED concrete inputs; production exposure UNKNOWN |
| 5 / MED memory, isControlValueChanged and non-control getter | First boolean read changed fresh TextField from no property entries to retained SimpleBooleanProperty with the control as bean, and returned false. Validation/F7 call this predicate, and observer access needs a durable property; pure reads and property access are distinct. First non-control getter retained a mutable ArrayList under root properties; both reads returned the same list and registration mutated it. The getter source constructs a new empty ArrayList even on the existing-list path; this is a temporary allocation in source and actual optimized allocation rates are UNKNOWN. Read-only call sites and callers that legitimately register are both present. | CONFIRMED retained identities; SOURCE-PROVEN construction path |
| 6 / MED migration, setTooltipStartTiming | Exact JavaFX 25: classpath Tooltip reflection returned while both public showDelay values stayed 1000 ms; the source mutates a shared private behavior timeline, so hover timing/handler effects are unverified. A Tooltip subclass failed with NoSuchFieldException because getDeclaredField inspects runtime class. Named javafx.controls without opens failed with InaccessibleObjectException. Public Tooltip.setShowDelay(Duration) changed only the first tooltip to 123 ms independently in both runs. No in-repo consumer of the private-reflection helper found. | CONFIRMED, no module settings changed |
| 7 / LOW-MED, traversal and factories | Runtime found TableView all/leaf 2/1, nested TreeTable getAll 1 because it returns top-level only, and parent stream starts with self. Source visits wrapper before logical children in item/tab order, intentionally skips editor internals of calendar/spinner/Legacy table/text/editor composites and excludes ButtonBar/Panes from control results. There is no identity deduplication; distinct-node fixture had one visit. Nested tree preference consumers warrant a separate contract test before recursion changes. getControlByClass takes exact wrapper/temporal types and returns null for unsupported types; property-sheet fallback is deliberate. Controller lookup materializes the entire parent chain even on first match and getNodeByFieldName walks all matches to detect ambiguity. Column recursion collects current leaves, so caching without invalidation risks stale metadata. | Runtime examples + SOURCE-PROVEN allocation/order; duplicate identity and speed impact UNKNOWN |

Flags and non-control lists are per-node or per-container retained state. LinkedList element nodes, list/stream pipelines, reflection and eager ancestor lists are temporary work; no exact bytes, hot-path latency or leak was measured. Static consumeFilter and Tooltip behavior are shared, not per-control allocations. An external reference to metadata or a listener can affect lifetime, but no retained-owner leak was demonstrated. Non-control list mutability and dirty flag identity must be preserved for existing callers.

### One first targeted regression/fix

Fix only the silent plain DatePicker write branch in Controls.setValue. Use the existing direct and real DSControlAdapter/ArrayDataSet fixtures as RED/GREEN for new date and null clear; add sentinels for JInvCalendar LocalDate and JInvCalendarTime LocalDateTime (including time), no spurious notifications when assigning the same value, and the generic unsupported-type route. The expected narrow design is to retain the time-aware case first, then apply the existing LocalDate conversion to ordinary DatePicker as well. Before accepting a fix, verify the Legacy cases and no mutation on conversion failure. This addresses confirmed data integrity before read allocations or migration cleanup.

**Validation and limits.** JDK 25.0.1 javac against repository compiled classes; actual JavaFX 25 toolkit runs on both classpath and named modules, using standalone controls and real in-memory Dataset adapter. No new production build, full form, SQL, BaseApp or physical input exercise. No new production, test, API, module or dependency changes.


---

## Reply CTRL-002

**Status:** Implemented. RED/GREEN verified on Windows, Java 25.0.1 and JavaFX 25 with a visible Stage. Implementation commit: [dafb93d64e2a1a2d8cd47589aa6c672e574c35ca](https://github.com/ssu95a/fore25/commit/dafb93d64e2a1a2d8cd47589aa6c672e574c35ca), parent ca05ae3c991973f48269542309041fd3eaf426ec, on `migration/java25-correctness`.

**Root cause and diff.** `DSControlAdapter.navigated` queues a call to `Controls.setValue` with the mapped row value. The DatePicker branch used to skip the write to a plain DatePicker because its LocalDate arm required JInvCalendar. The only production diff changes that inner `else if` to `else`. The JInvCalendarTime branch still comes first and uses the existing LocalDateTime conversion. Other DatePicker instances use the existing LocalDate conversion, including JInvCalendar. The typed read path, editor handling and signatures are unchanged.

| Check | RED on parent, exit 1 | GREEN on implementation, exit 0 |
| --- | --- | --- |
| Direct DatePicker write and null clear | Old typed date retained in both cases | 2025-03-04, then null, from both the value property getter and Controls.getValue |
| Real DSControlAdapter and in-memory ArrayDataSet | Cursor advanced to the second date and then a row with a null date, but the picker retained 2025-01-02 | Picker and typed read follow 2025-01-02, 2025-03-04, null |
| Invalid string and repeated equal LocalDate | Invalid string silently ignored because the write was skipped | TypeConverterException, prior typed date preserved, zero change events. Equal LocalDate reassignment adds zero change events |
| JInvCalendar and JInvCalendarTime | PASS | PASS. JInvCalendar accepts LocalDate and null. JInvCalendarTime retains 13:42:17 and accepts null |

**Additional checks.** Same regression source on RED and GREEN. PowerShell parse, Maven compile with existing custom settings, MoneyDecimal, DataBinding (GUI-009) and default GUI smoke (GUI-001) all passed. Remaining GUI dispatch modes were left untouched and were not rerun. Database save and forms requiring BaseApp bootstrap were outside the standalone fixture.

Windows recheck from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -DatePickerBinding -SettingsFile "D:\Java\.m2\settings.xml"
```

---

## Reply CTRL-003

**Status** — Implemented and verified on Windows with Java 25.0.1 and JavaFX 25. A non-force push advanced `migration/java25-correctness` from `dafb93d64e2a1a2d8cd47589aa6c672e574c35ca` to `c21a17e0b2ba8305a02d405449052f9aa0b5b607`. Both SHAs were verified with `git ls-remote` before and after the push, respectively. [Exact commit and diff](https://github.com/ssu95a/fore25/commit/c21a17e0b2ba8305a02d405449052f9aa0b5b607)

`Tab.getContent()`, `ScrollPane.getContent()` and `TitledPane.getContent()` can return null. The production diff adds an early return on null at the start of `Controls.helperGetListNodeInternal`, before the predicate. Existing depth-first order, container inclusion, composite exclusions and public signatures are preserved. The checked production consumers in `AbstractBaseController` and `ViewPrefAppService` select nodes with `instanceof` and do not use null entries. The test diff adds the `-NodeTraversal` runner mode and `NodeTraversalSmoke.java`.

| Check | RED on parent (exit 1) | GREEN on implementation (exit 0) |
| --- | --- | --- |
| Default predicate | Empty tab, ScrollPane and TitledPane each contributed one null | Each empty container returns only itself |
| `node.getId()` predicate | Received null and threw `NullPointerException` | Received each actual node once in expected order and returned the split pane and nested field in order |
| Traversal and lookup | Default traversal order assertion failed due to null entries. Control traversal and field lookups passed | Exact depth-first node order including containers, spinner editor excluded, control order preserved, unique and missing field lookups correct, duplicate rejected |

The same smoke source opened a Stage in both runs. PowerShell syntax and Maven `-Pbuild` compile passed with the existing settings. `-DatePickerBinding`, `-MoneyDecimal` and `-DataBinding` each passed on the fixed version. Coverage is limited to the standalone JavaFX and in-memory fixtures. Full forms and dynamic FXML trees remain unverified. `main` and `JInvCommon` were left untouched.

Windows recheck from the repository root with JDK 25 and Maven available:

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -NodeTraversal -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

---

## Reply CTRL-004

**Status** — Read-only investigation complete on `migration/java25-correctness` at `c21a17e0b2ba8305a02d405449052f9aa0b5b607`. Windows Java 25.0.1, JavaFX 25, real controls on the FX application thread in a shown Stage. The runner compiled only an ignored probe under `target/ctrl004` against the existing production classes and dependency classpath. Production and tracked tests were left intact.

### Contract and callers (source-proven)

- `Controls.java:247–343`. Both `Node` and `Control` convenience overloads delegate to `disableControl(Node, boolean)`. The Javadoc specifies that enable makes text editable and buttons visible. The implementation sets target values from the requested mode without saving prior values. There are no active in-repository calls to the named `enableControl` overloads outside `Controls`; false transitions use the boolean overload.
- `AbstractBaseController.bindControls:1505–1528` binds the controls first and applies `Controls::disableControl` to the binding-filtered list in read-only form modes when an FX entity exists. This is a separate writer from individual `readOnly` listeners. The loop has no per-control exception recovery. A binding conflict can therefore stop that loop partway through; an actual affected form startup is UNKNOWN.
- `JInvTextField`, `JInvTextFieldCUR`, `JInvTextArea` and `JInvPasswordField` wire `readOnly` to the `TextInputControl` branch (`editable = !disable`). `JInvTextFieldCUR` adds a listener atop the inherited one. `JInvCalendar` and its descendant `JInvCalendarTime` use the earlier `DatePicker` branch, which writes editable directly and leaves popup behavior outside that property. `JInvTextField` also binds its embedded buttons' native `disable` to `readOnly` and to editable/`buttonIgnoresEditable` policy. Its `editableOrLovEditable` binding has separate semantics. `ManageFilterController` sets the three filter fields' read-only state, and `JInvPropertySheetSkin` sets it on read-only editors. `JInvFEDialog` binds a separate plain `TextArea` to its source editor's editable property; a collision with this helper on that dialog target was not established.
- `JInvButton` applies hide/show with an early return for writable visibility. Bound visibility falls through to the CSS `disabled` pseudo-class only. `ChoiceFilterController.afterInit` binds visibility for actual `JInvButton` instances `btOK` and `btCancel`; concurrent use of their read-only transitions in that dialog is UNKNOWN.
- `JInvChoiceBox` and `JInvComboBox` set focus traversability, then add/remove the shared filter for mouse pressed/released/clicked. `JInvCheckBox`, `JInvMenuButton`, `JInvRadioButton` and the active `JInvTreeTable` listener reach the generic focus/mouse-transparency branch. `JInvRadioGroup` applies that same helper to its current `Control` toggles. `AbstractBaseController` assigns radios to these groups and `Binder` binds the group value, so group and individual read-only listeners can reach the same radio. No reference count or owner identity is present in the boolean overload.
- Null, `Label`, `ScrollPane`, `SplitPane`, `TabPane`, `TitledPane`, `ScrollBar` and `JInvTable` return directly; the helper does not recurse. `JInvXMLViewer` delegates to its own `setReadOnly`, with inverse editable binding on its internal code area. That viewer's bootstrap reads BaseApp and was source-checked only.

### Reproduction matrix (confirmed runtime)

| Fixture | JavaFX 25 observation |
| --- | --- |
| Plain TextField and DatePicker | With editable initially false, disable/enable yields false then true. With editable bound to true, disable throws `A bound value cannot be set` before CSS, leaves editable true, and preserves the binding. After moving the source to false, enable also throws. |
| Actual JInvTextField listener | Starting with editable false, `setReadOnly(true)` then `false` ends editable=true. With editable bound true, `setReadOnly(true)` reports a listener exception through the FX thread's uncaught exception handler and leaves readOnly=true, editable=true, bound=true and CSS `disabled` absent. |
| Button | Initially hidden becomes visible after disable/enable. With visibility bound true, disable returns with visible=true, native disabled=false and CSS `disabled` present. `Button.fire()` still delivers one action. Actual `setDisable(true)` suppresses the next programmatic fire. The helper can clear the CSS state even while native disabled remains true. |
| ComboBox and ChoiceBox | Initially focusTraversable=false becomes true after disable/enable. With focus bound, the setter throws before filter or CSS changes. Two disables consume all three synthetic mouse event types; one unbound enable restores all three and a second enable still delivers all three. If focus is bound to false after disabling and before enabling, enable throws and all three events remain blocked until unbinding and enabling. |
| Generic CheckBox | Enable forces focusTraversable=true and mouseTransparent=false, even if initially false and true respectively. Bound focus aborts at the first setter. Bound mouse transparency throws at the second setter after focus has already been written; the binding survives. |
| ScrollPane | Bound or unbound focus is untouched. Its contained TextField remains editable. |
| Real JInvRadioGroup and JInvRadioButton | With both reasons set, releasing the group leaves radio.readOnly=true but focus=true, mouseTransparent=false and CSS `disabled` absent. Releasing the individual while group.readOnly=true produces the same interaction state. Native ancestor disable remains effective after helper enable (`isDisable=false`, `isDisabled=true` on the radio), even though the helper clears the shared CSS pseudo-class. |

The event counts use `Event.fireEvent` at the control and an event handler for each type. Button action uses `fire()`. Physical mouse/keyboard gestures, popup navigation, form/DB validation, and production frequency of late focus bindings are UNKNOWN. The focus and editable observations come from the real JavaFX properties. A silently ineffective read-only transition is an integrity/UX risk; a production save bypass is unproven.

### State ownership and next step

JavaFX unidirectional binding makes its source the value owner. The tested setters throw even if an assignment would match the source. `isBound()` can guard that kind of write, while bidirectional binding has different semantics and may propagate changes. A helper-wide skip policy could report success while leaving editable or clickable controls active. Fail-fast preserves the signal and preflight can avoid a partial generic update, but callers such as read-only form setup can still fail and listener exceptions can leave requested and effective state divergent. Binding owner composition can express multiple reasons explicitly at the caller, with bindings/listeners only where needed. Restoring a snapshot on enable would change the documented force-enable behavior, risk overwriting intervening changes, and need storage and lifecycle rules for the affected nodes; a snapshot alone cannot identify two overlapping owners. None of these alternatives provides a measured retained-byte estimate. [JavaFX 25 property API](https://openjfx.io/javadoc/25/javafx.base/javafx/beans/property/Property.html)

Native `setDisable` propagates down to descendants and changes mouse/key delivery. The existing policy includes hiding buttons, making text non-editable and preserving some interaction, and selectively consuming mouse events. The public method should retain its specialized policy. [JavaFX 25 Node API](https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/Node.html#disableProperty())

**One proposed RED/GREEN task** — Release the helper's three mouse filters on enable for `ChoiceBox` and the non-DatePicker `ComboBoxBase` path even when focus traversal has become bound. In this specific enable path, leave focus traversal binding-owned, remove the helper filters, and clear the helper's grey state while respecting an effective native disabled state. Use no per-control snapshot. Keep disable-time policy and the normal unbound focus write. The target is stale input suppression after an explicit enable, where focus traversal and mouse delivery are already distinct mechanisms.

For both a `ChoiceBox` and `ComboBox`, start writable and enabled, call disable twice, inject pressed/released/clicked and assert zero deliveries. Bind focus traversal to false, enable twice, and assert no exception, all three events delivered exactly once per sequence, and the property remains bound and follows its source in both directions of change. Assert absence of helper grey state on an otherwise enabled control. Include an ancestor-disabled case asserting native disabled and its styling stay effective, the unbound repeated-cycle sentinel, and a bound-at-disable sentinel for the existing failure behavior. The current revision is RED on bound enable with zero deliveries. Resolving editable conflicts or agreeing on ownership for radio/group restrictions is outside this single follow-up.

---

## Reply CTRL-005

**Status** — Read-only audit complete at production commit c21a17e0b2ba8305a02d405449052f9aa0b5b607. Contract confirmed by the user: readOnly suppresses user-originated value changes while programmatic writes and model-to-GUI propagation remain supported. The Windows probe used Java 25.0.1 and JavaFX 25, a shown Stage, real Legacy controls and the FX application thread. It compiled an ignored probe against the existing production classes and Maven-resolved classpath. No production source, tracked test, dependency or module configuration was modified.

### Evidence and input provenance

The initial Robot run failed its unlocked positive controls while the operator was using input over RDP. Concurrent input may have affected that run. The repeat with exclusive input passed the unlocked controls and is reported below. The first matrix uses targeted Event.fireEvent key and mouse events routed through actual JavaFX skins and control handlers. For keyboard tests, the probe requested focus and checked retained focus in the radio fixture. Supplying a mouse target bypasses scene picking, including mouseTransparent. Both probes ran to completion with exit 0. This is a diagnostic exit code. [JavaFX 25 Robot API](https://openjfx.io/javadoc/25/javafx.graphics/javafx/scene/robot/Robot.html)

| Legacy control / input | Unlocked positive control | readOnly=true, confirmed under targeted JavaFX dispatch |
| --- | --- | --- |
| JInvTextField / JInvTextArea | Typed x changes abc to abcx. Ctrl+C copies selected abc. Backspace deletes selection. | Text remains abc after typing and Backspace, focus and copy remain available. Tested the text surface and standard skin; additional application editors and LOV integrations remain unverified. |
| JInvCalendar / JInvCalendarTime | F9 and arrow mouse sequence open a closed popup. Targeted day-cell selection changes March 10 to March 12; CalendarTime retains 13:42. | Neither tested path opens a closed popup. If the calendar popup was already open via show() before locking, it remains open and its day-cell event changes the value to March 12. This already-open case was run on JInvCalendar. |
| JInvComboBox (noneditable) | Mouse opens popup; focused Down changes A to B. | Mouse filter prevents the tested open sequence, but focused Down still changes A to B. The final popup mouse sequence did not select an item even unlocked; physical popup item selection remains UNKNOWN. |
| JInvChoiceBox | The closed-popup gesture probe did not open it even unlocked. With show() used solely to set up a visible popup, dispatch to item B selects B. | Locking while the popup is open leaves it open; the same item B dispatch changes the value. Conclusions about opening a closed popup require a positive control. |
| JInvCheckBox | Targeted mouse events and focused Space each change false to true. | Focused Space still changes false to true. Targeted mouse also toggles, but bypasses mouse transparency and does not establish a physical hit. |
| JInvButton | Targeted mouse and Space produce two actions. | Writable visibility hides the button. With visible bound true, the targeted mouse and Space each execute an action despite the grey pseudo-class. Separate programmatic fire() adds a third action. Binding survives and source=false hides the button. Action delivery is distinct from a control value change. |

**Bound-property runtime.** A bound editable=true causes the JInvTextField readOnly listener to report an exception while readOnly remains true. The focused typing and Backspace sequences still edit its text. Changing the source dynamically to false blocks those same sequences while the binding stays intact and copy still works. A bound focusTraversable=true causes the JInvComboBox listener to fail before filter installation. The synthetic mouse opens the popup and focused Down changes A to B. Changing the focus source to false preserves the binding and does not retroactively install filters: targeted mouse still opens and focused Down still changes A to B. Neither result implies a physical key can reach an unfocused control.

### Radio owner overlap and model propagation

The fixture used two actual JInvRadioButton instances with values A and B, one JInvRadioGroup and a bidirectionally bound SimpleStringProperty representing the model. Each trial reset selection to A, focused B before locking, then started a fresh model-change counter. Production Binder uses the same group value property with conversion to the FXEntity property; an actual FXEntity and persistence were outside the fixture.

| Owner state on B | Effective interaction flags | Targeted Space with B focused | Targeted mouse / programmatic selectToggle(B) |
| --- | --- | --- | --- |
| Both true | focusTraversable=false, mouseTransparent=true, CSS disabled=true; focus retained | B, one model change | B / B, one model change each; targeted mouse bypasses picking here |
| Released group only | group=false, radio=true; focusTraversable=true, mouseTransparent=false, CSS disabled=false | B, one model change | B / B, one model change each |
| Released radio only | group=true, radio=false; focusTraversable=true, mouseTransparent=false, CSS disabled=false | B, one model change | B / B, one model change each |
| Fully unlocked | both false; focusTraversable=true, mouseTransparent=false, CSS disabled=false | B, one model change | B / B, one model change each |

**Programmatic runtime.** With the date control locked, actual ArrayDataSet and DSControlAdapter navigated to March 10, March 12 and null. The final run observed one value change per row because the prior value differed. A run that started with March 10 produced 0/1/1 changes. Locked model-to-radio write A→B selected B and emitted one model change. Locked direct text, choice, combo and calendar-time updates each emitted one change. Checkbox setSelected(true) emitted one change when the preceding value was false and zero when it was already true. These are programmatic operations, independent of input dispatch.

### Source paths and prioritization

- Controls.disableControl sets editable for TextInputControl and DatePicker. JInvTextField and JInvTextArea wire their own readOnly listeners to it. JInvTextField uses separate embedded-button disable bindings; JInvLOVButton also has a readOnly-sensitive visibility binding. JInvCalendar gates show() on editable, gates F9 on editable plus valid state, and consumes F4 release. CalendarTime inherits the gate and has a separate date-time value synchronized with the date property. A check at show() cannot intercept an event delivered to a popup that was already opened. The standalone fixture used the default JavaFX skin because no BaseApp feature supplied a calendar skin. [JavaFX 25 ComboBoxBase API](https://openjfx.io/javadoc/25/javafx.controls/javafx/scene/control/ComboBoxBase.html)
- JInvComboBox and JInvChoiceBox listen to readOnly; Controls writes focusTraversable and then adds or removes only the three mouse filters. Generic JInvCheckBox, JInvRadioButton and JInvMenuButton reach the branch that changes focus traversal, mouse picking and CSS. JInvButton listens to readOnly and uses the Button visibility branch. ChoiceFilterController.afterInit binds visible on real JInvButton instances btOK and btCancel; collision in that actual dialog remains UNKNOWN. MenuButton activation and accessibility action were source-checked only.
- AbstractBaseController.bindControls applies a separate view-mode policy after Binder. It also assigns JInvRadioButton instances to JInvRadioGroup. The group listener writes directly to each current toggle via Controls, while JInvRadioButton's own listener writes independently. Binder.bindNonControlComponent binds group.valueProperty bidirectionally to FXEntity. The confirmed last-writer flag transition and focused Space model write are separate defects: composing flags alone would leave the both-locked keyboard path open.
- Highest integrity priority is the radio path because a focused activation and either owner release changed the bound model in the fixture. Closed combo keyboard, already-open popup commits, and bound text edits were also reproduced with Robot input as detailed below. Manual physical input, actual business-form frequency, FXEntity processing, and DB persistence remain UNKNOWN. No memory cost was measured.

### Exclusive Windows Robot rerun

The repeat ran on Java 25.0.1 and JavaFX 25 with synthetic=false and exclusive mouse/keyboard use. JavaFX Robot drove the Windows window and its popup hit-testing. The probe used requestFocus() to set up selected keyboard cases and show() to set up the already-open popup cases. Unlocked typing, selection, toggles and button actions all worked. No hand-operated input was tested.

| Case | Observed through Robot input |
| --- | --- |
| Radio, both owners locked | B retained focus. Click left model A with zero notifications. Space selected B and produced one model notification. |
| Radio, either owner released | With the other owner still locked, click and Space each selected B and produced one notification in separate trials. The fully unlocked positive control behaved the same way. |
| Text field and area | Unlocked typing and deletion changed text. Locked typing and deletion left abc intact; Ctrl+C copied selected abc. With bound editable=true the locked text field still accepted input after the listener exception. Source=false blocked edits and retained copy and the binding. |
| ComboBox and CheckBox | Lock blocked the tested click. Down on the focused combo still selected B. Space on the focused checkbox still selected it. With bound focus traversal, the combo listener threw before installing filters; the popup opened and B could be selected with source=true and again after switching it to false. Down also changed A to B in both source states. |
| ChoiceBox and calendar | Unlocked popup selection worked. From closed, the locked controls resisted the tested opening gestures. When opened before locking, a click selected B in ChoiceBox and March 12 in Calendar. CalendarTime was tested for the closed-popup case. |
| Button with bound visible=true | While locked, click and Space each fired one action. Separate programmatic fire() added a third action. Switching the visibility source to false hid the button and preserved the binding. |

Programmatic selectToggle(B) and model-to-radio writes each propagated once while locked. ArrayDataSet navigation through DSControlAdapter still delivered March 10, March 12 and null, with one calendar value change per transition in this run. The rerun confirms both the retained-focus keyboard defect and the actual mouse-picking difference between two locked owners and either partial release. The single proposed radio correction below retains priority.

### One recommended RED/GREEN correction

Scope the follow-up to JInvRadioButton/JInvRadioGroup user activation. Apply effective readOnly as the logical OR of the radio's own flag and its current Legacy group's flag, so either owner may release without enabling a radio still locked by the other. Gate value-changing user activation even when the radio has retained focus. Preserve programmatic setValue, ToggleGroup.selectToggle and bidirectional model updates; preserve public signatures and avoid global snapshot/restore state. Group attachment, reassignment and detachment must update effective state without leaving a listener or stale restriction on the old group. No broader focus policy is implied by the contract.

RED/GREEN acceptance: on a shown JavaFX 25 Stage, compare both locked, each partial release, radio-only and group-only from a fresh fixture, then fully unlocked. With B focused before locking, try Space and any supported activation shortcut; use a physical mouse on B and verify by hand that all locked cases hold A with zero model notifications, with full unlock producing B and one notification. A native positive control must pass before treating a blocked physical gesture as evidence. Also assert equivalent targeted keyboard outcomes in automation and verify direct selection and model→radio still update once while locked. Exercise group reassign/detach and native ancestor disable as compatibility sentinels. Extend the independent Windows manual matrix to Tab/focus and copy, ComboBox Down, ChoiceBox/calendar popup lock while open, checkbox Space and bound text typing. These are verification cases for the audit findings, not additional implementation assignments.

---

## Reply CTRL-006

**Status** — Implemented on Windows Java 25.0.1 and JavaFX 25. Production advanced by fast-forward from `c21a17e0b2ba8305a02d405449052f9aa0b5b607` to `6797a24554d40026025e49cd26d73f8503f93cd5`. Both revisions were checked with `git ls-remote`. [Exact commit and diff](https://github.com/ssu95a/fore25/commit/6797a24554d40026025e49cd26d73f8503f93cd5)

### Change and compatibility

The radio's own and group's listeners previously wrote interaction flags independently. An owner releasing its restriction could therefore re-enable the radio. Focus traversal flags and mouse transparency did not stop activation of a focused radio. JavaFX 25 toggle-arrow behavior calls `selectToggle` directly, so watching `fire()` alone would also miss selection through a sibling.

`JInvRadioButton` now reads effective readOnly as the OR of its own property and its current Legacy group property. The existing group listener refreshes its current Legacy toggles, and the radio's toggle-group property listener refreshes on transitions to or from a locked group. The pre-selection guards cover Space and Enter activation, primary mouse activation, access through `AccessibleAction.FIRE`, and arrow selection of or from a locked radio. Arrow target resolution follows JavaFX 25 wraparound, orientation and disabled-target skipping. The plain-group filter covers standard ToggleButton peers in mixed groups such as the export controller's format group. Native disabled is respected and its CSS state is preserved when a read-only owner releases. Bound focus/mouse-transparency properties remain owned by their sources while activation is guarded. [JavaFX ButtonBehavior source](https://raw.githubusercontent.com/openjdk/jfx/jfx25/modules/javafx.controls/src/main/java/com/sun/javafx/scene/control/behavior/ButtonBehavior.java)

Explicit programmatic `fire()` remains available and retains its original JavaFX behavior. The accessibility action is guarded independently. `selectToggle`, direct selected writes, group.setValue and bidirectional model writes use the existing selection pipeline. No public property identities, existing signatures, constructors or Binder behavior changed. Production diff is limited to the two authorized radio files.

There is no effective-state property, per-radio snapshot or new reference from a Legacy group to a detached button. A radio stores one additional toggle-group listener registration, and JavaFX stores registrations for shared key and mouse handlers. For a plain group with Legacy members, a shared list listener watches membership and installs a shared arrow handler on standard toggle peers. The old peer is unregistered on removal, and remaining peer handlers and the list listener are removed after the last Legacy member leaves. The handlers capture no group or radio instance. No retained-byte measurements were performed.

### RED and GREEN

Tracked `RadioReadOnlySmoke` first ran RED before production editing, using real controls and a bidirectionally bound SimpleStringProperty on a shown Windows Stage. The final probe was additionally run against the original two production classes compiled into an isolated classpath overlay. Unlocked positive controls passed and RED returned 1. GREEN returned 0 using the normal runner with Maven `-Pbuild` and the user settings.

| Case | RED | GREEN |
| --- | --- | --- |
| Unlocked Space, targeted click and Right | A → B, one model notification | A → B, one model notification |
| Both locked, only group locked, only radio locked, either owner released | Targeted Space, Right from A, targeted click and accessibility FIRE each selected B and wrote the group/model once | Every case preserves selected A, group A and model A, with zero notifications |
| Released both owners | Space selects B once | Space selects B once |
| Programmatic write while locked | selectToggle, model write, group.setValue, selected write and explicit fire each select B once | Same five paths select B once |
| Membership and native disabled | Detach could leave stale flags, and unlock could clear the native disabled pseudo-class | Reassign, detach, reattach, old/new group toggles and native ancestor disable pass |
| Mixed plain group | Arrow from a stock radio can select a locked Legacy peer | Blocked across late stock attachment, detach/reattach and bulk attachment. Unlocked arrow still selects the next radio. |

The matrix above uses targeted `Event.fireEvent` and a separate direct accessibility action. Targeted mouse events bypass native scene picking. Full application forms, real FXEntity integration, radio-specific DataSet navigation, and DB persistence are UNKNOWN. `-NodeTraversal`, `-DatePickerBinding`, `-MoneyDecimal` and `-DataBinding` (GUI-009) passed on the final version. PowerShell syntax and `git diff --check` passed.

### Exclusive Windows Robot verification

With the user's confirmation of exclusive mouse and keyboard access, the same tracked probe ran on Windows Java 25.0.1 / JavaFX 25 with Robot input. Both unlocked positive controls for Space and click passed on each revision, as did unlocked Right. The baseline exited 1 and the published fix exited 0 with `CTRL-006 PASS`.

| Input case | Baseline | Published fix |
| --- | --- | --- |
| Robot click, both owners locked | A, zero model notifications | A, zero |
| Robot click, either owner releases while the other holds | B, one notification for each release case | A, zero in both cases |
| Robot Space on B with retained focus and Right from A, any locked case | B, one notification | A, zero |
| Robot Right from a stock radio into a locked Legacy radio in a mixed group | B, including after detach/reattach and bulk attach | Arrow does not change selection. The stock radio stays selected. Unlocked Right still selects Legacy. |
| Full unlock and separate programmatic updates while locked | B, one notification per tested change | B, one notification per tested change |

Robot exercised Windows input routing and mouse picking. The accessibility action and explicit programmatic writes in the run were direct calls. Hand-operated input remains UNKNOWN.

Independent Windows recheck from the repository root:

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -RadioReadOnly -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

For the Robot variant, add `-RobotInput` to that command only after obtaining exclusive mouse and keyboard access, including RDP. The probe checks unlocked input before interpreting locked cases.

---

## Reply CTRL-007

**Status** — Implemented and verified on Windows with Java 25.0.1 and JavaFX 25. The production branch advanced from `6797a24554d40026025e49cd26d73f8503f93cd5` to `cf7b05c45319624f3f0134a78d59410484829af9` via fast-forward. Both remote SHAs were checked. [Exact commit and diff](https://github.com/ssu95a/fore25/commit/cf7b05c45319624f3f0134a78d59410484829af9)

### Root cause and change

The inherited JavaFX list behavior handles Up/Down by calling the selection model, even when the focused control has been removed from focus traversal. The previous Controls call also wrote focusTraversable before installing mouse filters, so a binding exception prevented mouse protection. An already-open popup has separate event routing. In editable mode the inner TextField could change a draft, which JavaFX committed on focus loss. [JavaFX 25 ComboBoxListViewBehavior source](https://raw.githubusercontent.com/openjdk/jfx/jfx25/modules/javafx.controls/src/main/java/com/sun/javafx/scene/control/behavior/ComboBoxListViewBehavior.java)

Only `JInvComboBox.java` changed in production. It guards Up/Down before skin selection and guards the editable Enter-release commit. It toggles the existing three mouse-filter event types locally and leaves a bound focus property owned by its source. Locking closes and disarms the popup. Calls to show() while locked leave it closed, including explicit programmatic show() calls. Unlock restores normal opening. The existing skin and the combo's editable property stay in use. For the standard writable editor flag, locking cancels the current draft and makes the inner editor non-editable. Unlock restores the flag only if the combo changed it. No converter, valueFactory, nullable, value/selection property or native disabled setting is replaced. Additional retained state is a boolean, self-owned listeners and registrations for shared handlers. Memory in bytes was not measured.

### RED and GREEN

The tracked regression first ran before production modification and returned RED with unlocked controls passing. The final probe also ran against an isolated compile of the original JInvComboBox and returned 1. GREEN on the real `JInvComboBoxListViewSkin` with Maven `-Pbuild` returned 0. Both used a shown Stage and real controls and model listeners.

| Case | Original | Fixed |
| --- | --- | --- |
| Unlocked Down/Up and arrow/popup mouse selection | Each supported gesture changed selection/value/model once | Same |
| Retained focus on Down/Up, unbound and bound focus | Selection/value/model each changed once under readOnly | Desired A or B retained, zero selection/value/model events |
| Bound focus source true/false, source changed while locked, repeated lock/unlock | Bound-value exception and mouse could open popup | Binding intact, no exception, locked popup stays closed; after unlock arrows and popup selection work once |
| Popup already open before lock | B could still be chosen, one model change | Closed on lock, A retained with zero events |
| Editable Backspace then focus loss | Draft changed and committed into selection/value/model | Standard editor inhibits edit, A retained and zero events; combo remains editable |
| Programmatic and model writes under lock | Updates work | setValue, selectionModel.select, setSelectedValue, Controls.setValue and bidirectional model write work, one event in each observable per change |
| Actual ArrayDataSet and DSControlAdapter under lock | A, B, null in editable fixture | Same, one event in each observable per row transition |

PowerShell parsing, `git diff --check`, `-RadioReadOnly`, `-NodeTraversal`, `-DatePickerBinding`, `-MoneyDecimal` and `-DataBinding` passed. Model binding coverage used SimpleStringProperty and the real adapter. Full business forms, FXEntity Binder integration and DB persistence remain UNKNOWN. The standard editor test uses a round-tripping converter. A caller-owned binding on the inner editor's editable property is left intact; if it keeps the editor editable, editing through that custom configuration is outside the verified protection. Replacing the skin and other readOnly controls are outside CTRL-007.

Input for this change was targeted JavaFX dispatch, including popup cell events. It verifies skin processing but targeted mouse dispatch bypasses native scene hit testing. Robot was not run without confirmation of exclusive mouse and keyboard access. Hand-operated input was not tested. The Windows Robot audit from CTRL-005 remains prior baseline evidence.

Independent Windows recheck from the repository root:

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ComboReadOnly -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

For Windows Robot input, add `-RobotInput` only once exclusive mouse and keyboard access, including RDP, is confirmed.

---

## Reply CTRL-008

**Status:** READ-ONLY investigation complete. Confirmed model writes under readOnly on the JavaFX 25 editor focus-loss path.
**Production:** `migration/java25-correctness` at `cf7b05c45319624f3f0134a78d59410484829af9`. Mailbox base `dbf85d264b987be74e5faa6e2641a7d4629da486`.

### CONFIRMED runtime

Windows JDK 25.0.1 and JavaFX 25, shown Stage on the FX thread, production JInvComboBox and JInvComboBoxListViewSkin recompiled into ignored target. Observable model: bidirectional SimpleStringProperty. Counts below are selection/value/model changes from A/0; editor text is recorded separately. Keyboard and popup mouse input used targeted JavaFX dispatch. Focus transfer and explicit API calls were direct calls. Robot and manual physical input were not run because exclusive desktop input was not established. Targeted popup cell dispatch bypasses OS hit testing.

| Case | Draft and focus | Selection/value/model |
| --- | --- | --- |
| Unlocked positive controls | Backspace + KEY_TYPED B produced draft B. Separate runs used Enter, focus transfer and explicit forceCommitValue. Normal popup mouse selection and Up/Down worked. | Prior to commit A/A/A, 0/0/0. Each successful action produced B/B/B, 1/1/1. |
| Locked with ordinary unbound editor | Backspace + B left draft A. Enter moved focus. Separate focus-loss and forceCommitValue runs also left draft A. | A/A/A, 0/0/0 throughout for the round-tripping converter. |
| Locked with caller-owned editor editable binding | Source false at lock left editor noneditable. Source changed to true under lock and the still-bound editor accepted Backspace + B. Draft B with focus retained. | A/A/A and 0/0/0 until commit. Binding intact, no bound-property write required. |
| Same bound editor, targeted Enter press then release | Focus lost and change had already happened after KEY_PRESSED; release added no write. | B/B/B, 1/1/1. This observation does not ascribe the write to the skin's Enter-release handler. |
| Same bound editor, direct next.requestFocus after targeted editing | Focus lost without any Enter event. | B/B/B, 1/1/1. This isolates the implicit commit of a user-style draft. |
| Same bound editor, direct forceCommitValue after targeted editing | Focus retained; explicit helper committed B. | B/B/B, 1/1/1. The draft came from targeted input and the commit invocation was programmatic. |
| Locked with ordinary editor, diagnostic asymmetric converter | toString(A)=a, fromString(a)=a. cancelEdit displayed a, with no local text edit. Moving focus committed a despite editorEditable=false. | Before focus transfer A/A/A, 0/0/0. After transfer a/a/a, 1/1/1, selected index -1. This is a separate mechanism and fixture; prevalence of such converters in locked forms is unknown. |
| Explicit public show(), arrow click and F4 while locked | Popup stayed closed. Locking an already open popup also closed it. | A/A/A, 0/0/0. |
| Diagnostic ancestor show bypass, then targeted click on B | A test-only special method handle invoked ancestor show on the same production combo, with readOnly=true. Popup was visible. | Opening alone kept A/A/A, 0/0/0. Popup B click produced B/B/B, 1/1/1. This simulates removing the opening guard and is not a normal public route. |
| Explicit editor.setText(B) then forceCommitValue in locked ordinary editor | Programmatic draft B accepted while editorEditable=false; force committed without loss of focus. | A/A/A and 0/0/0 after setText; B/B/B and 1/1/1 after explicit force. |
| Locked programmatic value/model updates | setValue, selectionModel.select, setSelectedValue, Controls.setValue and direct bound-model write. Real ArrayDataSet and DSControlAdapter navigated A, B, null in editable mode. | Each transition propagated exactly once to selection/value/model. |

The existing ComboReadOnlySmoke was rerun directly against the locally recompiled production combo/skin and cached project classpath: CTRL-007 PASS, Java exit 0. This was a runtime rerun, with no Maven rebuild. Temporary local probe and results are under ignored target/ctrl008. The probe includes the bound=false state observation and false-to-true transition; a separate bound-false-only gesture matrix was not run.

### SOURCE-PROVEN paths and compatibility

- JInvComboBox.applyReadOnly closes and disarms the control on lock and filters mouse events on the combo. lockEditor calls JavaFX ComboBox.cancelEdit to replace draft text with its rendered value, then sets the inner TextField noneditable only when the property is writable. The combo's own editable mode stays true. Searches of tracked application source found no caller binding the inner editor's editable property. External use is unknown.
- The installed JavaFX 25 bytecode shows that ComboBox registers a focused-property listener in its constructor. On focus loss it calls final commitValue(), which checks combo.isEditable(), reads the editor text, calls converter.fromString and sets value. Neither the editor editable state nor JInvComboBox.readOnly participates. The skin has a separate Enter KEY_RELEASED conversion path in ComboBoxPopupControl, which the CTRL-007 filter targets. The observed KEY_PRESSED focus transfer demonstrates why the release gate is insufficient. A focus listener added after ComboBox construction or a value rollback cannot be assumed to prevent the first model notification. A skin-only Enter change cannot remove the native constructor listener.
- Repository popup caller JInvKeyboardManager.fireOpenEvent calls ComboBox.show on keyboard Up/Down paths. The JavaFX behavior also opens via arrow/mouse, F4 and Alt+Up/Down. ComboBoxBase accessibility actions route through show/hide. The standard ComboBoxListViewSkin synchronizes popup list selection into the combo; the popup has its own dispatch route. JInvComboBox.applyReadOnly owns its explicit inherited hide call. The source search found no application caller explicitly opening a locked JInvComboBox for inspection and no combo-specific external hide or popup lifecycle handlers in this checkout. The public override changed observable behavior from pre-CTRL-007 inherited show, but whether external callers rely on locked viewing is unknown. Simply restoring inherited show would reopen the demonstrated selection path; a public viewing policy would need selection protection before model events, including popup keyboard and accessibility paths.
- JInvComboBox.forceCommitValue delegates to JInvComboBoxListViewSkin.forceCommitValue. It tests combo editable, reads the editor, parses through the converter with exception fallback, writes only a changed value, and resynchronizes the display. It does not check readOnly or editor editable. It relies on the installed custom skin (null or a different skin can fail independently of readOnly). The concrete application caller is ButtonValidationListener, a Scene mouse filter installed by AbstractBaseController. On a qualifying MOUSE_PRESSED on another IJInvControl with a focused JInvComboBox, fall-through past validation/ERROR takes the force path without a readOnly check. Thus the helper can be invoked on behalf of a user action. The direct probe verifies the callee can write; the full form callback was traced in source. No application call to the inherited combo commitValue or cancelEdit was found besides lockEditor's cancelEdit.
- Separate popup path: F7FilterDialog.initSearchControl wires JInvBindings.bindAutoCompletion(edSearch) to the combo editor. JInvAutoCompletionComboBoxBinding.showPopup compares converted draft and current value; completeUserInput applies the ControlsFX completion, converts the editor text and can call comboBox.setValue, with no readOnly gate. This popup is independent of JInvComboBox.show. The source path is established; the locked F7 screen, a popup already pending at lock, and actual selectable suggestions under lock remain unverified.
- Binder.bindComboBox uses bidirectional FXEntity binding or explicit valueFactory/model listeners; a leaked combo value can flow to the field. Controls.setValue delegates to setSelectedValue and DSControlAdapter drives row-to-control changes. Blocking setValue, the selection model, or the public force method indiscriminately would affect legitimate updates. Actual FXEntity and business form behavior was not exercised.

### HYPOTHESIS and UNKNOWN

Context-menu cut/paste, drag/drop, IME, undo/redo, formatter-driven edits, and accessibility text actions may supply drafts through routes other than the tested keys. A KEY_TYPED-only filter is inadequate as a completeness claim. Clipboard copy and selection should remain available where the control currently supports them. There is no proof of a normal public route into the standard locked popup after CTRL-007. External callers, replacement skins, full forms/validation, physical input and DB persistence remain UNKNOWN.

### Priority and one follow-up

Open one narrow RED/GREEN task for **preventing a user-edited inner-editor draft from reaching the model with a caller-owned editable binding**. First reproduce B through targeted typing and focus transfer on the real skin; also assert Enter press/release and the qualifying validation mouse-press route. Prefer rejecting user edits at their input entry points while locked, preserving the live editor binding and allowing display sync and explicit programmatic editor commits. Confirm the scope with paste/cut/context menu, drag/drop, IME and applicable accessibility routes before claiming completeness. If a validation-specific guard is needed, place it at the user-origin caller after proving the route in runtime. Do not treat all calls to the public helper as user activity. A formatter swap would change application-owned editor state; avoid assuming it is compatible. Use shared handlers or existing lifecycle where practical; no per-focus snapshots or listeners retained by retired skins/editors. Retained bytes were not measured.

GREEN must show no user-driven draft or model change and zero selection/value/model notifications under the locked bound=true and false-to-true cases, while preserving copy/selection, normal unlocked typing/commit, CTRL-007, programmatic editor.setText + forceCommitValue, direct value/selection/model writes and real adapter navigation. Verify repeated lock/unlock and unchanged binding identity. Keep the asymmetric-converter case as an explicit known failing diagnostic until a compatible guard for the native implicit conversion has been proven; an input-only correction cannot solve it. Keep the standard popup opening behavior as-is in this task. If locked viewing is desired, decide the public show policy explicitly and first prove the popup cannot commit before changing it.

Only this mailbox reply was prepared for publication. Production and tracked tests were left unchanged.

---

## Reply CTRL-009

**Status:** RED reproduced before the production edit. GREEN passed on Windows JDK 25.0.1 and JavaFX 25.
**Production:** `migration/java25-correctness` from `cf7b05c45319624f3f0134a78d59410484829af9` to `5fc050d452e32adf55407512c8e9b5e9cc13fa75`, fast-forward push and remote SHA verified.

### Cause and change

The old readOnly listener delegated to the generic Controls branch, which changed focus traversal, mouse transparency and the disabled pseudo-class. A CheckBox that had already acquired focus could still arm on Space press. JavaFX ButtonBehavior fired the CheckBox on Space release, writing selected and the bidirectionally bound model. The generic branch also wrote bound focus/mouse properties without checking binding ownership.

JInvCheckBox now checks readOnly before native activation. A shared key filter blocks activation presses and disarms before release so the skin can clear its key-down state. A shared mouse filter blocks primary activation even with mouseTransparent bound false and prevents an armed gesture from completing across the lock. The accessibility FIRE override gates the assistive activation entry point. Direct CheckBox.fire() keeps its JavaFX behavior, including native disabled handling. A direct executeAccessibleAction(FIRE) call intentionally takes the same gated route as accessibility activation. The readOnly listener changes focus and mouse properties only when writable and preserves their existing lock/unlock defaults. Disabled CSS reflects readOnly or native/ancestor disabled. The fix adds no instance fields, cache or per-instance closures beyond those already present. It adds two event-filter registrations and one shared disabled-listener registration per control. Retained bytes were not measured.

The exact diff adds the input/style changes in JInvCheckBox.java, the tracked CheckBoxReadOnlySmoke.java and the -CheckBoxReadOnly script mode. JInvCheckBoxString, renderer/table editor, Controls, ComboBox, build/dependency configuration, main and JInvCommon were untouched. [Production diff](https://github.com/ssu95a/fore25/commit/5fc050d452e32adf55407512c8e9b5e9cc13fa75)

### RED/GREEN, shown Stage with production controls and skins

Counts below are changes to selected/model plus emitted ActionEvent for the Boolean fixture. Keyboard/mouse results used targeted JavaFX events, including primary press/release sent to the actual control. Accessibility used a direct accessibility action call rather than a screen reader. Robot was not run because exclusive desktop input was not established. Native mouse hit testing, physical input and OS accessibility integration remain UNKNOWN.

| Scenario | RED on original JInvCheckBox | GREEN |
| --- | --- | --- |
| Unlocked positive controls | Space, primary mouse gesture and accessibility FIRE each toggled false to true with 1/1/1 | Same |
| Retained focus, locked Space from false and true | Both directions toggled selected/model, with 1/1/1 | State retained, 0/0/0 in both directions |
| Space pressed before lock and released under lock | 1/1/1 in both directions | 0/0/0; next Space after unlock toggled once, 1/1/1 |
| Primary press before lock, release under lock | 1/1/1 | 0/0/0 |
| Bound readOnly, focusTraversable and mouseTransparent with live focus/mouse sources | Focus setter threw. Space, targeted primary mouse and accessibility FIRE each wrote the model | Bindings remained live and intact. Each activation attempt produced 0/0/0; no listener exception |
| Unidirectionally bound selected | Space tried writing selected and threw | No throw or notifications from Space. Source change still updated selected once |
| Ancestor native disable and CSS | Unlock cleared the native disabled pseudo-class. Ancestor reenable while readOnly cleared locked CSS | Both transitions preserved the applicable CSS state; native-disabled direct fire had 0/0/0 |
| Programmatic setSelected, selectedProperty.set and bound Boolean model write while locked | 1/1/0 per change | 1/1/0 per change; no additional event for repeating the same value |
| Direct programmatic fire while locked and native enabled | 1/1/1 | 1/1/1 |
| Real ArrayDataSet with DSControlAdapter | false to true to false, 1/1/0 per transition | Same |

Both JInvCheckBoxString constructors were exercised with Y/N mappings and a bidirectional StringProperty model. Locked Space from the checked state on RED changed Y to N and added one selected, one value and one string-model notification in each constructor. On GREEN, locked Space from unchecked, checked and indeterminate produced zero selected, indeterminate, value and string-model notifications. Programmatic selected, setValue and model writes still mapped Y/N with exactly one change per affected observable. Direct setIndeterminate(true) retains the constructor-specific behavior: the no-arg constructor maps the value/model to null once, while the text constructor changes indeterminate alone until setValue(null) is called. This distinction was also observed on RED. SetValue(null) produced no redundant notification when the value was already null. Full business forms and DB persistence were not exercised.

PowerShell parsing and Maven -Pbuild compile passed. The final CTRL-009 script exited 0. Accepted regression probes ComboReadOnly, NodeTraversal, DatePickerBinding, MoneyDecimal and DataBinding passed when freshly compiled and run against the same Maven production classes and classpath. The initial direct RadioReadOnly run failed its mixed-group unlocked-arrow assertion; the official -RadioReadOnly script rerun passed the full matrix, including that assertion, with exit 0.

### Independent Windows rerun

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -CheckBoxReadOnly -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

Expect `CTRL-009 PASS` and exit 0. Add `-RobotInput` only with exclusive Windows desktop input available.

---

## Reply CTRL-010

**Status:** READ-ONLY audit complete. Selection/model writes confirmed. Preserving programmatic show while reliably preventing selection remains a policy/design decision.
**Production:** `migration/java25-correctness` at `5fc050d452e32adf55407512c8e9b5e9cc13fa75`. Mailbox base `ab25df7ae3ad4cb7196091950e2ce01f900d792a`.

### CONFIRMED runtime

Windows Java `25.0.1+8-LTS-27`, JavaFX `25`, production JInvChoiceBox and default ChoiceBoxSkin, FX thread and shown Stage. A/B items and a bidirectional SimpleStringProperty model. Counts are selected-index/value/model changes. The fixture recompiled the unmodified JInvChoiceBox against the local build and dependency classpath. The final audit process exited 0 after asserting the positive controls and reproductions. Inputs were targeted JavaFX events; pointer dispatch bypasses OS picking. Accessibility used direct calls to the control/menu item's accessibility entry points. Explicit API calls and adapter navigation are marked separately.

| Scenario | Observation |
| --- | --- |
| Unlocked keyboard positive control | Space opens. Popup Down focuses B; Enter commits B and closes the popup. A→B, 1/1/1. |
| Unlocked pointer positive control | Primary press/release at the control center opens. Targeted primary dispatch to the actual B menu-item node commits B, 1/1/1. Both positive controls passed before locked assertions. |
| Open before lock | readOnly transition leaves popup showing and A unchanged initially, 0/0/0. Separate keyboard and pointer runs then commit B under readOnly, each 1/1/1. |
| Focus retained, locked and originally closed | Space and Down reopen without an immediate value change; Down→Enter in each reopened popup commits B, 1/1/1. Up, F4, Enter, Alt+Up and Alt+Down did not open or select in either mode in this Windows fixture. Popup Escape closed when unlocked, 0/0/0. |
| Targeted primary click, closed under lock | Writable focusTraversable case stayed closed, 0/0/0. |
| readOnly and focusTraversable both bound | Two lock/unlock cycles generated four bound-value exceptions, delivered to the FX thread's uncaught-exception handler. Both bindings remained intact, and focus source false→true propagated while locked. In each locked cycle targeted primary click opened and B selection wrote the model, 1/1/1. |
| Accessibility | Direct EXPAND opened the locked ChoiceBox, 0/0/0. Direct FIRE on the actual B menu-item node then committed B, 1/1/1. The SHOW_MENU action was not the ChoiceBox expansion entry point in this runtime. |
| Application show()/hide() while locked | Explicit show opened with 0/0/0; B remained user-selectable through the live popup and changed the model, 1/1/1. Explicit hide closed normally. |
| Native ancestor disabled | With popup initially closed, attempted explicit show, Space, pointer and EXPAND left it closed, 0/0/0. Model→control still worked, 1/1/1. Disabling the ancestor while the popup was already open left it visible, and targeted B pointer activation still committed, 1/1/1. |
| Programmatic updates while locked | setValue, selectionModel.select, setSelectedValue using valueFactory and model→control updates each propagated exactly once. setValue(null) selected index -1 and propagated null, 1/1/1. setSelectedValue(null) retained B, 0/0/0, per existing API. |
| DataSet navigation while locked | Real ArrayDataSet with Binder.bindChoiceBox and DSChoiceBoxAdapter navigated A→B→A, each 1/1/1. Separate diagnostic generic DSControlAdapter binding hit the existing unsupported Controls.setValue dispatch for JInvChoiceBox; the specialized ChoiceBox adapter is the supported path used for this check. Null-row behavior of the specialized adapter was not established. |

### SOURCE-PROVEN on this checkout and installed JavaFX 25

- Both JInvChoiceBox constructors call Controls.disableControl when readOnly changes. Its ChoiceBox branch writes focusTraversable before installing mouse filters, and has no bound-property guard, keyboard filter or popup hide. It does not set native disable.
- ChoiceBoxBehavior maps Space release and Down release to showPopup; Space press cancels an existing popup. Escape/Cancel release cancel. Enter mappings live in a two-level-focus child input map; the Windows fixture did not activate them. Primary mouse release within the control opens a closed popup, and mouse release when already showing closes it. Neither focused opening nor public ChoiceBox.show commits by itself. JavaFX ChoiceBox.executeAccessibleAction maps EXPAND and COLLAPSE to show and hide. ChoiceBox.show checks native disabled state.
- ChoiceBoxSkin owns a separate ContextMenu. Its popup accessor is package-private. The skin creates a RadioMenuItem action that resolves the item index and calls the live selection model directly. ContextMenuContent routes pointer, keyboard and accessibility through MenuItemContainer.doSelect. This checks item-level disable, updates the menu radio mark, then fires the item action. A filter on the ChoiceBox alone cannot protect that action. Native temporary window focus/resize behavior was not substituted for model protection.
- Binder's field binding uses bidirectional FXEntity binding or valueFactory mapping and setSelectedValue. The public DataSet bind method constructs DSChoiceBoxAdapter, whose navigation calls selectionModel.select. Guarding the selection model or applying rollback after notification would break the contract. ExportController/Export.fxml is an actual repository use: it fills formats, sets initial value and reads the value. No application caller opening this ChoiceBox for locked viewing, specific popup hooks, or custom ChoiceBox skin was found in this checkout. External consumers are unknown.

### Feasibility and concrete next action

**Recommend documenting the limitation.** A small close-on-lock change plus bound-safe focus handling, control-level mouse/Space/Down guards, and an accessibility EXPAND guard can remove the ordinary reopening routes. Closing itself was tested: it left A and 0/0/0, but a subsequent explicit show reopened. While explicit show is supported, the baseline reproduced a subsequent menu action writing B. An unconditional show guard would change current public API semantics, including intentionally opening a list for viewing, so it needs a separate policy decision. Such a closure policy could reuse the existing readOnly listener and shared filters without adding an instance field; it would still add filter registrations. Exact retained bytes are unknown.

A test-only ContextMenu ACTION filter, attached after discovering the visible stock popup via Window.getWindows and ownerNode identity, blocked targeted pointer, keyboard, accessibility FIRE and even a direct hidden MenuItem.fire, all with 0/0/0. But the menu checked B while the control still held A, and still showed B checked on reopen. Bytecode explains the mismatch: the menu sets the mark before sending ACTION. Disabling discovered menu entries preempted the tested pointer/accessibility activation, but a production solution would need dependable popup ownership/identity, lock/unlock, item regeneration and skin replacement handling. Owner-node discovery and the observed stock popup ID are not a supported typed ChoiceBox popup API. A discovery-based gate would add popup or per-item registrations and lifecycle management beyond the evidence for a narrow fix. It is not a proposed production solution.

For a form that requires an input lock, document that JInvChoiceBox.readOnly is insufficient to protect the model. Close any open popup before applying native disable where disabled interaction is acceptable, and verify the form's model updates. A separate display control can serve inspection-only forms. If the application wants a production ChoiceBox policy change later, first decide whether explicit show under lock may be closed; the audit does not authorize that change.

### UNKNOWN / validation limits

Robot was NOT RUN because exclusive Windows input was unavailable. Physical keyboard/mouse picking, screen-reader integration, alternate skins/platforms, external callers, real FXEntity business forms and DB persistence remain unverified. The accepted regression suite was not rerun for this read-only task. The ignored local fixture is `target/ctrl010/ChoiceReadOnlyAudit.java`; runtime output and installed JavaFX bytecode diagnostics are in the same directory. No production or tracked-test changes were made.

---

## Reply CTRL-011

**Status:** READ-ONLY audit complete. Confirmed model writes under readOnly with a caller-owned editable binding. Recommend documenting the limitation.
**Production:** `migration/java25-correctness` at `5fc050d452e32adf55407512c8e9b5e9cc13fa75`. Mailbox base `571e839a4f5186e7fb167b7219ed42eaf795cd76`.

### CONFIRMED runtime

Windows Java `25.0.1+8-LTS-27` and JavaFX `25`, real JInvTextField/JInvTextFieldButtonSkin, shown Stage, focused field and bidirectional SimpleStringProperty model. Counts are text/model notifications per operation. Keyboard and committed IME text used targeted JavaFX events. Clipboard used actual Windows clipboard text with probe data. The built-in menu used real MenuItem nodes and direct accessibility FIRE; the fixture explicitly refreshed the stock menu before the menu checks. OS clipboard access for popup tests required the normal Windows execution context; the initial restricted run could not establish menu Paste. Clipboard snapshot restoration was attempted after the run if the current text still matched the probe's last write.

| Scenario | Result |
| --- | --- |
| Unlocked, editable bound true, positive controls | KEY_TYPED, Backspace, Delete, Ctrl+V, Ctrl+X, synthetic committed IME text, stock menu Paste/Cut, and shortcut undo/redo each worked, 1/1. Selection, Ctrl+C and stock menu Copy put selected text on clipboard with 0/0. |
| Locked, editable bound true | Lock kept focus and readOnly=true. The listener emitted JInvTextField.editable: A bound value cannot be set to the FX thread's uncaught-exception handler. The same mutations still worked, 1/1 each. Copy/selection still worked, 0/0. |
| Bound source true→false→true while locked | Binding intact. With false, the tested key/IME mutations stayed at 0/0 and freshly populated menu omitted Paste/Cut. With true again the same edits resumed, 1/1. Unlock while bound also emitted the bound-value exception. |
| Locked, ordinary writable editable | Native editable=false blocked the tested key/IME routes and omitted Paste/Cut in a freshly populated menu. Selection, keyboard and menu Copy still worked with 0/0. |
| Menu generated before lock, ordinary writable editable | Menu stayed showing after editable became false. The already-enabled Cut entry remained actionable; direct FIRE on the live menu node changed abcd→cd, 1/1. |
| Direct accessibility SET_TEXT | In the locked writable case with editable=false, the direct accessibility entry point changed text and model, 1/1. OS screen-reader availability of this action remains unverified. |
| Ancestor disabled with readOnly active | Targeted typing/Backspace left text unchanged, 0/0; model→GUI still worked, 1/1. This check did not establish clipboard/menu/accessibility behavior across ancestor disable. |
| Programmatic paths | Locked setText, textProperty and model writes and Controls.setValue each gave 1/1 with the bound-true field. With the ordinary locked field, the stock uppercase formatter normalized programmatic setText, textProperty and Controls.setValue without replacing the formatter, 1/1 per change. |
| Real ArrayDataSet and DSControlAdapter | While locked with editable bound true, navigation made A→B→null visible in text and model, 1/1 per row. |
| Diagnostic custom TextFormatter veto | Returning null for content changes under lock blocked targeted typing with 0/0. It also blocked setText and textProperty writes with 0/0. The already-seeded native undo changed text/model, 1/1, bypassing the filter. A formatter value-model write still reached the display with 1/1 through the formatter's separate value/display path. |

### F9, LOV and contextual changes

- Used a small JInvTextField subclass overriding only showLOV to record calls and optionally publish a distinct test result. It retained the production F9 handler, native control and skin. Unlocked F9 entered the route and the stub result wrote text/model once. Locked F9 with editable bound true did the same. With source false, F9 was blocked until buttonIgnoresEditable=true; that flag restored the locked user-key path and the stub result generated 1/1. Direct programmatic showLOV on the fixture also still delivered its result.
- The real JInvLOVButton installed through setInnerButton was disabled through a binding and invisible under lock; direct Button.fire had no effect. A real external JInvLOVButton attached through setTextField in the tested ordering was registered as the external button, visible and enabled under lock. Its **direct** Button.fire reached the same stub and generated 1/1. This demonstrates the action wiring, not physical button hit testing or all external-button layouts.
- Production F9 calls commitValue before showLOV and checks editable OR buttonIgnoresEditable plus validation state, with no readOnly check. With a real converter-backed TextFormatter and a stub chooser that performed no write, targeted typing produced text seedx while the formatter model remained seed (1 text event, 0 formatter-model events). F9 then changed the formatter model to seedx exactly once, with zero additional text events.
- Actual showLOV delegates to AbstractLovBase.showChoiceList when present; the LOV value listener updates text via setText or append/replace depending on configuration. Authorized programmatic LOV updates must retain both forms. The real chooser, validation and DB-backed results were not run. The stock extra Edit Dialog item goes through JInvFEButton/JInvFEDialog. The dialog binds its inner area's editable to the source field but uses a null result converter when its opening snapshot is readOnly, so that built-in route has a separate write-back check. A dialog opened before switching readOnly relies on its opening snapshot; actual late-lock dialog behavior was not tested.

### SOURCE-PROVEN and feasibility

JInvTextField constructors install the readOnly→Controls.disableControl listener. Controls attempts setEditable on TextInputControl with no binding guard, and the exception occurs before the CSS update. Merely skipping the setter preserves the binding and leaves the bound-true field editable. Binder.bindTextField binds text bidirectionally through TFSCnv; DSControlAdapter navigation sends values through Controls.setValue. A content mutation can therefore reach the model before any later listener rollback.

The stock CaseSensitiveFormatter is already field-owned and can delegate to an inner filter; JInvTextFieldFormatted and time controls can install different formatters. JavaFX 25 uses a formatter filter for both edit replacements and ordinary programmatic text assignment. Undo/redo operate on content directly; direct accessibility SET_TEXT uses setText. The stock menu snapshots editable at population and calls programmatic mutation APIs, and the skin deliberately preserves application-owned menus/handlers. F9, external buttons and delayed LOV results are independent routes. A single key filter misses menu, IME and accessibility. A blanket replacement/mutation override would also intercept API calls used for application updates. Origin cannot be inferred from TextFormatter.Change, and the temporary veto demonstrated both overblocking and bypass.

**Single recommended next action:** document that JInvTextField.readOnly with caller-bound editable is insufficient to protect mutable model values. The audit did not establish a small, complete fix that keeps the binding, application formatter and programmatic APIs compatible. Hardening only F9, or closing and guarding the stock context menu, fixes specific entry points but leaves the rest. Complete support under arbitrary caller binding would require coordinated input/IME, contextual, accessibility and LOV policy and lifecycle handling. Shared filters could avoid new per-instance handler objects, but still add registrations; formatter wrappers or menu/LOV tracking would add per-instance state and lifecycle costs. Exact retained bytes are unknown. Forms that require enforcement should enforce it at their model/action boundary and verify the display/inspection path independently.

### UNKNOWN

Robot NOT RUN because exclusive input was unavailable. Physical keyboard, mouse/clipboard gestures, OS IME composition, drag/drop, screen-reader behavior, app-owned context handlers, full FXEntity forms, real LOV/DB and production subclass runtime matrices remain unverified. No accepted regressions were rerun in this read-only audit. The local fixture and evidence are under ignored `target/ctrl011`. No production or tracked-test files changed.

---

## Reply CTRL-012

**Status:** READ-ONLY audit complete. Confirmed date/datetime writes from an already-open popup and additional bound-editor commit paths. Recommend documenting the limitation.
**Production:** `migration/java25-correctness` remained at `5fc050d452e32adf55407512c8e9b5e9cc13fa75`. Mailbox base `34925e29513a8740c4c47d19d8434be08e104634`.

### CONFIRMED runtime

Windows Java `25.0.1+8-LTS-27`, JavaFX `25`, shown Stage, actual JInvCalendar(), JInvCalendar(LocalDate), and JInvCalendarTime. All used the stock JavaFX DatePickerSkin, with no BaseApp. The ignored fixture recompiled the unchanged production calendar classes against the existing Maven classpath. Each fixture established its unlocked popup and text-entry positive controls before testing the locked variants. The final fixture exited 0. Counts below are date/datetime/model notifications; the date-only fixtures have no datetime observable. Separate models used bidirectional SimpleObjectProperty<LocalDate> or SimpleObjectProperty<LocalDateTime> on the same control properties selected by Binder.

| Case | Result |
| --- | --- |
| Unlocked popup positives | F9 and primary-arrow dispatch each opened; clicking the actual March 12 day changed March 10→12. Separate keyboard run focused the March 12 cell, then Enter selected it. Each change was 1/0/1 for date-only and 1/1/1 for CalendarTime, which retained exactly 13:42:00. |
| Unlocked typed positive | Replacing the selected editor text with a valid date/datetime changed the draft with 0/0/0. Direct focus transfer committed March 12, 1/0/1 or 1/1/1. CalendarTime used an explicit seconds field for deterministic checks. |
| Popup open before writable readOnly transition | Parent and editor became noneditable, but popup stayed showing with March 10, 0/0/0. Targeted pointer and focused Enter selected March 12 in separate runs, each 1/0/1 or 1/1/1. CalendarTime again retained 13:42:00. |
| Popup initially closed, writable lock | F9, arrow, programmatic show and direct accessibility EXPAND did not open or write. Those routes opened when unlocked. Targeted F4, Space, Down and Alt+Up/Down did not open even unlocked in this fixture. Down/Alt arrows could transfer focus, which in unlocked CalendarTime triggered a separate time-only reparse. |
| Native ancestor disabled | Disabling with popup already showing left it open; targeted day selection still wrote March 12 with normal counts in all variants. Starting closed, explicit show under ancestor disable remained closed. |
| Outer editableProperty externally bound | Two lock/unlock cycles plus a final lock each delivered an editable bound-value exception to the FX thread's uncaught-exception handler, five per fixture. readOnly and the binding remained active. With source true, F9, arrow, explicit show and direct EXPAND opened and subsequent day selection each wrote March 12, with normal counts. With source false while locked, explicit show and targeted text mutation were blocked; changing back to true restored editing. |
| Bound-true locked editor commit | Targeted valid editing followed by a direct focus transfer wrote March 12 once. Targeted Enter on the editor also transferred focus and produced the same write by the time dispatch completed. This does not isolate Enter's own commit callback from the focus-loss callback. |
| Bound-true locked CalendarTime, no draft edit | Focus loss reparsed unchanged displayed 10.03.2026 13:42 and changed business-model seconds from :14 to :44 in the final run, with no date event (0/1/1). The minute mask does not display seconds and the converter supplies current seconds when parsing minute input. |
| Explicit forceCommitValue while locked | Invalid draft preserved value (0/0/0), valid draft changed the date, and empty draft cleared date/datetime/model once. The writable locked editor also accepted a direct programmatic force of a valid draft. A draft typed before a writable lock survived native focus loss with 0/0/0 and was then committed by a direct force call. A same-date CalendarTime explicit force changed only time/model to 18:25, 0/1/1. |
| Programmatic dates while locked and bound true | setValue(LocalDate), direct bound-model writes, Controls.setValue and, for CalendarTime, setDateTimeValue all propagated once. The date-only setter kept the existing 13:42 in CalendarTime; datetime paths carried 18:25 exactly. |
| Real ArrayDataSet and DSControlAdapter | All variants on locked, bound-true controls navigated March 9→March 12→null. Each row applied once in date/model and, where present, datetime. CalendarTime carried 09:15 and 18:25 exactly, then cleared both properties. |
| Diagnostic explicit hide immediately after writable lock | Closed the popup and left date/datetime/model untouched, 0/0/0. Subsequent explicit show remained closed under the existing noneditable policy. Displayed text could still be selected programmatically. |

All key and pointer input above was targeted JavaFX dispatch. Day cells were selected in the actual popup scene. Targeted mouse dispatch bypasses OS picking. EXPAND was a direct accessibility API call, and focus transfer, hide/show and explicit force were direct calls. Robot NOT RUN because exclusive Windows input was not established.

### SOURCE-PROVEN

- Both JInvCalendar constructors delegate readOnly to Controls.disableControl. The DatePicker branch calls setEditable without checking binding ownership and does not hide the popup. Both constructors install the keyboard filter, including the F4 release suppression and the F9 editable/non-ERROR condition. Only the no-arg constructor installs initEditor, the application DateConverter, and its extra focus hooks; the LocalDate constructor used the JavaFX LocalDateStringConverter in this fixture. CalendarTime inherits the no-arg setup and replaces the converter. createDefaultSkin asks BaseApp for the JInvDataPickerSkin feature when one exists, and otherwise returns the stock skin. There is no implementation of that feature in this checkout.
- JInvCalendar.show gates on isEditable; stock ComboBoxBase.show additionally checks native disabled. Arrow and accessibility expansion lead to show. JInvKeyboardManager can consume focused Down/Up for navigation; the observed Alt/arrow-key non-opening result has no unlocked opening positive for those routes. Stock DatePickerSkin configures editor editability, but DatePickerContent.selectDayCell directly calls setValue and hide from the popup; it has no check of JInvCalendar.readOnly. The independently dispatched popup action is why a control-node input filter alone cannot protect an already-open popup.
- DatePicker's native focus callback uses its final commitValue(), which checks editable. The stock popup-control skin has a distinct editor Enter conversion path. The editor's stock context menu comes from JavaFX text-input behavior when no context menu property is supplied; its actions and their actual dispatch/commit under lock were not exercised here. The accessibility open path was tested; editor accessibility mutation/commit and custom popup accessibility were not.
- forceCommitValue calls the converter even while locked or native noneditable. CalendarTime's converter can write dateTimeValue before returning a LocalDate, including for same-date time changes and empty input. ButtonValidationListener has a qualifying Scene mouse-press branch that calls forceCommitValue on a focused JInvCalendar after its validation/error branches, without checking readOnly there. The forced commit runtime checks were direct, while the full validation/form dispatch was source traced.
- CalendarTime synchronizes inherited value and dateTimeValue two ways. Date-only changes preserve an existing time; when there is no datetime yet, its non-null date path supplies current time. Binder binds dateTimeValue for CalendarTime, valueProperty for the date-only control. Controls.setValue dispatches CalendarTime before DatePicker. These data-flow paths must remain available and must not be used for user-origin filtering or post-notification rollback.

### Decision

**Recommend documenting the limitation.** Immediate hide on lock is a small mitigation for an already-open stock popup in the ordinary writable case. It can reuse the existing readOnly listener and needs no new control fields or listener objects. But moving hide into the listener would need to avoid the bound-setter exception, and guarding reopening with readOnly would change the existing successful explicit show behavior for a locked, externally bound editable-true control. Leaving show available still exposes the independently committing popup. Neither closure policy addresses bound editor/focus commits, the CalendarTime time-only conversion, or the separate validation/force route. A comprehensive solution would require coordinated entry-point policy, including BaseApp skin verification, and likely additional registrations/lifecycle handling. Exact retained bytes are unknown.

State that readOnly is not a data-integrity boundary for these calendars. Native disable alone was insufficient when the popup was already open; forms that rely on native disable should close an open popup first and validate programmatic flow in context. This audit proposes no production or skin edit.

### UNKNOWN

BaseApp-provided/external skin behavior, physical picking/keyboard, OS accessibility and editor context-menu gestures, actual validation callback in a running business form, full FXEntity/DB persistence, and copy to the system clipboard were not verified. The temporary fixture and local evidence are under ignored `target/ctrl012`. No tracked source or test files changed and no implementation branch push was made.

---

## Reply CTRL-013

**Status:** RED reproduced before the production edit. GREEN verified on Windows JDK `25.0.1+8-LTS-27` and JavaFX `25`.
**Production:** `migration/java25-correctness`, baseline `5fc050d452e32adf55407512c8e9b5e9cc13fa75`, verified final commit `2e8c11556690e49968c575fd13d00e0b998dbacc`.

The prior predicate called getValueChangedProperty, which initializes a SimpleBooleanProperty and stores it in Node.getProperties. Inspection of the installed JavaFX 25 Node bytecode confirmed that getProperties creates the observable map and backing HashMap on first access. hasProperties only tests the existing field and map emptiness. Repository callers include F7FilterDialog through ValidMan.

The production diff changes only Controls.isControlValueChanged. It returns false for a null node or one with no properties, then looks up CONTROL_VALUE_CHANGED and returns the existing BooleanProperty.getValue if present. No allocation, mutation, cache, listener registration or instance field is introduced by the predicate. The reserved key still accepts BooleanProperty values, and the existing ClassCastException for an unexpected non-null type is retained. getValueChangedProperty and setControlValueChanged were left unchanged. [Exact production and test diff](https://github.com/ssu95a/fore25/commit/2e8c11556690e49968c575fd13d00e0b998dbacc)

### RED/GREEN evidence

The tracked ValueChangedFlagSmoke used real Pane instances on the JavaFX toolkit thread without a Stage. It exercised the same assertions against original and changed Controls.

| Check | RED, exit 1 | GREEN, exit 0 |
| --- | --- | --- |
| Pristine Node hasProperties before/after first/after repeat | false/true/true; predicate result was still false | false/false/false |
| Node with one unrelated metadata entry | False, but inserted the flag with one map notification | False, flag absent, zero map notifications; unrelated metadata identity preserved |
| Explicit getter and setter identity/notifications | Stored the one property; true and false notified exactly once each, repeated true did not notify | Same; an explicit false setter on a fresh node still creates the property |
| Existing bound BooleanProperty | Same instance and binding; observed true→false→true, two source-driven notifications | Same, zero Node map notifications from reads |
| Null predicate | False | False |

The allocation conclusion for a pristine read follows from the executed predicate and the JavaFX hasProperties implementation, in addition to the no-mutation assertions. Exact allocated or retained bytes were not measured.

Maven -Pbuild compile, PowerShell parsing and git diff --check passed. CheckBoxReadOnly, ComboReadOnly, RadioReadOnly, NodeTraversal, DatePickerBinding, MoneyDecimal and DataBinding were freshly compiled and run against the Maven-built production classes. All seven exited 0. GUI tests used targeted dispatch where the existing probe exercises input; no Robot input was used.

### Independent Windows rerun

```powershell
git pull --ff-only origin migration/java25-correctness
git rev-parse HEAD
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ValueChangedFlag -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

Expect `CTRL-013 PASS` and exit 0.

---

## Reply CTRL-014

**Status:** BLOCKED by unsafe null-clearing behavior in the supported selection-to-DataSet path. No production commit or push.
**Production:** `migration/java25-correctness` remains at `2e8c11556690e49968c575fd13d00e0b998dbacc`. Mailbox base `51cba63`.

### RED and provisional candidate

Windows Java 25.0.1+8-LTS-27 / JavaFX 25, shown Stage on the FX thread. Programmatic API calls only. The no-arg and items constructors with A/B, and a mapped Entry→Integer fixture, all passed the direct setSelectedValue positive control with one selected-index, item, value and bound-model notification. Original Controls.setValue(Node,…) and Controls.setValue(Object,…) threw java.lang.RuntimeException: Unsupported class control: ru.inversion.fx.form.controls.JInvChoiceBox. A retained its selection and model, 0/0/0/0. RED process exit 1.

The provisional diff was a single branch in Controls.setValue(Object,Object): JInvChoiceBox delegates non-null to setSelectedValue(value), and calls native setValue(null) for null. Maven -Pbuild compile and PowerShell parsing passed. The provisional -ChoiceSetValue run exited 0 on ordinary lists: both constructors, mapped 22→Entry B, getValue returning Entry T, exactly 1/1/1/1 on changes, zero additional events for repeats and unmatched keys, null clearing to index -1, and readOnly programmatic updates. Existing direct setSelectedValue(null) remained a no-op. Generic DSControlAdapter with ArrayDataSet navigated mapped 22→11→null and a missing row, one notification per observable and change. Specialized Binder/DSChoiceBoxAdapter with only non-null items left its source cursor at row 1 on clear and still navigated afterward. These are provisional results for the tested lists, not acceptance of the complete fix.

### Blocking evidence

A separate JavaFX program used the real specialized Binder.bindChoiceBox/DSChoiceBoxAdapter and ArrayDataSet with contents [A, B, null]. B at index 1 was valid and selected, with the source cursor at row 1. All three routes reproduced the same transition in the actual runtime:

| Route | Selected index events | Final value/index | DataSet cursor |
| --- | --- | --- | --- |
| Native setValue(null) | 1→-1→2 | null / 2 | 1→2 |
| Native selectionModel.clearSelection() | 1→-1→2 | null / 2 | 1→2 |
| Provisional Controls.setValue(choice,null) | 1→-1→2 | null / 2 | 1→2 |

JavaFX ChoiceBox has a value-change listener that resolves items.indexOf(newValue) and selects the index when found. With a literal null item, clearing the value selects that item again. DSChoiceBoxAdapter reacts synchronously to selected-index changes by calling setCurrentRowNum. A second clear would be too late to prevent the cursor notification. This differs from a missing row on the **generic** binding path, where the ChoiceBox items are still [A, B].

The inbox requires STOP when clearing can cause unsafe navigation. The candidate and runner changes were removed, leaving tracked code and tests at baseline. Local evidence and fixtures are under ignored `target/ctrl014`, including `ChoiceSetValueSmoke.java`, `SpecializedNullDiagnostic.java`, `red.log`, `green.log` and `specialized-null.log`. The provisional -ChoiceSetValue runner is not present in the published branch. The baseline fixture can be compiled against the existing target/classes and the Maven test-scope classpath and run as ChoiceSetValueSmoke. Previously accepted regression modes were not rerun after the blocker; no production change was published.

A separate decision is needed on the contract for literal null items in a specialized ChoiceBox DataSet before publishing a fix that clears selection. No changes were made to JInvChoiceBox, Binder, adapters or readOnly logic.

---

## Reply — Controls.java follow-up

Verified workspace changes on migration/java25-correctness at baseline `2e8c11556690e49968c575fd13d00e0b998dbacc`.

- Column titles: the original implementation threw NullPointerException on a detached JInvTableColumn and ClassCastException in a stock TableView. It also fell back to the ID when an empty text property had a visible Label graphic. The fix consults the controller only for JInvTable, handles empty text and missing title, and keeps existing attached-table title/tooltip precedence.
- Non-control components: the writable getter allocates only when its list is missing and still returns the same live mutable list. Binder and radio initialization use findNonControlComponents. A read of an empty container leaves its properties untouched, and reads of populated containers return the same stored list.
- Bounds and scrolling: identity conversion returns the supplied bounds; an unrelated ancestor produces IllegalArgumentException with a specific message. scrollToNode avoids moving the viewport for the content root or an already visible target, and accounts for both ends of the target and the configured vertical range.

Windows Java 25 / JavaFX 25. Maven -Pbuild compile and git diff --check passed. All 14 focused assertions passed, including nested up/down scrolling in a shown ScrollPane with vmin=10/vmax=30. CTRL-003 and GUI-009 passed. CTRL-006 initially failed the mixed-group unlocked-arrow check in a direct run. The complete official -RadioReadOnly rerun passed with exit 0.

The code changes are local to the workspace. This mailbox commit publishes the report. CTRL-014 and its null DataSet navigation blocker are separate.

---

## Reply CTRL-CONTROLS-FOLLOWUP

- Controls: getTitleFromTableColumn handles detached/stock columns and graphic/tooltip fallbacks. The non-control component getter preserves the live mutable list and allocates only on first use; Binder and AbstractBaseController use the non-creating lookup. Bounds conversion handles identity and rejects unrelated ancestors. Scrolling uses the nearest containing ScrollPane and its vertical range. Controller lookup and traversal avoid intermediate work while retaining property identity and traversal order.
- Production commit: `8a993e3e4aa7b24611902ac8fa8380288586b939` on `migration/java25-correctness` (verified fast-forward, local and remote HEAD match). Tracked `ControlsFollowupSmoke` via `-ControlsFollowup`. After publication, `git status --short --branch` showed a clean production checkout in sync with origin.
- PASS: Maven `-Pbuild compile`, PowerShell parse, `git diff --check`, and `-ControlsFollowup`. NodeTraversal, DataBinding, ValueChangedFlag, DatePickerBinding, CheckBoxReadOnly, ComboReadOnly and MoneyDecimal passed. RadioReadOnly passed the separate official rerun after a mixed-group unlocked-arrow failure in the direct batch.
- Remaining: CTRL-014 stays BLOCKED and its setter experiment is absent. Full business forms, DB and physical input were not run. No further follow-up changes are pending.

---

## Reply CTRL-015

**Status:** DONE. **Production:** baseline `8a993e3e4aa7b24611902ac8fa8380288586b939`, final `47c31310fac24692dc1b7b3118a127825de52a01` on `migration/java25-correctness`. [Exact diff](https://github.com/ssu95a/fore25/commit/47c31310fac24692dc1b7b3118a127825de52a01)

DSChoiceBoxAdapter now registers each listener once and restores the prior synchronous guard in finally. The original exception still reaches the JavaFX thread handler. No fields, handlers, API, or null-policy changes.

Windows JDK 25.0.1 / JavaFX 25, shown Stage, programmatic selection and navigation with real ArrayDataSet dispatch. The test subclass counted registration attempts and injected one exception before delegating a cursor write. The installed AbstractDataSetBase delegates listener registration to ListenerManBase, which deduplicates by object identity and dispatches synchronously.

| Scenario | RED | GREEN |
| --- | --- | --- |
| Row/navigation registration attempts | 2/2 | 1/1 |
| A→B→A and repeated B selection | Three adapter navigation callbacks for three DataSet events; one index and value change each, zero extra on repeat | Same |
| INSERT AFTER_CURRENT | One DataSet row event, one adapter callback and one ChoiceBox list addition (C) | Same |
| Navigation after failed selected-index cursor update | DataSet reached index 1 (C), ChoiceBox stayed at index 2 (B), zero selection/value events | DataSet and ChoiceBox reached index 1 (C); one selection/value event each; subsequent selection still navigated |
| Literal null item in A/B/null | Direct ChoiceBox.setValue(null) selected index 2 and moved cursor to row 2, one navigation event | Same. CTRL-014 remains BLOCKED |

Separate pre-existing observation during fixture setup: on this dependency, INSERT LAST with cursor at 0 for [A,B] appends C but reports event index 1, and the adapter's added sublist consequently contains B. Installed AbstractDataSetBase bytecode uses currentRow+1 on that append path. This is outside CTRL-015; the verified test uses AFTER_CURRENT while preserving the independent observation.

Maven `-Pbuild compile`, PowerShell syntax and diff checks passed. ControlsFollowup, ValueChangedFlag, CheckBoxReadOnly, ComboReadOnly, NodeTraversal, DatePickerBinding, MoneyDecimal and DataBinding passed. RadioReadOnly failed the mixed-group unlocked-arrow check in the first batch and first standalone run, then passed in paired JavaFX runs with both original and fixed adapters. The check was intermittent, with no reproduced difference attributable to this edit. No Robot, DB or full business-form run.

Independent Windows rerun:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ChoiceAdapter -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

---

## Reply CTRL-016

**Status:** READ-ONLY audit complete. CTRL-014 remains BLOCKED on the null policy. **Base:** `47c31310fac24692dc1b7b3118a127825de52a01`, verified local and remote after published DONE CTRL-015. Windows Java `25.0.1+8-LTS-27` / JavaFX `25`; actual controls, FX thread, direct programmatic operations. No synthetic or physical input was needed. Probe and exact event log are under ignored `target/ctrl016`.

**CONFIRMED runtime.** Started each operation at B/index 1. Counts are selected-item/value/model notifications; index events and DataSet navigation are listed separately. Plain fixtures bind a T-valued property. The FXEntity fixtures use the actual production Binder and an annotated entity with an Integer field R.

| Fixture / operation | Index transitions | Item/value/model changes | DataSet navigation / exception |
| --- | --- | --- | --- |
| A/B, positive non-null selection; structured key 2 → Row B | 0→1 | 1/1/1; Controls.getValue returns T | 0→1 for specialized adapter; no exception |
| Any fixture, direct setSelectedValue(null) | none, stays 1 | 0/0/0 | none |
| A/B, native setValue(null), clearSelection or select(-1) | 1→-1 | 1/1/1 | Specialized cursor stays 1, with no navigation event |
| A/B/null and structured A/B/null, same three clear operations | 1→-1→2 | 1/1/1 | Specialized cursor 1→2 once, no exception without mapped FXEntity binding |
| Explicit select(2) or valid DataSet navigation to literal null row | 1→2→-1→2 | 1/1/1 | One cursor event 1→2 when specialized |
| Valid DataSet navigation to A; invalid DataSet -1/99 or selection 99 | A: 1→0; invalid: none | A: 1/1/1; invalid: 0/0/0 | A: 1→0 once; invalid: no event, cursor unchanged |
| Mapped FXEntity with Row::key, non-null in both directions | A↔B once per call | 1/1/1 per direction, key 1↔2 | No exceptions or duplicate model notification |
| Mapped FXEntity with Row::key, A/B, native setValue(null) and clearSelection | 1→-1 | 1/1/0, stale key 2 | One NullPointerException through FX thread handler |
| Same strict FXEntity plus specialized A/B/null, native clear or DataSet nav to null | Clear 1→-1→2; navigation 1→2→-1→2 | 1/1/0, stale key 2 | Cursor 1→2 once; one NullPointerException |
| Same specialized fixture with null-aware mapper | Same index and cursor transitions | 1/1/1; model becomes null | No exception; cursor still moves to row 2 |
| Mapped FXEntity receives model key null; selected non-null item can itself project null | Key write leaves selection/cursor unchanged. Explicitly selecting a non-null item with a null key keeps that item selected. | Key change emits one model event in each case | No exception |

Additional sentinels: with [A,null,B] and Row::key, a scan for matching key 2 or unmatched key 99 threw before reaching the tail, with zero selection/model/navigation events. Production Controls.setValue still threw Unsupported class control for both 2 and null.

**SOURCE-PROVEN.** Controls.getValue exposes the control value T, with its existing empty-string normalization. The setter has no ChoiceBox branch. setSelectedValue scans for raw item equality or projected R equality and skips null input entirely; a scan can call the mapper on a null list element. FXEntity Binder uses bidirectional T binding without a factory; with a factory it projects cb.getValue unconditionally and calls setSelectedValue on model changes. The DataSet Binder overload constructs DSChoiceBoxAdapter rather than installing that FXEntity binding. Combined tests deliberately applied both APIs. JavaFX clears then reselects a matching null item via value/index synchronization. DSChoiceBoxAdapter forwards index changes to setCurrentRowNum; installed ArrayDataSet rejects negative and out-of-range indexes and accepts index 2 even when its row is null. Hence absence of choice (-1), valid null row (2), and null projected key are separate observable states.

**Policy options (not implemented):**

| Option | Compatibility and integrity impact |
| --- | --- |
| A. Forbid literal null items in specialized DataSet | Removes cursor selection of null by clearing, but rejects data currently accepted by ArrayDataSet and may need validation/migration for external forms. A null value still reaches strict FXEntity mappers when clearing. |
| B. Keep literal null as a selectable row | Preserves explicit navigation and its cursor event. A generic clear must be distinguished from row selection; projected-null policy is still needed to avoid stale model keys. |
| C. Allow generic clear only without literal null | The A/B runtime showed no cursor event. A guard can protect A/B/null from accidental navigation, but strict Binder still fails on A/B clear, and callers need a defined outcome when clear is refused. |
| D. Explicit no-selection operation at the adapter boundary | Can express intent without overloading a selected null row. Needs an explicit cursor/model notification contract and a supported way to avoid JavaFX reselecting the null row. API/lifecycle changes and migration risk are larger. |

**Recommended next RED/GREEN task, conditional on approval:** split out a non-null-only Controls.setValue dispatch for supported JInvChoiceBox lists, delegating to existing R mapping while preserving the currently unsupported generic null call. Verify one notification per real transition and zero for identical/unmatched keys on valid fixtures; include a literal-null-before-match sentinel so existing mapper failure is visible. Keep generic clear BLOCKED. This is a proposed separate scope, not approval to edit.

**Architectural question:** should a future generic null write preserve the current selection and cursor, or clear selection and the model key while keeping the cursor, reserving literal-null row selection for explicit index/navigation?

**HYPOTHESIS / UNKNOWN:** prevalence of literal null items and strict factories in external consuming forms, and real DB/persistence effects, remain unknown. The in-memory FXEntity result establishes property behavior, not a database write. Production and tracked tests were untouched in CTRL-016.

---

## Reply CTRL-017

**Status:** DONE. **Production:** base `47c31310fac24692dc1b7b3118a127825de52a01`, final `4e7ee03b6a85fac0e09b4a2d73d7cfce36e88eca` on `migration/java25-correctness`. [Exact diff](https://github.com/ssu95a/fore25/commit/4e7ee03b6a85fac0e09b4a2d73d7cfce36e88eca)

Only the ChoiceBox-to-FXEntity listener in Binder changed. It sends a null key without invoking the factory for a null ChoiceBox value. Existing non-null projection and model-to-control handling remain intact. No new listeners or instance state.

Windows Java 25.0.1 / JavaFX 25, shown Stage, actual annotated FXEntity and production Binder, strict Row.key mapping. Direct programmatic calls; no Robot.

| Operation | RED | GREEN |
| --- | --- | --- |
| A/B native setValue(null) and clearSelection, starting at B | Each index 1→-1, one item/value change, key stays 2, zero model events, one null mapper call and one NPE delivered to the FX thread handler | Same index/item/value; one model event 2→null, no mapper call or exception. Repeat null gives zero extra events. |
| Specialized A/B/null DataSet navigation to row 2 | Index 1→2→-1→2, one cursor event, one item/value change, stale key 2 and one NPE | Identical index/cursor/item/value transitions; one key event, no null mapper call or exception. |
| Non-null model/key and control mapping | Positive in both directions; real control→model writes projected once, independently of the existing reverse scan. Unmatched key on A/B retained selection. | Same, including recovery after null. Model→control key null still leaves the selection as before. |

Maven -Pbuild compile, PowerShell parse and git diff --check passed. BinderChoiceNull and nine direct regression probes passed. RadioReadOnly failed the initial unlocked-arrow check and then passed with both original and fixed Binder in a paired rerun. Full business forms and DB persistence were not run. Generic Controls setter, literal-null/cursor policy and CTRL-014 remain unchanged.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -BinderChoiceNull -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

---

## Reply CTRL-018

**Status:** DONE. **Production:** base `4e7ee03b6a85fac0e09b4a2d73d7cfce36e88eca`, final `11b1641e12efaa45cb82a8ec58f530b028f5acf0` on `migration/java25-correctness`. [Exact diff](https://github.com/ssu95a/fore25/commit/11b1641e12efaa45cb82a8ec58f530b028f5acf0)

JInvChoiceBox.setSelectedValue now skips literal-null list entries before comparing or projecting a non-null key. Public signatures, mapped/unmapped equality, null-key no-op, cursor policy and CTRL-014 are unchanged. No new allocations or per-instance state.

Windows Java 25.0.1 / JavaFX 25, real controls on shown Stage. Both constructors tested, direct programmatic operations only.

- RED: positive A/B lists selected B once. With A/null/B, both mapped key 2 and plain B threw before B; unmatched keys also threw. Each failed lookup had zero selected-index/item/value/model notifications and retained A.
- GREEN: both paths select B after the literal null, with 1/1/1/1 notifications. Repeating B, unmatched lookup and null-key no-op emit 0/0/0/0. A non-null row projecting null is ignored for a non-null search key.
- Explicit literal-null selection with specialized ArrayDataSet still selects index 1 and moves the cursor to 1, with one navigation event. The existing BinderChoiceNull regression with a real FXEntity still passes, including null-row navigation. No change to Controls, Binder or DSChoiceBoxAdapter in this commit.

Maven -Pbuild compile, PowerShell parse and diff check passed. ChoiceNullLookup and BinderChoiceNull passed through their Windows runner modes. ChoiceAdapter, ControlsFollowup, ValueChangedFlag, DataBinding, CheckBoxReadOnly, ComboReadOnly, MoneyDecimal, NodeTraversal, DatePickerBinding and the official RadioReadOnly run passed. No physical-input, full forms or DB runs.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ChoiceNullLookup -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

---

## Reply CTRL-019

**Status:** DONE. **Production:** baseline `11b1641e12efaa45cb82a8ec58f530b028f5acf0`, final `a4edd28f2d68e461c676e01a5a72880d8929cd54`. [Exact diff](https://github.com/ssu95a/fore25/commit/a4edd28f2d68e461c676e01a5a72880d8929cd54)

Only Controls.setValue(Object,Object) gained a non-null JInvChoiceBox branch delegating to setSelectedValue. The Node overload shares that implementation. Null still throws the exact previous RuntimeException, and Controls.getValue returns T.

Windows Java 25.0.1 / JavaFX 25, programmatic operations on the FX application thread. RED: both overloads threw Unsupported class control for valid non-null String and mapped keys, with 0/0/0/0 index/item/value/model events. Direct selection passed. GREEN: both constructors and both overloads select B once (1/1/1/1), including a list containing a literal null and a non-null row projecting null. Repeated/unmatched keys add zero notifications. Initial selection is empty in the fixtures. The specialized A/B and A/B/null fixtures kept cursor 1 with zero navigation, selection or model events on rejected null writes. Direct setSelectedValue(null) remained a no-op.

Actual generic DSControlAdapter and ArrayDataSet navigated using keys 2 and 1 with one transition per row. Generic null navigation remains unsupported; not driven through the adapter's error UI. CTRL-014 stays BLOCKED.

Maven -Pbuild compile, PowerShell parse and diff check passed. ChoiceNonNullSet and all twelve regression probes passed, including BinderChoiceNull, ChoiceNullLookup, ChoiceAdapter and the official RadioReadOnly run. Robot and DB not run.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -ChoiceNonNullSet -SettingsFile "D:\Java\.m2\settings.xml"
$LASTEXITCODE
```

---

## Reply CTRL-020

**Status:** DONE / NO CHANGE. **Production base and final SHA:** `a4edd28f2d68e461c676e01a5a72880d8929cd54`.

The unsupported ChoiceBox editor conversion is reproducible as a **direct API call**. It is not reached by the current F7 expression switch. The only source call sites are inside the legacy Boolean ExpressionChangeListener, whose installation in generatePaneByMapFilterItems is commented out. Installed bytecode creates only F7ExpressionChangeListener, which toggles visibility/focus and does not copy a value via Controls. The inline F7FilterTextField toggle changes its own value type.

Bound JInvChoiceBox can pass the info-control filter, but createFilterItemFromControl copies field metadata and factoryList. Only JInvComboBox, radio and checkbox inputs get a replacement item control there. initMainControl uses a supplied item control, list wrapper, typed default or F7 text editor. An external filter modifier can supply a custom control, but still uses the active visibility-only switch. No repository F7 customization supplying JInvChoiceBox was found. JInvCheckBoxString is wrapped as a JInvComboBox using checked/unchecked strings; the inactive listener also explicitly skips a direct CheckBox.

| Check | Evidence |
| --- | --- |
| TextField/DatePicker/ComboBox direct supported conversions | PASS, text transfer preserved draft and returned displayed date / selected String in the respective probes |
| ChoiceBox direct conversion | Unsupported class control; no real F7 route established |
| CheckBoxString direct conversion | Selected true returns editor string true; its distinct configured control value is Y |
| Full stock F7 dialog switch | Runtime UNKNOWN. F7FilterTextField initialization requires BaseApp view preferences, absent in the standalone JavaFX fixture. No artificial listener was installed. |

Windows Java 25.0.1 / JavaFX 25, direct API probes on FX thread, ignored target/ctrl020. No production/test changes. No full-app notification counts or DB/filter query execution measured. StringConverter.toString(T) is display text, TypeConverter.convertToString(T) is the existing generic ComboBox extraction, and projected R is its business key. No active F7 transfer establishes any of those as the right ChoiceBox representation. Recommend no change.

---

## Reply CTRL-021

**Status:** DONE (test-only). **Base:** `a4edd28f2d68e461c676e01a5a72880d8929cd54`. Final branch HEAD `ce32f72ccc4a03ccc09f959da9b9d2e77d00f993`. [Exact test-only diff](https://github.com/ssu95a/fore25/commit/ce32f72ccc4a03ccc09f959da9b9d2e77d00f993)

Windows Java 25.0.1 / JavaFX 25, targeted key dispatch, no Robot. Official baseline fresh processes #1–5 exited 0; #6 exited 1 at mixed group unlocked arrow, with the locked checks passing. The ten timing-instrumented natural runs exited 0; several nevertheless recorded a missing stock skin at the first locked gesture, followed by installation before the positive check. Logging may have affected timing.

In an ignored diagnostic, executing the existing mixed-group sequence in one FX turn reproduced the exact unlocked assertion failure with stock skin=null. Window active, stock focus owner, both toggles enabled, both unlocked at the positive, selection stayed stock. The paired run changed only CSS initialization, installed RadioButtonSkin, and the same Right selected Legacy; both prior locked checks still retained stock. JavaFX bytecode shows RadioButtonSkin creates ToggleButtonBehavior, whose Right mapping calls ToggleGroup.selectToggle. Production's group filter still intercepts locked traversal, but a missing stock skin otherwise makes a locked negative vacuous. The deterministic boundary probe reproduced the same before/after result.

The tracked fix applies CSS immediately after attaching the mixed-group nodes and asserts both skins are installed before the gestures. It preserves targets, key events and every assertion, adds no sleeps and changes only RadioReadOnlySmoke.java. Production source unchanged. All ten fresh official fixed runs exited 0. DataBinding, ChoiceNonNullSet and ControlsFollowup also passed through their official modes. Maven compile and git diff --check passed. Physical input was not tested.

Windows recheck: `-RadioReadOnly -SettingsFile "D:\Java\.m2\settings.xml"` on Test-LegacyGuiSmoke.ps1. Diagnostic sources and per-run logs remain ignored under target/ctrl021. Worktree clean after publication.

---

## Reply CTRL-022

**DONE (test-only).** Implementation HEAD and GitHub match at `a5901c251ce76a122a31e8c832b3199f33735c24`; clean worktree. Added only LegacyFormCancelSmoke.java and the -FormCancel runner. The fixture adds standard btCancel to the real FXML, which ships only btOK.

| Route | Consumed requests | Observed lifecycle | Hidden / cleanup / RET_CANCEL callback |
| --- | --- | --- | --- |
| Cancel | false | ON_CANCEL → ON_CLOSE | 1 / 1 / 1 |
| Window close request | false | ON_CANCEL → ON_CLOSE | 1 / 1 / 1 |
| Veto, then Cancel | true, false | ON_CANCEL → RUNTIME → ON_CANCEL → ON_CLOSE | 1 / 1 / 1 (all zero after veto) |
| closeNow | false | ON_CLOSE | 1 / 1 / 1 |

Windows Java 25.0.1 / JavaFX 25 PASS for FormCancel, FormLifecycle (GUI-010), DataBinding (GUI-009), ControlsFollowup and RadioReadOnly. Maven compile, PowerShell parser and staged diff check passed. Physical window-decoration input and singleton registry remain UNKNOWN. Rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -FormCancel -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-023

**DONE / READ-ONLY.** Implementation HEAD verified unchanged at `a5901c251ce76a122a31e8c832b3199f33735c24`; worktree clean.

**CONFIRMED (Java/JavaFX 25):** public Binder.bind with real FXEntity, three stock TextFields and controller/ViewContext. All-valid baseline propagated both ways: three model notifications for two editor writes and one model write, nine text notifications. Invalid middle property produced the Binder wrapper exception with cause IllegalArgumentException: Not found property 'missing'. First and third remained live both ways: three model and six text notifications, middle field unchanged. Fixing metadata and retrying restored all three with counts 3/9, no duplicate observable changes on this route. This does not count internal listener registrations or cover validator retry.

**SOURCE-PROVEN:** Binder continues after per-control exceptions, binds non-control components and then throws. AbstractBaseController.internalInit exits before subsequent configuration; SDI and embedded initialization handlers call close() on failure. **UNKNOWN:** corporate form reuse after error, custom caller cleanup, validation/LOV retry duplication; retention beyond the caller lifetime is a hypothesis.

Fail-fast metadata prevalidation adds another pass and covers only detectable setup errors; best-effort with documented partial state preserves existing propagation; transactional rollback requires recording every attachment and reversals across validation, adapters and LOV, with higher complexity and memory cost. No contract evidence justifies changing behaviour. No tracked changes.

---

## Reply CTRL-024

**DONE.** Implementation HEAD and GitHub match at `7282d378cf144d15bfb61b0cf944def96cbb577b`; clean worktree. Production diff is one guard in SceneKeyManager.getActionRecursive before reading CONTROL_PARENT. Added SceneKeyLookupSmoke.java and -SceneKeyLookup.

RED exit 1 on original implementation after successful action assertions: stock focused button and inner VBox changed from no properties map to allocated empty maps during F7 lookup. hasProperties remained false/false because those maps were empty; direct read of the private backing field in the test distinguishes absence from emptiness without initializing it. GREEN exit 0 in three fresh processes: both maps remained absent, metadata map identity preserved. Identical actions before/after: [nearest, logical, global, release, handler, handler]; target events [F7 release, F8 press, F7 press, F7 press]. The nearest filter consumed before reaching the target, logical override won over the physical parent, and disabled/security-blocked filter candidates left dispatch to the handler. No duplicate calls.

Maven compile, PowerShell parser, staged diff check, GUI-009, GUI-010, RadioReadOnly and ControlsFollowup passed. Rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -SceneKeyLookup -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-025

**DONE / READ-ONLY.** Implementation HEAD verified unchanged at `7282d378cf144d15bfb61b0cf944def96cbb577b`; worktree clean. Java/JavaFX 25 probe ran with an attached Stage, real JInvTextField behaviour and bidirectional StringProperty, and a minimal AbstractLovBase provider with the real value property/default check. A test BaseApp subclass supplied view preferences only.

**CONFIRMED runtime:** attaching a prepopulated LOV left the model unchanged; a subsequent programmatic change produced one text and one model notification. ACCEPT produced one LOV, one text and one model notification before one true callback; CANCEL produced only one false callback, including two repeated cancellations. Equal distinct String caused one update attempt but no value/text/model change notification; same reference caused no attempt. Focused selection replaced `tail` with `chosen` in `base,tail`; append quoted `x,y` and notified the model once. Equal-value validation short-circuited. With the default AbstractLovBase predicate, validateOnOk passed doSetValue=false but still mutated LOV, text and model once; production providers with their own predicates may behave differently.

**CONFIRMED data-integrity issue:** changing an old LOV after replacement appended the active LOV's value again (one extra text/model notification). Registering the same LOV again caused two appends and two text/model notifications for a single change. A later write to that LOV after unbind raised two listener NullPointerExceptions and zero model writes.

**SOURCE-PROVEN:** JInvTextField registers an invalidation listener capturing the field and reading current getLOV(); replacement/unbind does not remove it. A strongly reachable former LOV can therefore retain the field through its property and listener; application-level retention duration is unmeasured. JInvEntityLov maps RET_OK to setEntityValue before its callback, and cancel skips the value assignment. JInvTextField forwards the callback without a second text write. JInvLOVButton forwards navigation on acceptance, restores focus on cancel. ButtonValidationListener marks a click on the focused field's own LOV button as opening a choice dialog; LovValidator forwards the doSetValue flag to the provider. **UNKNOWN:** native modal/DB flow and end-to-end button/focus replay in a corporate form.

One scoped follow-up: manage a lazily created JInvTextField LOV listener, detach it from the former value property on replacement/unbind, and keep only one registration on same-LOV reattachment. Preserve selection, callback and validation semantics. No tracked changes.

---

## Reply CTRL-026

**DONE.** Implementation HEAD and GitHub match at `0c3af0c77f4c056314e268764184e857b4c423b8`; clean tree. Changed only JInvTextField.java, new LovListenerLifecycleSmoke.java and Test-LegacyGuiSmoke.ps1.

RED → GREEN on Java 25 / JavaFX 25: former-provider write 1 → 0 model changes; same-provider reattach 2 → 1 append/model changes; detached-provider write 2 → 0 NPEs. Other observers remained active, including another field sharing the provider. Initial attachment left model unchanged; accepted choice updated once, cancel twice delivered two callbacks with no writes, equal replacement generated zero observable changes, selected text replacement and comma quoting passed. Field holds one nullable reference; callback object created once at first successful LOV attachment and reused. No eager object allocation on fields without a LOV.

Direct lovProperty().set bypasses listener setup, as before. The former listener ignores writes while the property points elsewhere; supported setter/unbind performs detachment. Direct property ownership was kept outside scope. Native DB modal not run. Maven compile, PowerShell parse, staged diff check, TextFields, DataBinding, FormCancel and BinderChoiceNull PASS. Windows: `src/test/probes/Test-LegacyGuiSmoke.ps1 -LovListenerLifecycle -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-027

**DONE.** Implementation HEAD verified at `2390e7cdac4a463b1be24082b6502df0e54a4aa7`, clean worktree. Changed only AbstractLovBase.createDefaultCheck, new LovDefaultValidationSmoke and runner mode. Default false returns success without assignment, true retains assignment. No per-instance state; equality shortcut, entity/custom predicates unchanged.

RED exit 1: direct checkValue(false) and LovValidator.validateOnOk each emitted one LOV, text and model change for both distinct text and null. GREEN exit 0: all four emitted zero and retained all values; direct true and normal validation each emitted one of each, equal values zero. Replacement and unbind passed. Windows Java/JavaFX 25; Maven compile, PowerShell parse, staged diff check, LovListenerLifecycle, TextFields, FormCancel, DataBinding PASS. Rerun `src/test/probes/Test-LegacyGuiSmoke.ps1 -LovDefaultValidation -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-028

**DONE / ARCHITECTURE DECISION NEEDED (READ-ONLY).** HEAD unchanged at `2390e7cdac4a463b1be24082b6502df0e54a4aa7`. Temporary attached JavaFX 25 probe with real JInvLOVButton and controlled provider passed.

Field-only delegates to the editor provider: cancel no write/TAB, accept updates the field and emits one synthetic TAB. Provider-only calls its own provider with a null filter. Field → provider switches the handler/getters to standalone, but the button still holds the old field in inherited metadata and disable/visible bindings still follow it. Provider → field replaces the provider and delegates through the field. Reattaching the same or a different field raised RuntimeException (bound disable value cannot be set); on distinct reattach the action pair still points to the former field while inherited metadata points to the new one.

Source: JInvChoiceButton configures the icon; JInvLOVButton owns the action pair and bindings, inherited JInvButton.setTextField writes separate metadata and may set disable directly. Internal TextField/Area/CUR and help-controller wiring use field mode. Provider constructors implement standalone mode. ButtonValidationListener recognises the reporting field for validation deferral. Native modal/DB and real corporate mouse replay remain UNKNOWN.

Recommend keeping two exclusive modes unless a caller establishes a combined-mode contract. A focused future task can make mode transitions consistent by releasing old bindings/metadata and testing reattachment atomically. Retention through live button metadata/bindings is confirmed; lifetime in the corporate app is unmeasured. No tracked changes.

---

## Reply CTRL-029

**DONE / READ-ONLY.** HEAD unchanged at `2390e7cdac4a463b1be24082b6502df0e54a4aa7`. CONFIRMED via real FXFormLauncher, FXML, SDI Stage and annotated JInvFXFormController in a temporary no-DB fixture. First launch reached RUNTIME; second launch was suppressed before veto. Sending WINDOW_CLOSE_REQUEST to a controller returning false kept its Stage visible with zero callbacks. Launching again opened a second visible Stage of the same class. Accepting close on both and reopening worked; three total close callbacks after closing the last Stage.

SOURCE-PROVEN: internalShow registers by class before controller initialization and brings an existing Stage forward. internalOnClose finally removes by class on normal close, consumed Cancel/WAIT, validation/onOK rejection and exceptions alike. closeNow bypasses veto; SDI finalization happens on WINDOW_HIDDEN, whereas the no-Stage branch finalizes inline and launcher MDI creation is commented out. Launcher catch also removes on initialization failure. With duplicates, removing by class can remove another controller's registration (source implication; untested). Corporate DB, WAIT/kill-session, exception and MDI runtime remain UNKNOWN.

Recommend a separate fix that preserves registration on consumed veto, removes only the matching owner on accepted close, and covers init/close errors explicitly so failed launches cannot strand a registry entry. Preserve distinct SDI and no-Stage finalization paths. No tracked changes.

---

## Reply CTRL-030

**DONE / READ-ONLY.** HEAD unchanged at `2390e7cdac4a463b1be24082b6502df0e54a4aa7`. Temporary Windows JavaFX 25 Stage with real JInvTextField, counting validators, observing ValidMan subclass and a minimal view preference service.

CONFIRMED: single registration validates once on blur. Two registrations of the same instance validate twice within one focus dispatch. Disabling registration through unbind removes both copies; focus dispatch still comes from the first manager. A second manager can explicitly validate but receives zero automatic focus dispatches, even after the first manager's validator is unbound. Six controlled text edits yielded six model notifications. Unbound no-LOV control retained its metadata contents and no dirty marking. LOV reattach left one LovValidator; former provider gave zero writes, current provider validated on blur and validateOnOk did not mutate.

CONFIRMED independent defect: when a validator instance was registered on two controls, unbinding it from one removed it from both. Source: getValidators backing removal uses only validator equality, while registration keys by control and validator. Recommend one focused task to constrain removal to the requested control and preserve duplicate-add semantics.

SOURCE-PROVEN: first focus/change listeners capture the manager and the node Boolean marker prevents any later manager installing replacements; list unbind does not remove the listener. Controller initialization is the usual manager owner; the scene's ButtonValidationListener also captures it. Corporate reuse/retention, DB and click replay remain UNKNOWN. No tracked changes.

---

## Reply CTRL-031

**DONE / TEST-ONLY.** Published and verified clean implementation HEAD `3805ac5ba9096de48fc9697014d1d8063805dd29`. Added only LovIntegrationSmoke.java and runner mode. On Windows Java 25 / JavaFX 25 real Stage, JInvTextField, AbstractLovBase provider and LovValidator, accept produced one LOV/text/model event before its callback; cancel/equal produced zero; reattach caused one quoted append, former/detached writes only notified provider observers, no NPE. validateOnOk emitted zero writes and normal validation one per property. Real JInvLOVButton.fire() emitted exactly one synthetic TAB after accept and none on cancel. Physical mouse input, native DB modal and corporate validation replay remain UNKNOWN.

Maven compile, PowerShell parse, staged diff check and all nine modes passed: LovIntegration, LovListenerLifecycle, LovDefaultValidation, FormCancel, FormLifecycle, TextFields, DataBinding, SceneKeyLookup, ControlsFollowup. Windows rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -LovIntegration -SettingsFile "D:\Java\.m2\settings.xml"`.

| Task | Status | Implementation change / finding |
| --- | --- | --- |
| CTRL-026 | DONE | `0c3af0c77f4c056314e268764184e857b4c423b8` — listener ownership |
| CTRL-027 | DONE | `2390e7cdac4a463b1be24082b6502df0e54a4aa7` — default read-only validation |
| CTRL-028 | DONE / architecture decision | Button mode transition can retain prior bindings and reattach can fail |
| CTRL-029 | DONE / read-only | Real SDI launcher opened duplicate singleton after veto |
| CTRL-030 | DONE / read-only | Validator unbind can remove another control's registration; first manager retains automatic focus routing |
| CTRL-031 | DONE / test-only | `3805ac5ba9096de48fc9697014d1d8063805dd29` — integration smoke |

---

## Reply CTRL-032

**DONE.** Verified implementation HEAD `cad4dfd39cda4cb1f23bb8610d7ab24c00123bb5`. Real launcher + annotated FXML controller: before veto duplicate suppressed; RED after veto 2 visible singleton stages, 0 callbacks; GREEN 1 visible, 0 callbacks. Accepted Cancel and closeNow each allow reopen, with one callback and resource cleanup per closing controller (3 total across the complete sequence).

Only AbstractBaseController.java, new SingletonCloseVetoSmoke and runner changed. Registry release is by identity, after accepted SDI WINDOW_HIDDEN or inline completion, and on launcher/close error paths. Veto and validation rejection preserve the registration. No instance fields. Init failure/rejected OK runtime deferred to matrix if feasible; WAIT/DB/MDI UNKNOWN. Maven compile, PowerShell parse, staged diff check, SingletonCloseVeto, FormCancel, FormLifecycle and DataBinding PASS. Windows rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -SingletonCloseVeto -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-033

**DONE.** Verified implementation HEAD `562b9a917a6c708dd6f18cb10112a6aca3d7653f`. RED in both A/B orders: after unbinding one control, shared validator invoked zero times on the other. GREEN: one invocation on the other, distinct validator preserved on removed control, one model event per edit; same-control duplicate positive still validates twice before removal.

ValidMan.getValidators now scopes removal by control and validator. No instance state or changes to focus dispatch. Added ValidatorScopedUnbindSmoke and runner mode only. Java/FX25 Maven compile, PowerShell parse, staged diff check, ValidatorScopedUnbind, LovIntegration, LovDefaultValidation, FormCancel and DataBinding PASS.

---

## Reply CTRL-034

**BLOCKED (local API ownership).** Verified clean HEAD `562b9a917a6c708dd6f18cb10112a6aca3d7653f`; no production/test commit. JavaFX 25 real Stage field-only accept, cancel and standalone positives passed. RED exit 1: same and distinct reattach each threw bound-disable exception. On distinct call action pair held former field while inherited metadata held the new field, both Boolean properties still bound. RED probe and log preserved under ignored target/ctrl-mission.

A blanket unbind before super would also remove binding owned by an external caller. In production JInvTextField.setLOV itself replaces the button's disable binding with a readOnly-aware binding. JavaFX BooleanProperty exposes bound status but no public source identity; a marker in the button cannot detect a subsequent external rebind. Superclass writes metadata and may assign disable directly. Need explicit ownership/transfer policy or permission to make superclass attachment binding-safe before changing behaviour; mixed provider mode remains out of scope. Independent singleton, validator and read-only tasks can continue under protocol.

---

## Reply CTRL-035

**DONE / TEST-ONLY.** Verified HEAD `aba018ab2e14bab5c774668d3ab9b74d32ae7fa1`. Extended the CTRL-032 fixture and added -SingletonLifecycleMatrix. Real SDI launcher: veto and rejected onOK preserve one visible owner, no callback or cleanup. Accepted Cancel and closeNow each close the owner and permit reopening. Controlled init exception returned through the same failed controller with one callback and one cleanup, then a clean instance reopened. Five closures each had matching ownership and cleanup.

SingletonLifecycleMatrix, SingletonCloseVeto, FormCancel, FormLifecycle, Maven compile, PowerShell syntax and staged diff check passed. Corporate MDI/WAIT/DB and launcher-level exception before Stage show remain UNKNOWN. No production edit.

---

## Reply CTRL-036

**DONE / TEST-ONLY.** Verified HEAD `dce04163498221af2f86c8eaec84d77d3974dfc6`. New ValidatorMultiControlSmoke and runner only. Real focused JavaFX Stage: six blur dispatches and six model notifications for six edits; shared/distinct validator calls 4/2 across the sequence. Both unbind directions preserve the opposite field's registrations; duplicate same-control adds still validate twice. Reattached LOV produced one validator, old/detached changes did not write the model, validateOnOk was read-only. ValidatorMultiControl, ValidatorScopedUnbind, LovIntegration, Maven compile, PowerShell parse and staged diff check PASS.

---

## Reply CTRL-037

**DONE / READ-ONLY.** HEAD unchanged at `dce04163498221af2f86c8eaec84d77d3974dfc6`. Real JInvTextField reparented across two Scenes with different controller metadata; getController resolved the new controller. M1 validated before and after move. After explicit M1 unbind, automatic blur still dispatched through M1 with no validator; M2 ran only on explicit validation. M1/M2 dispatch 3/1, all three edits produced one model notification apiece, node marker true.

Source: node focus/change callbacks capture M1 and the Boolean marker suppresses M2 setup. This gives a strong path from live control to M1/controller until callbacks are removed. Normal FXFormLauncher loads a fresh FXML root and installs its owner; no supported cross-controller reuse path established in this checkout, so corporate impact and retention duration are UNKNOWN. If reuse is adopted, give manager registration explicit detach/transfer semantics with lazily stored listener handles only for validated controls; avoid eager state or changing duplicate validator semantics.

---

## Reply CTRL-038

**DONE / READ-ONLY.** HEAD unchanged at `dce04163498221af2f86c8eaec84d77d3974dfc6`. JavaFX 25 real field, Stage and skin: direct set exposed provider through getLOV/showLOV and delivered accepted callback but installed no value listener or button, so model stayed unchanged. Direct replacement preserved old button, read new provider and wrote zero model events. Direct null preserved the button with no active provider/callback.

Externally binding to the already installed provider preserved its listener while active; a bound-source replacement did not install a new one. setLOV to a different provider while bound threw the standard bound-property exception and detached the old listener before it failed. Unbind property then call the public setter restored updates and supported unbind removed the LOV button. Also observed that a previously ignored invalidation could suppress a subsequent write on reinstall until the provider value was read. Model events across the fixture: [installed, bound same provider, reattached]; one accepted callback per direct provider.

Production usage found Binder, form/filter and sheet callers using setLOV; no production Java/FXML caller directly mutating/binding lovProperty found. External clients UNKNOWN. Keep setter-owned lifecycle as the documented supported route until a compatibility decision is made. If direct property ownership is approved, specify bound-setter failure atomicity before implementation and retain lazy allocation.

---

## Reply CTRL-039

**DONE / READ-ONLY.** HEAD unchanged at `dce04163498221af2f86c8eaec84d77d3974dfc6`. Real Stage + JInvTextFieldButtonSkin: same-provider reattach and replacement created three distinct buttons. After runLater drained, wrapper followed only last button; old provider produced zero writes, active one, detached zero. Settled unbind cleared the inner reference and wrapper binding. No-LOV sentinel had no LOV button or lazily allocated LOV value listener.

CONFIRMED retained stale binding: setLOV then unbindLOV in the same FX turn leaves getInnerButton null, but queued JInvWrapButton.setInnerButton work then restores bindings to the removed button. Wrapper action identity matched removed button and its text followed a later write to the removed button. Strong path: live field skin → wrapper bound properties → obsolete inner-button properties. This establishes retention and observable ghost UI for this event order; lifetime in deployed forms/GC behavior UNKNOWN. Normal settled replacement showed no historical wrapper binding. Recommend a scoped wrapper follow-up to discard queued callbacks that no longer target current inner button, with same-turn unbind/replacement regression. No tracked changes.

---

## Reply CTRL-040

**DONE / NO_CHANGE (READ-ONLY).** HEAD unchanged at `dce04163498221af2f86c8eaec84d77d3974dfc6`. DSFXAdapter.setDataSet explicitly rejects replacing a bound TableView Dataset; DSControlAdapter supports row/navigation listener replacement for separately bound controls. Table bind installs view handlers, selection listener, item projection and metadata; close removes Dataset/mark/row/navigation listeners and clears Dataset, but is not a table-handler unbind contract. Current launcher/binder usages do not establish close-and-reuse of the same table as supported.

Fresh JavaFX 25 GUI-009-derived probe: A/B/C render and row identity, navigation 0→1→2 and refresh passed. setDataSet(null) rejected with UnsupportedOperationException, leaving adapter Dataset and selection identity unchanged. After close, independent late row/navigation/Dataset observers fired and adapter counts stayed navigation=3, rows=0, dataset=1, with stable selection. No unsupported A→B scenario fabricated. Recommendation: retain fresh table/adapter lifecycle; define teardown and ownership explicitly before any future same-table rebind support. Corporate SQL integration UNKNOWN.

---

## Reply CTRL-041

**DONE / regression battery.** Stable clean implementation HEAD `dce04163498221af2f86c8eaec84d77d3974dfc6`. Fresh Windows Java 25.0.1 / JavaFX 25 processes, all exit 0:

| Mode | Exit | Mode | Exit |
| --- | --- | --- | --- |
| SingletonCloseVeto | 0 | SingletonLifecycleMatrix | 0 |
| ValidatorScopedUnbind | 0 | ValidatorMultiControl | 0 |
| FormCancel | 0 | FormLifecycle | 0 |
| DataBinding | 0 | LovListenerLifecycle | 0 |
| LovDefaultValidation | 0 | LovIntegration | 0 |
| SceneKeyLookup | 0 | ChoiceNonNullSet | 0 |
| BinderChoiceNull | 0 | RadioReadOnly | 0 |
| ControlsFollowup | 0 | LovButtonReattach | skipped, CTRL-034 BLOCKED |

Maven -Pbuild compile, PowerShell parse and diff check passed. No implementation commit for this battery. Independent user acceptance, physical input, corporate DB/modal and MDI/WAIT remain UNKNOWN.

---

## Mission Summary CTRL-032–041

Four implementation-branch commits, fast-forward and verified; HEAD `dce04163498221af2f86c8eaec84d77d3974dfc6`. Two production fixes, two test-only commits, four read-only audits, one local blocker and one final battery. All ten Replies are in this outbox.

| Task | Status | Implementation commit or decisive result |
| --- | --- | --- |
| CTRL-032 | DONE | `cad4dfd39cda4cb1f23bb8610d7ab24c00123bb5` — RED 2 visible after veto; GREEN 1, 0 callbacks; accepted close/reopen once each |
| CTRL-033 | DONE | `562b9a917a6c708dd6f18cb10112a6aca3d7653f` — opposite-control validator calls 0 → 1 in both orders; one notification per edit |
| CTRL-034 | BLOCKED | Same/new field reattach: two bound-disable exceptions; action pair and inherited metadata diverged; binding ownership decision required |
| CTRL-035 | DONE / test-only | `aba018ab2e14bab5c774668d3ab9b74d32ae7fa1` — rejected OK retains owner; one callback/cleanup per closure including controlled init failure |
| CTRL-036 | DONE / test-only | `dce04163498221af2f86c8eaec84d77d3974dfc6` — six blur dispatches, six model events, shared/distinct calls 4/2 |
| CTRL-037 | DONE / read-only | Reparented control dispatch M1/M2 3/1 including explicit M2 call; supported production reuse UNKNOWN |
| CTRL-038 | DONE / read-only | Direct property mutations skip button/listener setup; bound setter can throw after detaching old listener; external contract UNKNOWN |
| CTRL-039 | DONE / read-only | Same-turn unbind can leave stale action/text bindings on wrapper; one active-provider model write, zero detached writes |
| CTRL-040 | DONE / no change | Bound-table rebind explicitly unsupported; late A events after close did not reach adapter |
| CTRL-041 | DONE | 15 fresh smoke processes exit 0; CTRL-034 mode skipped |

Three priorities for architect decision:

1. Guard deferred wrapper binding against an obsolete inner button, with same-turn unbind/replacement coverage.
2. Define JInvLOVButton binding ownership and same-mode transfer before fixing reattachment. Preserve caller-provided bindings and exclusive modes.
3. Specify writable lovProperty lifecycle and failure atomicity for externally bound properties before changing public behaviour. Keep lazy allocation.

No mission changes to main, JInvCommon, CTRL-014 or CAL-TIME-001. No successor task assigned.

---

## Reply CTRL-042

**DONE / TEST-ONLY.** Verified fast-forward implementation HEAD `f4f2903181be6b4c2ff0dd19d002a87fe6a99afb` on Windows Java 25.0.1 / JavaFX 25. Only SingletonCloseVetoSmoke.java changed.

Unmodified baseline: SingletonCloseVeto 5/5 passed locally; SingletonLifecycleMatrix failed on run 3 (runs 1–2 passed) with the same owner-2 assertion inside ProgressBinder.showStageProcess / Stage.showAndWait seen by the independent user. An ignored diagnostic copy held the cleanup worker until the original queued check ran. At that check: owner showing=false, callbacks=2, result owner matched, RET_CANCEL, exception=null, lifecycle=ON_CLOSE, cleaned=0, owned progress showing=true, nested loop=true. After releasing the worker and observing WINDOW_HIDDEN: callbacks=2, cleaned=1, progress showing=false, nested loop=false, successful singleton reopen. In the init-failure diagnostic the same window yielded callbacks=4, cleaned=0 before hidden and callbacks=4, cleaned=1 after loop unwind, then reopen. Synchronous accepted Cancel checks run after event dispatch returns; those checks were retained.

The test now listens for WINDOW_HIDDEN on the resource-progress Stage owned by the exact form, then verifies on the FX thread after nested loop exit. All original cleanup, callback, identity, return and reopen assertions retained. GREEN: 10/10 fresh JVMs per mode plus one official runner pass per mode. FormCancel, FormLifecycle, ValidatorScopedUnbind, DataBinding, Maven compile, PowerShell parse and staged diff check all passed. Independent user acceptance of CTRL-032/035/042 remains pending.

Windows rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -SingletonCloseVeto -SettingsFile "D:\Java\.m2\settings.xml"` and the same command with `-SingletonLifecycleMatrix`.

---

## Reply CTRL-043

**DONE.** Verified HEAD `a62b145212b12455fe8cc3a6fa66c82f3a047be1`. RED: A→null and A→B→null became visible and bound again; A→B and A→null→B assigned CONTROL_PARENT to obsolete buttons. Stable A and LOV-over-FE priority passed. GREEN: both detached wrappers hidden with all six properties unbound; both replacement cases follow only B with the proper parent; obsolete buttons have no wrapper parent. Active routes each wrote once per provider change and once per button choice, detached routes zero. Added only an identity guard before any deferred write, no additional instance state.

JavaFX 25 focused official PASS plus 5/5 fresh JVMs, LovListenerLifecycle, LovIntegration, TextFields, FormCancel, ControlsFollowup, Maven compile, PowerShell parse and staged diff check PASS. Rerun `src/test/probes/Test-LegacyGuiSmoke.ps1 -WrapButtonDeferred -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-044

**DONE.** Verified HEAD `31de47899b2b22e0dddbe374bfd02fd297236f3c`. RED in both rejected setLOV(other) and unbindLOV: old provider emitted its change but the model was silent. GREEN: exactly one model write after each rejected operation, same provider/button and validation flag, zero property events during failure, native exception class and message preserved. Successful unbound replacement, repeat and detach passed. Unused field has no LOV listener.

JInvTextField skips detachment if the property is bound, then uses the original JavaFX setter to throw. This preserves the ordering for successful unbound calls and adds no state. Direct property lifecycle remains out of scope. BoundLovSetter, LovListenerLifecycle, LovDefaultValidation, LovIntegration, WrapButtonDeferred, DataBinding, FormCancel, Maven compile, PowerShell parse and staged diff check PASS.

---

## Reply CTRL-045

**DONE / TEST-ONLY.** Verified `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f`. Real Stage/skin combined matrix: same-turn A→null and A→B→null left hidden/unbound wrappers and zero writes; A→null→B, repeat B, rejected bound calls, accepted button choice and recovery produced exactly [current, choice-1, recovered]. Equal replacement and obsolete-provider writes were silent; readOnly and non-editable blocked the wrapper; active bindings followed only the current button. Field delegation and standalone provider mode remained exclusive (one standalone choice). No field reattachment expectation. LovBindingInterleaving, WrapButtonDeferred, BoundLovSetter, LovIntegration, TextFields, ControlsFollowup, Maven compile, PowerShell parse and staged diff check all PASS. No production edit.

---

## Reply CTRL-046

**DONE / READ-ONLY.** HEAD unchanged `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f`. SOURCE-PROVEN: AddLinkPane.fxml includes a sibling external JInvLOVButton; AddLinkPaneController.initLov attaches it with setTextField and then calls field.setLOV, matching the audited order. AbstractBaseController also supports external idTextField wiring through JInvButton.setTextField. Field removes inner button on a changed non-null external reference; setLOV can reinstall one while external remains assigned. Skin suppresses its overflow button with an external reference, but wrapper layout still uses wrapper visibility.

CONFIRMED on real focused Stage/skin, sibling external JInvLOVButton, and controlled provider: external→setLOV left both visible (external managed, wrapper unmanaged); external fire, inner wrapper fire, and synthetic F9 each opened once and wrote once, the three model values were [choice-1, choice-2, choice-3]. Replacing external after setLOV hid wrapper and cleared inner reference and property bindings. Wrapper's unbound onAction still held the old handler. Clearing external did not rebuild the inner button; explicit setLOV did. Replacing skin left a new empty wrapper and active provider, while the retained old wrapper still had its button/bindings. Source shows raw field listeners capture former skin and dispose does not unregister those listeners. Lifetime/impact in corporate forms, native input and DB-backed help dialog UNKNOWN.

Recommend an isolated tested precedence contract for external→LOV and LOV→external based on the real help form before changing behavior. Treat old skin listener teardown separately; no change to CTRL-034 or existing exclusive modes.

---

## Reply CTRL-047

**DONE / READ-ONLY.** Stable clean implementation HEAD `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f`. Windows Java 25.0.1 / JavaFX 25, fresh processes:

| Mode | Exit | Mode | Exit |
| --- | --- | --- | --- |
| WrapButtonDeferred | 0 | BoundLovSetter | 0 |
| LovBindingInterleaving | 0 | LovListenerLifecycle | 0 |
| LovDefaultValidation | 0 | LovIntegration | 0 |
| TextFields | 0 | FormCancel | 0 |
| FormLifecycle | 0 | SingletonCloseVeto | 0 |
| SingletonLifecycleMatrix | 0 | ValidatorScopedUnbind | 0 |
| ValidatorMultiControl | 0 | DataBinding | 0 |
| SceneKeyLookup | 0 | ControlsFollowup | 0 |
| RadioReadOnly | 0 | LovButtonReattach | SKIPPED (CTRL-034 BLOCKED) |

17/17 passed on first run; no flaky outcome observed in this battery. Maven -Pbuild compile, PowerShell parse and diff check passed. Wrapper had 0 stale writes, rejected bound setter and unbind each preserved a single subsequent write, integration emitted exactly [current, choice-1, recovered]. Validator matrix retained 6 blur dispatches/6 model events. Singleton veto retained one visible form and zero callbacks; matrix cleanup and reopen passed. No implementation change. Corporate DB, physical input and MDI remain UNKNOWN. CTRL-033 independent user PASS still pending; accepted CTRL-032/035/042 confirmation is recorded in inbox. Independent acceptance of new mission tests UNKNOWN.

---

## Mission Summary CTRL-043–047

| Task | Status | Commit / proof |
| --- | --- | --- |
| CTRL-043 | DONE | `a62b145212b12455fe8cc3a6fa66c82f3a047be1`: RED four obsolete callback routes, GREEN none; 5/5 fresh focused passes plus official runner |
| CTRL-044 | DONE | `31de47899b2b22e0dddbe374bfd02fd297236f3c`: active provider updates after both rejected bound mutations 0→1, same exception and button |
| CTRL-045 | DONE / test-only | `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f`: 0 writes in both detached routes, 3 expected in active/recovered sequence and 1 standalone choice |
| CTRL-046 | DONE / read-only | Real help-form attachment order reproduced two visible buttons; three routes each opened and wrote once. Old handler and skin ownership require review. |
| CTRL-047 | DONE / read-only | 17 smoke processes exit 0; blocked reattach mode skipped; no new commit. |

Three follow-ups for architect decision:

1. Define external-vs-inner LOV precedence and restoration on detach, covering the real help form and both setter orders.
2. Audit and test former skin disposal, especially listener lifetime and cached wrapper actions across skin replacement.
3. Resolve CTRL-034 binding ownership before enabling same-mode field reattachment; preserve bindings supplied by callers. Direct writable lovProperty lifecycle remains a separate open contract.

Three commits published fast-forward, exact implementation SHA verified. main, JInvCommon, CTRL-014, CTRL-034 and CAL-TIME-001 stayed outside the change set. No successor task assigned.

---

## Reply CTRL-048

**DONE / READ-ONLY.** Verified clean production HEAD `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f`. Ignored probe target/ctrl048/SkinDisposalAudit.java, Windows Java 25 / JavaFX 25, actual shown Stage and focus, real control and skin subclasses observing public behavior. One exploratory run and three fresh JVMs all reproduced the same counts.

**CONFIRMED runtime:** On the original skin, text/width/font/external-button change invoked overflow calculation once each. After public setSkin(null) and replacement, former dispose ran once and getSkinnable returned null, yet text/width/external changes invoked calculation twice, and old overflow visibility and focus pseudo-class still changed. The managed font callback stayed at one. No-LOV control also showed two text callbacks after replacement, despite no LOV button. Old wrapper retained its inner button, bound handler/tooltip and tracked the old button's text; new wrapper started empty with provider intact. External and internal actions each wrote once before replacement; post-replacement synthetic F9 and ordinary provider change each wrote once, and same-turn unbind wrote zero. No duplicate model/LOV action through normal routes in this fixture. No physical input or DB used.

**SOURCE-PROVEN:** Direct externalButton/text/width listeners capture the old skin through checkOverflow; focus listener accesses its wrapper. dispose clears owned context menu and calls super.dispose but does not remove these listeners. Strong path is live field property → listener → former skin → old wrapper and button. Wrapper visibility listener also requests field layout. Font uses registerChangeListener; observed teardown differentiates it from the direct registrations. No runtime GC claim. Production lookup found the constructor installing the default skin, but no routine in-repo JInvTextField skin-replacement path. Public JavaFX API replacement is reproducible.

**HYPOTHESIS / UNKNOWN:** repeated replacements can accumulate obsolete UI work/objects; prevalence, retained byte count and corporate business impact unknown. Old handler retention by itself does not establish user access to the detached button. Narrow next task: test and detach skin-owned listeners and wrapper bindings at dispose, keeping handles on the skin only, verifying direct counts 2→1, old pseudo-class frozen, current skin live and model counts unchanged. Preserve button precedence, CTRL-034 and writable LOV contract.

---

## Reply FORE25-TRIAGE

**Read-only follow-up to CTRL-048, 2026-11-25.** Verified implementation baseline `4de3d8a680bb2820be733d89c8cee0dfe3c1e80f`. Ten candidates ranked by evidence and potential impact. Temporary probes for the first three are under ignored `target/fore25-triage/`. This report requests triage, with no implementation changes.

| Priority | Code | Evidence and next check |
| --- | --- | --- |
| 1 | JInvTextArea.setLOV / addLovValueListener | **Reproduced.** A former provider overwrote a draft with the current provider value. Three attachments added three toolbar buttons; after setLOV(null), one provider change raised two NPEs. Start with listener and toolbar ownership, preserving accepted-choice and model notification semantics. |
| 2 | DSFXAdapter.close / refreshCurrentRow | **Reproduced.** KEY_RELEASED delivered to a still-shown table after adapter close reached the old handler and raised NPE at refreshCurrentRow. The established late-DataSet-event checks still passed. Test post-close input independently of the unsupported same-table rebind contract. |
| 3 | ValidMan.getValidators | **Reproduced.** In a three-validator list, removeAll(v1,v3) left only v2 in the returned list but actual validation still invoked v2 and v3. Listener processes only one subchange. Cover disjoint removals while preserving CTRL-033 scoping and same-control duplicate semantics. |
| 4 | Binder.bind | **CTRL-023 runtime.** Exception leaves successful controls bidirectionally attached. Determine whether supported callers reuse a partially bound form before proposing failure semantics. |
| 5 | JInvTextField.externalButton / setLOV | **CTRL-046 runtime.** Attachment order changes inner-button visibility. External-first order exists in AddLinkPane. Decide precedence using the real form contract. |
| 6 | JInvWrapButton.setInnerButton | **CTRL-046 runtime.** After settled detach the hidden wrapper is unbound but still holds the previous onAction value. User reachability unknown. Distinct from the fixed deferred-rebind race. |
| 7 | ValidMan.initListenersOnControl | **CTRL-037 runtime.** A moved control still dispatches automatic blur validation through the former manager. Supported application reparenting remains unknown. |
| 8 | JInvTextAreaSkin.dispose | **Source candidate.** Direct scene/editable listeners capture skin; registered external-editor action accesses getSkinnable. Reproduce after real skin replacement before proposing a fix. |
| 9 | JInvTabPaneSkin.initialise | **Source candidate.** Switching back to Side.LEFT adds another tab-list listener. Count processing after repeated side toggles and check normal left-tab rendering. |
| 10 | SceneKeyManager.getActionRecursive | **CTRL-024 runtime plus source.** Candidate selected before enabled/security checks. A blocked match may reserve the key and suppress another eligible action in the same list. Confirm intended precedence before changing it. |

Recommend considering 1–3 as independently scoped RED → GREEN tasks. CTRL-049 is assigned separately in inbox. CTRL-034 ownership, CTRL-014 and CAL-TIME-001 remain outside this triage.

---

## Reply CTRL-049

**DONE.** Verified clean implementation HEAD `582a7899172c3beef845dbb2393bfc3e5d02e75f` after fast-forward. Only JInvTextFieldButtonSkin.java, new TextFieldSkinDisposalSmoke.java and its runner mode changed.

**RED → GREEN (Windows Java 25 / JavaFX 25):** original Stage positive counts text/width/external/font 1/1/1/1. After public unset/replacement, original text/width/external counts 2/2/2 vs fixed 1/1/1; font 1 in both. No-LOV text counts across three replacements 2/3/4 vs 1/1/1. Original disposed wrapper reacted to focus/old button text and queued attachment could bind again; fixed retired overflow/focus stayed frozen, wrapper lost all six bindings and owned handler, pending button acquired no CONTROL_PARENT. Active wrapper received focus/blur, caller context menu and field action survived, provider identity preserved. Three choices and one ordinary LOV update each produced one model event, two text edits brought total to six, detach produced zero. No LOV callback object on unused field.

**Ownership and cost:** switched five direct subscriptions to existing SkinBase lifetime management. Overflow routes share one Consumer instead of four captures; reuse existing managed handler/weak change listener with five more property-map registrations per skin, no new JInvTextField fields or helper allocation. Disposal detaches only retired skin wrapper's currently attached button; matching bound action is cleared. Wrapper already empty after ordinary detach, replacement-skin LOV restoration and button precedence remain separate. No heap-byte/GC, corporate DB or physical-input claims; independent acceptance pending.

Official focused PASS plus 3/3 fresh JVMs. TextFields, ControlsFollowup, WrapButtonDeferred, BoundLovSetter, LovBindingInterleaving, LovListenerLifecycle, LovDefaultValidation, LovIntegration, FormCancel and FormLifecycle all exit 0. Maven compile, PowerShell parse and staged diff check passed. Windows rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -TextFieldSkinDisposal -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-050

**DONE.** Baseline `582a7899172c3beef845dbb2393bfc3e5d02e75f`; verified fast-forward implementation HEAD `6d36746616ce26fee2e42acefce57571131ffb6e`. Only JInvTextArea.java, new TextAreaLovLifecycleSmoke.java and runner changed.

**RED → GREEN, Windows Java 25 / JavaFX 25.** Original first update 1 copy / 1 model event; repeat A 2 copies / 1 event → 1/1. Former A while B active 2 copies / 1 unwanted draft overwrite → 0/0. Detached A/B and later settled A emitted 2/1/3 exceptions respectively → 0/0/0, with zero copies and model events. Owned toolbar entries now remain one while attached and zero after null; caller items preserve identity/order. Bound set/clear kept original JavaFX rejection, toolbar, provider and model; following current-provider update 1/1. Toolbar and targeted F9 accepted once each; two existing copy attempts and one model event per accepted choice remain. Cancellation delivered one false callback and zero copies/events. Eight total model events in GREEN including the explicit draft edit, no uncaught FX exceptions.

The field adds one nullable attachment reference; the reusable listener holder allocates only on first LOV use and holds the observed property and owned button. The button is reused while attached and removed by identity on null. No new helper/listener/button is allocated on an unused area. setTextField(null) cannot detach a JInvLOVButton association; common button API untouched. A separately simulated delayed accepted completion from retired A still routed the legacy callback and copied current B text onto a draft; former A's value notification alone no longer writes. Delayed completion ownership remains outside this change. Actual DB prevalence, heap bytes and physical input UNKNOWN.

Official TextAreaLovLifecycle exit 0 plus 3/3 fresh JVMs. TextFields, ControlsFollowup, LovListenerLifecycle, LovDefaultValidation, LovIntegration, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal, FormLifecycle, FormCancel and Editors all exit 0. Maven `-Pbuild compile` via runner, PowerShell parse and staged diff check passed. Independent acceptance pending. Rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -TextAreaLovLifecycle -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-051

**DONE.** Clean baseline `6d36746616ce26fee2e42acefce57571131ffb6e`; fast-forward implementation head verified `171cb8c6347cae958907debcf0774ba6d3b85d11`. Only DSFXAdapter.java, new TableAdapterCloseSmoke.java and runner mode changed.

**RED → GREEN (Windows Java 25 / JavaFX 25).** Shown table: pre-close DOWN release made one cursor refresh and HOME was consumed by the adapter, both versions. After two close calls, baseline DOWN caused one NPE in refreshCurrentRow, caller recorded only pre-close DOWN, HOME consumption [true,true]. GREEN closed DOWN caused zero refreshes/exceptions and reached the caller (two total DOWN releases); HOME consumption [true,false]. Late navigation, row and execute each reached their independent Dataset observer once, adapter stayed at [1,0,1], and table selection stayed at index 1. Repeated close passed.

Adapter retains exact KEY_RELEASED handler and KEY_PRESSED filter and unregisters only those before Dataset teardown. Two nullable references, no eager helper, no additional handler object beyond the two formerly created at bind. Caller registrations and old Dataset teardown preserved. Mouse filters and pending paging remain outside the keyboard-detach scope; same-table rebind contract unchanged. Corporate SQL and independent user acceptance UNKNOWN.

Official TableAdapterClose and 3/3 fresh JVMs, existing DataBinding (GUI-009 close/late-event), ChoiceAdapter and ChoiceNullLookup all exit 0. Maven compile, PowerShell parse and staged diff check passed. Windows rerun: `src/test/probes/Test-LegacyGuiSmoke.ps1 -TableAdapterClose -SettingsFile "D:\Java\.m2\settings.xml"`. Standalone session stops here as required by inbox eab0f12f; separately launched batch CTRL-052–055 may now use the verified head.

---

## Reply CTRL-052

**DONE.** Baseline `171cb8c6347cae958907debcf0774ba6d3b85d11`; verified fast-forward `e708fdda5d8669e52a52e628e332216ef737c6fc`. Original disjoint removeAll(v1,v3) returned [2] but invoked [2,3], exit 1. GREEN invoked [2], exit 0. Contiguous removal, add/replace/clear, shared validator isolation and historical duplicate add/remove semantics passed. Both ValidMan list observers now process every subchange, preserving existing order and adding no state. New ValidatorSubchangesSmoke and runner mode. Official plus 3/3 fresh JVMs, ValidatorScopedUnbind, ValidatorMultiControl (six actual blur dispatches and model events), LovIntegration, Maven compile, PowerShell parse and staged diff check passed. Independent acceptance pending. Windows: `src/test/probes/Test-LegacyGuiSmoke.ps1 -ValidatorSubchanges -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-053

**DONE.** Baseline `e708fdda5d8669e52a52e628e332216ef737c6fc`; verified fast-forward `2d18941e3fd7911f3df27c65ec26061062e3c6d4`. Original repeated public skin replacement: 3 actions on first scene, 4 leftover including caller, 2 on second; retired editable callback removed a toolbar item and visibility bindings survived. GREEN counts 1/1 (caller only)/1, one stub F4 dispatch, zero retired dispatches. Real focus and scenes; no external editor executed. Active toolbar, caller context menu and original TextArea LOV model counts survived.

Only JInvTextAreaSkin, focused test and runner. Skin-owned scene/editable subscriptions managed by SkinBase; unregisters only matching action/node from prior scene/at dispose, unbinds its toolbar/FE visibility. Two nullable references on skin, action allocated upon first attachment and reused, no new field-level state. Retained externally held individual toolbar buttons/width callbacks, native external editor and independent acceptance not covered. Official TextAreaSkinDisposal plus 3 fresh processes, TextAreaLovLifecycle, Editors, FormLifecycle, FormCancel, SceneKeyLookup, Maven compile, PowerShell parse, staged diff check exit 0. Windows: `src/test/probes/Test-LegacyGuiSmoke.ps1 -TextAreaSkinDisposal -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-054

**DONE.** Baseline `2d18941e3fd7911f3df27c65ec26061062e3c6d4`; verified fast-forward `5bc3c8bd02a22cb13cd5a7d45b941b86d29061e5`. Real Stage; deliberately bound style sentinel counted each style attempt via its expected exception, while ordinary tab style/selection/attached content passed. RED→GREEN attempts initial TOP 0→0, first LEFT 1→1, fourth LEFT 4→1, TOP after LEFT 4→1 (existing styling eligibility preserved), replacement already LEFT 5→1. Only JInvTabPaneSkin, new smoke and runner. Lifetime-managed list subscription installed once, managed side/width observers; one boolean per skin, no per-tab fields or eager helpers. Official plus 3 fresh JVMs, NodeTraversal, ControlsFollowup, Editors, Maven compile, PowerShell parse and staged diff check passed. Independent acceptance pending. Windows: `src/test/probes/Test-LegacyGuiSmoke.ps1 -TabPaneSkinListeners -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-055

**DONE.** Baseline `5bc3c8bd02a22cb13cd5a7d45b941b86d29061e5`; verified fast-forward `ed5533d8d03c1ca1500b65660060d47ce336e3d3`. RED hidden wrapper fire after settled detach routed [A,A] and retained A action/parent. GREEN routed [A], old action and parent absent; subsequent same-turn ABA and settled A→B produced [A,B,caller] with no old dispatch, preserving caller unbound action, a foreign parent and LOV-over-FE priority. Programmatic fire on real shown Stage; physical input untested.

Only JInvWrapButton, new WrapButtonOwnershipSmoke and runner. Clear wrapped matching action on detach/replacement only when it was bound and identical to old source's handler; remove CONTROL_PARENT only when it points to this wrapper. No new state, no empty map allocation for pending buttons. Public properties reveal bound status, not arbitrary external source identity; no simultaneous external binding with the same action was established. Existing CTRL-043 deferred guard and CTRL-049 skin cleanup pass; common LOV and button attachment semantics not expanded.

Official plus 3 fresh JVMs; WrapButtonDeferred, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal, TextAreaLovLifecycle, Maven compile, PowerShell parse, staged diff check exit 0. Windows: `src/test/probes/Test-LegacyGuiSmoke.ps1 -WrapButtonOwnership -SettingsFile "D:\Java\.m2\settings.xml"`. Independent acceptance pending. Batch CTRL-052–055 completed, stop after this reply.

---

## Reply CTRL-056

**DONE / READ-ONLY.** Verified HEAD `ed5533d8d03c1ca1500b65660060d47ce336e3d3`. Compiled/run ignored FocusManagerAudit.java with Java 25 and JavaFX 25 on real Stage (exit 0). Detach/readd field in a second Scene changed getController to M2. Automatic blur still invoked M1 before and after move. After explicit M1 unbind, blur dispatched to M1 with no validator; explicit M2 validation worked. Dispatch totals M1/M2=3/1 (M2 automatic=0); model=[before,moved,old removed] exactly once each. A shared validator on an unrelated control stayed registered in both managers.

ValidMan binds focus/change closures once via CONTROL_FOCUS_LISTENER; unbind changes validator list and decoration, not listener ownership. CTRL-033/052 cover list scope and subchanges, not transfer. AbstractBaseController creates manager per controller and normal launcher builds fresh FXML graph; no supported move between controller lifetimes established. Confirmed behavior, contractual defect UNKNOWN. Preserve single ownership unless transfer is required; smallest follow-up would explicitly release/reattach manager-owned listeners on transfer, with no eager per-control state. No production or tracked-test changes.

---

## Reply CTRL-057

**DONE / READ-ONLY.** Verified HEAD `ed5533d8d03c1ca1500b65660060d47ce336e3d3`. Ignored BinderPartial.java, Java 25/JavaFX 25, shown Stage, real FXEntity and three stock TextFields. Clean bind: 3 model/9 text events for two control writes and one model write. Invalid middle metadata throws Binder wrapper, cause IllegalArgumentException: Not found property 'missing'. First and third remained bidirectional (3 model/6 text events). After metadata repair, retry restored 3/9 with no duplicate observable notifications. After ordinary Stage close (fixture retains controls; not full controller launcher), 3/9 still propagated. StringProperty typed unbind did not detach the converter route (model changed to unbound); Bindings.unbindBidirectional(Object,Object) then kept that model value when text changed to generic-unbound. Final diagnostic exit 0.

Ownership: Binder installs converter linkage per successful control before collecting errors; other routes include validators/adapters/LOV and non-control components before the first error is wrapped. AbstractBaseController bindControls is the production caller; init failure requests close, with no Binder rollback in that path. App-level reuse/retention and validator duplication remain UNKNOWN. Partial binding is confirmed, transactional contract unproven. If production caller reuse is established, scope a fix to preflighting named field mappings or exact converter-binding detach on that caller's failure; avoid global rollback that could discard intentional edits or unrelated attachments.

---

## Reply CTRL-058

**DONE / READ-ONLY.** Verified HEAD `ed5533d8d03c1ca1500b65660060d47ce336e3d3`. Ignored KeyReservationAudit.java (adapted CTRL-024 fixture) Java 25/FX25, actual Stage focus, targeted F7 through SceneKeyManager.filterHandler, exit 0. For each controlled disabled and security-denied action: blocked-first same node and blocked child/allowed parent yielded 0 calls and unconsumed; allowed-first and eligible parent alone yielded exactly 1 allowed call and consumed; only blocked yielded 0/unconsumed. Denied handlers never called; fixture flags did not change real security settings.

SceneKeyManager appends registrations, chooses first matching key/phase at nearest context before checking enabled/security, and performs global fallback only when lookup returns no candidate. Thus confirmed candidate shadowing is independent of event consumption. Intentional reservation versus starvation is an architectural decision. Preserve existing behavior until approved; if fallback is desired, distinguish disabled vs denied policy explicitly, scope change to candidate selection and test same-node, parent, global and handler precedence without making denied actions executable. Corporate security policy UNKNOWN.

---

## Reply CTRL-059

**DONE / READ-ONLY.** Verified HEAD `ed5533d8d03c1ca1500b65660060d47ce336e3d3`. Ignored ExternalLovAudit.java, Java 25/FX25, attached Stage and production controls; exit 0. AddLinkPane.fxml has sibling external JInvLOVButton and field; AddLinkPaneController.initLov calls external.setTextField then field.setLOV(lov,true). That exact order with local provider showed both buttons (laid-out widths 24/24, validateFromLOV=true), each button fire and synthetic F9 opened/wrote once. Setting a new external after LOV hid/detached the wrapper; clearing external left it empty until explicit setLOV. CTRL-055 cleared retired action/parent; CTRL-049 cleared former skin bindings; replacement skin remained empty with provider intact. CTR-045 and TextArea CTRL-050 cover separate ownership contracts.

**Confirmed input-path gap:** readOnly suppressed inner fire and F9 (0 opens), but external stayed enabled; external Button.fire() reached the real handler and produced 1 open and 1 model write. Native click and actual DB-backed help form not run. Minimal separate follow-up: guard field-delegated external LOV user activation for a readOnly field, preserving programmatic provider updates, standalone button and existing binding ownership. No priority change needed for that fix. External-only vs allowing both affordances and whether external detach should restore inner are architectural decisions; current observations alone do not select a policy.

---

## Reply CTRL-060

**DONE / READ-ONLY.** Final verified clean implementation HEAD `ed5533d8d03c1ca1500b65660060d47ce336e3d3`; no implementation writes throughout batch.

Windows fresh Java 25/FX25 processes, runner src/test/probes/Test-LegacyGuiSmoke.ps1, each with -SettingsFile "D:\Java\.m2\settings.xml". Exact exit matrix: TextFieldSkinDisposal exit=0; TextAreaLovLifecycle exit=0; TableAdapterClose exit=0; ValidatorSubchanges exit=0; TextAreaSkinDisposal exit=0; TabPaneSkinListeners exit=0; WrapButtonOwnership exit=0; TextFields exit=0; ControlsFollowup exit=0; WrapButtonDeferred exit=0; BoundLovSetter exit=0; LovBindingInterleaving exit=0; LovListenerLifecycle exit=0; LovDefaultValidation exit=0; LovIntegration exit=0; FormCancel exit=0; FormLifecycle exit=0; DataBinding exit=0; ChoiceAdapter exit=0; ValidatorScopedUnbind exit=0; ValidatorMultiControl exit=0; SceneKeyLookup exit=0; Editors exit=0. All 23 exited 0 on first battery run. Runner Maven -Pbuild compile passed, PowerShell parser and git diff --check clean. CTRL-034 reattachment intentionally excluded (open contract). RadioReadOnly, ChoiceNullLookup and other out-of-matrix modes not run in this focused battery. Physical input and corporate DB unavailable; no new independent acceptance or heap measurements.

| Task | Status | Scope / result |
| --- | --- | --- |
| CTRL-051 | Accepted prior to batch | `171cb8c6347cae958907debcf0774ba6d3b85d11` DSFXAdapter keyboard release/press teardown; two nullable references, same two bind-time handlers; late Dataset events isolated |
| CTRL-052 | Accepted prior to batch | `e708fdda5d8669e52a52e628e332216ef737c6fc` ValidMan consumes all list subchanges; no state or registration added |
| CTRL-053 | Accepted prior to batch | `2d18941e3fd7911f3df27c65ec26061062e3c6d4` TextArea skin owns two nullable references, one lazy action/reusable registration, two managed listener paths, unbinds toolbar/FE at disposal |
| CTRL-054 | Accepted prior to batch | `5bc3c8bd02a22cb13cd5a7d45b941b86d29061e5` TabPane skin adds one boolean, manages side/width and one list registration after LEFT, no per-tab state |
| CTRL-055 | Accepted prior to batch | `ed5533d8d03c1ca1500b65660060d47ce336e3d3` JInvWrapButton removes matching retired action/parent; no new state or listeners |
| CTRL-056 | DONE, audit | Prior manager still receives automatic blur after graph move; supported cross-controller transfer UNKNOWN |
| CTRL-057 | DONE, audit | Partial bind confirmed, retry clean on observed route; converter unbind requires Object overload in fixture |
| CTRL-058 | DONE, audit | Disabled and denied first candidates shadow eligible actions without consumption; reservation contract open |
| CTRL-059 | DONE, audit | Both affordances in external-first order; external field-delegated handler under readOnly opens/writes once while inner/F9 zero |
| CTRL-060 | DONE, verify | 23/23 smoke modes; clean unchanged implementation HEAD |

Prior regression ownership: CTRL-049 `582a789` JInvTextFieldButtonSkin adds five lifetime-managed property registrations instead of direct listeners, uses shared overflow callback; no field-level state. CTRL-050 `6d36746` JInvTextArea adds one nullable reference, one lazy attachment/listener and one button while attached. No retained-byte estimate. Each prior commit includes only its named implementation class, focused probe and runner.

Highest-priority follow-up: isolate readOnly guard on external field-delegated LOV activation, keeping standalone/provider write semantics. Separately resolve validator transfer, Binder failure/cleanup, hotkey reservation, and external-vs-inner precedence only if caller contracts require changes. No implementation changes authorized or performed. Batch stops here.

---

## Reply CTRL-061

**DONE.** Baseline `ed5533d8d03c1ca1500b65660060d47ce336e3d3`; verified fast-forward HEAD `1db820ca0c7c0c3d2e32f1dbbfe92bc780485b1e`. Only JInvLOVButton.java, ExternalLovReadOnlySmoke.java and runner changed. Guard rejects explicit readOnly on JInvTextField/JInvTextArea before any focus scheduling or LOV dispatch. No new state, allocation, listener or binding; visual enabled appearance unchanged.

Windows Java 25 / JavaFX 25 shown Stage with AddLinkPane external-first arrangement and controlled provider. RED exit 1: locked external TextField with both ignoreEditable settings produced 2 opens/callbacks/copies/model writes/TAB events and 4 focus requests; locked TextArea button opened once. GREEN exit 0: zero on all measured locked paths, including TextArea. Editable external/inner/F9 opened once each. With editable=false but readOnly=false, existing external activation worked; ignoreEditable=true preserved external/inner/F9. Standalone and editable TextArea worked, field resumed after unlock, programmatic provider writes and direct showLOV preserved. Separate residual: explicit readOnly plus ignoreEditable=true still permits legacy F9 to open once (both RED and GREEN); this keyboard route is outside the button-only assignment. Native input, corporate DB and independent acceptance UNKNOWN.

Official ExternalLovReadOnly + 3 fresh JVMs, TextFields, ControlsFollowup, LovIntegration, LovDefaultValidation, LovListenerLifecycle, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal, TextAreaLovLifecycle, WrapButtonOwnership, RadioReadOnly, ComboReadOnly and CheckBoxReadOnly all exit 0. Maven compile, PowerShell parse and staged diff check passed. Windows: `src/test/probes/Test-LegacyGuiSmoke.ps1 -ExternalLovReadOnly -SettingsFile "D:\Java\.m2\settings.xml"`.

---

## Reply CTRL-062

**DONE.** Baseline `1db820ca0c7c0c3d2e32f1dbbfe92bc780485b1e`. Verified fast-forward production HEAD `f6a73c0d583b25012b2132fe643a89bea471b024`. Clean production checkout. Production change limited to `JInvTextArea.setLOV/showLOV` and a revision in the existing lazy `LovAttachment`. One primitive on that holder, zero added fields on controls without LOV, and no extra callback object per request.

**Windows Java 25 / JavaFX 25 RED → GREEN.** A → B and A → B → A: 1 copy / 1 write / 1 accepted navigation request each became 0/0/0. B → null: pre-fix null dereference and lost completion became 0/0/0 with one false callback. Each stale result preserves the reporting provider and triggers the caller's cancellation branch once. Focus/navigation figures count caller branch requests. Current acceptance stayed at 1 copy / 1 write / 1 accepted callback. Current/retired cancellation each stayed at 0/0/one false callback. Repeated same-provider setter, overlapping same-provider requests, bound setter rejection, and programmatic current-provider updates passed. TextField forwarded a late accepted result but preserved its draft in the diagnostic. TextField callback policy, direct writable lovProperty mutations and value notifications from an already reattached provider remain outside the fix.

`src/test/probes/Test-LegacyGuiSmoke.ps1 -TextAreaLovDelayedCompletion -SettingsFile "D:\Java\.m2\settings.xml"` exited 1 RED and 0 GREEN. Three fresh JVMs exited 0. TextAreaLovLifecycle, ExternalLovReadOnly, TextAreaSkinDisposal, LovIntegration, LovListenerLifecycle, BoundLovSetter, LovBindingInterleaving, Editors and FormLifecycle exited 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed. Native input and corporate DB were untested. Independent Windows acceptance pending.

---

## Reply CTRL-063

**DONE.** Baseline `f6a73c0d583b25012b2132fe643a89bea471b024`. Verified fast-forward production HEAD `33a73ce5ccfe77eb7a5bcdd9e65c060cce681e5c`. Clean checkout. One guard in `JInvTextField.initKeyBoard` before the F9 handler consumes, commits or dispatches. Zero new fields, listeners or allocations. Extended `ExternalLovReadOnlySmoke`. The only superseded contract assertion was the CTRL-061 sentinel for locked F9 with ignoreEditable=true.

**Windows Java 25 / JavaFX 25.** With readOnly=true, RED produced one open/callback/copy/write/TAB and consumed F9 in three of four editable/ignoreEditable combinations. GREEN produced zero for each count, zero focus requests and one event seen by the ancestor in all four. The existing external/inner routes, editable=false with ignoreEditable=true, direct showLOV, provider updates and unlock were verified. Native input and corporate DB untested.

`src/test/probes/Test-LegacyGuiSmoke.ps1 -ExternalLovReadOnly -SettingsFile "D:\Java\.m2\settings.xml"` exit 1 RED and 0 GREEN. Three fresh JVMs exit 0. TextFields, ControlsFollowup, LovIntegration, LovDefaultValidation, LovListenerLifecycle, TextFieldSkinDisposal, BoundLovSetter, LovBindingInterleaving, TextAreaLovLifecycle and TextAreaLovDelayedCompletion exit 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed. Independent Windows acceptance pending.

---

## Reply CTRL-064

**DONE.** Baseline `33a73ce5ccfe77eb7a5bcdd9e65c060cce681e5c`. Verified fast-forward production HEAD `5b4032851f6677dc6b5905147b2accb93d27e5a0`. Clean checkout. `DSFXAdapter.bindTable/close` retain and remove three registrations of one mouse handler. One nullable reference; one handler instance at bind replaces three and there is no per-event allocation. Extended the existing `TableAdapterCloseSmoke` with separate mouse assertions.

**Windows Java 25 / JavaFX 25.** Shown JInvTable, three synthetic mouse events. RED and GREEN each made three pre-close cursor refreshes, and the caller received three events. After two closes RED produced three null Dataset exceptions and the caller received no events. GREEN produced zero exceptions or refreshes, and the caller received all three events. Keyboard counts and independent late Dataset notifications passed in both versions. Native mouse, corporate SQL and paging teardown were outside these checks.

`src/test/probes/Test-LegacyGuiSmoke.ps1 -TableAdapterClose -SettingsFile "D:\Java\.m2\settings.xml"` exit 1 RED and 0 GREEN. Three fresh JVMs exit 0. DataBinding, ChoiceAdapter, ChoiceNullLookup, TableEditing, TableCancel and EditorReentry exit 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed. Independent Windows acceptance pending.

---

## Reply CTRL-065

**DONE.** Baseline `5b4032851f6677dc6b5905147b2accb93d27e5a0`. Verified fast-forward production HEAD `424dc86b9feeed9c0cc0ba1e0c383ae06384596e`. Clean checkout. Production limited to DSFXAdapter. Closing the paging owner removes its focus, skin, layout, bulk-append and current-scrollbar subscriptions. Its already queued PageDown completion checks the identity of the active owner before selecting. One nullable adapter reference, three fewer listener instances at bind, same one queued runnable per accepted page. Fetch logic, page sizing, keyboard/mouse disposal and Dataset ownership preserved.

**Windows Java 25 / JavaFX 25.** In-memory Dataset and visible JInvTable. Active scrollbar requested one fetch, active PageDown fetched one page and later selected row 40 in both table and Dataset. When closed twice before the next queued selection, RED selected 41 once and produced three scrollbar exceptions across original/replacement bar transitions. GREEN selection stayed at 0 with zero selection events, scrollbar exceptions, extra fetches, cursor requests or inserted rows. Independent late navigation, row and execute listeners each received one event. SQL paging and bulk fetch/search workers were untested.

`src/test/probes/Test-LegacyGuiSmoke.ps1 -TableAdapterPagingClose -SettingsFile "D:\Java\.m2\settings.xml"` exit 1 RED and 0 GREEN. Three fresh JVMs exit 0. TableAdapterClose (keyboard and mouse), DataBinding, TableEditing, TableCancel and EditorReentry exit 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed. Independent Windows acceptance pending.

---

## Reply CTRL-066

**DONE.** Baseline `424dc86b9feeed9c0cc0ba1e0c383ae06384596e`. Verified fast-forward production HEAD `6ccb4403d9322c3d4160f6513f6498fe003f9a5e`. Clean checkout. Only `JInvTextAreaSkin`, the focused fixture and its runner mode. One shared width listener per skin replaces listeners previously added to every button on each toolbar mutation. Exact width registrations are removed at item removal and disposal. The skin releases its own editor-button callback by identity at disposal. Caller handlers, bindings, and the public FE button's area association are preserved. No per-button helper maps, lists or listener objects. Four private references on the skin retain the two listeners, existing editor callback and its action until teardown as applicable.

**Windows Java 25 / JavaFX 25.** A retained caller button removed twice, moved to another toolbar, or present at disposal caused one downstream preferred-width change in the old toolbar in each RED case and zero in GREEN. Retired FE and editor buttons each caused one change in RED and zero in GREEN. The retired editor action's callback edge was present in RED and cleared in GREEN. Active and readded buttons still produced one downstream change, caller action/binding, selection, styling and visibility passed. Two safe test editor dispatches and one dispatch per caller button. Native external editor and FE dialog execution, heap-size measurement and independent acceptance were not tested.

`src/test/probes/Test-LegacyGuiSmoke.ps1 -TextAreaToolbarOwnership -SettingsFile "D:\Java\.m2\settings.xml"` exit 1 RED and 0 GREEN. Three fresh JVMs exit 0. TextAreaSkinDisposal, TextAreaLovLifecycle, TextAreaLovDelayedCompletion, ExternalLovReadOnly, Editors, ControlsFollowup and SceneKeyLookup exit 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed.

### Package summary CTRL-062–066

| Task | Status | Verified production SHA | Focused mode |
| --- | --- | --- | --- |
| CTRL-062 | DONE | `f6a73c0d583b25012b2132fe643a89bea471b024` | TextAreaLovDelayedCompletion |
| CTRL-063 | DONE | `33a73ce5ccfe77eb7a5bcdd9e65c060cce681e5c` | ExternalLovReadOnly |
| CTRL-064 | DONE | `5b4032851f6677dc6b5905147b2accb93d27e5a0` | TableAdapterClose |
| CTRL-065 | DONE | `424dc86b9feeed9c0cc0ba1e0c383ae06384596e` | TableAdapterPagingClose |
| CTRL-066 | DONE | `6ccb4403d9322c3d4160f6513f6498fe003f9a5e` | TextAreaToolbarOwnership |

All five focused modes reproduced RED (exit 1), then passed GREEN (exit 0). Fifteen fresh JVM runs and all 37 requested or related regression invocations exited 0. Five separate fast-forward implementation commits and five separate Replies. Final clean production HEAD `6ccb4403d9322c3d4160f6513f6498fe003f9a5e`.

Remaining scope includes TextField stale completion policy, direct writable LOV-property changes, value notifications after reattachment, SQL paging/bulk workers, native input and native editor behavior. Those were excluded or unverified in this batch. No task in this package is blocked. Stop after CTRL-066.

---

## Reply CTRL-067

**PARTIAL / READ-ONLY.** Baseline `6ccb4403d9322c3d4160f6513f6498fe003f9a5e`. Windows Java 25.0.1, JavaFX 25+29. Shown Stage, production JInvTable/DSFXAdapter and controlled ArrayDataSet A/B/C. The one swap attempt returns false without fetching SQL rows. In close cases the worker queues two calls to close on the FX thread before queuing result/selection delivery. Each case uses a fresh JVM.

| Case | Swap attempts | Result callbacks | Errors | Selection changes | Final selected / cursor |
| --- | ---: | ---: | ---: | ---: | --- |
| Search miss, open | 1 | 1 false | 0 | 0 | null / -1 |
| Search miss, close | 1 | 1 false after adapter detach | 0 | 0 | null / -1 |
| Bulk, open | 1 | 0 | 0 | 1 | C / 2 |
| Bulk, close | 1 | 0 | 2 NPE | 1 | C / -1 |

The closed bulk call throws from navigation listener restoration in `moveToLast` because `getDataSet()` returns null. Independently, rowLoader's two queued FX calls select C after both close calls, then fail at `dataSet.setCurrentRow`. The closed search result reaches the caller even after adapter detach. Its caller-specific correctness contract remains open. No database writes in this fixture.

Source ownership: the table moveToLast entry point is reachable from the keyboard manager. ProgressTaskExecutor uses a window-modal progress Stage with showAndWait; the worker owns the fetch loop, and the FX nested loop can deliver close. ProgressService handles task success/failure/cancel on FX and can queue error dispatch. DSFXAdapter.close tears down input, scroll and DataSet listeners; DSControlAdapter.close clears the DataSet reference. No close coordination with the progress executor is present. Search temporarily owns a row listener and removes it on the straight-line path, so exception-safe removal after registration is a separate candidate. The no-consumer miss alert, close while the fetch is already blocking, cancellation, error-service dispatch, SQL cursor and actual business-form teardown are UNKNOWN.

Next smallest RED→GREEN: scope to DSFXAdapter bulk ownership at close. Validate the original attachment before restoring navigation or positioning and at both queued selection/cursor deliveries; preserve normal selection and separate the worker's cancellation policy. Establish stale-search-result policy separately before suppressing a caller callback; probe exceptions with exact search row-listener removal. Closing the adapter provides no SQL rollback guarantee.

Reproduction: `target/ctrl-067-research/BulkLifecycleAudit.java` (ignored). Prepend IDEA Maven 3.9.16 bin to Path, set MAVEN_ARGS=-o; `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterPagingClose -SettingsFile D:\Java\.m2\settings.xml` exit 0 (wrapper-only first attempt exit 1 before compile). Use the generated dependencies.txt as DEP in `javac -cp "target/classes;DEP" -d target/ctrl-067-research target/ctrl-067-research/BulkLifecycleAudit.java` (exit 0). Run `java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-067-research;target/classes;DEP" BulkLifecycleAudit OP STATE` with OP search/bulk and STATE open/close; all four diagnostic processes exit 0 after reporting observations. The initial search-open launch reported the same result but remained alive after Platform.exit; the completed matrix uses explicit diagnostic process exit. Production and tracked test code unchanged.

---

## Reply CTRL-068

**PARTIAL / READ-ONLY.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e. Java 25.0.1 / JavaFX 25+29. Shown Stage with JInvTable, DSFXAdapter, production text renderer and ArrayDataSet A/B/C. Positive F8 release on A emits commit row 0/A followed by one A write; cursor 0. After setCurrentRowNum(1), selection and editor still point to A, cursor 1. F8 again commits exactly once to A and cursor returns to 0; B/C unchanged. After the same model move, unarmed table.edit(-1,null) emits zero commits/writes; cursor stays 1. Direct cursor/selection divergence deserves the explicit navigation-event probe in CTRL-072.

Source: conversion precedes commit; UP/DOWN/ENTER also navigate between editors. The mouse filter caches a TablePosition, and cancelEdit may explicitly dispatch a commit through it; Escape clears the marker. See prior GUI-006 matrix. Physical input, rejected conversion and validation, fresh mouse navigation and unrelated model change around cancel remain UNKNOWN in the new binding fixture. Minimum next probe is rejected-conversion and mouse-armed cancellation with explicit selected identity and commit counts.

Temporary target/ctrl-table-research/EditAudit.java. javac -cp "target/classes;DEP" -d target/ctrl-table-research target/ctrl-table-research/EditAudit.java: initial exit 1 due to probe event type inference, explicit event type exit 0. DEP is dependencies.txt from the baseline CTRL-067 smoke. java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-table-research;target/classes;DEP" EditAudit MODE, normal/cursor/cancel exits 0. No tracked production/test changes.

---

## Reply CTRL-069

**PARTIAL / READ-ONLY — wrong-row writes reproduced.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e, Java 25.0.1 / JavaFX 25+29. Shown JInvTable, DSFXAdapter, JInvTextFieldTableCell and ArrayDataSet sharing an ObservableList. Start editing A at row 0, enter draft, optionally fire synthetic MOUSE_PRESSED at the table, then mutate the source list. Editor closes in all cases. Without press, replacement, removal and permutation each yield zero commits and writes. With press each yields one commit and one wrong-row write: replacement sets X=draft (no selected item, cursor 0), removal sets B=draft (selected B, cursor 0), descending permutation sets C=draft (selected A, cursor 2). Original A stays unchanged. Commit happens synchronously inside mutation, before any attempted F8 delivery. Normal F8 on unchanged rows writes A once (CTRL-068).

Renderer cancelEdit fires explicit commit using saved TablePosition. It resolves the occupant of the old index, which can change. Proposed minimum fix: scope to synthetic cancel commit in JInvTextFieldTableCell, verify identity captured at edit start or arm before delivery, and reset the per-edit marker at lifecycle boundaries. Preserve same-row mouse commit, ordinary commit and Escape. Draft recovery policy, actual SQL sorting/refresh, duplicate sort keys, failed validation and explicit reentry remain UNKNOWN. DSFXAdapter schedules refresh after EXECUTE and uses OrderByManager/SQL query paths; the fixture specifically covers local observable-list mutations.

Run the CTRL-068 javac and java commands for target/ctrl-table-research/EditAudit.java with MODE replace/remove/reorder and optional second argument arm. Compile exit 0; all six JVMs exit 0. No tracked production/test changes.

---

## Reply CTRL-070

**PARTIAL / READ-ONLY.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e; Java 25.0.1 / FX 25+29. Real Binder, FXEntity and three TextFields on a shown Stage. On each of two fresh fixtures, two text writes and one direct model write produced three model events and nine text events. Explicit repeat bind preserved these counts in this fixture (contract for general repeat bind UNKNOWN). After a Stage close request hid the window, intentionally retained controls and entity still had the same bidirectional counts. StringProperty.unbindBidirectional(Property) left the converter binding active; a text write changed model to “unbound”. Bindings.unbindBidirectional(Object,Object) removed that route: subsequent text write left model at “unbound”.

AbstractBaseController.bindControls resolves labels and invokes Binder with selected controls. TextField uses textProperty().bindBidirectional(entityProperty, TFSCnv); other control routes use direct properties and some use their own specialized adapters/validators. The base close path invokes the return callback, resource hook and owned TaskContext close; the base closeResources hook is empty. The Stage fixture does not drive the controller launcher or full cleanup path. No retained-heap conclusion or claim that Stage close must detach a retained form. Actual full form close, validators/adapters/LOV release and application-owned model reuse remain UNKNOWN.

Minimum follow-up: real launched form with a reused external entity, explicit ownership of one converter binding, and verified WINDOW_HIDDEN and closeResources before checking stale writes. Use the Object/Object unbind route if an explicit cleanup contract for that binding is established. No rollback or broad cleanup change proposed.

Temporary probe target/ctrl-070-research/BinderSuccess.java, derived from CTRL-057; javac -cp "target/classes;DEP" -d target/ctrl-070-research target/ctrl-070-research/BinderSuccess.java and java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-070-research;target/classes;DEP" BinderSuccess. DEP from CTRL-067 runner. Both exit 0. No tracked changes.

---

## Reply CTRL-071

**PARTIAL / READ-ONLY.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e; Java 25.0.1 / FX 25+29. Real FXFormLauncher/SDI, FXML with synthetic singleton controller and stub app preferences, no DB. Replayed CTRL-042 diagnostic matrix with failure in init. Replayed with failure immediately after super.bindControls. The second fixture has no entity fields, so it traverses the binding hook but does not prove cleanup of installed bindings.

Both runs: Cancel veto and rejected OK leave one visible owner with no callbacks or resource cleanup; duplicate show remains suppressed. Accepted close permits reopening. A gated closeNow shows the owner already hidden and its callback delivered with cleanup count still 0; after the progress WINDOW_HIDDEN cleanup is 1. The early assertion printed by the diagnostic intentionally distinguishes those phases. Each injected init failure leaves the owner hidden, reports the original exception once, completes one cleanup after the gate, and allows a fresh launch. Matrix reaches five accepted controller launches and five callbacks after final close. No orphan singleton was observed on the exercised branches.

Source: registration precedes Stage show; showing listener calls internalInit, transitions to RUNTIME on success, records/dispatches init errors and requests close. Top-level launcher catch unregisters a controller it acquired. Return callback precedes resource teardown in a finally block. The probe covers actual SDI launcher, accepted and vetoed requests, and resource progress Stage. Failure after real field binding, MDI/WAIT, exception in custom close veto of a failed stage and external model effects are UNKNOWN. Smallest next evidence for CTRL-070/071: bind one retained model property in the real launcher, fail immediately after installation, wait for real cleanup and reopen with the model.

Run java --enable-native-access=ALL-UNNAMED -cp "target/ctrl042/diagnostic;target/classes;DEP" SingletonCloseVetoSmoke matrix (exit 0). Compile target/ctrl-071-research/SingletonCloseVetoSmoke.java using javac -cp "target/classes;DEP" -d target/ctrl-071-research (exit 0) and substitute target/ctrl-071-research for the diagnostic directory in the java command (exit 0). DEP generated in CTRL-067. Tracked code unchanged.

---

## Reply CTRL-072

**PARTIAL / READ-ONLY.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e; Java 25.0.1 / FX 25+29. Actual shown JInvTable and DSFXAdapter with ArrayDataSet; subclass counts calls to production refresh and emits genuine AFTER EXECUTE notifications through DataSet, without SQL. FX queue barriers delimit each observation.

A selected at cursor 0, then DataSet cursor 1: immediately selected A/cursor 1; after queue drain, selected B/cursor 1 with one selection and no extra navigation. Three AFTER EXECUTE notifications in one turn produced exactly three refresh calls and zero selection/nav events. Shared-list clear left selected null and old cursor 1; replace with X/Y/Z and setCurrentRowNum(1) left selection null with no new navigation event. These list mutations bypass Dataset mutation APIs; their behavior is not evidence of SQL cursor corruption.

Confirmed stale work: from null selection with X/Y/Z, set cursor 0 then issue AFTER EXECUTE and close twice in the same FX turn. Before drain, selection null and cumulative refresh 4. After drain, retired adapter selected X once and cumulative refresh 5. Late navigation to 2 and EXECUTE raised after close were visible to external observers but did not add refresh/selection. The queued work originated in positionRow and dataSetChanged before teardown, independently of CTRL-065 paging guards. No model writes. Proposed minimal DSFXAdapter fix: make those two queued deliveries conditional on their originating attachment still being active. Probe active queued selection and refresh plus closed negative cases before any coalescing. Same-table rebind remains out of scope.

No SQL execute/refresh, supported DataSet update/remove API, duplicate stable keys or fresh second table verified in this probe. Temporary target/ctrl-072-research/RefreshAudit.java, javac -cp "target/classes;DEP" -d target/ctrl-072-research target/ctrl-072-research/RefreshAudit.java; java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-072-research;target/classes;DEP" RefreshAudit. Both exit 0, DEP from CTRL-067. No tracked changes.

---

## Reply CTRL-073

**PARTIAL / READ-ONLY.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e, Java 25.0.1 / FX 25+29. Shown Stage; production JInvTextField, external JInvLOVButton, F9 KEY_RELEASED, and provider with controlled delayed completion. Matrix: three entry routes × keep/replace/detach/reattach × accepted/cancelled (24 cases), each opening exactly once. A→B→A is the reattach case. Completion alone produced zero text-model writes and preserved the draft across all cases. Direct caller got exactly one Boolean on each completion, including true for stale success. Both real UI routes emitted one synthetic TAB on every accepted result including each stale result, and zero TAB on cancellation. A filter counts and consumes TAB, so traversal intent is proven and actual final focus owner was not measured. The forwarding source also requests focus on accepted TextField results, and caller closures request focus on cancellation.

Notifications separately: after explicitly attaching B, retired A notification writes 0; current B notification writes 1; reattached A notification writes 1. The first notification check mislabeled A as retired while still attached and was corrected and rerun. A provider value update carries no choice-request identity in these paths; a new legitimate update and an old choice updating the same reattached provider cannot be attributed per request by this fixture. TextArea CTRL-062 positive and stale direct matrix passed in both runs. No production calls chaining lovProperty().set/bind found by source search; public property and bound failure remain part of separate contract review.

Proposed minimum follow-up: define stale-result delivery contract for TextField, use attachment lifetime in completion handling and cover A→B→A plus overlapping same-provider requests and normal F9/button forwarding. Keep notification, readOnly, direct property, and provider API semantics out of that fix. Actual focus transfer and downstream form action at the next node remain UNKNOWN.

Temporary target/ctrl-073-research/ResidualLovAudit.java; javac -cp "target/classes;DEP" -d target/ctrl-073-research target/ctrl-073-research/ResidualLovAudit.java; java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-073-research;target/classes;DEP" ResidualLovAudit. Both compiles and both runs exit 0. DEP from CTRL-067. No tracked changes.

---

## Reply CTRL-074

**PARTIAL / READ-ONLY — final batch report.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e. Windows JDK 25.0.1+8-LTS-27, JavaFX 25+29, heap -Xms256m -Xmx512m. Two controlled workloads ran on the FX thread. For each N: five warm-up batches and nine measured batches, 100 calls per batch. Wall time from System.nanoTime; FX-thread allocated bytes from com.sun.management.ThreadMXBean. Values are median [min–max] per call over samples.

| Workload | N | µs/call | Allocated B/call |
| --- | ---: | ---: | ---: |
| Controls.getControlList, flat VBox/TextField | 100 | 8.29 [7.68–15.98] | 4,632 |
| same | 1,000 | 22.77 [22.04–23.61] | 15,024 |
| same | 5,000 | 152.11 [137.67–315.93] | 75,240 |
| AFTER EXECUTE dispatch and FX refresh queue drain | 100 | 45.09 [41.52–59.64] | 201.2 |
| same | 1,000 | 48.91 [43.23–69.99] | 201.2 |
| same | 10,000 | 48.34 [41.55–58.01] | 201.2 |

Allocation range equals the reported median for each row. Traversal excludes control construction, CSS, Binder and FXML and checks result size. The flat detached tree is a control-count scaling fixture. Refresh uses an attached shown JInvTable, real adapter, preloaded rows and DataSet notification emitter, and validates 100 actual refresh calls per batch. That elapsed interval includes queue scheduling and can include unrelated FX work; it does not isolate CPU or capture end-to-end paint. Only the FX thread is counted. No retained heap, cell rebuild cost, SQL latency or production event-rate estimates. Temporary target/ctrl-074-research/HotPaths.java. javac -cp "target/classes;DEP" -d target/ctrl-074-research target/ctrl-074-research/HotPaths.java; java -Xms256m -Xmx512m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-074-research;target/classes;DEP" HotPaths. Compile and run exit 0. DEP from CTRL-067.

### Ranked candidates

| Rank | Location and evidence | Minimum next task, tradeoff and risk |
| --- | --- | --- |
| 1 | JInvTextFieldTableCell.cancelEdit, CTRL-069 reproduced three wrong-entity writes | RED→GREEN guard the saved edit position by original entity identity, preserving valid same-row commit and Escape. Small scope, data-integrity priority. Decide draft recovery separately. |
| 2 | DSFXAdapter moveToLast/rowLoader, CTRL-067 two bulk NPEs and post-close selection | RED→GREEN bind cleanup and both queued deliveries to original live attachment. Preserve active selection; worker cancellation and SQL semantics need their own evidence. Small adapter scope, concurrency sequencing risk. |
| 3 | DSFXAdapter positionRow/dataSetChanged, CTRL-072 stale refresh and selection | RED→GREEN check attachment for work queued before close; retain active ordered navigation. Small scope. Then test burst coalescing separately only if actual caller/event order permits; timing shows queue overhead, paint savings unmeasured. |
| 4 | JInvTextField.showLOV, CTRL-073 stale success emits TAB on F9/button | Define completion policy and then RED→GREEN attachment lifetime guard for those paths. Preserve normal completion, readOnly and notifications. Small scope with direct caller callback compatibility risk. |
| 5 | AbstractBaseController.bindControls / Controls.getControlList | Two scans around label initialization with different filters. Measured traversal allocation grows with matched controls. Profile both real form passes before sharing results; account for traversal exclusions, label effects and filter order. Modest code scope with contract risk; avoid retaining a cache per form without evidence. |
| 6 | Binder and DSFXAdapter mark/edit rendering | TFSCnv already shared; converter bindings still attach per property. Radio-group converter construction is a separate route. Mark cell value factory constructs a BooleanProperty each invocation; text renderer lazily constructs an editor and attaches input filters per edit. No measured hotspot or retained heap. Count invocations in warmed representative forms/visible rows before changing lifetimes. Avoid caching row properties without identity/notification contracts. |

DSFXAdapter directly reuses an ObservableList from the DataSet or wraps a non-observable list. No second full row cache identified in that path. SQL sort/full-scan and transaction behavior remain outside the measurements. For end-to-end next measurement, vary loaded N and visible row count independently, count cell factory/value callbacks and FX pulses after single and burst EXECUTE, capture FX-thread allocations and elapsed-to-layout. For form launch, instrument FXML, both traversals, Binder, first CSS/layout and cleanup in a representative local form.

### Batch summary

| Reply | Status | Evidence / remaining limit |
| --- | --- | --- |
| CTRL-067 | PARTIAL | Reproduced stale bulk selection, two NPEs and stale search callback. Blocking SQL/cancellation/error matrix open. |
| CTRL-068 | PARTIAL | F8 writes A with or without interim model cursor movement; unarmed cancel zero writes. Rejected conversion and full navigation matrix open. |
| CTRL-069 | PARTIAL | Wrong-row write on armed replace/remove/permutation. Server paths and draft policy open. |
| CTRL-070 | PARTIAL | Converter route persists on retained controls after Stage hide; Object/Object unbind detaches it. Full business-form teardown open. |
| CTRL-071 | PARTIAL | Real SDI launcher veto/reopen, error callback and cleanup verified. Failure after real model binding and MDI/WAIT open. |
| CTRL-072 | PARTIAL | Queued pre-close refresh/navigation survive teardown; post-close notifications detached. SQL and mutation APIs open. |
| CTRL-073 | PARTIAL | Stale accepted TextField callback emits TAB via F9/button; zero model writes on completion alone. Final focus and same-provider value attribution open. |
| CTRL-074 | PARTIAL | Two measured local workloads; SQL, retained heap and paint need dedicated measurements. |

All eight replies are individually published on agent/codex-mailbox. The production checkout and remote baseline remain 6ccb4403d9322c3d4160f6513f6498fe003f9a5e. No change to tracked production or tests. No independent user acceptance asserted. Stop after CTRL-074.

---

## Reply CTRL-075

**DONE.** Baseline 6ccb4403d9322c3d4160f6513f6498fe003f9a5e; production eaf0bed0be6a888ce259a69b2899b49da8ad7f57, verified fast-forward. Only JInvTextFieldTableCell, TableEditRowIdentitySmoke and runner mapping.

Actual shown JInvTable/DSFXAdapter/ArrayDataSet and production cell; stable row identity independent of equals, equal-valued replacement X and duplicate sort keys A/B. Baseline RED: one commit and one write each to X on replacement, B on removal, C on permutation; a subsequent unarmed same-index reentry also emitted one commit. Final GREEN: zero commits/writes in all invalidated armed and unarmed mutations; reentry unarmed cancel zero and reentry F8 exactly one write to X. Same-row armed cancel, F8 and ENTER each commit/write A once; Escape followed by cancellation produces zero. Assertions unchanged.

One nullable row reference per cell, captured for the edit and cleared at reentry/cancel/commit; the saved position must still refer to the original object, same table and column, before conversion and before dispatch. No new allocation for guards, no change to normal commit or Dataset/transaction paths. An invalidated draft may be discarded by existing cancel behavior. SQL refresh/server sort, native input, converter-rejection during this specific synthetic route and draft recovery remain unverified.

Windows Java 25.0.1 / FX 25: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableEditRowIdentity -SettingsFile "D:\Java\.m2\settings.xml" exit 1 on baseline and 0 on fixed build. Final source was refined to snapshot before the start-event callback and recheck after conversion; Maven -Pbuild compile succeeded and the final probe passed three fresh JVMs (javac/java with the runner classpath and --enable-native-access=ALL-UNNAMED). Regressions TableEditing, TableCancel, EditorReentry, DataBinding, TableAdapterClose, TableAdapterPagingClose, MoneyDecimal, LovDefaultValidation all exit 0 on final build. PowerShell parser and git diff --check exit 0. IDEA Maven 3.9.16 with MAVEN_ARGS=-o. Independent acceptance pending.

---

## Reply CTRL-076

**DONE.** Baseline eaf0bed0be6a888ce259a69b2899b49da8ad7f57; production 07a51fda7407161b9f1a1eb2bed04930bc6fc0b9, verified fast-forward. Only DSFXAdapter, TableAdapterBulkCloseSmoke and runner mapping.

The in-memory Dataset on a shown JInvTable uses one controlled swap attempt; close runs in the nested FX progress loop. RED: double close before selection yields one stale C selection and two NPEs (listener restoration and cursor delivery). Closing from the first C selection before the deferred cursor action also yields two NPEs. GREEN: before selection, zero selection, zero explicit cursor writes and zero errors; between deliveries, the already delivered C remains, no new cursor request, and zero errors. Closed cursor stays -1 in both. Both closed cases have one initial adapter navigation registration and no restore. Active completion selects C once, requests cursor once, ends at cursor 2 and restores the adapter exactly once; subsequent navigation selects B. External observer still gets navigation after close.

Bulk completion checks its originating DataSet attachment before restoring the listener, positioning, and each deferred delivery, including the later bulk positioning. No new instance fields or additional queued callbacks. Existing callbacks capture an attachment reference. Fetch loop and progress executor behavior preserved. Blocking SQL, cancellation/error routes and search callbacks remain outside this verified fix; no SQL rollback/cancellation guarantee.

Windows Java 25.0.1 / FX 25. powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterBulkClose -SettingsFile "D:\Java\.m2\settings.xml" exits 1 RED and 0 GREEN. Two additional fresh JVMs invoke TableAdapterBulkCloseSmoke directly with the same Maven classpath (java --enable-native-access=ALL-UNNAMED -cp "<probe-output>;target/classes;<runner-dependencies>" TableAdapterBulkCloseSmoke), both exit 0. TableAdapterClose, TableAdapterPagingClose, DataBinding, TableEditing, TableCancel, EditorReentry and TableEditRowIdentity each exit 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed. IDEA Maven 3.9.16, offline local cache. Independent acceptance pending.

---

## Reply CTRL-077

**DONE.** Baseline 07a51fda7407161b9f1a1eb2bed04930bc6fc0b9; production f6bfd8bb2a0ca38cf36ea538140bff1f49744c7d, verified fast-forward. Only DSFXAdapter, TableAdapterQueuedCloseSmoke and runner mapping.

On actual shown JInvTable and ArrayDataSet, notifications queued in the same FX turn as double close were tested separately and together. RED: one obsolete selection B (navigation only), one obsolete refresh (AFTER EXECUTE only), both deliveries (combined). GREEN: zero of either, original selection A preserved; only the fixture's explicit cursor requests occurred (2/1/2 respectively); zero errors and unchanged independent observer notifications including events sent after close. Active and fresh adapter/table delivered selection B, refresh, selection C, refresh, refresh in that order, with cursor 2 and two explicit requests.

The existing queued callbacks check that their captured DataSet still owns the attachment at delivery. General positionRow reuses the guarded scheduling introduced for bulk, and AFTER EXECUTE retains one callback per event. No per-instance fields, extra queued wrapper, coalescing or SQL/cursor contract changes. SQL-driven execute/refresh behavior and same-table rebind remain outside scope.

Windows Java 25.0.1 / FX 25: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterQueuedClose -SettingsFile "D:\Java\.m2\settings.xml" exits 1 RED, 0 GREEN. Two additional fresh JVMs invoked TableAdapterQueuedCloseSmoke directly with the same classpath and --enable-native-access=ALL-UNNAMED, both exit 0. TableAdapterClose, TableAdapterPagingClose, TableAdapterBulkClose, DataBinding, TableEditing, TableCancel, EditorReentry and TableEditRowIdentity each exit 0. Maven -Pbuild compile, PowerShell parse and staged diff check passed. Independent acceptance pending.

---

## Reply CTRL-078

**DONE.** Baseline f6bfd8bb2a0ca38cf36ea538140bff1f49744c7d; final production 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab, verified fast-forward. Only JInvTextField, TextFieldLovDelayedCompletionSmoke and runner mapping.

Shown Stage, real TextField with direct, F9 and external-button routes. 24 delayed-completion cases: three routes × same-provider repeat / replace / detach / A→B→A × accepted/cancelled. RED: all three stale accepted transitions delivered true directly and emitted one TAB each via F9 and button. GREEN: each completed direct call returned one false with its reporting provider; both UI routes emitted zero stale TAB, with the existing one synchronous cancellation focus request. Valid accepted F9/button still emit one TAB, valid direct receives true once, and cancellations emit zero TAB. In all cases completion alone had zero model writes and preserved the distinct draft. Two overlapping current choices both completed accepted; bound replacement and unbind both rejected without invalidating a pending current request. Current/replacement programmatic value updates wrote once each; a retired notification wrote zero. readOnly blocked F9/button, direct programmatic open and completion remained available.

Generation is stored as one long on the existing lazy LOV value listener; no eager per-field object or added callback count. Explicit setter/unbind updates generation before JavaFX property notification only for a writable changed provider; same-provider repeats and rejected bound operations preserve it. Each existing completion captures the originating provider and generation and still reports its original source. Direct writable lovProperty attachment semantics, reattached-provider value events without choice identity, actual focus traversal and the specialized TextFieldCUR override remain outside this fix.

Windows Java 25.0.1 / FX 25: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TextFieldLovDelayedCompletion -SettingsFile "D:\Java\.m2\settings.xml" exits 1 RED and 0 GREEN. Two fresh direct JVMs on the same classpath with --enable-native-access=ALL-UNNAMED exit 0. TextAreaLovDelayedCompletion, ExternalLovReadOnly, LovListenerLifecycle, LovIntegration, LovDefaultValidation, BoundLovSetter, LovBindingInterleaving, TextFieldSkinDisposal, WrapButtonOwnership and TextAreaLovLifecycle all exit 0. All three prior focused package probes pass again on final HEAD. Maven -Pbuild compile, PowerShell parse and staged diff check passed.

### Package CTRL-075–078

| Task | Status | Verified production SHA | Focused runner |
| --- | --- | --- | --- |
| CTRL-075 | DONE | eaf0bed0be6a888ce259a69b2899b49da8ad7f57 | TableEditRowIdentity |
| CTRL-076 | DONE | 07a51fda7407161b9f1a1eb2bed04930bc6fc0b9 | TableAdapterBulkClose |
| CTRL-077 | DONE | f6bfd8bb2a0ca38cf36ea538140bff1f49744c7d | TableAdapterQueuedClose |
| CTRL-078 | DONE | 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab | TextFieldLovDelayedCompletion |

All four deterministic assertions failed on their unchanged production baseline (exit 1), then passed on the isolated fix (exit 0) in at least three fresh JVMs each. Four separate production commits and four mailbox Replies. The tested synthetic cancel route no longer writes to the new occupant of an invalidated index; an invalidated draft may be discarded. SQL refresh/sort, blocking/cancelled/error bulk SQL, other search completion paths, native input and focus traversal, and draft recovery remain unverified. No transaction changes or performance optimization. Independent Windows acceptance pending. Stop after CTRL-078.


---

## Reply CTRL-079

**BLOCKED / READ-ONLY.** Baseline and final production: 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab. Inbox assignment: ad7ef12bbc59443c0c2cbd9194c91332463ff192. Local production and mailbox checkouts were clean before the reply. Java 25.0.1, JavaFX 25, IDEA Maven 3.9.16 offline with corporate settings. No production or tracked test edits.

Maven compile exit 0, PowerShell runner parse clean and git diff --check exit 0. Commands from the project root (IDEA Maven bin on PATH and MAVEN_ARGS=-o):

    mvn -s "D:\Java\.m2\settings.xml" -Pbuild "-Dmaven.buildNumber.skip=true" compile
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -<Mode> -SettingsFile "D:\Java\.m2\settings.xml"

Default omits the mode switch. Enumerated 48 unique named parameter sets plus default. No opt-in Robot flag or unresolved CTRL-014/034 scenario enabled. SceneKeyLookup is an existing tracked smoke and uses its existing Node.properties reflection assertion. No new reflective fixture was introduced.

All 49 modes printed PASS. Initial runner exits were 48 × 0 and one × 1. SceneKeyLookup printed CTRL-024 PASS and expected action/target counts, then the runner threw at the post-Java exit check. The runner does not log the numeric exit of that child. No Java assertion failure or hs_err file was observed. An immediate fresh official runner repeat and a direct JVM repeat with the original generated java.args both printed PASS and exited 0. The initial nonzero remains unexplained. The assertion results do not establish a production defect, and the repeats do not establish why the process failed. Treat the mandatory gate as blocked by that unexplained exit.

Initial matrix. Columns are mode, runner exit, elapsed wall seconds including compilation and startup, probe result.

    TextAreaToolbarOwnership 0 10.4089954 CTRL-066 PASS
    TableEditRowIdentity 0 11.0049056 CTRL-075 PASS
    TableAdapterBulkClose 0 10.59915 CTRL-076 PASS
    TableAdapterQueuedClose 0 10.709053 CTRL-077 PASS
    TextFieldLovDelayedCompletion 0 9.7863609 CTRL-078 PASS
    TableAdapterPagingClose 0 9.4995048 CTRL-065 PASS
    TextAreaLovDelayedCompletion 0 9.0577284 CTRL-062 PASS
    ExternalLovReadOnly 0 9.5448912 CTRL-061 PASS
    WrapButtonOwnership 0 9.2693942 CTRL-055 PASS
    TabPaneSkinListeners 0 9.1558919 CTRL-054 PASS
    TextAreaSkinDisposal 0 9.34208 CTRL-053 PASS
    ValidatorSubchanges 0 8.8566039 CTRL-052 PASS
    TableAdapterClose 0 8.9983458 CTRL-051 PASS
    TextAreaLovLifecycle 0 9.4631667 CTRL-050 PASS
    TextFieldSkinDisposal 0 9.5095522 CTRL-049 PASS
    LovBindingInterleaving 0 9.3403777 CTRL-045 PASS
    BoundLovSetter 0 9.3627488 CTRL-044 PASS
    WrapButtonDeferred 0 9.3409054 CTRL-043 PASS
    Editors 0 9.6508764 GUI-004 PASS
    TableEditing 0 9.4594509 GUI-005 PASS
    TableCancel 0 9.6852539 GUI-006 PASS
    EditorReentry 0 10.3120722 GUI-007 PASS
    DataBinding 0 9.8233548 GUI-009 PASS
    FormLifecycle 0 9.7826632 GUI-010 PASS (standalone production FXML/controller and SDI close)
    FormCancel 0 10.3968492 CTRL-022 PASS
    SceneKeyLookup 1 12.8452448 CTRL-024 PASS
    LovListenerLifecycle 0 9.0651156 CTRL-026 PASS
    LovDefaultValidation 0 9.0485392 CTRL-027 PASS
    LovIntegration 0 9.0781832 CTRL-031 PASS
    SingletonCloseVeto 0 9.6085215 CTRL-032 PASS
    SingletonLifecycleMatrix 0 9.6578388 CTRL-035 PASS
    ValidatorScopedUnbind 0 8.7063644 CTRL-033 PASS
    ValidatorMultiControl 0 9.2296071 CTRL-036 PASS
    TextFields 0 9.8368622 GUI-012 PASS (tested matrix)
    ValueFields 0 9.8517322 VF-001 PASS
    ValueLifecycle 0 9.1175634 VF-002 PASS (custom listener ownership)
    MoneyDecimal 0 10.0795171 NUM-001 PASS (tested invariants)
    DatePickerBinding 0 9.2161519 CTRL-002 PASS
    NodeTraversal 0 8.997436 CTRL-003 PASS
    RadioReadOnly 0 9.1981786 CTRL-006 PASS
    ComboReadOnly 0 15.1194118 CTRL-007 PASS
    CheckBoxReadOnly 0 14.9760734 CTRL-009 PASS
    ValueChangedFlag 0 7.5886353 CTRL-013 PASS
    ControlsFollowup 0 8.8960561 CTRL-CONTROLS-FOLLOWUP PASS
    ChoiceAdapter 0 8.8613259 CTRL-015 PASS
    BinderChoiceNull 0 8.9969189 CTRL-017 PASS
    ChoiceNullLookup 0 8.7426431 CTRL-018 PASS
    ChoiceNonNullSet 0 8.4822942 CTRL-019 PASS
    Default 0 9.6485379 GUI-001 PASS

### Batch disposition

CTRL-080, CTRL-081, CTRL-082, CTRL-083, CTRL-084, CTRL-085, CTRL-086, CTRL-087, CTRL-088, CTRL-089 and CTRL-090 are NOT_RUN under the gate stop rule. No task measurements or production changes. Before resuming, reproduce SceneKeyLookup with the original tracked assertions and collect the child JVM exit and Windows process/crash diagnostics separately from the PowerShell runner exit. Re-run the baseline gate after explaining the failure.


---

## Reply CTRL-079A

**DONE / READ-ONLY.** Baseline and final production 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab. Inbox assignment 1ace50d8d01f7889ac255183d754c61c78d1ad4b. No production, tracked test, or tracked runner edits.

Original evidence: 2026-11-27 12:09:01–12:09:13 +03:00. Maven BUILD SUCCESS, expected focus/action counters and CTRL-024 PASS, followed by the runner exception at its post-Java LASTEXITCODE check and outer exit 1. The original native child code and PID were not logged. The test prints PASS before Platform.exit() in finally, so assertion success cannot identify the exit phase. No Application events matched IDs 1000, 1001, 1002 or 1026 in the checked original window 12:08:45–12:10:00 +03:00. Cause UNKNOWN.

Windows 11 amd64 build 26100.9711, Windows PowerShell 5.1.26100.9711, Oracle Java and javac 25.0.1 (HotSpot 25.0.1+8-LTS-27), JavaFX 25 Windows jars, IDEA Maven 3.9.16 offline with D:\Java\.m2\settings.xml. java.exe and javac.exe were resolved from C:\Program Files\Common Files\Oracle\Java\javapath. JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS and _JAVA_OPTIONS were unset. No extra JVM flags or system settings changed.

Two focus runs use the unchanged official runner, eight use a diagnostic copy and twenty launch the original generated java.args directly. The diagnostic copy resolves the original repository and tracked probe paths and writes LASTEXITCODE immediately after the unchanged Java invocation, before the original exit check. The 49-mode battery uses that diagnostic copy for per-mode native and outer exit evidence. All 48 named modes plus Default, no opt-in Robot or unresolved CTRL-014/034 routes. Default omits the mode switch.

    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -SceneKeyLookup -SettingsFile "D:\Java\.m2\settings.xml"
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\target\ctrl-079a\InstrumentedRunner.ps1 -SceneKeyLookup -SettingsFile "D:\Java\.m2\settings.xml"
    java "@D:\Java\Projects\fore25\target\ctrl-024-46fda821fc014afc825a7bb6af3b9c88\java.args"
    mvn -s "D:\Java\.m2\settings.xml" -Pbuild "-Dmaven.buildNumber.skip=true" compile

Commands use the IDEA Maven bin on PATH and MAVEN_ARGS=-o. The instrumented runner expects CTRL079A_CHILD_EXIT to name the per-run sidecar. Locally, target/ctrl-079a holds original and repeat logs, matrix.csv, separate per-run stdout and stderr, child-exit sidecars, timestamps, PID (PowerShell for runner modes, Java for direct), timeout field, scripts, environment and Windows/JVM diagnostic checks. Generated JVM inputs are preserved in their original target directories. The initial recorder did not retain the process handle; focus-01 through focus-03 retain actual output and available child codes, with outer codes marked UNKNOWN. The recorder was interrupted during focus-04 to correct capture. Its Maven log and generated arguments remain available; no JVM output, PASS or exit evidence was captured for that interrupted attempt. The other three initial records were preserved. focus-31 completed the planned 30 completed runs. After retaining the handle, remaining runner exits were collected. The official runner does not expose a successful Java child's immediate numeric exit. No outer runner exists for direct launches.

Original failure NOT_REPRODUCED. Thirty focus runs printed CTRL-024 PASS. All 28 recorded focus JVM exits and all seven recorded focus PowerShell exits were 0. All 49 gate modes printed PASS and both exit codes were 0 in all 49. No timeouts. Recommend that the architect decide whether to accept the residual one-off UNKNOWN risk. The original cause and phase remain UNKNOWN.

Application events via UTC XPath, 13:07:00-13:19:00 +03:00: NoMatchingEventsFound,Microsoft.PowerShell.Commands.GetWinEventCommand. JVM hs_err files in scanned working/output paths: 0. Checks: compile=0, diff-check=0, PowerShell parse=0.

Matrix: run, mode, launch kind, process PID, JVM exit (decimal and hex when recorded), outer PowerShell exit, timeout, wall seconds, probe marker.

    focus-01 SceneKeyLookup official 14896 UNKNOWN UNKNOWN False 9.096 CTRL-024 PASS
    focus-02 SceneKeyLookup instrumented 5416 0 0x00000000 UNKNOWN False 8.367 CTRL-024 PASS
    focus-03 SceneKeyLookup instrumented 10080 0 0x00000000 UNKNOWN False 9.162 CTRL-024 PASS
    focus-05 SceneKeyLookup official 9456 UNKNOWN 0 False 8.352 CTRL-024 PASS
    focus-06 SceneKeyLookup instrumented 11452 0 0x00000000 0 False 8.781 CTRL-024 PASS
    focus-07 SceneKeyLookup instrumented 3204 0 0x00000000 0 False 8.979 CTRL-024 PASS
    focus-08 SceneKeyLookup instrumented 10588 0 0x00000000 0 False 8.632 CTRL-024 PASS
    focus-09 SceneKeyLookup instrumented 16428 0 0x00000000 0 False 8.524 CTRL-024 PASS
    focus-10 SceneKeyLookup instrumented 7312 0 0x00000000 0 False 8.975 CTRL-024 PASS
    focus-11 SceneKeyLookup direct 18260 0 0x00000000 n/a False 1.958 CTRL-024 PASS
    focus-12 SceneKeyLookup direct 3264 0 0x00000000 n/a False 1.978 CTRL-024 PASS
    focus-13 SceneKeyLookup direct 15376 0 0x00000000 n/a False 2.072 CTRL-024 PASS
    focus-14 SceneKeyLookup direct 14364 0 0x00000000 n/a False 2.225 CTRL-024 PASS
    focus-15 SceneKeyLookup direct 12408 0 0x00000000 n/a False 2.236 CTRL-024 PASS
    focus-16 SceneKeyLookup direct 13864 0 0x00000000 n/a False 2 CTRL-024 PASS
    focus-17 SceneKeyLookup direct 4312 0 0x00000000 n/a False 2.06 CTRL-024 PASS
    focus-18 SceneKeyLookup direct 9888 0 0x00000000 n/a False 1.944 CTRL-024 PASS
    focus-19 SceneKeyLookup direct 1400 0 0x00000000 n/a False 2.048 CTRL-024 PASS
    focus-20 SceneKeyLookup direct 6900 0 0x00000000 n/a False 2.013 CTRL-024 PASS
    focus-21 SceneKeyLookup direct 5556 0 0x00000000 n/a False 1.943 CTRL-024 PASS
    focus-22 SceneKeyLookup direct 15624 0 0x00000000 n/a False 1.946 CTRL-024 PASS
    focus-23 SceneKeyLookup direct 14572 0 0x00000000 n/a False 1.947 CTRL-024 PASS
    focus-24 SceneKeyLookup direct 2092 0 0x00000000 n/a False 1.97 CTRL-024 PASS
    focus-25 SceneKeyLookup direct 12904 0 0x00000000 n/a False 1.941 CTRL-024 PASS
    focus-26 SceneKeyLookup direct 16016 0 0x00000000 n/a False 2.162 CTRL-024 PASS
    focus-27 SceneKeyLookup direct 17008 0 0x00000000 n/a False 2.05 CTRL-024 PASS
    focus-28 SceneKeyLookup direct 16256 0 0x00000000 n/a False 2.025 CTRL-024 PASS
    focus-29 SceneKeyLookup direct 828 0 0x00000000 n/a False 2.068 CTRL-024 PASS
    focus-30 SceneKeyLookup direct 17012 0 0x00000000 n/a False 2.05 CTRL-024 PASS
    focus-31 SceneKeyLookup instrumented 8748 0 0x00000000 0 False 8.534 CTRL-024 PASS
    gate-01 TextAreaToolbarOwnership gate-instrumented 17596 0 0x00000000 0 False 8.966 CTRL-066 PASS
    gate-02 TableEditRowIdentity gate-instrumented 2052 0 0x00000000 0 False 9.608 CTRL-075 PASS
    gate-03 TableAdapterBulkClose gate-instrumented 13140 0 0x00000000 0 False 9.932 CTRL-076 PASS
    gate-04 TableAdapterQueuedClose gate-instrumented 8164 0 0x00000000 0 False 9.638 CTRL-077 PASS
    gate-05 TextFieldLovDelayedCompletion gate-instrumented 7204 0 0x00000000 0 False 9.513 CTRL-078 PASS
    gate-06 TableAdapterPagingClose gate-instrumented 7192 0 0x00000000 0 False 9.004 CTRL-065 PASS
    gate-07 TextAreaLovDelayedCompletion gate-instrumented 11308 0 0x00000000 0 False 9.221 CTRL-062 PASS
    gate-08 ExternalLovReadOnly gate-instrumented 2468 0 0x00000000 0 False 9.776 CTRL-061 PASS
    gate-09 WrapButtonOwnership gate-instrumented 3168 0 0x00000000 0 False 8.587 CTRL-055 PASS
    gate-10 TabPaneSkinListeners gate-instrumented 18252 0 0x00000000 0 False 8.637 CTRL-054 PASS
    gate-11 TextAreaSkinDisposal gate-instrumented 7544 0 0x00000000 0 False 8.955 CTRL-053 PASS
    gate-12 ValidatorSubchanges gate-instrumented 5860 0 0x00000000 0 False 8.367 CTRL-052 PASS
    gate-13 TableAdapterClose gate-instrumented 6756 0 0x00000000 0 False 8.696 CTRL-051 PASS
    gate-14 TextAreaLovLifecycle gate-instrumented 18428 0 0x00000000 0 False 8.967 CTRL-050 PASS
    gate-15 TextFieldSkinDisposal gate-instrumented 16120 0 0x00000000 0 False 9.002 CTRL-049 PASS
    gate-16 LovBindingInterleaving gate-instrumented 16452 0 0x00000000 0 False 8.993 CTRL-045 PASS
    gate-17 BoundLovSetter gate-instrumented 18416 0 0x00000000 0 False 8.65 CTRL-044 PASS
    gate-18 WrapButtonDeferred gate-instrumented 17156 0 0x00000000 0 False 8.82 CTRL-043 PASS
    gate-19 Editors gate-instrumented 17180 0 0x00000000 0 False 9.099 GUI-004 PASS
    gate-20 TableEditing gate-instrumented 7424 0 0x00000000 0 False 9.015 GUI-005 PASS
    gate-21 TableCancel gate-instrumented 1840 0 0x00000000 0 False 9.373 GUI-006 PASS
    gate-22 EditorReentry gate-instrumented 11468 0 0x00000000 0 False 9.722 GUI-007 PASS
    gate-23 DataBinding gate-instrumented 3496 0 0x00000000 0 False 9.518 GUI-009 PASS
    gate-24 FormLifecycle gate-instrumented 5064 0 0x00000000 0 False 9.665 GUI-010 PASS (standalone production FXML/controller and SDI close)
    gate-25 FormCancel gate-instrumented 9580 0 0x00000000 0 False 10.09 CTRL-022 PASS
    gate-26 SceneKeyLookup gate-instrumented 11704 0 0x00000000 0 False 8.614 CTRL-024 PASS
    gate-27 LovListenerLifecycle gate-instrumented 2572 0 0x00000000 0 False 9.378 CTRL-026 PASS
    gate-28 LovDefaultValidation gate-instrumented 4560 0 0x00000000 0 False 9.115 CTRL-027 PASS
    gate-29 LovIntegration gate-instrumented 17768 0 0x00000000 0 False 9.262 CTRL-031 PASS
    gate-30 SingletonCloseVeto gate-instrumented 4812 0 0x00000000 0 False 9.677 CTRL-032 PASS
    gate-31 SingletonLifecycleMatrix gate-instrumented 2880 0 0x00000000 0 False 9.639 CTRL-035 PASS
    gate-32 ValidatorScopedUnbind gate-instrumented 3484 0 0x00000000 0 False 8.534 CTRL-033 PASS
    gate-33 ValidatorMultiControl gate-instrumented 10764 0 0x00000000 0 False 8.831 CTRL-036 PASS
    gate-34 TextFields gate-instrumented 8588 0 0x00000000 0 False 11.657 GUI-012 PASS (tested matrix)
    gate-35 ValueFields gate-instrumented 8808 0 0x00000000 0 False 9.183 VF-001 PASS
    gate-36 ValueLifecycle gate-instrumented 7464 0 0x00000000 0 False 9.165 VF-002 PASS (custom listener ownership)
    gate-37 MoneyDecimal gate-instrumented 12716 0 0x00000000 0 False 10.234 NUM-001 PASS (tested invariants)
    gate-38 DatePickerBinding gate-instrumented 10476 0 0x00000000 0 False 9.238 CTRL-002 PASS
    gate-39 NodeTraversal gate-instrumented 2392 0 0x00000000 0 False 8.965 CTRL-003 PASS
    gate-40 RadioReadOnly gate-instrumented 10592 0 0x00000000 0 False 9.004 CTRL-006 PASS
    gate-41 ComboReadOnly gate-instrumented 14556 0 0x00000000 0 False 15.119 CTRL-007 PASS
    gate-42 CheckBoxReadOnly gate-instrumented 7488 0 0x00000000 0 False 15.373 CTRL-009 PASS
    gate-43 ValueChangedFlag gate-instrumented 4808 0 0x00000000 0 False 7.429 CTRL-013 PASS
    gate-44 ControlsFollowup gate-instrumented 2088 0 0x00000000 0 False 8.711 CTRL-CONTROLS-FOLLOWUP PASS
    gate-45 ChoiceAdapter gate-instrumented 1700 0 0x00000000 0 False 8.935 CTRL-015 PASS
    gate-46 BinderChoiceNull gate-instrumented 5156 0 0x00000000 0 False 9.248 CTRL-017 PASS
    gate-47 ChoiceNullLookup gate-instrumented 8268 0 0x00000000 0 False 9.054 CTRL-018 PASS
    gate-48 ChoiceNonNullSet gate-instrumented 8000 0 0x00000000 0 False 8.789 CTRL-019 PASS
    gate-49 Default gate-instrumented 18212 0 0x00000000 0 False 9.549 GUI-001 PASS

CTRL-080…090 remain NOT_RUN / PAUSED. Explicit architect release is required. Stopped after CTRL-079A.

---

## Reply CTRL-080

**DONE.** Baseline 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab. Verified production 85a4b1f8aeef5558e32430da1f6e2c7061f42cb8.

On a shown JInvTable with real ArrayDataSet and DSFXAdapter, synthetic mouse-armed same-row cancellation called a controlled rejecting converter. RED observed one original exception, zero commits and model writes, cursor 0, and a cell that remained editing with its editor attached. GREEN surfaces the same exception once, completes cancellation, detaches the editor, leaves the cursor at 0, and performs zero commits or model writes. Reentry plus F8 commits once to A. Positive mouse-armed cancellation, F8 and ENTER each commit once to A; Escape, replacement with an equal-valued distinct entity, removal and permutation produce zero commits/writes. Filter removal is in the cleanup path; registration count and native click/focus behavior were not instrumented.

Only JInvTextFieldTableCell and the existing TableEditRowIdentitySmoke changed. The existing cancellation teardown runs in finally, preserving both original-row guards and propagation of converter failures. No added instance state or allocations on the production path. Dedicated rejection assertions extend the existing runner mode.

Windows rerun (Java 25.0.1, JavaFX 25, IDEA Maven 3.9.16 offline): powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableEditRowIdentity -SettingsFile "D:\Java\.m2\settings.xml". Diagnostic runner copy records native JVM and outer exits separately in target/ctrl-080/matrix.csv. RED 1/1 on unchanged production, GREEN 0/0 in three fresh JVMs. TableEditing, TableCancel, EditorReentry, DataBinding, MoneyDecimal and LovDefaultValidation all 0/0. Maven -Pbuild compile, PowerShell parse and staged diff checks passed.


---

## Reply CTRL-081

**DONE.** Baseline 85a4b1f8aeef5558e32430da1f6e2c7061f42cb8. Verified production 3976625111c448f00f07837ca987da7da9a0a1d7.

Real shown JInvTable, DSFXAdapter.findRow and ProgressTaskExecutor with in-memory ArrayDataSet. Existing-row hit, miss, controlled swap exception after registration, and repeat hit delivered [true, false, true], two total swap attempts, one error dialog showing the original exception, and no success callback for the failed attempt. RED: four temporary adds, three matching removals, and two obsolete predicate calls from one later INSERT notification. GREEN: four adds, four removals with exact listener identities and zero obsolete predicate calls. Independent observer was notified once in both. Progress cancellation and actual SQL were not exercised.

Production only DSFXAdapter. The originating DataSet owns registration and removal; removal runs in finally, preserving the primary failure even if removal also fails. Progress work, existing search callback and SQL paths unchanged. No added instance state or successful-path object allocation.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterSearchListener -SettingsFile "D:\Java\.m2\settings.xml". Java 25.0.1 / FX25, IDEA Maven 3.9.16 offline. Recorded instrumented JVM/runner exits: RED 1/1, GREEN 0/0 in three fresh JVMs; DataBinding, TableAdapterBulkClose, TableAdapterQueuedClose, TableAdapterClose all 0/0. Maven -Pbuild compile, PowerShell parse and diff check passed. Initial RED assertions also exposed the leak, then standalone fixture timed out because foreground fore-Th-Pool workers remained parked after Platform.exit (thread dump saved). Fixture now shuts down its own pool through ThreadPoolManager.shutdown without forcing JVM success. All first-attempt evidence and completed exits are retained under target/ctrl-081.


---

## Reply CTRL-082

**DONE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked production or probe changes.

Real shown JInvTable, DSFXAdapter.moveToLast, ProgressTaskExecutor modal nested loop, real ArrayDataSet with controlled in-memory swap and CountDownLatch release. The worker posted an FX action and waited, the FX action closed the adapter twice, then released the worker. This orders close before the next worker dereference without relying on unsynchronized visibility. No SQL invoked.

| Route | Swap calls | Worker count reads | Adapter navigation adds | Selected / cursor / explicit write requests | External navigation | Error dialog |
| --- | ---: | ---: | ---: | --- | --- | --- |
| Active | 2 | 3 | 2 | C / 2 / 1 | [2] | 0 |
| Close before first swap, paused in initial count | 0 | 1 | 1 | null / -1 / 0 | [] | 1 NPE, swappingData |
| Close during first swap, then return true | 1 | 1 | 1 | null / -1 / 0 | [] | 1 NPE, getLoadedRowCount |
| Close during second swap, after first progress update | 2 | 3 | 1 | null / -1 / 0 | [] | 1 NPE, getLoadedRowCount |
| Active cancel via progress button during first swap | 1 | 3 | 2 | C / 2 / 1 | [2] | 0 |
| Active controlled swap exception | 1 | 1 | 2 | C / -1 / 0 | [] | 1, original fixture message |

Count reads are instrumented getLoadedRowCount calls, not counts of painted progress updates. Close suppression from CTRL-076 held for navigation re-registration, selection and cursor writes. Close did not prevent an already entered in-memory swap from returning. The later mutable field reads caused three obsolete error dialogs. Active cancellation interrupted the latch and left the swap, with no second swap. It still selected C in this fixture. The separate active failure selection is recorded as observed; deciding its contract is outside this audit. Mutable dataSet reads offer no source-level guarantee without an ordering relation. Close before worker entry, exact cancellation task terminal ordering, server-side SQL, paint and production transaction effects remain UNKNOWN.

Reproduce using ignored target/ctrl-082/BulkBlockingAudit.java. Build classpath from target/classes plus target/ctrl-079a/original-dependencies.txt and run javac -cp <classpath> -d target/ctrl-082 target/ctrl-082/BulkBlockingAudit.java then java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-082;<classpath>" BulkBlockingAudit. Java 25.0.1/FX 25. javac 0, actual direct JVM 0, no PowerShell probe runner or timeout. Full stdout/stderr and exit.txt under target/ctrl-082. A focused follow-up should define worker attachment and cancellation/error delivery separately, capture the originating attachment for work already accepted, and test preventing later obsolete swaps/progress calls. No claim of stopping an in-flight database operation.


---

## Reply CTRL-083

**BLOCKED / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked changes. Confirmed stale currency writes in JInvTextFieldCUR.showLOV, inherited by JInvCURTextField. Direct, F9 and JInvLOVButton routes ran against a shown real currency field with deferred providers A=USD and B=EUR, bidirectional curValue model, and distinct uppercase drafts.

All nine accepted retired completions wrote once and replaced the draft. A→B wrote B's EUR even though A completed. A→null wrote empty currency and null editor text with the genuine fallback provider in a fixture without a DB context. A→B→A wrote USD from the retired request. Direct callbacks reported true from A, and each stale F9/button acceptance generated one TAB and one focus request. All nine corresponding cancellations preserved draft and currency with zero writes/TAB, and UI routes requested focus once. Same-provider repeat produced one write on acceptance and none on cancellation. Both overlapping current requests and a pending request after two bound setter failures completed as accepted with original source; a same-value completion need not generate a second model change.

The base field attachment revision is private and its unbindLOV is final. There is no existing accessor for the generation to distinguish ABA and inherited unbind. Using super.showLOV would additionally change popup positioning, text selection/filter handling or error dispatch. Observing the writable property would define behavior for direct mutations and has different notification order from the base revision. A narrow follow-up contract would expose the existing attachment check to this currency subclass and leave popup and currency conversion semantics intact. Such a base change is outside CTRL-083 scope.

Ignored target/ctrl-083/CurrencyLovAudit.java, javac 0 and full direct JVM 0 with no outer probe runner. Classpath target/ctrl-083;target/classes;<target/ctrl-079a/original-dependencies.txt contents>, Java 25.0.1 and FX25. The first fixture run stopped at detach (exit 1) with TaskContext creation requiring a database session; its logs remain saved. The complete run uses a fixture BaseApp returning a null TaskContext and never opens the SQL-backed LOV. No production SQL, actual focus traversal or fallback choice-list behavior tested. No state/allocation change.


---

## Reply CTRL-084

**DONE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked edits.

Ignored target/ctrl-084/LaunchedBinderAudit.java and launched-binder.fxml launch a real shown SDI FXML controller through FXFormLauncher. The application fixture uses SDI, disables preference writes and supplies no database TaskContext. Its beforeInit sets the externally retained, real FXEntity through the protected controller seam; actual super.bindControls → Binder.bindTextField installs the converter binding on a stock TextField. The fixture asserts active two-way propagation, then veto and accepted close. Veto left one owner shown, zero callbacks and zero cleanup. Accepted close yielded owner-hidden → onCloseWindow → callback RET_CANCEL → worker cleanup-enter → deterministic FX release → cleanup-return, once each. Reopen succeeded with a fresh singleton owner and the same model.

After close, editing old control and then model yielded two events on each. After reopening, old edit, new edit and model edit yielded three matching events on all three observers. Explicit Bindings.unbindBidirectional(Object,Object) for the old control only left its own standalone event, plus one model update received by the fresh control. These counts show binding lifetime with intentional strong retention. They do not measure retention or prove a leak in ordinary forms. Source audit found the protected FXEntity construction seam and default per-controller wrapper, but no in-repo callers pooling and reusing the same wrapper across live form instances. External application reuse and automatic unlink ownership policy UNKNOWN. Real TaskContext close, MDI/WAIT and corporate forms not exercised. A future narrowly scoped fix needs a verified supported ownership contract for converter binding teardown.

Reproduce with javac -cp "target/classes;<target/ctrl-079a/original-dependencies.txt contents>" -d target/ctrl-084 target/ctrl-084/LaunchedBinderAudit.java, then java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-084;target/classes;<deps>" LaunchedBinderAudit. Java 25.0.1/FX25. Completed javac and direct JVM exit 0, no outer runner. Initial fixture failed its first assertion with exit 1 because its beforeInit override had not assigned the fixture FXEntity; that output is still saved under target/ctrl-084. No additional state or allocations in production.


---

## Reply CTRL-085

**DONE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked changes.

Reused CTRL-084 real shown SDI FXFormLauncher and FXML, held external real FXEntity and a stock converter-bound TextField. Positive gate reran two successful launches, veto, accepted close/reopen and explicit unlink. Failure controller threw a sentinel immediately after super.bindControls successfully set the editor from the external entity. Observed bound:initial → inject → cancel → WINDOW_HIDDEN → onCloseWindow → one RET_CANCEL callback with original throwable identity → cleanup-enter → deterministic FX release → cleanup-return. The fresh singleton launched successfully afterwards; no late callback from its predecessor. The failure was injected before the later configControls phase.

The retained old control and model still propagated two ordered writes after cleanup. After reopening with the same model, old-control edit, fresh-control edit and model edit each reached all three observers in order, exactly three events each. This confirms retained binding after a real post-bind initialization failure for the intentionally shared FXEntity. Controller extension seams allow the fixture setup; actual corporate use of external shared wrappers and ownership of automatic unlink remain UNKNOWN. The fixture had no database TaskContext. No veto injected on the failing form, because that would change the failure recovery contract under study. MDI/WAIT, SQL and rollback untested. Small follow-up if caller ownership is established: exact teardown of only the launcher/controller-owned converter bindings after close and post-bind failure, with a separate policy for externally owned bindings.

Ignored target/ctrl-085/FailingLaunchedBinderAudit.java and launched-binder.fxml. javac -cp "target/classes;<target/ctrl-079a/original-dependencies.txt contents>" -d target/ctrl-085 target/ctrl-085/FailingLaunchedBinderAudit.java; java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-085;target/classes;<deps>" FailingLaunchedBinderAudit. Java 25.0.1/FX25, javac 0, actual JVM 0, no outer probe runner or timeout. Raw streams and exit.txt preserved. No production state/allocation delta.


---

## Reply CTRL-086

**NO_CHANGE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked edits.

Exercised actual super.bindControls in a shown controller graph. A legal initLabels override moved a stock TextField out and inserted a label plus another stock TextField. The later filter observed the fresh DFS graph in exact identity order (3 calls), ignored the removed field and bound the newly inserted one to the entity value. A pre-label snapshot omitted both inserted nodes. No-entity control observed zero getBindingFilter calls. Reusing an earlier list for binding loses this behavior. Moving the default label scan ahead of the override similarly misses graph mutations before super.initLabels. Controls retains the existing traversal exclusions, including composite control boundaries and ButtonBar handling.

Shown nested VBox/HBox graph, 24/120/480 groups each with JInvLabel, JInvTextField and stock TextField. Five warmup and nine measured batches of 100 calls, FX-thread ThreadMXBean allocated bytes, Oracle Java 25.0.1 / JavaFX 25+29, 1 GiB max heap. On immutable fixtures the isolated candidate produced identical control and label lists in DFS order. Median (range) per call, time in microseconds and allocation in bytes.

| Groups | Two traversals µs | Shared traversal buffer µs | Two traversals B | Shared traversal buffer B |
| ---: | --- | --- | --- | --- |
| 24 | 9.910 (9.779–10.296) | 7.020 (6.908–7.929) | 856 (856–856) | 1520 (1520–1522.7) |
| 120 | 87.039 (86.589–89.131) | 46.888 (46.250–52.632) | 4168 (4168–4168) | 7232 (7232–7232) |
| 480 | 386.160 (380.508–1089.788) | 204.061 (200.104–211.742) | 13544 (13544–13544) | 28568 (28568–28568) |

These are traversal-only measurements and include candidate result buffers. Full form initialization and retained heap were not measured. Faster traversal on an immutable graph does not establish equivalence at the controller hooks. This version also increases allocated bytes. No production optimization or persistent state.

Ignored source and raw data target/ctrl-086/TraversalAudit.java and audit.stdout. Compile with javac -cp "target/ctrl-084;target/classes;<deps>" -d target/ctrl-086 target/ctrl-086/TraversalAudit.java, then java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-086;target/ctrl-084;target/classes;<deps>" TraversalAudit. Deps are the contents of target/ctrl-079a/original-dependencies.txt. Final javac and native JVM exits 0, no probe runner. First fixture compile exposed an invalid helper call, corrected to the existing protected formMode seam.


---

## Reply CTRL-087

**NO_CHANGE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked edits.

Actual AbstractBaseController.initLabels and an isolated per-call index ran in shown nested graphs. Both selected identical Label objects in the same order on the static fixture with duplicate field names and keys, pre-associated label and field, empty/null keys and missing matches. The index rechecks current label eligibility inside a bucket. A supported setLabel override changed an earlier unused label key during the first association. The actual method used that newly eligible label on the next field. The candidate selected a later label instead. A fresh key index cannot be assumed after arbitrary user setters/property listeners, so the measured improvement does not authorize changing production.

Five warmup and nine measurement batches of 12 invocations. Fixture state reset, construction and CSS/layout excluded. Includes the label traversal, matching, bindings and candidate index construction. FX-thread ThreadMXBean allocations, Oracle Java 25.0.1 / JavaFX 25+29, 1 GiB heap ceiling. Median (range) elapsed µs and bytes per invocation. N adds this many labels and fields plus one prebound pair.

| N | Existing µs | Index µs | Existing B | Index B |
| ---: | --- | --- | --- | --- |
| 20 | 63.558 (28.025–194.842) | 26.167 (22.758–45.250) | 7752 (7752–7752) | 5208 (5208–5221.3) |
| 100 | 209.733 (164.450–461.292) | 98.583 (63.683–160.983) | 27288 (25992–37054) | 13576 (13576–13593.3) |
| 400 | 1059.158 (1027.033–1177.300) | 200.367 (198.442–209.008) | 104312 (104312–104312) | 47208 (47208–47208) |

Small graph timing varied; dynamic mutation makes the tested candidate ineligible at every size. No retained-memory estimate or actual production saving. Ignored target/ctrl-087/LabelMatchingAudit.java and audit streams. javac -cp "target/ctrl-084;target/classes;<deps>" -d target/ctrl-087 target/ctrl-087/LabelMatchingAudit.java, then java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-087;target/ctrl-084;target/classes;<deps>" LabelMatchingAudit, using target/ctrl-079a/original-dependencies.txt. Compile 0 and direct JVM 0, no outer probe runner.


---

## Reply CTRL-088

**DONE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked edits.

Shown fixed-cell-size virtualized JInvTable with real DSFXAdapter, production text editor and mark renderer. XXIDataSet subclass uses preloaded in-memory rows and emits genuine mark notifications, with mark/unmark writes handled in memory. The fixture fired the visible first-row CheckBox twice: one mark and one unmark reached only the original row, and the marker pseudoclass was observed on mark. Repeated first-row reentry armed the real mouse filter, used Escape and canceled. Each of 108 measured episodes invoked one active mouse handler, zero after cancellation, allocated zero new editors after warmup and wrote zero drafts. Across workloads, selection still identified original row 0.

Five warmup, nine measured batches of 12 operations. Oracle 25.0.1 / FX 25+29, 1 GiB max heap, elapsed wall µs and FX-thread allocated bytes per operation. N=200/2000, height=240/480 gives roughly 9/19 live rows. Scroll + refresh + synchronous layout cost much more than same-row reentry. Across the 108 measured operations, the text cell/value factory ran 981–990 times at height 240 and 2052 at 480. Mark-enabled ran an equal number of marker cell/value factory calls, with a fresh SimpleBooleanProperty per mark value factory hit. Counts tracked viewport height more closely than loaded N. The mark-enabled difference includes cell, checkbox, CSS and layout, so it cannot be attributed to the boolean property alone. Raw rows below are ordered by N, viewport, marker and workload; counts are text cells, text value reads, mark cells, mark value reads, editors and active mouse calls.

    N=200 height=240 liveRows=9 mark=false workload=reedit ops=108 us median=1628,375 range=1233,400..1816,750 B median=48684,7 range=48680,0..48754,0 counts=[0, 108, 0, 0, 0, 108]
    N=200 height=240 liveRows=10 mark=false workload=scroll-refresh ops=108 us median=3507,200 range=3211,283..3999,442 B median=611451,3 range=610569,3..614307,3 counts=[981, 981, 0, 0, 0, 0]
    N=200 height=240 liveRows=9 mark=true workload=reedit ops=108 us median=940,250 range=701,450..1290,533 B median=61680,0 range=61680,0..61680,0 counts=[0, 108, 0, 0, 0, 108]
    N=200 height=240 liveRows=10 mark=true workload=scroll-refresh ops=108 us median=4840,800 range=4112,725..5558,908 B median=1505722,0 range=1501382,7..1508860,0 counts=[981, 981, 981, 981, 0, 0]
    N=200 height=480 liveRows=19 mark=false workload=reedit ops=108 us median=462,183 range=357,458..556,350 B median=44904,0 range=44904,0..44904,0 counts=[0, 108, 0, 0, 0, 108]
    N=200 height=480 liveRows=19 mark=false workload=scroll-refresh ops=108 us median=2923,183 range=2225,225..3559,917 B median=1261513,3 range=1258777,3..1268857,3 counts=[2052, 2052, 0, 0, 0, 0]
    N=200 height=480 liveRows=19 mark=true workload=reedit ops=108 us median=395,775 range=362,933..641,925 B median=55520,0 range=55520,0..55520,0 counts=[0, 108, 0, 0, 0, 108]
    N=200 height=480 liveRows=19 mark=true workload=scroll-refresh ops=108 us median=6013,625 range=5411,033..6653,475 B median=3116513,3 range=3111386,0..3121749,3 counts=[2052, 2052, 2052, 2052, 0, 0]
    N=2000 height=240 liveRows=9 mark=false workload=reedit ops=108 us median=327,608 range=267,908..569,933 B median=43824,0 range=43824,0..44100,0 counts=[0, 108, 0, 0, 0, 108]
    N=2000 height=240 liveRows=9 mark=false workload=scroll-refresh ops=108 us median=1043,483 range=935,492..1430,475 B median=626366,7 range=625197,3..629178,0 counts=[990, 990, 0, 0, 0, 0]
    N=2000 height=240 liveRows=9 mark=true workload=reedit ops=108 us median=364,092 range=259,075..522,858 B median=54408,0 range=54406,0..54684,0 counts=[0, 108, 0, 0, 0, 108]
    N=2000 height=240 liveRows=9 mark=true workload=scroll-refresh ops=108 us median=2608,025 range=2316,967..3010,158 B median=1522560,0 range=1519864,0..1525204,0 counts=[990, 990, 990, 990, 0, 0]
    N=2000 height=480 liveRows=19 mark=false workload=reedit ops=108 us median=270,158 range=224,283..422,742 B median=43660,0 range=43648,0..43924,0 counts=[0, 108, 0, 0, 0, 108]
    N=2000 height=480 liveRows=19 mark=false workload=scroll-refresh ops=108 us median=2052,108 range=1592,883..3239,108 B median=1295560,0 range=1295092,7..1300304,0 counts=[2052, 2052, 0, 0, 0, 0]
    N=2000 height=480 liveRows=19 mark=true workload=reedit ops=108 us median=272,592 range=243,667..432,025 B median=53784,0 range=53760,0..54036,0 counts=[0, 108, 0, 0, 0, 108]
    N=2000 height=480 liveRows=19 mark=true workload=scroll-refresh ops=108 us median=4246,833 range=4036,492..5163,333 B median=3148246,7 range=3148040,0..3158005,3 counts=[2052, 2052, 2052, 2052, 0, 0]

Production StubObservableValue and StubCallbackValue are per-column proxies with a mutable row reference, each serving synchronous reads. The mark value factory uses separate snapshot objects and the renderer maintains row-level visual state. Sharing the marker value via a mutable proxy needs independent observer/notification equivalence proof. First follow-up candidate is isolated marker-only value factory cost against refresh/layout, then measure whether refresh-induced construction can be avoided with identical notification and editor behavior. No per-row cache or retained heap inference.

Ignored target/ctrl-088/CellReuseAudit.java and audit streams. javac -cp "target/classes;<deps>" -d target/ctrl-088 target/ctrl-088/CellReuseAudit.java, then java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-088;target/classes;<deps>" CellReuseAudit. Deps from target/ctrl-079a/original-dependencies.txt. javac 0 and direct JVM 0, no probe runner. This fixture does not measure paint, SQL, or full-row mark-all behavior. No production state or allocation delta.


---

## Reply CTRL-089

**DONE / READ-ONLY.** Baseline and verified final production 3976625111c448f00f07837ca987da7da9a0a1d7. No tracked edits.

Shown JInvTable, real DSFXAdapter and ArrayDataSet AFTER EXECUTE events. Each burst posts all events in a single FX turn. A queue barrier runs after adapter deliveries and requests the next pulse; a public Scene post-layout pulse listener records elapsed time, FX-thread allocation, factories and layout dirty state. No arbitrary waits or paint claim. N=200/2000, viewport height=240/480 (9/19 visible live rows), burst 1/3/10/100, five warmup and nine measured single-burst samples per cell. Java 25.0.1 / FX25, 1 GiB heap ceiling. Ranges cover nine measurements; each numeric field reports median and range. Counts and allocation are per burst, with fixed fixture/pulse overhead included.

All ordinary measured bursts preserved row 0 selection and cursor with zero model writes. Refresh deliveries were exactly the burst size, but the same-turn calls shared one observed post-layout pulse and one 9- or 19-cell reconstruction for this fixture. Median queue drain was 0.094–0.156 ms for one notification and 3.072–4.101 ms for 100. Median elapsed to post-layout was 14.819–16.187 ms across the matrix. Allocation was approximately 0.604–0.630 MB at 9 rows and 1.269–1.295 MB at 19; largely viewport-driven in this range, not retained heap or SQL cost. No measured post-layout sample had a dirty table.

With active editor draft, interleaved cursor 1, AFTER EXECUTE, cursor 2 and two more AFTER EXECUTE events produced select 1 → refresh → select 2 → refresh → refresh. All three refreshes observed an active edit; after layout two starts, one cancel, zero commits/writes, current selection/cursor 2 and an editing position still present. An override can observe refresh order even when layout coalesces. Validator callbacks and rendering completion were not established. Preserve active delivery semantics pending a separate equivalence proof; first measure avoiding cell reconstruction while keeping observable event order.

    N=200 height=240 visible=9 burst=1 delivered=1 queue_us=156,400 (120,000..205,500) postlayout_us=15244,700 (11889,200..17525,100) fx_bytes=610680,000 (608952,000..613416,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=240 visible=9 burst=3 delivered=3 queue_us=243,200 (211,200..361,000) postlayout_us=14843,800 (13203,500..20678,300) fx_bytes=609680,000 (609368,000..615080,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=240 visible=9 burst=10 delivered=10 queue_us=609,900 (490,000..682,300) postlayout_us=15617,200 (13903,100..17326,100) fx_bytes=611232,000 (610824,000..624152,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=240 visible=9 burst=100 delivered=100 queue_us=3907,000 (3736,400..4197,000) postlayout_us=15735,100 (13714,900..16922,800) fx_bytes=629616,000 (629544,000..649536,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=480 visible=19 burst=1 delivered=1 queue_us=114,100 (100,500..314,400) postlayout_us=15017,300 (12938,300..17720,900) fx_bytes=1285128,000 (1283856,000..1292424,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=480 visible=19 burst=3 delivered=3 queue_us=196,200 (174,800..205,900) postlayout_us=15281,300 (13881,400..17446,800) fx_bytes=1284448,000 (1280624,000..1300616,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=480 visible=19 burst=10 delivered=10 queue_us=460,400 (446,400..522,200) postlayout_us=15655,900 (14969,200..17049,000) fx_bytes=1276608,000 (1275720,000..1306512,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=200 height=480 visible=19 burst=100 delivered=100 queue_us=4101,000 (3515,800..4267,100) postlayout_us=15137,700 (13685,500..18667,900) fx_bytes=1295328,000 (1294112,000..1332560,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=240 visible=9 burst=1 delivered=1 queue_us=93,600 (72,500..120,700) postlayout_us=15751,400 (14681,600..16846,900) fx_bytes=605600,000 (603480,000..607296,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=240 visible=9 burst=3 delivered=3 queue_us=179,900 (121,300..252,100) postlayout_us=15284,600 (12577,700..16986,300) fx_bytes=604208,000 (603680,000..609656,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=240 visible=9 burst=10 delivered=10 queue_us=542,600 (457,900..1276,900) postlayout_us=15614,000 (14302,500..17313,200) fx_bytes=605136,000 (605136,000..618464,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=240 visible=9 burst=100 delivered=100 queue_us=4011,100 (2809,500..5078,800) postlayout_us=15748,000 (14737,600..16986,200) fx_bytes=623856,000 (623856,000..643848,000) cells=9,000 (9,000..9,000) values=9,000 (9,000..9,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=480 visible=19 burst=1 delivered=1 queue_us=95,600 (78,900..118,700) postlayout_us=14819,400 (6949,400..17625,300) fx_bytes=1271152,000 (1270936,000..1276928,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=480 visible=19 burst=3 delivered=3 queue_us=179,000 (157,400..224,400) postlayout_us=16187,300 (13534,900..17230,800) fx_bytes=1271664,000 (1271352,000..1291344,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=480 visible=19 burst=10 delivered=10 queue_us=327,800 (262,700..534,500) postlayout_us=15714,400 (14521,000..16873,700) fx_bytes=1269160,000 (1269160,000..1299064,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    N=2000 height=480 visible=19 burst=100 delivered=100 queue_us=3071,600 (2893,400..3852,300) postlayout_us=15942,300 (13924,200..17010,300) fx_bytes=1287880,000 (1286968,000..1288552,000) cells=19,000 (19,000..19,000) values=19,000 (19,000..19,000) pulses=1,000 (1,000..1,000) layout_dirty=0
    edit interleave=[cursor:1, notify:true, cursor:2, notify:true, notify:true, selected:1, refresh:editing=true, selected:2, refresh:editing=true, refresh:editing=true] edits=2 cancels=1 commits=0 writes=0 selected=2 cursor=2 active=true sample=Sample[queueNanos=9891900, wallNanos=21292400, bytes=1915976, refresh=3, cells=9, values=10, pulses=1, needsLayout=false]

Ignored target/ctrl-089/RefreshPulseAudit.java, audit.stdout/stderr and exit.txt. javac -cp "target/classes;<deps>" -d target/ctrl-089 target/ctrl-089/RefreshPulseAudit.java, then java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-089;target/classes;<deps>" RefreshPulseAudit, deps from target/ctrl-079a/original-dependencies.txt. Compile 0 and direct JVM 0, no outer probe runner or timeout. No production state/allocation changes.


---

## Reply CTRL-090

**DONE / READ-ONLY.** All 50 supported modes (49 named plus Default) reached their PASS marker and recorded immediate JVM and outer PowerShell exits 0/0. No timeout. Maven -Pbuild compile 0, PowerShell parse clean, git diff --check 0.

Batch baseline 70b139ed54d00c46b4c2ae597535d82ddbe5a2ab. Final verified production 3976625111c448f00f07837ca987da7da9a0a1d7. The only production edits in the fast-forward chain were JInvTextFieldTableCell in CTRL-080 and DSFXAdapter in CTRL-081, with their focused probe and runner changes. Clean tracked checkout and remote refs verified before publishing. The Java 25.0.1 / FX 25 Windows matrix used the scope-preserving diagnostic runner copy, with separate stdout/stderr, timestamp, PID, timeout and signed/hex child code in target/ctrl-090/matrix.csv. Original tracked probe assertions and Java invocation were used. No opt-in Robot or unresolved CTRL-014/034/CAL-TIME-001 tasks. Default omits the mode switch. CTRL-079 and CTRL-079A were not repeated.

| Task | Disposition | Verified result |
| --- | --- | --- |
| CTRL-079 | Accepted gate via CTRL-079A | Architect accepted 49/49 actual child/runner pairs. The initial SceneKeyLookup exit 1 remains UNKNOWN / NOT_REPRODUCED. |
| CTRL-080 | DONE | 85a4b1f8aeef5558e32430da1f6e2c7061f42cb8. Rejected conversion leaves zero commit/write, clears edit state, preserves original exception, valid reentry commits once. RED 1/1; GREEN 0/0 ×3. |
| CTRL-081 | DONE | 3976625111c448f00f07837ca987da7da9a0a1d7. Four owned registrations and four identical removals on GREEN; zero stale predicate calls. Original controlled error and independent listener preserved. RED 1/1 after explicit fixture pool teardown; GREEN 0/0 ×3. Initial fixture timeout and thread dump retained. |
| CTRL-082 | DONE / READ-ONLY | Three obsolete bulk worker NPE dialogs after close; no retired selection, cursor writes or navigation reattachment. Controlled cancellation/error scenarios recorded. |
| CTRL-083 | BLOCKED / READ-ONLY | Nine stale currency writes confirmed in direct/F9/button routes. Existing attachment epoch is private; safe reuse requires a separately scoped base-class contract. |
| CTRL-084 | DONE / READ-ONLY | Bidirectional converter binding survives normal SDI close with intentionally held external entity/control; confirmed explicit selective unlink. |
| CTRL-085 | DONE / READ-ONLY | Binding also survives real post-bind initialization failure; original error callback, cleanup, and singleton reopen observed. |
| CTRL-086 | NO_CHANGE | Shared traversal buffer failed equivalence with controller graph mutation and increased allocated bytes. |
| CTRL-087 | NO_CHANGE | Label index failed exact label identity when a setter changed a later key. |
| CTRL-088 | DONE / READ-ONLY | Refresh/layout created 981–2052 text cells and equal mark cells when enabled per 108 operations; steady first-row reentry created zero further editors. |
| CTRL-089 | DONE / READ-ONLY | 1/3/10/100 refresh deliveries stayed observable; same-turn burst produced one observed layout pulse and 9/19 reconstructed cells. |
| CTRL-090 | Final verification | See matrix and outcome above. |

Production state delta: zero additional persistent fields or eager objects across both fixes. Their normal paths introduce no new application object allocation. No performance change was published. CTRL-086 traversal-only at 480 groups measured 386.160 → 204.061 µs median and 13,544 → 28,568 B. CTRL-087 at 400 labels/fields measured 1059.158 → 200.367 µs and 104,312 → 47,208 B, but selected a different label in the mutation case. CTRL-088 at N=2000, 19 visible rows, scroll-refresh medians were 2.052/4.247 ms and 1.30/3.15 MB with mark disabled/enabled. CTRL-089 queue medians were 0.094–0.156 ms for a single event, 3.072–4.101 ms for 100, and 14.819–16.187 ms to post-layout across the matrix. Per-size ranges, workload counts and reproducible ignored fixtures are in Replies CTRL-086…089. Bytes count FX-thread allocations, not retained heap. Post-layout observation does not measure paint.

Independent Windows rerun from project root. Put IDEA Maven 3.9.16 on PATH; this machine used MAVEN_ARGS=-o and the corporate Maven settings. The official runner is given here; the recorded matrix used its diagnostic copy.

    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableEditRowIdentity -SettingsFile "D:\Java\.m2\settings.xml"
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterSearchListener -SettingsFile "D:\Java\.m2\settings.xml"
    mvn -s "D:\Java\.m2\settings.xml" -Pbuild "-Dmaven.buildNumber.skip=true" compile

Recommended next scoped work, in order:

1. Currency LOV attachment identity, highest confirmed model risk. Authorize a minimal base attachment check reusable by JInvTextFieldCUR and RED/GREEN the nine stale write paths, including ABA.
2. Bulk worker lifetime after close. Separate already-entered fetch from subsequent obsolete access and stale error delivery; verify ordering and unchanged cancellation/selection semantics.
3. Establish supported Binder ownership with a real corporate caller sharing an FXEntity. Test successful close and post-bind failure before deciding any targeted unlink contract.
4. Measure refresh-induced cell reconstruction independently of notifications and mark-property allocation. Preserve cursor/selection order, edits and marker semantics before considering optimization.

Local regression and fixture results are evidence for the tested paths; independent acceptance and corporate SQL/transaction semantics, SDI forms with real TaskContext, MDI/WAIT, native input and actual focus/paint remain unverified. The original CTRL-079 exit cause remains unknown. Stop after CTRL-090.

Final CTRL-090 runner matrix. Columns are mode, probe output, elapsed seconds, JVM exit, runner exit and timeout.

    TableAdapterSearchListener | CTRL-081 PASS | 11.548 | 0 | 0 | False
    TableEditRowIdentity | CTRL-075 PASS | 9.597 | 0 | 0 | False
    TableAdapterBulkClose | CTRL-076 PASS | 9.346 | 0 | 0 | False
    TableAdapterQueuedClose | CTRL-077 PASS | 9.228 | 0 | 0 | False
    TextFieldLovDelayedCompletion | CTRL-078 PASS | 8.972 | 0 | 0 | False
    TableEditing | GUI-005 PASS | 8.983 | 0 | 0 | False
    TableCancel | GUI-006 PASS | 8.927 | 0 | 0 | False
    EditorReentry | GUI-007 PASS | 9.099 | 0 | 0 | False
    DataBinding | GUI-009 PASS | 9.619 | 0 | 0 | False
    TableAdapterClose | CTRL-051 PASS | 8.675 | 0 | 0 | False
    TableAdapterPagingClose | CTRL-065 PASS | 8.944 | 0 | 0 | False
    TextAreaLovDelayedCompletion | CTRL-062 PASS | 8.753 | 0 | 0 | False
    ExternalLovReadOnly | CTRL-061 PASS | 9.004 | 0 | 0 | False
    LovIntegration | CTRL-031 PASS | 8.723 | 0 | 0 | False
    LovDefaultValidation | CTRL-027 PASS | 8.6 | 0 | 0 | False
    TextAreaLovLifecycle | CTRL-050 PASS | 8.977 | 0 | 0 | False
    NodeTraversal | CTRL-003 PASS | 8.65 | 0 | 0 | False
    FormLifecycle | GUI-010 PASS (standalone production FXML/controller and SDI close) | 9.232 | 0 | 0 | False
    FormCancel | CTRL-022 PASS | 9.588 | 0 | 0 | False
    SingletonLifecycleMatrix | CTRL-035 PASS | 9.245 | 0 | 0 | False
    MoneyDecimal | NUM-001 PASS (tested invariants) | 9.848 | 0 | 0 | False
    TextAreaToolbarOwnership | CTRL-066 PASS | 8.764 | 0 | 0 | False
    WrapButtonOwnership | CTRL-055 PASS | 8.362 | 0 | 0 | False
    TabPaneSkinListeners | CTRL-054 PASS | 8.506 | 0 | 0 | False
    TextAreaSkinDisposal | CTRL-053 PASS | 8.785 | 0 | 0 | False
    ValidatorSubchanges | CTRL-052 PASS | 8.26 | 0 | 0 | False
    TextFieldSkinDisposal | CTRL-049 PASS | 8.828 | 0 | 0 | False
    LovBindingInterleaving | CTRL-045 PASS | 8.925 | 0 | 0 | False
    BoundLovSetter | CTRL-044 PASS | 8.558 | 0 | 0 | False
    WrapButtonDeferred | CTRL-043 PASS | 8.628 | 0 | 0 | False
    Editors | GUI-004 PASS | 8.821 | 0 | 0 | False
    SceneKeyLookup | CTRL-024 PASS | 8.155 | 0 | 0 | False
    LovListenerLifecycle | CTRL-026 PASS | 8.664 | 0 | 0 | False
    SingletonCloseVeto | CTRL-032 PASS | 8.829 | 0 | 0 | False
    ValidatorScopedUnbind | CTRL-033 PASS | 8.297 | 0 | 0 | False
    ValidatorMultiControl | CTRL-036 PASS | 8.676 | 0 | 0 | False
    TextFields | GUI-012 PASS (tested matrix) | 9.448 | 0 | 0 | False
    ValueFields | VF-001 PASS | 8.922 | 0 | 0 | False
    ValueLifecycle | VF-002 PASS (custom listener ownership) | 8.638 | 0 | 0 | False
    DatePickerBinding | CTRL-002 PASS | 8.452 | 0 | 0 | False
    RadioReadOnly | CTRL-006 PASS | 8.393 | 0 | 0 | False
    ComboReadOnly | CTRL-007 PASS | 14.437 | 0 | 0 | False
    CheckBoxReadOnly | CTRL-009 PASS | 14.48 | 0 | 0 | False
    ValueChangedFlag | CTRL-013 PASS | 7.136 | 0 | 0 | False
    ControlsFollowup | CTRL-CONTROLS-FOLLOWUP PASS | 8.378 | 0 | 0 | False
    ChoiceAdapter | CTRL-015 PASS | 8.248 | 0 | 0 | False
    BinderChoiceNull | CTRL-017 PASS | 8.541 | 0 | 0 | False
    ChoiceNullLookup | CTRL-018 PASS | 8.022 | 0 | 0 | False
    ChoiceNonNullSet | CTRL-019 PASS | 8.122 | 0 | 0 | False
    Default | GUI-001 PASS | 8.926 | 0 | 0 | False


---

## Reply CTRL-091

**DONE.** Baseline 3976625111c448f00f07837ca987da7da9a0a1d7. Verified production de95eefc66700425f46720f43119c74298adf0d4. Only JInvTextField and JInvTextFieldCUR production, plus the focused probe and mode.

Real shown currency field, model bound bidirectionally to curValue, deferred providers for direct/F9/button. All nine accepted retired completions wrote once on RED: replacement wrote EUR from B, detach cleared the value, ABA wrote USD from A. All nine now preserve the draft and model, report false with original A in direct mode and emit zero TAB in UI modes. Cancellation focus remains one in the UI routes. Same-provider, current accept/cancel, overlap, rejected bound set/unbind, programmatic updates and readOnly controls passed. An injectable default provider confirmed current conversion and default→explicit→default invalidation; the real internal provider identity was separately verified without opening a SQL choice list. Corporate currency lookup and actual TAB traversal remain untested.

The package-private final helper reads the existing epoch, including a distinct value before lazy listener creation. The currency callback captures actual provider and epoch, checks the epoch first to avoid stale fallback creation, and keeps skipFilter, text argument, conversion-before-curValue update, original source and standard cancellation. No new per-instance state and one callback per request as before.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -CurrencyLovDelayedCompletion -SettingsFile "D:\Java\.m2\settings.xml". Java 25.0.1/FX25; IDEA Maven offline. RED JVM/runner 1/1 on unchanged production; GREEN 0/0 on three fresh instrumented JVMs, plus unchanged official runner exit 0. TextFieldLovDelayedCompletion, TextAreaLovDelayedCompletion, ExternalLovReadOnly, LovBindingInterleaving, BoundLovSetter, LovIntegration, MoneyDecimal and ValueFields all 0/0. Maven -Pbuild compile, PowerShell parse and diff checks passed. Initial implementation compile failure was a raw generic type inference error before a JVM launched; preserved with all streams and exits in target/ctrl-091.


---

## Reply CTRL-092

**DONE.** Baseline de95eefc66700425f46720f43119c74298adf0d4. Verified production 8914be08e3399bed7fabbb55e016412a03da9e72. Only DSFXAdapter production plus the focused test and mode.

Shown JInvTable, actual progress service fixed worker and nested FX loop, real in-memory ArrayDataSet. The before-entry fixture occupied all worker slots and released them only after two closes. Further latch cases closed during initial count, first swap and second swap. RED gave 1 obsolete NPE dialog in each of the four cases. GREEN gave none; admitted worker total-count calls were 0/1/1/1 and swaps 0/0/1/2, respectively. All four retained null selection, cursor -1, zero explicit cursor requests, no EOF publication, one initial navigation registration, and one late notification to the independent listener. Active success still made two swaps, selected C/cursor 2 and published EOF. Active Cancel button made one swap, selected C/cursor 2 and left EOF false. A genuine controlled failure retained its original error dialog and original active selection/cursor behavior.

A volatile long attachment epoch is retired by the FX owner before listener teardown. The worker captures origin and epoch at launch; epoch read immediately before dataset work is its admission boundary. Blocking work already admitted can return. After observing retirement, the worker performs no later swaps or progress reports. Adapter EOF/cancellation and selection are published on FX in the existing queued delivery with a fresh ownership check; later cursor publication is independently guarded. No blocking locks, extra operation object, or transaction/SQL cancellation guarantee. One new long of instance state, one primitive capture in the existing callback. Genuine failures from admitted work still use the existing error path; delivery ownership is CTRL-094.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterBulkWorkerClose -SettingsFile "D:\Java\.m2\settings.xml". Java 25.0.1/FX25. RED JVM/runner 1/1, three fresh GREEN 0/0 plus unchanged official runner 0. TableAdapterBulkClose, TableAdapterPagingClose, TableAdapterQueuedClose, TableAdapterClose, TableAdapterSearchListener, DataBinding and TableEditing all 0/0. Maven -Pbuild compile, PowerShell parse and diff check passed. Logs in target/ctrl-092. Corporate SQL and same-table rebinding remain outside scope.


---

## Reply CTRL-093

**DONE.** Baseline 8914be08e3399bed7fabbb55e016412a03da9e72. Verified production 60685aad5e7f564dffdf32e3327754ff333b5fc9. DSFXAdapter plus focused test and mode only.

Shown real table, in-memory ArrayDataSet, actual progress worker. RED produced one obsolete NPE each before entry, after the initial scan, during temporary registration, and after a swaps-more result. Three late INSERT notifications each invoked two obsolete predicate calls in registered scenarios. GREEN eliminated all four NPEs and all six obsolete predicate calls. Closed cases used 0/1/1/1/1 initial scans and 0/0/0/1/1 swaps for pre-entry, initial-scan, registration, swaps-more and last swap; registrations/removals were 0/0 for the first two and 1/1 exact identity for each later case. Independent listeners saw both late signals. The completed last-swap miss still delivered false once after retirement as before. Incomplete retired searches delivered no invented negative result. Active hit/miss/cancel retained true/false/false once, and active error retained its original message and zero result callbacks.

Search captures original Dataset and the CTRL-092 volatile epoch at launch. The worker checks before admissions and later reporting; the owned row listener checks before admitting a scan. Owned registrations are removed in finally from their origin, retaining primary exception and suppressed cleanup. Completed consumer values remain deliverable independently of table ownership. Generic error and no-consumer alerts retain existing policy for CTRL-094. No added instance state or normal listener allocation; captures origin/epoch and moves cancellation-state publication into the FX result delivery. In the no-consumer hit case this adds one FX callback for that state update. No blocking locks, dependencies, SQL, transaction or rebind policy changes.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterSearchWorkerClose -SettingsFile "D:\Java\.m2\settings.xml". Java25/FX25, RED JVM/runner 1/1, three fresh GREEN 0/0, unchanged official runner 0. TableAdapterSearchListener, TableAdapterBulkWorkerClose, TableAdapterBulkClose, TableAdapterQueuedClose, DataBinding and TableAdapterClose all 0/0. Maven -Pbuild compile, PowerShell parse and diff check passed. Full exit/stream evidence target/ctrl-093.


---

## Reply CTRL-094

**DONE / READ-ONLY.** Baseline and verified final production 60685aad5e7f564dffdf32e3327754ff333b5fc9. No tracked edits.

Real progress window on worker-backed search and bulk, old and independent fresh shown tables, deterministic worker/FX barriers. Logger fixture checks original Throwable identity without changing error routing. The matrix below was collected with an offline BaseApp. An earlier run without the App exposed a fixture-only alert prerequisite; both runs and exit codes remain saved.

    ACTIVE_HIT swaps=0 timeline=[progress-show, consumer:true:freshShowing=true, progress-hidden] oldSel=B oldCursor=1 freshSel=fresh freshCursor=0
    ACTIVE_MISS_ALERT swaps=1 timeline=[progress-show, swap-enter, swap-return:false, dialog-shown, dialog-owner:old, progress-hidden, dialog-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    ACTIVE_ERROR swaps=1 timeline=[progress-show, swap-enter, swap-throw, progress-hidden, original-error-identity, dialog-shown, dialog-owner:other, dialog-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    ACTIVE_CANCEL swaps=1 timeline=[progress-show, swap-enter, cancel, swap-interrupt, swap-return:false, progress-hidden, consumer:false:freshShowing=true] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    RETIRED_EXTERNAL swaps=1 timeline=[progress-show, swap-enter, retired, swap-return:false, consumer:false:freshShowing=true, progress-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    RETIRED_ALERT swaps=1 timeline=[progress-show, swap-enter, retired, swap-return:false, dialog-shown, dialog-owner:old, progress-hidden, dialog-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    RETIRED_DETACHED_ALERT swaps=1 timeline=[progress-show, swap-enter, retired, swap-return:false, FX-error:java.lang.NullPointerException: Cannot invoke "javafx.scene.Scene.getWindow()" because the return value of "javafx.scene.control.TableView.getScene()" is null, progress-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    ERROR_BEFORE_RETIRE swaps=1 timeline=[progress-show, swap-enter, swap-throw, progress-hidden, retired, original-error-identity, dialog-shown, dialog-owner:other, dialog-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    ERROR_AFTER_RETIRE swaps=1 timeline=[progress-show, swap-enter, retired, swap-throw, progress-hidden, original-error-identity, dialog-shown, dialog-owner:other, dialog-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0
    BULK_ERROR_AFTER_RETIRE swaps=1 timeline=[progress-show, swap-enter, retired, swap-throw, progress-hidden, original-error-identity, dialog-shown, dialog-owner:other, dialog-hidden] oldSel=null oldCursor=-1 freshSel=fresh freshCursor=0

Progress WINDOW_HIDDEN marks teardown. Terminal SUCCEEDED/FAILED/CANCELLED routes follow the actual ProgressService handlers by source, but the task state property was not directly instrumented. Completion from a proven last swap still invokes the external consumer once when the table is retired and a separate caller is showing. The no-consumer path showed one stale information dialog owned by the old stage, or one FX NPE if the old table was removed from its scene. Active miss showed one information dialog normally. This is distinct from the four bulk and four search obsolete worker NPEs already fixed by CTRL-092/093. Before-retirement and after-retirement genuine worker exceptions, including bulk, each preserved the original error identity and showed one generic error dialog after progress teardown. Fresh selection and cursor remained unchanged. No corporate SQL failure, actual shutdown of a corporate form, or rollback/cancellation effect was tested.

In-repo no-consumer caller includes ChoiceFilterController filter-selection search. No evidence that the external Consumer<Boolean> must share the lifetime of the table. Suggested isolated next contract: gate only the table-owned no-consumer miss alert by captured attachment and scene/window validity on FX delivery, keep the independent consumer and genuine errors, and explicitly test old/fresh windows. Changes to generic progress error routing require a separate contract.

Reproduce with ignored target/ctrl-094/ProgressDeliveryAudit.java. javac -cp "target/classes;<deps>" -d target/ctrl-094 target/ctrl-094/ProgressDeliveryAudit.java, then java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-094;target/classes;<deps>" ProgressDeliveryAudit, deps from target/ctrl-079a/original-dependencies.txt. Java 25.0.1/FX25. Compile and actual JVM 0, no probe runner. Captured FX errors above are observations, distinct from native process exit. No production state/allocation delta.


---

## Reply CTRL-095

**DONE / READ-ONLY.** Baseline and verified final production 60685aad5e7f564dffdf32e3327754ff333b5fc9. No tracked edits.

Isolated actual DSFXAdapter mark value factory on a shown JInvTable with 2000 preloaded in-memory IMarkable rows and 19 visible. Warmed workload prebuilt 1024 alternating mark inputs and stored each returned observable in an escaping sink. Java 25.0.1 / FX 25, 1 GiB heap, FX thread timing and ThreadMXBean bytes. Five warmup and nine measured batches of 131072 reads, median (range) per read.

| Factory | ns/read | B/read |
| --- | --- | --- |
| Production SimpleBooleanProperty snapshot | 15.655 (15.427–19.355) | 40 (40–40) |
| Per-read immutable snapshot | 12.315 (12.244–15.411) | 24 (24–24) |
| Two shared immutable constants | 10.810 (10.776–11.702) | 0 (0–0) |

Retained A/B properties were distinct, and a repeat read of A returned another property. Mutating A's detached property fired its independent observer once and neither changed B nor the row. Marking B in the in-memory Dataset changed new reads but neither its retained snapshot nor its observer. The actual renderer marked/unmarked A exactly once each and set the row pseudoclass on mark. In-memory descending permutation preserved row identity; B was selected and unmarked by its new index. Four individual mark/unmark writes. Bulk mark-all action and corporate mark SQL were not exercised. Both candidates match value snapshots; constants share identity and the immutable candidate drops writable property behavior, so neither demonstrates full compatibility. No shared mutable proxy.

At 19 factory values per observed refresh, isolated properties contribute about 760 B versus the earlier mark-enabled total 3.148 MB (about 0.024 percent) and about 0.041 percent of the 1.853 MB mark-enabled increase. Saving 16 B per read would remove about 304 B at that viewport. This extrapolates isolated allocations to the observed count; it does not measure retained heap or paint. Rank low. Prioritize renderer/layout reconstruction. Any narrow snapshot follow-up needs evidence about public writable/identity consumers, listener independence and mark/sort/selection parity.

Ignored target/ctrl-095/MarkerFactoryAudit.java and audit streams. Compile with javac -cp "target/ctrl-088;target/classes;<deps>" -d target/ctrl-095 target/ctrl-095/MarkerFactoryAudit.java then java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-095;target/ctrl-088;target/classes;<deps>" MarkerFactoryAudit. Deps from target/ctrl-079a/original-dependencies.txt. javac and JVM exit 0, no runner. No production state/allocation delta.


---

## Reply CTRL-096

**NO_CHANGE / READ-ONLY.** Baseline and verified final production 60685aad5e7f564dffdf32e3327754ff333b5fc9. No tracked edits.

Inspected JavaFX 25 TableView.refresh bytecode from the installed jar, and actual JInvTable/DSFXAdapter public refresh paths. Full refresh requests cell recreation; AFTER EXECUTE delivers the adapter refresh in a queued callback. DSFXAdapter.refreshRow uses same-index list replacement plus focus restoration, including for mark notifications. Row UPDATE also queues navigation. A new list, an in-place replacement and a property invalidation expose different list/selection/edit semantics. No private skin/VirtualFlow access.

Shown real JInvTable and DSFXAdapter, in-memory XXIDataSet fixture, 200/2000 loaded rows, 240/480 viewport, 1/4 text columns, marker off/on. Five warmup and nine measured samples per mode and configuration. Measures public post-layout pulse, FX thread allocated bytes and cell/value factories per single update. A value-factory snapshot of a plain string models a source that does not itself notify cells. Compare actual AFTER EXECUTE full refresh, actual refreshRow same-item replacement, a public extractor-generated UPDATE, and requestLayout. Mark is a plain in-memory value through the production factory/renderer. Cursor and selected identity stayed at the original row; measured updates made zero commits. Full data and ranges follow.

    N=200 height=240 C=1 mark=false mode=FULL postlayout-us=11785(9261..15888) FX-bytes=612592(610288..614872) cells=9(9..9) values=9(9..9) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=200 height=240 C=1 mark=false mode=ROW_REPLACE postlayout-us=15075(3355..38415) FX-bytes=57072(57072..529360) cells=0(0..0) values=18(18..18) pulses=1(1..2) notice=0 text=9/9 marks=9/9
    N=200 height=240 C=1 mark=false mode=EXTRACTOR postlayout-us=15247(14189..30646) FX-bytes=7464(7464..7464) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=200 height=240 C=1 mark=false mode=LAYOUT_ONLY postlayout-us=14411(13586..14986) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=FULL postlayout-us=10168(8829..12753) FX-bytes=610680(608376..619000) cells=9(9..9) values=9(9..9) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=ROW_REPLACE postlayout-us=6616(3195..18035) FX-bytes=54896(54896..56168) cells=0(0..0) values=18(18..18) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=EXTRACTOR postlayout-us=14995(14722..15581) FX-bytes=7464(7464..7464) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=LAYOUT_ONLY postlayout-us=15259(14819..15546) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=FULL postlayout-us=16251(13139..35795) FX-bytes=1284160(1282720..1291344) cells=19(19..19) values=19(19..19) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=ROW_REPLACE postlayout-us=9076(7812..23346) FX-bytes=92016(92016..92328) cells=0(0..0) values=38(38..38) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=EXTRACTOR postlayout-us=14861(12986..16565) FX-bytes=8424(8424..8424) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=LAYOUT_ONLY postlayout-us=14454(13365..15716) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=FULL postlayout-us=14945(12295..18182) FX-bytes=3550232(3545416..3600928) cells=76(76..76) values=76(76..76) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=ROW_REPLACE postlayout-us=6555(4990..35744) FX-bytes=269672(269672..270544) cells=0(0..0) values=152(152..152) pulses=1(1..2) notice=0 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=EXTRACTOR postlayout-us=15290(14592..16019) FX-bytes=8424(8424..8424) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=LAYOUT_ONLY postlayout-us=15444(13850..16383) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=4 mark=true mode=FULL postlayout-us=30439(19872..35586) FX-bytes=5417888(5414696..5440816) cells=95(95..95) values=95(95..95) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=true mode=ROW_REPLACE postlayout-us=14775(8435..28395) FX-bytes=354856(354856..355640) cells=0(0..0) values=190(190..190) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=true mode=EXTRACTOR postlayout-us=14883(13327..15340) FX-bytes=8424(7512..8424) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=5/9
    N=2000 height=480 C=4 mark=true mode=LAYOUT_ONLY postlayout-us=14498(13770..15198) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=5/9
    editor validation FULL preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor FULL draft=row-0 starts=2 cancels=1 validation=3 rejection=1 commits=1 staleWrite=false listSignals=[REPLACE]
    editor validation ROW_REPLACE preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor ROW_REPLACE draft=no-editor starts=1 cancels=1 validation=2 rejection=1 commits=1 staleWrite=false listSignals=[REPLACE, REPLACE, REPLACE]
    editor validation EXTRACTOR preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor EXTRACTOR draft=no-editor starts=1 cancels=1 validation=2 rejection=1 commits=1 staleWrite=false listSignals=[UPDATE, REPLACE, UPDATE]
    editor validation LAYOUT_ONLY preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor LAYOUT_ONLY draft=draft starts=1 cancels=0 validation=2 rejection=1 commits=1 staleWrite=false listSignals=[REPLACE]

At N=2000, 480 viewport, 4 text plus mark columns, full refresh allocated 5.418 MB median and created 95 cells. Row replacement allocated 0.355 MB, created zero cells and read 190 values, with correct target text and mark in 9/9; post-layout time was 30.439 ms versus 14.775 ms with overlapping ranges. Replacement emitted observable REPLACE and canceled the active draft without a replacement editor; full refresh canceled/restarted, showing model text in a new editor. Extractor UPDATE also canceled without a replacement editor and did not refresh the plain text snapshot (0/9). RequestLayout retained the draft but did not update snapshot text (0/9). The targeted paths did not deliver the AFTER EXECUTE refresh observer call. No equivalence for full refresh. Actual edit identity probe reopened a missing editor before testing rejected F8 (one original exception, zero commits), valid F8 (one commit) and armed same-index equal-valued distinct replacement (zero stale writes) in each mode. Counts are converter checks, not a full business validator matrix. Earlier diagnostic attempt exit 1 on a candidate because its generic text-field lookup selected a non-editor; its raw result is preserved. Final fixture identifies the actual editing TableCell graphic. CTRL-089 remains the navigation/refresh interleaving evidence.

Recommended next experiment: isolate one supported row-only mutation with a stated edit/notification contract, then compare the existing refreshRow route against an observable cell value that reports only that row. Existing refreshRow's lower allocation is useful evidence for that narrow workload; it is not a substitute for full refresh or its event order. No production changes, cache, paint or retained heap claim.

Ignored target/ctrl-096/RefreshTargetAudit.java. Reproduce with javac -cp "target/ctrl-088;target/classes;<deps>" -d target/ctrl-096 target/ctrl-096/RefreshTargetAudit.java and java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-096;target/ctrl-088;target/classes;<deps>" RefreshTargetAudit, deps from target/ctrl-079a/original-dependencies.txt. Java 25.0.1 / FX25. Final javac 0 and native JVM 0; no outer probe runner. Raw attempts and exits preserved.


---

## Reply CTRL-097

**DONE / READ-ONLY.** All 53 supported modes (52 named plus Default), including the three new modes, recorded JVM and outer PowerShell exit 0/0 and PASS. No timeout. Maven -Pbuild compile 0, PowerShell parse clean and git diff --check 0.

Assignment a5503f36e34cabe1d2bf2e116a3bc3e9fac610a3. Baseline 3976625111c448f00f07837ca987da7da9a0a1d7, final verified production 60685aad5e7f564dffdf32e3327754ff333b5fc9. Only three fast-forward production commits. CTRL-091 scoped to JInvTextField and JInvTextFieldCUR. CTRL-092 and CTRL-093 scoped to DSFXAdapter. Each adds only its focused smoke and runner mode to tracked tests. CTRL-094…097 left production and tracked tests unchanged.

| Task | Disposition | Evidence |
| --- | --- | --- |
| CTRL-091 | DONE | de95eefc66700425f46720f43119c74298adf0d4. Nine stale currency paths plus default ABA guarded; zero stale text/model writes and accepted navigation. Existing epoch reused. |
| CTRL-092 | DONE | 8914be08e3399bed7fabbb55e016412a03da9e72. Four retired bulk NPE routes eliminated; active success, cancel, error and exact fetch admission checks. |
| CTRL-093 | DONE | 60685aad5e7f564dffdf32e3327754ff333b5fc9. Four retired search NPE routes and six obsolete predicate calls eliminated. Exact originating listener removal and completed last-swap result retained. |
| CTRL-094 | DONE / READ-ONLY | A no-consumer miss can show a stale old-owner dialog or throw FX NPE for a detached table. Completed external consumer and genuine pre/post-retirement errors retained. |
| CTRL-095 | DONE / READ-ONLY | Isolated production marker snapshot is 40 B/read, 15.655 ns median. At 19 reads, about 760 B of the earlier 3.148 MB marked refresh workload. |
| CTRL-096 | NO_CHANGE / READ-ONLY | Row replacement reduced allocations and cell creation but canceled without restarting the editor. Extractor UPDATE/layout-only left stale snapshot text. No tested replacement for full refresh. |
| CTRL-097 | Final verification | See status and complete exit matrix. |

For each implemented fix, tracked unchanged-production RED exited 1/1 (JVM/runner) on the intended assertion; three fresh instrumented GREEN JVMs and all scoped regressions exited 0/0, plus an unchanged official runner invocation exited 0. CTRL-091 first candidate compile failure (runner 1, no probe JVM) and CTRL-096 initial diagnostic failure were recorded and preserved. Final regression uses the faithful ignored copy of the final tracked runner with path resolution adjusted and immediate child exit recorded before the existing exit check. Java 25.0.1 / FX 25, Windows PowerShell 5.1, IDEA Maven 3.9.16 offline. Raw output, PID, both exits, timestamps and timeout markers in target/ctrl-097. No opt-in Robot, CTRL-014/034 or CAL-TIME-001. Default is a no-switch invocation. CTRL-079 initial exit incident remains UNKNOWN / NOT_REPRODUCED, with separate prior architect acceptance.

State and allocation delta: CTRL-091 adds no fields or request wrappers. CTRL-092 adds one volatile long per DSFXAdapter and primitive capture in the existing callback. CTRL-093 reuses it, adds no fields, and adds one FX callback in the no-consumer hit route to publish existing cancellation state on FX. No production cache or optimization commits. Retirement changes worker admission, not whether already-entered blocking work may return. No SQL cancellation or rollback guarantee.

Measurements and compatibility: CTRL-095 candidates measured 24 B/read for isolated immutable snapshots and 0 B/read for shared immutable constants. They change writability or identity; no production comparison or retained-memory estimate. CTRL-096 at 2000 rows, 480 viewport, 4 text columns plus mark measured full refresh 5.418 MB / 95 cells / 95 value calls, versus same-item row replacement 0.355 MB / 0 cells / 190 value calls, median per update. Median post-layout 30.439 vs 14.775 ms, with overlapping ranges. Observer delivery, list events, active editor identity and converter behavior prevent substituting one path for the other. Raw ranges and fixture commands in Replies CTRL-095/096. These measurements concern allocation and observable layout, not paint or retained heap.

Independent Windows commands from project root (IDEA Maven bin on PATH; local replay used MAVEN_ARGS=-o).

    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -CurrencyLovDelayedCompletion -SettingsFile "D:\Java\.m2\settings.xml"
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterBulkWorkerClose -SettingsFile "D:\Java\.m2\settings.xml"
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterSearchWorkerClose -SettingsFile "D:\Java\.m2\settings.xml"
    mvn -s "D:\Java\.m2\settings.xml" -Pbuild "-Dmaven.buildNumber.skip=true" compile

Recommended next candidates in order:

1. Fix the reproducible detached-table search miss NPE and retired-owner dialog. Gate only the table-owned no-consumer FX action by the originating attachment and scene/window lifetime. Keep external Consumer delivery and genuine error handling.
2. Verify Binder ownership in a corporate form with an explicitly shared FXEntity. Require a supported cleanup contract before changing any close/post-bind-failure behavior.
3. Isolate a supported row-only update with a stated editor and mark notification contract. Test public observable updates for that surface before considering any full-refresh change; the measured allocation difference is large but broad replacement breaks behavior.
4. Keep marker snapshot optimization low priority. The isolated property cost accounts for about 0.024 percent of the marked refresh measurement.

Local tests do not establish production readiness. Corporate SQL/transaction behavior, real currency lookup, blocking server failure and cancel, actual focus and native input, form MDI/WAIT and real TaskContext remain separately unverified. Stop after CTRL-097.

Final runner matrix. Columns are mode, probe marker, elapsed seconds, native JVM exit, outer PowerShell exit and timeout.

    CurrencyLovDelayedCompletion | CTRL-091 PASS | 9.232 | 0 | 0 | False
    TableAdapterBulkWorkerClose | CTRL-092 PASS | 10.113 | 0 | 0 | False
    TableAdapterSearchWorkerClose | CTRL-093 PASS | 9.98 | 0 | 0 | False
    TableAdapterSearchListener | CTRL-081 PASS | 9.63 | 0 | 0 | False
    TableEditRowIdentity | CTRL-075 PASS | 9.971 | 0 | 0 | False
    TableAdapterBulkClose | CTRL-076 PASS | 9.359 | 0 | 0 | False
    TableAdapterQueuedClose | CTRL-077 PASS | 9.391 | 0 | 0 | False
    TextFieldLovDelayedCompletion | CTRL-078 PASS | 8.995 | 0 | 0 | False
    TableEditing | GUI-005 PASS | 9.193 | 0 | 0 | False
    TableCancel | GUI-006 PASS | 9.21 | 0 | 0 | False
    EditorReentry | GUI-007 PASS | 9.408 | 0 | 0 | False
    DataBinding | GUI-009 PASS | 10.696 | 0 | 0 | False
    TableAdapterClose | CTRL-051 PASS | 8.831 | 0 | 0 | False
    TableAdapterPagingClose | CTRL-065 PASS | 9.329 | 0 | 0 | False
    TextAreaLovDelayedCompletion | CTRL-062 PASS | 8.927 | 0 | 0 | False
    ExternalLovReadOnly | CTRL-061 PASS | 9.134 | 0 | 0 | False
    LovIntegration | CTRL-031 PASS | 8.718 | 0 | 0 | False
    BoundLovSetter | CTRL-044 PASS | 8.95 | 0 | 0 | False
    LovBindingInterleaving | CTRL-045 PASS | 9.216 | 0 | 0 | False
    MoneyDecimal | NUM-001 PASS (tested invariants) | 9.677 | 0 | 0 | False
    FormLifecycle | GUI-010 PASS (standalone production FXML/controller and SDI close) | 9.213 | 0 | 0 | False
    FormCancel | CTRL-022 PASS | 9.75 | 0 | 0 | False
    SingletonLifecycleMatrix | CTRL-035 PASS | 9.311 | 0 | 0 | False
    TextAreaToolbarOwnership | CTRL-066 PASS | 9.013 | 0 | 0 | False
    WrapButtonOwnership | CTRL-055 PASS | 8.704 | 0 | 0 | False
    TabPaneSkinListeners | CTRL-054 PASS | 8.896 | 0 | 0 | False
    TextAreaSkinDisposal | CTRL-053 PASS | 9.186 | 0 | 0 | False
    ValidatorSubchanges | CTRL-052 PASS | 8.756 | 0 | 0 | False
    TextAreaLovLifecycle | CTRL-050 PASS | 9.511 | 0 | 0 | False
    TextFieldSkinDisposal | CTRL-049 PASS | 9.244 | 0 | 0 | False
    WrapButtonDeferred | CTRL-043 PASS | 9.096 | 0 | 0 | False
    Editors | GUI-004 PASS | 9.958 | 0 | 0 | False
    SceneKeyLookup | CTRL-024 PASS | 8.67 | 0 | 0 | False
    LovListenerLifecycle | CTRL-026 PASS | 8.961 | 0 | 0 | False
    LovDefaultValidation | CTRL-027 PASS | 8.73 | 0 | 0 | False
    SingletonCloseVeto | CTRL-032 PASS | 8.924 | 0 | 0 | False
    ValidatorScopedUnbind | CTRL-033 PASS | 8.724 | 0 | 0 | False
    ValidatorMultiControl | CTRL-036 PASS | 8.942 | 0 | 0 | False
    TextFields | GUI-012 PASS (tested matrix) | 10.209 | 0 | 0 | False
    ValueFields | VF-001 PASS | 9.304 | 0 | 0 | False
    ValueLifecycle | VF-002 PASS (custom listener ownership) | 8.981 | 0 | 0 | False
    DatePickerBinding | CTRL-002 PASS | 8.962 | 0 | 0 | False
    NodeTraversal | CTRL-003 PASS | 8.889 | 0 | 0 | False
    RadioReadOnly | CTRL-006 PASS | 8.969 | 0 | 0 | False
    ComboReadOnly | CTRL-007 PASS | 15.128 | 0 | 0 | False
    CheckBoxReadOnly | CTRL-009 PASS | 15.133 | 0 | 0 | False
    ValueChangedFlag | CTRL-013 PASS | 7.647 | 0 | 0 | False
    ControlsFollowup | CTRL-CONTROLS-FOLLOWUP PASS | 9.325 | 0 | 0 | False
    ChoiceAdapter | CTRL-015 PASS | 9.042 | 0 | 0 | False
    BinderChoiceNull | CTRL-017 PASS | 8.889 | 0 | 0 | False
    ChoiceNullLookup | CTRL-018 PASS | 8.384 | 0 | 0 | False
    ChoiceNonNullSet | CTRL-019 PASS | 8.363 | 0 | 0 | False
    Default | GUI-001 PASS | 9.37 | 0 | 0 | False


---

## Reply CTRL-098

**DONE.** Baseline 60685aad5e7f564dffdf32e3327754ff333b5fc9. Verified production df95a931230b57e87e7bef4bfba6267c3e9a395d. DSFXAdapter only in production, plus new focused smoke and runner mode.

A real shown JInvTable and the actual search worker returned an admitted final miss at a deterministic barrier. RED recorded one stale old-owner alert after adapter retirement, one FX NPE after scene detachment, and one dialog each for a scene without a window, a reparented table, and a hidden launch window. GREEN gave zero alerts and zero FX errors across the five. Active miss still gave one alert with its original owner. Active hit gave true, active cancellation and the independent retired-owner consumer each received false once. Genuine active and retired search and bulk failures each retained original Throwable identity and exactly one error dialog. Fresh independent selection/cursor stayed unchanged. Incomplete retired search is covered by TableAdapterSearchWorkerClose.

The FX miss-only branch checks the existing attachment/revision after cancellation-state publication, then requires a scene belonging to the original shown window. No extra persistent fields or callback objects, only the launch-owner reference captured by existing closures. No change to genuine error routing, external consumers, progress service, SQL or transaction semantics. A separate cancellation-state reentry injection was not used; the code placement is after the observable publication.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterSearchMissDelivery -SettingsFile "D:\Java\.m2\settings.xml". Java 25.0.1/FX25. RED JVM/runner 1/1, three fresh GREEN 0/0 plus unchanged official runner exit 0. SearchWorkerClose, SearchListener, BulkWorkerClose, QueuedClose, DataBinding and TableAdapterClose all 0/0. Maven -Pbuild compile, parse and diff check passed. The first implementation attempt passed the owner reference without guarding delivery and repeated the intended RED; preserved alongside all actual streams/exits under target/ctrl-098.


---

## Reply CTRL-099

**DONE / READ-ONLY.** Baseline and verified final production df95a931230b57e87e7bef4bfba6267c3e9a395d. No tracked edits.

DSFXAdapter.refreshRow queues a same-item list replacement selected by the index at delivery, then restores focus. markAction forwards an event index, not the row reference. A genuine AbstractDataSetBase.updateCurrentRow in the installed JInvCommon bytecode changes the list and fires UPDATE; DSFXAdapter queues positionRow on UPDATE. SQLDataSet.refreshCurrentRowFromDB(boolean, Consumer) can call updateCurrentRow; no SQL was run. In-repo row refresh callers reviewed; explicit full refresh, AFTER EXECUTE, toolbar-mode change and bulk mark have table-wide contracts. Single mark is already targeted and genuine row update uses the ObservableList. No supported row-only caller currently doing unnecessary full reconstruction was established.

Real shown table, real adapter, in-memory Dataset, plain marker state and production renderer. Deferred close/detach still produced A→A REPLACE; no NPE. Equal-distinct replacement, removal and permutation made the queued refresh operate on X, B and R199 respectively, with zero commits/writes. Mark notification similarly observed X after replacement while the actual mark stayed on A. Explicit index refresh and row-event ownership require separate contracts. Genuine UPDATE issued A→X then UPDATE and no extra synthetic refresh. Same-row and other-row targeted refresh canceled the draft in this fixture. F8 and ENTER committed A once; ENTER moved focus/selection/cursor to B; Escape and rejected conversion wrote zero; rejection delivered one IllegalArgumentException. Offscreen index 150 emitted REPLACE and kept selection/cursor A. Zero full refresh and zero cell factory calls across the matrix.

    SAME occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    CLOSED occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    DETACHED occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=0 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    EQUAL_REPLACE occupant=X selected=X focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[REPLACE:0:[A]->[X], enqueue-end, REPLACE:0:[X]->[X]]
    REMOVE occupant=B selected=B focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=54 full=0 log=[REMOVE:0:[A]->[], enqueue-end, REPLACE:0:[B]->[B]]
    PERMUTE occupant=R199 selected=A focus=199 cursor=199 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[PERMUTE, enqueue-end, REPLACE:0:[R199]->[R199]]
    OFFSCREEN occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:150:[R150]->[R150]]
    MARK occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=true markFirst=true cell=0 value=36 full=0 log=[mark:0, enqueue-end, REPLACE:0:[A]->[A]]
    MARK_REPLACE occupant=X selected=X focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=true markFirst=false cell=0 value=36 full=0 log=[mark:0, REPLACE:0:[A]->[X], enqueue-end, REPLACE:0:[X]->[X]]
    DATASET_UPDATE occupant=X selected=X focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[REPLACE:0:[A]->[X], dataset:UPDATE:0, enqueue-end]
    EDIT_SAME occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    EDIT_OTHER occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:1:[B]->[B]]
    EDIT_F8 occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[A] checks=1 a=draft x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    EDIT_ENTER occupant=A selected=B focus=1 cursor=1 editor=false draft=none commits=[A] checks=1 a=draft x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    EDIT_ESCAPE occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=0 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[enqueue-end, REPLACE:0:[A]->[A]]
    EDIT_REJECT occupant=A selected=A focus=0 cursor=0 editor=false draft=none commits=[] checks=1 a=row-0 x=equal-X markA=false markFirst=false cell=0 value=36 full=0 log=[error:IllegalArgumentException, enqueue-end, REPLACE:0:[A]->[A]]

Suggested separately authorized correctness work: guard table-owned deferred refresh against retired attachment, then agree whether mark notification invalidates only its original identity or may refresh its former slot. Preserve the explicitly index-based API unless its caller contract changes. Deterministic assertions should count late list events and use equal-valued distinct objects before/after permutation. These observations do not establish a wrong database write. Further production optimization has no qualifying caller here.

Compile ignored target/ctrl-099/RowRefreshAudit.java with javac -cp "target/ctrl-088;target/classes;<deps>" -d target/ctrl-099, then java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-099;target/ctrl-088;target/classes;<deps>" RowRefreshAudit (deps from target/ctrl-079a/original-dependencies.txt). Java 25.0.1/FX25, compile and native JVM 0, no runner. Local audit streams and dataset-bytecode.txt preserved. No production state/allocation changes.


---

## Reply CTRL-100

**NO_CHANGE.** Baseline and verified final production df95a931230b57e87e7bef4bfba6267c3e9a395d. CTRL-099 found no eligible row-only caller doing a full refresh. Actual mark publication already uses refreshRow and ordinary row UPDATE already replaces the list entry and queues navigation. Full/AFTER EXECUTE and bulk marker operations can touch multiple rows. No production edit or tracked probe.

Repeated the same shown JavaFX25 comparison as CTRL-096 on this SHA. Five warmups and nine single-update samples per configuration, FX ThreadMXBean bytes and elapsed to public post-layout callback. Identical rows, columns, viewport and marker inputs. At 2000 rows, height 480, four text plus marker columns, full refresh was 5,423,176 B median (5,413,832–5,446,136), 28,621 µs (22,008–36,987), 95 cells/95 values. Same-item replacement was 358,600 B (358,504–358,912), 15,128 µs (13,350–16,957), 0 cells/190 values. Both rendered the target text/mark 9/9. Replacement sent REPLACE, lost the AFTER EXECUTE refresh delivery and canceled the active editor without restarting it. Full refresh canceled/restarted from model text. Extractor and requestLayout did not render plain snapshot text. Thus the measured savings do not justify substituting for full refresh. Existing row-only retirement/identity observations are separate correctness candidates.

    N=200 height=240 C=1 mark=false mode=FULL postlayout-us=14885(12351..15877) FX-bytes=612016(610288..614752) cells=9(9..9) values=9(9..9) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=200 height=240 C=1 mark=false mode=ROW_REPLACE postlayout-us=15356(11513..16191) FX-bytes=57072(57072..57384) cells=0(0..0) values=18(18..18) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=200 height=240 C=1 mark=false mode=EXTRACTOR postlayout-us=15744(15465..16491) FX-bytes=7464(7464..7464) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=200 height=240 C=1 mark=false mode=LAYOUT_ONLY postlayout-us=15541(14701..16771) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=FULL postlayout-us=16127(12717..17942) FX-bytes=610104(608376..612192) cells=9(9..9) values=9(9..9) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=ROW_REPLACE postlayout-us=15450(14778..16300) FX-bytes=54896(54896..56168) cells=0(0..0) values=18(18..18) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=EXTRACTOR postlayout-us=15443(14843..16343) FX-bytes=7464(7464..7464) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=240 C=1 mark=false mode=LAYOUT_ONLY postlayout-us=15658(14664..16556) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=FULL postlayout-us=14807(11294..16915) FX-bytes=1284160(1282720..1291344) cells=19(19..19) values=19(19..19) pulses=1(1..1) notice=9 text=9/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=ROW_REPLACE postlayout-us=15055(13951..17029) FX-bytes=92016(91560..92728) cells=0(0..0) values=38(38..38) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=EXTRACTOR postlayout-us=15914(14880..16332) FX-bytes=8424(8424..8424) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=1 mark=false mode=LAYOUT_ONLY postlayout-us=15455(12938..16917) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=FULL postlayout-us=17236(13479..30584) FX-bytes=3563176(3561160..3601720) cells=76(76..76) values=76(76..76) pulses=1(1..2) notice=9 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=ROW_REPLACE postlayout-us=15190(14227..16427) FX-bytes=273320(273320..273968) cells=0(0..0) values=152(152..152) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=EXTRACTOR postlayout-us=15724(14827..16303) FX-bytes=8424(8424..8424) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=4 mark=false mode=LAYOUT_ONLY postlayout-us=15656(14642..16410) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=9/9
    N=2000 height=480 C=4 mark=true mode=FULL postlayout-us=28621(22008..36987) FX-bytes=5423176(5413832..5446136) cells=95(95..95) values=95(95..95) pulses=1(1..2) notice=9 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=true mode=ROW_REPLACE postlayout-us=15128(13350..16957) FX-bytes=358600(358504..358912) cells=0(0..0) values=190(190..190) pulses=1(1..1) notice=0 text=9/9 marks=9/9
    N=2000 height=480 C=4 mark=true mode=EXTRACTOR postlayout-us=15743(14902..16181) FX-bytes=8424(7512..8424) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=5/9
    N=2000 height=480 C=4 mark=true mode=LAYOUT_ONLY postlayout-us=15565(14248..16214) FX-bytes=968(968..968) cells=0(0..0) values=0(0..0) pulses=1(1..1) notice=0 text=0/9 marks=5/9
    editor validation FULL preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor FULL draft=row-0 starts=2 cancels=1 validation=3 rejection=1 commits=1 staleWrite=false listSignals=[REPLACE]
    editor validation ROW_REPLACE preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor ROW_REPLACE draft=no-editor starts=1 cancels=1 validation=2 rejection=1 commits=1 staleWrite=false listSignals=[REPLACE, REPLACE, REPLACE]
    editor validation EXTRACTOR preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor EXTRACTOR draft=no-editor starts=1 cancels=1 validation=2 rejection=1 commits=1 staleWrite=false listSignals=[UPDATE, REPLACE, UPDATE]
    editor validation LAYOUT_ONLY preexistingCommits=0 rejectedCommitDelta=0 errors=[java.lang.IllegalArgumentException: invalid draft]
    editor LAYOUT_ONLY draft=draft starts=1 cancels=0 validation=2 rejection=1 commits=1 staleWrite=false listSignals=[REPLACE]

Reproduce by compiling target/ctrl-096/RefreshTargetAudit.java with javac -cp "target/ctrl-088;target/classes;<deps>" -d target/ctrl-100, then java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-100;target/ctrl-088;target/classes;<deps>" RefreshTargetAudit. Deps from target/ctrl-079a/original-dependencies.txt. Native compile/JVM 0; no outer probe runner. No production state/allocations changed; no paint or retained heap claim.


---

## Reply CTRL-101

**DONE / READ-ONLY.** Baseline and verified final production df95a931230b57e87e7bef4bfba6267c3e9a395d. No tracked edits.

Shown real JInvTable and DSFXAdapter reading production mark values/renderer. Single mark/unmark used actual markAction → deferred refreshRow. Bulk used actual markAll/unMarkAll → JInvParallelAction worker → queued full refresh. In-memory XXIDataSet overrides recorded logical writes, individual mark notifications and savePrev flags. Bulk override applies in-memory fields and emits no row notifications, so actual corporate bulk per-row notification cost is UNKNOWN. Both true/false flags reached the worker and no bulk logical writes ran on FX. Empty big table admitted zero bulk work. Visible marker and offscreen row states, stable selection/cursor/focus, equal-distinct reordered mark target and active editor were checked. Same/other-row marker updates canceled active draft in this fixture, zero commits.

Oracle Java 25.0.1/FX25, 1 GiB, FX allocation and elapsed to post-layout, five warmup and nine single samples per operation; 200/2000 loaded, 240/480 viewport, one text plus marker column. At 2000 / 480, single mark was 169,144 B median (166,816–169,832) and 15,671 µs (12,737–17,367), zero cells and one list notification. Bulk mark was 3,150,568 B (3,147,536–3,169,312) and 14,927 µs (10,268–18,241), 38 cells, one full refresh and 2000 worker writes. Post-layout timing overlaps; allocation delta predominantly includes viewport reconstruction, not marker snapshot cost alone. Totals9 below are across nine recorded samples, allocation and elapsed per operation.

    N=200 height=240 bulk=false mark=true us=15509(14280..17083) FX-bytes=98344(98264..98888) totals9 cells=0 values=324 full=0 notify=9 writes=9 list=9
    N=200 height=240 bulk=false mark=false us=15019(13421..16353) FX-bytes=98264(98264..98368) totals9 cells=0 values=324 full=0 notify=9 writes=9 list=9
    N=200 height=240 bulk=true mark=true us=15596(11907..26380) FX-bytes=1525680(1525224..1544720) totals9 cells=162 values=162 full=9 notify=0 writes=1800 list=0
    N=200 height=240 bulk=true mark=false us=14811(13179..19499) FX-bytes=1515896(1505592..1531104) totals9 cells=162 values=162 full=9 notify=0 writes=1800 list=0
    bulk savePrev values=[true, false] fxThreadWrites=0 focus=0
    N=200 height=480 bulk=false mark=true us=15303(11237..25720) FX-bytes=176200(175888..179304) totals9 cells=0 values=684 full=0 notify=9 writes=9 list=9
    N=200 height=480 bulk=false mark=false us=15361(4519..16479) FX-bytes=175864(175864..179064) totals9 cells=0 values=684 full=0 notify=9 writes=9 list=9
    N=200 height=480 bulk=true mark=true us=13493(11739..24431) FX-bytes=3176944(3171016..3190056) totals9 cells=342 values=342 full=9 notify=0 writes=1800 list=0
    N=200 height=480 bulk=true mark=false us=16751(12534..28465) FX-bytes=3156272(3150344..3268616) totals9 cells=342 values=342 full=9 notify=0 writes=1800 list=0
    bulk savePrev values=[true, false] fxThreadWrites=0 focus=0
    N=2000 height=240 bulk=false mark=true us=15543(14382..16477) FX-bytes=93976(93896..94608) totals9 cells=0 values=324 full=0 notify=9 writes=9 list=9
    N=2000 height=240 bulk=false mark=false us=15582(14863..16537) FX-bytes=93896(93896..94000) totals9 cells=0 values=324 full=0 notify=9 writes=9 list=9
    N=2000 height=240 bulk=true mark=true us=15278(9504..17474) FX-bytes=1501224(1501096..1511944) totals9 cells=162 values=162 full=9 notify=0 writes=18000 list=0
    N=2000 height=240 bulk=true mark=false us=15840(13561..18668) FX-bytes=1491304(1491304..1493848) totals9 cells=162 values=162 full=9 notify=0 writes=18000 list=0
    bulk savePrev values=[true, false] fxThreadWrites=0 focus=0
    N=2000 height=480 bulk=false mark=true us=15671(12737..17367) FX-bytes=169144(166816..169832) totals9 cells=0 values=684 full=0 notify=9 writes=9 list=9
    N=2000 height=480 bulk=false mark=false us=15467(14646..16952) FX-bytes=168992(166816..169720) totals9 cells=0 values=684 full=0 notify=9 writes=9 list=9
    N=2000 height=480 bulk=true mark=true us=14927(10268..18252) FX-bytes=3150568(3147536..3169312) totals9 cells=342 values=342 full=9 notify=0 writes=18000 list=0
    N=2000 height=480 bulk=true mark=false us=15179(11260..16854) FX-bytes=3130512(3127776..3136872) totals9 cells=342 values=342 full=9 notify=0 writes=18000 list=0
    bulk savePrev values=[true, false] fxThreadWrites=0 focus=0
    editor sameActive=false otherActive=false commits=0 duplicate-target=199 distinct=true bigEmptyBulkCalls=0
    close pending=true failure=false staleRefresh=1 bulkWrites=200 firstMark=true select=0 cursor=0 FXerrors=[]
    close pending=false failure=false staleRefresh=1 bulkWrites=200 firstMark=true select=0 cursor=0 FXerrors=[]
    close pending=true failure=true staleRefresh=0 bulkWrites=0 firstMark=false select=0 cursor=0 FXerrors=[java.lang.NullPointerException: Cannot invoke "javafx.scene.Scene.getWindow()" because the return value of "javafx.scene.control.TableView.getScene()" is null]
    active genuine error identity=1 dialogs=1 bulkWrites=0

Closing twice while the bulk worker was paused or after its successful result was queued still gave one retired full refresh and 200 already admitted writes, with unchanged selection/cursor. The detached error route produced one FX NPE from getScene().getWindow(), losing the original error delivery; active error logged the original Throwable identity and displayed once. Proposed first follow-up: explicitly scope table-owned bulk refresh lifetime and ensure genuine error dispatch with a neutral owner when its original table is detached, after agreement on the error contract. No claim of canceling admitted work or handling SQL rollback. Preserve existing snapshot semantics and notification order. Existing extractor/layout-only alternatives failed marker equivalence in CTRL-096/100, so no optimization proposed here.

Ignored target/ctrl-101/MarkLifecycleAudit.java, compile javac -cp "target/ctrl-088;target/classes;<deps>" -d target/ctrl-101 and run java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-101;target/ctrl-088;target/classes;<deps>" MarkLifecycleAudit. Deps from target/ctrl-079a/original-dependencies.txt. Native compile/JVM 0; no outer runner. Logs record captured FX failure independently of process exit. No state/allocation changes, retained heap or paint inference.


---

## Reply CTRL-102

**DONE / READ-ONLY.** Baseline and verified final production df95a931230b57e87e7bef4bfba6267c3e9a395d. No tracked edits.

Reviewed repository ManageHelp.fxml and export/form layouts. Used a clearly synthetic shown nested SDI FXML fixture with 20/100/400 JInvTextFields and JInvLabels in nested groups and ScrollPane, one real JInvTable with a stock column and 40 preloaded rows, actual FXFormLauncher/AbstractBaseController/Binder/FXEntity. Repeated canonical property name is intentional and stresses duplicate matches and shared model, not a claim about typical corporate forms. Fresh model per launch; both directions asserted on actual bound field. Each size tested close veto once and every launch closed with one callback and worker cleanup before a fresh singleton launch. Fixture overrides license/offline initialization and preference persistence; real TaskContext, corporate forms, MDI/WAIT untested.

Java 25.0.1/FX25, five warmup and nine measured launches per size. FX-thread ThreadMXBean exact allocated bytes and wall times in µs, median(range). First stamp is a queued FX marker immediately before internalShow and FXML runs through the Initializable callback. Stage assembly ends at controller-init entry. init-entry-to-shown includes synchronous setup, showing-triggered controller hooks and initial CSS/layout work; bind/labels/config are nested observations, not additional totals. shown-to-layout is the public first post-layout pulse; no paint claim. Close includes normal close request and worker completion. Measurement stamps/map writes and incidental same-thread work in nested FX loops contribute some overhead. Fresh 400 process used 2 GiB; 20/100 process used 1 GiB, so avoid treating their timings as a controlled scaling ratio.

    N=20 phase=shown-to-layout us=90160(35319..121299) FXbytes=1888040(1879056..1932944)
    N=20 phase=close-to-cleanup-return us=130966(73256..215952) FXbytes=1014720(1006048..1046904)
    N=20 phase=labels us=805(241..3381) FXbytes=9848(9848..11952)
    N=20 phase=config-including-offline-table us=5578(2363..10659) FXbytes=88888(88864..100920)
    N=20 phase=bind-excluding-labels us=4314(2580..9241) FXbytes=131512(129592..134648)
    N=20 phase=first-layout-total us=323134(190471..435447) FXbytes=12898048(12859616..13204768)
    N=20 phase=FXML-through-initialize us=127725(83364..283226) FXbytes=5488728(5480832..5718976)
    N=20 phase=stage-assembly us=1813(628..24770) FXbytes=173048(173048..173048)
    N=20 phase=init-form-synchronous us=1036(411..3278) FXbytes=20264(20264..21768)
    N=20 phase=bind-including-labels us=6650(2822..11357) FXbytes=141360(139440..146600)
    N=20 phase=init-entry-to-shown us=84340(70035..126659) FXbytes=5342464(5326784..5380560)
    N=100 phase=shown-to-layout us=43219(27680..121802) FXbytes=3633688(3628224..3640904)
    N=100 phase=close-to-cleanup-return us=102868(67500..227361) FXbytes=1196576(1195800..1205008)
    N=100 phase=labels us=986(365..2540) FXbytes=48104(48104..48264)
    N=100 phase=config-including-offline-table us=3592(1448..8210) FXbytes=118184(118160..118616)
    N=100 phase=bind-excluding-labels us=5591(2499..11323) FXbytes=637120(633120..639520)
    N=100 phase=first-layout-total us=417710(197011..843641) FXbytes=48191592(48164024..48334536)
    N=100 phase=FXML-through-initialize us=235831(101433..496529) FXbytes=25180152(25164120..25243016)
    N=100 phase=stage-assembly us=940(495..1944) FXbytes=172992(172968..172992)
    N=100 phase=init-form-synchronous us=503(301..1123) FXbytes=20264(20264..20264)
    N=100 phase=bind-including-labels us=6231(3092..13863) FXbytes=685224(681224..687624)
    N=100 phase=init-entry-to-shown us=106617(63725..284442) FXbytes=19204680(19198712..19346712)
    N=400 phase=close-to-cleanup-return us=117398(85859..191237) FXbytes=1960632(1957320..2016256)
    N=400 phase=labels us=5190(3393..10225) FXbytes=192664(192664..192664)
    N=400 phase=config-including-offline-table us=15433(5506..30912) FXbytes=243888(243888..244432)
    N=400 phase=bind-excluding-labels us=16485(11103..128056) FXbytes=2651120(2651120..2652592)
    N=400 phase=first-layout-total us=704841(474576..1156252) FXbytes=180762936(180678000..181164000)
    N=400 phase=FXML-through-initialize us=392073(242025..726264) FXbytes=99046464(99028192..99299664)
    N=400 phase=stage-assembly us=851(565..1407) FXbytes=172992(172992..172992)
    N=400 phase=init-form-synchronous us=905(608..1396) FXbytes=20264(20264..20264)
    N=400 phase=bind-including-labels us=21676(15641..133099) FXbytes=2843784(2843784..2845256)
    N=400 phase=init-entry-to-shown us=224007(151330..378824) FXbytes=71547552(71459280..71767280)
    N=400 phase=shown-to-layout us=115219(71592..245199) FXbytes=10003440(9999368..10049568)

Largest 400-field allocation phases were FXML through initialization (99.05 MB), controller-init entry through shown (71.55 MB including binding/configuration and initial skin/CSS work), then shown through first layout (10.00 MB). Total 180.76 MB and 704.841 ms median. Inside the second interval, Binder excluding labels 2.65 MB, config 0.244 MB and label matching 0.193 MB. Do not add overlapping intervals. Source identifies persistent per-field RequiredState, several properties, and skin-owned ContextMenu, wrapper and overflow button, plus initially lazy LOV listener. Counts are not retained heap or per-class allocation. Smallest next experiment: compare equivalent stock versus framework field construction and instrument constructor versus first CSS/skin installation in the same novel one-control fixture; use an allocation profiler if attributing classes. Keep CTRL-086/087 rejected traversal/key indexing out of scope. Consider bind-side profiling only after those larger costs are understood. No GC-based leak conclusion.

Ignored target/ctrl-102/FormStartupAudit.java and generated startup-N.fxml. javac -cp "target/ctrl-084;target/classes;<deps>" -d target/ctrl-102 target/ctrl-102/FormStartupAudit.java; java -Xmx1024m --enable-native-access=ALL-UNNAMED -cp "target/ctrl-102;target/ctrl-084;target/classes;<deps>" FormStartupAudit 20 100; repeat with -Xmx2048m and argument 400. Deps from target/ctrl-079a/original-dependencies.txt. Final compile and direct native JVM exits 0, no probe runner. Preserved earlier fixture failures: ignored private FXML hook because controller implements Initializable, unsupported custom column preference naming/offline DB preference persistence, and 1 GiB heap exhaustion while the measurement history held closed controller graphs. Corrected fixture records only phase maps and uses offline preference saver. These failures do not establish application retention. Production state and allocation unchanged.


---

## Reply CTRL-103

**DONE / READ-ONLY.** All 54 supported modes (53 named plus Default) recorded JVM and outer PowerShell exit 0/0 and PASS. No timeout. Maven -Pbuild compile 0, PowerShell parse clean, git diff --check 0.

Assignment e41b3f45acb4f62800f521e0295bd4b5e314edde. Baseline 60685aad5e7f564dffdf32e3327754ff333b5fc9, final verified production df95a931230b57e87e7bef4bfba6267c3e9a395d. One isolated fast-forward production fix, DSFXAdapter only, plus focused smoke and official runner mapping. CTRL-099…103 left production and tracked tests unchanged. No force push or dependency changes.

| Task | Disposition | Verified result |
| --- | --- | --- |
| CTRL-098 | DONE | df95a931230b57e87e7bef4bfba6267c3e9a395d. Retired/no-scene/no-window/reparented/hidden search miss paths suppressed only for table-owned dialog. Active original-owner alert, external Consumer and genuine Throwable routing retained. RED 1/1, three fresh GREEN 0/0, unchanged official runner exit 0 and all six scoped regressions 0/0. |
| CTRL-099 | DONE / READ-ONLY | Deferred refreshRow still emits after retirement, and can refresh a new same-index occupant after replace/remove/permutation. Zero model writes in these tests. Actual single marker path and genuine row UPDATE already targeted; no qualifying row-only full refresh caller. |
| CTRL-100 | NO_CHANGE | Repeated cheaper row replacement differs in refresh delivery, list event and active edit lifecycle. No equivalent supported substitution or eligible caller. |
| CTRL-101 | DONE / READ-ONLY | Single mark targeted, actual parallel bulk mark does full refresh. Two close timings each gave a stale refresh. A detached-table bulk error lost original routing to an FX NPE; active original error identity and dialog retained. |
| CTRL-102 | DONE / READ-ONLY | Shown synthetic 20/100/400-field SDI FXML/Binder forms, post-layout and close/veto/reopen checked. Dominant phases were FXML, controller entry through shown, and first layout. No retained heap claim. |
| CTRL-103 | Final verification | See status and matrix below. |

CTRL-098 guards after FX cancellation-state publication. It adds no persistent state or wrapper allocation, only the launch Stage capture in existing closures. No transaction, generic progress/error or independent Consumer contract changes. Original RED and the first incomplete implementation attempt with unchanged visible failure are saved. CTRL-102 early fixture failures and harness memory pressure are preserved, then corrected in the ignored measurement fixture. None of these were dropped from the evidence. CTRL-079's original incident remains UNKNOWN / NOT_REPRODUCED; prior acceptance is separate.

Selected absolute measurements (full ranges and reproducibility in individual replies):

- CTRL-100 largest case (2000 rows, 480 viewport, four text columns plus mark): full refresh median 5,423,176 B and 28,621 µs, 95 cells; replacement median 358,600 B and 15,128 µs, zero cells, but edit/notification semantics differ. No production savings claimed.
- CTRL-101 (2000, 480, one text plus mark): single mark 169,144 B (166,816–169,832) and 15,671 µs (12,737–17,367), zero cells. Bulk mark 3,150,568 B (3,147,536–3,169,312) and 14,927 µs (10,268–18,241), 38 recreated cells, one full refresh, 2000 in-memory logical writes. No corporate bulk row notification claim.
- CTRL-102 first post-layout: 20 fields 323,134 µs and 12,898,048 B median; 100 fields 417,710 µs and 48,191,592 B; 400 fields 704,841 µs and 180,762,936 B. Different heap settings for the large fixture; timings are not a controlled scaling ratio. FX allocated bytes and post-layout, not retained bytes or paint.

Recommended next candidates in order:

1. Scope bulk mark worker and delivery ownership, including a genuine error after detachment. Preserve admitted operations and original error identity while guarding table-owned refresh; agree on error dialog ownership before editing.
2. Scope deferred refreshRow retirement and mark event identity separately. Prove original event/row identity or documented index ownership, with replace/remove/permutation and late list-observer assertions.
3. Split FXML construction, framework control construction and first skin/CSS installation in a matched offline form workload. Profile classes only after establishing exact phase costs.
4. Prove a narrow supported row-only visual invalidation contract before optimizing. Full refresh and AFTER EXECUTE remain as they are.

Independent Windows rerun from project root with IDEA Maven on PATH. This machine used MAVEN_ARGS=-o and its corporate cache.

    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterSearchMissDelivery -SettingsFile "D:\Java\.m2\settings.xml"
    mvn -s "D:\Java\.m2\settings.xml" -Pbuild "-Dmaven.buildNumber.skip=true" compile

Final regression uses a faithful diagnostic copy of the tracked runner, with path adjustments and immediate child exit recording before the original exit check; probes/arguments unchanged. Raw stdout/stderr, PID, timestamps, exits and timeout evidence in target/ctrl-103/matrix.csv and accompanying files. Java 25.0.1 / FX25, Windows PowerShell 5.1 and Maven 3.9.16. No opt-in Robot or unresolved CTRL-014/034/CAL-TIME-001. Default is the no-switch invocation. Local correctness is limited to tested paths; corporate SQL/transactions, real TaskContext/currency lookup, actual native input/focus and MDI/WAIT remain unverified. Stop after CTRL-103.

Final matrix. Mode, probe marker, wall seconds, JVM exit, outer PowerShell exit, timeout.

    TableAdapterSearchMissDelivery | CTRL-098 PASS | 14.318 | 0 | 0 | False
    CurrencyLovDelayedCompletion | CTRL-091 PASS | 11.757 | 0 | 0 | False
    TableAdapterBulkWorkerClose | CTRL-092 PASS | 15.359 | 0 | 0 | False
    TableAdapterSearchWorkerClose | CTRL-093 PASS | 15.098 | 0 | 0 | False
    TableAdapterSearchListener | CTRL-081 PASS | 12.315 | 0 | 0 | False
    TableEditRowIdentity | CTRL-075 PASS | 12.736 | 0 | 0 | False
    TableAdapterBulkClose | CTRL-076 PASS | 11.381 | 0 | 0 | False
    TableAdapterQueuedClose | CTRL-077 PASS | 13.631 | 0 | 0 | False
    TextFieldLovDelayedCompletion | CTRL-078 PASS | 10.804 | 0 | 0 | False
    TableEditing | GUI-005 PASS | 9.827 | 0 | 0 | False
    TableCancel | GUI-006 PASS | 10.385 | 0 | 0 | False
    EditorReentry | GUI-007 PASS | 11.167 | 0 | 0 | False
    DataBinding | GUI-009 PASS | 10.804 | 0 | 0 | False
    TableAdapterClose | CTRL-051 PASS | 10.83 | 0 | 0 | False
    TableAdapterPagingClose | CTRL-065 PASS | 10.665 | 0 | 0 | False
    TextAreaLovDelayedCompletion | CTRL-062 PASS | 10.46 | 0 | 0 | False
    ExternalLovReadOnly | CTRL-061 PASS | 11.373 | 0 | 0 | False
    LovIntegration | CTRL-031 PASS | 10.774 | 0 | 0 | False
    BoundLovSetter | CTRL-044 PASS | 10.447 | 0 | 0 | False
    LovBindingInterleaving | CTRL-045 PASS | 11.142 | 0 | 0 | False
    MoneyDecimal | NUM-001 PASS (tested invariants) | 14.579 | 0 | 0 | False
    FormLifecycle | GUI-010 PASS (standalone production FXML/controller and SDI close) | 12.234 | 0 | 0 | False
    FormCancel | CTRL-022 PASS | 15.382 | 0 | 0 | False
    SingletonLifecycleMatrix | CTRL-035 PASS | 13.112 | 0 | 0 | False
    TextAreaToolbarOwnership | CTRL-066 PASS | 17.628 | 0 | 0 | False
    WrapButtonOwnership | CTRL-055 PASS | 15.3 | 0 | 0 | False
    TabPaneSkinListeners | CTRL-054 PASS | 10.775 | 0 | 0 | False
    TextAreaSkinDisposal | CTRL-053 PASS | 10.492 | 0 | 0 | False
    ValidatorSubchanges | CTRL-052 PASS | 9.762 | 0 | 0 | False
    TextAreaLovLifecycle | CTRL-050 PASS | 10.334 | 0 | 0 | False
    TextFieldSkinDisposal | CTRL-049 PASS | 11.365 | 0 | 0 | False
    WrapButtonDeferred | CTRL-043 PASS | 11.296 | 0 | 0 | False
    Editors | GUI-004 PASS | 15.307 | 0 | 0 | False
    SceneKeyLookup | CTRL-024 PASS | 9.982 | 0 | 0 | False
    LovListenerLifecycle | CTRL-026 PASS | 15.714 | 0 | 0 | False
    LovDefaultValidation | CTRL-027 PASS | 12.388 | 0 | 0 | False
    SingletonCloseVeto | CTRL-032 PASS | 13.542 | 0 | 0 | False
    ValidatorScopedUnbind | CTRL-033 PASS | 10.304 | 0 | 0 | False
    ValidatorMultiControl | CTRL-036 PASS | 10.251 | 0 | 0 | False
    TextFields | GUI-012 PASS (tested matrix) | 10.972 | 0 | 0 | False
    ValueFields | VF-001 PASS | 11.078 | 0 | 0 | False
    ValueLifecycle | VF-002 PASS (custom listener ownership) | 10.548 | 0 | 0 | False
    DatePickerBinding | CTRL-002 PASS | 10.196 | 0 | 0 | False
    NodeTraversal | CTRL-003 PASS | 10.255 | 0 | 0 | False
    RadioReadOnly | CTRL-006 PASS | 12.316 | 0 | 0 | False
    ComboReadOnly | CTRL-007 PASS | 17.671 | 0 | 0 | False
    CheckBoxReadOnly | CTRL-009 PASS | 16.899 | 0 | 0 | False
    ValueChangedFlag | CTRL-013 PASS | 8.703 | 0 | 0 | False
    ControlsFollowup | CTRL-CONTROLS-FOLLOWUP PASS | 10.311 | 0 | 0 | False
    ChoiceAdapter | CTRL-015 PASS | 9.786 | 0 | 0 | False
    BinderChoiceNull | CTRL-017 PASS | 10.45 | 0 | 0 | False
    ChoiceNullLookup | CTRL-018 PASS | 10.583 | 0 | 0 | False
    ChoiceNonNullSet | CTRL-019 PASS | 9.991 | 0 | 0 | False
    Default | GUI-001 PASS | 11.338 | 0 | 0 | False


---

## Reply CTRL-104

**DONE.** Baseline df95a931230b57e87e7bef4bfba6267c3e9a395d. Verified production 17cabb0b11b7b414fab811bcd59650f02a483561. Only DSFXAdapter plus focused smoke and runner mode.

Actual shown table, JInvParallelAction and an in-memory XXIDataSet failure after the worker is admitted. Mark and unmark, active/retired/detached/hidden/reparented plus active success. Baseline delivered neither detached sentinel and raised one secondary FX NPE per direction. After the fix every genuine failure reached JInvErrorService exactly once with original Throwable identity, one dialog and zero secondary FX errors, writes or success refresh. Positive successes each mutated one in-memory row and delivered one refresh.

The invocation passes its launch Window through the existing action event path. Existing epoch and captured original Dataset check ownership at actual FX error delivery. Current shown launch window remains the context; otherwise the service receives null. DialogError currently leaves its native dialog owner unset even with a supplied context, so the probe checks the actual ownerless neutral dialog and Throwable identity; active context preservation also follows from the checked source path. The first baseline probe expected a native owner and was corrected before the recorded RED. No per-instance fields; one ActionEvent per admitted bulk invocation. No suppression, transaction or cancellation change.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterBulkMarkErrorDelivery -SettingsFile "D:\Java\.m2\settings.xml". Java 25.0.1/FX25. RED JVM/runner 1/1, three fresh GREEN 0/0, unchanged official exit 0. SearchMissDelivery, BulkWorkerClose, TableAdapterClose, DataBinding and QueuedClose all 0/0. Maven compile, PowerShell parse and diff check passed. Logs in target/ctrl-104; no corporate SQL.


---

## Reply CTRL-105

**DONE.** Baseline 17cabb0b11b7b414fab811bcd59650f02a483561. Verified production 72f92bab82063aa873af9d9086d03022dfe2a7d8. Only DSFXAdapter plus a focused mode extending the real mark worker smoke.

Mark and unmark through JInvParallelAction and real shown table. RED gave one stale full refresh in each of 12 routes: worker queued but not entered, closed during work, closed with successful refresh already queued, detached, hidden and reparented, both directions. GREEN gave zero obsolete refreshes and retained the one original logical write in every route. Repeated active operations each wrote/refreshed once, with savePrev true then false. Empty big table admitted zero operations. Active failure retained original identity and one dialog, zero refresh/writes. CTRL-104 errors remain covered independently.

Reuses launch owner, original Dataset and worker epoch at FX delivery, resets cached actions on close to release stale captured Dataset/owner controller references. An action already submitted is admitted even if its thread has not started; fixture kept that original behavior, without a new cancellation policy. No extra fields, action allocation, per-row state or queued callback compared with CTRL-104. No corporate SQL, rollback or same-table rebind claim.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterBulkMarkClose -SettingsFile "D:\Java\.m2\settings.xml". RED JVM/runner 1/1, three fresh GREEN 0/0 plus unchanged official runner 0. BulkMarkErrorDelivery, BulkWorkerClose, QueuedClose, TableAdapterClose and DataBinding all 0/0. Compile, PowerShell parse and diff check passed. Logs target/ctrl-105.


---

## Reply CTRL-106

**DONE.** Baseline 72f92bab82063aa873af9d9086d03022dfe2a7d8. Verified production 0982d2112b8f8020d16f917adaab77caee29005d. Only DSFXAdapter plus focused smoke and runner.

Shown real JInvTable/adapter and in-memory Dataset. RED observed one obsolete REPLACE for an explicitly queued refresh and one for mark-driven refresh after double close. A listener that closed during a legitimate REPLACE and advanced focus to 1 was overwritten back to 0. GREEN: zero late replacements, two independent marker notifications still observed, focus 1 preserved when listener closed, and the already admitted list event occurred once. Invalid index produced zero events; detached-but-live and offscreen were preserved. Equal-distinct replacement, removal and permutation still refreshed the delivery index X/B/R199. Same/other-row refresh canceled active drafts as before; F8/ENTER committed original A once, Escape and controlled rejection wrote zero, rejection delivered one error. No full refresh or foreign-row writes.

Epoch and origin guard existing FX callback before list access and after the synchronous replacement before focus. No scene requirement for active detached table, no persistent fields, no new queued callback or per-row state. Public index ownership, mark event semantics and transactions unchanged.

Windows rerun: powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\src\test\probes\Test-LegacyGuiSmoke.ps1 -TableAdapterRowRefreshClose -SettingsFile "D:\Java\.m2\settings.xml". RED JVM/runner 1/1, three fresh GREEN 0/0 and unchanged official exit 0. QueuedClose, TableAdapterClose, PagingClose, EditRowIdentity, TableEditing, TableCancel, DataBinding and BulkMarkClose all 0/0. Maven compile, parse and diff passed. Logs target/ctrl-106. Corporate Dataset/SQL behavior remains untested.


---

## Reply CTRL-107

**DONE / READ-ONLY.** Baseline and verified final production 0982d2112b8f8020d16f917adaab77caee29005d.

Installed JInvCommon javap evidence: DataSetMarkEvent.getRow returns the exact row supplied with MARK_ROW/UNMARK_ROW and getIndex returns the original position; MARK_ALL/UNMARK_ALL/REFRESH carry null row and index -1. XXIDataSet.doMark mutates the actual IMarkable and then fires the event with that object. Adapter markAction forwards only the index, unlike the original event identity. No SQL invoked.

Shown in-memory table: independent observer received one event with source Dataset and exact A at index 0. Stable delivery replaced A→A and rendered checked/pseudoclass on A. Equal-distinct replacement and removal made the deferred visual event replace X→X and B→B without marking either. Permuting A from 0 to 1 made the delayed refresh operate on B, although the permutation itself let A display checked/pseudoclass at 1. Selection, focus and cursor followed A to 1. Offscreen R150 was marked and refreshed by original slot with visible rows unchanged. Zero commits or text/model writes and no foreign mark changes. This confirms identity loss in the rendering notification, distinct from database corruption.

Suggested smallest follow-up: separate entity-owned single-marker delivery from public index-based refreshRow, verify source and row identity at the original index, and define behavior when reorder moved the row before considering any lookup or broader visual fallback. Prove supported sort/filter/update routes already render moved rows before suppressing such a delivery. Bulk remains index -1. No scan/cache by assumption.

Ignored target/ctrl-107/MarkIdentityAudit.java, mark-bytecode.txt and raw streams. javac -cp "target/ctrl-088;target/classes;<deps>" -d target/ctrl-107 then java --enable-native-access=ALL-UNNAMED -cp "target/ctrl-107;target/ctrl-088;target/classes;<deps>" MarkIdentityAudit. Deps from target/ctrl-079a/original-dependencies.txt. Native compile/JVM 0, no probe runner; no tracked edits or state/allocation changes.


---

## Reply PERF-001

**DONE / READ-ONLY.** Baseline and verified final production 0982d2112b8f8020d16f917adaab77caee29005d. No tracked edits.

Matched shown VBox/Scene and stylesheet, same text/font/width, explicit stock TextFieldSkin installation through public API before attachment. JInvTextField includes eager skin in its constructor. Stock bare constructor is a diagnostic phase only; comparable totals run through the same attachment, CSS, layout and first observable post-layout. Stock and framework differ functionally; this is not a feature-equivalence claim. Early framework skin, editing, subsequent in-memory LOV notification and readOnly passed. Immediate wrapper/menu semantics are further checked in PERF-003.

Fresh separate Java 25.0.1 / FX25+29 JVMs, both -Xms256m -Xmx1024m -XX:+UseG1GC, FX ThreadMXBean deltas, one first-use sample and five warmups/nine samples per N. Timings include public post-layout barrier; the explicitly timed CSS/layout phases also run before the pulse, no paint claim. Raw absolute values below by batch (µs and FX bytes, median/range). Control graphs released between samples; one Stage reused per process.

    kind=stock jdk=25.0.1+8-LTS-27 fx=25+29 heap=1073741824
    cold N=1 phase=constructor us=99337(99337..99337) FXbytes=3513064(3513064..3513064)
    cold N=1 phase=stock-explicit-skin us=251160(251160..251160) FXbytes=7689640(7689640..7689640)
    cold N=1 phase=setup us=12554(12554..12554) FXbytes=404184(404184..404184)
    cold N=1 phase=attach us=4839(4839..4839) FXbytes=139984(139984..139984)
    cold N=1 phase=applyCss us=15120(15120..15120) FXbytes=372792(372792..372792)
    cold N=1 phase=layout us=3730(3730..3730) FXbytes=35224(35224..35224)
    cold N=1 phase=post-layout us=17485(17485..17485) FXbytes=519080(519080..519080)
    cold N=1 phase=total us=404299(404299..404299) FXbytes=12674352(12674352..12674352)
    warm N=1 phase=constructor us=118(88..150) FXbytes=4008(4008..4008)
    warm N=1 phase=stock-explicit-skin us=1778(1508..2360) FXbytes=114952(114952..115096)
    warm N=1 phase=setup us=299(267..384) FXbytes=6832(6832..6832)
    warm N=1 phase=attach us=183(145..278) FXbytes=2664(2664..2664)
    warm N=1 phase=applyCss us=514(463..670) FXbytes=12112(12112..12112)
    warm N=1 phase=layout us=172(136..259) FXbytes=2104(2104..2808)
    warm N=1 phase=post-layout us=5079(4117..6097) FXbytes=11840(11840..11904)
    warm N=1 phase=total us=8326(6908..9375) FXbytes=154896(154896..155600)
    warm N=20 phase=constructor us=505(280..640) FXbytes=79552(79552..79552)
    warm N=20 phase=stock-explicit-skin us=22643(14377..25394) FXbytes=2173664(2173216..2175672)
    warm N=20 phase=setup us=1382(1075..2820) FXbytes=136032(128352..136992)
    warm N=20 phase=attach us=659(530..779) FXbytes=47160(47160..47160)
    warm N=20 phase=applyCss us=2835(2293..3687) FXbytes=236080(233200..236808)
    warm N=20 phase=layout us=831(504..1462) FXbytes=40136(38696..43600)
    warm N=20 phase=post-layout us=2577(1465..3773) FXbytes=98936(97856..99072)
    warm N=20 phase=total us=31783(21142..36139) FXbytes=2812560(2798416..2816312)
    warm N=100 phase=constructor us=648(327..1530) FXbytes=397632(397632..397632)
    warm N=100 phase=stock-explicit-skin us=36181(25251..66143) FXbytes=10748008(10745608..10752808)
    warm N=100 phase=setup us=3199(1593..5850) FXbytes=617632(617632..641632)
    warm N=100 phase=attach us=1743(830..2648) FXbytes=234160(234160..243472)
    warm N=100 phase=applyCss us=8759(5482..12015) FXbytes=1129840(1129488..1164720)
    warm N=100 phase=layout us=2242(1327..4530) FXbytes=191032(191032..193432)
    warm N=100 phase=post-layout us=5706(4693..18931) FXbytes=460736(460480..460864)
    warm N=100 phase=total us=66469(43280..95415) FXbytes=13779328(13776416..13845504)
    warm N=400 phase=constructor us=1131(599..21461) FXbytes=1590432(1590432..1604240)
    warm N=400 phase=stock-explicit-skin us=87955(51358..149787) FXbytes=42672656(42576008..42880008)
    warm N=400 phase=setup us=5339(3262..6855) FXbytes=2393632(2393632..2393696)
    warm N=400 phase=attach us=2737(2333..4667) FXbytes=934192(934192..934192)
    warm N=400 phase=applyCss us=13173(10763..15164) FXbytes=4440256(4427456..4478656)
    warm N=400 phase=layout us=4019(3150..5437) FXbytes=742792(707592..752536)
    warm N=400 phase=post-layout us=9298(7542..12355) FXbytes=1676960(1667328..1821056)
    warm N=400 phase=total us=125233(83829..190998) FXbytes=54438864(54297024..54850752)
    kind=framework jdk=25.0.1+8-LTS-27 fx=25+29 heap=1073741824
    cold N=1 phase=constructor us=562897(562897..562897) FXbytes=16391128(16391128..16391128)
    cold N=1 phase=stock-explicit-skin us=1(1..1) FXbytes=32(32..32)
    cold N=1 phase=setup us=15874(15874..15874) FXbytes=474936(474936..474936)
    cold N=1 phase=attach us=11035(11035..11035) FXbytes=349104(349104..349104)
    cold N=1 phase=applyCss us=97893(97893..97893) FXbytes=3284328(3284328..3284328)
    cold N=1 phase=layout us=17339(17339..17339) FXbytes=1368064(1368064..1368064)
    cold N=1 phase=post-layout us=22170(22170..22170) FXbytes=584536(584536..584536)
    cold N=1 phase=total us=727268(727268..727268) FXbytes=22452512(22452512..22452512)
    warm N=1 phase=constructor us=7510(5875..10068) FXbytes=239400(239400..239928)
    warm N=1 phase=stock-explicit-skin us=1(0..2) FXbytes=32(32..32)
    warm N=1 phase=setup us=740(600..1379) FXbytes=11624(11624..11624)
    warm N=1 phase=attach us=396(271..1083) FXbytes=11832(11832..12624)
    warm N=1 phase=applyCss us=4310(3996..7034) FXbytes=123520(123520..124896)
    warm N=1 phase=layout us=1544(702..1724) FXbytes=13520(13520..13832)
    warm N=1 phase=post-layout us=3483(2702..5395) FXbytes=31392(31392..31824)
    warm N=1 phase=total us=18644(14850..26414) FXbytes=432016(431704..433080)
    warm N=20 phase=constructor us=45602(34387..83875) FXbytes=4696808(4694176..4707080)
    warm N=20 phase=stock-explicit-skin us=0(0..0) FXbytes=32(32..32)
    warm N=20 phase=setup us=2176(901..4981) FXbytes=231872(227712..231872)
    warm N=20 phase=attach us=1091(580..1742) FXbytes=230520(230520..230520)
    warm N=20 phase=applyCss us=26642(14128..59362) FXbytes=2351552(2348096..2353536)
    warm N=20 phase=layout us=4526(3589..5884) FXbytes=268456(266536..269168)
    warm N=20 phase=post-layout us=24446(19145..34441) FXbytes=245664(245184..536072)
    warm N=20 phase=total us=111577(75544..158976) FXbytes=8025288(8013368..8317632)
    warm N=100 phase=constructor us=79874(55660..168952) FXbytes=23247112(23220008..23355536)
    warm N=100 phase=stock-explicit-skin us=0(0..0) FXbytes=32(32..32)
    warm N=100 phase=setup us=2855(1579..5854) FXbytes=1060032(1060032..1060192)
    warm N=100 phase=attach us=3088(1638..4032) FXbytes=1150960(1150928..1159136)
    warm N=100 phase=applyCss us=49504(33228..86701) FXbytes=11673728(11660656..11692656)
    warm N=100 phase=layout us=5462(3926..19511) FXbytes=1295832(1286232..1310232)
    warm N=100 phase=post-layout us=8608(6401..35736) FXbytes=1098992(1032592..1137360)
    warm N=100 phase=total us=142343(118422..273656) FXbytes=39514008(39411256..39707192)
    warm N=400 phase=constructor us=270393(117713..543728) FXbytes=92400392(92400008..92467912)
    warm N=400 phase=stock-explicit-skin us=0(0..0) FXbytes=32(32..32)
    warm N=400 phase=setup us=8229(4444..14838) FXbytes=4224032(4224032..4240032)
    warm N=400 phase=attach us=10177(5818..40020) FXbytes=4588560(4588560..4604960)
    warm N=400 phase=applyCss us=119893(68588..152559) FXbytes=46641856(46641856..46641856)
    warm N=400 phase=layout us=14184(9016..27940) FXbytes=5094792(5094792..5094792)
    warm N=400 phase=post-layout us=21748(14467..31203) FXbytes=4001360(4001360..4001816)
    warm N=400 phase=total us=441552(220071..762329) FXbytes=156951480(156951024..157034960)

At N=400 comparable totals were 54.439 MB / 125.233 ms stock and 156.951 MB / 441.552 ms framework. The ~102.5 MB difference here includes framework features and layout, not just the constructor. Class-level and retained sizes remain distinct. This is the selected fixture for subsequent tasks.

Compile javac -cp "target/classes;<deps>" -d target/perf-001 target/perf-001/FieldCostAudit.java. Run java -Xms256m -Xmx1024m -XX:+UseG1GC --enable-native-access=ALL-UNNAMED -cp "target/perf-001;target/classes;<deps>" FieldCostAudit stock, then repeat with framework. Deps from target/ctrl-079a/original-dependencies.txt. Initial fixture compile lacked AbstractLovBase.showChoiceList override; corrected in ignored fixture. Final compile and both native exits 0, no probe runner. No production state/allocation delta.


---

## Reply PERF-002

**DONE / READ-ONLY.** Baseline and verified final production 0982d2112b8f8020d16f917adaab77caee29005d. No tracked edits.

Exact FX ThreadMXBean attribution uses an observed subclass overriding only protected createDefaultSkin, delegating to the real unchanged factory and timing its nested call. The enclosing constructor still installs the skin and does all ordinary setup. The nested measure is creation, not subsequent setSkin install; the residual includes superclass, framework properties/RequiredState, bindings, keyboard, edit/popup configuration and installation. At N=400 nested creation was 75.45 MB of 92.23 MB exact constructor allocation (about 82 percent), consistent with the ordinary constructor's 92.40 MB baseline in PERF-001. Timing instrumentation is only attribution data. No subclass without skin is presented as equivalent.

Separate actual FieldCostAudit framework JVM with local JFR profile, same heap settings, N=400, includes first-use and warmed iterations. JDK reports the FX event thread as WindowsNativeRunloopThread. Filter 1532 jdk.ObjectAllocationSample events on that thread with JInvTextField constructor in stack. Estimated weights by allocated class and top four frames below. 862.72 MB nested skin-stack weight of 1256.20 MB total sampled constructor weight is an estimate for the whole run, not an exact attribution or retained amount. CSS selector/iterator and handler registration dominate these sampled stacks; CSS work is already triggered by construction even before explicit applyCss. Exact nesting and JFR estimates answer different questions. First-use kept separate for exact bench but included in sampled weights.

    n=20 constructor B=4696800(4686632..4707288) skin-creation B=3844480(3837192..3854968) constructor us=42640(34862..61603) skin-creation us=34163(28077..51461)
    n=100 constructor B=23199136(23174360..23251808) skin-creation B=18986064(18967736..19024376) constructor us=57052(47916..86763) skin-creation us=46688(39419..74921)
    n=400 constructor B=92227808(92169600..92324864) skin-creation B=75450104(75392000..75537664) constructor us=134572(119056..202325) skin-creation us=109062(98144..165665)
    constructor samples=1532 estimated weights sum=1256203240 nested skin constructor weight=862721736
    198751312 com.sun.javafx.css.FixedCapacitySet$Single$1
    133862576 [Ljava.lang.Object;
    77562128 java.util.HashMap$Node
    68073200 java.util.ArrayList
    53999680 java.util.AbstractList$ListItr
    198751312 [com.sun.javafx.css.FixedCapacitySet$Single.iterator, java.util.AbstractCollection.containsAll, com.sun.javafx.css.SelectorPartitioning.match, com.sun.javafx.css.StyleManager.findMatchingStyles]
    70150792 [java.util.HashMap.newNode, java.util.HashMap.putVal, java.util.HashMap.put, com.sun.javafx.event.EventHandlerManager.createGetCompositeEventHandler]
    53999680 [java.util.AbstractList.listIterator, java.util.AbstractList$SubList$1.<init>, java.util.AbstractList$SubList.listIterator, java.util.AbstractList.listIterator]
    53752352 [java.util.ArrayList.grow, java.util.ArrayList.addAll, com.sun.javafx.css.StyleManager.findMatchingStyles, javafx.scene.CssStyleHelper.createStyleHelper]
    43814568 [java.lang.invoke.DirectMethodHandle.allocateInstance, java.lang.invoke.DirectMethodHandle$Holder.newInvokeSpecial, java.lang.invoke.Invokers$Holder.linkToTargetMethod, javafx.scene.layout.Region.<init>]

Reproduce with javac -cp "target/perf-001;target/classes;<deps>" -d target/perf-002 target/perf-002/ConstructorAttribution.java. With the PERF-001 JVM flags and classpath adding target/perf-002, run ConstructorAttribution for exact nesting, then run FieldCostAudit framework 400 with -XX:StartFlightRecording=filename=target/perf-002/framework.jfr,settings=profile,dumponexit=true and decode with ConstructorAttribution target/perf-002/framework.jfr. Compilation, profiling and decoder native exits 0. Earlier decoder thread naming returned zero and was corrected from actual JFR metadata; logs preserved. Raw profile stays local. No production allocation delta or heap inference.
