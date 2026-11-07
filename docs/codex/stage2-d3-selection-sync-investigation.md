# Stage 2 — D3: TableView selection ↔ Dataset current row

## Контекст и итог

- Repository: `ssu95a/fore25`.
- Branch: `migration/java25-correctness`.
- Исследованный HEAD: `16c794b63415b2c151625a0aa6b189377ce4a83a`.
- Режим: read-only investigation + reproduction. После исследования отдельно разрешено сохранить этот отчёт.
- Production files не изменены. D1, D2 и D4–D7 не исправлялись.
- Commit и push не выполнялись.
- Public/protected API должен быть сохранён; новые public API и изменения прикладного кода не предполагаются.

**Итоговый verdict: D3 PROGRAMMATIC SELECTION GAP — UNKNOWN.**

Механизм, поддерживающий гипотезу, найден по коду. Однако обязательное наблюдение состояния реального связанного TableView после programmatic selection не получено из-за ограничения JavaFX runtime. Подтверждённым root cause гипотеза не объявляется.

## Исследованные production files

Основные файлы:

- `src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java` — binding, UI events, navigation, positioning, execute, swapping, loading и search.
- `src/main/java/ru/inversion/dataset/fx/DSControlAdapter.java` — navigation callbacks и обновление связанных controls.
- `src/main/java/ru/inversion/dataset/fx/LiveArrayListEx.java` — доставка list notifications в FX thread.
- `src/main/java/ru/inversion/fx/app/BaseApp.java` — factory ObservableList для Dataset.
- `src/main/java/ru/inversion/fx/form/controls/JInvTable.java` — selection listeners, mouse handling, marking и navigation helpers.
- `src/main/java/ru/inversion/fx/form/controls/skin/JInvTableViewSkin.java` — поиск дополнительной синхронизации.
- `src/main/java/ru/inversion/fx/form/controls/sortbutton/OrderByManager.java` — DB-side sorting.
- `src/main/java/ru/inversion/fx/form/action/JInvKeyboardManager.java` — keyboard routing и navigation actions.
- `src/main/java/ru/inversion/fx/form/controls/JInvLovSearchPane.java` — явная синхронизация search selection.
- `src/main/java/ru/inversion/fx/form/controls/dsbar/Part_RowNum.java` — отображение selected index.
- `src/main/java/ru/inversion/fx/form/controls/table/JInvTableContextMenuFactory.java` — использование selection в table helpers.
- `src/main/java/ru/inversion/fx/form/controls/renderer/JInvTextFieldTableCell.java` — programmatic selection при editing navigation.
- `src/main/java/ru/inversion/fx/form/controls/renderer/JInvTableCellFilter.java` — programmatic selection filter cell.
- `src/main/java/ru/inversion/fx/form/controls/renderer/JInvTableCellMark.java` — programmatic selection при marking.
- `src/main/java/ru/inversion/fx/help/controller/EditTab.java` — использование selected item и Dataset current row.
- `src/main/java/ru/inversion/fx/app/frame/menu/IndicatorsListDialog.java` — programmatic selection; сам по себе этот call site не доказывает наличие DSFXAdapter binding.

Дополнительно проверены доступные классы `JInvCommon` и JavaFX 11 посредством `javap`. Корпоративные dependencies не подменялись и не изменялись.

## A. Intended invariant

**CONFIRMED по коду:** предусмотрено согласование Dataset current row и TableView selection при обычной навигации.

Строгий универсальный invariant «они всегда совпадают после завершения notifications» реализация не устанавливает:

- `bindTable` явно не согласовывает начальные позиции;
- `clearSelection()` используется отдельно от очистки Dataset current row;
- public `setCurRow(...)` подавляет обратное позиционирование таблицы;
- `positionRow` асинхронен.

**HYPOTHESIS о intended contract:** для валидного выделения выбранный объект должен соответствовать текущей записи Dataset. Для отсутствующего выделения единое правило кодом не задано.

## B. Table → Dataset

