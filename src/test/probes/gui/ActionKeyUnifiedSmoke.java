import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventType;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.inversion.fore.form.action.ActionKeyBinder;
import ru.inversion.fore.form.action.ForeActions;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.action.JInvActionEvent;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.action.SceneKeyBinder;
import ru.inversion.fx.form.action.SceneKeyManager;

public final class ActionKeyUnifiedSmoke {
    private static final KeyCodeCombination KEY = new KeyCodeCombination(KeyCode.F4);

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void press(TextField field, EventType<KeyEvent> type) {
        field.fireEvent(new KeyEvent(type, "", "", KeyCode.F4, false, false, false, false));
    }

    private static void exercise(boolean callbackFirst) {
        var field = new TextField();
        var root = new VBox(field);
        var form = new AbstractBaseController<>();
        root.getProperties().put(AbstractBaseController.PROPERTY_CONTROLLER, new SimpleObjectProperty<>(form));
        var scene = new Scene(root, 260, 140);
        var stage = new Stage();
        stage.setScene(scene);
        stage.show();
        field.requestFocus();
        var pulse = new Object();
        Platform.runLater(() -> Platform.exitNestedEventLoop(pulse, null));
        Platform.enterNestedEventLoop(pulse);
        try {
            require(scene.getFocusOwner() == field, "actual focused control");
            var log = new ArrayList<String>();
            var veto = new boolean[]{false};
            scene.addEventFilter(KeyEvent.ANY, event -> {
                log.add("before");
                if (veto[0]) {
                    event.consume();
                }
            });
            var denied = new boolean[]{false};
            var action = new JInvAction() {
                @Override public boolean isEnabledBySecurity() { return !denied[0]; }
            };
            action.setHotKey(List.of(KEY));
            action.setKeyEventType(KeyEvent.KEY_PRESSED);
            action.setHandler(event -> {
                var payload = (JInvEvent<?>) event;
                require(payload.getControl() == field && payload.getPlaceType() == JInvEvent.PlaceType.FILTER,
                        "legacy action payload");
                log.add("action");
            });
            Runnable registerAction = () -> SceneKeyManager.addAction(field, action, JInvEvent.PlaceType.FILTER);
            Runnable registerCallback = () -> SceneKeyBinder.addAction(scene, KEY, event -> {
                require(event.getControl() == field && event.getPlaceType() == JInvActionEvent.PlaceType.FILTER,
                        "legacy callback payload");
                log.add("callback");
                return null;
            });
            if (callbackFirst) registerCallback.run(); else registerAction.run();
            scene.addEventFilter(KeyEvent.ANY, event -> log.add("between:" + event.isConsumed()));
            if (callbackFirst) registerAction.run(); else registerCallback.run();
            scene.addEventFilter(KeyEvent.ANY, event -> log.add("after:" + event.isConsumed()));
            scene.addEventHandler(KeyEvent.ANY, event -> log.add("downstream"));

            press(field, KeyEvent.KEY_PRESSED);
            require(log.equals(List.of("before", "action", "between:true", "after:true")), "deterministic order " + log);
            System.out.println((callbackFirst ? "callback-first " : "action-first ") + log);
            log.clear();
            veto[0] = true;
            press(field, KeyEvent.KEY_PRESSED);
            require(log.equals(List.of("before", "between:true", "after:true")), "consumed input " + log);
            veto[0] = false;

            action.setEnabled(false);
            log.clear();
            press(field, KeyEvent.KEY_PRESSED);
            require(log.equals(List.of("before", "between:false", "after:false", "downstream")), "disabled shadows " + log);
            action.setEnabled(true);
            denied[0] = true;
            log.clear();
            press(field, KeyEvent.KEY_PRESSED);
            require(log.equals(List.of("before", "between:false", "after:false", "downstream")), "denied shadows " + log);
            denied[0] = false;

            var nativeAction = ForeActions.custom("native", event -> log.add("native"));
            nativeAction.setAccelerator(KEY);
            form.getActionMap().put("command", nativeAction);
            try (var binder = ActionKeyBinder.attach(scene, command -> true)) {
                log.clear();
                press(field, KeyEvent.KEY_PRESSED);
                require(log.equals(List.of("before", "native", "between:true", "after:true")), "native priority " + log);
            }
            var actions = SceneKeyManager.getActionMap(scene, JInvEvent.PlaceType.FILTER);
            var callbacks = SceneKeyBinder.getCallBackMap(scene, JInvActionEvent.PlaceType.FILTER);
            require(actions == SceneKeyManager.getActionMap(scene, JInvEvent.PlaceType.FILTER)
                    && callbacks == SceneKeyBinder.getCallBackMap(scene, JInvActionEvent.PlaceType.FILTER),
                    "live collection identity");
            actions.clear();
            SceneKeyBinder.addAction(scene, KEY, event -> {
                log.add("handler");
                return null;
            }, JInvActionEvent.PlaceType.HANDLER);
            log.clear();
            press(field, KeyEvent.KEY_PRESSED);
            require(log.equals(List.of("before", "callback", "between:false", "after:false", "downstream", "handler")),
                    "callback and distinct handler phase " + log);
            callbacks.put(KEY, event -> {
                log.add("replacement");
                event.getEvent().consume();
                return null;
            });
            log.clear();
            press(field, KeyEvent.KEY_PRESSED);
            require(log.equals(List.of("before", "replacement", "between:true", "after:true")), "live mutation " + log);

            var released = new JInvAction();
            released.setHotKey(List.of(KEY));
            released.setKeyEventType(KeyEvent.KEY_RELEASED);
            released.setHandler(event -> log.add("released"));
            SceneKeyManager.addAction(field, released, JInvEvent.PlaceType.HANDLER);
            log.clear();
            press(field, KeyEvent.KEY_RELEASED);
            require(log.equals(List.of("before", "between:false", "after:false", "downstream", "released")),
                    "release in handler only " + log);
        } finally {
            stage.close();
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        var test = new FutureTask<Void>(() -> {
            exercise(false);
            exercise(true);
            return null;
        });
        Platform.runLater(test);
        try {
            test.get(20, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ACT-016 PASS");
    }
}
