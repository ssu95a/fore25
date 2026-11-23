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
