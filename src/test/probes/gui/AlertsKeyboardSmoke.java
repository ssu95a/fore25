import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Window;
import ru.inversion.fx.app.BaseApp;
import ru.inversion.fx.form.AlertBuilder;
import ru.inversion.fx.form.Alerts;

public final class AlertsKeyboardSmoke {
    private static class OfflineApp extends BaseApp {
        static void install() {
            g_app = new OfflineApp();
        }

        @Override
        public boolean isAfterLogin() {
            return false;
        }
    }

    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void press(Node node, KeyCode code) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false));
    }

    private static void exercise() throws Exception {
        OfflineApp.install();
        var content = new TextArea("draft");
        var dialog = new AlertBuilder(null, Alert.AlertType.CONFIRMATION)
                .contentText(content).buttons(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).build();
        var pane = dialog.getDialogPane();
        var no = (Button) pane.lookupButton(ButtonType.NO);
        var count = new AtomicInteger();
        var shown = new AtomicInteger();
        no.addEventFilter(ActionEvent.ACTION, event -> count.incrementAndGet());
        dialog.setOnShown(event -> shown.incrementAndGet());
        for (int i = 0; i < 2; i++) {
            int expected = i + 1;
            var check = new FutureTask<Void>(() -> {
                try {
                    content.requestFocus();
                    press(content, KeyCode.ENTER);
                    require("draft\n".equals(content.getText()), "TextArea inserts newline");
                    require(dialog.isShowing(), "TextArea ENTER leaves dialog open");
                    no.setDisable(true);
                    press(no, KeyCode.ENTER);
                    require(count.get() == expected - 1 && dialog.isShowing(), "disabled NO cannot fire");
                    no.setDisable(false);
                    no.requestFocus();
                    require(pane.getScene().getFocusOwner() == no, "NO focused");
                    press(no, KeyCode.ENTER);
                    require(count.get() == expected, "exactly one NO action");
                } finally {
                    if (dialog.isShowing()) {
                        ((Button) pane.lookupButton(ButtonType.CANCEL)).fire();
                    }
                }
                return null;
            });
            content.setText("draft");
            content.positionCaret(content.getLength());
            dialog.setResult(null);
            Platform.runLater(check);
            var result = dialog.showAndWait();
            check.get();
            require(result.orElseThrow() == ButtonType.NO, "focused NO result");
            require(shown.get() == expected, "caller onShown preserved");
        }
        dialog.setResult(null);
        Platform.runLater(() -> press(pane, KeyCode.ESCAPE));
        require(dialog.showAndWait().orElseThrow() == ButtonType.CANCEL, "ESC result");

        var defaultCheck = new FutureTask<Void>(() -> {
            var shownPane = Window.getWindows().stream().filter(Window::isShowing)
                    .map(window -> window.getScene().getRoot())
                    .filter(DialogPane.class::isInstance).map(DialogPane.class::cast).findFirst().orElseThrow();
            var yes = (Button) shownPane.lookupButton(ButtonType.YES);
            var makerNo = (Button) shownPane.lookupButton(ButtonType.NO);
            try {
                require(!yes.isDefaultButton() && makerNo.isDefaultButton(), "explicit default");
                press(shownPane, KeyCode.ENTER);
            } finally {
                if (shownPane.getScene() != null) {
                    ((Button) shownPane.lookupButton(ButtonType.CANCEL)).fire();
                }
            }
            return null;
        });
        Platform.runLater(defaultCheck);
        var defaultResult = Alerts.maker().type(Alert.AlertType.CONFIRMATION)
                .buttons(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
                .defaultButton(ButtonType.NO).showButtonAndWait();
        defaultCheck.get();
        require(defaultResult.orElseThrow() == ButtonType.NO, "default ENTER result");
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
        System.out.println("ALERT-003 PASS");
    }
}
