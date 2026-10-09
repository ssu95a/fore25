# LOV Oracle Forms → Fore / JavaFX

Каталог для проектирования декларативных LOV. Исследованы сам объект LOV, его колонки, связанное поле, record group, программные операции и настройки окружения. Поведение прототипа описано в [lxml.md](lxml.md), запуск — в [lov-prototype/README.md](../lov-prototype/README.md).

## Источники и границы полноты

1. [Oracle Form Builder Reference, Release 6i, A73074_01](https://download.oracle.com/otn_hosted_doc/forms/forms/A73074_01.pdf) — подробные определения классических свойств и built-in. Далее номера **физических страниц PDF**, начиная с 1: печатная нумерация внутри многотомного файла повторяется.
2. [Oracle9i Forms Upgrade Reference](https://www.oracle.com/technetwork/topics/forms-upgrade-reference-128184.pdf), раздел Obsolete Properties — удалённые свойства старых LOV.
3. [Oracle E-Business Suite Developer’s Guide: LOVs](https://docs.oracle.com/cd/E18727_01/doc.121/e12897/T302934T456928.htm) — практические ограничения и правила использования. Это стандарты EBS, а не дополнительные свойства ядра Forms.
4. [Oracle Forms 14.1.2 New Features](https://www.oracle.com/europe/a/otn/docs/oracleforms-1412-newfeatures.pdf), страницы 6–7, 12–13 — новые средства вызова и оформления LOV.
5. [Oracle Forms 14.1.2: Environment Variables](https://docs.oracle.com/en/middleware/developer-tools/forms/14.1.2/working-forms/environment-variables.html) — настройки выборки и фонового запроса.
6. [Oracle APEX: About Oracle Forms Components](https://docs.oracle.com/database/apex-18.1/AEMIG/about-Oracl-forms-components.htm) — границы объектов record group, property class, visual attributes при миграции Forms.
7. [JavaFX 25: Dialog](https://openjfx.io/javadoc/25/javafx.controls/javafx/scene/control/Dialog.html) — модальность, владелец и неблокирующий `show()`.

**Версионная оговорка:** это каталог классической модели с проверенными дополнениями 14.1.2, а не заверенный экспорт Property Palette конкретной установленной версии Forms. Полное совпадение всех доступных свойств конкретного релиза требует сверки с его Form Builder Help/JDAPI; сам обзор 14.1.2 прямо указывает, что не перечисляет абсолютно все изменения. Неподтверждённые свойства не выдаются ниже за свойства LOV.

Обозначения реализации: **готово** — реализовано; **адаптация** — есть функциональный эквивалент с отличиями; **позже** — решение определено, но не включено в прототип; **миграция** — отдельный runtime-флаг не нужен.

## Объект LOV

Классические определения: [1], страницы 579–587, 614–616, 981, 1027; общие метаданные — одноимённые разделы Reference.

| Свойство Forms | Представление в Fore | Статус |
|---|---|---|
| Name | `lov/@id`, идентификатор определения | Готово |
| Comments | `<comment>` для прикладного описания | Готово |
| Subclass Information | Общие определения можно повторно загружать; наследование XML-шаблонов потребует отдельного механизма | Позже |
| Property Class — общий механизм наследования | Композиция неизменяемых `LovDefinition`; декларативного `extends` пока нет | Позже |
| Record Group | `<source>`: статические значения, зарегистрированный provider или SQL | Адаптация |
| Column Mapping Properties | Упорядоченные `<columns>` и `return-to` | Готово |
| Filter Before Display | `behavior/@filter-before-display`: при пустом начальном условии запрос ждёт команды поиска | Адаптация |
| Automatic Display | `behavior/@auto-display`: `LovBinding` открывает список при входе в поле | Готово |
| Automatic Refresh | `behavior/@auto-refresh`: перечитывание либо кэш одной выборки | Адаптация |
| Automatic Select | `behavior/@auto-select`: подтверждение единственного результата только при `complete=true` | Адаптация |
| Automatic Skip | `behavior/@auto-skip`: прикладной `nextFocus` после подтверждённого выбора | Адаптация |
| Title | `lov/@title`; также `%ключ` из переданного `ResourceBundle` | Готово |
| X Position | `window/@x` | Адаптация |
| Y Position | `window/@y` | Адаптация |
| Width | `window/@width` | Адаптация |
| Height | `window/@height` | Адаптация |
| Automatic Position | `window/@automatic-position`: возле вызывающего поля, в пределах монитора | Готово |
| Automatic Column Width | `window/@automatic-column-width`: ширина заголовка с отступами, без просмотра значений строк | Готово |
| Direction — общий механизм интернационализации | `window/@direction` → `NodeOrientation`; в таблице 6i поддержка LOV отмечена «for future use» | Адаптация |

Для классических логических настроек по умолчанию `Automatic Refresh = Yes`; остальные перечисленные `Automatic…`, а также `Filter Before Display = No`. В LXML это сохранено. Размер 640 × 420 и отсутствие координат — собственные значения Fore. Геометрия LXML измеряется в логических пикселях JavaFX; единицы Forms нельзя механически копировать.

## Колонки и возврат значений

Источник: [1], Column Mapping Properties, страницы 614–616.

| Свойство Forms | Представление в Fore | Статус |
|---|---|---|
| Column Name | `column/@name`, точное имя/SQL alias | Готово |
| Title / Column Title | `column/@title`, допускает `%ключ` | Готово |
| Display Width / Column Width | `column/@width`; `0` скрывает колонку, сохраняя её участие в возврате | Готово |
| Return Item | `column/@return-to` → явно зарегистрированная JavaFX `Property` | Адаптация |
| Порядок колонок | Порядок элементов `<column>`; пользовательская перестановка выключена | Готово |

`return-to` — имя получателя, а не выражение, рефлексия или строка Java-кода. Можно регистрировать имена `block.item`, `PARAMETER.name`, `GLOBAL.name`, но области жизни соответствующих значений определяет приложение. Один выбор заполняет несколько получателей, включая скрытый идентификатор.

Типизированная подготовка всех возвращаемых значений выполняется до первого присваивания. Уведомления отдельных JavaFX properties последовательны; транзакции над побочными эффектами слушателей прототип не обещает.

## Связанное поле и форма

Классические свойства: [1], страницы 855–856, 1090. Новое свойство кнопки: [4], страницы 6–7.

| Свойство / настройка Forms | Решение для Fore | Статус |
|---|---|---|
| List of Values, runtime `LOV_NAME` | `LovBinding` связывает `TextField`/`ForeTextField` с экземпляром `ForeLov` | Готово |
| Validate from List; старое наименование Use LOV for Validation | `behavior/@validate-from-list` включает проверку при потере фокуса; `validate()` служит барьером перед сохранением | Адаптация |
| List X Position, `LOV_X_POS` | Координаты текущего определения; отдельное переопределение на уровне привязки пока отсутствует | Позже |
| List Y Position, `LOV_Y_POS` | Аналогично; `automatic-position` учитывает конкретное поле | Позже |
| LOV Button, Forms 14.1.2 | Кнопка рядом с полем вызывает `binding.show()`; показ внутри skin поля и XML-флаг пока отсутствуют | Адаптация |
| Validation, Validation Unit | Ответственность формы; перед её сохранением надо дождаться всех необходимых `validate()` | Адаптация |
| Interaction Mode | Запросы LOV всегда асинхронны; отдельного синхронного режима нет | Адаптация |
| Coordinate System, Real Unit, Font Scaling | Геометрия JavaFX и масштабирование ОС; миграция координат при переносе формы | Миграция |
| Required, Data Type, Format Mask, Maximum Length, Case Restriction | Остаются контрактом поля/валидации; не подменяются свойствами LOV | Вне LOV |

Для валидации используется первая **видимая** колонка. В прототипе она обязана возвращаться в привязанное поле. Точное единственное совпадение заполняет получателей без окна; неоднозначность или отсутствие точного совпадения переводят пользователя к выбору. Требование уникальности — осознанное усиление контракта Fore.

## Record group

Источники: [1], Column Specifications, страницы 617–618, Record Group, страницы 981–984, CREATE_GROUP_FROM_QUERY; [5] — современный вычисляемый fetch size; [6] — роль record group при миграции.

| Свойство / характеристика Forms | Решение для Fore | Статус |
|---|---|---|
| Name, Comments, Subclass Information | Имя зарегистрированного источника и комментарий LOV; отдельной модели наследования record group нет | Адаптация |
| Record Group Type | `<static>`, `<sql>`; программно наполняемый источник — `<provider>` | Готово |
| Record Group Query | `<sql><query>` с JDBC-параметрами `?` | Готово |
| Record Group Fetch Size | `sql/@fetch-size` → `PreparedStatement.setFetchSize` | Адаптация |
| Column Specifications: Name | Имена `<column>` / SQL aliases | Готово |
| Column Specifications: Data Type | `string`, `long`, `decimal`, `date`, `datetime`, `boolean` | Адаптация |
| Column Specifications: Length | Ограничения схемы БД и валидаторов; декларативный предел длины ячейки пока не реализован | Позже |
| Column Specifications: Value | `<static><row><value column="…">…` | Готово |
| FORM_SCOPE / GLOBAL_SCOPE — параметр создания | Область жизни реестра задаёт приложение; глобального изменяемого кэша строк нет | Адаптация |
| Программное наполнение, изменение, выделение строк | Provider отвечает за свой источник; GUI получает неизменяемую выборку для одиночного выбора | Адаптация |

`fetch-size` — подсказка драйверу о порции чтения, **не** размер списка на экране. Для Fore отдельно введён `max-rows`. JDBC читает дополнительную строку, чтобы отличить полную выборку от усечённой; эта строка не материализуется в `LovRow`.

Для Oracle `NUMBER` выбирают `decimal` либо целочисленный `long` по смыслу данных. Для Oracle `DATE`, содержащего время, нужен `datetime`: `date` в LXML означает именно `LocalDate`. Часовой пояс и NLS-форматирование являются отдельным контрактом приложения.

## Оформление и окружение

Общие visual attributes и property classes описаны в [6]. Не следует без проверки приписывать LOV всю палитру свойств обычного text item. В частности, у runtime-свойств `Font_Name`, `Background_Color` и подобных в [1] перечислены другие типы объектов. Fore оформляет составное окно собственными CSS-правилами.

| Настройка | Решение для Fore |
|---|---|
| Visual Attribute Group; Font Name, Size, Weight, Style, Spacing; цвета и узоры — общая тема | `window/@style-class`, CSS сцены владельца, правила `.fore-lov`; эквиваленты выбираются по возможностям JavaFX CSS |
| `default.lovRowLine.color`, Forms 14.1.2 [4] | Цвет границы `.fore-lov .table-row-cell` в CSS приложения |
| `FORMS_COMPUTED_RGFS_DIVIDEND`, `FORMS_MIN_COMPUTED_RGFS`, `FORMS_MAX_COMPUTED_RGFS` [5] | Вычисляемую настройку Forms не переносим; в прототипе положительный JDBC `fetch-size` |
| `FORMS_LOV_INITIAL`, `FORMS_LOV_MINIMUM`, `FORMS_LOV_WEIGHT`, `FORMS_NONBLOCKING_SLEEP` [5] | Фоновый исполнитель, индикатор загрузки, отмена запроса и отбрасывание устаревшего ответа; опрос окон Forms не нужен |
| `FORMS_EXTENDED_STRING` [5] | Типы/ограничения JDBC и БД; отдельного флага Fore нет |
| `FORMS_DISABLE_UNPAD_LOV` [5] | В таблице Oracle нет описания семантики; автоматическое соответствие намеренно не назначено |

## Устаревшие свойства

Источник: [2]. Старый `List Type` удалён в Oracle9i; `Old LOV Text` относится к старому способу описания списка. При переносе оба преобразуются в явный источник и колонки. В LXML 1 этих флагов нет. `Automatic Confirm`, встречающееся в тексте описания `Automatic Select` в старом Reference, не вводится как второе независимое свойство Fore.

## Программный API и события

Это операции и события, а не дополнительные XML-свойства. Источник: [1], GET_LOV_PROPERTY, SET_LOV_PROPERTY, SET_LOV_COLUMN_PROPERTY, LIST_VALUES, SHOW_LOV и соответствующие разделы триггеров.

| Forms | Fore |
|---|---|
| `FIND_LOV`, идентификатор LOV | Загрузка ресурса и сохранённая ссылка на `ForeLov` |
| `LIST_VALUES`, `SHOW_LOV`, `KEY-LISTVAL` | `binding.show()` / `lov.show(...)`; стандартная клавиша прототипа F9, можно заменить в XML |
| `GET_LOV_PROPERTY`: `AUTO_REFRESH`, `GROUP_NAME`, `HEIGHT`, `WIDTH`, `X_POS`, `Y_POS` | Неизменяемое `getDefinition()`; текущая геометрия доступна через показанную панель/окно |
| `SET_LOV_PROPERTY`: `AUTO_REFRESH`, `GROUP_NAME`, `LOV_SIZE`, `POSITION`, `TITLE` | Новое определение/экземпляр между вызовами; изменяемая палитра runtime-свойств не эмулируется |
| `SET_LOV_COLUMN_PROPERTY`: `TITLE`, `WIDTH` | Заголовки и ширины в определении; публичного механизма замены колонок во время показа пока нет |
| `CREATE_GROUP`, `CREATE_GROUP_FROM_QUERY`, `POPULATE_GROUP`, `POPULATE_GROUP_WITH_QUERY`, `DELETE_GROUP` | Реестр источников, `refresh()`, `invalidate()`, `close()`; SQL/жизненным циклом соединений владеет источник |
| `WHEN-VALIDATE-ITEM` | Прикладная валидация после `binding.validate()` |
| `WHEN-NEW-ITEM-INSTANCE`, навигация `KEY-NXT-ITEM` | Фокус JavaFX и явно переданный `nextFocus` |

`PRE-LOV`/`POST-LOV` не объявляются здесь стандартными триггерами Forms. Прикладные обработчики до и после выбора можно строить вокруг возвращаемого `CompletableFuture`.

## Отличия прототипа, которые нельзя скрывать

1. Поиск повторно обращается к источнику; результаты предыдущих поисков не накапливаются. Условия `prefix`, `contains`, `exact` работают с буквальным вводом: `%` и `_` пользователя не являются SQL-шаблоном.
2. `auto-refresh=false` хранит одну ограниченную выдачу, причём ключ включает условие, режим и параметры. Общий изменяемый record group нескольких LOV не эмулируется. Это также устраняет описанную в [3] проблему сохранения первоначально отфильтрованного подмножества.
3. `auto-select` применим и к первому запросу; частичная выдача с одной строкой не считается единственным совпадением.
4. `Filter Before Display` реализован в том же окне, без отдельного предварительного диалога. Для обязательного непустого фильтра дополнительно задаётся `min-length`.
5. `Validate from List` проверяет ввод при уходе с поля, но навигация JavaFX уже продолжается. Сохранение должно явно ждать результата проверки. Дополнительные бизнес-правила формы остаются обязательными.
6. Нет исполнения PL/SQL/Java из XML, импорта FMB, многострочного выбора, постраничной навигации, наследования property classes и прямой интеграции с `IDataSet`/`SQLDataSet`.
7. SQL проверен через JDBC на H2. Проверка конкретного Oracle JDBC-драйвера, NLS-сравнения, отмены запроса и прикладной транзакции остаётся интеграционной работой.
