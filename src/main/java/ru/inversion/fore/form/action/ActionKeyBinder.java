package ru.inversion.fore.form.action;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javafx.collections.MapChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.action.SceneKeyBinder;
import ru.inversion.fx.form.action.SceneKeyManager;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;

/** Диспетчер контекстных команд для сцены без старых регистраторов клавиатуры. Только FX-поток. */
public final class ActionKeyBinder implements AutoCloseable {
    private static final Object OWNER = new Object();

    private final Scene scene;
    private final Predicate<? super ForeAction> authorized;
    private final EventHandler<KeyEvent> handler = this::dispatch;
    private final MapChangeListener<Object, Object> registration = change -> {
        if ((change.wasAdded() && legacyKey(change.getKey()))
                || (change.getKey() == OWNER && change.wasRemoved())) {
            close();
        }
    };
    private boolean attached = true;

    private ActionKeyBinder(Scene scene, Predicate<? super ForeAction> authorized) {
        this.scene = scene;
        this.authorized = authorized;
    }

    /**
     * Подключает один диспетчер. При установке старого регистратора
     * диспетчер автоматически отсоединяется, сохраняя порядок старых обработчиков.
     */
    public static ActionKeyBinder attach(Scene scene, Predicate<? super ForeAction> authorized) {
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(authorized, "authorized");
        var properties = scene.getProperties();
        if (properties.containsKey(OWNER) || properties.keySet().stream().anyMatch(ActionKeyBinder::legacyKey)) {
            throw new IllegalStateException("Scene already has an action key dispatcher");
        }
        var binder = new ActionKeyBinder(scene, authorized);
        properties.put(OWNER, binder);
        properties.addListener(binder.registration);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, binder.handler);
        return binder;
    }

    private static boolean legacyKey(Object key) {
        return SceneKeyManager.KEYBOARD_CALLBACK_FILTER.equals(key)
                || SceneKeyManager.KEYBOARD_CALLBACK_HANDLER.equals(key)
                || SceneKeyBinder.KEYBOARD_CALLBACK_FILTER.equals(key)
                || SceneKeyBinder.KEYBOARD_CALLBACK_HANDLER.equals(key);
    }

    private void dispatch(KeyEvent event) {
        Node start = event.getTarget() instanceof Node node ? node : scene.getFocusOwner();
        if (start != null && start.getScene() != scene) {
            return;
        }
        if (start == null) {
            start = scene.getRoot();
        }
        ActionMap scope = resolve(start);
        if (scope == null) {
            return;
        }
        var match = new Match(scope, event);
        for (ActionMap map = scope; map != null; map = map.getParent()) {
            match.scope = map;
            map.forEachLocal(match);
            if (match.command != null) {
                event.consume();
                if (!match.ambiguous && ActionInvocation.admit(match.command,
                        action -> !action.isDisabled(), authorized::test) == ActionInvocation.Admission.ALLOWED) {
                    ActionInvocation.invoke(match.command, ForeAction::handle, new ActionEvent(start, event.getTarget()));
                }
                return;
            }
        }
    }

    private ActionMap resolve(Node start) {
        Node fast = start;
        for (Node node = start; node != null; node = next(node)) {
            if (node.hasProperties()) {
                var properties = node.getProperties();
                Object adapter = properties.get(DSFXAdapter.PROPERTY_DATA_SET_ADAPTER);
                ActionMap map = switch (adapter) {
                    case DSFXAdapter<?> table -> table.peekActionMap();
                    case TSFXAdapter<?> tree -> tree.peekActionMap();
                    case null, default -> null;
                };
                if (map != null) {
                    return map;
                }
                if (properties.containsKey(AbstractBaseController.PROPERTY_CONTROLLER)) {
                    AbstractBaseController<?> form = Controls.getControllerFromControl(node);
                    if (form != null) {
                        return form.peekActionMap();
                    }
                }
            }
            fast = fast == null ? null : next(fast);
            fast = fast == null ? null : next(fast);
            if (fast != null && fast == next(node)) {
                return null;
            }
        }
        return null;
    }

    private Node next(Node node) {
        if (node.hasProperties()
                && node.getProperties().get(Controls.CONTROL_PARENT) instanceof Node owner
                && owner.getScene() == scene) {
            return owner;
        }
        return node.getParent();
    }

    @Override
    public void close() {
        var properties = scene.getProperties();
        if (!attached) {
            return;
        }
        attached = false;
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, handler);
        properties.removeListener(registration);
        properties.remove(OWNER, this);
    }

    private static final class Match implements BiConsumer<Comparable<?>, ForeAction> {
        private final ActionMap origin;
        private final KeyEvent event;
        private ActionMap scope;
        private ForeAction command;
        private boolean ambiguous;

        private Match(ActionMap origin, KeyEvent event) {
            this.origin = origin;
            this.event = event;
        }

        @Override
        public void accept(Comparable<?> key, ForeAction action) {
            if (!action.matches(event)) {
                return;
            }
            for (ActionMap preceding = origin; preceding != scope; preceding = preceding.getParent()) {
                if (preceding.getLocal(key) != null) {
                    return;
                }
            }
            if (command == null) {
                command = action;
            } else {
                ambiguous = true;
            }
        }
    }
}
