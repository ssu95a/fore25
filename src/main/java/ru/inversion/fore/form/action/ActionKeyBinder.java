package ru.inversion.fore.form.action;

import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;
import ru.inversion.dataset.fx.DSFXAdapter;
import ru.inversion.fx.form.AbstractBaseController;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvActionEvent;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.controls.Controls;
import ru.inversion.fx.form.controls.IJInvControl;
import ru.inversion.fx.form.controls.treetableex.TSFXAdapter;
import ru.inversion.utils.Pair;

/** Диспетчер контекстных команд сцены. Регистрация и вызовы выполняются в FX-потоке. */
public final class ActionKeyBinder implements AutoCloseable {
    private static final Object OWNER = new Object();
    public static final String ACTION_FILTER = "ru.inversion.ActionMapFilter";
    public static final String ACTION_HANDLER = "ru.inversion.ActionMapHandler";
    public static final String CALLBACK_FILTER = "ru.inversion.KeyBoardCallBackFilter";
    public static final String CALLBACK_HANDLER = "ru.inversion.KeyBoardCallBackHandler";

    private final Scene scene;
    private Predicate<? super ForeAction> authorized;
    private final EventHandler<KeyEvent> filter = event -> dispatch(event, true);
    private final EventHandler<KeyEvent> handler = event -> dispatch(event, false);
    private boolean filtering;
    private boolean handling;
    private boolean legacyFilter;

    private ActionKeyBinder(Scene scene) {
        this.scene = scene;
    }

    /**
     * Включает контекстные команды в фазе фильтра общего диспетчера сцены.
     */
    public static ActionKeyBinder attach(Scene scene, Predicate<? super ForeAction> authorized) {
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(authorized, "authorized");
        var binder = owner(scene);
        if (binder.authorized != null) {
            throw new IllegalStateException("Scene already has contextual keyboard registration");
        }
        binder.authorized = authorized;
        binder.install(true);
        return binder;
    }

    private static ActionKeyBinder owner(Scene scene) {
        return (ActionKeyBinder) scene.getProperties().computeIfAbsent(OWNER, key -> new ActionKeyBinder(scene));
    }

    private void install(boolean filtering) {
        if (filtering && !this.filtering) {
            scene.addEventFilter(KeyEvent.ANY, filter);
            this.filtering = true;
        } else if (!filtering && !handling) {
            scene.addEventHandler(KeyEvent.ANY, handler);
            handling = true;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Pair<Node, IAction>> legacyActions(Scene scene, boolean filter) {
        registerPhase(scene, filter);
        return (List<Pair<Node, IAction>>) scene.getProperties().computeIfAbsent(
                filter ? ACTION_FILTER : ACTION_HANDLER, key -> new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public static HashMap<KeyCodeCombination, Callback<JInvActionEvent<KeyEvent>, Object>> legacyCallbacks(
            Scene scene, boolean filter) {
        registerPhase(scene, filter);
        return (HashMap<KeyCodeCombination, Callback<JInvActionEvent<KeyEvent>, Object>>) scene.getProperties()
                .computeIfAbsent(filter ? CALLBACK_FILTER : CALLBACK_HANDLER, key -> new HashMap<>());
    }

    private static void registerPhase(Scene scene, boolean filter) {
        var binder = owner(scene);
        if (filter) {
            binder.legacyFilter = true;
        }
        binder.install(filter);
    }

    public static void dispatchRegistered(KeyEvent event, boolean filter) {
        if (!event.isConsumed() && event.getSource() instanceof Scene scene
                && scene.getProperties().get(OWNER) instanceof ActionKeyBinder binder) {
            binder.dispatch(event, filter);
        }
    }

    @SuppressWarnings("unchecked")
    private void dispatch(KeyEvent event, boolean filter) {
        if (event.isConsumed()) {
            return;
        }
        if (filter && authorized != null && event.getEventType() == KeyEvent.KEY_PRESSED && dispatchNative(event)) {
            return;
        }
        var properties = scene.getProperties();
        var actions = (List<Pair<Node, IAction>>) properties.get(filter ? ACTION_FILTER : ACTION_HANDLER);
        if (actions != null && scene.getFocusOwner() instanceof Control control) {
            IAction action = findLegacy(control, actions, event);
            if (action != null) {
                if (ActionInvocation.admit(action, IAction::isEnabled, IAction::isEnabledBySecurity)
                        == ActionInvocation.Admission.ALLOWED) {
                    var place = filter ? JInvEvent.PlaceType.FILTER : JInvEvent.PlaceType.HANDLER;
                    ActionInvocation.invoke(action, IAction::handle, new JInvEvent(control, place, event));
                    event.consume();
                }
                return;
            }
        }
        if (event.getEventType() == KeyEvent.KEY_PRESSED) {
            var callbacks = (HashMap<KeyCodeCombination, Callback<JInvActionEvent<KeyEvent>, Object>>)
                    properties.get(filter ? CALLBACK_FILTER : CALLBACK_HANDLER);
            if (callbacks != null) {
                for (var entry : callbacks.entrySet()) {
                    if (entry.getKey().match(event)) {
                        var callback = entry.getValue();
                        if (callback != null) {
                            var place = filter ? JInvActionEvent.PlaceType.FILTER : JInvActionEvent.PlaceType.HANDLER;
                            callback.call(new JInvActionEvent<>((Control) scene.getFocusOwner(), place, event));
                        }
                        return;
                    }
                }
            }
        }
    }

    private IAction findLegacy(Control control, List<Pair<Node, IAction>> actions, KeyEvent event) {
        IAction action = findLegacyContext(control, actions, event);
        if (action == null && control instanceof IJInvControl legacyControl) {
            var adapter = legacyControl.getDataSetAdapter();
            if (adapter != null) {
                action = findLegacyContext(adapter.getTable(), actions, event);
            }
        }
        if (action != null) {
            return action;
        }
        for (var entry : actions) {
            if (matchesLegacy(entry.second, event)) {
                return entry.second;
            }
        }
        return null;
    }

    private IAction findLegacyContext(Node start, List<Pair<Node, IAction>> actions, KeyEvent event) {
        Node fast = start;
        for (Node node = start; node != null; node = legacyParent(node)) {
            for (var entry : actions) {
                if (entry.first.equals(node) && matchesLegacy(entry.second, event)) {
                    return entry.second;
                }
            }
            fast = legacyParent(legacyParent(fast));
            if (fast != null && fast == legacyParent(node)) {
                return null;
            }
        }
        return null;
    }

    private static Node legacyParent(Node node) {
        if (node == null) {
            return null;
        }
        if (node.hasProperties() && node.getProperties().get(Controls.CONTROL_PARENT) instanceof Node owner) {
            return owner;
        }
        return node.getParent();
    }

    private static boolean matchesLegacy(IAction action, KeyEvent event) {
        return action.getHotKey().stream().anyMatch(key -> key != null && key.match(event))
                && action.getKeyEventType() == event.getEventType();
    }

    private boolean dispatchNative(KeyEvent event) {
        Node start = event.getTarget() instanceof Node node ? node : scene.getFocusOwner();
        if (start != null && start.getScene() != scene) {
            return false;
        }
        if (start == null) {
            start = scene.getRoot();
        }
        ActionMap scope = resolve(start);
        if (scope == null) {
            return false;
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
                return true;
            }
        }
        return false;
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
        authorized = null;
        if (filtering && !legacyFilter) {
            scene.removeEventFilter(KeyEvent.ANY, filter);
            filtering = false;
        }
        if (!filtering && !handling) {
            scene.getProperties().remove(OWNER, this);
        }
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
