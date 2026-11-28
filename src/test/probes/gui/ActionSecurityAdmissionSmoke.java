import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import ru.inversion.fx.app.sec.SecurityStrategyEnum;
import ru.inversion.fx.form.action.JInvAction;
import ru.inversion.fx.form.controls.JInvButton;
import ru.inversion.fx.form.controls.JInvMenuItem;

public final class ActionSecurityAdmissionSmoke {
    static final class OfflineAction extends JInvAction {
        boolean allowed;
        int checks;
        final SecurityStrategyEnum strategy;

        OfflineAction(SecurityStrategyEnum strategy) {
            this.strategy = strategy;
        }

        @Override public Integer getId() { return 123; }
        @Override public boolean isEnabledBySecurity() { checks++; return allowed; }
        @Override public SecurityStrategyEnum getSecurityStrategy() { return strategy; }
    }

    static void check(boolean ok, String assertion) {
        if (!ok) throw new AssertionError(assertion);
    }

    static void run() {
        for (var strategy : SecurityStrategyEnum.values()) {
            var action = new OfflineAction(strategy);
            var received = new AtomicReference<ActionEvent>();
            var error = new AtomicReference<Throwable>();
            action.setHandler(received::set);
            action.setExceptionHandler(error::set);
            var one = new JInvButton();
            var two = new JInvMenuItem();
            one.setId("button");
            two.setId("menu");
            one.setAction(action);
            two.setAction(action);
            check(one.isDisabled() == (strategy == SecurityStrategyEnum.DISABLE)
                    && two.isDisable() == one.isDisabled(), "disable strategy");
            check(one.isVisible() == (strategy != SecurityStrategyEnum.HIDE)
                    && two.isVisible() == one.isVisible(), "hide strategy");
            int before = action.checks;
            var event = new ActionEvent(one, one);
            action.setEnabled(false);
            action.handle(event);
            check(action.checks == before && error.get() == null && received.get() == null, "disabled before admission");
            action.setEnabled(true);
            action.handle(event);
            check(action.checks == before + 1 && received.get() == null && action.isEnabled()
                    && error.get() instanceof SecurityException, "deny while requested enabled");
            action.allowed = true;
            action.handle(event);
            check(received.get() == event && action.checks == before + 2, "allow original event");
            check(one.isDisabled() == (strategy == SecurityStrategyEnum.DISABLE)
                    && one.isVisible() == (strategy != SecurityStrategyEnum.HIDE), "security presentation stays independent");
            check(!two.isDisable() && two.isVisible() == (strategy != SecurityStrategyEnum.HIDE),
                    "menu keeps its legacy requested-state update order");
        }
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> { run(); return null; });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(task); });
        try {
            task.get(30, TimeUnit.SECONDS);
            System.out.println("ACT-006 PASS");
        } finally {
            Platform.exit();
        }
    }
}
