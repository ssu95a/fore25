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
