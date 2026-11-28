import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import ru.inversion.fx.form.action.ActionBuilder;
import ru.inversion.fx.form.action.IAction;
import ru.inversion.fx.form.action.JInvAction;

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