**CONFIRMED по коду:** `refreshCurrentRow()` вызывают:

- handler `KeyEvent.KEY_RELEASED`;
- filters `MouseEvent.MOUSE_PRESSED`, `MouseEvent.MOUSE_RELEASED`, `MouseEvent.DRAG_DETECTED`.

Метод под guard передаёт `tableView.getSelectionModel().getSelectedIndex()` в `dataSet.setCurrentRowNum(...)`.

| Путь | Установленный механизм | Runtime verdict |
| --- | --- | --- |
| Mouse click | Чтение selection на press/release; press-filter может предшествовать изменению selection | UNKNOWN |
| UP/DOWN | Чтение selection при дошедшем до handler KEY_RELEASED | UNKNOWN |
| PAGE_UP/PAGE_DOWN | Release-path; PAGE_DOWN дополнительно может вызвать swapping и отложенный selectLast | UNKNOWN |
| HOME/END | Адаптер consumes KEY_PRESSED; release-handler читает фактический selection | UNKNOWN |
| Drag | Синхронизация в перечисленных mouse events; непрерывная гарантия не установлена | UNKNOWN |
| Изменение selection model | Общего listener, обновляющего Dataset, не найдено | UNKNOWN для фактического результата |

Физические события, consumption и порядок обработки skin/editing controls не воспроизведены.

## C. Programmatic selection

**CONFIRMED по исследованному коду:** отдельного общего Table → Dataset механизма для `select(index)`, `select(item)`, `selectFirst()`, `selectLast()`, `clearAndSelect()` и `clearSelection()` не найдено.

Проверены соседние механизмы:

- listener `JInvTable.selectedIndexProperty()` меняет только `previousIndex`;
- `Part_RowNum` обновляет отображаемый номер строки;
- дополнительной синхронизации в `JInvTableViewSkin` не найдено;
- `JInvLovSearchPane.selectRow()` явно вызывает и `select(r)`, и `setCurrentRowNum(j)`;
- `rowLoader` после `selectLast()` отдельно вызывает `setCurrentRow(selectedItem)`;
- cell editors, filter cell и mark cell программно вызывают `select(...)`.

**HYPOTHESIS:** изменение selection без перечисленных key/mouse events не вызывает Dataset navigation.

**UNKNOWN:** требуемое наблюдение `table selectedIndex = 2`, `Dataset current row index = 0` на реальном связанном TableView. Поэтому verdict не повышен до CONFIRMED GAP.

## D. Dataset → Table

**CONFIRMED по коду:**

```text
Dataset navigation event
→ DSFXAdapter.navigated()
→ positionRow(newRowIndex), если guard выключен
→ Platform.runLater(...)
→ проверка диапазона
→ scrollTo(...)
→ selectionModel.select(index)
```

`super.navigated()` независимо ставит обновление связанных controls в FX queue.

Другие источники `positionRow`: UPDATE row event, пути executeQuery и завершение загрузки строк. Search использует Dataset `findRow`, меняющий current row.

**UNKNOWN:** итоговое selection после исполнения реальной FX queue.

## E. Loop prevention

**CONFIRMED:** `insideInSetCurrentRow` включается на время синхронного Dataset setter в `refreshCurrentRow()` и `setCurRow()`, затем сбрасывается в `finally`.

Dataset-probe подтвердил синхронность navigation callbacks для доступного ArrayDataSet. Поэтому guard подавляет немедленный возврат через `positionRow()` на этом пути.

Guard не:

- охватывает выполнение отложенного select;
- отменяет уже поставленные задачи;
- подавляет `super.navigated()`;
- обеспечивает межпоточную синхронизацию.

Сейчас отложенный select не имеет найденного обратного listener. Candidate fix с таким listener требует защиты Dataset → Table направления.

## F. Platform.runLater ordering и threading

**CONFIRMED по коду:** каждая задача `positionRow` захватывает индекс. При выполнении проверяется его диапазон, но не актуальность navigation event/current row.

