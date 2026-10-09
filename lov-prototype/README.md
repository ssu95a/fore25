# Прототип декларативного LOV

Изолированный Maven-проект компилирует новые классы LOV из общего `src` и запускается без `JInvCommon`, `TaskContext`, корпоративного Nexus и Oracle Database. Требуются **JDK 25** и доступ Maven к публичным зависимостям.

Из корня репозитория:

```sh
mvn -version
mvn -f lov-prototype/pom.xml -Pheadless clean test
mvn -f lov-prototype/pom.xml javafx:run
```

В первой команде должна отображаться Java 25. В IDE можно импортировать этот POM, выбрать JDK 25 и запустить `ru.inversion.fore.demo.lov.LovDemo`. Профиль `headless` нужен только тестам без графического окружения.

В демо:

1. F9 или кнопка `…` открывает справочник подразделений.
2. Поиск выполняется по коду, например `БУ`.
3. Подтверждение заполняет код, название и скрытый идентификатор. Отмена сохраняет прежние значения.
4. Ввод точного кода `БУХ` и кнопка проверки демонстрируют проверку без окна выбора.

Демо использует [departments.lxml](../src/main/resources/ru/inversion/fore/demo/lov/departments.lxml). SQL-вариант [departments-sql.lxml](../src/main/resources/ru/inversion/fore/demo/lov/departments-sql.lxml) требует зарегистрированного `DataSource` и прикладной таблицы; автоматически он не запускается.

Тесты используют JavaFX с Monocle, H2 и JUnit. Они проверяют окно выбора, возврат нескольких значений, отмену, асинхронные запросы, ограниченную выдачу, строгую загрузку XML и параметризованный JDBC. Проверки Oracle JDBC и транзакционной интеграции с JInvCommon в этот проект не входят.

- [Каталог свойств Oracle Forms и соответствий JavaFX](../docs/lov-oracle-forms.md).
- [Формат LXML, API, ограничения и подключение к форме](../docs/lxml.md).

Сборка `standalone/pom.xml` также включает LOV, но для неё, как и раньше, нужен корпоративный `JInvCommon`. Сборка Scene Builder от этого прототипа не зависит.
