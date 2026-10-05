# Fore25 in Gluon Scene Builder

Build the small independent UI-preview JAR with JDK 25 (no corporate parent, no legacy application classes):

```sh
mvn -f scenebuilder/pom.xml package
```

Result: `scenebuilder/target/fore25-scenebuilder-0.2.0-SNAPSHOT.jar`. It contains Fore action/control classes, the bundled existing FontAwesome resource and captions. `ActionPrototype` uses `Checks` from the real `ru.inversion:JInvCommon:1.0.0` binary, so Maven needs your corporate settings/mirror or the dependency already installed locally. In Scene Builder's `Library` > `JAR/FXML Manager`, make ControlsFX 11.2.4 and JInvCommon available and import this preview JAR. Select `ForeButton` and `ForeToolBar`, both concrete Node subclasses with public zero-argument constructors.

The preview JAR also includes `ForeTextField` and `IForeControl`. Run all action/control contract tests without a display or the corporate Java 8 build using JDK 25:

```sh
mvn -f scenebuilder/pom.xml -Pheadless clean test
```

Monocle is a test-only headless backend. This does not test the full form runtime. See [the standalone build](../standalone/README.md) to compile all new Fore25 classes and run `FormControllerTest` against the real `JInvCommon` binary.

Import the five `Fore*Button.fxml` snippets into the same Library Manager: drag-and-drop CREATE/UPDATE/DELETE/VIEW/REFRESH are then available as already styled prototypes, without choosing text, tooltip, or icon. `ForeStandardToolBar.fxml` supplies all five. `ForeMenuExample.fxml` demonstrates the FXML `ForeMenuItem`. Since `MenuItem` is NOT a Node, it is not guaranteed to show as an independent Scene Builder custom-palette entry. The supplied MenuBar fragment works around that limitation.

FXML mode:

```xml
<?import ru.inversion.fore.form.control.ForeButton?>
<ForeButton fx:id="btCreate" standardAction="CREATE" onAction="#onCreate" />
```

`standardAction` is a real JavaBean enum property; it configures presentation defaults, but never replaces the standard FXML onAction callback. Explicit text/tooltip/graphic overrides survive regardless of attribute setter order. The property may be exposed in Inspector/Miscellaneous according to the Scene Builder version; the ready-made FXML fragments also work if no dedicated enum drop-down is offered.

To share ONE action across button/menu at runtime instead, do not assign individual FXML onAction callbacks. In the controller after FXML injection:

```java
var create = ForeActions.create(StandardAction.CREATE, e -> createRecord());
btCreate.setAction(create);
miCreate.setAction(create);
```

For form-specific labels, pass the form's `ResourceBundle`:

```java
var create = ForeActions.create(StandardAction.CREATE, getBundle(), e -> createRecord());
```

`ActionPrototype.bundle()` retains this bundle. `text()` reads the exact `StandardAction.name()` key (`CREATE`, `UPDATE`, `DELETE`, `VIEW`, `REFRESH`). `tooltip()` reads the optional `<name>_TOOLTIP` key and falls back to the caption. A missing caption key fails when the prototype is constructed. Existing overloads use the bundled standard captions for the current locale.

The importer is DESIGN TIME. It must not need a running ForeApp, TaskContext, database, or user business handler. JInvCommon is needed for the shared argument checks. A real GUI smoke test in the chosen Gluon Scene Builder version is still required to certify its importer/Inspector behavior.
