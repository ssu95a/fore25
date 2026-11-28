import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import ru.inversion.fx.form.ActionFactory;
import ru.inversion.fx.form.ActionFactory.ActionTypeEnum;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.action.JInvEvent;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvMenuItem;

public final class LegacyActionFacadeSmoke {
    static final class CustomAction extends JInvAction {
        final List<ActionEvent> forwarded = new ArrayList<>();

        @Override
        public void handle(ActionEvent event) {
            forwarded.add(event);
            super.handle(event);
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void run() {
        checkFactory();
        var action = new CustomAction();
        var events = new ArrayList<ActionEvent>();
        action.setHandler(events::add);
        var first = new ActionEvent();
        IAction entryPoint = action;
        entryPoint.handle(first);
        entryPoint.handle();
        check(action.forwarded.size() == 2 && events.size() == 2
                && events.getFirst() == first && events.getLast() == null, "direct original event and virtual dispatch");
        var enabled = action.enabledProperty().orElseThrow();
        var upstream = new SimpleBooleanProperty(false);
        enabled.bind(upstream);
        entryPoint.handle(first);
        check(events.size() == 2 && enabled == action.enabledProperty().orElseThrow(), "same enabled property, disabled");
        try {
            action.setEnabled(true);
            throw new AssertionError("bound write rejection");
        } catch (RuntimeException expected) {
            check(!enabled.get(), "bound state unchanged");
        }
        upstream.set(true);
        enabled.unbind();
        var failure = new IllegalStateException("handler failure");
        var delivered = new AtomicReference<Throwable>();
        action.setExceptionHandler(delivered::set);
        action.setHandler(event -> { throw failure; });
        entryPoint.handle(first);
        check(delivered.get() == failure && events.size() == 2, "original throwable and handler replacement");
        action.setHandler(null);
        entryPoint.handle();
        check(delivered.get() == failure, "no handler is a no-op");
        var next = new JInvAction();
        action.setNextAction(next, false);
        action.setHandlerWithNextAction((event, actualNext) -> {
            check(event == first && actualNext == next, "next action preserved");
            events.add(event);
        });
        entryPoint.handle(first);
        check(events.size() == 3, "one chained callback");
        var builder = new ActionBuilder().setTitle("first");
        var built = builder.build();
        check(builder.setTitle("second").build() == built && built.getTitle().equals("second"), "builder result alias");
    }

    @SuppressWarnings("deprecation")
    private static void checkFactory() {
        var type = ActionTypeEnum.REFRESH;
        var catalog = (JInvAction) type.getAction();
        var oldTitle = catalog.getTitle();
        var received = new ArrayList<ActionEvent>();
        var keys = new ArrayList<>(List.of(new KeyCodeCombination(KeyCode.F7)));
        check(type.getAction() == catalog && catalog.getHotKey().isEmpty(), "shared legacy REFRESH without F8");
        try {
            catalog.setTitle("Updated catalog");
            var fromCatalog = ActionFactory.createAction(type, received::add);
            var fromDeprecated = ActionFactory.getAction(type, received::add, true);
            var custom = ActionFactory.createAction(type, null, "Custom title", "Original tooltip",
                    received::add, keys, null, null);
            var parallel = ActionFactory.createParallelAction(type, null, "Custom title", "Original tooltip",
                    received::add, keys, null, null);
            var empty = ActionFactory.createAction(null, received::add);
            var emptyDeprecated = ActionFactory.getAction(null, received::add, true);
            check(fromCatalog.getClass() == JInvAction.class && fromCatalog != catalog
                    && fromCatalog != ActionFactory.createAction(type, received::add)
                    && "Updated catalog".equals(fromCatalog.getTitle())
                    && "Updated catalog".equals(fromDeprecated.getTitle()), "catalog snapshots and identity");
            check(((JInvAction) fromDeprecated).isParallel() && ((JInvAction) emptyDeprecated).isParallel()
                    && ((JInvAction) parallel).isParallel() && !((JInvAction) custom).isParallel()
                    && empty.getActionType() == null && empty.getTitle() == null, "construction modes and nulls");
            check(custom.getIcon() == catalog.getIcon() && custom.getHotKey() == keys
                    && custom.getToolTip().contains("F7") && "Custom title".equals(custom.getTitle()),
                    "legacy metadata fallback and shortcut suffix");
            var button = (JInvButton) ActionFactory.createButton(custom);
            var menu = new JInvMenuItem(custom);
            check(button.getGraphic() != menu.getGraphic() && menu.getLastAction() == custom,
                    "separate graphics and same operation");
            var tooltip = button.getToolTipText();
            ((JInvAction) custom).setToolTip("Changed after configuration");
            check(tooltip.equals(button.getToolTipText()) && menu.getTooltip() == null, "legacy tooltip snapshot");
            button.fire();
            menu.fire();
            check(received.size() == 2
                    && received.get(0).getSource() == button
                    && ((ActionEvent) ((JInvEvent<?>) received.get(1)).getEvent()).getSource() == menu,
                    "legacy event sources and once per view");
            custom.setEnabled(false);
            check(button.isDisabled() && menu.isDisable(), "enabled state projects into both views");
        } finally {
            catalog.setTitle(oldTitle);
        }
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> { run(); return null; });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(task); });
        try {
            task.get(35, TimeUnit.SECONDS);
            System.out.println("ACT-005 PASS");
        } finally {
            Platform.exit();
        }
    }
}
