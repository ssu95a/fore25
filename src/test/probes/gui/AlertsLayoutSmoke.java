import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.inversion.fx.form.AlertBuilder;
import ru.inversion.fx.form.Alerts;

public final class AlertsLayoutSmoke {
    private static void require(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
    }

    private static void exercise() throws Exception {
        String line = "\u042F".repeat(3200);
        String multiline = ("\u0422\u0435\u043A\u0441\u0442\n").repeat(400);
        var dialog = new AlertBuilder(null, Alert.AlertType.INFORMATION)
                .contentText(line).detailText(new String[]{multiline, "tail"}).build();
        var check = new FutureTask<Void>(() -> {
            var pane = dialog.getDialogPane();
            try {
                var message = (TextArea) ((VBox) pane.getContent()).getChildren().getFirst();
                var detail = (TextArea) ((GridPane) pane.getExpandableContent()).getChildren().getFirst();
                var screen = Screen.getPrimary().getVisualBounds();
                require(message.getText().equals(line) && message.isWrapText() && !message.isEditable(),
                        "long Cyrillic content selectable and wrapped");
                message.selectAll();
                require(message.getSelectedText().equals(line), "full message selectable");
                require(message.getPrefHeight() > 100 && message.getPrefHeight() <= screen.getHeight() / 3,
                        "long unbroken message wraps within the screen budget");
                require(detail.getText().equals(multiline + "\ntail") && !pane.isExpanded(), "array detail");
                pane.setExpanded(true);
                pane.applyCss();
                pane.layout();
                require(detail.getPrefHeight() <= screen.getHeight() / 3, "expanded detail height bounded");
                dialog.setContentText("word ".repeat(100));
                require(message.getText().equals(dialog.getContentText()), "content remains bound");
                message.resize(450, message.getHeight());
                double wide = message.getPrefHeight();
                message.resize(230, message.getHeight());
                require(message.getPrefHeight() > wide, "narrow width increases measured height");
                double beforeFont = message.getPrefHeight();
                message.setFont(Font.font(20));
                require(message.getPrefHeight() > beforeFont, "larger font increases measured height");
            } finally {
                ((Button) pane.lookupButton(ButtonType.OK)).fire();
            }
            return null;
        });
        Platform.runLater(check);
        require(dialog.showAndWait().orElseThrow() == ButtonType.OK, "legacy OK");
        check.get();

        var content = new Label("content");
        var detail = new Label("detail");
        var maker = Alerts.maker().content(content).detail(detail).expanded(false).resizable(false);
        for (int show = 0; show < 2; show++) {
            boolean expanded = show == 1;
            var inspect = new FutureTask<Void>(() -> {
                var stage = Window.getWindows().stream().filter(Window::isShowing).map(Stage.class::cast)
                        .filter(window -> window.getScene().getRoot() instanceof DialogPane).findFirst().orElseThrow();
                var pane = (DialogPane) stage.getScene().getRoot();
                try {
                    require(((VBox) pane.getContent()).getChildren().getFirst() == content, "same content node");
                    require(pane.getExpandableContent() == detail, "same details node");
                    require(expanded == pane.isExpanded(), "explicit initial expansion");
                    require(stage.isResizable() == expanded, "explicit resize policy");
                } finally {
                    ((Button) pane.lookupButton(ButtonType.OK)).fire();
                }
                return null;
            });
            Platform.runLater(inspect);
            require(maker.expanded(expanded).resizable(expanded).showButtonAndWait().orElseThrow() == ButtonType.OK,
                    "maker result with reused nodes");
            inspect.get();
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
        System.out.println("ALERT-005 PASS");
    }
}
