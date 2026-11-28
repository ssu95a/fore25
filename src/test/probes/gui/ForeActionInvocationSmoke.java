import java.util.ArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.action.ActionUtils;
import ru.inversion.fore.form.action.ForeActions;

public final class ForeActionInvocationSmoke {
    static void check(boolean condition, String what) {
        if (!condition) throw new AssertionError(what);
    }

    static void run() {
        var received = new ArrayList<ActionEvent>();
        var action = ForeActions.custom("native", received::add);
        var button = ActionUtils.createButton(action);
        var item = ActionUtils.createMenuItem(action);
        var stage = new Stage();
        stage.setScene(new Scene(new VBox(button), 250, 100));
        stage.show();
        try {
            var direct = new ActionEvent(button, button);
            action.handle(direct);
            action.handle(null);
            button.fire();
            item.fire();
            check(received.size() == 4 && received.getFirst() == direct && received.get(1) == null
                    && received.get(2).getSource() == button && received.get(3).getSource() == item, "original events");
            action.setDisabled(true);
            action.handle(direct);
            button.fire();
            item.fire();
            check(received.size() == 4, "disabled bypasses invocation");
            var error = new IllegalStateException("original");
            action.setHandler(e -> { throw error; });
            action.setDisabled(false);
            try {
                action.handle(direct);
                throw new AssertionError("exception propagated");
            } catch (IllegalStateException reported) {
                check(reported == error, "original failure");
            }
            var replacement = new ArrayList<ActionEvent>();
            action.setHandler(replacement::add);
            item.fire();
            check(received.size() == 4 && replacement.size() == 1 && replacement.getFirst().getSource() == item,
                    "live replacement in configured view");
        } finally {
            stage.hide();
        }
    }

    public static void main(String[] args) throws Exception {
        var task = new FutureTask<Void>(() -> { run(); return null; });
        Platform.startup(() -> { Platform.setImplicitExit(false); Platform.runLater(task); });
        try {
            task.get(35, TimeUnit.SECONDS);
            System.out.println("ACT-004 PASS");
        } finally {
            Platform.exit();
        }
    }
}