В доступном JavaFX 11 dispatcher использует очередь `addLast`/`takeFirst`: это подтверждено байткодом. FIFO постановки не исключает вмешательства других событий.

**CONFIRMED executable Dataset-probe:** navigation callbacks выполняются на вызывающем потоке, включая worker thread, до возврата setter. В проекте имеются фоновые query/search/load paths. `LiveArrayListEx.fireModification` переносит list notification в FX queue при вызове вне FX thread.

**HYPOTHESIS / риски:**

- несколько navigation events ставят задачи с промежуточными индексами;
- UI selection может измениться до выполнения старой задачи;
- после reload тот же индекс может обозначать другой объект;
- `rowLoader` использует вложенный runLater.

**UNKNOWN:** реальная последовательность selection callbacks, stale-task reproduction и итог после FX flush. Один barrier не гарантирует завершение задач, которые предыдущие задачи дополнительно поставили после него.

## G. Index correctness

**CONFIRMED по коду:** обычный binding передаёт таблице ObservableList Dataset либо wrapper над тем же списком. BaseApp настраивает Dataset list factory на FX-списки.

Для SQL Dataset `OrderByManager` перехватывает sort event, формирует DB order, запускает query и consumes event.

**INDEX-BASED SYNC RISK — условный:** selectedIndex корректен как Dataset row index при одинаковом порядке items/rows. Запрета на замену TableView.items, custom sort policy или преобразованный список не найдено. Универсальная гарантия одинакового порядка отсутствует.

Конкретного TableView call site с SortedList/FilteredList в исследованных местах не найдено. Реальная ошибка index/object mapping не воспроизведена.

## H. Initial binding state

**CONFIRMED по коду:** в bindTable нет явного начального переноса позиции ни Dataset → Table, ни Table → Dataset.

Source of truth при binding явно не выбран. Dataset current row сохраняется; итог preselected/невыделенного TableView зависит от setItems и selection model.

| Dataset до binding | Явная reconciliation | Фактический TableView результат |
| --- | --- | --- |
| current = 0 | Нет | UNKNOWN |
| current = middle | Нет | UNKNOWN |
| current = last | Нет | UNKNOWN |
| current = -1 / none | Нет | UNKNOWN |
| TableView preselected | Нет | UNKNOWN |
| TableView без selection | Нет | UNKNOWN |

## I. Boundary cases

Результаты настоящего ArrayDataSet из доступного JInvCommon:

| Сценарий | CONFIRMED executable результат |
| --- | --- |
| Заполненный ArrayDataSet сразу после создания | current index = -1, current row = null |
| setCurrentRowNum(2) из 0 | index = 2, row = C; один navigation callback |
| Повторный setter того же индекса | true; без нового callback |
| setCurrentRowNum(-1) / 99 | false; current row не меняется |
| setCurrentRow(null) | false; current row не меняется |
| Удаление выбранной строки, остаются строки | Row DELETE и последующая navigation к оставшейся записи |
| Удаление единственной строки | index = -1, row = null |
| Empty Dataset, setter 0 | false |
| Вставка единственной строки с positioning | INSERT, затем navigation -1 → 0 |

Таким образом, передача selectedIndex = -1 существующему Dataset setter не очищает current row.

Установлено по production code:

- positionRow(-1) ничего не выделяет и явно не очищает selection;
- parallel executeQuery предварительно очищает selection;
- PAGE_DOWN может поставить отдельный selectLast;
- rowLoader явно выполняет последующую Dataset-синхронизацию;
- setItems не сопровождается общей reconciliation.

**UNKNOWN:** TableView selection после clearSelection, invalid index, delete, reload, swapping append и items replacement.

## J. Probes и reproduction

Probes передавались через stdin в JShell JDK 25; файлов probe не создавалось. Production DSFXAdapter не подменялся собственной моделью selection. Полноценный integration probe адаптера не выполнен.

### Реальный TableView

**ENVIRONMENT CONSTRAINT / BLOCKED.** Доступен JavaFX 11, а не corporate JavaFX 25 runtime.

