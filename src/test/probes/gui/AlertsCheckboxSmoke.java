import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.inversion.fx.form.Alerts;

public final class AlertsCheckboxSmoke {
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static DialogPane showingPane() {
        return Window.getWindows().stream().filter(Window::isShowing)
                .map(window -> window.getScene().getRoot())
                .filter(DialogPane.class::isInstance).map(DialogPane.class::cast).findFirst().orElseThrow();
    }

    private static Button button(DialogPane pane, ButtonType type) {
        return (Button) pane.lookupButton(type);
    }

    private static CheckBox checkBox(DialogPane pane) {
        return (CheckBox) ((VBox) pane.getContent()).getChildren().getLast();
    }

    private static void key(Node node, javafx.event.EventType<KeyEvent> type, KeyCode code, String text) {
        Event.fireEvent(node, new KeyEvent(type, text, "", code, false, false, false, false));
    }

    private static Optional<Alerts.AlertResult> show(Alerts.IAlertMaker maker, Consumer<DialogPane> inspect,
                                                     ButtonType cleanup) throws Exception {
        var task = new FutureTask<Void>(() -> {
            var pane = showingPane();
            try {
                inspect.accept(pane);
            } finally {
                if (pane.getScene() != null) {
                    button(pane, cleanup).fire();
                }
            }
            return null;
        });
        Platform.runLater(task);
        var result = maker.showResultAndWait();
        task.get();
        return result;
    }

    private static void exercise() throws Exception {
        var service = Alerts.maker().checkBoxText("Remember");
        var selected = show(service, pane -> {
            CheckBox box = checkBox(pane);
            require(!box.isSelected() && !button(pane, ButtonType.OK).isDisabled(), "service starts clear");
            box.fire();
        }, ButtonType.OK);
        require(selected.orElseThrow().equals(new Alerts.AlertResult(ButtonType.OK, true)), "service result");
        var cleared = show(service.checkBoxSelected(true), pane -> {
            CheckBox box = checkBox(pane);
            require(box.isSelected(), "initial true");
            box.fire();
        }, ButtonType.OK);
        require(!cleared.orElseThrow().checkBoxSelected(), "state is read per show");

        var calls = new AtomicInteger();
        var gated = Alerts.maker().type(Alert.AlertType.CONFIRMATION)
                .buttons(ButtonType.YES, ButtonType.APPLY, ButtonType.CANCEL)
                .checkBoxText("Confirm").requireCheckBox(ButtonType.YES)
                .validator(() -> { calls.incrementAndGet(); return false; });
        var applied = show(gated, pane -> {
            require(button(pane, ButtonType.YES).isDisabled(), "YES gated");
            require(!button(pane, ButtonType.APPLY).isDisabled()
                    && !button(pane, ButtonType.CANCEL).isDisabled(), "APPLY and CANCEL independent");
            button(pane, ButtonType.YES).fire();
            require(pane.getScene() != null, "disabled YES cannot fire");
            checkBox(pane).fire();
            require(!button(pane, ButtonType.YES).isDisabled(), "YES enabled after selection");
        }, ButtonType.APPLY);
        require(applied.orElseThrow().equals(new Alerts.AlertResult(ButtonType.APPLY, true)), "APPLY result");
        require(calls.get() == 0, "no OK means the supplier is unused");

        var field = new TextField();
        var confirmed = Alerts.maker().type(Alert.AlertType.CONFIRMATION).content(field)
                .checkBoxText("Confirm").requireCheckBox(ButtonType.OK)
                .validator(() -> {
                    require(Platform.isFxApplicationThread(), "validator FX thread");
                    return switch (field.getText()) {
                        case "throw" -> throw new IllegalStateException("expected test failure");
                        case "null" -> null;
                        default -> field.getText().equals("x");
                    };
                });
        var initial = new FutureTask<Void>(() -> {
            var pane = showingPane();
            require(button(pane, ButtonType.OK).isDisabled(), "initial false");
            checkBox(pane).fire();
            require(button(pane, ButtonType.OK).isDisabled(), "checkbox alone insufficient");
            key(pane, KeyEvent.KEY_PRESSED, KeyCode.ENTER, "");
            require(pane.getScene() != null, "disabled default ENTER cannot confirm");
            field.requestFocus();
            key(field, KeyEvent.KEY_TYPED, KeyCode.UNDEFINED, "x");
            require("x".equals(field.getText()), "text entered through JavaFX dispatch");
            key(field, KeyEvent.KEY_RELEASED, KeyCode.X, "");
            return null;
        });
        var finish = new FutureTask<Void>(() -> {
            var pane = showingPane();
            try {
                initial.get();
                var ok = button(pane, ButtonType.OK);
                require(!ok.isDisabled(), "input refresh enables OK");
                field.setText("throw");
                ok.fire();
                require(ok.isDisabled() && pane.getScene() != null, "exception fails closed at final check");
                require(!button(pane, ButtonType.CANCEL).isDisabled(), "CANCEL unaffected by exception");
            } finally {
                button(pane, ButtonType.CANCEL).fire();
            }
            return null;
        });
        Platform.runLater(() -> {
            initial.run();
            Platform.runLater(finish);
        });
        var cancelled = confirmed.showResultAndWait().orElseThrow();
        finish.get();
        require(cancelled.equals(new Alerts.AlertResult(ButtonType.CANCEL, true)), "cancel with checked state");

        field.setText("null");
        show(confirmed.checkBoxSelected(true), pane -> {
            require(button(pane, ButtonType.OK).isDisabled(), "null blocks OK");
            require(checkBox(pane).isSelected(), "fresh configured state");
        }, ButtonType.CANCEL);
        field.setText("x");
        var result = show(confirmed, pane -> {
            require(!button(pane, ButtonType.OK).isDisabled(), "both conditions satisfied");
            key(pane, KeyEvent.KEY_PRESSED, KeyCode.ENTER, "");
        }, ButtonType.CANCEL);
        require(result.orElseThrow().equals(new Alerts.AlertResult(ButtonType.OK, true)), "valid default ENTER");

        for (var invalid : new Alerts.IAlertMaker[]{
                Alerts.maker().requireCheckBox(ButtonType.OK),
                Alerts.maker().checkBoxText("Missing").requireCheckBox(ButtonType.YES)
        }) {
            try {
                invalid.showResultAndWait();
                throw new AssertionError("invalid gating must fail fast");
            } catch (IllegalArgumentException expected) {
                require(Window.getWindows().stream().noneMatch(Window::isShowing), "invalid dialog never shown");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Platform.startup(() -> Platform.setImplicitExit(false));
        var task = new FutureTask<Void>(() -> {
            exercise();
            return null;
        });
        Platform.runLater(task);
        try {
            task.get(15, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("ALERT-006 PASS");
    }
}
