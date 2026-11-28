import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.dataset.ArrayDataSet;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fore.form.action.ActionKeyBinder;
import ru.inversion.fore.form.action.ForeAction;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.JInvFXFormController;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.action.SceneKeyManager;
import ru.inversion.fx.form.controls.Controls;

public final class ActionKeyContextSmoke {
    private static final class OfflineForm extends JInvFXFormController<Object> {
        @Override
        public ru.inversion.tc.TaskContext getTaskContext() {
            return null;
        }
    }

    public static final class Row {}

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void press(Node node, KeyCode code, boolean control) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, control, false, false));
    }

    private static ForeAction command(String id, KeyCode key, List<String> seen) {
        var action = ForeActions.custom(id, event -> {
            require(event.getSource() instanceof Node && event.getTarget() instanceof Node, "action event payload");
            seen.add(id);
        });
        action.setAccelerator(new KeyCodeCombination(key));
        return action;
    }

    private static void exercise() throws Exception {
        var left = new TextField();
        var right = new TextField();
        var proxy = new TextField();
        var table = new TableView<Row>();
        var leftForm = new OfflineForm();
        var rightForm = new OfflineForm();
        var leftPane = new VBox(left, table);
        var rightPane = new VBox(right, proxy);
        leftPane.getProperties().put(AbstractBaseController.PROPERTY_CONTROLLER, new SimpleObjectProperty<>(leftForm));
        rightPane.getProperties().put(AbstractBaseController.PROPERTY_CONTROLLER, new SimpleObjectProperty<>(rightForm));
        var scene = new Scene(new VBox(leftPane, rightPane), 460, 400);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();
        var observed = new ArrayList<String>();
        var nativeCalls = new AtomicInteger();
        var permissionChecks = new AtomicInteger();
        var parent = command("form", KeyCode.F5, observed);
        var other = command("other", KeyCode.F5, observed);
        var local = command("adapter", KeyCode.F3, observed);
        var denied = command("denied", KeyCode.F7, observed);
        var controlKey = new KeyCodeCombination(KeyCode.F9, KeyCombination.CONTROL_DOWN);
        var conflict = command("conflict", KeyCode.F9, observed);
        var adapter = new DSFXAdapter<Row>();
        adapter.bindTable(new ArrayDataSet<>(Row.class, new ArrayList<Row>(), false), table, null, false);
        try (var binder = ActionKeyBinder.attach(scene, action -> {
            permissionChecks.incrementAndGet();
            return action != denied;
        })) {
            var form = leftForm.getActionMap();
            var data = adapter.getActionMap();
            var sibling = rightForm.getActionMap();
            form.put("refresh", parent);
            sibling.put("refresh", other);
            data.put("refresh", local);
            data.put("denied", denied);
            proxy.getProperties().put(Controls.CONTROL_PARENT, table);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), nativeCalls::incrementAndGet);
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F7), nativeCalls::incrementAndGet);
            scene.getAccelerators().put(controlKey, nativeCalls::incrementAndGet);
            try {
                ActionKeyBinder.attach(scene, action -> true);
                throw new AssertionError("duplicate binder accepted");
            } catch (IllegalStateException expected) {
                // На сцену допускается один диспетчер.
            }
            press(table, KeyCode.F5, false);
            require(nativeCalls.get() == 1 && observed.isEmpty(), "changed local hotkey shadows ancestor ID");
            press(table, KeyCode.F3, false);
            press(left, KeyCode.F5, false);
            press(right, KeyCode.F5, false);
            press(proxy, KeyCode.F3, false);
            require(observed.equals(List.of("adapter", "form", "other", "adapter")), "context and logical owner");

            local.setDisabled(true);
            press(table, KeyCode.F3, false);
            press(table, KeyCode.F7, false);
            require(nativeCalls.get() == 1 && observed.size() == 4 && permissionChecks.get() == 5,
                    "disabled/denied matches reserve the key");
            local.setDisabled(false);
            local.setHotkeys(List.of(new KeyCodeCombination(KeyCode.F8), controlKey));
            conflict.setAccelerator(new KeyCodeCombination(KeyCode.F9, KeyCombination.CONTROL_ANY));
            data.put("overlap", conflict);
            press(table, KeyCode.F9, true);
            require(observed.size() == 4 && nativeCalls.get() == 1, "overlap dispatches no arbitrary operation");
            data.remove("overlap");
            press(table, KeyCode.F9, true);
            local.setAccelerator(new KeyCodeCombination(KeyCode.F11));
            press(table, KeyCode.F11, false);
            require(observed.subList(4, 6).equals(List.of("adapter", "adapter")), "live primary and alternative keys");

            Event.fireEvent(left, new KeyEvent(KeyEvent.KEY_TYPED, "x", "", KeyCode.UNDEFINED,
                    false, false, false, false));
            require(left.getText().equals("x"), "native text editing");
            leftPane.getChildren().remove(table);
            press(table, KeyCode.F11, false);
            require(observed.size() == 6, "detached view never dispatches");
            rightPane.getChildren().add(table);
            press(table, KeyCode.F11, false);
            require(observed.size() == 7 && data.getParent() == sibling, "reattached view uses new scope");
            adapter.close();
            press(table, KeyCode.F5, false);
            require(observed.getLast().equals("other"), "closed adapter recovers controller context");

            SceneKeyManager.getActionMap(scene, JInvEvent.PlaceType.FILTER);
            press(right, KeyCode.F5, false);
            require(nativeCalls.get() == 2 && observed.size() == 8, "legacy attach detaches native binder");
            try {
                ActionKeyBinder.attach(scene, action -> true);
                throw new AssertionError("legacy Scene accepted native binder");
            } catch (IllegalStateException expected) {
                // Свойства старых регистраторов резервируют сцену.
            }
        } finally {
            adapter.close();
            stage.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        var test = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.runLater(test);
        try {
            test.get(20, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ACT-015 PASS");
    }
}