Первая попытка создания настоящего TableView завершилась ошибкой QuantumRenderer. Повторная попытка с доступными native libraries завершилась `Toolkit not initialized`. Полноценного headless runtime не найдено. Runtime не устанавливался, dependencies не скачивались и не подменялись.

### Настоящий Dataset

Использован `ArrayDataSet<String>` из:

```text
D:/Java/.m2/repository/ru/inversion/JInvCommon/1.0.0/JInvCommon-1.0.0.jar
```

Проверены setters, количество/порядок navigation callbacks, поток доставки, удаления, insert и clear. Существенные результаты:

```text
initial populated:
  ds=-1 row=null events=[]

set 0->2 returned:
  ds=2 row=C
  events=[nav:0->2:main]

rapid 2->0->1->2:
  ds=2 row=C
  events=[nav:2->0:main, nav:0->1:main, nav:1->2:main]

worker returned:
  ds=0 row=A
  events=[nav:1->0:D3-worker]

delete final row:
  ds=-1 row=null
  events=[row:DELETE:current=-1, nav:0->-1:main]

one row insert:
  ds=0 row=ONLY
  events=[row:INSERT:current=-1, nav:-1->0:main]

clear:
  ds=-1 row=null
  events=[nav:0->-1:main, row:DELETE:current=-1]
```

### Статус желательной TableView matrix

| Сценарий | Результат |
| --- | --- |
| Dataset navigation 0 → 2 обновляет Table selection | Dataset callback подтверждён; Table selection UNKNOWN |
| Programmatic Table select 0 → 2 | UNKNOWN |
| Keyboard-like selection path | UNKNOWN |
| Mouse-like selection path | UNKNOWN |
| selectLast | UNKNOWN |
| clearSelection | UNKNOWN |
| Empty Dataset + TableView | Dataset boundary подтверждён; TableView UNKNOWN |
| One row + TableView | Dataset boundary подтверждён; TableView UNKNOWN |
| Несколько Dataset navigation до FX flush | Dataset callbacks/order подтверждены; FX selection UNKNOWN |
| Programmatic select после Dataset-driven select | UNKNOWN |

Не получены значения table selectedIndex/selectedItem и реальные FX queue flush результаты для этой matrix. Dataset-only observations не выдаются за TableView reproduction.

## K. Root cause

**CONFIRMED статический факт:** адаптер использует отдельные UI events для Table → Dataset; общей подписки selection → Dataset в исследованном коде не найдено.

**HYPOTHESIS:** selection без этих events не вызывает refreshCurrentRow и оставляет Dataset на старой строке.

**UNKNOWN:** root cause D3 как воспроизведённого correctness bug. До успешного real TableView reproduction гипотеза не считается подтверждённой.

## L. Candidate fix — не реализовано

Оба варианта должны оставаться внутренними для DSFXAdapter, без изменения public/protected signatures, visibility, параметров, return types и требований к прикладному коду.

| Аспект | Вариант 1: selected index listener | Вариант 2: selected item listener → setCurrentRow(item) |
| --- | --- | --- |
| Correctness | При одинаковом порядке items/rows; нужна политика для -1 | Учитывает различие порядка; нужно проверить equality/поиск Dataset и политику null |
| Loop risk | Нужен guard вокруг Dataset-driven select, а не только вокруг Dataset setter | Та же необходимость |
| Threading | Обработка таблицы на FX thread; нужна защита от stale queued positioning | Те же требования |
| Listener lifecycle | Однократная установка при binding; cleanup согласовать с D4 | Те же требования |
| Performance | O(1) для index mapping | Доступный Dataset использует indexOf, то есть O(N) |

**PUBLIC API CONFLICT не выявлен.** Ни один вариант не реализован. Нельзя считать простое добавление listener достаточным исправлением без проверки clear selection, initial state, очереди и directional guard.

## M. Оставшиеся UNKNOWN

