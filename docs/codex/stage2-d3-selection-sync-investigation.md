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
