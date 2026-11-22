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