- Programmatic selection gap на реальном TableView/DSFXAdapter.
- Initial selection после binding и preselection.
- Физический mouse/keyboard/drag ordering.
- Итог нескольких queued positionRow и stale selection.
- TableView selection после delete, reload, swapping и items replacement.
- Corporate JavaFX 25 и поведение всех corporate Dataset implementations.

## Состояние репозитория

В конце read-only investigation HEAD оставался `16c794b63415b2c151625a0aa6b189377ce4a83a`; working tree был clean. По последующему разрешению пользователя добавляется только этот Markdown-отчёт. Production diff отсутствует. Commit и push не выполняются.

## Follow-up: executable probe для corporate Java 25 / JavaFX 25

### Контекст

- Branch: `migration/java25-correctness`.
- HEAD перед подготовкой probe: `ef8209852ba3c741387c312cf25ad9d2de8e08a8` (включает исходный investigation report).
- **PRODUCTION CODE CHANGES: NONE.** DSFXAdapter, public/protected API и JInvCommon не изменены. D1/D2 и D4–D7 не затронуты.
- Commit и push не выполнялись.

### Созданные probe files

- `src/test/probes/Test-D3SelectionSync.ps1` — Windows/PowerShell runner.
- `src/test/probes/d3/D3SelectionSyncProbe.java` — executable Java probe.

Runner компилирует проект и probe с настоящими Maven dependencies. Не копирует production logic, не создаёт fake DSFXAdapter или SelectionModel и не подменяет corporate dependencies. Служебные файлы помещаются в игнорируемый `target/d3-selection-probe/`.

### Minimal setup

Используются `Platform.startup`, настоящий DSFXAdapter из `target/classes`, JavaFX TableView с его штатной selection model и ArrayDataSet с тремя разными объектами A/B/C. Операции TableView выполняются только на FX Application Thread.

TableView помещён в Scene, привязанную к Stage; Stage не показывается, business UI не запускается. У таблицы нет колонок, Dataset не SQL, `enableMark=false`. По исследованному коду это позволяет вызвать настоящий bindTable целиком, завершив bindColumns до metadata/controller/mark setup. Reflection-hacks, subclasses с переопределённым binding и production API для probe не используются.

**HYPOTHESIS о минимальном setup:** этого lifecycle достаточно на корпоративных dependencies. Если реально потребуется BaseApp/controller или упадёт инициализация, probe выдаст UNKNOWN с причиной; setup не будет скрытно обходиться. Binding дополнительно проверяется по adapter.getDataSet/getTable и identity/order items/rows.

### Corporate command

Из корня репозитория с JAVA_HOME на JDK 25 и рабочим корпоративным Maven:

```powershell
.\src\test\probes\Test-D3SelectionSync.ps1
```

Runner выполняет Maven compile с согласованными параметрами и получает classpath тем же Maven:

```powershell
mvn -Pbuild "-Dmaven.buildNumber.skip=true" "-DskipTests" compile dependency:build-classpath "-Dmdep.outputFile=<generated absolute path>" "-DincludeScope=test"
```

Если `mvn` не найден, используется project `mvnw.cmd`. Ручных скачиваний альтернативных JavaFX/JInvCommon нет. javac/java запускаются через argument files, чтобы избежать ограничения длины Windows command line. Probe требует Java 25 и проверяет JavaFX version 25 после toolkit startup.

### Matrix и queue semantics

P2 выполняется первым как строгий control scenario, до вынесения D3 verdict.

| Сценарий | Действие и проверка |
| --- | --- |
| Initial binding | Dataset current=2, TableView без selection; печатаются snapshots до binding и после flush без исправления состояния |
| P2 | Из согласованного 0 выполняется Dataset setter 2; после flush строго проверяются оба индекса=2 и identity обоих объектов=C |
| P1 | Из согласованного 0 выполняется table.select(2) без mouse/key events; проверяются оба индекса и объекты |
| P3 | Из согласованного 0 выполняется selectLast; Dataset state выводится без навязывания ожидаемой позиции |
| P4 | Из согласованного 1 выполняется clearSelection; выводятся index/item/current row без навязывания Dataset semantics |
| P5 | Dataset setters 0,1,2 выполняются в одной FX task, до исполнения queued positionRow; финальный mismatch выводится отдельно |

