# Fore25 in Gluon Scene Builder

Build the small independent UI-preview JAR with JDK 25 (no corporate parent, no legacy application classes):

```sh
mvn -f scenebuilder/pom.xml package
```

Result: `scenebuilder/target/fore25-scenebuilder-0.2.0-SNAPSHOT.jar`. It contains Fore action/control classes, the bundled existing FontAwesome resource and captions. This UI build has no JInvCommon dependency. In Scene Builder's `Library` > `JAR/FXML Manager`, make ControlsFX 11.2.4 available and import this preview JAR. Select `ForeButton` and `ForeToolBar`, both concrete Node subclasses with public zero-argument constructors.

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

`StandardAction` owns all immutable standard defaults: `text()`, `tooltip()`, `icon()` and `hotkeys()`. One private `ResourceBundle` supplies captions and is loaded once for the default locale when the enum is initialized. `text()` reads the exact enum name (`CREATE`, `UPDATE`, `DELETE`, `VIEW`, `REFRESH`). `tooltip()` reads the optional `<name>_TOOLTIP` key and falls back to the caption. `ForeAction`, `ForeButton` and `ForeMenuItem` use the enum directly. Each factory call creates an independent runtime action whose text, icon, hotkeys and handler/state can be customized.

Inside a `FormController`, use its registration helper in `init()` or `guiInit()`:

```java
var create = createAction(StandardAction.CREATE, e -> createRecord());
btCreate.setAction(create);
miCreate.setAction(create);
```

After `guiInit()`, the framework installs one `ActionKeyBinder` on the form's Scene. Both F2 and F6 now invoke this same action. Matching key presses are consumed before native menu accelerators, including when the action is disabled, so a menu does not trigger a second callback. Other Scene accelerators are preserved. FXML `standardAction` alone supplies presentation; register a runtime action to enable its complete shortcut list.

`registerAction(action)` accepts an existing runtime action and `unregisterAction(action)` removes its keyboard binding. Registering the same instance twice has no effect. Conflicting registered actions are rejected, including overlapping ANY modifiers and platform Shortcut aliases. `action.setHotkeys(...)` checks every attached Scene before replacing the keys; rejected changes retain the previous keys and accelerator. Changes to bound actions run on the FX thread.

For use outside `FormController`, create `new ActionKeyBinder(scene)`, call `bind(action)` and close it on the FX thread when the owner is released. There is one active binder per Scene. Registered keys take precedence over native Scene/menu accelerators for those keys; unrelated accelerators are untouched.

On actual hiding or a failed launch, the framework removes the binder, validation listeners and its title binding on the FX thread. A vetoed close keeps them installed. Override `closeGuiResources()` for your own FX cleanup after initialization has begun; `closeResources()` still runs afterwards on a virtual thread for background resources.

The importer is DESIGN TIME. It must not need a running ForeApp, TaskContext, JInvCommon, database, or user business handler. A real GUI smoke test in the chosen Gluon Scene Builder version is still required to certify its importer/Inspector behavior.
