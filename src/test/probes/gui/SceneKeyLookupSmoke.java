import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.action.JInvEvent.PlaceType;
import ru.inversion.fx.form.action.SceneKeyManager;
import ru.inversion.fx.form.controls.Controls;

public final class SceneKeyLookupSmoke {
    public static void main(String[] args) throws Exception {
        var test = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.startup(() -> Platform.runLater(test));
        try {
            test.get(20, TimeUnit.SECONDS);
            System.out.println("CTRL-024 PASS");
        } finally {
            Platform.exit();
        }
    }

    private static void exercise() throws Exception {
        String fx = System.getProperty("javafx.version", "");
        check(Runtime.version().feature() == 25 && fx.matches("25(?:\\..*)?"), "Java / JavaFX 25");
        var focus = new Button("Focus");
        var inner = new VBox(focus);
        var nearest = new VBox(inner);
        var logical = new VBox();
        var root = new VBox(nearest, logical);
        var scene = new Scene(root);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();
        try {
            focus.requestFocus();
            check(scene.getFocusOwner() == focus, "stock button owns focus");
            var calls = new ArrayList<String>();
            var global = new Action("global", focus, PlaceType.FILTER, KeyCode.F7, KeyEvent.KEY_PRESSED, calls);
            var local = new Action("nearest", focus, PlaceType.FILTER, KeyCode.F7, KeyEvent.KEY_PRESSED, calls);
            var override = new Action("logical", focus, PlaceType.FILTER, KeyCode.F7, KeyEvent.KEY_PRESSED, calls);
            var handler = new Action("handler", focus, PlaceType.HANDLER, KeyCode.F7, KeyEvent.KEY_PRESSED, calls);
            var release = new Action("release", focus, PlaceType.HANDLER, KeyCode.F7, KeyEvent.KEY_RELEASED, calls);
            SceneKeyManager.addAction(scene, global, PlaceType.FILTER);
            SceneKeyManager.addAction(nearest, local, PlaceType.FILTER);
            SceneKeyManager.addAction(logical, override, PlaceType.FILTER);
            SceneKeyManager.addAction(scene, handler, PlaceType.HANDLER);
            SceneKeyManager.addAction(scene, release, PlaceType.HANDLER);
            var reachedTarget = new ArrayList<KeyCode>();
            focus.addEventHandler(KeyEvent.ANY, e -> reachedTarget.add(e.getCode()));

            // Пустая карта и ещё не созданная карта одинаковы для hasProperties().
            Field properties = Node.class.getDeclaredField("properties");
            properties.setAccessible(true);
            check(!focus.hasProperties() && !inner.hasProperties()
                    && properties.get(focus) == null && properties.get(inner) == null, "pristine visited nodes");
            fire(focus, KeyEvent.KEY_PRESSED, KeyCode.F7);
            check(calls.equals(List.of("nearest")) && reachedTarget.isEmpty(), "nearest filter wins and consumes");
            boolean allocatedFocus = properties.get(focus) != null;
            boolean allocatedInner = properties.get(inner) != null;
            System.out.printf("nearest=%s target=%s maps before=false/false after=%s/%s hasProperties=%s/%s%n",
                    calls, reachedTarget, allocatedFocus, allocatedInner, focus.hasProperties(), inner.hasProperties());

            inner.getProperties().put(Controls.CONTROL_PARENT, logical);
            Object existing = properties.get(inner);
            fire(focus, KeyEvent.KEY_PRESSED, KeyCode.F7);
            inner.getProperties().remove(Controls.CONTROL_PARENT);
            nearest.getChildren().remove(inner);
            root.getChildren().add(inner);
            focus.requestFocus();
            check(scene.getFocusOwner() == focus, "focus after physical reparent");
            fire(focus, KeyEvent.KEY_PRESSED, KeyCode.F7);
            fire(focus, KeyEvent.KEY_RELEASED, KeyCode.F7);
            fire(focus, KeyEvent.KEY_PRESSED, KeyCode.F8);
            global.setEnabled(false);
            fire(focus, KeyEvent.KEY_PRESSED, KeyCode.F7);
            global.setEnabled(true);
            global.security = false;
            fire(focus, KeyEvent.KEY_PRESSED, KeyCode.F7);
            check(calls.equals(List.of("nearest", "logical", "global", "release", "handler", "handler")),
                    "action order and exact counts " + calls);
            check(reachedTarget.equals(List.of(KeyCode.F7, KeyCode.F8, KeyCode.F7, KeyCode.F7)),
                    "filter / handler propagation " + reachedTarget);
            check(properties.get(inner) == existing, "existing metadata map identity");
            System.out.println("actions=" + calls + " target=" + reachedTarget + " metadata identity preserved");
            check(!allocatedFocus && !allocatedInner, "dispatch allocated maps on pristine nodes");
        } finally {
            stage.hide();
        }
    }

    private static void fire(Node node, EventType<KeyEvent> type, KeyCode code) {
        node.fireEvent(new KeyEvent(type, "", "", code, false, false, false, false));
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }

    private static final class Action implements IAction {
        private final String name;
        private final Node target;
        private final PlaceType place;
        private final List<KeyCodeCombination> keys;
        private final EventType<KeyEvent> phase;
        private final List<String> calls;
        private boolean enabled = true;
        private boolean security = true;

        Action(String name, Node target, PlaceType place, KeyCode key, EventType<KeyEvent> phase, List<String> calls) {
            this.name = name;
            this.target = target;
            this.place = place;
            this.keys = List.of(new KeyCodeCombination(key));
            this.phase = phase;
            this.calls = calls;
        }

        @Override public List<KeyCodeCombination> getHotKey() { return keys; }
        @Override public EventType<KeyEvent> getKeyEventType() { return phase; }
        @Override public boolean isEnabledBySecurity() { return security; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public void setEnabled(boolean value) { enabled = value; }
        @Override public void handle() { throw new AssertionError("action requires event context"); }

        @Override public void handle(ActionEvent event) {
            check(event instanceof JInvEvent<?> fired && fired.getControl() == target && fired.getPlaceType() == place
                    && fired.getEvent() instanceof KeyEvent key && key.getEventType() == phase
                    && key.getTarget() == target && keys.getFirst().match(key), "action context: " + name);
            calls.add(name);
        }
    }
}