Baseline задаётся явно только для изоляции P1–P5. Наблюдение initial binding производится до этой подготовки.

`onFx` использует FutureTask + Platform.runLater + timeout, а не Thread.sleep. Flush — четыре последовательных barrier cycles; каждый ставится после завершения предыдущего. Они покрывают известную ограниченную глубину вложенных runLater в изолированном setup. Это не детектор глобального idle произвольного business UI. Пока выполняется P5 task, queued FX tasks не могут исполняться на том же FX thread.

Trace содержит BEFORE/IMMEDIATE/AFTER snapshots с table index/item, Dataset index/row и накопленными NAV/SEL/INPUT counters. Navigation и selection callbacks печатают порядок и thread name. Неперехваченная ошибка FX task делает probe INVALID.

### Output и exit code

- P2 FAIL → overall INVALID → D3 UNKNOWN → non-zero exit.
- P1 table=2/C, Dataset=0/A → `D3 PROGRAMMATIC SELECTION GAP — CONFIRMED`.
- P1 оба=2/C → `D3 PROGRAMMATIC SELECTION GAP — REJECTED`.
- Неожиданное состояние, ошибка toolkit/binding/compilation или другая техническая ошибка → UNKNOWN, non-zero.
- Технически завершённый probe с определённым CONFIRMED/REJECTED verdict → exit 0. Подтверждение bug само по себе не является ошибкой запуска.
- P5 выводит PASS либо STALE_OR_MISMATCHED_SELECTION; последнее является наблюдением, не автоматически технической невалидностью P1.

### Локальная проверка и ограничения

**CONFIRMED:** PowerShell parser успешно разбирает runner; `-ValidateOnly` успешно разбирает Java source JDK 25 parser через JavacTask.parse без stubs и corporate dependencies. Это проверка синтаксиса/структуры, а не полноценная type-check/compile Java probe.

```powershell
.\src\test\probes\Test-D3SelectionSync.ps1 -ValidateOnly
```

**UNKNOWN / ENVIRONMENT CONSTRAINT:** полноценные corporate compile, минимальный настоящий binding, JavaFX 25 startup и результаты P1–P5 локально не проверены. Локальная syntax validation не меняет прежний D3 verdict UNKNOWN. Runner выполняет полную type-check/compile и runtime reproduction на корпоративной машине, прежде чем сообщать VALID verdict.

`git diff --check` пройден. Изменения ограничены двумя probe files и этим отчётом; production diff отсутствует. `git status` показывает изменённый report и новые probe files. Commit и push не выполнялись.

### Повторная проверка executable deliverable

Подтверждено наличие обоих probe files в рабочем дереве. Запуск runner без `-ValidateOnly` действительно выполняет Maven compile, компиляцию отдельного Java probe и его main, запускающий toolkit и P1–P5. В output явно печатаются `MODE=CORPORATE_EXECUTION`, вход/возврат настоящего `bindTable` и `SCENARIOS COMPLETED=P1,P2,P3,P4,P5,INITIAL_BINDING` при технически успешном завершении. Это executable probe, а не только обновление отчёта.

Повторно проверены PowerShell parsing, Java 25 syntax parsing и whitespace новых probe files. Corporate type-check и runtime результаты по-прежнему UNKNOWN до запуска на корпоративной машине. Конкретная невозможность минимального bindTable не установлена: код использует настоящий binding и при его ошибке выводит stack trace, UNKNOWN и non-zero, без fallback на fake/mock. Production code/API и JInvCommon не изменены; commit/push не выполнялись.

## D3 final corporate verification

### Контекст и источник evidence

- Repository: `ssu95a/fore25`.
- Branch: `migration/java25-correctness`.
- Accepted production fix commit: `475b24f935aa0f38e1ed6993809ec18d71f4ac5f` — `fix: synchronize table selection with dataset row`.
- Пользователь предоставил результаты повторного запуска probe на корпоративной машине: Java 25.0.1, JavaFX 25, production DSFXAdapter, настоящий TableView и ArrayDataSet.
- Результаты ниже являются corporate runtime evidence пользователя, а не локальной симуляцией. Исторические UNKNOWN предыдущих разделов отражают состояние на момент первоначального investigation; для проверенной P1–P5 matrix они разрешены этим запуском.

### CONFIRMED: defect до fix

Контрольный Dataset → Table путь работал (P2 PASS), но programmatic `table.select(2)` оставлял Dataset на row 0 при Table selection 2 (P1 CONFIRMED_GAP). `selectLast()` воспроизводил то же расхождение: table.index=2, dataset.index=0.

Root cause: DSFXAdapter не имел общего listener на изменение selection. Table → Dataset synchronization зависела от mouse/key events, которые programmatic selection не генерирует.

### Accepted fix и compatibility

В `src/main/java/ru/inversion/dataset/fx/DSFXAdapter.java` добавлен listener `selectedIndexProperty`, обрабатывающий только валидный индекс через существующий `refreshCurrentRow()` и прежний index-based Dataset setter. Private directional guard `insideInSetTableSelection` установлен непосредственно вокруг Dataset-driven `select(index)` в queued `positionRow`, чтобы изменение Table selection не инициировало обратный Dataset setter. Контракт существующего `insideInSetCurrentRow` и legacy mouse/key handlers сохранён.

Public/protected API не изменён; изменений прикладного кода не требуется. Cleanup/rebinding listeners не расширялся до D4.

### CONFIRMED: corporate results после fix

| Сценарий | Результат |
| --- | --- |
| P2 Dataset → Table | PASS |
| P1 programmatic select(2) | REJECTED_GAP: Table и Dataset согласованы на row 2 |
| P1 additional methods | PASS: select(item), selectFirst, clearAndSelect |
| P3 selectLast | PASS |
| P4 clearSelection | PASS: Dataset остаётся на B |
| P5 rapid Dataset navigation | PASS; точный trace NAV=[0->1, 1->2] |

Итоговый output предоставленного запуска:

```text
P2 RESULT=PASS
P1 RESULT=REJECTED_GAP
P1 ADDITIONAL METHODS=PASS
P3 RESULT=PASS
P4 RESULT=PASS (Dataset remains on B)
P5 RESULT=PASS NAV=[0->1, 1->2]
SCENARIOS COMPLETED=P1,P2,P3,P4,P5,INITIAL_BINDING
D3 PROGRAMMATIC SELECTION GAP — REJECTED
PROBE STATUS=VALID EXIT=0
```

Точный P5 trace содержит только `0->1` и `1->2`. Дополнительных reverse navigation callbacks `2->1` / `1->2` нет: отсутствие feedback loop в этом сценарии подтверждено runtime probe.

### Сохранённые semantics и границы

Initial binding не изменён: после binding Table selectedIndex=-1, Dataset current index=2. Это отдельно подтверждено corporate запуском и остаётся отдельным вопросом, вне D3 fix.

`clearSelection()` сохраняет прежнее observable behavior: Table index=-1 и item=null; Dataset index=1 и row=B. Новая semantics отсутствующей current row не вводилась.

Закрытие относится к проверенному D3 contract валидного selection при прежнем index mapping. Оно не означает устранения всех возможных stale-queue/interleaving рисков или проверки всех Dataset implementations. D4–D7 не исправлялись; D4 не начат этой документационной задачей.

### Итог

**D3 — CLOSED**

```text
PUBLIC API CHANGES: NONE
JInvCommon CHANGES: NONE
D4-D7 CHANGES: NONE
```

В рамках final verification update изменён только этот Markdown-отчёт. Production и probe code не изменены. Push не выполнялся.
